# MicrobotDashboardPlus Changelog

Native Swing monitoring dashboard, distributed as a Microbot Hub plugin. Pilot #6 of the Skill Plus Template (SPT) lineage; the first non-skilling Plus plugin. As of v0.2.0 the dashboard renders into a floating RuneLite window (Var Inspector style) with a compact sidebar panel; the v0.1.x browser-tab approach has been retired. See `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo for shared conventions.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

## [1.1.0] - 2026-06-05

Three monitoring enhancements. Restores the Watchdog reader the v0.2.0 rewrite left as a placeholder, adds a per-skill ETA, and adds an Antiban State panel so a silent stall can be told apart from an intentional anti-AFK pause.

### Added

- **Antiban State panel** (`panels/AntibanStatePanel.java`). Reads in-process state only: the static `Rs2AntibanSettings` flags (antiban enabled, action cooldown active, micro break active, take micro breaks), the global `Microbot.pauseAllScripts` switch, and the `BlockingEventManager` registered-handler count. Shows a one-line state ("Running", "Micro break in progress", "Action cooldown", "All scripts paused", "Handling a blocking event"). New `showAntibanState` Layout toggle (default ON). New `AntibanState` field on `PollSnapshot`, collected each tick by `GameStatePoller.collectAntibanState()`.
- **Per-skill ETA column** in the Skills section. Computed from the rolling XP per hour the dashboard already tracks and `Experience.getXpForLevel`. The target is read from a new **Skill targets (ETA)** config field (comma-separated `SKILL:LEVEL` pairs, e.g. `MINING:70`). A skill with no target still shows an ETA to its next level while it is being trained. ETA renders as `L70 3h 25m`; reached targets render `done`.
- **Watchdog last-seen and uptime**. `WatchdogStatus` gains `lastSeenText` ("12s ago") and `uptimeText` ("2h 05m"), recomputed on every poll so they keep ticking between watchdog writes. Health downgrades to "Stalled" when the watchdog has not written a line for over five minutes.

### Changed

- **Watchdog panel** now reads the disk log honestly and surfaces health, last seen, uptime, last event, last restart, and total restarts. Plain-language health labels (Healthy / Idle / Stalled / Unavailable) replace the raw ok/warn/bad strings. The v0.2.0 "unavailable until the disk reader reconnects" placeholder javadoc is removed; the reader was already present, the panel and docs now reflect that. When no watchdog log exists the panel shows Unavailable, the graceful fallback for users who do not run the watchdog.
- Version constant bumped to `1.1.0`. The window footer and sidebar subtitle now read the version constant instead of a hardcoded string so they cannot drift again.
- `ConfigInformation`, the in-dashboard Guide panel, and the README updated for the two new panels, the ETA column, and the skill-targets field.

### Notes

- The `BlockingEventManager` "is an event running right now" flag has no public getter, so it is read by reflection and omitted (handler count only) when it cannot be read on the running client version. There is no in-client Watchdog object: the watchdog is the external `watchdog.ps1` helper, so the panel reads its CSV log rather than a client API.

## [1.0.0] — 2026-05-25

Version bump for upstream PR readiness. No functional changes since 0.3.4.

### Changed

- `version` constant in `MicrobotDashboardPlusPlugin` bumped to `1.0.0`. Footer string in `DashboardWindow` and sidebar subtitle in `DashboardPanel` updated to match.

## [0.3.4] — 2026-05-25

UX cleanup. Removed architecture-detail strings that aren't relevant to end users.

### Removed

- `<p>No HTTP server, no port conflicts, no external dependencies.</p>` from ConfigInformation. Mattered when comparing to the v0.1.x browser-based implementation; meaningless to a v0.3.x user who never knew there was an HTTP server.
- "MicrobotDashboardPlus v0.3.3 - in-process poller, no HTTP" footer line in the dashboard window. Same reason. Replaced with just a small version string ("v0.3.4") in the corner so users can still tell which version they're running for bug reports.

### Rationale

Pete's feedback: "can we remove the 'no http server....' line from the description, along with the 'in-process poller...' line on the dashboard page? im not sure its relevant to the user, yk?" Correct read — those lines were leftover technical framing from the v0.1.x → v0.2.0 architecture transition. Now that we're past the transition, they're noise.

