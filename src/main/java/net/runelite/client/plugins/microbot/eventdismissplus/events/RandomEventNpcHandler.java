package net.runelite.client.plugins.microbot.eventdismissplus.events;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusConfig;
import net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusEventLog;
import net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusScript;
import net.runelite.client.plugins.microbot.eventdismissplus.data.RandomEventType;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;

/**
 * BlockingEvent for NPC-based random events. Forks DismissNpcEvent from upstream
 * eventdismiss/ with three additions:
 * <ol>
 *   <li>Variable 2-5 second response delay (anti-detection -- instant dismiss is a bot signal)</li>
 *   <li>Per-event engagement decisions via {@link EventDismissPlusConfig}</li>
 *   <li>Complete Genie/Beekeeper/Count Check lamp dialogue including the skill picker
 *       (upstream just clicks Continue once and leaves the lamp unused)</li>
 * </ol>
 *
 * <p>Detection uses {@code Rs2Npc.getRandomEventNPC()} -- the existing Microbot client API
 * that maintains the canonical random-event-NPC ID list. We don't hardcode IDs.
 *
 * <p>Priority is {@code LOWEST} so other BlockingEvents (e.g. WoodcuttingPlus's Forestry
 * events) take precedence when they fire at the same time.
 */
public class RandomEventNpcHandler implements BlockingEvent {

    private final EventDismissPlusConfig config;
    private final EventDismissPlusScript script;

    public RandomEventNpcHandler(EventDismissPlusConfig config, EventDismissPlusScript script) {
        this.config = config;
        this.script = script;
    }

    private Rs2NpcModel getRandomEventNpc() {
        var oldModel = Rs2Npc.getRandomEventNPC();
        if (oldModel == null) return null;
        return Microbot.getRs2NpcCache().query()
                .where(n -> n.getNpc().equals(oldModel.getRuneliteNpc()))
                .nearestOnClientThread();
    }

    @Override
    public boolean validate() {
        Rs2NpcModel npc = getRandomEventNpc();
        if (npc == null) return false;
        return npc.hasLineOfSight();
    }

    @Override
    public boolean execute() {
        Rs2NpcModel npc = getRandomEventNpc();
        if (npc == null) return true;

        String name = npc.getName();
        if (name == null) return true;

        // Variable response delay (anti-detection). Instant dismiss is a bot signal.
        int delayMin = Math.max(0, config.responseDelayMin());
        int delayMax = Math.max(delayMin, config.responseDelayMax());
        if (delayMax > 0) {
            Global.sleep(Rs2Random.between(delayMin, delayMax));
        }

        // Re-fetch in case the NPC despawned during the delay
        npc = getRandomEventNpc();
        if (npc == null) {
            return true;
        }

        boolean engage = shouldEngage(name);

        // Random skip chance for antiban. Even if we would engage, roll a die and
        // force-dismiss with the configured probability. Real humans don't engage every
        // event; some skip due to being busy / focused on their main activity.
        String skipNote = null;
        if (engage && config.globalSkipChance() > 0) {
            int roll = Rs2Random.between(1, 101);
            if (roll <= config.globalSkipChance()) {
                Microbot.log("EventDismissPlus: random skip (antiban roll " + roll + "/" + config.globalSkipChance() + ") for " + name);
                engage = false;
                skipNote = "random skip (antiban roll)";
            }
        }

        try {
            if (engage) {
                engage(npc, name);
                EventDismissPlusEventLog.append(name, EventDismissPlusEventLog.Action.ENGAGE,
                        EventDismissPlusEventLog.Outcome.OK, "");

                // Defensive fallback for any engagement path. If engage() ran but the NPC is
                // still on screen (e.g. Sandwich Lady's tray widget wasn't picked because we
                // don't model widget interactions, Drunken Dwarf's yes/no option text didn't
                // match, or any future event whose engagement is structurally incomplete),
                // wait briefly for natural despawn then fall through to dismiss. Prevents a
                // BlockingEventManager re-fire loop. The CSV log captures these as
                // DISMISS/ERROR/"engagement fallback" so the empirical record shows which
                // events slip through.
                Global.sleepUntil(() -> !validate(), 3000);
                if (validate()) {
                    Microbot.log("EventDismissPlus: " + name + " engagement didn't despawn NPC; falling back to dismiss");
                    EventDismissPlusEventLog.append(name, EventDismissPlusEventLog.Action.DISMISS,
                            EventDismissPlusEventLog.Outcome.ERROR, "engagement fallback");
                    try {
                        // Re-resolve: npc was captured before a multi-second dialogue/sleep
                        // and may now be stale (despawned/respawned).
                        Rs2NpcModel fallbackNpc = getRandomEventNpc();
                        if (fallbackNpc != null) {
                            dismiss(fallbackNpc);
                        }
                    } catch (Exception ex) {
                        Microbot.log("EventDismissPlus: fallback dismiss failed: " + ex.getMessage());
                    }
                }
            } else {
                dismiss(npc);
                EventDismissPlusEventLog.append(name, EventDismissPlusEventLog.Action.DISMISS,
                        EventDismissPlusEventLog.Outcome.OK, skipNote == null ? "" : skipNote);
            }
        } catch (Exception ex) {
            Microbot.logStackTrace("RandomEventNpcHandler (" + name + ")", ex);
            EventDismissPlusEventLog.append(name,
                    engage ? EventDismissPlusEventLog.Action.ENGAGE : EventDismissPlusEventLog.Action.DISMISS,
                    EventDismissPlusEventLog.Outcome.ERROR,
                    ex.getMessage() == null ? "" : ex.getMessage());
            // Fall back to dismiss on any unexpected exception. Re-resolve first: npc may be
            // stale after a multi-second dialogue/sleep.
            try {
                Rs2NpcModel fallbackNpc = getRandomEventNpc();
                if (fallbackNpc != null) {
                    dismiss(fallbackNpc);
                }
            } catch (Exception ignored) {
            }
        }

        script.recordEventHandled(name);
        return !validate();
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.LOWEST;
    }

