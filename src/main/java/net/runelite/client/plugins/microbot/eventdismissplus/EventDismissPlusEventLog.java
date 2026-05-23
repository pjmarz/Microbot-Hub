package net.runelite.client.plugins.microbot.eventdismissplus;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * v0.2.0: append-only CSV log of every random event EventDismissPlus handles.
 *
 * <p>File location: {@code ~/.runelite/eventdismissplus-events.csv}.
 *
 * <p>Schema:
 * <pre>
 * timestamp,event_name,action,outcome,note
 * 2026-05-23T22:14:07Z,Mysterious Old Man,ENGAGE,OK,
 * 2026-05-23T22:18:55Z,Freaky Forester,DISMISS,OK,
 * 2026-05-23T22:23:00Z,Mysterious Old Man,DECLINE,OK,Maze prompt
 * 2026-05-23T22:30:11Z,Genie,DISMISS,OK,random skip (antiban roll)
 * </pre>
 *
 * <p>Foundation for v0.3.0+ analytics: drop-rate empirical study, engagement-vs-dismiss
 * pattern verification, frequency-of-events over long soak. CSV chosen over JSON for
 * trivial downstream tooling (pandas, Excel, jq).
 *
 * <p>Thread-safe: {@link #append} synchronizes on the class so concurrent BlockingEvent
 * handlers (NPC handler + Strange Plant handler) can't interleave their writes.
 *
 * <p>IO errors are logged via slf4j and swallowed. Failing to write the log must NOT
 * crash event handling.
 */
@Slf4j
public final class EventDismissPlusEventLog {

    private static final Path FILE_PATH = Paths.get(
            System.getProperty("user.home"),
            ".runelite",
            "eventdismissplus-events.csv");

    private static final String HEADER = "timestamp,event_name,action,outcome,note";

    private static boolean headerEnsured = false;

    private EventDismissPlusEventLog() {}

    public enum Action {
        /** Engaged with the event (dialogue accept, item pickup, etc.). */
        ENGAGE,
        /** Dismissed via the universal {@code npc.click("Dismiss")} action. */
        DISMISS,
        /** Declined a sub-prompt within the event (e.g. Old Man Maze "Sorry, I'm busy"). */
        DECLINE
    }

    public enum Outcome {
        /** Action completed without exception. */
        OK,
        /** Action threw an exception or failed mid-flow. */
        ERROR
    }

    /**
     * Append one row to the log file. Safe to call from any thread. Swallows IO errors.
     */
    public static synchronized void append(String eventName, Action action, Outcome outcome, String note) {
        try {
            ensureHeader();
            String line = String.format(
                    "%s,%s,%s,%s,%s%n",
                    Instant.now().toString(),
                    csvEscape(eventName),
                    action.name(),
                    outcome.name(),
                    csvEscape(note == null ? "" : note));
            Files.writeString(FILE_PATH, line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            log.warn("EventDismissPlus event log write failed: {}", ex.getMessage());
        }
    }

    private static void ensureHeader() throws IOException {
        if (headerEnsured) return;
        Files.createDirectories(FILE_PATH.getParent());
        if (!Files.exists(FILE_PATH) || Files.size(FILE_PATH) == 0) {
            Files.writeString(FILE_PATH, HEADER + System.lineSeparator(),
                    StandardOpenOption.CREATE);
        }
        headerEnsured = true;
    }

    /**
     * RFC 4180-flavoured CSV escaping. Quote-wraps fields containing comma, quote, or
     * newline; doubles embedded quotes.
     */
    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
