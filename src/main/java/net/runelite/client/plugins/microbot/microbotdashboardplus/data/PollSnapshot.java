package net.runelite.client.plugins.microbot.microbotdashboardplus.data;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of all data the dashboard renders in one poll cycle.
 *
 * <p>Produced by {@link net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller}
 * on each tick or scheduled refresh. Panels read fields directly; no shared
 * mutable state between poller and UI.
 *
 * <p>Use {@link #empty()} for the "no data yet" initial state.
 */
@Value
@Builder
public class PollSnapshot {

    /** Wall-clock millis when this snapshot was produced. */
    long timestampMillis;

    /** Whether the client was logged in when this snapshot was produced. */
    boolean loggedIn;

    // ------- Player section -------
    String playerName;
    int combatLevel;
    String gameState;
    int worldId;
    String profileName;
    String positionText;
    String animationText;

    // ------- Skills section -------
    /** Per-skill total XP. */
    Map<Skill, Integer> skillXp;

    /** Per-skill level (real, not virtual). */
    Map<Skill, Integer> skillLevels;

    // ------- Active scripts section -------
    List<ScriptStatus> activeScripts;

    // ------- Plus plugins quickstart -------
    List<PlusPluginStatus> plusPlugins;

    // ------- Inventory -------
    /** Inventory items (slot 0..27) with display names + quantities. */
    List<InventoryItem> inventory;

    // ------- Nearby NPCs -------
    List<NearbyNpc> nearbyNpcs;

    // ------- Watchdog (read from log file) -------
    WatchdogStatus watchdog;

    public static PollSnapshot empty() {
        return PollSnapshot.builder()
                .timestampMillis(System.currentTimeMillis())
                .loggedIn(false)
                .playerName("--")
                .gameState("--")
                .profileName("--")
                .positionText("--")
                .animationText("--")
                .skillXp(Collections.emptyMap())
                .skillLevels(Collections.emptyMap())
                .activeScripts(Collections.emptyList())
                .plusPlugins(Collections.emptyList())
                .inventory(Collections.emptyList())
                .nearbyNpcs(Collections.emptyList())
                .build();
    }

    @Value
    @Builder
    public static class ScriptStatus {
        String pluginClassName;
        String displayName;
        String status;           // "Running", "Paused", etc.
        long runtimeMillis;
    }

    @Value
    @Builder
    public static class PlusPluginStatus {
        String pluginClassName;
        String displayName;
        boolean installed;
        boolean active;
    }

    @Value
    @Builder
    public static class InventoryItem {
        int slot;
        int itemId;
        String name;
        int quantity;
        boolean noted;
    }

    @Value
    @Builder
    public static class NearbyNpc {
        String name;
        int combatLevel;
        int distance;
        boolean randomEvent;
    }

    @Value
    @Builder
    public static class WatchdogStatus {
        /** "ok", "warn", "bad", "unavailable". */
        String status;
        String lastEventText;
        String lastRestartText;
        int totalRestarts;
    }
}