    // --- Engagement logic ---

    private boolean shouldEngage(String name) {
        if (name == null) return false;
        // Switch on NPC name. Multiple cases per event handle wiki spelling ambiguities
        // (Bee keeper vs Beekeeper, Dr Jekyll vs Dr. Jekyll, Sandwich lady vs Sandwich Lady).
        switch (name) {
            case "Genie": return config.engageGenie();
            case "Sandwich lady":
            case "Sandwich Lady": return config.engageSandwichLady();
            case "Drunken Dwarf":
            case "Drunken dwarf": return config.engageDrunkenDwarf();
            case "Mysterious Old Man":
            case "Mysterious old man": return config.engageMysteriousOldMan();
            case "Bee keeper":
            case "Beekeeper": return config.engageBeekeeper();
            case "Count Check": return config.engageCountCheck();
            case "Frog Prince":
            case "Frog Princess": return config.engageFrog();
            case "Rick Turpentine": return config.engageRickTurpentine();
            case "Dr Jekyll":
            case "Dr. Jekyll": return config.engageDrJekyll();
            default: return false; // unknown event -> dismiss
        }
    }

    private void engage(Rs2NpcModel npc, String name) {
        // Frog Prince / Princess: special right-click action "Kiss"
        if ("Frog Prince".equals(name) || "Frog Princess".equals(name)) {
            npc.click("Kiss");
            advanceDialogueClicks(8);
            return;
        }

        // Everything else uses standard Talk-to
        npc.click("Talk-to");
        Rs2Dialogue.sleepUntilHasContinue();

        if (RandomEventType.givesLamp(name)) {
            handleLampDialogue(name);
        } else {
            // Accept-and-acknowledge events (Sandwich Lady, Drunken Dwarf, Mysterious Old
            // Man, Rick Turpentine, Dr Jekyll). Just click through the dialog continues
            // until it ends.
            advanceDialogueClicks(10);
        }
    }

