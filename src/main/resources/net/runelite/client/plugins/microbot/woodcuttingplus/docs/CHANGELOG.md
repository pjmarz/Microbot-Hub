# AutoWoodcuttingPlus Changelog

Auto-chopping-and-handling-logs "Plus" fork of upstream AutoWoodcutting v1.8.3. Part of the Skill Plus Template (SPT) lineage; see `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

## [0.5.7] — 2026-05-23

### Fixed
- **Pause now actually stops the bot.** Added `Rs2Walker.setTarget(null)` to the overlay pause-click handler to interrupt the in-flight WebWalker. See AutoMiningPlusPlugin v0.5.7 CHANGELOG for full diagnosis.

### Added
- `Microbot.pauseAllScripts.compareAndSet(true, false)` in `startUp()` and `shutDown()`.

## [0.5.6] — 2026-05-23

### Fixed
- **Pause button now actually works.** Missing `woodcuttingOverlay.pauseButton.hookMouseListener()` call in `startUp()`. See AutoMiningPlusPlugin v0.5.6 CHANGELOG for the full diagnostic journey.
- Field visibility: `pauseButton` changed to `public final`.

### Changed
- Button text reads "Resume" (not "Unpause") when paused.
- Overlay Status line still shows `[PAUSED]` (in place of the script-state enum) while paused.

## [0.5.1 - 0.5.5] — dead-end iterations (see Mining CHANGELOG for detail).

## [0.5.0] — 2026-05-22

## [0.5.0] — 2026-05-22

Cross-plugin v0.5.0: Target Level + Pause shipped across all 4 Plus plugins.

### Added
- `targetLevel` config (int, default 0). When Woodcutting level reaches the target, the script runs one cleanup pass (BANK or DROP per `primaryAction`) then shuts down. `shutdownAfterCleanup` flag intercepted in `resetInventory` before the state-flip-back to WOODCUTTING.
- `paused` config (boolean, default false). Tick early-exits without resetting session metrics.
- Overlay shows `[PAUSED]` (replaces the script-state display) and a `Target: X (Y to go)` line when `targetLevel > 0`.

### Known gaps
- BURN_CAMPFIRE / BURN and FLETCH `primaryAction` paths don't trip the `shutdownAfterCleanup` intercept cleanly — the script just runs out naturally rather than doing a clean cleanup-then-shutdown. Acceptable for v0.5.0; revisit if a user reports issues.

## [0.3.0] — 2026-05-21

Bare-fork pilot landed in working order.

### Added
- Full `woodcuttingplus/` package: 22 files cloned from upstream `woodcutting/` (4 top-level + 9 Forestry events + 7 enum files + `LocationOption.java`, `ResourceLocationOption.java`).
- `@ConfigGroup("WoodcuttingPlus")` (renamed from upstream's `AutoWoodcutting` to avoid settings cross-pollination).
- `speedMode` boolean config — disables Microbot's antiban via single-flag `Rs2AntibanSettings.antibanEnabled = false`. Throwaway-only.
- `stopAfterMinutes` and `stopAfterXp` config items (Polish-Cycle 2 parity with other Plus plugins).
- Citation headers on `WoodcuttingTree.java` + `WoodcuttingTreeLocations.java`.
- `tools/wiki-audit-trees.ps1` (cloned from `wiki-audit-bars.ps1`, adapted for tree-name audit against the OSRS Wiki Tree page).
- All upstream features inherited: progressive mode, walk-back modes (INITIAL_LOCATION vs LAST_LOCATION), autohop via `hopWhenPlayerDetected`, Hardwood Tree Patch support, Bird nest + Seed looting, Firemaking mode, Fletch + String bows, Log basket, all 9 Forestry events (Egg, Entlings, Flowers, Fox, Hives, Leprechaun, Ritual, Root, Sapling).

## [0.1.0] — 2026-05-14 (scope: bare fork only)

Pilot #4 v0.1.0 was scoped to clone upstream as-is. The work landed but evolved quickly into the v0.3.0 baseline above as the rest of the upstream functionality was preserved.

## Recommended companion plugins

- **EventDismissPlus v0.1.0+** — global random event handler. Enable alongside AutoWoodcuttingPlus to handle random events during long chopping sessions. Genie lamps auto-apply to Woodcutting (your active skill). **Note**: Forestry events take precedence over EventDismissPlus's `BlockingEventPriority.LOWEST`, so both can coexist safely. See `eventdismissplus/docs/CHANGELOG.md`.

## Deferred / candidate work

- **v0.6.0 candidate**: `TreeLocationOption` named-location dropdown (mirror `MineLocationOption` pattern). Source data already in `WoodcuttingTreeLocations.java`; just need a facade enum.
- **v0.7.0 candidate**: `BankLocationOption` dropdown (copy from `template/skeleton/data/` or from `miningplus/data/BankLocationOption.java`).
- **v0.8.0+**: forestry-events audit script (NPC + animation IDs are baked into the event subclasses; if they drift in a microbot bump, we silently lose forestry support).
- **v1.0.0**: ≥20 hours throwaway uptime + docs page + PR to `chsami/Microbot-Hub` `development` branch.

## Known gaps (carried forward)

- v0.5.0 cleanup intercept only handles `primaryAction == BANK` and `primaryAction == DROP`. BURN_CAMPFIRE / BURN / FLETCH paths don't shutdown cleanly.
- `@ConfigGroup` rename resets the user's existing AutoWoodcutting settings on first launch of Plus — documented in the descriptor description, but users may need to re-tick boxes.
- `WoodcuttingTreeLocations` v0.3.0 audit only validates tree names, not location coords or quest/skill gates. Coord-level audit deferred to when the named-location dropdown wires up (~v0.6.0).
- Forestry event NPC/animation IDs are baked into the event subclasses. Drift on a microbot bump silently breaks forestry. Audit deferred.
- `private static WorldPoint returnPoint` in the script is a static field — risks state leakage across plugin enable/disable cycles. Reset in `run()`; deferred refactor to instance field.
