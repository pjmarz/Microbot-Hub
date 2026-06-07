package net.runelite.client.plugins.microbot.cookingplus;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingAreaType;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingItem;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingLocation;
import net.runelite.client.plugins.microbot.cookingplus.enums.State;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

/**
 * AutoCookingPlus.
 *
 * <p>Forks the base AutoCooking loop (useItemOnObject -> production widget -> space to cook all ->
 * full-bank deposit/withdraw -> drop burnt) and adds the Plus layer:
 * <ul>
 *   <li>Stop conditions (minutes / XP / target level / cooked count) checked each tick.</li>
 *   <li>RESETTING + shutdownAfterCleanup for a clean stop (one bank/drop pass first).</li>
 *   <li>Pause via the shared {@link Microbot#pauseAllScripts} flag (overlay button).</li>
 *   <li>Speed mode (antiban off) + League mode (arrow-key anti-AFK).</li>
 *   <li>Progressive food selection by Cooking level requirement.</li>
 *   <li>Accurate cooked counter via Cooking XP-drop detection.</li>
 * </ul>
 * 600ms loop. FULL_BANK only (cooking must withdraw raw food; deposit boxes cannot).
 */
public class AutoCookingPlusScript extends Script {

    private static final String CLEANUP_COMPLETE_LOG = "AutoCookingPlus: cleanup complete. Shutting down.";

    private AutoCookingPlusConfig config;
    private State state = State.COOKING;
    private boolean init = true;
    private CookingItem activeItem;
    private CookingLocation location;

    // Plus layer: runtime stats (read by the overlay) + accurate cooked counter.
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int itemsCooked = 0;
    private int lastCookingXp = 0;

    // Set when a stop condition fires; gates one bank/drop cleanup pass before shutdown.
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getItemsCooked() { return itemsCooked; }
    public CookingItem getActiveItem() { return activeItem; }

