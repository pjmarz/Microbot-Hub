# Microbot Dashboard Plus

**Aggregate session dashboard for Microbot.** A floating window outside the game with ten live-updating panels covering player state, scripts, inventory, skills, NPCs, and more. Optional Discord webhook for level-ups and alerts.

![Dashboard window — top half](assets/dashboard-top.png)

![Dashboard window — bottom half (XP chart, event stats, log, guide)](assets/dashboard-bottom.png)

---

## The problem

Microbot's default UX puts a separate in-game overlay in the top-left corner for every running plugin. With 3-4 Plus plugins active those overlays stack up and cover a large fraction of the game screen, and each one shows only its own slice of state.

The built-in **Discord** plugin forwards chat-log lines but has no concept of game state changes. The **Agent Server** plugin exposes an HTTP API but consuming it requires writing your own client.

Dashboard Plus fills the gap: one floating window outside the game, optional Discord notifications driven by real game state, and an XP-over-time chart.

| Without Dashboard Plus | With Dashboard Plus |
|---|---|
| Stacked per-plugin overlays cover the game screen | One floating window, game screen clean |
| No XP-over-time trend | Java2D line chart, 5m / 15m / 30m / 1h / 4h / 24h windows |
| Stop scripts one at a time from the plugin list | One click on any row in Active Scripts |
| No notifications for level-ups, random events, or alert thresholds | Optional Discord webhook covering all three |

---

## What it does

The floating window has ten panels:

| Section | Shows |
|---|---|
| **Player** | Name, combat level, login state, world, profile, session duration, position, animation |
| **Active Scripts** | User-facing Microbot plugins currently enabled, per-plugin runtime, Stop button per row |
| **Plus Plugins** | Quick start/stop grid for every plugin whose name ends in "Plus" |
| **Inventory** | Slot grid with item names + quantities; noted items styled distinctly |
| **Skills** | All 22 skills with current level, total XP, gain since session start, rolling 5-min XP/hr |
| **Nearby NPCs** | NPC list sorted by distance, max-distance JSpinner, random-event NPCs highlighted orange |
| **Watchdog** | Real-time status from `~/.runelite/microbot-watchdog.csv` |
| **XP Over Time** | Java2D line chart with skill + window selectors (5m to 24h) |
| **Event Dismiss Stats** | Per-event-type counts from EventDismissPlus's CSV log |
| **Event Log** | Rolling 10-entry ring buffer of login/logout/world-hop events |

A compact summary lives in the right sidebar as a plugin panel: status, player, world, active-script count, plus **Open Dashboard** and **Refresh now** buttons.

---

## How to use it

1. Enable **Microbot Dashboard Plus** from the plugin list
2. A green chart-line icon appears in the right sidebar
3. Click the icon to open the panel, then click **Open Dashboard** to launch the floating window
4. The floating window remembers its size and position across launches

The sidebar icon and panel are only present while the plugin is enabled. Disabling the plugin removes both, along with the floating window.

---

## Requirements

- **Microbot client** v2.0.13 or newer
- No external dependencies (no HTTP server, no port binding, no other plugins required)
- Optional: a Discord channel webhook URL for notifications
- Optional: EventDismissPlus v0.2.0+ for the Event Dismiss Stats section to populate (sibling Plus plugin, not yet upstreamed; without it that one section just shows "no events logged yet")

---

## Configuration

The plugin config has four sections.

![Plugin settings panel in the launcher sidebar](assets/settings-panel.png)

### Behavior

| Setting | Default | Notes |
|---|---|---|
| Auto-open dashboard on startup | ON | Open the floating window when the plugin enables |
| Poll interval (sec) | 5 | How often to refresh from game state (1-60) |
| Nearby NPCs max distance (tiles) | 20 | Filter for the NPC section (1-200) |

### Layout

Ten boolean toggles, one per panel. All default ON. Untick any section to hide it; the floating window re-evaluates immediately.

### Notifications

| Setting | Default | Notes |
|---|---|---|
| Discord webhook URL | (blank) | Paste your channel webhook here. Field is masked in the UI. Leave blank to disable Discord entirely. |
| Notify on level-up | ON | "Level up: Mining 53 → 54" |
| Notify on random event | ON | "Random event detected (1 new entry in EventDismiss log)" |
| Notify on session start/stop | OFF | "Dashboard session started." / "Dashboard session stopped." |
| Notify on alert threshold | ON | "ALERT: Mining reached level 60!" |

### Alerts

| Setting | Default | Notes |
|---|---|---|
| Alert thresholds | (blank) | Comma-separated `SKILL:LEVEL` pairs, e.g. `MINING:60, WOODCUTTING:80, FISHING:70` |

When any skill reaches its threshold, the dashboard shows a yellow banner at the top of the window and (if Discord is configured) sends a notification. Each threshold fires exactly once per session.

---

## Discord webhook setup

If you've never created a Discord webhook:

1. In Discord, open the channel you want notifications sent to
2. Channel name → gear icon → **Integrations** → **Webhooks** → **New Webhook**
3. Give it a name + avatar (optional) → **Copy Webhook URL**
4. Paste into the plugin config's **Discord webhook URL** field

The webhook URL is treated as a secret: never logged on error, never returned in exception messages. The UI field is masked.

---

## What this plugin is NOT

- **Not a bot controller.** Observes and reports. Doesn't schedule, decide, or run game logic.
- **Not a replacement for in-game overlays.** Per-plugin overlays still appear in-game. The dashboard adds an aggregate layer.
- **Not a remote-view tool.** Lives in the client process. No HTTP, no port, no LAN access.
- **Not for vanilla RuneLite users.** Requires Microbot client APIs.

---

## Disclaimer

This plugin is for educational and research purposes only. Use at your own risk. Automation or integration may violate OSRS or Discord terms of service. The developers are not responsible for any consequences resulting from the use of this plugin.

---

## Credits

Pilot #6 of the Skill Plus Template (SPT) lineage. First non-skilling Plus plugin in the Hub.

- Original PowerShell dashboard concept + Java port: Pete (pjmarz)
- Iterative development via Claude Code

Built on the **Microbot Agent Server** API. See `CHANGELOG.md` for per-version notes.

For issues or suggestions, open an issue on the Microbot Hub repository.
