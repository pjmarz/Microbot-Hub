package net.runelite.client.plugins.microbot.autofishingplus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.autofishingplus.enums.AutoFishingPlusState;
import net.runelite.client.plugins.microbot.autofishingplus.enums.BankingStrategy;
import net.runelite.client.plugins.microbot.autofishingplus.enums.Fish;
import net.runelite.client.plugins.microbot.autofishingplus.enums.FishingPlusLocation;
import net.runelite.client.plugins.microbot.autofishingplus.enums.HarpoonType;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.awt.event.KeyEvent;

@Slf4j
public class AutoFishingPlusScript extends Script {

    private AutoFishingPlusConfig config;
    private Fish selectedFish;
    @Getter
    private HarpoonType selectedHarpoon;
    private FishingPlusLocation selectedLocation = FishingPlusLocation.AUTO;
    @Getter
    private AutoFishingPlusState currentState = AutoFishingPlusState.IDLE;
    private WorldPoint fishingLocation;
    private String fishAction = "";

    // Plus layer: runtime stats (read by the overlay) + accurate fish counter.
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int fishCaught = 0;
    private int lastFishingXp = 0;
    // Set when a stop condition fires; gates one deposit/drop cleanup pass before shutdown.
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getFishCaught() { return fishCaught; }

    public boolean run(AutoFishingPlusConfig config) {
        this.config = config;
        this.selectedFish = config.fishToCatch();
        this.selectedHarpoon = config.harpoonSpec();
        this.selectedLocation = config.fishingLocation() != null ? config.fishingLocation() : FishingPlusLocation.AUTO;

        // Seed stats from the client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.FISHING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.FISHING)).orElse(1);
        lastFishingXp = startSkillXp;
        fishCaught = 0;
        shutdownAfterCleanup = false;

        // Pre-anchor the walk target for a named location so isAtFishingLocation()/handleTraveling
        // work immediately. AUTO leaves it null and resolves the nearest spot lazily.
        if (selectedLocation != FishingPlusLocation.AUTO && selectedLocation.getWorldPoint() != null) {
            fishingLocation = selectedLocation.getWorldPoint();
        }
        if (selectedLocation.isMembersOnly()
                && !Microbot.getClientThread().runOnClientThreadOptional(Rs2Player::isMember).orElse(true)) {
            Microbot.log("AutoFishingPlus: " + selectedLocation + " is members-only and this looks like an F2P world/account.");
        }

        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyFishingSetup();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::loop, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void loop() {
        try {
            if (!super.run() || !Microbot.isLoggedIn()) return;

            // Pause: shared global flag toggled by the overlay Pause button (mirrors AIO Fighter
            // and the rest of the Plus suite). Stats keep accumulating; unpause resumes the flow.
            if (Microbot.pauseAllScripts.get()) {
                Microbot.status = "[PAUSED]";
                return;
            }

            // Accurate fish counter: one catch per Fishing XP increase. The 600ms tick is well
            // below catch cadence, so one increment per XP drop is exact. Drives the overlay stat
            // and the stopAfterFish target; also reused for the stopAfterXp check.
            int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getSkillExperience(Skill.FISHING)).orElse(lastFishingXp);
            if (currentXp > lastFishingXp) {
                fishCaught++;
                lastFishingXp = currentXp;
            }

            // Stop conditions -> shutdownAfterCleanup. Once set, run one deposit/drop pass (so the
            // catch ends up banked/dropped, not abandoned) then shut down.
            if (!shutdownAfterCleanup) {
                checkStopConditions(currentXp);
            }
            if (shutdownAfterCleanup) {
                if (!hasCatchInInventory()) {
                    Microbot.log("AutoFishingPlus: cleanup complete. Shutting down.");
                    super.shutdown();
                    return;
                }
                currentState = cleanupState();
                dispatch(currentState);
                return;
            }

            // Normal flow: wait out the fishing animation / movement before acting.
            boolean isAnimation = sleepUntil(() -> Rs2Player.getAnimation() != -1, 2_000);
            if (isAnimation || (Rs2Player.isMoving() && currentState != AutoFishingPlusState.TRAVELING)) {
                return;
            }

            currentState = determineState();
            dispatch(currentState);
        } catch (Exception ex) {
            log.error("An unexpected error occurred", ex);
            currentState = AutoFishingPlusState.ERROR_RECOVERY;
        }
    }