    public boolean run(AutoCookingPlusConfig config) {
        this.config = config;
        Microbot.enableAutoRunOn = false;

        this.activeItem = config.cookingItem();
        this.location = null;
        this.state = State.COOKING;
        this.init = true;
        initialPlayerLocation = null;

        // Seed stats from the client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.COOKING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.COOKING)).orElse(1);
        lastCookingXp = startSkillXp;
        itemsCooked = 0;
        shutdownAfterCleanup = false;

        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyCookingSetup();
        Rs2Antiban.setActivity(Activity.GENERAL_COOKING);

        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::loop, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void loop() {
        try {
            if (!Microbot.isLoggedIn()) return;
            if (!super.run()) return;

            // Pause: shared global flag toggled by the overlay Pause button. Stats keep
            // accumulating; unpause resumes the flow.
            if (Microbot.pauseAllScripts.get()) {
                Microbot.status = "[PAUSED]";
                return;
            }

            // Accurate cooked counter: one item per Cooking XP increase. The 600ms tick is well
            // below cook cadence, so one increment per XP drop is exact. Drives the overlay stat,
            // the stopAfterCooked target, and (reused) the stopAfterXp check.
            int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getSkillExperience(Skill.COOKING)).orElse(lastCookingXp);
            if (currentXp > lastCookingXp) {
                itemsCooked++;
                lastCookingXp = currentXp;
            }

            // League mode: periodic key press resets the idle-logout timer.
            if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length)]);
            }

            // Stop conditions -> shutdownAfterCleanup. Once set, run one bank/drop pass (so the
            // food ends up banked/dropped, not abandoned) via RESETTING then shut down.
            if (!shutdownAfterCleanup) {
                checkStopConditions(currentXp);
            }
            if (shutdownAfterCleanup) {
                if (Rs2Inventory.isEmpty()) {
                    Microbot.log(CLEANUP_COMPLETE_LOG);
                    Rs2Bank.closeBank();
                    super.shutdown();
                    return;
                }
                state = State.RESETTING;
            }

            if (Rs2AntibanSettings.actionCooldownActive) return;

            if (init) {
                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }

                if (!resolveActiveItemAndLocation()) {
                    return; // resolution failed and the script was shut down.
                }

                getState();
            }

            if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

            switch (state) {
                case COOKING:
                    handleCooking();
                    break;
                case DROPPING_BURNT:
                    handleDroppingBurnt();
                    break;
                case BANKING:
                    handleBanking();
                    break;
                case WALKING:
                    handleWalking();
                    break;
                case RESETTING:
                    handleResetting();
                    break;
            }
        } catch (Exception ex) {
            Microbot.logStackTrace("AutoCookingPlusScript", ex);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (Rs2Bank.isOpen()) Rs2Bank.closeBank();
        Rs2Antiban.resetAntibanSettings();
    }

    // --- Active item + location resolution ---

    /**
     * Resolve the food to cook (progressive vs manual) and the location (nearest vs configured).
     * Runs once at init. Afterwards handleBanking() re-evaluates activeItem (and refreshes the
     * nearest location) on each bank trip; this method is not re-run.
     * Returns false if resolution failed and the script was shut down.
     */
    private boolean resolveActiveItemAndLocation() {
        // When a fixed location is configured, constrain progressive selection to food that location
        // can actually cook so it never hands us an incompatible item (e.g. a RANGE-only pie for a fire).
        CookingAreaType areaConstraint = config.useNearestLocation()
                ? null
                : config.cookingLocation().getCookingAreaType();

        if (config.progressiveCook()) {
            CookingItem best = chooseProgressiveItem(areaConstraint);
            if (best != null) {
                activeItem = best;
            }
        } else {
            activeItem = config.cookingItem();
        }

        if (config.useNearestLocation()) {
            location = CookingLocation.findNearestCookingLocation(activeItem);
        } else {
            location = config.cookingLocation();
            if (activeItem.getCookingAreaType() != CookingAreaType.BOTH
                    && location.getCookingAreaType() != activeItem.getCookingAreaType()) {
                Microbot.showMessage("Cooking area does not match the item's cooking area");
                shutdown();
                return false;
            }
        }

        if (location == null) {
            Microbot.showMessage("No suitable cooking location found");
            shutdown();
            return false;
        }
        return true;
    }

    /**
     * Highest-level-requirement food whose requirements the player meets. Ordering by
     * levelRequired gives the F2P-first progressive ladder the plan asks for. When areaConstraint is
     * non-null (a fixed location is configured), food the location cannot cook is skipped.
     */
    private CookingItem chooseProgressiveItem(CookingAreaType areaConstraint) {
        CookingItem best = null;
        for (CookingItem item : CookingItem.values()) {
            if (!item.hasRequirements()) continue;
            if (areaConstraint != null
                    && item.getCookingAreaType() != CookingAreaType.BOTH
                    && item.getCookingAreaType() != areaConstraint) continue;
            if (best == null || item.getLevelRequired() > best.getLevelRequired()) {
                best = item;
            }
        }
        return best;
    }

    /**
     * Progressive choice that also requires the food to be in the bank, so we never pick a food the
     * player cannot withdraw. Used at the bank (after depositAll, all food is in the bank). When
     * areaConstraint is non-null (a fixed location is configured), food the location cannot cook is skipped.
     */
    private CookingItem chooseProgressiveItemInBank(CookingAreaType areaConstraint) {
        CookingItem best = null;
        for (CookingItem item : CookingItem.values()) {
            if (!item.hasRequirements()) continue;
            if (areaConstraint != null
                    && item.getCookingAreaType() != CookingAreaType.BOTH
                    && item.getCookingAreaType() != areaConstraint) continue;
            if (!hasRawItemInBank(item)) continue;
            if (best == null || item.getLevelRequired() > best.getLevelRequired()) {
                best = item;
            }
        }
        return best;
    }

    // --- State determination ---

    private void getState() {
        if (!hasRawItem(activeItem)) {
            if (config.shouldDropBurntItems() && hasBurntItem(activeItem)) {
                state = State.DROPPING_BURNT;
                init = false;
                return;
            }
            state = State.BANKING;
            init = false;
            return;
        }

        if (!isNearCookingLocation(location, 4)) {
            state = State.WALKING;
            init = false;
            return;
        }

        state = State.COOKING;
        init = false;
    }

    // --- COOKING ---

    private void handleCooking() {
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            return;
        }
        if (!activeItem.hasRequirements()) {
            Microbot.showMessage("You do not meet the requirements to cook this item");
            shutdown();
            return;
        }

        Rs2TileObjectModel cookingObject = Microbot.getRs2TileObjectCache().query()
                .withId(location.getCookingObjectID()).nearestOnClientThread();
        if (cookingObject == null) {
            cookingObject = Microbot.getRs2TileObjectCache().query()
                    .where(o -> o.getWorldLocation().equals(location.getCookingObjectWorldPoint()))
                    .nearestOnClientThread();
        }

        if (cookingObject == null) {
            state = State.WALKING;
            return;
        }

        if (!Rs2Camera.isTileOnScreen(cookingObject.getLocalLocation())) {
            Rs2Camera.turnTo(cookingObject.getLocalLocation());
            return;
        }

        Rs2Inventory.useItemOnObject(activeItem.getRawItemID(), cookingObject.getId());

        boolean productionWidgetOpen = Rs2Widget.isProductionWidgetOpen();
        if (!productionWidgetOpen) {
            productionWidgetOpen = sleepUntilTrue(Rs2Widget::isProductionWidgetOpen, 200, 12000);
        }
        if (!productionWidgetOpen) {
            return;
        }

        sleepUntilTrue(() -> !Rs2Player.isMoving(), 200, 8000);

        Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
        Microbot.status = "Cooking " + activeItem.getRawItemName();

        Rs2Antiban.actionCooldown();
        Rs2Antiban.takeMicroBreakByChance();

        // Wait until the cooking animation kicks in (-1 == idle / no animation).
        sleepUntil(() -> (Rs2Player.getAnimation() != -1));
        sleepUntilTrue(() -> (!hasRawItem(activeItem) && !Rs2Player.isAnimating(3500))
                || Rs2Dialogue.isInDialogue() || Rs2Player.isMoving(), 500, 150000);

        if (hasRawItem(activeItem)) {
            return;
        }

        if (config.shouldDropBurntItems() && hasBurntItem(activeItem)) {
            state = State.DROPPING_BURNT;
            return;
        }

        state = State.BANKING;
    }

    // --- DROPPING_BURNT ---

    private void handleDroppingBurnt() {
        Microbot.status = "Dropping " + activeItem.getBurntItemName();
        Rs2Inventory.dropAll(item -> item.getName().equalsIgnoreCase(activeItem.getBurntItemName()), config.getDropOrder());
        sleepUntilTrue(() -> !hasBurntItem(activeItem), 500, 150000);
        state = State.BANKING;
    }

    // --- BANKING (FULL_BANK only) ---

    private void handleBanking() {
        if (!openNearestBank()) {
            return;
        }

        Rs2Bank.depositAll();
        Rs2Inventory.waitForInventoryChanges(1800);

        // Re-evaluate progressive food now that the bank is open. Bank-aware so we never pick a food
        // the player cannot withdraw (after depositAll, all food sits in the bank).
        if (config.progressiveCook()) {
            CookingItem best = chooseProgressiveItemInBank(config.useNearestLocation()
                    ? null
                    : config.cookingLocation().getCookingAreaType());
            if (best != null) {
                activeItem = best;
                // Food may have upgraded; refresh the nearest cooking spot to match it.
                if (config.useNearestLocation()) {
                    CookingLocation nearest = CookingLocation.findNearestCookingLocation(activeItem);
                    if (nearest != null) {
                        location = nearest;
                    }
                }
            }
        }

        if (!hasRawItemInBank(activeItem)) {
            Microbot.showMessage("No raw food item found in bank");
            shutdown();
            return;
        }

        if (activeItem == CookingItem.GIANT_SEAWEED) {
            Rs2Bank.withdrawX(activeItem.getRawItemID(), 4);
        } else {
            Rs2Bank.withdrawAll(activeItem.getRawItemID());
        }
        Rs2Inventory.waitForInventoryChanges(1800);

        Rs2Bank.closeBank();
        state = State.WALKING;
    }

    // --- WALKING ---

    private void handleWalking() {
        boolean hasRawItems = hasRawItem(activeItem);
        int distanceToCookingObject = Rs2Player.getWorldLocation().distanceTo(location.getCookingObjectWorldPoint());

        if (hasRawItems && distanceToCookingObject <= 20) {
            state = State.COOKING;
            sleepUntil(() -> !Rs2Player.isMoving());
            return;
        }

        if (!isNearCookingLocation(location, 10)) {
            boolean walkTo = Rs2Walker.walkTo(location.getCookingObjectWorldPoint(), 2);
            if (!walkTo) return;
        } else if (!isNearCookingLocation(location, 2)) {
            Rs2Walker.walkFastCanvas(location.getCookingObjectWorldPoint());
        }

        if (hasRawItems) {
            state = State.COOKING;
        } else {
            state = State.BANKING;
        }
    }

    // --- RESETTING (clean stop) ---

    private void handleResetting() {
        // Burnt food first (if dropping), then bank everything else, then shut down.
        if (config.shouldDropBurntItems() && hasBurntItem(activeItem)) {
            Microbot.status = "Cleaning up: dropping burnt food";
            Rs2Inventory.dropAll(item -> item.getName().equalsIgnoreCase(activeItem.getBurntItemName()), config.getDropOrder());
            sleepUntilTrue(() -> !hasBurntItem(activeItem), 500, 150000);
        }

        if (Rs2Inventory.isEmpty()) {
            Microbot.log(CLEANUP_COMPLETE_LOG);
            Rs2Bank.closeBank();
            super.shutdown();
            return;
        }

        if (!openNearestBank()) {
            return;
        }
        Rs2Bank.depositAll();
        sleepUntil(() -> Rs2Inventory.isEmpty(), 3000);
        Rs2Bank.closeBank();
        Microbot.log(CLEANUP_COMPLETE_LOG);
        super.shutdown();
    }

    // --- Helpers ---

    private boolean openNearestBank() {
        if (Rs2Bank.isOpen()) {
            return true;
        }
        // If a bank is already in reach, open it in place; otherwise walk to the nearest one.
        if (Rs2Bank.isNearBank(8)) {
            boolean isBankOpen = Rs2Bank.openBank();
            if (isBankOpen && Rs2Bank.isOpen()) {
                sleepUntil(() -> !Rs2Player.isMoving());
                return true;
            }
        }
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        return isBankOpen && Rs2Bank.isOpen();
    }

    private void checkStopConditions(int currentXp) {
        if (config.stopAfterMinutes() > 0
                && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
            Microbot.log("AutoCookingPlus: reached stopAfterMinutes (" + config.stopAfterMinutes()
                    + " min). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
            return;
        }
        if (config.stopAfterXp() > 0 && currentXp - startSkillXp >= config.stopAfterXp()) {
            Microbot.log("AutoCookingPlus: reached stopAfterXp (" + (currentXp - startSkillXp)
                    + " XP). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
            return;
        }
        if (config.targetLevel() > 0) {
            int level = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getRealSkillLevel(Skill.COOKING)).orElse(startSkillLevel);
            if (level >= config.targetLevel()) {
                Microbot.log("AutoCookingPlus: reached target level (" + level + " >= "
                        + config.targetLevel() + "). Cleaning up then shutting down.");
                shutdownAfterCleanup = true;
                return;
            }
        }
        if (config.stopAfterCooked() > 0 && itemsCooked >= config.stopAfterCooked()) {
            Microbot.log("AutoCookingPlus: reached stopAfterCooked (" + itemsCooked + " >= "
                    + config.stopAfterCooked() + "). Cleaning up then shutting down.");
            shutdownAfterCleanup = true;
        }
    }

    private boolean isNearCookingLocation(CookingLocation location, int distance) {
        return Rs2Player.getWorldLocation().distanceTo(location.getCookingObjectWorldPoint()) <= distance
                && !Rs2Player.isMoving();
    }

    private boolean hasRawItem(CookingItem cookingItem) {
        return Rs2Inventory.hasItem(cookingItem.getRawItemID());
    }

    private boolean hasRawItemInBank(CookingItem cookingItem) {
        return Rs2Bank.hasBankItem(cookingItem.getRawItemID(), 1);
    }

    private boolean hasBurntItem(CookingItem cookingItem) {
        // burntItemID == 0 is the "no burnt variant" sentinel (e.g. giant seaweed).
        return cookingItem.getBurntItemID() > 0 && Rs2Inventory.hasItem(cookingItem.getBurntItemID());
    }
}