## [0.3.3] — 2026-05-25

UX cleanup. Verbose config reference was pushing the in-launcher description into wall-of-text territory; moved it inside the dashboard window itself.

### Added

- **GuidePanel** (`panels/GuidePanel.java`). New 11th section at the bottom of the floating window. Static HTML rendered into a `JEditorPane` with a stylesheet matching the RuneLite dark theme. Contains: a per-panel legend (what each of the other ten panels shows) and a numbered config-options reference (the seven items previously crammed into ConfigInformation).
- **showGuide** config toggle (default ON). Eleventh entry in the Layout section. Users untick once they're familiar with the dashboard.

### Changed

- **ConfigInformation trimmed** to three short paragraphs: what the plugin is, how to access (sidebar icon → Open Dashboard), and the architecture line. The detailed config reference now lives inside the dashboard via the Guide panel, where users are already engaging with the feature.
- `README.md` updated to mention the in-dashboard Guide section.

### Rationale

Pete's feedback: "looks good, but it's VERY wordy. would we be able to cut down on this while still preserving the information? maybe we move an in-depth guide to the [dashboard] itself, under like a 'guide' or 'legend' section?"

Better separation of concerns. The launcher-side ConfigInformation should answer "what is this plugin?" in seconds. The in-dashboard Guide answers "how do I use it?" with the user already in front of the dashboard. Users who don't need either can hide the Guide panel via Layout.

## [0.3.2] — 2026-05-25

Two correctness fixes caught during README-prep screenshot review.

### Fixed

- **XP delta + XP/hr inflation on first login after plugin start**. `GameStatePoller.buildSnapshot()` was calling `xpHistory.record(skill, currentXp)` unconditionally on every tick. On the login screen, `client.getSkillExperience()` returns `0` for every skill, so `0` became the baseline. After login, when XP jumped to real values, `delta = currentXp - 0 = currentXp` and `XP/hr` extrapolated absurd values (e.g. Mining showed `+154,537 Δ, 12,362,960 XP/hr`). Fix: gate the `xpHistory.record` call behind `loggedIn`.
- **Level-up notification false positives on first login after plugin start**. Same root cause. `detectAndFireNotifications` established the `lastSkillLevels` baseline on the first poll regardless of login state, so logged-out levels of `0` became baseline. The first poll after login fired "Level up: Attack 0 → 43" Discord notifications for every skill (Discord spam, ~22 messages in seconds, if `notifyLevelUp` was on). Fix: defer baseline establishment until `snapshot.isLoggedIn()`.

### Changed

- **Dropped active-state green border on Plus Plugins rows**. The double-signal (border tint + button color) was a redundant indicator. v0.3.2 uses only the button color: green Start when stopped, red Stop when running. Row borders are uniform gray. Less visual noise.

## [0.3.1] — 2026-05-25

Pre-v1.0.0 polish before upstream PR prep. Three small items.

### Added

- **In-dashboard alert banner**. Yellow strip at the top of the floating window (below the header) that appears when an alert threshold crosses. Dismissable. Fires regardless of Discord configuration, so users without a webhook still see threshold crossings. Implemented via a new `GameStatePoller.setBannerCallback(Consumer<String>)` hook called whenever a threshold fires; `DashboardWindow.showAlertBanner()` routes to the EDT and shows the banner. Hidden by default; max height 32px so it doesn't push the section grid down by much.

### Changed

- **Discord webhook URL field is now masked** in the config panel. Added `secret = true` to the `ConfigItem` so the value renders as dots in the UI. Helps prevent accidental leakage in screenshots. The value still persists in the config file like any other string -- this is a UI-level mask only.
- **Active Scripts filter** now excludes "Test Runner" via the `"test runner"`/`"testrunner"` substrings in `INFRA_NAME_SUBSTRINGS`. Quest Helper and Mouse Macro Recorder remain visible: those are legitimate user-facing plugins, not dev infrastructure. Pete observed Test Runner leaking through in v0.3.0; v0.3.1 catches it without expanding the heuristic too aggressively.

