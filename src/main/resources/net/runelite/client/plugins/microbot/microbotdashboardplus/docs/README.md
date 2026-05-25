# Microbot Dashboard Plus

Browser-based monitoring dashboard for your Microbot session, distributed as a Hub plugin.

## What you get

Enable this plugin and open `http://localhost:8088/` in your browser. The dashboard shows live data about your bot:

| Section | Data |
|---|---|
| Player | Name, combat level, login state, world, profile, session duration, position, animation |
| Active Scripts | All running plugins with Stop buttons (~click to stop in-browser) |
| Plus Plugins | Quick start/stop buttons for the 5 known Auto*Plus plugins + EventDismissPlus |
| Inventory | Aggregated tile grid of inventory items + diff log to event log |
| Skills | All 23 skills with current XP, delta since page load, rolling 5-min XP/hr, forecast to next level |
| Nearby NPCs | Aggregated NPC list sorted by distance, with random-event NPC highlighting (configurable max distance) |
| Watchdog | Status panel reading `~/.runelite/microbot-watchdog.csv` if the PowerShell watchdog is running alongside |
| XP Over Time | Chart.js line chart with skill + window selectors (5m to 24h) |
| Event Dismiss Stats | Drop-rate aggregation parsed from `~/.runelite/eventdismissplus-events.csv` (EventDismissPlus v0.2.0+) |
| Event Log | Last 10 state-change events: logins, world hops, script start/stop, inventory diffs, etc. |

## Quick start

1. Enable the `[M] Agent Server` plugin (required dependency)
2. Enable this plugin (`[M] Microbot Dashboard Plus`)
3. Your default browser opens to `http://localhost:8088/` automatically

To disable auto-open: plugin config → uncheck "Auto-open browser on startup".

## Requirements

- The `[M] Agent Server` plugin must be running. This plugin proxies through it.
- Default port is 8088. Change in config if conflicting with another service.
- Auth is handled automatically by reading the token from `~/.runelite/.agent-token`.

## Configuration

| Setting | Default | Notes |
|---|---|---|
| Server port | 8088 | The HTTP server port. Dashboard URL is `http://localhost:<port>/` |
| Agent Server port | 8081 | The port the Agent Server plugin listens on |
| Auto-open browser on startup | ON | Opens dashboard in default browser when plugin enables |
| Dev-mode dashboard path | (empty) | Advanced: serve dashboard files from a filesystem path instead of the bundled JAR |

## Architecture (vs the PowerShell variant)

This plugin replaces the `tools/agentserver/dashboard/serve.ps1` PowerShell script. Same dashboard, same endpoints, just packaged as a Hub plugin so users get a one-click experience instead of needing two PowerShell windows.

| Without plugin (PowerShell setup) | With plugin |
|---|---|
| Open PowerShell, run `serve.ps1` | Toggle plugin in client |
| Open browser to localhost:8088 | Browser opens automatically |
| Manage script lifecycle manually | Plugin lifecycle managed by client |
| `Ctrl+C` to stop | Disable plugin in client |

The PowerShell variant remains useful for development (faster iteration on HTML/CSS/JS without rebuilding the plugin JAR).

## Companion tools

- **`tools/agentserver/watchdog/`** — PowerShell script that polls the Agent Server and auto-restarts the Microbot Launcher on hangs. **Stays as PowerShell** because it must be external to the launcher (a plugin can't restart the JVM it runs in).
- **`tools/agentserver/dashboard/`** — PowerShell-served dashboard for development iteration. Same source files as this plugin bundles.

## Future versions

- **v0.2.0**: direct in-process Agent Server calls. Skips HTTP between this plugin and the Agent Server (both in same JVM). Includes true global pause via direct `Microbot.pauseAllScripts.set(...)`.
- **v0.3.0**: WebSocket push for sub-100ms realtime. Replaces 5-sec polling.
- **v1.0.0**: graduation — ≥20 hours soak + docs + PR to chsami/Microbot-Hub.
