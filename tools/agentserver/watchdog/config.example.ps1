# Microbot Watchdog - example configuration.
#
# Copy to config.local.ps1 (gitignored) and tweak. The watchdog reads
# config.local.ps1 if present, falling back to these example values otherwise.

# === Agent Server ===
$AgentUrl   = "http://127.0.0.1:8081"
$TokenPath  = "$env:USERPROFILE\.runelite\.agent-token"

# === Microbot Launcher ===
# Path that will be Start-Processed for relaunch. Auto-discovered for Pete via
# `Get-Process | ? { $_.ProcessName -like '*Microbot*' }`.
$LauncherPath = "$env:USERPROFILE\AppData\Local\Programs\Microbot Launcher\Microbot Launcher.exe"

# Process-name patterns to kill on restart. The launcher spawns several helper
# processes plus the actual javaw.exe game client. Kill everything that matches
# any of these patterns.
$KillPatterns = @(
    "Microbot Launcher",
    "javaw"             # the game client (matched on title "RuneLite - <user>")
)

# Some javaw.exe processes are unrelated (e.g. IDEs, other tools). Only kill
# javaw if its MainWindowTitle matches this pattern. Leave empty to kill all
# javaw.
$JavawTitleFilter = "RuneLite"

# === Detection thresholds (seconds) ===
$PollIntervalSec        = 60     # poll every minute
$HttpTimeoutSec         = 10     # individual /state request timeout
$PositionFreezeThresh   = 600    # 10 min: position unchanged while logged-in + script active
$XpFreezeThresh         = 900    # 15 min: zero XP gain while logged-in + script active
$GameStateFreezeThresh  = 300    # 5 min: gameState stuck (LOGIN_SCREEN, HOPPING, etc.)
$LoggedOutThresh        = 300    # 5 min: not logged in despite scripts being active

# After a restart, give the launcher time to spin up before judging again.
$RestartGracePeriodSec  = 180    # 3 min grace after each restart

# === Optional: Discord webhook ===
# Leave blank to disable. Get one from your Discord server settings:
# Server -> Integrations -> Webhooks -> New Webhook -> Copy URL
$DiscordWebhookUrl = ""

# === Logging ===
$LogCsvPath = "$env:USERPROFILE\.runelite\microbot-watchdog.csv"
$VerboseConsole = $true   # $true = print every poll; $false = print only events
