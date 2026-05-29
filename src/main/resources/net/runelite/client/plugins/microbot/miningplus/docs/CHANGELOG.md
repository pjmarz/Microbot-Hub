# AutoMiningPlus Changelog

Auto-walking-and-mining "Plus" fork of upstream AutoMining. Part of the Skill Plus Template (SPT) lineage; see `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo for shared conventions.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: Semver-flavored (`0.x` = pre-stable, `1.0.0` = upstream-PR ready).

## [0.5.8] — 2026-05-28

Mining Guild F2P correction. Closes the v0.4.0 deferred verification ("Live-verification deferred until a P2P account with 60 Mining is available") now that a 60-Mining account is available. Live-verified via RuneLite dev-tool tile hovers + the agent server `/state` endpoint.

### Fixed

- **Mining Guild anchor pointed at a door, not the rocks.** All Mining Guild entries used coord `(3046, 9756)` and were flagged members-only with display "Mining Guild (P2P 60)". Live verification found `(3046, 9756)` is the **chamber-divider door**, ~16 tiles north of the iron/coal field. Selecting Mining Guild routed the walker to the door and it never reached ore. Classic stale-seed coord (same shape as the v0.4.0 Mining Guild bank + Al Kharid Arena bugs).
- **Per-ore anchors corrected to verified F2P tiles:**
  - Iron → `(3028, 9737)` (chamber 1, south of door)
  - Coal → `(3045, 9741)` (chamber 1) — the premier F2P coal spot, 37 rocks
  - Mithril → `(3037, 9773)` (chamber 2, north of door)
  - Adamantite → `(3042, 9772)` (chamber 2; needs 70 Mining to actually mine)
- **`membersOnly` flipped to false** on the four F2P entries. The only gate is 60 Mining (the F2P Mining Guild exists and requires no membership). Display name "Mining Guild (P2P 60)" → "Mining Guild (60)".
- **Rock counts corrected** to F2P-actual (4 iron / 37 coal / 5 mithril / 2 adamant) from the inflated members-combined numbers the entries carried (8 / 57 / 15 / 10).

### Known limitations / test items

- **Mithril and adamant are in chamber 2, behind the door at `(3046, 9756)`.** The web walker must open and path through that door from the chamber-1 entry ladder. Iron and coal don't have this dependency (same chamber as the ladder). Confirm Rs2Walker handles the door during the soak.
- **F2P banking has no in-guild bank.** The walker climbs the ladder `(3020, 9740)→(3020, 3339)` and banks at Falador East `(3015, 3354)`. Use `AUTO_NEAREST`; confirm the multi-region route resolves.
- **The runite Mining Guild entry is left untouched** (old coord, members flag). Runite isn't in the F2P area, and the members guild's runite presence is unverifiable without a members account. Flagged in `MiningRockLocations` audit log for a future members-side pass.

## [0.5.7] — 2026-05-23

### Fixed
- **Pause now actually stops the bot.** v0.5.6 fixed the button click registration, but the script kept walking to bank after pause was clicked. Symptom (caught during Pete's soak): button text toggles correctly to "Resume", `Microbot.pauseAllScripts` flips to true, script's main 100ms tick polls the flag and returns early — but the WebWalker runs on a separate executor and doesn't honor `pauseAllScripts`. In-flight walks continue to completion.
- Root cause: missing `Rs2Walker.setTarget(null)` call in the pause-on branch of the overlay click handler. AIO Fighter source (`AIOFighterInfoOverlay.java:39`) calls it explicitly. We copied the boolean toggle and text update during the v0.5.1 borrow but missed the walker interrupt.
- Diagnosis approach: source-diff against AIO Fighter (the known-working precedent) before any speculative fixes. Found the missing line on first read. Total time from bug report to fix-in-source: ~15 min, vs. the 5 wrong theories of v0.5.1-v0.5.5.

### Added
- `Microbot.pauseAllScripts.compareAndSet(true, false)` in both `startUp()` and `shutDown()`. Lifecycle hygiene that matches ~30 other Hub plugins. Without it, a paused session could leak state into the next plugin Pete enables.

## [0.5.6] — 2026-05-23

### Fixed
- **Pause button now actually works.** v0.5.1 introduced the overlay button but clicks passed through to the game world for 5 iterations. Root cause: `ButtonComponent.setOnClick(lambda)` only stores the lambda; the parent Plugin must explicitly call `overlay.pauseButton.hookMouseListener()` after `overlayManager.add()` to wire it into RuneLite's mouse dispatch. AIO Fighter's source (`AIOFighterPlugin.startUp()` lines 143-144) revealed the missing call.
- Field visibility: `pauseButton` changed from `private final` to `public final` so the Plugin can call `hookMouseListener()`.

### Changed
- Button text reads "Resume" (not "Unpause") when paused (Pete's preference).

## [0.5.1 - 0.5.5] — 2026-05-22/23 (dead-end iterations)

Five intermediate versions chasing the same click-through bug. Kept here for posterity; nothing else from these versions ships in v0.5.6. The journey:
- **v0.5.1**: refactored Pause from config checkbox to overlay button. Button rendered but didn't accept clicks.
- **v0.5.2**: removed `panelComponent.getChildren().clear()` from render (was wiping click registry, allegedly). Still broken.
- **v0.5.3**: added diagnostic log to onClick handler. No log fired → click never reached the handler.
- **v0.5.4**: moved button add outside conditional render branch (race-condition theory). Still broken.
- **v0.5.5**: stripped overlay to minimal Title + Button (ruled out render complexity). Still broken.
- **v0.5.6**: found `hookMouseListener()` requirement in AIO Fighter source. Fixed.

## [0.5.0] — 2026-05-22

## [0.5.0] — 2026-05-22

Cross-plugin v0.5.0: Target Level + Pause shipped across all 4 Plus plugins.

### Added
- `targetLevel` config (int, default 0 = disabled). When Mining level reaches the target, the script runs one cleanup pass (bank if `useBank` on, drop otherwise) then shuts down. Uses a `shutdownAfterCleanup` flag intercepted before state-flip-back in the RESETTING branch.
- `paused` config (boolean, default false). Tick early-exits without resetting session metrics. Toggle on/off without restarting the plugin.
- Overlay shows `Target: X (Y to go)` when `targetLevel > 0`; status shows `[PAUSED]` when paused.

## [0.4.2] — 2026-05-22

### Changed
- Default `itemsToBank` expanded via OSRS Wiki Mining/Mineable_items sweep: `"ore, uncut, coal, clay, basalt, essence, ash, shard, geode, salt, limestone, granite, sandstone, amethyst, pay-dirt"`. Forward-compat for Tier B Rocks expansion (BLURITE/BARRONITE/etc.) when those Rocks enum entries eventually ship.

## [0.4.1] — 2026-05-22

### Fixed
- Coal/Clay/Basalt deposit gotcha. Mining coal at Al Kharid with default settings caused a bank↔mine oscillation. Default `itemsToBank = "ore"` didn't match `"Coal"` (no "ore" suffix), so the deposit predicate matched zero items, bank closed empty, walker returned to find inventory still full. Loop forever.
- Script now auto-augments the deposit filter with the active rock's first word at deposit time: `activeRock.getName().split(" ")[0].toLowerCase()`. `Rocks.COAL.getName()` = `"coal rocks"` → first word `"coal"` → substring-matches `"Coal"`. Generic across all rocks.

### Changed
- Default `itemsToBank` expanded from `"ore"` to `"ore, uncut, coal, clay, basalt"` as belt-and-suspenders.

## [0.4.0] — 2026-05-22

Wiki-driven data expansion cycle Phase A. F2P-first re-audit of every location enum.

### Fixed
- `MINING_GUILD_FALADOR` bank coord was `(3013, 3355, 0)` — IDENTICAL to `FALADOR_EAST`. Stale-seed bug. Picking "Mining Guild" silently routed the walker to Falador East's booth. New coord `(3046, 9760, 0)` targets the underground bank chest. Live-verification deferred until a P2P account with 60 Mining is available.
- `AL_KHARID_ARENA` (Fadli's bank) coord was `(3315, 3242, 0)`, sending walker to a NW residential building far from the arena. Live-verified by Pete: corrected to `(3383, 3269, 0)` at the bank chest. Wiki Fadli page map confirms approximately 3382, 3270.

### Added
- `FEROX_ENCLAVE` bank entry (F2P, Wilderness gateway). Warning: Rs2Walker may path through lvl 1-2 Wilderness on approach.
- Rimmington Mine entry (F2P, 2 tin + 5 copper + 6 iron + 2 clay + 2 gold rocks per wiki — best F2P gold spot pre-Crafting Guild).
- Edgeville Dungeon Mine entry (F2P, 7 rock types). Low utility per wiki (monsters dense, far from bank); flagged in inline comments.
- Varrock Central (Horvik's, 2 anvils) + Varrock East (2 anvils south of bank) anvil entries to the shared `AnvilLocationOption`.
- New `tools/wiki-audit-banks.ps1` (was missing previously).
- Audit log entries on `MiningRockLocations.java` and `BankLocationOption.java`.

### Investigated and rejected (wiki proved initial assumptions wrong)
- Ferox Enclave has NO furnace (only bank/chapel/pub).
- Mining Guild F2P side has NO bank chest (only shops + rocks).
- No Al Kharid or Edgeville anvil exists in OSRS.
- Corsair Cove deposit box is P2P-gated (Corsair Curse quest); deferred to Phase B.

## [0.3.3] — 2026-05-21

### Added
- Fadli's Al Kharid Arena bank (initially with approximate coord; corrected at v0.4.0). F2P, ~75 tiles from Al Kharid Mine vs ~150 to south-city bank.

## [0.3.2] — 2026-05-21

### Changed
- Display names shortened across `BankLocationOption`, `MineLocationOption` to fit the RuneLite dropdown width without truncation. `(Members)` → `(P2P)`, "bank" suffix dropped where redundant.

## [0.3.1] — 2026-05-21

### Changed
- Switched from `Rs2GameObject.findReachableObject` (slow BFS reachability check per candidate, painful on crowded mines) to `Microbot.getRs2TileObjectCache().query().within(...).withName(...).nearestOnClientThread()`. Verified 82 ores in 10:23 = 12,522 XP/hr at Al Kharid iron post-switch.

## [0.3.0] — 2026-05-21

Polish-Cycle 2 (cross-plugin overlay + threshold work).

### Added
- Runtime stats overlay: uptime, Mining XP gained, XP/hr, ores mined, level + delta, status.
- `stopAfterMinutes` and `stopAfterXp` config items. Auto-shutdown thresholds.
- Per-plugin getter accessors on the Script (`getStartTimeMillis()`, `getStartSkillXp()`, etc.) for the overlay to read.

## [0.1.0 → 0.1.11] — 2026-05-14

Pilot #1 v0.1.x rapid-iteration cycle.

- 0.1.0: MVP — `mineLocation` dropdown + auto-walk to chosen mine.
- 0.1.1: UX polish — AUTO_BEST walks; deterministic picks; wrong-ore status warning.
- 0.1.2: Data fix — Lumbridge tin/copper ↔ mithril/adamantite swap (user-caught bug).
- 0.1.3: Full data audit via `tools/wiki-audit-mines.ps1` — 15 corrections; final 17 OK / 0 mismatch / 0 not found.
- 0.1.4: Bank routing — `BankLocationOption` dropdown with 17 banks.
- 0.1.5: Removed 5-tile anchor cap (caused yo-yo walking between rocks).
- 0.1.6: State-gated anchor check (`ensureConfiguredLocation` skipped during RESETTING).
- 0.1.7: Startup state — auto-flip to RESETTING if inv is full at toggle.
- 0.1.8/9: Speed mode — `Rs2AntibanSettings.antibanEnabled = false` single-flag toggle.
- 0.1.10: Pace — `waitForXpDrop` replaced with `sleepUntil(isAnimating, 1200)`.
- 0.1.11: Anchor fix — `initialPlayerLocation` re-pinned to `activeLocation.getWorldPoint()` per tick.

## Recommended companion plugins

- **EventDismissPlus v0.1.0+** — global random event handler. Enable alongside AutoMiningPlus to auto-handle Genie lamps (auto-applied to Mining when you're mining), Sandwich Lady food drops, Strange Plant fruit pickup, and dismiss the rest with a human-like 2-5s delay. Without it, random events stall the mining loop and the bot looks more suspicious (no engagement = behavioral signal). See `eventdismissplus/docs/CHANGELOG.md`.

## Deferred / candidate work

- **v0.6.0 candidate**: Polish Cycle C — dual `walkBack` mode (`INITIAL_LOCATION` vs `LAST_LOCATION` ported from AutoWoodcuttingPlus). Track last productive rock; route bank-trip walk-back through `getReturnPoint(config)`.
- **v0.7.0+ candidate**: Tier B Rocks expansion. Add Rocks enum entries for walkable F2P + P2P content: BLURITE, BARRONITE, LIMESTONE, GRANITE, SANDSTONE, AMETHYST, DAEYALT. Each gets a Rocks entry + MineLocationOption + MiningRockLocations rows.
- **v0.8.0+ candidate**: Tier C teleport-gated rocks (RUNE_ESSENCE + PURE_ESSENCE). Adds Aubury teleport handling to the script.
- **v1.0.0 graduation**: ≥20 hours throwaway uptime; docs page; PR to `chsami/Microbot-Hub` `development` branch.
