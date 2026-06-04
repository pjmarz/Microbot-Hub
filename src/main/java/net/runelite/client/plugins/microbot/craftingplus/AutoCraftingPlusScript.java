package net.runelite.client.plugins.microbot.craftingplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.api.coords.WorldPoint;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

/**
 * AutoCraftingPlus v0.1.1 - leather crafting + gem cutting, on a bank-and-do loop with the Plus
 * layer (stop conditions, target level + clean shutdown, overlay/pause, speed mode, league mode).
 *
 * <p>Leather (v0.1.1) forks the make-X interaction proven by the base DragonLeatherScript
 * (use needle + leather -&gt; make-X interface widget 17694733 -&gt; select the product). Gem
 * cutting (v0.1.0) forks GemsScript. Furnace jewellery is v0.2.0.</p>
 */
@Slf4j
public class AutoCraftingPlusScript extends Script {

    private static final int MAKE_INTERFACE_WIDGET = 17694733; // make-X interface root (from DragonLeatherScript)

    // Stats (read by AutoCraftingPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }

    public boolean run(AutoCraftingPlusConfig config) {
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.CRAFTING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.CRAFTING)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false;

        Microbot.enableAutoRunOn = true;
        Rs2Walker.disableTeleports = true; // keep banking on foot (the RC v0.1.1 lesson)
        Rs2Antiban.resetAntibanSettings();
        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (Microbot.pauseAllScripts.get()) {
                    Microbot.status = "[PAUSED]";
                    return;
                }

                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoCraftingPlus: reached stopAfterMinutes. Shutting down.");
                    super.shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0) {
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(Skill.CRAFTING)).orElse(startSkillXp);
                    if (currentXp - startSkillXp >= config.stopAfterXp()) {
                        Microbot.log("AutoCraftingPlus: reached stopAfterXp. Shutting down.");
                        super.shutdown();
                        return;
                    }
                }
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.CRAFTING)).orElse(startSkillLevel);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoCraftingPlus: reached targetLevel (" + currentLevel
                                + "). Banking then shutting down.");
                        shutdownAfterCleanup = true;
                    }
                }

                if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                    int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length - 1)]);
                }

                if (Rs2AntibanSettings.actionCooldownActive) return;

                switch (config.activity()) {
                    case LEATHER:
                        runLeather(config);
                        break;
                    case GEM_CUTTING:
                        runGems(config);
                        break;
                    case JEWELLERY:
                        runJewellery(config);
                        break;
                }

                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            } catch (Exception ex) {
                Microbot.logStackTrace("AutoCraftingPlusScript", ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    // --- Leather ---

    private void runLeather(AutoCraftingPlusConfig config) {
        final Leather product = config.leatherProduct();
        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, product.getLevelRequired())) {
            Microbot.showMessage("Crafting level too low to make " + product.getProductName() + ".");
            super.shutdown();
            return;
        }

        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItem(product.getMaterialId())
                || !Rs2Inventory.hasItem(ItemID.NEEDLE)
                || !Rs2Inventory.hasItem(ItemID.THREAD);

        if (needBank) {
            handleLeatherBanking(product);
        } else {
            craftLeather(product);
        }
    }

    private void handleLeatherBanking(Leather product) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit crafted products (keep needle/thread/material).
        Rs2Bank.depositAllExcept(ItemID.NEEDLE, ItemID.THREAD, product.getMaterialId());
        sleep(400);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        if (!Rs2Inventory.hasItem(ItemID.NEEDLE)) {
            if (!Rs2Bank.hasItem(ItemID.NEEDLE)) {
                Microbot.showMessage("No needle in the bank!");
                super.shutdown();
                return;
            }
            Rs2Bank.withdrawItem(true, ItemID.NEEDLE);
        }
        if (!Rs2Inventory.hasItem(ItemID.THREAD)) {
            if (!Rs2Bank.hasItem(ItemID.THREAD)) {
                Microbot.showMessage("No thread in the bank!");
                super.shutdown();
                return;
            }
            Rs2Bank.withdrawX(true, ItemID.THREAD, 10); // bounded so leather still fits
        }

        if (!Rs2Bank.hasItem(product.getMaterialId())) {
            Microbot.showMessage("Out of " + product.getMaterialName() + " in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing leather";
        Rs2Bank.withdrawAll(product.getMaterialId());
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    private void craftLeather(Leather product) {
        Microbot.status = "Crafting " + product.getProductName();
        Rs2Inventory.use(ItemID.NEEDLE);
        Rs2Inventory.use(product.getMaterialId());
        if (sleepUntil(() -> Rs2Widget.getWidget(MAKE_INTERFACE_WIDGET) != null, 5000)) {
            Rs2Widget.clickWidget(product.getProductName(), true);
            sleep(1800);
            // Craft until the leather is used up OR thread runs out (v0.1.2: without the thread
            // check this blocked the full timeout while leftover leather sat un-sewable, looking
            // stopped; now it breaks immediately and the next tick banks to restock thread).
            sleepUntil(() -> !Rs2Inventory.hasItem(product.getMaterialId())
                    || !Rs2Inventory.hasItem(ItemID.THREAD), 60000);
            actionsCompleted++;
        }
    }

    // --- Gem cutting (v0.1.0, forked from GemsScript) ---

    private void runGems(AutoCraftingPlusConfig config) {
        int requiredLevel = config.gemType().getLevelRequired();
        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, requiredLevel)) {
            Microbot.showMessage("Crafting level too low to cut " + config.gemType().getName() + ".");
            super.shutdown();
            return;
        }

        final String gemName = config.gemType().getName();
        final String uncutGemName = "uncut " + gemName;

        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItem(uncutGemName)
                || !Rs2Inventory.hasItem("chisel");

        if (needBank) {
            handleGemBanking(gemName, uncutGemName);
        } else {
            cutGems(uncutGemName);
        }
    }

    private void handleGemBanking(String gemName, String uncutGemName) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        Rs2Bank.depositAll(gemName);
        Rs2Bank.depositAll("crushed gem");
        sleepUntil(() -> !Rs2Inventory.hasItem(gemName) && !Rs2Inventory.hasItem("crushed gem"), 3000);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        if (!Rs2Bank.hasItem(uncutGemName)) {
            Microbot.showMessage("Out of " + uncutGemName + " in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing gems";
        Rs2Bank.withdrawItem(true, "chisel");
        Rs2Bank.withdrawAll(true, uncutGemName);
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    private void cutGems(String uncutGemName) {
        Microbot.status = "Cutting gems";
        Rs2Inventory.use("chisel");
        Rs2Inventory.use(uncutGemName);
        sleep(600);
        Rs2Keyboard.keyPress(KeyEvent.VK_SPACE); // confirm "make all" on the quantity dialog
        sleep(1800);
        sleepUntil(() -> !Rs2Inventory.hasItem(uncutGemName), 60000);
        actionsCompleted++;
    }

    // --- Furnace jewellery (v0.2.0, forked from crafting/jewelry JewelryScript) ---

    private void runJewellery(AutoCraftingPlusConfig config) {
        final Jewelry jewelry = config.jewellery();
        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, jewelry.getLevelRequired())) {
            Microbot.showMessage("Crafting level too low to make " + jewelry.getItemName() + ".");
            super.shutdown();
            return;
        }

        final boolean needsGem = jewelry.getGem() != Gem.NONE;
        final int barId = jewelry.getJewelryType().getItemID();
        final int mouldId = jewelry.getToolItemID();
        final int cutGemId = needsGem ? jewelry.getGem().getCutItemID() : -1;

        boolean haveInputs = Rs2Inventory.hasItem(barId)
                && Rs2Inventory.hasItem(mouldId)
                && (!needsGem || Rs2Inventory.hasItem(cutGemId));

        if (shutdownAfterCleanup || !haveInputs) {
            handleJewelleryBanking(config, jewelry, needsGem, barId, mouldId, cutGemId);
        } else {
            craftJewellery(config, jewelry, barId);
        }
    }

    private void handleJewelleryBanking(AutoCraftingPlusConfig config, Jewelry jewelry,
                                        boolean needsGem, int barId, int mouldId, int cutGemId) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        CraftingLocation loc = config.furnaceLocation();
        boolean isBankOpen = (loc.getBankLocation() != null)
                ? Rs2Bank.walkToBankAndUseBank(loc.getBankLocation())
                : Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit everything except the mould (crafted jewellery + leftovers), then restock.
        Rs2Bank.depositAllExcept(mouldId);
        sleep(400);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        if (!Rs2Inventory.hasItem(mouldId)) {
            if (!Rs2Bank.hasItem(mouldId)) {
                Microbot.showMessage("No mould in the bank for " + jewelry.getItemName() + ".");
                super.shutdown();
                return;
            }
            Rs2Bank.withdrawItem(true, mouldId);
            sleep(300);
        }

        // 1 bar (+1 cut gem) per piece; leave a slot for the mould.
        int maxPerTrip = needsGem ? 13 : 27;
        int bars = Rs2Bank.count(barId);
        int amount = needsGem
                ? Math.min(Math.min(maxPerTrip, bars), Rs2Bank.count(cutGemId))
                : Math.min(maxPerTrip, bars);

        if (amount <= 0) {
            Microbot.showMessage(needsGem
                    ? "Out of " + jewelry.getJewelryType() + " bars or cut " + jewelry.getGem().getCutItemName() + " in the bank!"
                    : "Out of " + jewelry.getJewelryType() + " bars in the bank!");
            super.shutdown();
            return;
        }

        Microbot.status = "Withdrawing materials";
        if (needsGem) {
            Rs2Bank.withdrawX(cutGemId, amount);
            sleep(300);
        }
        Rs2Bank.withdrawX(barId, amount);
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    private void craftJewellery(AutoCraftingPlusConfig config, Jewelry jewelry, int barId) {
        Microbot.status = "Crafting " + jewelry.getItemName();
        WorldPoint furnaceLoc = config.furnaceLocation().getFurnaceLocation();
        WorldPoint anchor = furnaceLoc != null ? furnaceLoc : Rs2Player.getWorldLocation();

        Rs2TileObjectModel furnace = Microbot.getRs2TileObjectCache().query()
                .withName("Furnace")
                .where(o -> Rs2GameObject.hasAction(o, "Smelt"))
                .nearest(anchor, 20);

        if (furnace == null) {
            if (furnaceLoc != null && !Rs2Player.isMoving()) {
                Rs2Walker.walkTo(furnaceLoc);
            }
            return;
        }

        if (!Rs2Camera.isTileOnScreen(furnace.getLocalLocation())) {
            Rs2Camera.turnTo(furnace.getLocalLocation());
            return;
        }

        furnace.click("smelt");
        if (sleepUntil(() -> Rs2Widget.isGoldCraftingWidgetOpen() || Rs2Widget.isSilverCraftingWidgetOpen(), 10000)) {
            Rs2Widget.clickWidget(jewelry.getItemName());
            sleep(1800);
            // The gold/silver interface makes the whole batch; wait until the bars are consumed.
            sleepUntil(() -> !Rs2Inventory.hasItem(barId), 60000);
            actionsCompleted++;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Walker.disableTeleports = false;
        Rs2Antiban.resetAntibanSettings();
    }
}
