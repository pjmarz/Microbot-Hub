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

    // Essence + pouch settings, resolved from config in run() (v0.2.0).
    private int essenceId;
    private String essenceName;
    private boolean usePouches;

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

    public boolean run(AutoRunecraftPlusConfig config) {
        altar = config.altar();
        essenceId = config.essenceType().getItemId();
        essenceName = config.essenceType().getItemName();
        usePouches = config.usePouches();
        initialise = true;
        state = State.BANKING;

        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.RUNECRAFT)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.RUNECRAFT)).orElse(1);
        actionsCompleted = 0;
        shutdownAfterCleanup = false;

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
                    state = (haveTalismanOrTiara && haveEssence) ? State.WALKING_TO_ALTAR : State.BANKING;
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
                        Microbot.status = "Crafting runes";
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

    private void handleBanking() {
        Microbot.status = "Walking to bank";
        boolean isBankOpen = Rs2Bank.walkToBankAndUseBank();
        if (!isBankOpen || !Rs2Bank.isOpen()) return;

        // Deposit crafted runes.
        if (Rs2Inventory.hasItem(altar.getRuneName(), false)) {
            Microbot.status = "Depositing runes";
            Rs2Bank.depositAll(altar.getRuneName(), false);
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

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Walker.disableTeleports = false; // v0.1.1: reset the shared flag for other plugins
        Rs2Antiban.resetAntibanSettings();
    }
}
