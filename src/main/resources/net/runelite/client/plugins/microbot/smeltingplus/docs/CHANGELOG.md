# AutoSmeltingPlus Changelog

Auto-walking-and-smelting "Plus" fork of upstream AutoSmelting. Part of the Skill Plus Template (SPT) lineage; see `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

## [0.5.6] — 2026-05-23

### Fixed
- **Pause button now actually works.** v0.5.1 introduced the overlay button but clicks passed through to the game world. Root cause: missing `overlay.pauseButton.hookMouseListener()` call in `startUp()`. See AutoMiningPlusPlugin v0.5.6 CHANGELOG for the full diagnostic journey across v0.5.1-v0.5.5.
- Field visibility: `pauseButton` changed to `public final` so the Plugin can reach it.

### Changed
- Button text reads "Resume" (not "Unpause") when paused.

## [0.5.1 - 0.5.5] — dead-end iterations (see Mining CHANGELOG for detail).

## [0.5.0] — 2026-05-22

## [0.5.0] — 2026-05-22

Cross-plugin v0.5.0: Target Level + Pause shipped across all 4 Plus plugins.

### Added
- `targetLevel` config (int, default 0). When Smithing level reaches the target, the script runs one final deposit pass then shuts down. `shutdownAfterCleanup` flag intercepted after the deposit step in `handleBankAndWithdraw`, before re-withdrawing materials.
- `paused` config (boolean, default false). Tick early-exits without resetting `startTimeMillis` / `startSkillXp` / `actionsCompleted`. Toggle on/off without restarting.
- Overlay shows `Target: X (Y to go)` when `targetLevel > 0`; status shows `[PAUSED]` when paused.

## [0.4.0] — 2026-05-22

Wiki-driven data expansion Phase A (shared with MiningPlus / SmithingPlus since `BankLocationOption.java` is the shared dataset).

### Fixed
- `MINING_GUILD_FALADOR` bank coord collision with `FALADOR_EAST` — corrected to underground chest area `(3046, 9760, 0)`.
- `AL_KHARID_ARENA` (Fadli's bank) coord live-verified and corrected to `(3383, 3269, 0)`.

### Added
- `FEROX_ENCLAVE` bank entry (F2P).
- New `tools/wiki-audit-banks.ps1`.
- Audit log entries on `BankLocationOption.java`.

## [0.3.0] — 2026-05-21

Polish-Cycle 2 (cross-plugin overlay + threshold work).

### Added
- Runtime stats overlay: uptime, Smithing XP gained, XP/hr, smelt cycles, level + delta, status.
- `stopAfterMinutes` and `stopAfterXp` config items.
- Per-plugin getter accessors on the Script for the overlay to read.

## [0.3.1 / 0.3.2] — 2026-05-21

### Changed
- Display name shortening across location dropdowns to fit the RuneLite panel width.
- Fadli's Al Kharid Arena bank added at v0.3.2 (coord corrected at v0.4.0).

## [0.2.0] — 2026-05-15

Polish Cycle A. Borrows from AutoWoodcuttingPlus + AutoMiningPlus.

### Added
- CSV `itemsToBank` (default `"bar"`) and `itemsToKeep` (default `"coal bag, ring of forging, gauntlets of goldsmithing"`).
- `maxPlayersInArea` autohop for busy/PK furnaces.
- `leagueMode` arrow-key press to defeat idle-logout.
- `dropOrder` (`InteractOrder` enum) config item for cross-plugin parity (unused at v0.2.0; reserved).
- `progressiveSmelt` boolean. Auto-picks highest-tier bar where Smithing level + bank ore stock allow. Re-evaluated each bank trip.
- Goldsmith gauntlets + Ring of Forging auto-equip when bar is GOLD / IRON respectively.
- Coal bag auto-fill for Steel/Mithril/Adamantite/Runite on members worlds.

## [0.1.1] — 2026-05-14

### Fixed
- Phantom "Varrock West Furnace" entry removed (Varrock has anvils, no furnace; was seeded from memory). Caught by audit.
- Audit log section added to `FurnaceLocations.java`.

## [0.1.0] — 2026-05-14

Pilot #2 MVP. Smelting only (smithing is Pilot #3).

### Added
- `FurnaceLocationOption` dropdown + auto-walk to configured furnace.
- `selectedBar` config + cycle: walk to bank → withdraw ores → walk to furnace → smelt at widget → walk back.
- Forked `Bars.java`, `Ores.java`, `FurnaceLocations.java` from upstream `smelting/enums/`.
- Citation headers on data files.
- `tools/wiki-audit-bars.ps1` and `tools/wiki-audit-furnaces.ps1`.

## Recommended companion plugins

- **EventDismissPlus v0.1.0+** — global random event handler. Enable alongside AutoSmeltingPlus to handle random events during long smelting sessions without stalling at the furnace. Genie lamps auto-apply to Smithing (your active skill). See `eventdismissplus/docs/CHANGELOG.md`.

## Deferred / candidate work

- **v0.4.1 candidate**: Audit `itemsToBank` default for non-"bar"-suffix smelting outputs. Cannonballs are the obvious case but we don't smelt those (separate AutoCannonballSmelter upstream). Verify no other gotchas in our active bar set.
- **v0.6.0+**: Smelt-mode cross-plugin coordination if MiningPlus eventually supports `LAST_LOCATION` walk-back into the bank flow.
- **v1.0.0**: ≥20 hours uptime + docs + upstream PR.
