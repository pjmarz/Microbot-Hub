package net.runelite.client.plugins.microbot.smithingplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.smithingplus.data.AnvilItem;
import net.runelite.client.plugins.microbot.smithingplus.data.AnvilLocationOption;
import net.runelite.client.plugins.microbot.smithingplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.smithingplus.data.Bars;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AutoSmithingPlus: smiths bars into items at a configured anvil, with a bank cycle, autohop,
 * pre-flight validation (bronze-only anvils, members-only items, Smithing level), and a
 * progressive mode that picks the best item for the current level and bar tier.
 */
@Slf4j
public class AutoSmithingPlusScript extends Script {

    private static final int ANVIL_WIDGET_CONTAINER = 312;
    private static final int ANVIL_MAKE_QTY_CHILD = 7; // "All" multiplier in the smithing widget
    // Player varp holding the anvil make-quantity; when it already equals the bar count "All" is
    // selected and re-clicking the multiplier is redundant.
    private static final int ANVIL_MAKE_QTY_VARP = 2224;
    // Animation-poll window (ms) used to treat the player as busy while a smith batch runs.
    private static final int SMITH_ANIM_TIMEOUT_MS = 2400;

    State state = State.SMITHING;

    // runtime stats tracking (read by AutoSmithingPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    // target-level cleanup flag. Intercepted after deposit in handleBankAndWithdraw.
    private boolean shutdownAfterCleanup = false;