## [0.3.0] — 2026-05-25

Feature release: per-section visibility, Discord webhook notifications, and per-skill alert thresholds.

### Added

- **Per-section visibility config** (`Layout` section, 10 boolean toggles). Each of the 9 dashboard sections (Player, Active Scripts, Plus Plugins, Inventory, Skills, Nearby NPCs, Watchdog, XP Chart, Event Dismiss Stats, Event Log) can be shown or hidden individually. `DashboardWindow.applyVisibility()` reads each predicate from config; toggling a `show*` key fires `ConfigChanged` → window re-evaluates. To approximate a "compact mode", uncheck the chart + NPC + event sections.
- **Discord webhook notifications** (`Notifications` section). New `notify/DiscordNotifier.java`: single-thread daemon executor posts `{"content": "..."}` to the configured webhook URL with 4-second timeouts. Treats the URL as a secret — never logged on error, never returned in exception messages. Truncates message body to 1900 chars (Discord cap is 2000).
- **Three configurable trigger types**:
  - `notifyLevelUp` (default ON): "Level up: Mining 53 → 54"
  - `notifyRandomEvent` (default ON): "Random event detected (1 new entry in EventDismiss log)"
  - `notifySessionLifecycle` (default OFF): "Dashboard session started." / "Dashboard session stopped."
- **Alert thresholds** (`Alerts` section). New `notify/AlertManager.java`. Config format: `MINING:60, WOODCUTTING:80, FISHING:70` (comma-separated SKILL:LEVEL pairs). Parser tolerates whitespace, validates skill names against the OSRS API enum, clamps levels to [1, 99], skips malformed entries with a debug log. Crossing fires exactly once per (skill, level) pair across the session; sends a "ALERT: Mining reached level 60!" Discord notification when `notifyAlerts` is on.

### Changed

- `GameStatePoller` now detects level transitions and EventDismiss CSV row-count diffs to drive notifications. Suppresses level-up false positives on the first poll (baseline establishment). Random-event detection uses total-row delta across snapshots, not per-row diff.
- Config grew from 3 fields to 18 across 4 sections (Behavior / Layout / Notifications / Alerts).

### Carried forward / known limitations

- No in-dashboard alert banner UI; alerts only fire to Discord. A JLabel banner at the top of the floating window is queued for v0.3.1 if useful.
- Discord URL not field-masked in the config UI; users should be aware that screenshots can leak the URL.
- Plugin icon still programmatic (the v0.2.2 chart-line glyph). Hand-designed PNG ships in v1.0.0 with upstream-PR-prep artwork.

## [0.2.2] — 2026-05-25

Small QoL polish bundle before v0.3.0 feature work. Five carry-forward items from the v0.2.1 CHANGELOG.

### Added

- **Random-event NPC highlighting** in Nearby NPCs. Catalog of known event names (Genie, Sandwich lady, Bee keeper, Mysterious Old Man, Niles/Miles/Giles, Freaky Forester, Evil Bob, Leo, etc.) copied from EventDismissPlus's RandomEventType enum. Case-insensitive match. Matching rows render orange via the existing `NearbyNpc.randomEvent=true` path.
- **Window size + position persistence**. `DashboardWindow` saves bounds to ConfigManager keys `windowX/Y/Width/Height` on `componentMoved`/`componentResized`. Restores on construct, with a sanity check that the saved position intersects at least one visible monitor (handles multi-monitor disconnect cleanly). Falls back to centered 1100x800 if no saved bounds.
- **Per-section persistence for the XP chart**. Skill (enum name) and window-index choices saved to config on combo change. Restored on construct. The chart now remembers what you were looking at across launcher restarts.
- **Inventory noted-state detection**. `GameStatePoller.isNoted()` reads `ItemComposition.getNote()` + reflectively probes `getNoteTemplate()` (defensive against client API drift). Sets `PollSnapshot.InventoryItem.noted=true` when the item is the noted form. `InventoryPanel` already styles noted items in orange italic.
- **Polished plugin icon**. Replaced the simple "D" placeholder with a dashboard glyph: dark rounded background + RuneLite-green rising-line chart + dot at the right endpoint. Still programmatic (no PNG resource yet); a hand-designed PNG ships in v1.0.0 alongside the upstream-PR cardUrl artwork.