    private void dispatch(AutoFishingPlusState state) {
        switch (state) {
            case GETTING_GEAR: handleGettingGear(); break;
            case TRAVELING: handleTraveling(); break;
            case FISHING: handleFishing(); break;
            case PROCESSING_FISH: handleProcessingFish(); break;
            case COOKING: handleCooking(); break;
            case DEPOSITING: handleDepositing(); break;
            case DROPPING: handleDropping(); break;
            case ERROR_RECOVERY: handleErrorRecovery(); break;
            default: break;
        }
    }

    private AutoFishingPlusState determineState() {
        if (Rs2Inventory.isFull()) {
            if (isSpecialFish(selectedFish)) return AutoFishingPlusState.PROCESSING_FISH;
            if (config.cookFish() && !getRawFishInInventory().isEmpty()) return AutoFishingPlusState.COOKING;
            return resolveBankingStrategy() == BankingStrategy.DROP
                    ? AutoFishingPlusState.DROPPING
                    : AutoFishingPlusState.DEPOSITING;
        }

        if (!hasRequiredGear()) {
            return AutoFishingPlusState.GETTING_GEAR;
        }

        if (!isAtFishingLocation()) {
            return AutoFishingPlusState.TRAVELING;
        }

        return AutoFishingPlusState.FISHING;
    }

    /**
     * Handle methods
     */
    private void handleTraveling() {
        WorldPoint target = resolveFishingAnchor();
        if (target != null) {
            fishingLocation = target;
            Microbot.status = "Walking to " + selectedLocation + "...";
            Rs2Walker.walkTo(target);
        }
    }

    private void handleFishing() {
        Rs2NpcModel fishingSpot = findNearestFishingSpot();
        if (fishingSpot == null) {
            // v0.2.3: no spot of the chosen fish is nearby. Don't busy-idle on the same null tick
            // after tick. Walk back toward the anchor so a drifted/depleted spot can come back into
            // range; if there is no anchor to walk to, stop with a clear status instead of spinning.
            WorldPoint anchor = resolveFishingAnchor();
            WorldPoint here = Rs2Player.getWorldLocation();
            if (anchor != null && here != null && here.distanceTo(anchor) > 5) {
                fishingLocation = anchor;
                Microbot.status = "No " + selectedFish + " spot in range -- walking to fishing spot";
                if (!Rs2Player.isMoving()) {
                    Rs2Walker.walkTo(anchor, 5);
                }
                return;
            }
            Microbot.status = "No " + selectedFish + " spot here -- check fish/location match";
            // At the anchor but still no spot: wait a beat for it to respawn rather than spamming
            // the cache query every tick. Not a walk target, so spinning here is unavoidable, but
            // the back-off keeps it from hammering.
            sleep(1_000, 2_000);
            return;
        }
        Microbot.status = "Fishing " + selectedFish + " @ " + selectedLocation;
        activateSpec();
        if (fishAction.isEmpty()) {
            fishAction = Rs2Npc.getAvailableAction(new net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel(fishingSpot.getNpc()), selectedFish.getActions());
        }
        if (!fishAction.isEmpty() && fishingSpot.click(fishAction)) {
            Rs2Player.waitForXpDrop(Skill.FISHING);
            Rs2Antiban.actionCooldown();
            Rs2Antiban.takeMicroBreakByChance();
        }
    }

    private void handleDropping() {
        Rs2Inventory.dropAll(selectedFish.getItemNames().toArray(new String[0]));
    }

