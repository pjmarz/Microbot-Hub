# Microbot Watchdog

Polls the Agent Server, detects hangs, restarts the Microbot Launcher automatically.

Solves the "left it running for a soak, came back to a frozen client" scenario. Turns multi-hour hang downtime into a sub-2-minute recovery.

## What it detects

| Trigger | Description | Default threshold |
|---|---|---|
| `HTTP_TIMEOUT` | Agent Server unresponsive while the launcher is running (hard freeze) | 10s timeout |
| `GAME_STATE_FROZEN` | `state.gameState` stuck in `LOGIN_SCREEN` / `HOPPING` | 5 min |
| `LOGGED_OUT_TOO_LONG` | `state.loggedIn=false` but scripts are still flagged active | 5 min |
| `POSITION_FROZEN` | `state.player.position` unchanged while a script is active | 10 min |
| `XP_FROZEN` | Total XP across all skills unchanged while a script is active (soft hang) | 15 min |

## What it does NOT do

- **No auto-restart when the launcher is closed.** If you intentionally close the launcher (e.g., to go to bed), the watchdog will sit and wait until you start it again. No surprise restarts.
- **Will not double-restart during the grace period** (default 3 min after each restart). Avoids restart loops if the launcher takes a while to come back up.
- **Does not interfere with BreakHandler V2 breaks.** During a normal break, the launcher stays running, so detection logic doesn't fire. Position and XP being frozen during a break is expected — but the watchdog only triggers `POSITION_FROZEN` while a script is `active`, and during a BreakHandler-initiated logout the scripts are paused, so the check is skipped.

## Quick start

```powershell
# 1. Copy and customize config
Copy-Item .\config.example.ps1 .\config.local.ps1
notepad .\config.local.ps1   # tweak Discord webhook + thresholds if you want

# 2. Start the watchdog
.\watchdog.ps1
# OR double-click start-watchdog.cmd

# 3. Stop with Ctrl+C
```

## Configuration

All knobs live in `config.local.ps1` (gitignored). If absent, falls back to `config.example.ps1` with built-in defaults.

Key fields:

```powershell
$AgentUrl     = "http://127.0.0.1:8081"
$TokenPath    = "$env:USERPROFILE\.runelite\.agent-token"
$LauncherPath = "$env:USERPROFILE\AppData\Local\Programs\Microbot Launcher\Microbot Launcher.exe"

$PollIntervalSec       = 60     # 1 min between polls
$HttpTimeoutSec        = 10     # per-request timeout
$PositionFreezeThresh  = 600    # 10 min
$XpFreezeThresh        = 900    # 15 min
$GameStateFreezeThresh = 300    # 5 min
$LoggedOutThresh       = 300    # 5 min
$RestartGracePeriodSec = 180    # 3 min grace after restart

$DiscordWebhookUrl     = ""     # optional, see "Discord notifications" below
$LogCsvPath            = "$env:USERPROFILE\.runelite\microbot-watchdog.csv"
$VerboseConsole        = $true  # print every poll, not just events
```

Tune thresholds per usage:
- Tighter (5 min position, 10 min XP): faster recovery, more false positives during long banking trips or AFK random events
- Looser (15 min position, 20 min XP): fewer false positives, longer hangs before recovery
- Default values target a balance for active skilling like AutoMiningPlus or AutoWoodcuttingPlus

## Discord notifications

To get a Discord ping on every restart event:

1. In your Discord server: Settings -> Integrations -> Webhooks -> New Webhook
2. Copy the webhook URL
3. Paste into `$DiscordWebhookUrl` in `config.local.ps1`
4. Restart the watchdog

The ping format is:
```
Microbot watchdog restarting (HTTP_TIMEOUT): Agent Server unresponsive while launcher running at 14:32:18
```

Useful for unattended overnight runs. Skip if you're at the desk.

## CSV log

Every restart event (and the initial WATCHDOG_START) gets logged to `~/.runelite/microbot-watchdog.csv`:

```csv
timestamp,reason,details
2026-05-25T14:32:18.123-04:00,WATCHDOG_START,Polling every 60s
2026-05-25T16:42:55.421-04:00,HTTP_TIMEOUT,Agent Server unresponsive while launcher running
```

Analyze patterns over time:

```powershell
Import-Csv $env:USERPROFILE\.runelite\microbot-watchdog.csv | Group-Object reason | Sort-Object Count -Descending
```

Tells you what failure modes dominate. If `XP_FROZEN` dominates → maybe an antiban setting is too aggressive. If `HTTP_TIMEOUT` dominates → genuine client crashes.

## Verifying it works

End-to-end smoke test (run in two PowerShell windows):

**Window 1 - start the watchdog:**
```powershell
cd C:\Users\peter\src\Microbot-Hub\tools\agentserver\watchdog
.\watchdog.ps1
```

Look for the heartbeat lines every minute:
```
[14:33:18] OK | gameState=LOGGED_IN pos=3304,3315,0 active=3 restarts=0
[14:34:18] OK | gameState=LOGGED_IN pos=3304,3316,0 active=3 restarts=0
```

**Window 2 - simulate a hang by killing the client:**
```powershell
Get-Process | Where-Object { $_.MainWindowTitle -like '*RuneLite*' } | Stop-Process -Force
```

Within ~1 minute, Window 1 should:
1. Log `agent-unresponsive` (red)
2. Print `=== RESTART TRIGGERED: HTTP_TIMEOUT ===`
3. Kill all matching processes
4. Relaunch the launcher
5. Enter grace period

Within ~3 minutes, the launcher should be back, Agent Server reachable, and `restarts=1` in the heartbeat.

If all of that happens cleanly, the watchdog is operational.

## Running unattended

For a true overnight / multi-day setup:

1. **Use `start-watchdog.cmd`** to launch in a dedicated PowerShell window so you can monitor logs visually
2. **Or schedule via Task Scheduler** for boot-time auto-start (advanced)
3. **Enable Discord notifications** so you know when restarts happen
4. **Monitor the CSV log** after the soak for pattern analysis

Recommended companion: run the Agent Server dashboard (`tools/agentserver/dashboard/serve.ps1`) alongside the watchdog in another window. Watchdog handles the recovery; dashboard shows live state.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Failed to start listener on...` | Port collision with another service | Different port — only affects dashboard, watchdog uses outbound HTTP |
| Watchdog never restarts even though client is frozen | Wrong launcher process name in `$KillPatterns` | Run `Get-Process` to find actual process name, update `config.local.ps1` |
| Multiple restarts in a row | Grace period too short OR Agent Server takes >3 min to come back | Increase `$RestartGracePeriodSec` |
| False positives during banking trips | `$XpFreezeThresh` too tight | Increase to 1200 (20 min) |
| Restart fires but launcher doesn't come back | `$LauncherPath` wrong | Verify path exists, update config |

## Risks

- **Misses unique hang modes**. New OSRS update introduces a hang we don't detect — manual recovery still needed once, watchdog learns the pattern next iteration via threshold tuning
- **Process-name brittleness**. If Microbot renames the launcher exe, our kill patterns miss. Re-probe via `Get-Process` and update config
- **CSV log grows unbounded**. ~100 bytes/event, ~100 events/year typical. Won't be a problem for years; trim manually if it bothers you

## Files in this directory

```
watchdog/
+-- watchdog.ps1           # Main polling loop + restart logic
+-- config.example.ps1     # Defaults; ships with the repo
+-- config.local.ps1       # Your personalized config (gitignored)
+-- start-watchdog.cmd     # Windows convenience launcher
+-- README.md              # This file
```
