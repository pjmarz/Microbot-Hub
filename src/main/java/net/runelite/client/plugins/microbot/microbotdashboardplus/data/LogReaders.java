package net.runelite.client.plugins.microbot.microbotdashboardplus.data;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Disk readers for the EventDismissPlus events CSV and the agent-server
 * watchdog CSV.
 *
 * <p>Both files live under {@code ~/.runelite/}. EventDismissPlus writes
 * {@code eventdismissplus-events.csv}; the PowerShell {@code watchdog.ps1}
 * writes {@code microbot-watchdog.csv}.
 *
 * <p>Readers cache results keyed by file modification time so the poller can
 * call them on every tick without re-parsing unchanged files.
 *
 * <p>Schemas:
 * <pre>
 *   eventdismissplus-events.csv:
 *     timestamp,event_name,action,outcome,note
 *     2026-05-24T01:09:22Z,Miles,DISMISS,OK,
 *
 *   microbot-watchdog.csv  (UTF-8 with BOM):
 *     timestamp,reason,details
 *     2026-05-25T09:27:04-04:00,WATCHDOG_START,Polling every 60s
 *     2026-05-25T09:32:18-04:00,STALE_RESPONSE,Client hung 312s
 * </pre>
 */
@Slf4j
public final class LogReaders {

    public static final Path EVENT_DISMISS_PATH = Paths.get(
            System.getProperty("user.home"), ".runelite", "eventdismissplus-events.csv");

    public static final Path WATCHDOG_PATH = Paths.get(
            System.getProperty("user.home"), ".runelite", "microbot-watchdog.csv");

    private static final DateTimeFormatter HUMAN_TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // Cache.
    private long eventDismissMtime = -1L;
    private List<PollSnapshot.EventDismissStatRow> eventDismissCache = Collections.emptyList();

    private long watchdogMtime = -1L;
    private PollSnapshot.WatchdogStatus watchdogCache = PollSnapshot.WatchdogStatus.builder()
            .status("unavailable").lastEventText("--").lastRestartText("--").totalRestarts(0)
            .lastSeenText("--").uptimeText("--").build();

    /**
     * Last-known watchdog instants, cached so that "last seen" and "uptime"
     * keep counting up on every poll even when the file has not changed (the
     * mtime cache short-circuits before re-parsing, so we recompute the human
     * text from these on each call).
     */
    private Instant lastWatchdogLineInstant = null;
    private Instant lastWatchdogStartInstant = null;

    // -----------------------------------------------------------------
    // EventDismiss
    // -----------------------------------------------------------------

    public List<PollSnapshot.EventDismissStatRow> readEventDismissStats() {
        try {
            if (!Files.exists(EVENT_DISMISS_PATH)) return Collections.emptyList();
            long mtime = Files.getLastModifiedTime(EVENT_DISMISS_PATH).toMillis();
            if (mtime == eventDismissMtime) return eventDismissCache;

            List<String> lines = Files.readAllLines(EVENT_DISMISS_PATH);
            if (lines.isEmpty()) return Collections.emptyList();

            // Aggregate: eventName -> [engaged, dismissed, declined, errors]
            Map<String, int[]> agg = new LinkedHashMap<>();
            boolean firstLine = true;
            for (String line : lines) {
                if (firstLine) { firstLine = false; continue; } // header
                if (line.isEmpty()) continue;
                String[] cols = parseCsvRow(line);
                if (cols.length < 4) continue;
                String eventName = cols[1];
                String action = cols[2];
                String outcome = cols[3];

                int[] counts = agg.computeIfAbsent(eventName, k -> new int[4]);
                if ("ERROR".equalsIgnoreCase(outcome)) counts[3]++;
                else if ("ENGAGE".equalsIgnoreCase(action)) counts[0]++;
                else if ("DISMISS".equalsIgnoreCase(action)) counts[1]++;
                else if ("DECLINE".equalsIgnoreCase(action)) counts[2]++;
            }

            List<PollSnapshot.EventDismissStatRow> rows = new ArrayList<>();
            for (Map.Entry<String, int[]> e : agg.entrySet()) {
                int[] c = e.getValue();
                rows.add(PollSnapshot.EventDismissStatRow.builder()
                        .eventName(e.getKey())
                        .engaged(c[0])
                        .dismissed(c[1])
                        .declined(c[2])
                        .errors(c[3])
                        .build());
            }
            rows.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));