    // smith stall detection. If smith clicks produce no Smithing XP across several attempts, the
    // selected item is greyed (above our Smithing level, or wrong bar for it) and the bot would
    // loop forever clicking it ("looks frozen"). Track XP between attempts and bail.
    private int smithLastAttemptXp = -1;
    private int smithNoProgressAttempts = 0;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }

    // Config reference kept so the overlay can resolve the active bar + item for the GP/hr line.
    private AutoSmithingPlusConfig config;
    /** The bar tier currently being smithed, for the overlay GP/hr line. */
    public Bars getActiveBar() { return config != null ? config.selectedBar() : null; }
    /** The item currently being smithed (resolves progressive mode), for the overlay GP/hr line. */
    public AnvilItem getActiveItem() { return config != null ? activeItem(config) : null; }

    public boolean run(AutoSmithingPlusConfig config) {
        this.config = config;
        initialPlayerLocation = null;
        state = State.SMITHING;

        // seed stats trackers from client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false;
        smithLastAttemptXp = -1;      // reset stall detection
        smithNoProgressAttempts = 0;

        Rs2Walker.disableTeleports = true;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applySmithingSetup();

        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        // Pre-flight checks. Refuse-start with a chat log if the config is incoherent.
        if (config.anvilLocation() != null && config.anvilLocation().isBronzeOnly()
                && config.selectedBar() != Bars.BRONZE) {
            Microbot.log("Pre-flight FAILED: anvil " + config.anvilLocation().getDisplayName()
                    + " is bronze-only but selected bar is " + config.selectedBar().getName()
                    + ". Pick BRONZE or a different anvil. Shutting down.");
            super.shutdown();
            return false;
        }
        // item/level pre-flight. Progressive mode validates that SOME item is makeable at the
        // chosen bar for our level; manual mode validates the user's specific item + bar (members
        // gate + Smithing-level gate). The stall-detection stays as a backstop.
        if (config.progressiveSmith()) {
            AnvilItem progBest = AnvilItem.bestForLevel(config.selectedBar(), startSkillLevel, Rs2Player.isMember());
            if (progBest == null) {
                Microbot.log("Pre-flight FAILED: progressive mode found no smithable item for "
                        + config.selectedBar().getName() + " at Smithing level " + startSkillLevel
                        + " (members items excluded on F2P). Lower the bar tier. Shutting down.");
                super.shutdown();
                return false;
            }
        } else {
            if (AnvilItem.isMembersOnly(config.selectedItem()) && !Rs2Player.isMember()) {
                Microbot.log("Pre-flight FAILED: item " + config.selectedItem().getName()
                        + " is members-only and you're on F2P. Pick a different item. Shutting down.");
                super.shutdown();
                return false;
            }
            int requiredSmithLevel = config.selectedItem().getRequiredLevel(config.selectedBar());
            if (requiredSmithLevel > 0 && startSkillLevel < requiredSmithLevel) {
                Microbot.log("Pre-flight FAILED: " + config.selectedItem().getName() + " ("
                        + config.selectedBar().getName() + ") needs Smithing " + requiredSmithLevel
                        + " but you are level " + startSkillLevel + ". Pick a lower item or bar tier. Shutting down.");
                super.shutdown();
                return false;
            }
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                // Pause check using global Microbot.pauseAllScripts (toggled via overlay button).
                // Stats keep accumulating naturally during pause.
                if (Microbot.pauseAllScripts.get()) {
                    Microbot.status = "[PAUSED]";
                    return;
                }

                // stopAfterMinutes / stopAfterXp threshold check.
                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoSmithingPlus: reached stopAfterMinutes (" + config.stopAfterMinutes()
                            + " min). Shutting down.");
                    super.shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0) {
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(startSkillXp);
                    if (currentXp - startSkillXp >= config.stopAfterXp()) {
                        Microbot.log("AutoSmithingPlus: reached stopAfterXp (" + (currentXp - startSkillXp)
                                + " XP). Shutting down.");
                        super.shutdown();
                        return;
                    }
                }

                // target-level check.
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(startSkillLevel);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoSmithingPlus: reached targetLevel (" + currentLevel + " >= "
                                + config.targetLevel() + "). Depositing items before shutdown.");
                        shutdownAfterCleanup = true;
                        if (Rs2Inventory.isEmpty()) {
                            super.shutdown();
                            return;
                        }
                        state = State.RESETTING;
                    }
                }

                // League mode: periodic key press resets the idle-logout. Pattern from
                // AutoMiningPlus.
                if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                    int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length - 1)]);
                }

                if (Rs2AntibanSettings.actionCooldownActive) return;

                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }
                if (initialPlayerLocation == null) return;

                // Anchor re-pin per tick when a specific anvil is configured.
                AnvilLocationOption anvilChoice = config.anvilLocation();
                if (anvilChoice != null && anvilChoice != AnvilLocationOption.AUTO_NEAREST
                        && anvilChoice.getWorldPoint() != null) {
                    initialPlayerLocation = anvilChoice.getWorldPoint();
                }

                if (Rs2Player.isMoving() || Rs2Player.isAnimating(SMITH_ANIM_TIMEOUT_MS)) return;

                // Autohop: only relevant while actively smithing at the anvil (busy/PK risk).
                if (state == State.SMITHING && config.maxPlayersInArea() > 0) {
                    if (hopIfCrowded(config)) return;
                }

                // State auto-flip: if we don't have enough bars or no hammer, switch to RESETTING.
                if (!inventoryHasMaterialsForOneCraft(config)) {
                    state = State.RESETTING;
                }

                switch (state) {
                    case SMITHING:
                        smithAtAnvil(config);
                        break;
                    case RESETTING:
                        handleBankAndWithdraw(config);
                        break;
                }

                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            } catch (Exception ex) {
                Microbot.logStackTrace("AutoSmithingPlusScript", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        // Reset the disableTeleports flag set in run() so it doesn't leak to the next plugin that
        // uses Rs2Walker.
        Rs2Walker.disableTeleports = false;
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    // --- Autohop ---

    private boolean hopIfCrowded(AutoSmithingPlusConfig config) {
        int maxPlayers = config.maxPlayersInArea();
        WorldPoint localLocation = Rs2Player.getWorldLocation();
        if (localLocation == null) return false;

        long nearbyPlayers = Microbot.getClientThread().runOnClientThreadOptional(() ->
                        Microbot.getClient().getTopLevelWorldView().players().stream()
                                .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                                .filter(p -> {
                                    if (config.distanceToStray() == 0) {
                                        return p.getWorldLocation().equals(localLocation);
                                    }
                                    return p.getWorldLocation().distanceTo(localLocation) <= config.distanceToStray();
                                })
                                .count())
                .orElse(0L);

        if (nearbyPlayers >= maxPlayers) {
            Microbot.status = "Too many players nearby. Hopping...";
            Rs2Random.waitEx(3200, 800);
            int world = Login.getRandomWorld(Rs2Player.isMember());
            boolean hopped = Microbot.hopToWorld(world);
            if (hopped) {
                Microbot.status = "Hopped to world: " + world;
                return true;
            }
        }
        return false;
    }

    // --- SMITHING state ---

    private void smithAtAnvil(AutoSmithingPlusConfig config) {
        AnvilLocationOption choice = config.anvilLocation();

        if (choice != null && choice != AnvilLocationOption.AUTO_NEAREST
                && choice.getWorldPoint() != null) {
            WorldPoint target = choice.getWorldPoint();
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null && playerLoc.distanceTo(target) > config.distanceToStray()) {
                Microbot.status = "Walking to " + choice.getDisplayName();
                Rs2Walker.walkTo(target, Math.min(config.distanceToStray(), 5));
                return;
            }
        }

        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen(), 1500);
            return;
        }

        // Name-based anvil query matches both regular anvils ("Anvil", object ID 2097) and
        // variant anvils ("Rusted anvil", object ID 39620).
        Rs2TileObjectModel anvil = Microbot.getRs2TileObjectCache().query()
                .withNameContains("anvil")
                .nearestOnClientThread(initialPlayerLocation, 20);

        if (anvil == null) {
            if (initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) > 4) {
                Microbot.status = "Walking back to anvil anchor";
                Rs2Walker.walkTo(initialPlayerLocation, 4);
            } else {
                Microbot.status = "AutoSmithingPlus: no anvil within 20 tiles of anchor - pick an Anvil in config or stand near one";
            }
            return;
        }

        Bars bar = config.selectedBar();
        AnvilItem item = activeItem(config);
        Microbot.status = "Smithing " + bar.getName() + " -> " + item.getName();

        anvil.click("Smith");
        sleepUntil(() -> Rs2Widget.getWidget(ANVIL_WIDGET_CONTAINER, 1) != null, 5000);
        sleep(180, 480);

        if (Rs2Widget.getWidget(ANVIL_WIDGET_CONTAINER, 1) == null) {
            Microbot.status = "Anvil widget didn't open; will retry next tick";
            return;
        }

        // Click "All" multiplier only when the make-quantity is not already maxed, so we don't
        // fire a redundant click + sleep every cycle once "All" is selected.
        if (Microbot.getVarbitPlayerValue(ANVIL_MAKE_QTY_VARP) < Rs2Inventory.count(bar.getId())) {
            Rs2Widget.clickWidget(ANVIL_WIDGET_CONTAINER, ANVIL_MAKE_QTY_CHILD);
            sleep(180, 480);
        }

        // Stall detection. Compare Smithing XP since the previous smith click. If 4 consecutive
        // clicks produce no XP, the item is greyed (above our Smithing level, or the wrong bar for
        // it) and we'd loop forever on a no-op click. Bail with a clear message. Also catches a
        // drifted widget child id. XP recorded BEFORE the smith, so a working cycle's gain registers
        // by the next click and resets the counter (bank trips don't false-trip it).
        int smithXpNow = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(smithLastAttemptXp);
        if (smithLastAttemptXp >= 0 && smithXpNow <= smithLastAttemptXp) {
            if (++smithNoProgressAttempts >= 4) {
                Microbot.log("AutoSmithingPlus: 4 smith attempts with no Smithing XP gained. '"
                        + item.getName() + "' is likely above your Smithing level, or the wrong "
                        + "bar (" + bar.getName() + ") is selected for it. Shutting down so you're "
                        + "not stuck clicking a greyed item.");
                super.shutdown();
                return;
            }
        } else {
            smithNoProgressAttempts = 0;
        }
        smithLastAttemptXp = smithXpNow;

        // Click the chosen item's child slot. Count the cycle only if smithing actually started:
        // a greyed/no-op click never animates, while a real "Smith All" does, so the counter and
        // the overlay's GP/hr estimate track real cycles instead of blind clicks.
        Rs2Widget.clickWidget(ANVIL_WIDGET_CONTAINER, item.getChildId());
        boolean started = sleepUntil(() -> Rs2Player.isAnimating(SMITH_ANIM_TIMEOUT_MS), 1200);
        if (started) {
            actionsCompleted++;
        }
    }

    // --- RESETTING state ---

    private void handleBankAndWithdraw(AutoSmithingPlusConfig config) {
        if (!Rs2Bank.isOpen()) {
            if (!walkToConfiguredBank(config)) {
                return;
            }
            return;
        }
        Rs2Player.waitForWalking();
        sleep(600, 1200);

        // CSV-driven deposit. Inclusion list wins; exclusion list as fallback.
        depositByCsv(config);
        AnvilItem item = activeItem(config);
        sleepUntil(() -> !Rs2Inventory.hasItem(item.getName()), 3000);

        // targetLevel cleanup done: shutdown before re-withdrawing hammer/bars.
        if (shutdownAfterCleanup) {
            Microbot.log("AutoSmithingPlus: targetLevel cleanup complete. Shutting down.");
            Rs2Bank.closeBank();
            super.shutdown();
            return;
        }

        // Withdraw hammer if missing. Skipped entirely when the hammer is on the tool belt (config
        // toggle), since no loose hammer is needed in that case.
        if (!config.hammerOnToolBelt() && !Rs2Inventory.hasItem(ItemID.HAMMER)) {
            if (!Rs2Bank.hasItem(ItemID.HAMMER)) {
                Microbot.log("No hammer in inventory or bank. Tick 'Hammer on tool belt' in config "
                        + "if yours is stored there. Shutting down.");
                Rs2Bank.closeBank();
                shutdown();
                return;
            }
            Rs2Bank.withdrawOne(ItemID.HAMMER);
            sleepUntil(() -> Rs2Inventory.hasItem(ItemID.HAMMER), 3000);
            return;
        }

        // Withdraw bars: all of them.
        String barName = config.selectedBar().getName();
        if (Rs2Bank.count(barName) < item.getRequiredBars()) {
            Microbot.log("Bank lacks " + item.getRequiredBars() + " " + barName
                    + " for one " + item.getName() + ". Shutting down.");
            Rs2Bank.closeBank();
            shutdown();
            return;
        }
        Rs2Bank.withdrawAll(barName);
        sleepUntil(() -> Rs2Inventory.hasItem(barName), 3000);

        if (!Rs2Inventory.hasItem(barName)) {
            Microbot.log("Withdraw of " + barName + " didn't settle in 3s; retrying next tick.");
            return;
        }

        if (inventoryHasMaterialsForOneCraft(config)) {
            Rs2Bank.closeBank();
            state = State.SMITHING;
        }
    }

    private void depositByCsv(AutoSmithingPlusConfig config) {
        List<String> bankNames = parseCsv(config.itemsToBank());
        List<String> keepNames = parseCsv(config.itemsToKeep());

        if (!bankNames.isEmpty()) {
            Rs2Bank.depositAll(i -> i.getName() != null
                    && bankNames.stream().anyMatch(b -> i.getName().toLowerCase().contains(b))
                    && keepNames.stream().noneMatch(k -> i.getName().toLowerCase().contains(k)));
        } else if (!keepNames.isEmpty()) {
            Rs2Bank.depositAll(i -> i.getName() != null
                    && keepNames.stream().noneMatch(k -> i.getName().toLowerCase().contains(k)));
        } else {
            Rs2Bank.depositAll();
        }
    }

    private List<String> parseCsv(String s) {
        if (s == null) return Collections.emptyList();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean walkToConfiguredBank(AutoSmithingPlusConfig config) {
        BankLocationOption choice = config.bankLocation();
        if (choice == null || choice == BankLocationOption.AUTO_NEAREST) {
            return Rs2Bank.walkToBankAndUseBank();
        }
        WorldPoint bankPoint = choice.getWorldPoint();
        if (bankPoint == null) {
            return Rs2Bank.walkToBankAndUseBank();
        }
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) return false;

        if (playerLocation.getX() != bankPoint.getX()
                || playerLocation.getY() != bankPoint.getY()
                || playerLocation.getPlane() != bankPoint.getPlane()) {
            if (playerLocation.distanceTo(bankPoint) > 4) {
                if (!Rs2Player.isMoving()) {
                    Rs2Walker.walkTo(bankPoint, 4);
                }
                return false;
            }
        }
        return Rs2Bank.openBank();
    }

    // --- Helpers ---

    /**
     * The item to smith this tick: in progressive mode, the best item our current Smithing level
     * can make at the configured bar (recomputed as we level up); otherwise the configured item.
     * Falls back to the configured item if progressive somehow finds nothing.
     */
    private AnvilItem activeItem(AutoSmithingPlusConfig config) {
        if (!config.progressiveSmith()) return config.selectedItem();
        int level = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(startSkillLevel);
        AnvilItem best = AnvilItem.bestForLevel(config.selectedBar(), level, Rs2Player.isMember());
        return best != null ? best : config.selectedItem();
    }

    private boolean inventoryHasMaterialsForOneCraft(AutoSmithingPlusConfig config) {
        if (!hasHammer(config)) return false;
        String barName = config.selectedBar().getName();
        int needed = activeItem(config).getRequiredBars();
        return Rs2Inventory.hasItemAmount(barName, needed, false, true);
    }

    /**
     * Hammer present for smithing? A hammer in the inventory satisfies it, and so does the "Hammer
     * on tool belt" config opt-in. The OSRS tool belt is not exposed by any varbit or Rs2 helper in
     * this client/API version, so it cannot be auto-detected from code; the toggle is how the user
     * declares it. With the toggle on we never require or withdraw a loose hammer, since the tool
     * belt one is always available at the anvil.
     */
    private boolean hasHammer(AutoSmithingPlusConfig config) {
        return config.hammerOnToolBelt() || Rs2Inventory.hasItem(ItemID.HAMMER);
    }
}