### Carried forward to v0.3.0

- Discord webhook for level-up / random-event notifications
- Compact mode toggle
- Per-section visibility config
- Alert thresholds (Mining hits 60 → notify)

## [0.2.1] — 2026-05-25

Polish bundle. Fills the v0.2.0 placeholders with real data.

### Added

- **XP-over-time chart** (`panels/XpChartPanel.java`). Custom Java2D paint, no external charting dep. Skill JComboBox + window JComboBox (5m / 15m / 30m / 1h / 4h / 24h) in section header. Full-width section between Watchdog and Event Dismiss Stats. Replaces the Chart.js implementation from the v0.4.0 browser dashboard.
- **EventDismiss CSV reader** (`data/LogReaders.java`). Reads `~/.runelite/eventdismissplus-events.csv`. Aggregates per-event counts (engaged / dismissed / declined / errors / total). Cache keyed by file mtime so the poller can call every tick.
- **Watchdog disk log reader**. Reads `~/.runelite/microbot-watchdog.csv`. Computes status (ok/warn/bad) from the latest row + WATCHDOG_START vs restart events. Same mtime-keyed cache.
- **Inventory data wiring**. `Rs2Inventory.items()` populates `PollSnapshot.inventory`. Cell grid renders slot/name/qty per item.
- **Per-plugin runtime tracking**. `GameStatePoller` records first-observed enabled-millis per plugin class; the Active Scripts table's Runtime column comes alive (formatted as `Xh YYm` / `Xm YYs`). Reset when a plugin disables.
- **Sample retention extended to 24h** in `XpHistory` so the chart's 24-hour window works. XP/hr rate calc still uses the inner 5-min window via filter.

### Changed

- **Active Scripts filter refined** (`GameStatePoller.collectActiveScripts`). Excludes Microbot core utility classes (`MicrobotPlugin`, "Antiban") and the dashboard itself. Sidebar "Active" count drops from ~13 to the actual user-facing-script count.
- **Plus Plugins quickstart excludes self** to avoid the user accidentally stopping the dashboard from the dashboard.
- **EventDismissStatsPanel** rewritten as a real `JTable` (was a placeholder JLabel).

### Fixed (in-cycle polish)

- **Active Scripts filter**: was leaking "Antiban", "[M] Web Walker", "[M] MInventory Setups", "F2P Web Walker Harness", "GE Lumbridge Teleport Harness" through. v0.2.1 switches from exact-match exclusion to substring-based infra detection ("antiban", "harness", "web walker", "minventory") on both display name and simple class name. Verified live with Pete: sidebar `Active` count drops from 13 → 5-ish, and the table shows only user-facing scripts (Agent Server, Auto Mining Plus, Event Dismiss Plus, QoL).
- **Section header title truncation**: "XP Over Time" was rendering as "XP Over Ti..." and "Event Dismiss Stats" as "Event Dismiss Sta..." because BorderLayout(WEST/EAST) + FlowLayout under-allocated width to the JLabel for the custom Runescape font. Rewrote `DashboardSection`'s header to use `GridBagLayout` with explicit weights (title + subtitle pinned left @ weightx=0, flex spacer @ weightx=1, controls pinned right @ weightx=0). Titles render at full natural width regardless of font metrics quirks.

### Carried forward / still pending

- Plugin icon remains a programmatic 16x16 green "D". Proper PNG ships in v0.2.2 with the upstream-PR-prep polish.
- Inventory noted-state flag (`Rs2ItemModel.isNoted()` is not exposed in the current API; field always `false` for now).
- Random-event NPC detection in NearbyNpcs (would highlight Genie / Sandwich Lady / Strange Plant in orange).

## [0.2.0] — 2026-05-25

Full Swing rewrite. Graduates from browser-tab UX to a native floating RuneLite window plus a compact right-sidebar plugin panel. Eliminates the embedded HTTP server, the port binding, and the dependency on the `[M] Agent Server` plugin.

### Changed