            eventDismissMtime = mtime;
            eventDismissCache = Collections.unmodifiableList(rows);
            return eventDismissCache;
        } catch (IOException ex) {
            log.debug("readEventDismissStats failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    // -----------------------------------------------------------------
    // Watchdog
    // -----------------------------------------------------------------

    public PollSnapshot.WatchdogStatus readWatchdog() {
        try {
            if (!Files.exists(WATCHDOG_PATH)) {
                lastWatchdogLineInstant = null;
                lastWatchdogStartInstant = null;
                return PollSnapshot.WatchdogStatus.builder()
                        .status("unavailable").lastEventText("no log file").lastRestartText("--")
                        .totalRestarts(0).lastSeenText("--").uptimeText("--").build();
            }
            long mtime = Files.getLastModifiedTime(WATCHDOG_PATH).toMillis();
            // File unchanged since last parse: reuse the parsed fields but
            // recompute the live "last seen" / "uptime" text so they keep
            // ticking up between watchdog writes.
            if (mtime == watchdogMtime) {
                watchdogCache = withLiveText(watchdogCache);
                return watchdogCache;
            }

            List<String> lines = Files.readAllLines(WATCHDOG_PATH);
            if (lines.isEmpty()) {
                watchdogMtime = mtime;
                lastWatchdogLineInstant = null;
                lastWatchdogStartInstant = null;
                watchdogCache = PollSnapshot.WatchdogStatus.builder()
                        .status("unavailable").lastEventText("empty log").lastRestartText("--")
                        .totalRestarts(0).lastSeenText("--").uptimeText("--").build();
                return watchdogCache;
            }

            int totalRestarts = 0;
            String lastRestart = "--";
            String lastEventText = "--";
            String latestReason = null;
            Instant latestInstant = null;
            Instant latestStartInstant = null;

            boolean firstLine = true;
            for (String line : lines) {
                if (firstLine) { firstLine = false; continue; }
                // Strip BOM from the very first data line if present.
                if (line.startsWith("﻿")) line = line.substring(1);
                if (line.isEmpty()) continue;

                String[] cols = parseCsvRow(line);
                if (cols.length < 2) continue;
                String ts = cols[0];
                String reason = cols[1];
                String details = cols.length > 2 ? cols[2] : "";

                Instant parsed = parseLooseInstant(ts);
                if (parsed != null) {
                    latestInstant = parsed;
                    latestReason = reason;
                    lastEventText = HUMAN_TIME.format(parsed) + " - " + reason + (details.isEmpty() ? "" : " (" + details + ")");
                }
                if ("WATCHDOG_START".equalsIgnoreCase(reason)) {
                    if (parsed != null) latestStartInstant = parsed;
                } else {
                    totalRestarts++;
                    if (parsed != null) {
                        lastRestart = HUMAN_TIME.format(parsed);
                    }
                }
            }

            String status;
            if (latestInstant == null) {
                status = "unavailable";
            } else {
                Duration sinceLatest = Duration.between(latestInstant, Instant.now());
                boolean isStart = "WATCHDOG_START".equalsIgnoreCase(latestReason);
                if (isStart && sinceLatest.toMinutes() < 5) status = "ok";
                else if (isStart) status = "warn";
                else if (sinceLatest.toMinutes() < 5) status = "bad"; // recent non-start = recent restart
                else status = "warn";
            }

            watchdogMtime = mtime;
            lastWatchdogLineInstant = latestInstant;
            lastWatchdogStartInstant = latestStartInstant;
            watchdogCache = withLiveText(PollSnapshot.WatchdogStatus.builder()
                    .status(status)
                    .lastEventText(lastEventText)
                    .lastRestartText(lastRestart)
                    .totalRestarts(totalRestarts)
                    .lastSeenText("--")
                    .uptimeText("--")
                    .build());
            return watchdogCache;
        } catch (IOException ex) {
            log.debug("readWatchdog failed: {}", ex.getMessage());
            return watchdogCache;
        }
    }

    /**
     * Recompute the live "last seen" and "uptime" text from the cached
     * instants. Called on every poll so the values advance even when the log
     * file has not been touched since the previous read.
     *
     * <p>If the latest line is older than the staleness window the status is
     * downgraded to "bad": the watchdog process itself looks stopped.
     */
    private PollSnapshot.WatchdogStatus withLiveText(PollSnapshot.WatchdogStatus base) {
        if (base == null) return null;
        Instant now = Instant.now();

        String lastSeen = "--";
        String status = base.getStatus();
        if (lastWatchdogLineInstant != null) {
            Duration since = Duration.between(lastWatchdogLineInstant, now);
            lastSeen = humanizeAgo(since);
            // No fresh line for over 5 minutes: the watchdog looks dead.
            if (since.toMinutes() >= 5 && !"unavailable".equals(status)) {
                status = "bad";
            }
        }

        String uptime = "--";
        if (lastWatchdogStartInstant != null) {
            uptime = humanizeSpan(Duration.between(lastWatchdogStartInstant, now));
        }

        return PollSnapshot.WatchdogStatus.builder()
                .status(status)
                .lastEventText(base.getLastEventText())
                .lastRestartText(base.getLastRestartText())
                .totalRestarts(base.getTotalRestarts())
                .lastSeenText(lastSeen)
                .uptimeText(uptime)
                .build();
    }

    /** "12s ago", "3m ago", "2h ago". Never negative. */
    private static String humanizeAgo(Duration d) {
        if (d == null || d.isNegative()) return "just now";
        long secs = d.getSeconds();
        if (secs < 60) return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        return hours + "h " + (mins % 60) + "m ago";
    }

    /** "2h 05m", "8m", "45s". Never negative. */
    private static String humanizeSpan(Duration d) {
        if (d == null || d.isNegative()) return "--";
        long secs = d.getSeconds();
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        if (mins < 60) return mins + "m";
        long hours = mins / 60;
        return String.format("%dh %02dm", hours, mins % 60);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Minimal CSV row parser: handles double-quoted fields with embedded commas + escaped quotes. */
    private static String[] parseCsvRow(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    /**
     * Watchdog file uses PowerShell offset timestamps; EventDismiss uses Java {@code Instant.toString()}.
     * Try both.
     */
    private static Instant parseLooseInstant(String text) {
        if (text == null || text.isEmpty()) return null;
        try { return OffsetDateTime.parse(text).toInstant(); } catch (Exception ignored) {}
        try { return Instant.parse(text); } catch (Exception ignored) {}
        try {
            DateTimeFormatter local = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
                    .toFormatter();
            return LocalDateTime.parse(text, local).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {}
        return null;
    }
}
