package net.runelite.client.plugins.microbot.runecraftplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spellbook;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

/**
 * AutoRunecraftPlus v0.1.0.
 *
 * <p>Forks the chillRunecraft essence-&gt;altar-&gt;craft-&gt;bank loop and wraps it in the Plus
 * layer (stop conditions, target level + clean shutdown, overlay/pause, speed mode, league mode).
 * v0.1.0 uses Pure essence (the proven path; works at every altar). Rune essence + pouch fill/empty
 * are the v0.2.0 increment.</p>
 */
@Slf4j
public class AutoRunecraftPlusScript extends Script {

    // Binding necklace (the enchanted emerald necklace used to guarantee a combo-rune bind). Item id
    // 5521; in the in-client gameval table it is named MAGIC_EMERALD_NECKLACE, so the literal id and
    // the display name are kept here to avoid that confusing constant name. A worn necklace lasts 16
    // altar clicks then crumbles to dust (OSRS Wiki); a worn necklace makes every bind succeed.
    private static final int BINDING_NECKLACE_ID = 5521;
    private static final String BINDING_NECKLACE_NAME = "Binding necklace";

    // Essence + pouch settings, resolved from config in run() (v0.2.0).
    private int essenceId;
    private String essenceName;
    private boolean usePouches;

    // Combo-rune settings, resolved from config in run() (v0.3.0). comboRune == NONE keeps the normal
    // single-rune path entirely unchanged.
    private ComboRune comboRune = ComboRune.NONE;
    private int spareBindingNecklaces;

    private State state = State.BANKING;
    private Altars altar;
    private boolean initialise;

    // Stats (read by AutoRunecraftPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;

