# EventDismissPlus Changelog

Random event handling as a global companion plugin for any other Microbot Hub plugin. Pilot #5 of the Skill Plus Template (SPT) lineage. Forked from the upstream `eventdismiss/` plugin; see `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo for shared conventions.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

## [0.2.1] — 2026-05-26

Defensive fallback for stuck lamp dialogues. Closes a real bug Pete hit while fighting Hill Giants in the Edgeville Dungeon: Bee keeper random event triggered, the handler tried to claim a lamp via the Genie-style flow (Continue → "Yes please" → skill picker) but Bee keeper's modern dialogue uses a help/decline question with different option text. The safety loop in `handleLampDialogue` exhausted without matching anything. The NPC stayed on screen, `validate()` kept returning true, and `BlockingEventManager` re-fired the handler every ~4 seconds, logging "handled Bee keeper (session total: N)" while the NPC just stood there. 5 iterations in 16 seconds before Pete intervened.

### Fixed

- **Lamp-dialogue safety loop falls through to common decline options.** New `tryDeclineFallback(String npcName)` method called from `handleLampDialogue` when the existing "Yes please" / skill-picker pattern doesn't match. Tries a list of decline phrasings ("Sorry, but I'd rather not help", "Sorry, I'm busy", "Buzz off", "I don't want to", "I'm too busy", "No thanks", "No, thank you") and clicks the first match. Same shape as the v0.1.1 Mysterious Old Man Maze fix, generalized across more events.
- **`handleLampDialogue` now takes the NPC name** as a parameter so the fallback path can log which event triggered it. Caller in `engage()` updated.
- **CSV log records lamp-fallback events** as `DECLINE` action / `OK` outcome with note `"lamp dialogue fallback, declined with '<text>'"`. Lets analytics distinguish lamp-fallback declines from regular Maze declines and from successful engagements.
- **Hard miss logged as `DECLINE`/`ERROR`** with note `"lamp dialogue stuck, no decline option matched"`. The signal to widen the decline-text catalog or add proper engagement for the affected event.

### Notes

- **Generalizes beyond Bee keeper.** Count Check, Genie, and any future lamp-event whose dialogue doesn't fit the Genie pattern benefit from the same fallback. Belt-and-suspenders against Jagex shuffling dialogue text on us.
- **Out of scope**: real Bee keeper engagement. The exact option text and reward type aren't in the catalog yet. v0.3.0 OCR/dialogue audit (already on the roadmap) could nail down the proper engage path; for now defensive decline is correct behavior. Declining a random event still satisfies "engaged with the random" from an antiban perspective (the detection signal is "never interacts", not "always accepts").

## [0.2.0] — 2026-05-23

Light polish minor: behavioral variability + analytics foundation + catalog hygiene. Heavier event engagement (Freaky Forester full quest, Prison Pete balloon-animal, OCR for Quiz Master / Mime) deferred to v0.3.0 as the "complex events" minor.

### Added

- **Random skip chance** (`globalSkipChance` config, default 0). Probability 0-100 of skipping engagement and just dismissing, even when the per-event toggle is ON. Real humans don't engage every random event; some get skipped due to being busy / focused on the main activity. Antiban depth without per-event slider clutter. Roll is independent per-event, logged with the actual roll value for verifiability (`random skip (antiban roll 47/30)` log line means the 47 roll passed the threshold of 30, so engage proceeded).
- **CSV event log** at `~/.runelite/eventdismissplus-events.csv`. Append-only, thread-safe, includes:
  - Timestamp (ISO-8601 UTC)
  - Event name (e.g. "Mysterious Old Man", "Freaky Forester", "Niles")
  - Action: `ENGAGE`, `DISMISS`, or `DECLINE`
  - Outcome: `OK` or `ERROR`
  - Note: free-text (e.g. "Maze prompt", "random skip (antiban roll)", exception message on error)
  Foundation for v0.3.0+ analytics: drop-rate empirical study, frequency-of-events over long soak, engagement-vs-dismiss validation, error-rate triage.
- **Catalog entries for Mime event NPCs**: `MIME_NILES`, `MIME_MILES`, `MIME_GILES` (Niles + Miles + Giles). Pete encountered Niles during soak 2026-05-23; default-dismiss handled it cleanly. Catalog entry documents the name so it's not "unknown" in code reviews. Full engagement (mimic 5 emotes) requires animation recognition and is v0.3.0+ scope.

### Changed

- Maze decline (introduced v0.1.1) now logs to the CSV file as `DECLINE` action with note "Maze prompt". Analytics can distinguish Old Man gift variant (`ENGAGE`) from Maze variant (`DECLINE`) for empirical rate measurement.
- Exception handlers now log to CSV as `ERROR` outcome with the exception message in the note field. Easier post-soak triage.

### Notes for v0.2.0 → v0.3.0 transition

CSV log accumulates from this version on. After a week or two of soak data, the file becomes the empirical basis for v0.3.0 priorities: which events should we engage (high frequency / high reward), which should we keep dismissing (low frequency / low reward), which need new handlers (currently ERROR-rate or unknown-but-frequent).

## [0.1.1] — 2026-05-23

### Fixed