    // we process fish that require special handling
    private void handleProcessingFish() {
        if (selectedFish == Fish.SACRED_EEL && Rs2Inventory.hasItem("Knife") && Rs2Inventory.hasItem("Sacred eel")) {
            Rs2Inventory.useItemOnObject(ItemID.KNIFE, ItemID.SNAKEBOSS_EEL);
        } else if (selectedFish == Fish.INFERNAL_EEL && Rs2Inventory.hasItem("Hammer") && Rs2Inventory.hasItem("Infernal eel")) {
            Rs2Inventory.useItemOnObject(ItemID.HAMMER, ItemID.INFERNAL_EEL);
        }
    }

    private void handleCooking() {
        TileObject fireOrRange = getNearbyFireOrRange();
        if (fireOrRange == null) {
            log.error("There is no fire nearby, shutdown");
            shutdown();
            return;
        }

        String fishToCook = getRawFishInInventory().stream().findFirst().orElse(null);

        if (fishToCook != null) {
            Rs2Inventory.useUnNotedItemOnObject(fishToCook, fireOrRange);
            if (sleepUntil(() -> Rs2Widget.findWidget("How many would you like to cook?", null) != null)) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                sleepUntil(() -> Rs2Player.getAnimation() != -1, 3_000);
            }
        }
    }

    private void handleGettingGear() {
        if (Rs2Bank.walkToBankAndUseBank()) {
            for (String tool : selectedFish.getMethod().getRequiredItems()) {
                if (!Rs2Inventory.hasItem(tool) && !Rs2Equipment.isWearing(tool)) {
                    withdrawAndEquipItem(tool);
                }
            }
            if (selectedHarpoon != HarpoonType.NONE) {
                String harpoonName = selectedHarpoon.getName();
                if (!Rs2Inventory.hasItem(harpoonName) && !Rs2Equipment.isWearing(harpoonName)) {
                    withdrawAndEquipItem(harpoonName);
                }
            }
            Rs2Bank.closeBank();
        }
    }

    private void handleDepositing() {
        // Named locations carry their own strategy. DEPOSIT_BOX walks to a specific box approach
        // tile (Corsair Cove); FULL_BANK uses the nearest bank booth.
        if (resolveBankingStrategy() == BankingStrategy.DEPOSIT_BOX) {
            handleDepositBox();
            return;
        }

        if (Rs2Bank.walkToBankAndUseBank()) {
            // v0.2.3: tool retention by name instead of slot-locks. The old path locked the tool
            // slots then called depositAll(), which only keeps the tool if the bank's "deposit
            // ignores inventory locks" varbit is 0 -- and the widget dance that set it could fail
            // on timing, dumping the whole inventory (the Draynor net-loss the journal flagged).
            // lockAllBySlot also returns false when every slot is already locked, so its sleepUntil
            // wrapper could spin to timeout even on success. depositAllExcept(names) keeps the tools
            // directly and is the same mechanism the deposit-box path already uses reliably.
            List<String> keep = toolsToKeep();

            // Empty the fish barrel (banks its catch) before depositing, and keep the barrel itself.
            if (Rs2Inventory.hasItem(ItemID.FISH_BARREL_CLOSED) || Rs2Inventory.hasItem(ItemID.FISH_BARREL_OPEN)) {
                Rs2Bank.emptyFishBarrel();
                keep.add("Fish barrel");
            }

            Rs2Bank.depositAllExcept(keep);
            sleepUntil(() -> !Rs2Inventory.isFull());
            Rs2Bank.closeBank();
        }
    }

    /**
     * Deposit-box banking for named locations (Corsair Cove). Walk to the box approach tile, then
     * deposit everything except the fishing tools. The return walk to the spot is handled by the
     * normal TRAVELING flow on the next tick.
     *
     * <p>Watch-point for live testing: the Corsair Cove box is a long (~112-tile) free overland
     * walk from the lobster pier. If Rs2Walker exhausts retries on that haul, harden here with
     * intermediate waypoints. No NPC/object interaction is required on this route.
     */
    private void handleDepositBox() {
        WorldPoint box = selectedLocation.getBankPoint();
        if (box != null) {
            WorldPoint here = Rs2Player.getWorldLocation();
            if (here == null || here.distanceTo(box) > 4) {
                Microbot.status = "Walking to deposit box...";
                if (!Rs2Player.isMoving()) {
                    Rs2Walker.walkTo(box, 4);
                }
                return;
            }
        }
        if (Rs2DepositBox.openDepositBox()) {
            sleepUntil(Rs2DepositBox::isOpen, 3_000);
            Rs2DepositBox.depositAllExcept(toolsToKeep(), false);
            sleepUntil(() -> !Rs2Inventory.isFull(), 3_000);
            Rs2DepositBox.closeDepositBox();
        }
    }

    private void handleErrorRecovery() {
        if (Rs2Bank.isOpen()) Rs2Bank.closeBank();
        if (Rs2DepositBox.isOpen()) Rs2DepositBox.closeDepositBox();
        fishAction = "";
        // Only forget a lazily-resolved AUTO anchor; keep a configured location's fixed anchor.
        if (selectedLocation == FishingPlusLocation.AUTO) {
            fishingLocation = null;
        }
    }

    /**
     * Helper methods
     */
    private Rs2NpcModel findNearestFishingSpot() {
        int[] spotIds = selectedFish.getFishingSpot();
        return Microbot.getRs2NpcCache().query()
                .where(npc -> Arrays.stream(spotIds).anyMatch(id -> npc.getId() == id))
                .nearestOnClientThread();
    }

    /** Tools to keep when depositing (method items + harpoon). */
    private List<String> toolsToKeep() {
        List<String> keep = new ArrayList<>(selectedFish.getMethod().getRequiredItems());
        if (selectedHarpoon != HarpoonType.NONE) {
            keep.add(selectedHarpoon.getName());
        }
        return keep;
    }

    /** True while any raw/cooked/burnt state of the selected fish is still in the pack. */
    private boolean hasCatchInInventory() {
        return selectedFish.getItemNames().stream().anyMatch(Rs2Inventory::hasItem);
    }

    /** Banking strategy: a named location's own strategy, else the AUTO Use-Bank toggle. */
    private BankingStrategy resolveBankingStrategy() {
        if (selectedLocation != null && selectedLocation != FishingPlusLocation.AUTO) {
            return selectedLocation.getBankingStrategy();
        }
        return config.useBank() ? BankingStrategy.FULL_BANK : BankingStrategy.DROP;
    }

    /** Cleanup state for a stop-condition shutdown: drop or deposit per strategy. */
    private AutoFishingPlusState cleanupState() {
        return resolveBankingStrategy() == BankingStrategy.DROP
                ? AutoFishingPlusState.DROPPING
                : AutoFishingPlusState.DEPOSITING;
    }

    /** Walk-to anchor: a named location's fixed point, else the nearest spot of the chosen fish. */
    private WorldPoint resolveFishingAnchor() {
        if (selectedLocation != null && selectedLocation != FishingPlusLocation.AUTO
                && selectedLocation.getWorldPoint() != null) {
            return selectedLocation.getWorldPoint();
        }
        if (fishingLocation == null) {
            return selectedFish.getClosestLocation(Rs2Player.getWorldLocation());
        }
        return fishingLocation;
    }

    private void checkStopConditions(int currentXp) {
        if (config.stopAfterMinutes() > 0
                && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
            Microbot.log("AutoFishingPlus: reached stopAfterMinutes (" + config.stopAfterMinutes()
                    + " min). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
            return;
        }
        if (config.stopAfterXp() > 0 && currentXp - startSkillXp >= config.stopAfterXp()) {
            Microbot.log("AutoFishingPlus: reached stopAfterXp (" + (currentXp - startSkillXp)
                    + " XP). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
            return;
        }
        if (config.targetLevel() > 0) {
            int level = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getRealSkillLevel(Skill.FISHING)).orElse(startSkillLevel);
            if (level >= config.targetLevel()) {
                Microbot.log("AutoFishingPlus: reached target level (" + level + " >= "
                        + config.targetLevel() + "). Cleaning up then shutting down.");
                shutdownAfterCleanup = true;
                return;
            }
        }
        if (config.stopAfterFish() > 0 && fishCaught >= config.stopAfterFish()) {
            Microbot.log("AutoFishingPlus: reached stopAfterFish (" + fishCaught + " >= "
                    + config.stopAfterFish() + "). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
        }
    }

    private List<String> getRawFishInInventory() {
        return selectedFish.getItemNames().stream()
                .filter(name -> name.startsWith("Raw") && Rs2Inventory.hasItem(name))
                .collect(Collectors.toList());
    }

    private TileObject getNearbyFireOrRange() {
        Integer[] fireIds = {
            ObjectID.FIRE,
            ObjectID.FORESTRY_FIRE,
            ObjectID.EAGLEPEAK_CAMPFIRE_TIDY,
            ObjectID.FIRE_COOK
        };
        return Rs2GameObject.getGameObject(fireIds, 15);
    }

    private boolean isSpecialFish(Fish fish) {
        return fish == Fish.SACRED_EEL || fish == Fish.INFERNAL_EEL;
    }

    private boolean hasRequiredGear() {
        boolean hasTools = selectedFish.getMethod().getRequiredItems().stream().allMatch(this::hasTool);

        if (selectedHarpoon != HarpoonType.NONE) {
            return hasTools && (Rs2Inventory.hasItem(selectedHarpoon.getName()) || Rs2Equipment.isWearing(selectedHarpoon.getName()));
        }
        return hasTools;
    }

    /**
     * True if the player has the given tool. "Harpoon" matches any harpoon variant (Dragon,
     * Crystal, Infernal, Barb-tail, etc.), so a special-attack harpoon on its own satisfies the
     * requirement without also carrying a plain harpoon.
     */
    private boolean hasTool(String tool) {
        if (tool.equalsIgnoreCase("Harpoon")) {
            return Rs2Inventory.contains(i -> i.getName() != null && i.getName().toLowerCase().contains("harpoon"))
                    || Rs2Equipment.isWearing(i -> i.getName() != null && i.getName().toLowerCase().contains("harpoon"));
        }
        return Rs2Inventory.hasItem(tool) || Rs2Equipment.isWearing(tool);
    }

    private boolean isAtFishingLocation() {
        if (fishingLocation == null) return false;
        return Rs2Player.getWorldLocation().distanceTo(fishingLocation) <= 5;
    }

    private void activateSpec() {
        if (selectedHarpoon != HarpoonType.NONE && Rs2Combat.getSpecEnergy() >= 100) {
            Rs2Combat.setSpecState(true, 1000);
            sleepUntil(() -> Rs2Combat.getSpecEnergy() < 100, 2000);
        }
    }

    private boolean withdrawAndEquipItem(String itemName) {
        if (Rs2Bank.hasItem(itemName)) {
            Rs2Bank.withdrawOne(itemName);
            if (sleepUntil(() -> Rs2Inventory.hasItem(itemName))) {
                if (itemName.equalsIgnoreCase("Hammer") || itemName.equalsIgnoreCase("Knife")) {
                    return true;
                }
                Rs2Inventory.wield(itemName);
                return sleepUntil(() -> Rs2Equipment.isWearing(itemName));
            }
        } else {
            log.warn("The object '{}' was not found in the bank", itemName);
        }
        return false;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (Rs2Bank.isOpen()) Rs2Bank.closeBank();
        if (Rs2DepositBox.isOpen()) Rs2DepositBox.closeDepositBox();
        Rs2Antiban.resetAntibanSettings();
        this.fishingLocation = null;
        this.fishAction = "";
    }
}
