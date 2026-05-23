// TEMPLATE FILE — see TEMPLATE.md. Replace "Skill" / "skillplus" / "SkillPlus" tokens.
//
// This skeleton encodes the script-side patterns we learned from AutoMiningPlus v0.1.0–v0.1.11.
// If you copy this and adapt for a new skill, you inherit ALL the bug fixes that took 10
// iterations to surface. Do NOT redesign the main loop or state machine unless you have a
// specific reason — the patterns interlock and skipping one resurfaces past bugs.
//
// REQUIRED READING before editing:
//   - chsami/Microbot/AGENTS.md (threading, sleepUntil, cache queries)
//   - chsami/Microbot/runelite-client/.../microbot/AGENTS.md (plugin & script lifecycle)
//   - PATTERNS.md in this template/ folder
//
// THE 7 SCRIPT-SIDE PATTERNS encoded below (search for the comments to find each):
//   1. Anchor re-pin: initialPlayerLocation re-set to activeLocation.getWorldPoint() each tick
//   2. Inventory-full pre-check: flip state to RESETTING BEFORE the anchor check runs
//   3. State-gated anchor: ensureConfiguredLocation only fires when state == ACTIVE
//   4. Animation-based wait: Global.sleepUntil(isAnimating, 1200) NOT Rs2Player.waitForXpDrop
//   5. wrongActiveLocation flag: detect ore-vs-location mismatch, surface via overlay
//   6. walkToConfiguredBank: respect user's bank pick, fall back to nearest
//   7. Speed mode: single Rs2AntibanSettings.antibanEnabled = false flip

