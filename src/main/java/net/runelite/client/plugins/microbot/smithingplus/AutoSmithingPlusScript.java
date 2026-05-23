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
 * AutoSmithingPlus v0.2.0 (Cycle B of the polish cycle).
 *
 * <p>v0.1.0 shipped the auto-travel MVP + bank cycle + speed mode. v0.2.0 adds:
 * <ul>
 *   <li><b>Bronze-only pre-flight</b> at LUMBRIDGE_RUSTED (refuses non-bronze bar)</li>
 *   <li><b>Members-only pre-flight</b> for items like CLAWS (refuses on F2P)</li>
 *   <li><b>LUMBRIDGE_RUSTED walk-to coord fix</b> (now points at the anvil tile, not the furnace)</li>
 *   <li><b>maxPlayersInArea + autohop</b> ported from MiningPlus</li>
 *   <li><b>League mode</b> arrow-key press to defeat idle-logout</li>
 *   <li><b>CSV itemsToBank / itemsToKeep</b> replaces v0.1.0's hardcoded depositAllExcept</li>
 *   <li><b>dropOrder</b> config item for parity</li>
 * </ul>
 *
 * <h2>Deferred to v0.2.1+</h2>
 * <ul>
 *   <li>Smithing-level pre-flight (e.g. refuse Rune Plate Body at Smithing 1). Needs the
 *       AnvilItemLevels table (26 items x 6 bars = 156 entries).</li>
 *   <li>Toolbelt hammer detection. Current Rs2Inventory.hasItem(HAMMER) misses toolbelt hammers.</li>
 *   <li>Progressive smith. Picks highest-XP item for current level + bar.</li>
 * </ul>
 */
@Slf4j
public class AutoSmithingPlusScript extends Script {

    private static final int ANVIL_WIDGET_CONTAINER = 312;
    private static final int ANVIL_MAKE_QTY_CHILD = 7; // "All" multiplier in the smithing widget

    State state = State.SMITHING;

    // Polish-Cycle 2 v0.3.0: runtime stats tracking (read by AutoSmithingPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    // v0.5.0: target-level cleanup flag. Intercepted after deposit in handleBankAndWithdraw.
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }

    public boolean run(AutoSmithingPlusConfig config) {
        initialPlayerLocation = null;
        state = State.SMITHING;

        // Polish-Cycle 2 v0.3.0: seed stats trackers from client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false; // v0.5.0

        Rs2Walker.disableTeleports = true;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applySmithingSetup();

        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        // Pre-flight checks (v0.2.0). Refuse-start with a chat log if the config is incoherent.
        if (config.anvilLocation() != null && config.anvilLocation().isBronzeOnly()
                && config.selectedBar() != Bars.BRONZE) {
            Microbot.log("Pre-flight FAILED: anvil " + config.anvilLocation().getDisplayName()
                    + " is bronze-only but selected bar is " + config.selectedBar().getName()
                    + ". Pick BRONZE or a different anvil. Shutting down.");
            super.shutdown();
            return false;
        }
        if (AnvilItem.isMembersOnly(config.selectedItem()) && !Rs2Player.isMember()) {
            Microbot.log("Pre-flight FAILED: item " + config.selectedItem().getName()
                    + " is members-only (e.g. Cabin Fever quest gate) and you're on F2P. "
                    + "Pick a different item. Shutting down.");
            super.shutdown();
            return false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                // v0.5.1: pause check using global Microbot.pauseAllScripts (toggled via overlay
                // button). Stats keep accumulating naturally during pause.
                if (Microbot.pauseAllScripts.get()) {
                    Microbot.status = "[PAUSED]";
                    return;
                }

                // Polish-Cycle 2 v0.3.0: stopAfterMinutes / stopAfterXp threshold check.
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

                // v0.5.0: target-level check.
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

                if (Rs2Player.isMoving() || Rs2Player.isAnimating(2400)) return;

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
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    // --- Autohop (Cycle B v0.2.0) ---

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
        AnvilItem item = config.selectedItem();
        Microbot.status = "Smithing " + bar.getName() + " -> " + item.getName();

        anvil.click("Smith");
        sleepUntil(() -> Rs2Widget.getWidget(ANVIL_WIDGET_CONTAINER, 1) != null, 5000);
        sleep(180, 480);

        if (Rs2Widget.getWidget(ANVIL_WIDGET_CONTAINER, 1) == null) {
            Microbot.status = "Anvil widget didn't open; will retry next tick";
            return;
        }

        // Click "All" multiplier so the smith runs through the whole inventory of bars.
        Rs2Widget.clickWidget(ANVIL_WIDGET_CONTAINER, ANVIL_MAKE_QTY_CHILD);
        sleep(180, 480);

        // Click the chosen item's child slot.
        Rs2Widget.clickWidget(ANVIL_WIDGET_CONTAINER, item.getChildId());
        sleep(600, 1200);
        actionsCompleted++; // Polish-Cycle 2 v0.3.0: count completed smith cycles
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

        // CSV-driven deposit (Cycle B v0.2.0). Inclusion list wins; exclusion list as fallback.
        depositByCsv(config);
        sleepUntil(() -> !Rs2Inventory.hasItem(config.selectedItem().getName()), 3000);

        // v0.5.0: targetLevel cleanup done -- shutdown before re-withdrawing hammer/bars.
        if (shutdownAfterCleanup) {
            Microbot.log("AutoSmithingPlus: targetLevel cleanup complete. Shutting down.");
            Rs2Bank.closeBank();
            super.shutdown();
            return;
        }

        // Withdraw hammer if missing.
        if (!Rs2Inventory.hasItem(ItemID.HAMMER)) {
            if (!Rs2Bank.hasItem(ItemID.HAMMER)) {
                Microbot.log("No hammer in inventory or bank. Shutting down.");
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
        if (Rs2Bank.count(barName) < config.selectedItem().getRequiredBars()) {
            Microbot.log("Bank lacks " + config.selectedItem().getRequiredBars() + " " + barName
                    + " for one " + config.selectedItem().getName() + ". Shutting down.");
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

    private boolean inventoryHasMaterialsForOneCraft(AutoSmithingPlusConfig config) {
        if (!Rs2Inventory.hasItem(ItemID.HAMMER)) return false;
        String barName = config.selectedBar().getName();
        int needed = config.selectedItem().getRequiredBars();
        return Rs2Inventory.hasItemAmount(barName, needed, false, true);
    }
}
