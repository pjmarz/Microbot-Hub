package net.runelite.client.plugins.microbot.microbotdashboardplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Configuration for the MicrobotDashboardPlus plugin (v0.3.0).
 *
 * <p>Sections:
 * <ul>
 *     <li><b>Behavior</b> - core polling + window open semantics.</li>
 *     <li><b>Layout</b> - per-section visibility toggles.</li>
 *     <li><b>Notifications</b> - Discord webhook + which events fire.</li>
 *     <li><b>Alerts</b> - per-skill level-threshold alerts.</li>
 * </ul>
 */
@ConfigGroup("MicrobotDashboardPlus")
@ConfigInformation(
    "<h2>Microbot Dashboard Plus</h2>" +
    "<h3>Version: " + MicrobotDashboardPlusPlugin.version + "</h3>" +
    "<p>Native Swing monitoring dashboard for your Microbot session. Opens in a floating window outside the client, like RuneLite's Var Inspector.</p>" +
    "<p>A compact summary lives in the right sidebar as a plugin panel. Click <strong>Open Dashboard</strong> to launch the full window.</p>" +
    "<p>No HTTP server, no port conflicts, no external dependencies. Reads game state directly from the client.</p>" +
    "<p>v0.3.0 adds Discord webhook notifications, per-section visibility, and per-skill alert thresholds.</p>"
)
public interface MicrobotDashboardPlusConfig extends Config {

    @ConfigSection(name = "Behavior", description = "Window + polling settings", position = 0)
    String behaviorSection = "behavior";

    @ConfigSection(name = "Layout", description = "Which sections to show", position = 1)
    String layoutSection = "layout";

    @ConfigSection(name = "Notifications", description = "Discord webhook + which events fire", position = 2)
    String notificationsSection = "notifications";

    @ConfigSection(name = "Alerts", description = "Per-skill level-threshold alerts", position = 3)
    String alertsSection = "alerts";

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    @ConfigItem(
            keyName = "autoOpenDashboard",
            name = "Auto-open dashboard on startup",
            description = "When the plugin enables, open the floating dashboard window automatically. Untick this if you'd rather launch it manually from the sidebar panel.",
            position = 0,
            section = behaviorSection
    )
    default boolean autoOpenDashboard() {
        return true;
    }

    @ConfigItem(
            keyName = "pollIntervalSeconds",
            name = "Poll interval (sec)",
            description = "How often to refresh the dashboard from in-process game state. Lower = more responsive, slightly higher CPU. Default 5.",
            position = 1,
            section = behaviorSection
    )
    @Range(min = 1, max = 60)
    default int pollIntervalSeconds() {
        return 5;
    }

    @ConfigItem(
            keyName = "npcMaxDistance",
            name = "Nearby NPCs max distance (tiles)",
            description = "Maximum tile distance for NPCs to show in the Nearby NPCs section. Higher = more NPCs visible, slightly slower poll.",
            position = 2,
            section = behaviorSection
    )
    @Range(min = 1, max = 200)
    default int npcMaxDistance() {
        return 20;
    }

    // ------------------------------------------------------------------
    // Layout (per-section visibility)
    // ------------------------------------------------------------------

    @ConfigItem(keyName = "showPlayer", name = "Show Player", description = "Show the Player section.", position = 0, section = layoutSection)
    default boolean showPlayer() { return true; }

    @ConfigItem(keyName = "showActiveScripts", name = "Show Active Scripts", description = "Show the Active Scripts section.", position = 1, section = layoutSection)
    default boolean showActiveScripts() { return true; }

    @ConfigItem(keyName = "showPlusPlugins", name = "Show Plus Plugins", description = "Show the Plus Plugins quick-start section.", position = 2, section = layoutSection)
    default boolean showPlusPlugins() { return true; }

    @ConfigItem(keyName = "showInventory", name = "Show Inventory", description = "Show the Inventory section.", position = 3, section = layoutSection)
    default boolean showInventory() { return true; }

    @ConfigItem(keyName = "showSkills", name = "Show Skills", description = "Show the Skills section.", position = 4, section = layoutSection)
    default boolean showSkills() { return true; }

    @ConfigItem(keyName = "showNearbyNpcs", name = "Show Nearby NPCs", description = "Show the Nearby NPCs section.", position = 5, section = layoutSection)
    default boolean showNearbyNpcs() { return true; }

    @ConfigItem(keyName = "showWatchdog", name = "Show Watchdog", description = "Show the Watchdog section.", position = 6, section = layoutSection)
    default boolean showWatchdog() { return true; }

    @ConfigItem(keyName = "showXpChart", name = "Show XP Chart", description = "Show the XP-over-time chart section.", position = 7, section = layoutSection)
    default boolean showXpChart() { return true; }

    @ConfigItem(keyName = "showEventDismissStats", name = "Show Event Dismiss Stats", description = "Show the EventDismissPlus stats section.", position = 8, section = layoutSection)
    default boolean showEventDismissStats() { return true; }

    @ConfigItem(keyName = "showEventLog", name = "Show Event Log", description = "Show the Event Log ring buffer section.", position = 9, section = layoutSection)
    default boolean showEventLog() { return true; }

    // ------------------------------------------------------------------
    // Notifications (Discord webhook)
    // ------------------------------------------------------------------

    @ConfigItem(
            keyName = "discordWebhookUrl",
            name = "Discord webhook URL",
            description = "Paste a Discord channel webhook URL (https://discord.com/api/webhooks/...). Leave blank to disable Discord notifications. Treat this URL as a secret -- do not share it.",
            position = 0,
            section = notificationsSection,
            secret = true
    )
    default String discordWebhookUrl() {
        return "";
    }

    @ConfigItem(
            keyName = "notifyLevelUp",
            name = "Notify on level-up",
            description = "Send a Discord message when any skill level increases.",
            position = 1,
            section = notificationsSection
    )
    default boolean notifyLevelUp() {
        return true;
    }

    @ConfigItem(
            keyName = "notifyRandomEvent",
            name = "Notify on random event",
            description = "Send a Discord message when EventDismissPlus logs a new random-event row (engaged / dismissed / declined).",
            position = 2,
            section = notificationsSection
    )
    default boolean notifyRandomEvent() {
        return true;
    }

    @ConfigItem(
            keyName = "notifySessionLifecycle",
            name = "Notify on session start/stop",
            description = "Send a Discord message when the dashboard plugin enables (session start) or disables (session stop).",
            position = 3,
            section = notificationsSection
    )
    default boolean notifySessionLifecycle() {
        return false;
    }

    @ConfigItem(
            keyName = "notifyAlerts",
            name = "Notify on alert threshold",
            description = "Send a Discord message when any configured Alert Threshold is crossed (see Alerts section).",
            position = 4,
            section = notificationsSection
    )
    default boolean notifyAlerts() {
        return true;
    }

    // ------------------------------------------------------------------
    // Alerts
    // ------------------------------------------------------------------

    @ConfigItem(
            keyName = "alertThresholds",
            name = "Alert thresholds",
            description = "Comma-separated SKILL:LEVEL pairs. Example: MINING:60, WOODCUTTING:80, FISHING:70. Skill names follow the OSRS API enum (uppercase). Crossings fire an in-dashboard banner and (if enabled) a Discord notification.",
            position = 0,
            section = alertsSection
    )
    default String alertThresholds() {
        return "";
    }
}