- **Native Swing rendering**. Floating `JFrame` modeled on the RuneLite Var Inspector. Compact summary lives in the right-sidebar `PluginPanel` (Hub convention) with an "Open Dashboard" button to launch the full window. Closes cleanly with the client.
- **In-process polling**. Background `ScheduledExecutorService` (single thread, daemon) reads game state directly via `Microbot.getClient()` + Rs2 utility APIs. Client-thread-restricted reads go through `Microbot.getClientThread().runOnClientThreadOptional()`. Listeners are invoked on the EDT via `SwingUtilities.invokeLater()`.
- **Config simplified** from 4 fields to 3: `autoOpenDashboard`, `pollIntervalSeconds` (1-60), `npcMaxDistance` (1-200). HTTP-era fields (`serverPort`, `agentServerPort`, `devModePath`) removed.

### Removed

- `DashboardHttpServer.java` (embedded `com.sun.net.httpserver` HTTP server).
- Bundled `index.html`, `style.css`, `app.js` resources.
- Reverse-proxy `/api/*` to Agent Server. Plugin no longer reads `~/.runelite/.agent-token` or speaks HTTP at all.
- Dev-mode override (`devModePath`). With no HTML/JS to override, the feature has no purpose.
- "Auto-close on disconnect" countdown UX from v0.1.1. JFrame lifecycle is bound to the plugin lifecycle now; window disposes on plugin disable.

### Added

- `data/PollSnapshot.java` — immutable record of per-poll state.
- `data/XpHistory.java` — rolling 5-min XP/hr per skill + baseline delta tracking.
- `poller/GameStatePoller.java` — background poller, configurable interval, listener registry.
- `window/DashboardWindow.java` — floating JFrame skeleton, 9-section grid placeholder layout, header (status + last poll) and footer.
- `DashboardPanel.java` — right-sidebar `PluginPanel`: status, player, world, active-script count, "Open Dashboard" + "Refresh now" buttons.

### Known limitations (this version)

Section panels are placeholders ("panel under construction") in v0.2.0 ship. End-to-end architecture (poller → window + panel) is validated; the 9 real section panels (Player, Active Scripts, Plus Plugins, Inventory, Skills, Nearby NPCs, Watchdog, Event Dismiss Stats, Event Log) land iteratively in v0.2.1+.

- XP-over-time chart not ported. Deferred to v0.2.x as Java2D paint (no Chart.js dependency).
- Active scripts list is a heuristic enumeration of enabled Microbot plugins; per-plugin runtime not tracked in-process yet.
- Watchdog status reads "unavailable" until the disk-log reader reconnects in a follow-up.
- Plugin icon is a programmatic 16x16 "D" placeholder. v1.0.0 ships a proper PNG.

### Rationale

The v0.1.x "two PowerShell windows" UX was the original problem the plugin solved. v0.1.x replaced that with a browser tab, but introduced its own UX issues: sea-of-tabs on every disable/enable cycle (v0.1.1 patched), browser auto-open semantics on launcher boot, dependency on Agent Server staying enabled. Going native eliminates all three classes of issue, matches RuneLite / Microbot UI conventions, and removes the port-binding fingerprint. Costs: ~1500 LOC of HTML/CSS/JS retired, multi-device LAN viewing lost, no Chart.js for the XP visualization.

The `tools/agentserver/dashboard/` source tree remains as a historical artifact and standalone PowerShell-served dev tool. Not in the plugin JAR.

## [0.1.1] — 2026-05-25

### Fixed

- **"Sea of tabs" UX**: every plugin disable + re-enable cycle was spawning a fresh browser tab without closing the prior one. After several toggles, the user accumulated multiple dashboard tabs pointing at the same plugin.
- Dashboard now self-closes after sustained disconnect (~30 sec at default 5-sec polling). Counts consecutive failed polls; when threshold passes, calls `window.close()`.
- Counts down in the status banner for the last 15 sec before close: `"Disconnected — closing in 15s"` → `"... in 10s"` → `"... in 5s"` → `"Plugin disconnected — closing tab"`. Gives the user time to disable the feature in Settings if they want to keep the tab open for debugging.
- Configurable opt-out via Settings panel checkbox **"Auto-close this tab when plugin disconnects (~30 sec)"** (default ON). Persisted to localStorage.