- **Mysterious Old Man Maze variant looped indefinitely.** Soak surfaced (2026-05-23): the bot clicked Talk-to, hit the Maze prompt's two-option dialog ("Sure, I like exploring mazes" / "Sorry, I'm busy"), then exited the dialogue handler because `advanceDialogueClicks` only knew how to click `Continue` buttons. The Old Man stayed standing, `validate()` re-fired, and the handler re-entered for another round. Chat log showed `handled Mysterious Old Man (session total: 5)`, then `... total: 6`, then `... total: 7` in 12 sec — three "successful handles" that didn't progress.
- Root cause: `advanceDialogueClicks` was a pure Continue-clicker. Maze variant interrupts the gift flow with options. v0.1.0 plan called this out as a known risk (`"Rarely teleports to Maze; if so, dismiss + log (Maze solver is v0.4.0)"`) but the decline path wasn't actually wired.

### Changed

- `advanceDialogueClicks` rewritten as a loop that handles both Continue clicks AND the "Sorry, I'm busy" option. When the Maze prompt fires, the handler picks "Sorry, I'm busy" and logs `EventDismissPlus: declined Maze prompt`. Other accept-and-acknowledge events (Sandwich Lady, Drunken Dwarf, Rick Turpentine, Dr Jekyll, Frog Prince) don't use that exact option text, so the new branch is Maze-specific in practice.

### Verification

Next time the Old Man's Maze variant fires during soak, expect a single `declined Maze prompt` log line + a clean exit. No more 3-in-12-sec loop. Bot continues its main script without manual intervention.

## [0.1.0] — 2026-05-22

Pilot #5 MVP. Tier B engagement scope.

### Added

- Random event handling using Microbot's `BlockingEvent` framework. Registers two handlers:
  - `RandomEventNpcHandler` — NPC-based events via `Rs2Npc.getRandomEventNPC()` (client maintains canonical random-event-NPC ID list; we don't hardcode).
  - `StrangePlantHandler` — GameObject id 32956 (Strange Plant) via the tile-object cache.
- Variable response delay (default 2000-5000 ms, configurable). Anti-detection — instant dismiss is a bot signal.
- High-value engagement with **10 events** (each with per-event toggle in config):
  - **Genie** — rub lamp, click skill picker, apply to active skill (auto-detected via XP delta tracking)
  - **Sandwich Lady** — accept food offering
  - **Drunken Dwarf** — accept beer + kebab
  - **Mysterious Old Man** — accept gift
  - **Strange Plant** — pick fruit for 30% energy restore
  - **Beekeeper** — claim XP lamp (same dialogue shape as Genie)
  - **Count Check** — claim XP lamp (requires Bank PIN on account)
  - **Frog Prince / Princess** — "Kiss" right-click for Frog Token
  - **Rick Turpentine** — accept loot (avg ~551 gp, rare crystal key half)
  - **Dr Jekyll** — accept potion (matches clean herb in inventory if available)
- Everything else dismissed via the universal `npc.click("Dismiss")` action (confirmed available on all random event NPCs post-2014 reform).
- Genie lamp **auto-detect active skill**: 30-second rolling window of `client.getSkillExperience(Skill.*)` deltas. The skill that gained the most XP recently is "active" — apply lamp there. Fallback skill configurable when auto-detect finds nothing.
- Overlay panel: events handled this session, last event name, time since last event.
- `data/RandomEventType.java` enum with Source-of-truth + Audit log + Known gaps citation header per template convention.
- `BlockingEventPriority.LOWEST` — Forestry events (WoodcuttingPlus) take precedence when both fire.

### Companion plugin

Enable EventDismissPlus alongside any other plugin (AutoMiningPlus, AutoSmeltingPlus, AutoSmithingPlus, AutoWoodcuttingPlus, AIO Fighter, etc.). The BlockingEvent framework interrupts whatever script is running when a random event fires, then resumes. No per-plugin integration needed.

## Deferred / candidate work

- **v0.2.0**: Prison Pete engagement (requires popping a balloon-animal mini-task to claim reward; wiki research surfaced complexity post-plan). Selective engagement ratios per event (e.g. 70% dismiss / 30% engage for low-value events). CSV/JSON event log to disk for later pattern analysis.
- **v0.3.0**: OCR-based solvers for Quiz Master + Surprise Exam.
- **v0.4.0**: teleport-event solvers (Pillory escape puzzle, Maze pathfinding, Evil Bob fishing task, Gravedigger coffin matching). Major work; only if Pete encounters them as real disruptions.
- **v1.0.0**: ≥20 hours uptime + docs + PR back to `chsami/Microbot-Hub`. Could replace upstream `eventdismiss/` entirely if maintainer accepts.

## Known gaps

- **Beekeeper NPC name spelling** not confirmed by research (wiki disambiguation page). We match both "Bee keeper" (two words per wiki) and "Beekeeper" (alt) via two enum entries. Smoke test resolves.
- **Dr. Jekyll spelling**: wiki shows "Dr Jekyll" (no period). We match both with/without.
- **Strange Plant action verb**: assumed "Pick" — if actual right-click is "Pick-fruit", the click silently fails. Logged warning on failure; smoke test resolves.
- **Genie skill-picker widget** child IDs may drift across RuneScape updates. v0.2.0 could add runtime widget-tree verification.
- **Active-skill detection lag**: 30-second rolling window. If you switch from Mining to Woodcutting mid-session, lamps may go to Mining for up to 30 sec after the switch. Acceptable.
- **Dialogue flow brittleness**: Genie/Beekeeper/Count Check dialogues are clicked through with a 12-step safety loop. Edge cases (e.g. "you don't have enough XP for this skill") may bail out without using the lamp. v0.1.1 patch if observed.