package net.runelite.client.plugins.microbot.skillplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.skillplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.skillplus.data.SkillLocationOption;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AutoSkillPlusScript extends Script {

    State state = State.ACTIVE;

    // Currently-resolved location, set every tick by updateActiveTarget(). Used by the overlay
    // and by ensureConfiguredLocation. May be null if AUTO_BEST with no accessible mines.
    // TODO(plus): add an active-target field too (e.g. private Rocks activeRock; for mining).

    // Pattern 5: wrongActiveLocation flag — true when user picked a specific location that
    // doesn't host the configured target. We still walk there (deterministic UX) but the
    // overlay surfaces a clear warning via updateStatus().
    private boolean wrongActiveLocation = false;

    public boolean run(AutoSkillPlusConfig config) {
        // Reset fields BEFORE scheduling — static state leaks across enable/disable cycles.
        initialPlayerLocation = null;
        wrongActiveLocation = false;
        state = State.ACTIVE;

        // Antiban setup. The matching template configures usePlayStyle/naturalMouse/etc. for
        // your skill. Available: applyMiningSetup, applyWoodcuttingSetup, applyFishingSetup,
        // applyCookingSetup, applyCombatSetup, applySmithingSetup, applyCraftingSetup.
        Rs2Antiban.resetAntibanSettings();
        // TODO(plus): replace with your skill's setup template.
        // Rs2Antiban.antibanSetupTemplates.applySmithingSetup();
        Rs2AntibanSettings.actionCooldownChance = 0.1;

        // Pattern 7: Speed mode — flip the master antiban switch off. Every check inside
        // Rs2Antiban.actionCooldown(), takeMicroBreakByChance(), naturalMouseMovement() etc.
        // short-circuits when antibanEnabled is false. Throwaway-only.
        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;

                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }
                if (initialPlayerLocation == null) return;

                // Update the active target (e.g. ore, fish, log type) and resolve activeLocation
                // from the user's SkillLocationOption pick. updateActiveTarget() ALSO re-anchors
                // initialPlayerLocation to activeLocation.getWorldPoint() (pattern 1).
                updateActiveTarget(config);

                // Pattern 2: inventory-full check runs BEFORE the anchor-check. If the user
                // toggled the script with a full inventory, we go to RESETTING immediately
                // instead of being walked to the work site first.
                if (Rs2Inventory.isFull() && state != State.RESETTING) {
                    state = State.RESETTING;
                }

                // Pattern 3: state-gated anchor check. During RESETTING the deposit branch
                // walks the player away from the work site; if we let ensureConfiguredLocation
                // run, it would override the bank route as soon as the walker arrived at the
                // bank tile and the player stopped moving.
                if (state == State.ACTIVE && ensureConfiguredLocation(config)) {
                    return;
                }

                if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

                switch (state) {
                    case ACTIVE:
                        if (Rs2Inventory.isFull()) {
                            state = State.RESETTING;
                            return;
                        }
                        // TODO(plus): your skill's primary action. Pattern shape:
                        //   GameObject target = Rs2GameObject.findReachableObject(
                        //       activeTarget.getName(), true, config.distanceToStray(), initialPlayerLocation);
                        //   if (target != null && Rs2GameObject.interact(target)) {
                        //       Global.sleepUntil(Rs2Player::isAnimating, 1200);   // Pattern 4
                        //       Rs2Antiban.actionCooldown();
                        //       Rs2Antiban.takeMicroBreakByChance();
                        //   }
                        //
                        // For stationary skills (cooking, smelting, fletching):
                        //   Rs2GameObject.interact(furnace);                    // or Rs2Npc.interact, etc.
                        //   Global.sleepUntil(() -> Rs2Widget.hasWidget(...), 3000);
                        //   Rs2Widget.clickWidget(<bar/food/etc. childId>);
                        //   Global.sleepUntil(Rs2Player::isAnimating, 1200);
                        Microbot.status = "TODO(plus): activity";
                        break;

                    case RESETTING:
                        // TODO(plus): handle banking. The walkToConfiguredBank() helper below
                        // (pattern 6) takes care of routing to the user's chosen bank or falling
                        // back to nearest. After deposit/withdrawal, set state = ACTIVE.
                        if (config.useBank()) {
                            if (!Rs2Bank.isOpen()) {
                                if (!walkToConfiguredBank(config)) return;
                                return;
                            }
                            // TODO(plus): deposit by name filter, then any skill-specific
                            // withdrawals (e.g. raw food, ores+coal for steel bars). Pattern:
                            //   Rs2Bank.depositAll(item -> item.getName() != null &&
                            //           itemsToBank.contains(item.getName().toLowerCase()));
                            //   // optional withdraw step here
                            if (!Rs2Bank.closeBank()) return;
                            Rs2Walker.walkTo(initialPlayerLocation, config.distanceToStray());
                        } else {
                            // Power-mining style: drop the gathered items.
                            Rs2Inventory.dropAllExcept();
                        }
                        state = State.ACTIVE;
                        break;
                }
            } catch (Exception ex) {
                Microbot.log(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    /**
     * Updates activeTarget and activeLocation each tick. ALSO re-anchors
     * initialPlayerLocation to activeLocation.getWorldPoint() (pattern 1).
     *
     * <p>TODO(plus): adapt for your skill. The shape should be:
     * <ol>
     *   <li>Pick activeTarget (config-driven or progressive-mode best-for-level).</li>
     *   <li>Call SkillLocationOption.resolve(activeTarget) to get the LocationOption.</li>
     *   <li>Check hostsTarget; set wrongActiveLocation flag.</li>
     *   <li>Re-anchor initialPlayerLocation to activeLocation.getWorldPoint().</li>
     *   <li>Call updateStatus().</li>
     * </ol>
     */
    private void updateActiveTarget(AutoSkillPlusConfig config) {
        // TODO(plus): set activeTarget from config (ore/fish/bar/etc.)

        SkillLocationOption choice = config.skillLocation();
        // TODO(plus): activeLocation = resolveLocation(choice, activeTarget);
        // wrongActiveLocation = choice != null
        //         && choice != SkillLocationOption.AUTO_BEST
        //         && !choice.hostsTarget(activeTarget);
        // if (wrongActiveLocation) {
        //     log.warn("AutoSkillPlus: {} does not host {}. Walking there anyway.",
        //             choice.name(), activeTarget);
        // }

        // Pattern 1: anchor re-pin. Without this, initialPlayerLocation stays at the toggle-on
        // tile (login spawn, bank, etc.) and the script yo-yos between work site and toggle tile.
        // if (activeLocation != null && activeLocation.getWorldPoint() != null) {
        //     initialPlayerLocation = activeLocation.getWorldPoint();
        // }

        updateStatus();
    }

    /**
     * Pattern 6: respect the user's BankLocationOption pick if set, otherwise fall back to
     * upstream's "nearest by raw distance" heuristic. The nearest-bank heuristic often picks
     * toll-gated banks (e.g. Al Kharid over Lumbridge from the East mine).
     *
     * <p>Returns true when the bank is open and ready to deposit, false while still in transit.
     */
    private boolean walkToConfiguredBank(AutoSkillPlusConfig config) {
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

        if (playerLocation.getX() != bankPoint.getX() || playerLocation.getY() != bankPoint.getY()
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

    /**
     * Walks the player to activeLocation.getWorldPoint() if they're more than distanceToStray
     * tiles away. Returns true while the walk is in progress so the caller can early-return.
     *
     * <p>TODO(plus): activeLocation is a field you maintain; uncomment the body once you
     * have that field on your script.
     */
    private boolean ensureConfiguredLocation(AutoSkillPlusConfig config) {
        // if (activeLocation == null || activeLocation.getWorldPoint() == null) return false;
        // WorldPoint targetPoint = activeLocation.getWorldPoint();
        // if (initialPlayerLocation == null) initialPlayerLocation = targetPoint;
        // WorldPoint playerLocation = Rs2Player.getWorldLocation();
        // if (playerLocation == null) return true;
        //
        // // Honor user's distanceToStray. Don't hard-cap (the upstream "min(stray, 5)" cap
        // // caused yo-yo walking in v0.1.4; lifted in v0.1.5).
        // int acceptableDistance = Math.max(1, config.distanceToStray());
        // int distanceToTarget = playerLocation.distanceTo(targetPoint);
        // if (distanceToTarget > acceptableDistance) {
        //     if (Rs2Player.isMoving()) return true;
        //     Rs2Walker.walkTo(targetPoint, 3);
        //     return true;
        // }
        return false;
    }

    /**
     * Sets Microbot.status. Surface a loud warning when wrongActiveLocation is true so the user
     * can see why the bot is idle at a location that doesn't host their target.
     */
    private void updateStatus() {
        // TODO(plus): replace with your skill's target name.
        String targetName = "TODO";
        // String targetName = activeTarget != null ? activeTarget.getName() : "Unknown";
        // String locationName = (activeLocation != null && activeLocation.getName() != null)
        //         ? activeLocation.getName() : "current area";
        String locationName = "TODO";
        if (wrongActiveLocation) {
            Microbot.status = "WRONG TARGET: no " + targetName + " at " + locationName
                    + " — change target or location";
        } else {
            Microbot.status = "Working " + targetName + " @ " + locationName;
        }
    }
}
