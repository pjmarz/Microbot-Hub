package net.runelite.client.plugins.microbot.craftingplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
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
 * AutoCraftingPlus - leather crafting, gem cutting, furnace jewellery, amethyst cutting, and amulet
 * stringing, on a bank-and-do loop with the Plus layer (stop conditions, target level + clean
 * shutdown, overlay/pause, speed mode, league mode).
 *
 * <p>Leather uses the needle + leather -&gt; make-X interface to select the product. Gem cutting and
 * amethyst cutting use chisel on the material; amulet stringing uses a ball of wool. Furnace
 * jewellery casts at a furnace from a gold/silver bar (plus a cut gem for gem pieces).</p>
 */
@Slf4j
public class AutoCraftingPlusScript extends Script {

    // Root component of the make-X / production (skill-multi) dialog, used to detect it is open.
    private static final int MAKE_INTERFACE_WIDGET = InterfaceID.Skillmulti.BOTTOM_HOLDER;

    // Stats (read by AutoCraftingPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    private boolean shutdownAfterCleanup = false;

    // Resolved active picks (manual or progressive). Exactly one leather field is non-null at a time.
    private Leather activeSoftLeather = null;
    private DragonLeather activeDragonLeather = null;
    private Gems activeGem = null;
    private AmethystProduct activeAmethyst = null;
    private StringAmulet activeAmulet = null;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }
    /** The soft-leather piece currently being made, or null when d'hide/other is active. For the overlay. */
    public Leather getActiveSoftLeather() { return activeSoftLeather; }
    /** The dragonhide piece currently being made, or null when soft/other is active. For the overlay. */
    public DragonLeather getActiveDragonLeather() { return activeDragonLeather; }
    /** The gem currently being cut (resolves progressive mode), for the overlay. */
    public Gems getActiveGem() { return activeGem; }
    /** The amethyst product currently being cut, for the overlay. */
    public AmethystProduct getActiveAmethyst() { return activeAmethyst; }
    /** The amulet currently being strung, for the overlay. */
    public StringAmulet getActiveAmulet() { return activeAmulet; }

    public boolean run(AutoCraftingPlusConfig config) {
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.CRAFTING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.CRAFTING)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false;
        activeSoftLeather = null;
        activeDragonLeather = null;
        activeGem = null;
        activeAmethyst = null;
        activeAmulet = null;

        Microbot.enableAutoRunOn = true;
        Rs2Walker.disableTeleports = true; // keep banking on foot
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
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length)]);
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
                    case AMETHYST:
                        runAmethyst(config);
                        break;
                    case STRINGING:
                        runStringing(config);
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

    /**
     * Dispatches the LEATHER activity to either the soft-leather flow or the dragonhide flow.
     *
     * <p>Selection order:
     * <ol>
     *   <li><b>Progressive on</b>: pick the highest-level item (soft Leather L1-28 and DragonLeather
     *       L57-84 considered together, ordered by level) the player's Crafting level allows AND whose
     *       material is in the bank. Only re-evaluates while the bank is open (so we can read stock);
     *       otherwise it keeps the last pick for the in-progress trip. The chosen item's type then
     *       dispatches to the soft or d'hide flow below.</li>
     *   <li><b>Progressive off, Dragonhide set</b>: run the d'hide flow for that piece.</li>
     *   <li><b>Otherwise</b>: run the soft-leather flow for the manually picked Leather item.</li>
     * </ol></p>
     */
    private void runLeather(AutoCraftingPlusConfig config) {
        if (config.progressiveCraft()) {
            runProgressiveLeather(config);
            return;
        }

        if (config.dragonLeather() != DragonLeather.NONE) {
            activeSoftLeather = null;
            activeDragonLeather = config.dragonLeather();
            runDragonLeather(config.dragonLeather());
        } else {
            activeDragonLeather = null;
            activeSoftLeather = config.leatherProduct();
            runSoftLeather(config.leatherProduct());
        }
    }

    /**
     * Progressive LEATHER: hold the current pick to decide craft-vs-bank, but re-resolve the pick at
     * the bank (where stock is readable). If the inventory still has the active material we craft;
     * otherwise we bank, and the banking step re-evaluates and may switch soft &lt;-&gt; d'hide before
     * withdrawing. Dispatch to the soft or d'hide craft flow based on the resolved type.
     */
    private void runProgressiveLeather(AutoCraftingPlusConfig config) {
        // Seed a pick so the very first tick can travel to the bank.
        if (activeSoftLeather == null && activeDragonLeather == null) {
            activeSoftLeather = config.leatherProduct();
        }

        boolean haveActiveMaterial;
        if (activeDragonLeather != null) {
            haveActiveMaterial = Rs2Inventory.hasItemAmount(
                    activeDragonLeather.getLeatherId(), activeDragonLeather.getLeatherPerCraft());
        } else {
            haveActiveMaterial = Rs2Inventory.hasItem(activeSoftLeather.getMaterialId());
        }

        boolean needBank = shutdownAfterCleanup
                || !haveActiveMaterial
                || !Rs2Inventory.hasItem(ItemID.NEEDLE)
                || !Rs2Inventory.hasItem(ItemID.THREAD);

        if (needBank) {
            handleProgressiveLeatherBanking(config);
        } else if (activeDragonLeather != null) {
            craftDragonLeather(activeDragonLeather);
        } else {
            craftLeather(activeSoftLeather);
        }
    }

    /**
     * Banking for progressive leather. Walks to the bank, deposits crafted output and stale leftovers
     * (keeping tools and all leather types), re-evaluates the best pick with the bank open, then
     * withdraws needle + thread + the chosen leather. Stops cleanly if no leather is craftable.
     */
    private void handleProgressiveLeatherBanking(AutoCraftingPlusConfig config) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Keep tools; deposit everything else so leftover leather of the wrong type goes back.
        Rs2Bank.depositAllExcept(ItemID.NEEDLE, ItemID.THREAD);
        sleep(400);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        // Now the bank is open: pick the highest craftable item with banked materials.
        updateProgressiveLeather(config);

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
            Rs2Bank.withdrawX(true, ItemID.THREAD, 10);
        }

        int materialId;
        String materialLabel;
        if (activeDragonLeather != null) {
            materialId = activeDragonLeather.getLeatherId();
            materialLabel = "dragon leather for " + activeDragonLeather.getName();
        } else {
            materialId = activeSoftLeather.getMaterialId();
            materialLabel = activeSoftLeather.getMaterialName();
        }

        if (!Rs2Bank.hasItem(materialId)) {
            Microbot.showMessage("Out of " + materialLabel + " in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing leather";
        Rs2Bank.withdrawAll(materialId);
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    private void runSoftLeather(Leather product) {
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
            // Craft until the leather is used up OR thread runs out, so a thread shortage banks to
            // restock immediately instead of waiting out the full timeout on un-sewable leather.
            sleepUntil(() -> !Rs2Inventory.hasItem(product.getMaterialId())
                    || !Rs2Inventory.hasItem(ItemID.THREAD), 60000);
            actionsCompleted++;
        }
    }

    // --- Dragonhide leather ---

    private void runDragonLeather(DragonLeather product) {
        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, product.getLevelRequired())) {
            Microbot.showMessage("Crafting level too low to make " + product.getName() + ".");
            super.shutdown();
            return;
        }

        int perCraft = product.getLeatherPerCraft();
        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItemAmount(product.getLeatherId(), perCraft)
                || !Rs2Inventory.hasItem(ItemID.NEEDLE)
                || !Rs2Inventory.hasItem(ItemID.THREAD);

        if (needBank) {
            handleDragonLeatherBanking(product);
        } else {
            craftDragonLeather(product);
        }
    }

    private void handleDragonLeatherBanking(DragonLeather product) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit crafted pieces and leftovers, keeping needle + thread + the dragon leather.
        Rs2Bank.depositAllExcept(ItemID.NEEDLE, ItemID.THREAD, product.getLeatherId());
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

        if (!Rs2Bank.hasItem(product.getLeatherId())) {
            Microbot.showMessage("Out of dragon leather for " + product.getName() + " in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing dragon leather";
        Rs2Bank.withdrawAll(product.getLeatherId());
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    /**
     * Use needle on the dragon leather, wait for the make-X production dialog, then press the piece's
     * menuEntry digit to select it (body=1, vambraces=2, chaps=3 -- the number keys the dialog lists
     * for that colour). The batch then sews until the leather runs out (chaps use 2, body uses 3 per
     * piece) or thread runs out, at which point the next tick banks to restock.
     */
    private void craftDragonLeather(DragonLeather product) {
        Microbot.status = "Crafting " + product.getName();
        Rs2Inventory.use(ItemID.NEEDLE);
        Rs2Inventory.use(product.getLeatherId());
        if (sleepUntil(() -> Rs2Widget.getWidget(MAKE_INTERFACE_WIDGET) != null, 5000)) {
            Rs2Keyboard.keyPress(product.getMenuEntry());
            sleep(1800);
            int perCraft = product.getLeatherPerCraft();
            sleepUntil(() -> !Rs2Inventory.hasItemAmount(product.getLeatherId(), perCraft)
                    || !Rs2Inventory.hasItem(ItemID.THREAD), 60000);
            actionsCompleted++;
        }
    }

    // --- Progressive leather selection (soft Leather + DragonLeather, ordered by level) ---

    /**
     * Resolves the best progressive leather pick. Soft Leather (L1-28) and DragonLeather (L57-84) are
     * considered together; we choose the highest-level item the Crafting level allows whose material
     * is in the bank. Called only with the bank open (from handleProgressiveLeatherBanking) so stock
     * is readable. Sets exactly one of activeSoftLeather / activeDragonLeather (the other is nulled)
     * so the craft step dispatches to the right flow. If nothing is craftable it leaves the prior
     * pick unchanged, and the caller then reports the shortage and stops.
     */
    private void updateProgressiveLeather(AutoCraftingPlusConfig config) {
        int crafting = Rs2Player.getRealSkillLevel(Skill.CRAFTING);
        int bestLevel = -1;
        Leather bestSoft = null;
        DragonLeather bestDragon = null;

        for (Leather l : Leather.values()) {
            if (l.getLevelRequired() > crafting) continue;
            if (!Rs2Bank.hasItem(l.getMaterialId())) continue;
            if (l.getLevelRequired() > bestLevel) {
                bestLevel = l.getLevelRequired();
                bestSoft = l;
                bestDragon = null;
            }
        }
        for (DragonLeather d : DragonLeather.values()) {
            if (d == DragonLeather.NONE) continue;
            if (d.getLevelRequired() > crafting) continue;
            if (!Rs2Bank.hasItem(d.getLeatherId())) continue;
            if (d.getLevelRequired() > bestLevel) {
                bestLevel = d.getLevelRequired();
                bestDragon = d;
                bestSoft = null;
            }
        }

        if (bestDragon != null) {
            activeDragonLeather = bestDragon;
            activeSoftLeather = null;
        } else if (bestSoft != null) {
            activeSoftLeather = bestSoft;
            activeDragonLeather = null;
        }
        // If neither found (no materials banked), leave the prior pick; the caller reports + stops.
    }

    // --- Gem cutting ---

    private void runGems(AutoCraftingPlusConfig config) {
        updateActiveGem(config);
        final Gems gem = activeGem;

        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, gem.getLevelRequired())) {
            Microbot.showMessage("Crafting level too low to cut " + gem.getName() + ".");
            super.shutdown();
            return;
        }

        final String gemName = gem.getName();
        final String uncutGemName = "uncut " + gemName;

        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItem(uncutGemName)
                || !Rs2Inventory.hasItem("chisel");

        if (needBank) {
            handleGemBanking(config, gemName, uncutGemName);
        } else {
            cutGems(uncutGemName);
        }
    }

    /**
     * Resolves the gem to cut. Manual mode uses the config pick. Progressive mode chooses the
     * highest-level Gems the Crafting level allows whose uncut gem is in the bank; bank-aware, so it
     * only re-evaluates while the bank is open and otherwise keeps the in-progress pick.
     */
    private void updateActiveGem(AutoCraftingPlusConfig config) {
        if (!config.progressiveCraft()) {
            activeGem = config.gemType();
            return;
        }
        if (activeGem == null) {
            activeGem = config.gemType();
        }
        if (!Rs2Bank.isOpen()) {
            return; // can't read bank stock yet; keep the in-progress pick
        }

        int crafting = Rs2Player.getRealSkillLevel(Skill.CRAFTING);
        Gems best = null;
        for (Gems g : Gems.values()) {
            if (g.getLevelRequired() > crafting) continue;
            if (!Rs2Bank.hasItem("uncut " + g.getName())) continue;
            if (best == null || g.getLevelRequired() > best.getLevelRequired()) {
                best = g;
            }
        }
        if (best != null) {
            activeGem = best;
        }
    }

    private void handleGemBanking(AutoCraftingPlusConfig config, String gemName, String uncutGemName) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Re-evaluate the progressive gem now the bank is open (so we can read uncut-gem stock).
        updateActiveGem(config);
        final String gem = activeGem.getName();
        final String uncutGem = "uncut " + gem;

        Rs2Bank.depositAll(gem);
        Rs2Bank.depositAll("crushed gem");
        sleepUntil(() -> !Rs2Inventory.hasItem(gem) && !Rs2Inventory.hasItem("crushed gem"), 3000);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        if (!Rs2Bank.hasItem(uncutGem)) {
            Microbot.showMessage("Out of " + uncutGem + " in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing gems";
        Rs2Bank.withdrawItem(true, "chisel");
        Rs2Bank.withdrawAll(true, uncutGem);
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

    // --- Amethyst cutting (chisel + amethyst -> make-X product) ---

    /**
     * Cuts amethyst (item {@link ItemID#AMETHYST}) into the chosen product. Mirrors the GEM_CUTTING
     * chisel flow but uses the production / make-X dialog (widget group 270) to pick the product,
     * because one amethyst can become bolt tips / arrowtips / javelin heads / dart tips.
     */
    private void runAmethyst(AutoCraftingPlusConfig config) {
        final AmethystProduct product = config.amethystProduct();
        activeAmethyst = product;

        if (!Rs2Player.getSkillRequirement(Skill.CRAFTING, product.getLevelRequired())) {
            Microbot.showMessage("Crafting level too low to make " + product.getProductName() + ".");
            super.shutdown();
            return;
        }

        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItem(ItemID.AMETHYST)
                || !Rs2Inventory.hasItem("chisel");

        if (needBank) {
            handleAmethystBanking(product);
        } else {
            cutAmethyst(product);
        }
    }

    private void handleAmethystBanking(AmethystProduct product) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit the finished product (keep the chisel + any remaining amethyst).
        Rs2Bank.depositAll(product.getProductId());
        sleep(400);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        if (!Rs2Inventory.hasItem("chisel")) {
            if (!Rs2Bank.hasItem("chisel")) {
                Microbot.showMessage("No chisel in the bank!");
                super.shutdown();
                return;
            }
            Rs2Bank.withdrawItem(true, "chisel");
        }

        if (!Rs2Bank.hasItem(ItemID.AMETHYST)) {
            Microbot.showMessage("Out of amethyst in the bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing amethyst";
        Rs2Bank.withdrawAll(ItemID.AMETHYST);
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    /**
     * Chisel on amethyst -&gt; wait for the production/make-X dialog -&gt; click the chosen product by
     * name (which makes the whole batch) -&gt; wait until the amethyst is used up.
     */
    private void cutAmethyst(AmethystProduct product) {
        Microbot.status = "Cutting amethyst (" + product.getLabel() + ")";
        Rs2Inventory.use("chisel");
        Rs2Inventory.use(ItemID.AMETHYST);
        // The production dialog header text lives in the skill-multi group; wait for it, then pick.
        if (Rs2Widget.sleepUntilHasWidgetText("How many do you wish to make?", InterfaceID.SKILLMULTI, 5, false, 5000)) {
            Rs2Widget.clickWidget(product.getProductName(), true);
            sleep(1800);
            sleepUntil(() -> !Rs2Inventory.hasItem(ItemID.AMETHYST), 60000);
            actionsCompleted++;
        }
    }

    // --- Amulet stringing (ball of wool on unstrung amulet -> make-X batch) ---

    /**
     * Strings the chosen unstrung amulet "(u)" with a ball of wool. Using wool on the amulet opens a
     * make-X / production dialog and strings the whole batch (4 Crafting XP each, no level
     * requirement). Bank withdraws the unstrung amulet + wool 1:1 and banks the strung result.
     */
    private void runStringing(AutoCraftingPlusConfig config) {
        final StringAmulet amulet = config.stringAmulet();
        activeAmulet = amulet;

        boolean needBank = shutdownAfterCleanup
                || !Rs2Inventory.hasItem(amulet.getUnstrungId())
                || !Rs2Inventory.hasItem(ItemID.BALL_OF_WOOL);

        if (needBank) {
            handleStringingBanking(amulet);
        } else {
            stringAmulets(amulet);
        }
    }

    private void handleStringingBanking(StringAmulet amulet) {
        if (Rs2Player.isMoving()) return;
        Microbot.status = "Banking";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit strung amulets and any leftover wool / wrong-tier amulets (stringing needs no tools),
        // then restock. Depositing everything clears stale items if the Amulet config changed mid-run.
        Rs2Bank.depositAll();
        sleep(400);

        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoCraftingPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        // String 1:1 -> split a full inventory half wool, half amulets (14 each).
        final int perTrip = 14;
        int unstrungStock = Rs2Bank.count(amulet.getUnstrungId());
        int woolStock = Rs2Bank.count(ItemID.BALL_OF_WOOL);
        int amount = Math.min(perTrip, Math.min(unstrungStock, woolStock));

        if (amount <= 0) {
            Microbot.showMessage("Out of " + amulet.getLabel() + " (u) or balls of wool in the bank!");
            super.shutdown();
            return;
        }

        Microbot.status = "Withdrawing amulets + wool";
        Rs2Bank.withdrawX(ItemID.BALL_OF_WOOL, amount);
        sleep(300);
        Rs2Bank.withdrawX(amulet.getUnstrungId(), amount);
        Rs2Random.wait(400, 900);
        Rs2Bank.closeBank();
    }

    /**
     * Use a ball of wool on the unstrung amulet to open the make-X / production dialog, then make the
     * whole batch (space confirms "make all"). Waits until the unstrung amulets are gone.
     */
    private void stringAmulets(StringAmulet amulet) {
        Microbot.status = "Stringing " + amulet.getLabel();
        Rs2Inventory.use(ItemID.BALL_OF_WOOL);
        Rs2Inventory.use(amulet.getUnstrungId());
        sleep(600);
        Rs2Keyboard.keyPress(KeyEvent.VK_SPACE); // confirm "make all" on the quantity dialog
        sleep(1800);
        sleepUntil(() -> !Rs2Inventory.hasItem(amulet.getUnstrungId()), 60000);
        actionsCompleted++;
    }

    // --- Furnace jewellery ---

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

        // The tile-object cache query walks the live scene graph (player/world-view/scene/tick) with
        // no internal client-thread hop, so the whole chain must run on the client thread.
        Rs2TileObjectModel furnace = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getRs2TileObjectCache().query()
                        .withName("Furnace")
                        .where(o -> Rs2GameObject.hasAction(o, "Smelt"))
                        .nearest(anchor, 20)
        ).orElse(null);

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
