package net.runelite.client.plugins.microbot.eventdismissplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Background poller for EventDismissPlus. Two responsibilities:
 * <ol>
 *   <li>Track XP gain per skill over a 30-second rolling window so the NPC handler can pick
 *       the "active skill" to apply Genie lamps to.</li>
 *   <li>Maintain session counters (events handled, last event name, last event time) for the
 *       overlay panel.</li>
 * </ol>
 *
 * <p>The script doesn't drive any in-game actions itself -- it's a passive observer. The
 * {@link net.runelite.client.plugins.microbot.eventdismissplus.events.RandomEventNpcHandler}
 * and {@link net.runelite.client.plugins.microbot.eventdismissplus.events.StrangePlantHandler}
 * BlockingEvents are what fire when a random event spawns.
 */
@Slf4j
public class EventDismissPlusScript extends Script {

    private static final long WINDOW_MILLIS = 30_000L; // 30-second rolling window

    // Skill XP at the start of the current rolling window
    private final Map<Skill, Integer> windowStartXp = new EnumMap<>(Skill.class);
    // Latest XP delta within the window
    private final Map<Skill, Integer> recentXpDelta = new EnumMap<>(Skill.class);
    private long windowStartTime = 0;

    // Session counters (read by overlay)
    private int eventsHandled = 0;
    private String lastEventName = null;
    private long lastEventTime = 0;
    private long startTimeMillis = 0;

    public int getEventsHandled() { return eventsHandled; }
    public String getLastEventName() { return lastEventName; }
    public long getLastEventTime() { return lastEventTime; }
    public long getStartTimeMillis() { return startTimeMillis; }

    /**
     * Called by the BlockingEvent handlers after they finish handling an event.
     */
    public void recordEventHandled(String name) {
        eventsHandled++;
        lastEventName = name;
        lastEventTime = System.currentTimeMillis();
        Microbot.log("EventDismissPlus: handled " + name + " (session total: " + eventsHandled + ")");
    }

    public boolean run(EventDismissPlusConfig config) {
        startTimeMillis = System.currentTimeMillis();
        windowStartTime = startTimeMillis;
        recentXpDelta.clear();
        windowStartXp.clear();
        eventsHandled = 0;
        lastEventName = null;
        lastEventTime = 0;

        seedWindowSnapshot();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                long now = System.currentTimeMillis();
                // Roll the window when it expires (clears stale deltas)
                if (now - windowStartTime >= WINDOW_MILLIS) {
                    seedWindowSnapshot();
                    windowStartTime = now;
                }

                // Update deltas for all skills (skip OVERALL which is a derived total)
                for (Skill skill : Skill.values()) {
                    if (skill == Skill.OVERALL) continue;
                    Integer start = windowStartXp.get(skill);
                    if (start == null) continue;
                    int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getSkillExperience(skill)).orElse(start);
                    recentXpDelta.put(skill, currentXp - start);
                }
            } catch (Exception ex) {
                Microbot.log("EventDismissPlusScript tick error: " + ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        return true;
    }

    private void seedWindowSnapshot() {
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;
            int currentXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getClient().getSkillExperience(skill)).orElse(0);
            windowStartXp.put(skill, currentXp);
            recentXpDelta.put(skill, 0);
        }
    }

    /**
     * The skill with the largest positive XP delta in the current rolling window.
     * Returns null if no skill gained XP in the window (e.g. bot just started, or player is
     * AFK between actions).
     */
    public Skill getActiveSkill() {
        Skill best = null;
        int bestDelta = 0;
        for (Map.Entry<Skill, Integer> entry : recentXpDelta.entrySet()) {
            if (entry.getValue() > bestDelta) {
                bestDelta = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }
}
