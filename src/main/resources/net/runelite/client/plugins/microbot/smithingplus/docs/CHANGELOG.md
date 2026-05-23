# AutoSmithingPlus Changelog

Auto-walking-and-smithing-at-anvil "Plus" fork of upstream VarrockAnvil. Part of the Skill Plus Template (SPT) lineage; see `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

## [0.5.7] — 2026-05-23

### Fixed
- **Pause now actually stops the bot.** Added `Rs2Walker.setTarget(null)` to the overlay pause-click handler to interrupt the in-flight WebWalker. See AutoMiningPlusPlugin v0.5.7 CHANGELOG for full diagnosis.

### Added
- `Microbot.pauseAllScripts.compareAndSet(true, false)` in `startUp()` and `shutDown()`.

## [0.5.6] — 2026-05-23

### Fixed
- **Pause button now actually works.** Missing `overlay.pauseButton.hookMouseListener()` call in `startUp()`. See AutoMiningPlusPlugin v0.5.6 CHANGELOG for the full diagnostic journey.
- Field visibility: `pauseButton` changed to `public final`.

### Changed
- Button text reads "Resume" (not "Unpause") when paused.

## [0.5.1 - 0.5.5] — dead-end iterations (see Mining CHANGELOG for detail).

## [0.5.0] — 2026-05-22

## [0.5.0] — 2026-05-22

Cross-plugin v0.5.0: Target Level + Pause shipped across all 4 Plus plugins.

### Added
- `targetLevel` config (int, default 0). When Smithing level reaches the target, deposit current items then shutdown via the `shutdownAfterCleanup` flag intercepted after `depositByCsv` in `handleBankAndWithdraw`.
- `paused` config (boolean, default false). Tick early-exits without resetting session metrics.
- Overlay shows `Target: X (Y to go)` and `[PAUSED]` prefix.

## [0.4.0] — 2026-05-22

Wiki-driven data expansion Phase A.

### Fixed
- `MINING_GUILD_FALADOR` bank coord collision with `FALADOR_EAST` (shared fix across all 3 Plus plugins that copy `BankLocationOption.java`).
- `AL_KHARID_ARENA` (Fadli's bank) coord live-verified, corrected to `(3383, 3269, 0)`.

### Added
- `FEROX_ENCLAVE` bank entry (F2P).
- Varrock Central (Horvik's, 2 anvils, 31 sq from W bank) + Varrock East (2 anvils south of bank, 35 sq) anvil entries to `AnvilLocationOption` and `AnvilLocations`. Both F2P, all-bar tiers.
- New `tools/wiki-audit-banks.ps1`.

## [0.3.0] — 2026-05-21

Polish-Cycle 2 (cross-plugin overlay + threshold work).

### Added
- Runtime stats overlay: uptime, Smithing XP gained, XP/hr, smith cycles, level + delta, status.
- `stopAfterMinutes` and `stopAfterXp` config items.

## [0.2.0] — 2026-05-15

Polish Cycle B. Borrows from AutoMiningPlus + AutoSmeltingPlus + WC. **Partial** — 6/8 borrows shipped.

### Added
- CSV `itemsToBank` (default `""`) and `itemsToKeep` (default `"hammer, bar"`).
- `maxPlayersInArea` autohop.
- `leagueMode` arrow-key press to defeat idle-logout.
- `dropOrder` (`InteractOrder` enum) config item for cross-plugin parity.
- Bronze-only pre-flight check at Lumbridge Rusted Anvil (refuses to start if non-Bronze bar configured).
- Members-only pre-flight check for items like Bronze Claws (Cabin Fever quest gate). Refuses to start on F2P.
- Lumbridge Rusted Anvil walk-to coord refined: `(3227, 3258, 0)` after three iterations. The actual anvil tile sits behind the furnace in the Smiths' building.

### Deferred to a future v0.x.x
- Toolbelt hammer detection (current `Rs2Inventory.hasItem(HAMMER)` returns false for toolbelt hammers).
- Progressive smith (auto-pick highest-XP item player can make given Smithing level + bar stock + anvil compat).
- `smithingplus/data/AnvilItemLevels.java` (26 items × 6 bar tiers = 156 entries).
- `tools/wiki-audit-anvil-item-levels.ps1`.

## [0.1.0] — 2026-05-14

Pilot #3 MVP.

### Added
- `AnvilLocationOption` dropdown + auto-walk to configured anvil.
- `selectedBar` + `selectedItem` configs + cycle: walk to bank → withdraw bars + hammer → walk to anvil → click anvil → click All multiplier → click item widget child → smith inventory full.
- Forked `AnvilItem.java` from upstream `varrockanvil/enums/` with citation header. 28 smithable items, each with `(name, widget childId, requiredBars)`.
- Copied `Bars.java` from `smeltingplus/data/`.
- New `AnvilLocations.java` with Lumbridge Rusted Anvil (bronze-only) + Varrock West Anvil. Citation header.
- `tools/wiki-audit-anvils.ps1` and `tools/wiki-audit-anvil-items.ps1`.

## Recommended companion plugins

- **EventDismissPlus v0.1.0+** — global random event handler. Enable alongside AutoSmithingPlus to handle random events during anvil sessions without stalling. Genie lamps auto-apply to Smithing (your active skill). See `eventdismissplus/docs/CHANGELOG.md`.

## Deferred / candidate work

- **v0.2.x continuation (Polish Cycle B remainder)**: toolbelt hammer detection, progressive smith, `AnvilItemLevels.java` data file, level-table audit script.
- **v0.6.0+**: Drop-at-anvil mode for cheap items (Nails) to skip banking for max XP/hr. Auto-equip Smithing cape.
- **v1.0.0**: ≥20 hours uptime + docs + upstream PR.

## Known gaps (carried forward)

- Anvil widget child IDs are hardcoded in `AnvilItem.java` (Dagger=9, Plate body=22, etc.) per upstream VarrockAnvil. If microbot/RuneLite reshuffles the smithing widget in a future release, all items break at once. v0.4.0+ could add runtime widget-tree verification.
- Smithing-level mismatch (e.g. Rune Plate Body at Smithing 1) doesn't pre-flight refuse; widget greys the slot, click is no-op, bot looks frozen. Pre-flight check requires the `AnvilItemLevels` data work above.
- Toolbelt hammer edge case — accounts with toolbelt hammers see `Rs2Inventory.hasItem(HAMMER) == false` even though smithing works. Inherits upstream behavior (always withdraws an inventory hammer).
- Cross-plugin contention with AutoSmeltingPlus when both run simultaneously and target the same bank. Documented: "run one at a time."