### Caveat

`window.close()` silently fails for user-opened tabs (browser security blocks closing tabs the user explicitly opened, only allows it for tabs opened via JS `window.open()` or `Desktop.browse()`). Status remains "Plugin disconnected — closing tab" indefinitely in that case; user closes manually.

For auto-opened tabs from the plugin's `Desktop.getDesktop().browse(...)` call, close works reliably across Chrome/Edge/Firefox tested.

### Validated

Pete tested v0.1.1 via dev-mode override workflow:
1. Set `devModePath` config to `C:\Users\peter\src\Microbot-Hub\tools\agentserver\dashboard`
2. Hard-refreshed browser (no plugin rebuild needed)
3. Verified countdown + auto-close on plugin disable
4. Verified fresh single tab on re-enable

This double-validates the dev-mode override feature itself — the iteration loop works as designed.

## [0.1.0] — 2026-05-25

Pilot #6 MVP. Hub plugin port of the PowerShell-served dashboard (`tools/agentserver/dashboard/`) developed in v0.1.0 → v0.4.0 of that tool. Same UI, same 9 dashboard sections, same feature set — distributed as a plugin instead of two PowerShell windows.

### Added

- Embedded HTTP server (Java `com.sun.net.httpserver.HttpServer`) starts on plugin enable, stops cleanly on plugin disable. Default port 8088, configurable.
- Static-file serving from JAR resources at `/net/runelite/client/plugins/microbot/microbotdashboardplus/dashboard/`. Bundles the same `index.html`, `style.css`, `app.js` developed in `tools/agentserver/dashboard/`.
- Reverse proxy `/api/*` to the Agent Server plugin. Reads the auth token from `~/.runelite/.agent-token` and attaches `X-Agent-Token` header server-side. Browser never sees the token.
- Local file serving for `/watchdog-log` (CSV), `/eventdismiss-log` (CSV), `/history/log` (JSONL GET + POST). Same paths the PowerShell `serve.ps1` exposes.
- Auto-open browser config toggle. On plugin enable, opens `http://localhost:<port>/` in the user's default browser via `Desktop.getDesktop().browse(...)`.
- Dev-mode override: config field `devModePath` reads dashboard HTML/CSS/JS from a filesystem path instead of JAR resources. Iterating on dashboard UI doesn't require rebuilding the plugin JAR.

### Configuration

- `serverPort` (1024-65535, default 8088) — HTTP server port. Change if conflicting with PowerShell `serve.ps1`.
- `agentServerPort` (1024-65535, default 8081) — Agent Server's port. Plugin proxies `/api/*` here.
- `autoOpenBrowser` (default true) — open browser when plugin enables.
- `devModePath` (default empty) — disk path for dashboard files (overrides JAR resources when set).

### Requires

- The `[M] Agent Server` plugin must be enabled. This plugin proxies through it for game-state data.

### Notes for development

The dashboard HTML/CSS/JS lives in two places by design:
- `tools/agentserver/dashboard/` — canonical source for development. Edit here.
- `src/main/resources/.../microbotdashboardplus/dashboard/` — copy bundled into the plugin JAR.

Build process (currently manual): copy from `tools/agentserver/dashboard/` to the resources directory before each `gradle build`. Future enhancement: gradle task to automate the copy.

### What this version does NOT include (future versions)

- **v0.2.0**: direct in-process Agent Server calls (no HTTP between plugin and Agent Server). True global pause via `Microbot.pauseAllScripts.set(...)`.
- **v0.3.0**: WebSocket push for sub-100ms realtime updates. Replaces 5-sec polling.

### Verification

End-to-end:
1. Build: `./gradlew build -PpluginList=MicrobotDashboardPlusPlugin -x test`
2. Deploy: copy `build/libs/MicrobotDashboardPlusPlugin-0.1.0.jar` to `~/.runelite/microbot-plugins/`
3. Enable in client: open plugin list, find "Microbot Dashboard Plus", toggle on.
4. Browser should open to `http://localhost:8088/`. Dashboard renders all 9 sections with live data.
5. Disable plugin: HTTP server stops cleanly, port freed.
