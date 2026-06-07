package net.runelite.client.plugins.microbot.woodcuttingplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2LogBasket;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.skills.fletching.Rs2Fletching;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.*;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.WoodcuttingTreeLocations;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.api.gameval.AnimationID.*;
import static net.runelite.api.gameval.ItemID.TINDERBOX;
import static net.runelite.client.plugins.microbot.util.player.Rs2Player.getRealSkillLevel;


@Slf4j
public class AutoWoodcuttingPlusScript extends Script {

    public static final List<Integer> BURNING_ANIMATION_IDS = List.of(
            FORESTRY_CAMPFIRE_BURNING_LOGS,
            FORESTRY_CAMPFIRE_BURNING_MAGIC_LOGS,
            FORESTRY_CAMPFIRE_BURNING_MAHOGANY_LOGS,
            FORESTRY_CAMPFIRE_BURNING_MAPLE_LOGS,
            FORESTRY_CAMPFIRE_BURNING_OAK_LOGS,
            FORESTRY_CAMPFIRE_BURNING_REDWOOD_LOGS,
            FORESTRY_CAMPFIRE_BURNING_TEAK_LOGS,
            FORESTRY_CAMPFIRE_BURNING_WILLOW_LOGS,
            FORESTRY_CAMPFIRE_BURNING_YEW_LOGS,
            HUMAN_CREATEFIRE
    );

    public static final int FORESTRY_DISTANCE = 15;
    // Corsair Cove Resource Area deposit box. F2P maple/yew trains here, so banking deposits logs
    // at this box instead of the long walk to a full bank.
    private static final WorldPoint CORSAIR_COVE_DEPOSIT_BOX = new WorldPoint(2569, 2862, 0);
    private static final List<WoodcuttingTree> PROGRESSIVE_TREE_ORDER = List.of(
            WoodcuttingTree.TREE,
            WoodcuttingTree.OAK,
            WoodcuttingTree.WILLOW,
            WoodcuttingTree.TEAK_TREE,
            WoodcuttingTree.MAPLE,
            WoodcuttingTree.MAHOGANY,
            WoodcuttingTree.YEW,
            WoodcuttingTree.MAGIC,
            WoodcuttingTree.REDWOOD
    );
    private static WorldPoint returnPoint;
    public volatile boolean cannotLightFire = false;
    WoodcuttingScriptState woodcuttingScriptState = WoodcuttingScriptState.WOODCUTTING;
    private boolean hasAutoHopMessageShown = false;
    private final AutoWoodcuttingPlusPlugin plugin;
    public int currentLogBasketCount = -1;
    private WoodcuttingTree activeTree = WoodcuttingTree.TREE;
    private ResourceLocationOption activeLocation;

    // runtime stats for threshold-based auto-shutdown. Overlay still
    // maintains its own independent tracking; these are script-side fields used only by the
    // stopAfterMinutes / stopAfterXp check in the main loop.
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    // target-level cleanup flag. Intercepted in resetInventory before each primary's state-flip-back
    // to WOODCUTTING/FIREMAKING so we stop once the inventory has been processed.
    private boolean shutdownAfterCleanup = false;
    @Inject
    public AutoWoodcuttingPlusScript(AutoWoodcuttingPlusPlugin plugin) {
        this.plugin = plugin;
    }
    @Inject
    Rs2TileObjectCache rs2TileObjectCache;

    private void handleFiremaking(AutoWoodcuttingPlusConfig config) {
        WoodcuttingTree treeType = getActiveTree();

        if (!Rs2Inventory.hasItem(TINDERBOX)) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
            Rs2Bank.withdrawItem(true, "Tinderbox");
        }

