package net.runelite.client.plugins.microbot.microbotdashboardplus.poller;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.data.XpHistory;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background poller that builds a {@link PollSnapshot} on a fixed cadence and
 * notifies listeners on the EDT.
 *
 * <p>Replaces the v0.1.x HTTP-polling browser dashboard. Reads game state
 * directly from the Microbot client APIs; no HTTP, no Agent Server dependency.
 *
 * <p>Threading:
 * <ul>
 *     <li>Polling runs on a dedicated single-thread scheduled executor.</li>
 *     <li>All client-thread-restricted reads (widgets, world view, varbits)
 *         are wrapped in {@link net.runelite.client.callback.ClientThread#runOnClientThreadOptional}.</li>
 *     <li>Listeners are invoked on the EDT via {@link SwingUtilities#invokeLater}.</li>
 * </ul>
 *
 * <p>The {@link XpHistory} side-channel tracks per-skill deltas + rolling
 * XP/hr. It's mutated by the poller thread only.
 */
@Slf4j
public class GameStatePoller {

    private final XpHistory xpHistory = new XpHistory();
    private final List<Consumer<PollSnapshot>> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledTask;
    private volatile PollSnapshot lastSnapshot = PollSnapshot.empty();
    private volatile int pollIntervalSeconds = 5;

    /** Maximum distance for NPCs included in {@code nearbyNpcs}. Adjustable from the UI. */
    private volatile int npcMaxDistance = 20;

    public void start(int pollIntervalSeconds) {
        this.pollIntervalSeconds = Math.max(1, pollIntervalSeconds);
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MicrobotDashboardPlus-Poller");
            t.setDaemon(true);
            return t;
        });
        scheduledTask = executor.scheduleAtFixedRate(this::tickSafely, 0, this.pollIntervalSeconds, TimeUnit.SECONDS);
        log.info("MicrobotDashboardPlus poller started (interval={}s)", this.pollIntervalSeconds);
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        log.info("MicrobotDashboardPlus poller stopped");
    }

    public void addListener(Consumer<PollSnapshot> listener) {
        listeners.add(listener);
        // Push the most recent snapshot so newly-registered panels render immediately.
        SwingUtilities.invokeLater(() -> listener.accept(lastSnapshot));
    }

    public void removeListener(Consumer<PollSnapshot> listener) {
        listeners.remove(listener);
    }

    public PollSnapshot getLastSnapshot() {
        return lastSnapshot;
    }

    public XpHistory getXpHistory() {
        return xpHistory;
    }

    public void setNpcMaxDistance(int distance) {
        this.npcMaxDistance = Math.max(1, Math.min(200, distance));
    }

    public int getNpcMaxDistance() {
        return npcMaxDistance;
    }

    /** Trigger an immediate refresh outside the scheduled cadence. */
    public void refreshNow() {
        if (executor != null && !executor.isShutdown()) {
            executor.submit(this::tickSafely);
        }
    }

    private void tickSafely() {
        try {
            PollSnapshot snapshot = buildSnapshot();
            lastSnapshot = snapshot;
            notifyListeners(snapshot);
        } catch (Throwable t) {
            // Never let a poll error kill the executor.
            log.warn("Poll iteration failed: {}", t.getMessage(), t);
        }
    }

    private void notifyListeners(PollSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> {
            for (Consumer<PollSnapshot> l : listeners) {
                try {
                    l.accept(snapshot);
                } catch (Throwable t) {
                    log.warn("Listener threw on snapshot delivery: {}", t.getMessage(), t);
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Snapshot construction. All client-touching reads go through the
    // client thread.
    // ---------------------------------------------------------------------

    private PollSnapshot buildSnapshot() {
        Client client = Microbot.getClient();
        if (client == null) {
            return PollSnapshot.empty();
        }

        // Wrap the entire build in a single client-thread invocation when game
        // state matters; falls back to "empty" if the call returns nothing.
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            PollSnapshot.PollSnapshotBuilder b = PollSnapshot.builder()
                    .timestampMillis(System.currentTimeMillis());

            GameState gs = client.getGameState();
            boolean loggedIn = (gs == GameState.LOGGED_IN);
            b.loggedIn(loggedIn);
            b.gameState(gs == null ? "--" : gs.name());

            // Player
            Player local = client.getLocalPlayer();
            if (local != null) {
                b.playerName(safe(local.getName()));
                b.combatLevel(local.getCombatLevel());
                WorldPoint wp = local.getWorldLocation();
                b.positionText(wp == null ? "--" : (wp.getX() + "," + wp.getY() + "," + wp.getPlane()));
                int anim = local.getAnimation();
                b.animationText(anim < 0 ? "idle" : Integer.toString(anim));
            } else {
                b.playerName("--").positionText("--").animationText("--");
            }

            b.worldId(client.getWorld());
            b.profileName(profileName(client));

            // Skills
            Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
            Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
            for (Skill s : Skill.values()) {
                if (s == Skill.OVERALL) continue;
                int currentXp = client.getSkillExperience(s);
                xp.put(s, currentXp);
                levels.put(s, client.getRealSkillLevel(s));
                xpHistory.record(s, currentXp);
            }
            b.skillXp(Collections.unmodifiableMap(xp));
            b.skillLevels(Collections.unmodifiableMap(levels));

            // Inventory + NPCs deferred to dedicated helpers (called once they're
            // wired in subsequent commits)
            b.inventory(Collections.emptyList());
            b.nearbyNpcs(collectNearbyNpcs(client, local));

            // Plugins / scripts
            b.activeScripts(collectActiveScripts());
            b.plusPlugins(collectPlusPlugins());

            // Watchdog log read deferred (read from disk in v0.2.0 follow-up)
            b.watchdog(PollSnapshot.WatchdogStatus.builder()
                    .status("unavailable")
                    .lastEventText("--")
                    .lastRestartText("--")
                    .totalRestarts(0)
                    .build());

            return b.build();
        }).orElse(PollSnapshot.empty());
    }

    private static String safe(String s) {
        return s == null ? "--" : s;
    }

    private static String profileName(Client client) {
        try {
            return safe(Microbot.getConfigManager().getRSProfileKey());
        } catch (Throwable t) {
            return "--";
        }
    }

    private List<PollSnapshot.NearbyNpc> collectNearbyNpcs(Client client, Player local) {
        if (local == null) return Collections.emptyList();
        WorldPoint playerWp = local.getWorldLocation();
        if (playerWp == null) return Collections.emptyList();

        List<PollSnapshot.NearbyNpc> out = new ArrayList<>();
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            WorldPoint npcWp = npc.getWorldLocation();
            if (npcWp == null) continue;
            int dist = playerWp.distanceTo(npcWp);
            if (dist > npcMaxDistance) continue;

            out.add(PollSnapshot.NearbyNpc.builder()
                    .name(safe(npc.getName()))
                    .combatLevel(npc.getCombatLevel())
                    .distance(dist)
                    .randomEvent(false) // refined when we wire up Rs2RandomEvent detection
                    .build());
        }
        out.sort((a, b) -> Integer.compare(a.getDistance(), b.getDistance()));
        return Collections.unmodifiableList(out);
    }

    /** Package prefix used to identify Microbot Hub plugins (excludes core RuneLite plugins). */
    private static final String MICROBOT_PACKAGE_PREFIX = "net.runelite.client.plugins.microbot.";

    /**
     * Active scripts: enumerate currently-enabled Microbot Hub plugins only.
     * Filters out core RuneLite plugins (XP tracker, world map, ground items,
     * etc.) which are enabled by default and would otherwise drown the list.
     */
    private List<PollSnapshot.ScriptStatus> collectActiveScripts() {
        try {
            List<PollSnapshot.ScriptStatus> out = new ArrayList<>();
            for (Plugin p : Microbot.getPluginManager().getPlugins()) {
                if (p == null) continue;
                if (!Microbot.isPluginEnabled(p.getClass())) continue;
                // Filter to Microbot Hub plugins only.
                if (!p.getClass().getName().startsWith(MICROBOT_PACKAGE_PREFIX)) continue;

                String displayName = p.getName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = p.getClass().getSimpleName();
                }
                out.add(PollSnapshot.ScriptStatus.builder()
                        .pluginClassName(p.getClass().getName())
                        .displayName(displayName)
                        .status("Running")
                        .runtimeMillis(0L) // per-plugin runtime tracking deferred to v0.2.x
                        .build());
            }
            out.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
            return Collections.unmodifiableList(out);
        } catch (Throwable t) {
            log.debug("collectActiveScripts failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    /** Plus plugins quickstart: just the active subset for now. v0.2.1 expands this to a known list. */
    private List<PollSnapshot.PlusPluginStatus> collectPlusPlugins() {
        try {
            List<PollSnapshot.PlusPluginStatus> out = new ArrayList<>();
            for (Plugin p : Microbot.getPluginManager().getPlugins()) {
                if (p == null) continue;
                String displayName = p.getName();
                if (displayName == null) continue;
                // Heuristic: anything tagged with a "Plus" suffix in the display name.
                if (!displayName.endsWith("Plus")) continue;

                out.add(PollSnapshot.PlusPluginStatus.builder()
                        .pluginClassName(p.getClass().getName())
                        .displayName(displayName)
                        .installed(true)
                        .active(Microbot.isPluginEnabled(p.getClass()))
                        .build());
            }
            out.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
            return Collections.unmodifiableList(out);
        } catch (Throwable t) {
            log.debug("collectPlusPlugins failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }
}