    /**
     * Genie / Beekeeper / Count Check share the same flow: dialogue continues, then a
     * "Yes please" option appears, then a skill picker. We click through, pick "Yes
     * please" if shown, then click the active skill.
     *
     * <p>The NPC name is passed so the fallback path can log it. If the safety loop exits
     * without claiming a lamp (e.g. a modern dialogue uses a help/decline question instead
     * of the Genie-style "Yes please" + skill picker flow), it falls through to
     * {@link #tryDeclineFallback(String)} to close the dialogue via a decline option.
     * Without this the NPC stays on screen and BlockingEventManager re-fires the handler
     * every few seconds.
     */
    private void handleLampDialogue(String npcName) {
        int safety = 12;
        while (safety-- > 0) {
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                Global.sleep(Rs2Random.between(400, 900));
                continue;
            }

            // Yes/No before the skill picker on some events
            if (Rs2Dialogue.hasDialogueOption("Yes please")) {
                Rs2Dialogue.clickOption("Yes please");
                Global.sleep(Rs2Random.between(400, 900));
                continue;
            }

            // Skill picker
            Skill targetSkill = pickLampSkill();
            if (targetSkill != null) {
                String skillName = targetSkill.getName();
                if (Rs2Dialogue.hasDialogueOption(skillName, true)) {
                    Rs2Dialogue.clickOption(skillName, true);
                    Global.sleep(Rs2Random.between(800, 1500));
                    // After skill is picked, dialog may continue with "you gained X xp"
                    advanceDialogueClicks(4);
                    return;
                }
            }
            // No actionable state -- break out
            break;
        }
        // Safety loop exited without claiming a lamp; the dialogue is still open (we never
        // hit the skill-picker happy path). Fall through to common decline phrasings to
        // close it cleanly.
        tryDeclineFallback(npcName);
    }

    /**
     * Defensive fallback for stuck lamp dialogues. Tries common decline phrasings to close
     * the dialogue when the Genie-style flow ("Yes please" + skill picker) doesn't match the
     * actual dialogue structure (some events use a help/decline question instead, which would
     * otherwise leave the NPC on screen and loop the handler).
     *
     * <p>Logs to the CSV event log as DECLINE/OK on success, DECLINE/ERROR on hard miss.
     * ERROR rows are the signal that the decline-text catalog needs widening or the event
     * needs proper engagement.
     */
    private void tryDeclineFallback(String npcName) {
        String[] declineTexts = {
                "Sorry, but I'd rather not help",
                "Sorry, I'm busy",
                "Buzz off",
                "I don't want to",
                "I'm too busy",
                "No thanks",
                "No, thank you"
        };
        for (String text : declineTexts) {
            if (Rs2Dialogue.hasDialogueOption(text)) {
                Microbot.log("EventDismissPlus: " + npcName + " lamp dialogue mismatch; declined with '" + text + "'");
                EventDismissPlusEventLog.append(npcName,
                        EventDismissPlusEventLog.Action.DECLINE,
                        EventDismissPlusEventLog.Outcome.OK,
                        "lamp dialogue fallback, declined with '" + text + "'");
                Rs2Dialogue.clickOption(text);
                Global.sleep(Rs2Random.between(400, 900));
                advanceDialogueClicks(4);
                return;
            }
        }
        Microbot.log("EventDismissPlus: " + npcName + " lamp dialogue exhausted and no decline option matched");
        EventDismissPlusEventLog.append(npcName,
                EventDismissPlusEventLog.Action.DECLINE,
                EventDismissPlusEventLog.Outcome.ERROR,
                "lamp dialogue stuck, no decline option matched");
    }

    private Skill pickLampSkill() {
        // User override: when Force lamp skill is set to a specific skill, use it directly and
        // skip both auto-detect and the fallback. AUTO_DETECT (default) maps to null and uses
        // the auto-detect -> fallback path below.
        EventDismissPlusConfig.LampSkillOverride override = config.forceLampSkill();
        if (override != null && override.getSkill() != null) {
            return override.getSkill();
        }

        if (config.autoDetectLampSkill()) {
            Skill active = script.getActiveSkill();
            if (active != null) return active;
        }
        return config.fallbackLampSkill();
    }

    private void advanceDialogueClicks(int maxClicks) {
        int safety = Math.max(0, maxClicks);
        while (safety-- > 0) {
            if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
                Global.sleep(Rs2Random.between(400, 900));
                continue;
            }
            // Mysterious Old Man's Maze variant interrupts the gift flow with a two-option
            // dialog ("Sure, I like exploring mazes" / "Sorry, I'm busy"). Decline politely;
            // the Maze itself is not handled. Other accept-and-acknowledge events (Sandwich
            // Lady, Drunken Dwarf, Rick Turpentine, Dr Jekyll, Frog Prince) don't use that
            // option text, so this branch is Maze-specific in practice.
            if (Rs2Dialogue.hasDialogueOption("Sorry, I'm busy")) {
                Rs2Dialogue.clickOption("Sorry, I'm busy");
                Microbot.log("EventDismissPlus: declined Maze prompt");
                // Log the Maze decline separately so analytics can distinguish the Old Man
                // gift variant (ENGAGE) from the Maze variant (DECLINE).
                EventDismissPlusEventLog.append("Mysterious Old Man",
                        EventDismissPlusEventLog.Action.DECLINE,
                        EventDismissPlusEventLog.Outcome.OK,
                        "Maze prompt");
                Global.sleep(Rs2Random.between(400, 900));
                continue;
            }
            // No actionable state -- dialogue ended.
            break;
        }
    }

    private void dismiss(Rs2NpcModel npc) {
        npc.click("Dismiss");
        Global.sleepUntil(() -> getRandomEventNpc() == null, 3000);
    }
}