        if (!Rs2Inventory.hasItem(treeType.getLog())) {
            Microbot.log("Opening bank");
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
            Rs2Bank.withdrawAll(treeType.getLog());
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
        }
    }

    public static WorldPoint getReturnPoint(AutoWoodcuttingPlusConfig config) {
        if (config.walkBack().equals(WoodcuttingWalkBack.LAST_LOCATION)) {
            return returnPoint == null ? Rs2Player.getWorldLocation() : returnPoint;
        } else {
            return initialPlayerLocation == null ? Rs2Player.getWorldLocation() : initialPlayerLocation;
        }
    }

    public boolean run(AutoWoodcuttingPlusConfig config) {
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyWoodcuttingSetup();
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.dynamicIntensity = true;
        // speed mode disables antiban entirely for throwaway accounts.
        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }
        activeTree = config.TREE();
        activeLocation = null;
        returnPoint = null; // reset cross-restart static state (also cleared in shutdown())
        // seed stats trackers from client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.WOODCUTTING)).orElse(0);
        shutdownAfterCleanup = false;
        if (config.firemakeOnly()) {
            woodcuttingScriptState = WoodcuttingScriptState.FIREMAKING;
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // pause check using the global Microbot.pauseAllScripts flag (toggled via the
                // overlay button). The overlay reads the same flag to display [PAUSED].
                if (Microbot.pauseAllScripts.get()) {
                    return;
                }

                // stopAfterMinutes / stopAfterXp threshold check.
                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoWoodcuttingPlus: reached stopAfterMinutes ("
                            + config.stopAfterMinutes() + " min). Shutting down.");
                    shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0) {
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(Skill.WOODCUTTING)).orElse(startSkillXp);
                    if (currentXp - startSkillXp >= config.stopAfterXp()) {
                        Microbot.log("AutoWoodcuttingPlus: reached stopAfterXp ("
                                + (currentXp - startSkillXp) + " XP). Shutting down.");
                        shutdown();
                        return;
                    }
                }

                // target-level check. Forces RESETTING for one cleanup pass; the intercept in
                // resetInventory shuts us down before the state-flip-back, once the inventory has
                // been processed by the active primary (BANK/DROP/BURN/FLETCH).
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING)).orElse(0);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoWoodcuttingPlus: reached targetLevel (" + currentLevel
                                + " >= " + config.targetLevel() + "). Cleanup pass then shutdown.");
                        shutdownAfterCleanup = true;
                        if (Rs2Inventory.isEmpty()) {
                            shutdown();
                            return;
                        }
                        woodcuttingScriptState = WoodcuttingScriptState.RESETTING;
                    }
                }

                if (preFlightChecks(config)) return;
                switch (woodcuttingScriptState) {
                    case WOODCUTTING:
                        if (beforeCuttingTreesChecks(config)) return;
                        handleWoodcutting(config);
                        break;
                    case FIREMAKING:
                        handleFiremaking(config);
                        walkBack(config);
                        woodcuttingScriptState = WoodcuttingScriptState.RESETTING;
                        break;
                    case RESETTING:
                        resetInventory(config);
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(getClass().getSimpleName(), ex);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleWoodcutting(AutoWoodcuttingPlusConfig config) {
        WoodcuttingTree treeType = getActiveTree();
        Rs2TileObjectModel tree = null;
        if (config.HardwoodTreePatch()) {
            var patchIds = List.of(30480, 30481, 30482);
            tree = rs2TileObjectCache.query()
                    .where(x -> patchIds.contains(x.getId()))
                    .nearest();
        } else {
            tree = rs2TileObjectCache.query().within(getInitialPlayerLocation(), config.distanceToStray()).withName(treeType.getName()).nearestOnClientThread();
        }

        if (tree != null) {
            if (tree.click(treeType.getAction())) {
                Rs2Player.waitForAnimation();
                Rs2Antiban.actionCooldown();

                if (config.walkBack().equals(WoodcuttingWalkBack.LAST_LOCATION)) {
                    returnPoint = Rs2Player.getWorldLocation();
                }
            }
        }
    }

    private boolean beforeCuttingTreesChecks(AutoWoodcuttingPlusConfig config) {
        WoodcuttingTree treeType = getActiveTree();

        if (Rs2Equipment.isWearing(ItemID.DRAGON_AXE) || Rs2Equipment.isWearing(ItemID.DRAGON_AXE_2H) || Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE) ||
                Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE_2H) || Rs2Equipment.isWearing(ItemID.INFERNAL_AXE) ||
                Rs2Equipment.isWearing(ItemID.TRAILBLAZER_AXE))
            Rs2Combat.setSpecState(true, 1000);
        boolean willBank = willBankItems(config);
        int currentLogCountBeforeFill = Rs2Inventory.count(treeType.getLogID());
        if ( currentLogCountBeforeFill > 0 && currentLogBasketCount < Rs2LogBasket.LOG_BASKET_CAPACITY && Rs2LogBasket.hasLogBasket()  && willBank) {
            if (currentLogBasketCount == -1) {
                Rs2LogBasket.BasketContents content  = Rs2LogBasket.getCurrentBasketContents();
                currentLogBasketCount = content == null ? 0 : content.quantity;
                log.info("Initialized log basket count to {}", currentLogBasketCount);
            }
            if(currentLogBasketCount < Rs2LogBasket.LOG_BASKET_CAPACITY && Rs2Inventory.isFull() && Rs2Inventory.contains(treeType.getLog())) {

                if (Rs2LogBasket.fillLogBasket()) {
                    Rs2Antiban.actionCooldown();
                }
                int currentLogCountAfterFill = Rs2Inventory.count(treeType.getLogID());
                int addedLogs = currentLogCountBeforeFill - currentLogCountAfterFill;
                currentLogBasketCount += addedLogs;
                log.info("Added {} logs to basket, current count: {}", addedLogs, currentLogBasketCount);
            }
        }
       

        if (Rs2Inventory.isFull()) {
            woodcuttingScriptState = WoodcuttingScriptState.RESETTING;
            return true;
        }

        if (handleLooting(config)) {
            Rs2Antiban.actionCooldown();
            return true;
        }

        return false;
    }

    private boolean preFlightChecks(AutoWoodcuttingPlusConfig config) {
        if (!Microbot.isLoggedIn()) return true;
        if (!super.run()) return true;
        if (Rs2Player.getRealSkillLevel(Skill.WOODCUTTING) <= 0) return true;

        if (!config.enableWoodcutting()) {
            updateActiveTree(config);
            return true;
        }

        if (config.hopWhenPlayerDetected()) {
            if (Rs2Player.logoutIfPlayerDetected(1, 10000))
                return true;
        }

        if (Rs2AntibanSettings.actionCooldownActive) return true;

        if (!hasAutoHopMessageShown && config.hopWhenPlayerDetected()) {
            Microbot.showMessage("Make sure autologin plugin is enabled and randomWorld checkbox is checked!");
            hasAutoHopMessageShown = true;
        }

        if (config.hopWhenPlayerDetected() && config.enableForestry()) {
            Microbot.showMessage("Autohop is not supported with forestry enabled, shutting down.");
            shutdown();
            return true;
        }

        if (initialPlayerLocation == null) {
            initialPlayerLocation = Rs2Player.getWorldLocation();
        }

        if (returnPoint == null) {
            returnPoint = Rs2Player.getWorldLocation();
        }

        updateActiveTree(config);

        if (config.progressiveMode() && ensureProgressiveLocation(config)) {
            return true;
        }

        if (!getActiveTree().hasRequiredLevel()) {
            Microbot.showMessage("You do not have the required woodcutting level to cut this tree. " + Rs2Player.getRealSkillLevel(Skill.WOODCUTTING));
            shutdown();
            return true;
        }

        if (!Rs2Inventory.hasItem("axe")) {
            if (!Rs2Equipment.isWearing("axe")) {
                Microbot.showMessage("Unable to find axe in inventory/equipped");
                shutdown();
                return true;
            }
        }

        if (woodcuttingScriptState != WoodcuttingScriptState.RESETTING &&
                (Rs2Player.isMoving() || (Rs2Player.isAnimating() && !BURNING_ANIMATION_IDS.contains(Rs2Player.getLastAnimationID())))) {
            return true;
        }

        if (this.plugin.currentForestryEvent != ForestryEvents.NONE) {
            this.plugin.currentForestryEvent = ForestryEvents.NONE;
        }

        return Rs2AntibanSettings.actionCooldownActive;
    }

    private void resetInventory(AutoWoodcuttingPlusConfig config) {
        switch (config.primaryAction()) {
            case DROP:
                var itemNames = Arrays.stream(config.itemsToKeep().split(",")).map(String::trim).toArray(String[]::new);
                Rs2Inventory.dropAllExcept(false, config.interactOrder(), itemNames);
                // targetLevel cleanup done -- shutdown before resuming chopping.
                if (shutdownAfterCleanup) {
                    Microbot.log("AutoWoodcuttingPlus: targetLevel cleanup (DROP) complete. Shutting down.");
                    shutdown();
                    return;
                }
                woodcuttingScriptState = WoodcuttingScriptState.WOODCUTTING;
                break;
            case BANK:
                if (!handleBanking(config))
                    return;
                // targetLevel cleanup done -- shutdown before resuming chopping.
                if (shutdownAfterCleanup) {
                    Microbot.log("AutoWoodcuttingPlus: targetLevel cleanup (BANK) complete. Shutting down.");
                    shutdown();
                    return;
                }
                woodcuttingScriptState = WoodcuttingScriptState.WOODCUTTING;
                break;
            case BURN_CAMPFIRE:
            case BURN:
                woodcuttingScriptState = WoodcuttingScriptState.FIREMAKING;
                burnLog(config);

                if (Rs2Inventory.contains(getActiveTree().getLog())) return; // still burning

                // targetLevel cleanup done once the inventory is clear -- shutdown before resuming.
                if (shutdownAfterCleanup) {
                    Microbot.log("AutoWoodcuttingPlus: targetLevel cleanup (BURN) complete. Shutting down.");
                    shutdown();
                    return;
                }

                walkBack(config);

                if (config.firemakeOnly()){
                    woodcuttingScriptState = WoodcuttingScriptState.FIREMAKING;
                } else {
                    woodcuttingScriptState = WoodcuttingScriptState.WOODCUTTING;
                }
                break;
            case FLETCH:
                if (handleFletchingWorkflow(config)) {
                    // targetLevel cleanup done -- shutdown before resuming chopping.
                    if (shutdownAfterCleanup) {
                        Microbot.log("AutoWoodcuttingPlus: targetLevel cleanup (FLETCH) complete. Shutting down.");
                        shutdown();
                        return;
                    }
                    woodcuttingScriptState = WoodcuttingScriptState.WOODCUTTING;
                }
                break;
        }
    }

    private boolean ensureProgressiveLocation(AutoWoodcuttingPlusConfig config) {
        if (activeLocation == null || activeLocation.getWorldPoint() == null) {
            return false;
        }

        WorldPoint targetPoint = activeLocation.getWorldPoint();

        if (initialPlayerLocation == null || !initialPlayerLocation.equals(targetPoint)) {
            initialPlayerLocation = targetPoint;
        }

        if (returnPoint == null || !returnPoint.equals(targetPoint)) {
            returnPoint = targetPoint;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return true;
        }

        int acceptableDistance = Math.min(Math.max(1, config.distanceToStray()), 5);
        int distanceToTarget = playerLocation.distanceTo(targetPoint);
        if (distanceToTarget > acceptableDistance) {
            if (Rs2Player.isMoving()) {
                return true;
            }

            Rs2Walker.walkTo(targetPoint, 3);
            return true;
        }

        return false;
    }

    private boolean handleBanking(AutoWoodcuttingPlusConfig config) {
        // Corsair Cove (F2P maple/yew) banks at the nearby deposit box instead of a far full bank.
        if (shouldUseDepositBox()) {
            return handleDepositBoxBanking(config);
        }

        BankLocation nearestBank = Rs2Bank.getNearestBank();
        boolean isBankOpen = Rs2Bank.isNearBank(nearestBank, 8) ? Rs2Bank.openBank() : Rs2Bank.walkToBankAndUseBank(nearestBank);
        if (!isBankOpen || !Rs2Bank.isOpen()) return false;

        // empty log basket first if we have one
        Rs2LogBasket.emptyLogBasketAtBank();
        currentLogBasketCount = 0;
        // deposit items
        List<String> itemNames = Arrays.stream(config.itemsToBank().split(",")).map(String::toLowerCase).collect(Collectors.toList());
        itemNames.add(config.fletchingType().getContainsInventoryName().toLowerCase());
        Rs2Bank.depositAll(i -> itemNames.stream().anyMatch(itemName -> i.getName().toLowerCase().contains(itemName)));
        Rs2Inventory.waitForInventoryChanges(1800);

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen());

        Rs2Walker.walkTo(getReturnPoint(config));
        return true;
    }

    /**
     * True when the current spot banks via a deposit box rather than a full bank. In progressive
     * mode the resolved {@link #activeLocation} carries the flag. Outside progressive mode the
     * location data is not resolved, so we fall back to proximity to the Corsair Cove deposit box
     * (the only deposit-box spot the picker offers).
     */
    private boolean shouldUseDepositBox() {
        if (activeLocation != null && activeLocation.isUseDepositBox()) {
            return true;
        }
        WorldPoint here = Rs2Player.getWorldLocation();
        return here != null && here.distanceTo(CORSAIR_COVE_DEPOSIT_BOX) <= 10;
    }

    /**
     * Deposit-box banking for Corsair Cove. Walks to the deposit box, deposits the same items the
     * full-bank flow deposits (the itemsToBank list plus the fletching output), then walks back to
     * the return point. Woodcutting only ever deposits, so a box is enough. There is no log-basket
     * empty step here: deposit boxes cannot empty a log basket, and the basket is members-only
     * while Corsair maple/yew is F2P, so F2P trainers have no basket.
     */
    private boolean handleDepositBoxBanking(AutoWoodcuttingPlusConfig config) {
        WorldPoint here = Rs2Player.getWorldLocation();
        if (here == null || here.distanceTo(CORSAIR_COVE_DEPOSIT_BOX) > 4) {
            Microbot.status = "Walking to Corsair Cove deposit box...";
            if (!Rs2Player.isMoving()) {
                Rs2Walker.walkTo(CORSAIR_COVE_DEPOSIT_BOX, 4);
            }
            return false;
        }

        if (!Rs2DepositBox.openDepositBox()) {
            return false;
        }
        sleepUntil(Rs2DepositBox::isOpen, 3000);
        if (!Rs2DepositBox.isOpen()) {
            return false;
        }

        List<String> itemNames = Arrays.stream(config.itemsToBank().split(",")).map(String::toLowerCase).collect(Collectors.toList());
        itemNames.add(config.fletchingType().getContainsInventoryName().toLowerCase());
        Rs2DepositBox.depositAll(i -> itemNames.stream().anyMatch(itemName -> i.getName().toLowerCase().contains(itemName)));
        Rs2Inventory.waitForInventoryChanges(1800);

        Rs2DepositBox.closeDepositBox();
        sleepUntil(() -> !Rs2DepositBox.isOpen());

        Rs2Walker.walkTo(getReturnPoint(config));
        return true;
    }

    private boolean handleLooting(AutoWoodcuttingPlusConfig config)
    {
        if (!config.lootBirdNests() && !config.lootSeeds()) {
            return false; // No looting options selected
        }

        List<String> itemsToLootList = new ArrayList<>();

            if (config.lootSeeds()) {
                itemsToLootList.add("seed");
            }
            if (config.lootBirdNests()) {
                itemsToLootList.add("nest");
            }

            String[] itemsToLoot = itemsToLootList.toArray(new String[0]);

        LootingParameters itemLootParams = new LootingParameters(
                15,
                1,
                1,
                1,
                false,
                config.lootMyItemsOnly(),
                itemsToLoot
        );
        return Rs2GroundItem.lootItemsBasedOnNames(itemLootParams);
    }

    private void burnLog(AutoWoodcuttingPlusConfig config) {
        WoodcuttingTree treeType = getActiveTree();
        WorldPoint fireSpot;
        boolean useCampfire = false;

        // prioritize campfire if available
        Rs2TileObjectModel fire = rs2TileObjectCache.query().where(x -> x.getId() == 49927).nearest(6); // Forester's campfire
        if (fire == null) {
            fire = rs2TileObjectCache.query().where(x -> x.getId() == 26185).nearest(6);
        }
        if (config.primaryAction() == WoodcuttingPrimaryAction.BURN_CAMPFIRE) {
            if (fire != null) {
                useCampfire = true;
            }
        }
        if ((Rs2Player.isStandingOnGameObject() || cannotLightFire) && !Rs2Player.isAnimating() && !useCampfire) {
            fireSpot = fireSpot(1);
            Rs2Walker.walkFastCanvas(fireSpot);
            cannotLightFire = false;
        }
        if (!isFiremake() && !useCampfire) {
            Rs2Inventory.waitForInventoryChanges(() -> {
                Rs2Inventory.use("tinderbox");
                sleepUntil(Rs2Inventory::isItemSelected);
                Rs2Inventory.useLast(treeType.getLogID());
            }, 300, 100);
        } else if (!isFiremake() && useCampfire) {
            Rs2Inventory.useItemOnObject(treeType.getLogID(), fire.getId());
            sleepUntil(() -> (!Rs2Player.isMoving() && Rs2Widget.findWidget("How many would you like to burn?", null, false) != null), 5000);
            Rs2Random.waitEx(400, 200);
            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            sleepUntil(Rs2Player::isAnimating, 2000);
            Microbot.log("Sleeping until not animating or no more logs");
            sleepUntil(() -> !Rs2Inventory.contains(treeType.getLog()) || !Rs2Player.isAnimating(), 40000);

            return;
        }
        sleepUntil(() -> !isFiremake());
        if (!isFiremake()) {
            sleepUntil(() -> cannotLightFire, 1500);
        }
        if (!cannotLightFire && isFiremake()) {
            sleepUntil(() -> Rs2Player.waitForXpDrop(Skill.FIREMAKING, 40000), 40000);
        }
    }

    private WorldPoint fireSpot(int distance) {
        List<WorldPoint> worldPoints = Rs2Tile.getWalkableTilesAroundPlayer(distance);
        WorldPoint playerLocation = Rs2Player.getWorldLocation();

        // Create a map to group tiles by their distance from the player
        Map<Integer, WorldPoint> distanceMap = new HashMap<>();

        for (WorldPoint walkablePoint : worldPoints) {
            if (rs2TileObjectCache.query().where(x -> x.getWorldLocation().equals(walkablePoint)).nearest(distance) == null) {
                int tileDistance = playerLocation.distanceTo(walkablePoint);
                distanceMap.putIfAbsent(tileDistance, walkablePoint);
            }
        }

        // Find the minimum distance that has walkable points
        Optional<Integer> minDistanceOpt = distanceMap.keySet().stream().min(Integer::compare);

        if (minDistanceOpt.isPresent()) {
            return distanceMap.get(minDistanceOpt.get());
        }

        // Recursively increase the distance if no valid point is found
        return fireSpot(distance + 1);
    }

    private boolean isFiremake() {
        if (cannotLightFire) return false;
        return Rs2Player.isAnimating(1800) && BURNING_ANIMATION_IDS.contains(Rs2Player.getLastAnimationID());
    }

    private void walkBack(AutoWoodcuttingPlusConfig config) {
        Rs2Walker.walkTo(new WorldPoint(getReturnPoint(config).getX() - Rs2Random.between(-1, 1), getReturnPoint(config).getY() - Rs2Random.between(-1, 1), getReturnPoint(config).getPlane()));
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(getReturnPoint(config)) <= 4);
    }
    
    /**
     * determine if this workflow will bank items
     */
    private boolean willBankItems(AutoWoodcuttingPlusConfig config) {
        return config.primaryAction() == WoodcuttingPrimaryAction.BANK || 
               (config.primaryAction() == WoodcuttingPrimaryAction.FLETCH && 
                config.secondaryAction() == WoodcuttingSecondaryAction.BANK);
    }
    
    /**
     * handle fletching workflow with secondary actions
     */
    private boolean handleFletchingWorkflow(AutoWoodcuttingPlusConfig config) {
        // fletch logs in inventory
        if (!Rs2Fletching.hasKnife()) {
            log.info("Unable to find knife in inventory/equipped");
            switch (config.secondaryAction()) {
                case BANK:
                case STRING_AND_BANK:
                    if (!handleBanking(config)) {
                        walkBack(config);
                        return false;
                    }
                    break;
                case DROP:
                case STRING_AND_DROP:
                    log.info("Dropping items to find knife");
                    String [] itemNames = Arrays.stream(config.itemsToKeep().split(",")).map(String::trim).toArray(String[]::new);
                    // additional item to keep axe and log basket
                    itemNames = Arrays.copyOf(itemNames, itemNames.length + 1);
                    itemNames[itemNames.length - 1] = "axe";
                    if (Rs2Inventory.hasItem("log basket")) {
                        itemNames = Arrays.copyOf(itemNames, itemNames.length + 1);
                        itemNames[itemNames.length - 1] = "log basket";
                    }


                    Rs2Inventory.dropAllExcept(false, InteractOrder.COLUMN,itemNames );
                    break;
                case NONE:
                    break;
            }
            return !Rs2Inventory.isFull();
        }
        WoodcuttingTree treeType = getActiveTree();
        int logCount = Rs2Inventory.count(treeType.getLogID());
        if (logCount > 0) {
            // fletch the logs; on failure we return false below so the inventory step retries next tick
            boolean startFletchingSucces = Rs2Fletching.fletchItems(treeType.getLogID(), config.fletchingType().getContainsInventoryName(), "All");
            int fletchedItems = Rs2Inventory.getList(itemBounds -> itemBounds.getName().contains(config.fletchingType().getContainsInventoryName())).size();
            log.info("We fletched {} {} into {} of {} , success: {}", logCount, treeType, fletchedItems, config.fletchingType().getContainsInventoryName(), startFletchingSucces);
            if (!startFletchingSucces) {
                return false;
            }
            if (Rs2Inventory.count(treeType.getLogID())!=0){
                return false;
            }
        }

        // handle secondary action
        switch (config.secondaryAction()) {
            case BANK:
                if (!handleBanking(config)) return false;
                walkBack(config);
                break;
            case DROP:

                Rs2Fletching.dropFletchedItems(config.fletchingType().getContainsInventoryName());
                Rs2Inventory.waitForInventoryChanges(1800);
                break;
            case STRING_AND_DROP:
            case STRING_AND_BANK:
                if (Rs2Inventory.contains("bow string")) {
                    Rs2Fletching.stringBows(config.fletchingType().getContainsInventoryName());
                }
                if (config.secondaryAction() == WoodcuttingSecondaryAction.STRING_AND_BANK) {
                    if (!handleBanking(config)) return false;
                    walkBack(config);
                } else {
                    Rs2Fletching.dropFletchedItems(config.fletchingType().getContainsInventoryName());
                    Rs2Inventory.waitForInventoryChanges(1800);
                }
                break;
            case NONE:
                break;
        }


        return !Rs2Inventory.isFull();
    }

    public WoodcuttingTree getActiveTree() {
        return activeTree;
    }

    private void updateActiveTree(AutoWoodcuttingPlusConfig config) {
        WoodcuttingTree previousTree = activeTree;
        WoodcuttingTree resolvedTree;
        ResourceLocationOption candidateLocation = null;
        boolean progressive = config.progressiveMode();

        if (progressive) {
            int woodcuttingLevel = getRealSkillLevel(Skill.WOODCUTTING);
            ProgressiveSelection selection = determineProgressiveSelection(woodcuttingLevel);
            resolvedTree = selection.getTree();
            candidateLocation = selection.getLocation();
        } else {
            resolvedTree = config.TREE();
        }

        if (resolvedTree == null) {
            resolvedTree = WoodcuttingTree.TREE;
        }

        activeTree = resolvedTree;

        if (progressive) {
            boolean keepExistingLocation = previousTree == resolvedTree && activeLocation != null && activeLocation.hasRequirements();

            if (!keepExistingLocation) {
                activeLocation = candidateLocation;
            }
        } else {
            activeLocation = null;
        }
    }

    private ProgressiveSelection determineProgressiveSelection(int woodcuttingLevel) {
        ProgressiveSelection bestSelection = null;

        for (WoodcuttingTree tree : PROGRESSIVE_TREE_ORDER) {
            if (woodcuttingLevel < tree.getWoodcuttingLevel()) {
                break;
            }

            ResourceLocationOption location = WoodcuttingTreeLocations.getBestAccessibleLocation(tree);
            if (location != null) {
                bestSelection = new ProgressiveSelection(tree, location);
            }
        }

        if (bestSelection != null) {
            return bestSelection;
        }

        ResourceLocationOption fallbackLocation = WoodcuttingTreeLocations.getBestAccessibleLocation(WoodcuttingTree.TREE);
        return new ProgressiveSelection(WoodcuttingTree.TREE, fallbackLocation);
    }

    private static class ProgressiveSelection {
        private final WoodcuttingTree tree;
        private final ResourceLocationOption location;

        private ProgressiveSelection(WoodcuttingTree tree, ResourceLocationOption location) {
            this.tree = tree;
            this.location = location;
        }

        public WoodcuttingTree getTree() {
            return tree;
        }

        public ResourceLocationOption getLocation() {
            return location;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (Rs2DepositBox.isOpen()) Rs2DepositBox.closeDepositBox();
        currentLogBasketCount = -1;
        Rs2Fletching.stopFletchingWhileMoving();
        Rs2Walker.setTarget(null);
        returnPoint = null;
        initialPlayerLocation = null;
        hasAutoHopMessageShown = false;
        Rs2Antiban.resetAntibanSettings();
        activeLocation = null;
    }
}