    // Set when targetLevel is reached; intercepted after the deposit step in handleBanking so we
    // shut down before withdrawing more essence.
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }
    public Altars getAltar() { return altar; }
    public int getEssenceId() { return essenceId; }

    public boolean run(AutoRunecraftPlusConfig config) {
        comboRune = config.comboRune();
        spareBindingNecklaces = Math.max(0, config.spareBindingNecklaces());
        // Combo crafting binds at the combo's element altar and ignores the General altar choice. The
        // secondary runes are bound onto pure essence, so combo mode forces pure essence regardless of
        // the essence picker (rune essence cannot be used at non-rune-essence altars and the combo
        // recipe always wants pure essence).
        if (comboRune.isCombo()) {
            altar = comboRune.getAltar();
            essenceId = EssenceType.PURE.getItemId();
            essenceName = EssenceType.PURE.getItemName();
        } else {
            altar = config.altar();
            essenceId = config.essenceType().getItemId();
            essenceName = config.essenceType().getItemName();
        }
        // Pouches are deliberately disabled in combo mode. Combo crafting must keep pure essence and
        // the secondary runes at an exact 1:1 count, and each altar click also burns one secondary
        // talisman plus one binding-necklace charge. Mixing that with the fill/empty pouch dance is
        // the most error-prone, least testable path, so combo trips do a single full-inventory bind
        // (one click) per trip: essence count == secondary rune count, one talisman, one charge.
        usePouches = config.usePouches() && !comboRune.isCombo();
        initialise = true;
        state = State.BANKING;

        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.RUNECRAFT)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.RUNECRAFT)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false;

        // Combo runes have a hard level requirement; below it the altar simply will not bind, so refuse
        // to start rather than walk loops banking essence that never converts.
        if (comboRune.isCombo() && startSkillLevel < comboRune.getLevelRequired()) {
            Microbot.showMessage(comboRune.getDisplayName() + " needs Runecraft level "
                    + comboRune.getLevelRequired() + " (you are " + startSkillLevel + ").");
            return false;
        }

        Microbot.enableAutoRunOn = true;
        // v0.1.1: keep the running loop on foot. Without this the walker's nearest-bank fallback
        // treats the free Lumbridge Home Teleport as a cheap transport edge and "home-teles to
        // Lumbridge bank" instead of walking to the geographically-nearest bank (e.g. Edgeville
        // for the Body altar). Matches AutoSmeltingPlus / AutoMiningPlus.
        Rs2Walker.disableTeleports = true;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyRunecraftingSetup();
        Rs2Antiban.setActivity(Activity.GENERAL_RUNECRAFT);
        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                // Pause check (overlay button toggles the global flag).
                if (Microbot.pauseAllScripts.get()) {
                    Microbot.status = "[PAUSED]";
                    return;
                }

                // Stop conditions.
                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoRunecraftPlus: reached stopAfterMinutes. Shutting down.");
                    super.shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0) {
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(Skill.RUNECRAFT)).orElse(startSkillXp);
                    if (currentXp - startSkillXp >= config.stopAfterXp()) {
                        Microbot.log("AutoRunecraftPlus: reached stopAfterXp. Shutting down.");
                        super.shutdown();
                        return;
                    }
                }
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.RUNECRAFT)).orElse(startSkillLevel);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoRunecraftPlus: reached targetLevel (" + currentLevel
                                + "). Banking then shutting down.");
                        shutdownAfterCleanup = true;
                        state = State.BANKING;
                    }
                }

                // League mode: periodic key press resets the idle-logout.
                if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                    int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length - 1)]);
                }

                if (Rs2AntibanSettings.actionCooldownActive) return;

                // First-tick state seed: go straight to the altar if we already have gear + essence.
                if (initialise) {
                    initialise = false;
                    boolean haveTalismanOrTiara = Rs2Inventory.hasItem(altar.getTalismanName())
                            || Rs2Equipment.isWearing(altar.getTiaraName());
                    boolean haveEssence = Rs2Inventory.hasItem(essenceName, false)
                            || (usePouches && Rs2Inventory.hasAnyPouch() && !Rs2Inventory.allPouchesEmpty());
                    boolean comboReady = !comboRune.isCombo()
                            || (Rs2Inventory.hasItem(comboRune.getSecondaryRuneId())
                                && (Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false)
                                    || Rs2Inventory.hasItem(BINDING_NECKLACE_ID)));
                    state = (haveTalismanOrTiara && haveEssence && comboReady) ? State.WALKING_TO_ALTAR : State.BANKING;
                }

                if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;
                if (Rs2Player.isInteracting()) return;

                // Pouches in an unknown state (e.g. after a break): right-click "Check" once.
                if (usePouches && Rs2Inventory.anyPouchUnknown()) {
                    Rs2Inventory.checkPouches();
                    return;
                }

                switch (state) {
                    case BANKING:
                        handleBanking();
                        break;

                    case WALKING_TO_ALTAR:
                        Microbot.status = "Walking to altar";
                        if (!Rs2Walker.walkTo(altar.getAltarWorldPoint(), 2)) return;
                        Rs2Random.wait(600, 1200);
                        state = State.ENTERING_ALTAR;
                        break;

                    case ENTERING_ALTAR:
                        Microbot.status = "Entering altar";
                        if (!Rs2Equipment.isWearing(altar.getTiaraName())) {
                            Rs2Inventory.useItemOnObject(altar.getTalismanID(), altar.getAltarRuinsID());
                        } else {
                            Microbot.getRs2TileObjectCache().query().withId(altar.getAltarRuinsID()).interact("Enter");
                        }
                        sleepUntil(() -> !Rs2Player.isMoving());
                        Rs2Random.wait(1600, 2200);
                        state = State.CRAFTING;
                        break;

                    case CRAFTING:
                        // Out of loose essence: empty the pouches and craft again, or leave the
                        // altar once everything (loose + pouches) is used.
                        if (!Rs2Inventory.hasItem(essenceId)) {
                            if (usePouches && Rs2Inventory.hasAnyPouch() && !Rs2Inventory.allPouchesEmpty()) {
                                Rs2Inventory.emptyPouches();
                                Rs2Inventory.waitForInventoryChanges(1800);
                                return;
                            }
                            state = State.EXITING_ALTAR;
                            break;
                        }
                        // Combo: bind only while a necklace guarantees success. The worn necklace
                        // crumbles after 16 altar clicks (no warning, it just vanishes from the neck
                        // slot), so before every bind re-equip a spare. Bind with no necklace would
                        // be a 50% loss of essence + runes, so bail to the bank if none are left.
                        if (comboRune.isCombo()) {
                            if (!Rs2Inventory.hasItem(comboRune.getSecondaryRuneId())) {
                                // Ran out of secondary runes (e.g. failed binds before a necklace was
                                // on, or a mismatched stack). Nothing more to bind this trip.
                                state = State.EXITING_ALTAR;
                                break;
                            }
                            if (!ensureBindingNecklaceWorn()) {
                                state = State.EXITING_ALTAR;
                                break;
                            }
                        }
                        Microbot.status = comboRune.isCombo() ? "Crafting combo runes" : "Crafting runes";
                        Rs2Inventory.useItemOnObject(essenceId, altar.getAltarID());
                        Rs2Random.wait(600, 1200);
                        sleepUntil(() -> Rs2Player.waitForXpDrop(Skill.RUNECRAFT));
                        actionsCompleted++;
                        break;

                    case EXITING_ALTAR:
                        Microbot.status = "Exiting altar";
                        Microbot.getRs2TileObjectCache().query().withId(altar.getPortalID()).interact("Use");
                        sleepUntil(() -> !Rs2Player.isMoving());
                        Rs2Random.wait(600, 1200);
                        state = State.BANKING;
                        break;
                }

                Rs2Antiban.actionCooldown();
                Rs2Antiban.takeMicroBreakByChance();
            } catch (Exception ex) {
                Microbot.logStackTrace("AutoRunecraftPlusScript", ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Make sure a binding necklace is worn before a combo bind. Returns true if one is already worn
     * or a spare was equipped from the inventory; false if none are left (caller should leave the
     * altar and re-bank). A worn necklace crumbles silently after 16 altar clicks, so this is checked
     * before every single bind rather than once per trip.
     */
    private boolean ensureBindingNecklaceWorn() {
        if (Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false)) {
            return true;
        }
        if (!Rs2Inventory.hasItem(BINDING_NECKLACE_ID)) {
            Microbot.status = "Out of binding necklaces";
            return false;
        }
        Microbot.status = "Equipping binding necklace";
        Rs2Inventory.equip(BINDING_NECKLACE_ID);
        // Wait for the necklace to actually move to the neck slot before binding, otherwise the next
        // craft could fire while unworn and burn essence at the 50% failure rate.
        sleepUntil(() -> Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false), 2000);
        return Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false);
    }

    private void handleBanking() {
        Microbot.status = "Walking to bank";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit crafted runes. In combo mode the product is the combo rune, not the altar's own
        // single rune; the secondary runes carried for binding are deliberately kept (they re-balance
        // 1:1 against essence in the combo withdrawal below).
        String craftedRuneName = comboRune.isCombo() ? comboRune.getDisplayName() : altar.getRuneName();
        if (Rs2Inventory.hasItem(craftedRuneName, false)) {
            Microbot.status = "Depositing runes";
            Rs2Bank.depositAll(craftedRuneName, false);
            Rs2Random.wait(600, 1200);
        }

        // target-level cleanup: deposit done, stop before withdrawing more essence.
        if (shutdownAfterCleanup) {
            Rs2Bank.closeBank();
            Microbot.log("AutoRunecraftPlus: target reached, banked, shutting down.");
            super.shutdown();
            return;
        }

        // Ensure a talisman or tiara is available.
        if (!Rs2Inventory.hasItem(altar.getTalismanName()) && !Rs2Equipment.isWearing(altar.getTiaraName())) {
            if (Rs2Bank.hasBankItem(altar.getTiaraName())) {
                Microbot.status = "Withdrawing tiara";
                Rs2Bank.withdrawAndEquip(altar.getTiaraName());
                Rs2Random.wait(600, 1200);
            } else if (Rs2Bank.hasBankItem(altar.getTalismanName())) {
                Microbot.status = "Withdrawing talisman";
                Rs2Bank.withdrawOne(altar.getTalismanName());
                Rs2Random.wait(600, 1200);
            } else {
                Microbot.showMessage("No " + altar.getTiaraName() + " or " + altar.getTalismanName() + " in bank!");
                super.shutdown();
                return;
            }
        }

        // Combo mode owns the rest of the banking (necklaces + secondary runes + secondary talisman
        // + matching pure essence). It returns or shuts down internally and never falls through to
        // the single-rune essence path below.
        if (comboRune.isCombo()) {
            handleComboWithdraw();
            return;
        }

        // Repair degraded pouches via NPC Contact (Lunar spellbook only). No-op for F2P / non-Lunar.
        if (usePouches && Rs2Inventory.hasDegradedPouch()
                && Rs2Magic.isSpellbook(Rs2Spellbook.LUNAR)
                && Rs2Magic.hasRequiredRunes(Rs2Spells.NPC_CONTACT)) {
            Microbot.status = "Repairing pouches";
            Rs2Bank.closeBank();
            Rs2Magic.repairPouchesWithLunar();
            return; // re-open the bank next tick
        }

        // Withdraw essence (fill pouches first when enabled).
        if (!Rs2Bank.hasBankItem(essenceName, false)) {
            Microbot.showMessage("No " + essenceName + " in bank!");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing essence";
        if (usePouches && Rs2Inventory.hasAnyPouch()) {
            int guard = 0;
            while (!Rs2Inventory.allPouchesFull() && isRunning() && guard++ < 6) {
                Rs2Bank.withdrawAll(essenceId);
                Rs2Inventory.fillPouches();
                Rs2Inventory.waitForInventoryChanges(1800);
            }
        }
        Rs2Bank.withdrawAll(essenceName, true);
        Rs2Random.wait(600, 1200);
        Rs2Bank.closeBank();
        state = State.WALKING_TO_ALTAR;
    }

    /**
     * Combo-mode withdrawal (bank already open, crafted runes already deposited, entry talisman/tiara
     * already secured by {@link #handleBanking()}). Builds a single full-inventory bind: one binding
     * necklace worn plus spares, one secondary talisman (consumed once per altar click, and a combo
     * trip is a single click), then equal counts of pure essence and secondary runes. Shuts the bot
     * down with a message if any required bank stock is missing.
     */
    private void handleComboWithdraw() {
        // Reset the secondary runes so they re-balance exactly 1:1 with essence below. Without this,
        // leftover runes from a prior trip would drift the essence:rune ratio and waste essence.
        if (Rs2Inventory.hasItem(comboRune.getSecondaryRuneId())) {
            Rs2Bank.depositAll(comboRune.getSecondaryRuneId());
            Rs2Random.wait(300, 700);
        }

        // Stock checks up front so we fail fast with a clear message rather than half-equipping.
        if (!Rs2Bank.hasBankItem(BINDING_NECKLACE_NAME, false)
                && !Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false)
                && !Rs2Inventory.hasItem(BINDING_NECKLACE_ID)) {
            Microbot.showMessage("No " + BINDING_NECKLACE_NAME + " in bank!");
            super.shutdown();
            return;
        }
        if (!Rs2Bank.hasBankItem(comboRune.getSecondaryRuneName(), false)) {
            Microbot.showMessage("No " + comboRune.getSecondaryRuneName() + " in bank!");
            super.shutdown();
            return;
        }
        if (!Rs2Bank.hasBankItem(comboRune.getSecondaryTalismanName(), false)
                && !Rs2Inventory.hasItem(comboRune.getSecondaryTalismanId())) {
            Microbot.showMessage("No " + comboRune.getSecondaryTalismanName() + " in bank!");
            super.shutdown();
            return;
        }
        if (!Rs2Bank.hasBankItem(essenceName, false)) {
            Microbot.showMessage("No " + essenceName + " in bank!");
            super.shutdown();
            return;
        }

        // Equip a binding necklace now if none is worn, then top up spares in the inventory. The worn
        // one carries the bind; spares are swapped in mid-trip by ensureBindingNecklaceWorn() when it
        // crumbles.
        if (!Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false)) {
            Microbot.status = "Equipping binding necklace";
            if (Rs2Inventory.hasItem(BINDING_NECKLACE_ID)) {
                Rs2Inventory.equip(BINDING_NECKLACE_ID);
            } else {
                Rs2Bank.withdrawAndEquip(BINDING_NECKLACE_NAME);
            }
            sleepUntil(() -> Rs2Equipment.isWearing(BINDING_NECKLACE_NAME, false), 2000);
        }
        if (spareBindingNecklaces > 0) {
            int spareShort = spareBindingNecklaces - Rs2Inventory.count(BINDING_NECKLACE_ID);
            if (spareShort > 0 && Rs2Bank.hasBankItem(BINDING_NECKLACE_NAME, false)) {
                Microbot.status = "Withdrawing spare necklaces";
                Rs2Bank.withdrawX(BINDING_NECKLACE_ID, spareShort);
                Rs2Random.wait(300, 700);
            }
        }

        // One secondary talisman per altar click; a combo trip is one click, so one talisman is
        // enough. Withdraw only if we are not already holding one (talismans do not stack).
        if (!Rs2Inventory.hasItem(comboRune.getSecondaryTalismanId())) {
            Microbot.status = "Withdrawing " + comboRune.getSecondaryTalismanName();
            Rs2Bank.withdrawOne(comboRune.getSecondaryTalismanId());
            Rs2Random.wait(300, 700);
        }

        // Size the essence/secondary-rune batch from the free slots that remain after gear, spares,
        // the secondary talisman and one slot reserved for the (stackable) secondary rune. Withdraw
        // an equal count of each so every essence has a rune to bind with (1:1) and none is wasted.
        int essenceBatch = Rs2Inventory.emptySlotCount() - 1; // reserve one slot for the rune stack
        if (essenceBatch < 1) {
            Microbot.showMessage("Not enough inventory space for combo crafting. Reduce spare necklaces.");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing essence";
        Rs2Bank.withdrawX(essenceId, essenceBatch);
        Rs2Inventory.waitForInventoryChanges(1800);

        int essenceHeld = Rs2Inventory.count(essenceId);
        if (essenceHeld < 1) {
            Microbot.showMessage("No " + essenceName + " withdrawn for combo crafting.");
            super.shutdown();
            return;
        }
        Microbot.status = "Withdrawing " + comboRune.getSecondaryRuneName();
        Rs2Bank.withdrawX(comboRune.getSecondaryRuneId(), essenceHeld);
        Rs2Inventory.waitForInventoryChanges(1800);

        Rs2Random.wait(300, 700);
        Rs2Bank.closeBank();
        state = State.WALKING_TO_ALTAR;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Walker.disableTeleports = false; // v0.1.1: reset the shared flag for other plugins
        Rs2Antiban.resetAntibanSettings();
    }
}
