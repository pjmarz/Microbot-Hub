package net.runelite.client.plugins.microbot.smeltingplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.smeltingplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.smeltingplus.data.Bars;
import net.runelite.client.plugins.microbot.smeltingplus.data.FurnaceLocationOption;
import net.runelite.client.plugins.microbot.smeltingplus.data.Ores;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AutoSmeltingPlus v0.2.0.
 *
 * <p>v0.1.x shipped the auto-travel MVP + bank routing + speed mode. v0.2.0 adds the WC borrows:
 * <ul>
 *   <li><b>Progressive smelt</b>: auto-pick highest bar the player can smelt given Smithing level
 *       + bank ore stock. Re-evaluated each bank trip.</li>
 *   <li><b>Max players in area + autohop</b>: world-hop when busy. Borrowed verbatim from
 *       AutoMiningPlus v0.1.x.</li>
 *   <li><b>League mode</b>: periodic arrow-key press to reset the game's idle-logout.</li>
 *   <li><b>CSV itemsToBank / itemsToKeep</b>: replaces v0.1.x's hardcoded
 *       {@code depositAllExcept(COAL_BAG_ID)}. Inclusion list first, exclusion list as fallback.</li>
 *   <li><b>dropOrder</b>: config item present for parity; not yet used at v0.2.0.</li>
 * </ul>
 */
@Slf4j
public class AutoSmeltingPlusScript extends Script {

    private static final int COAL_BAG_ID = ItemID.COAL_BAG;

    State state = State.SMELTING;
    private Bars activeBar = null;
    private boolean coalBagEmpty = true;
    private boolean coalBagHasBeenFilled = false;

