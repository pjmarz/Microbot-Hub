package net.runelite.client.plugins.microbot.microbotdashboardplus.data;

import net.runelite.api.Skill;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks per-skill XP history to compute deltas and rolling XP/hr rates.
 *
 * <p>For each skill, maintains:
 * <ul>
 *     <li>The XP value observed at page-load time (baseline for "Δ since session start")</li>
 *     <li>A bounded deque of (timestamp, xp) samples within the rolling window
 *         (default 5 minutes) used to compute XP/hr</li>
 * </ul>
 *
 * <p>Thread-safety: not synchronized internally. Callers (the poller) must
 * serialize {@link #record(Skill, int)} calls. UI reads via the snapshot, not
 * directly.
 */
public class XpHistory {

    /** Retention window for the sample deque. Long enough to feed the 24h chart. */
    public static final long RETENTION_WINDOW_MS = 24 * 60 * 60 * 1000L; // 24 h
    /** Inner window used for XP/hr rate calculation. */
    public static final long RATE_WINDOW_MS = 5 * 60 * 1000L; // 5 min

    private final Map<Skill, Integer> baselineXp = new EnumMap<>(Skill.class);
    private final Map<Skill, Deque<Sample>> samplesBySkill = new EnumMap<>(Skill.class);

    /**
     * Record an XP observation. First call per skill establishes the baseline.
     */
    public void record(Skill skill, int currentXp) {
        baselineXp.putIfAbsent(skill, currentXp);

        long now = System.currentTimeMillis();
        Deque<Sample> samples = samplesBySkill.computeIfAbsent(skill, k -> new ArrayDeque<>());
        samples.addLast(new Sample(now, currentXp));

        // Trim samples beyond the long retention window (keeps chart data
        // available; XP/hr filters internally for the rate window).
        long cutoff = now - RETENTION_WINDOW_MS;
        while (!samples.isEmpty() && samples.peekFirst().timestampMillis < cutoff) {
            samples.pollFirst();
        }
    }

    /** XP gained since the first observation for this skill. */
    public int deltaSinceBaseline(Skill skill, int currentXp) {
        Integer baseline = baselineXp.get(skill);
        return baseline == null ? 0 : Math.max(0, currentXp - baseline);
    }

    /**
     * Extrapolated XP/hr based on the {@link #RATE_WINDOW_MS} rolling window.
     * Returns 0 if fewer than 2 samples in that window or if no XP has been
     * gained in it.
     */
    public int xpPerHour(Skill skill) {
        Deque<Sample> samples = samplesBySkill.get(skill);
        if (samples == null || samples.size() < 2) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long rateCutoff = now - RATE_WINDOW_MS;

        Sample first = null;
        for (Sample s : samples) {
            if (s.timestampMillis >= rateCutoff) {
                first = s;
                break;
            }
        }
        if (first == null) return 0;

        Sample last = samples.peekLast();
        long elapsedMs = last.timestampMillis - first.timestampMillis;
        if (elapsedMs <= 0) return 0;

        int xpDelta = last.xp - first.xp;
        if (xpDelta <= 0) return 0;

        return (int) ((xpDelta * 3_600_000.0) / elapsedMs);
    }

    /** Reset all tracking. Used on plugin reload or "Clear" action. */
    public void reset() {
        baselineXp.clear();
        samplesBySkill.clear();
    }

    /**
     * Snapshot of all samples for a skill, oldest first. Used by the XP chart
     * for time-series rendering. Returned list is a defensive copy.
     */
    public java.util.List<SamplePoint> getSamples(Skill skill) {
        Deque<Sample> samples = samplesBySkill.get(skill);
        if (samples == null) return java.util.Collections.emptyList();
        java.util.List<SamplePoint> out = new java.util.ArrayList<>(samples.size());
        for (Sample s : samples) {
            out.add(new SamplePoint(s.timestampMillis, s.xp));
        }
        return out;
    }

    /** Baseline XP for a skill (the first observation we ever recorded). 0 if none. */
    public int baselineFor(Skill skill) {
        Integer b = baselineXp.get(skill);
        return b == null ? 0 : b;
    }

    private static final class Sample {
        final long timestampMillis;
        final int xp;

        Sample(long timestampMillis, int xp) {
            this.timestampMillis = timestampMillis;
            this.xp = xp;
        }
    }

    /** Public projection of an XP sample for chart use. */
    public static final class SamplePoint {
        public final long timestampMillis;
        public final int xp;

        public SamplePoint(long timestampMillis, int xp) {
            this.timestampMillis = timestampMillis;
            this.xp = xp;
        }
    }
}
