# MicrobotDashboardPlus Changelog

Browser-based monitoring dashboard, distributed as a Microbot Hub plugin. Pilot #6 of the Skill Plus Template (SPT) lineage; the first non-skilling Plus plugin. Embeds the dashboard files in the plugin JAR and starts an HTTP server inside the Microbot JVM. See `template/TEMPLATE.md` and `template/PATTERNS.md` in the Hub repo for shared conventions.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Semver-flavored.

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