    // runtime stats tracking (read by AutoSmeltingPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    // v0.5.0: set when targetLevel is reached; intercepted after the deposit step in
    // handleBankAndWithdraw so we shutdown before withdrawing new ores.
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }

    public boolean run(AutoSmeltingPlusConfig config) {
        initialPlayerLocation = null;
        coalBagEmpty = true;
        coalBagHasBeenFilled = false;
        state = State.SMELTING;
        activeBar = null;

        // seed stats trackers from client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false; // v0.5.0: reset target-level cleanup flag on startup

        Rs2Walker.disableTeleports = true;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applySmithingSetup();

        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
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

                // stopAfterMinutes / stopAfterXp threshold check.
                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoSmeltingPlus: reached stopAfterMinutes (" + config.stopAfterMinutes()
                            + " min). Shutting down.");
                    super.shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0) {
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(Skill.SMITHING)).orElse(startSkillXp);
                    if (currentXp - startSkillXp >= config.stopAfterXp()) {
                        Microbot.log("AutoSmeltingPlus: reached stopAfterXp (" + (currentXp - startSkillXp)
                                + " XP). Shutting down.");
                        super.shutdown();
                        return;
                    }
                }

                // v0.5.0: target-level check. Trips RESETTING for one final deposit pass,
                // then shutdownAfterCleanup short-circuits before withdrawing new ores.
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.SMITHING)).orElse(startSkillLevel);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoSmeltingPlus: reached targetLevel (" + currentLevel + " >= "
                                + config.targetLevel() + "). Depositing bars before shutdown.");
                        shutdownAfterCleanup = true;
                        if (Rs2Inventory.isEmpty()) {
                            super.shutdown();
                            return;
                        }
                        state = State.RESETTING;
                    }
                }

                // League mode: periodic key press resets the idle-logout. Pattern from
                // AutoMiningPlus :73-76.
                if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                    int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length - 1)]);
                }

                if (Rs2AntibanSettings.actionCooldownActive) return;

                // Resolve activeBar (manual selection vs progressive).
                updateActiveBar(config);
                if (activeBar == null) return; // shouldn't happen; defensive

                int currentSmithing = Rs2Player.getRealSkillLevel(Skill.SMITHING);
                if (currentSmithing <= 0) return; // transient during login / region load
                int requiredSmithing = activeBar.getRequiredSmithingLevel();
                if (currentSmithing < requiredSmithing) {
                    Microbot.log("Smithing " + currentSmithing + " is below the " + requiredSmithing
                            + " required for " + activeBar + ". Shutting down.");
                    super.shutdown();
                    return;
                }

                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }
                if (initialPlayerLocation == null) return;

                FurnaceLocationOption furnaceChoice = config.furnaceLocation();
                if (furnaceChoice != null && furnaceChoice != FurnaceLocationOption.AUTO_NEAREST
                        && furnaceChoice.getWorldPoint() != null) {
                    initialPlayerLocation = furnaceChoice.getWorldPoint();
                }

                if (Rs2Player.isMoving() || Rs2Player.isAnimating(6500)) return;

                // Autohop: only relevant while smelting at the furnace (busy/PK risk).
                // Pattern from AutoMiningPlus :114-145.
                if (state == State.SMELTING && config.maxPlayersInArea() > 0) {
                    if (hopIfCrowded(config)) return;
                }

                // State auto-flip: if we don't have enough materials, switch to RESETTING.
                if (!inventoryHasMaterialsForOneBar()) {
                    state = State.RESETTING;
                }

                switch (state) {
                    case SMELTING:
                        smeltAtFurnace(config);
                        break;
                    case RESETTING:
                        handleBankAndWithdraw(config);
                        break;
                }

                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            } catch (Exception ex) {
                Microbot.logStackTrace("AutoSmeltingPlusScript", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        // v0.5.9: reset disableTeleports flag set in run() so it doesn't leak to other plugins
        // sharing Rs2Walker. Same lifecycle-hygiene shape as the v0.5.7 pauseAllScripts reset.
        Rs2Walker.disableTeleports = false;
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    // --- Active bar selection ---

    private void updateActiveBar(AutoSmeltingPlusConfig config) {
        if (!config.progressiveSmelt()) {
            activeBar = config.selectedBar();
            return;
        }
        if (activeBar == null) {
            activeBar = config.selectedBar();
        }
        // Progressive only re-evaluates when the bank is open (we can read bank state).
        if (Rs2Bank.isOpen()) {
            Bars best = chooseProgressiveBar();
            if (best != null) {
                activeBar = best;
            }
        }
    }

    private Bars chooseProgressiveBar() {
        int smithing = Rs2Player.getRealSkillLevel(Skill.SMITHING);
        Bars best = null;
        for (Bars b : Bars.values()) {
            if (b.getRequiredSmithingLevel() > smithing) continue;
            boolean hasOres = b.getRequiredMaterials().entrySet().stream()
                    .allMatch(e -> Rs2Bank.hasBankItem(e.getKey().toString(), e.getValue(), true));
            if (!hasOres) continue;
            if (best == null || b.getRequiredSmithingLevel() > best.getRequiredSmithingLevel()) {
                best = b;
            }
        }
        return best;
    }

    // --- Autohop ---

    private boolean hopIfCrowded(AutoSmeltingPlusConfig config) {
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

    // --- SMELTING state ---

    private void smeltAtFurnace(AutoSmeltingPlusConfig config) {
        FurnaceLocationOption choice = config.furnaceLocation();

        if (choice != null && choice != FurnaceLocationOption.AUTO_NEAREST
                && choice.getWorldPoint() != null) {
            WorldPoint targetFurnace = choice.getWorldPoint();
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null && playerLoc.distanceTo(targetFurnace) > config.distanceToStray()) {
                Microbot.status = "Walking to " + choice.getDisplayName();
                Rs2Walker.walkTo(targetFurnace, Math.min(config.distanceToStray(), 5));
                return;
            }
        }

        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen(), 1500);
            return;
        }

        Rs2TileObjectModel furnace = Microbot.getRs2TileObjectCache().query()
                .withNameContains("furnace")
                .nearestOnClientThread(initialPlayerLocation, 20);

        if (furnace == null) {
            if (initialPlayerLocation.distanceTo(Rs2Player.getWorldLocation()) > 4) {
                Microbot.status = "Walking back to furnace anchor";
                Rs2Walker.walkTo(initialPlayerLocation, 4);
            } else {
                Microbot.status = "AutoSmeltingPlus: no furnace within 20 tiles of anchor - pick a Furnace in config or stand near one";
            }
            return;
        }

        Microbot.status = "Smelting " + activeBar.getName();
        furnace.click("smelt");
        sleepUntil(Rs2Player::isMoving, 1000);
        sleepUntil(() -> !Rs2Player.isMoving(), 6000);
        Rs2Widget.sleepUntilHasWidgetText("What would you like to smelt?", 270, 5, false, 4000);
        Rs2Widget.clickWidget(activeBar.getName());
        Rs2Widget.sleepUntilHasNotWidgetText("What would you like to smelt?", 270, 5, false, 4000);
        actionsCompleted++; // count completed smelt cycles
    }

    // --- RESETTING state ---

    private void handleBankAndWithdraw(AutoSmeltingPlusConfig config) {
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
        sleepUntil(() -> !Rs2Inventory.isFull(), 3000);

        // v0.5.0: targetLevel cleanup done -- shutdown before withdrawing new ores.
        if (shutdownAfterCleanup) {
            Microbot.log("AutoSmeltingPlus: targetLevel cleanup complete. Shutting down.");
            Rs2Bank.closeBank();
            super.shutdown();
            return;
        }

        // Re-evaluate progressive bar now that the bank is open (we can see ore stock).
        updateActiveBar(config);

        if (activeBar == Bars.IRON
                && Rs2Bank.hasItem(ItemID.RING_OF_FORGING)
                && !Rs2Equipment.isWearing(ItemID.RING_OF_FORGING)) {
            Rs2Bank.withdrawAndEquip(ItemID.RING_OF_FORGING);
            return;
        }

        if (activeBar == Bars.GOLD
                && Rs2Bank.hasItem(ItemID.GAUNTLETS_OF_GOLDSMITHING)
                && !Rs2Equipment.isWearing(ItemID.GAUNTLETS_OF_GOLDSMITHING)) {
            Rs2Bank.withdrawAndEquip(ItemID.GAUNTLETS_OF_GOLDSMITHING);
            return;
        }

        // Coal bag handling for Steel/Mithril/Adamant/Rune on members worlds.
        int activeBarId = activeBar.getId();
        boolean needsCoalBag = Rs2Player.isInMemberWorld() && (
                activeBarId == ItemID.STEEL_BAR
                        || activeBarId == ItemID.MITHRIL_BAR
                        || activeBarId == ItemID.ADAMANTITE_BAR
                        || activeBarId == ItemID.RUNITE_BAR);

        if (needsCoalBag && Rs2Bank.hasItem(COAL_BAG_ID) && !Rs2Inventory.hasItem(COAL_BAG_ID)) {
            Rs2Bank.withdrawItem(COAL_BAG_ID);
            return;
        }
        if (!needsCoalBag && Rs2Inventory.hasItem(COAL_BAG_ID)) {
            Rs2Bank.depositItems(COAL_BAG_ID);
            return;
        }
        if ((coalBagEmpty || !coalBagHasBeenFilled) && Rs2Inventory.hasItem(COAL_BAG_ID)) {
            handleCoalBag();
        }

        withdrawMaterials();

        if (inventoryHasMaterialsForOneBar()) {
            state = State.SMELTING;
        }
    }

    private void depositByCsv(AutoSmeltingPlusConfig config) {
        List<String> bankNames = parseCsv(config.itemsToBank());
        List<String> keepNames = parseCsv(config.itemsToKeep());

        if (!bankNames.isEmpty()) {
            // v0.5.8: auto-augment with active bar's first word so non-"bar"-suffix outputs
            // (e.g. MOLTEN_GLASS = "Molten glass") don't slip through the default "bar" filter.
            // Mirrors AutoMiningPlus v0.4.1 deposit-filter auto-augment. For the 9 metal bars
            // the first word ("bronze"/"iron"/etc.) is a no-op since they already match via "bar".
            List<String> filterNames = new ArrayList<>(bankNames);
            if (activeBar != null && activeBar.getName() != null) {
                String firstWord = activeBar.getName().split("\\s+")[0].toLowerCase();
                if (!firstWord.isEmpty() && !filterNames.contains(firstWord)) {
                    filterNames.add(firstWord);
                }
            }
            // Deposit items matching the filter, but never anything in itemsToKeep.
            Rs2Bank.depositAll(i -> i.getName() != null
                    && filterNames.stream().anyMatch(b -> i.getName().toLowerCase().contains(b))
                    && keepNames.stream().noneMatch(k -> i.getName().toLowerCase().contains(k)));
        } else if (!keepNames.isEmpty()) {
            // Deposit everything except itemsToKeep.
            Rs2Bank.depositAll(i -> i.getName() != null
                    && keepNames.stream().noneMatch(k -> i.getName().toLowerCase().contains(k)));
        } else {
            Rs2Bank.depositAll();
        }
    }

    private List<String> parseCsv(String s) {
        if (s == null) return java.util.Collections.emptyList();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean walkToConfiguredBank(AutoSmeltingPlusConfig config) {
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

    private boolean inventoryHasMaterialsForOneBar() {
        if (activeBar == null) return false;
        if (!coalBagEmpty && coalBagHasBeenFilled) {
            Rs2Inventory.interact(COAL_BAG_ID, "Empty");
            Rs2Inventory.waitForInventoryChanges(3000);
            return true;
        }
        for (Map.Entry<Ores, Integer> req : activeBar.getRequiredMaterials().entrySet()) {
            String oreName = req.getKey().toString();
            int amount = req.getValue();
            if (!Rs2Inventory.hasItemAmount(oreName, amount, false, true)) {
                return false;
            }
        }
        return true;
    }

    private void withdrawMaterials() {
        if (activeBar == null) return;
        for (Map.Entry<Ores, Integer> req : activeBar.getRequiredMaterials().entrySet()) {
            String oreName = req.getKey().toString();
            int oneBarCost = req.getValue();
            int totalAmount = Rs2Inventory.hasItem(COAL_BAG_ID)
                    ? activeBar.getWithdrawalsWithCoalBag(Rs2Inventory.capacity()).get(req.getKey())
                    : activeBar.maxBarsForFullInventory() * oneBarCost;
            if (!Rs2Bank.hasBankItem(oreName, totalAmount, true)) {
                Microbot.log("Bank lacks " + totalAmount + " " + oreName + " for "
                        + activeBar.getName() + ". Shutting down.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawX(oreName, totalAmount, true);
            final int expected = totalAmount;
            sleepUntil(() -> Rs2Inventory.hasItemAmount(oreName, expected, false, true), 3500);
            if (!Rs2Inventory.hasItemAmount(oreName, expected, false, true)) {
                Microbot.log("Withdraw of " + expected + " " + oreName
                        + " didn't settle in 3.5s; retrying next tick.");
                return;
            }
        }
    }

    private void handleCoalBag() {
        boolean filled = Rs2Inventory.interact(COAL_BAG_ID, "Fill");
        if (!filled) return;
        sleep(300, 1200);
        coalBagHasBeenFilled = true;
        coalBagEmpty = false;
    }
}
