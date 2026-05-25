package net.runelite.client.plugins.microbot.microbotdashboardplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Configuration for the MicrobotDashboardPlus plugin (v0.2.0 Swing rewrite).
 *
 * <p>HTTP-era fields ({@code serverPort}, {@code agentServerPort},
 * {@code devModePath}) are gone. The plugin no longer runs a server; it polls
 * the Microbot client APIs directly and renders into a native Swing window.
 */
@ConfigGroup("MicrobotDashboardPlus")
@ConfigInformation(
    "<h2>Microbot Dashboard Plus</h2>" +
    "<h3>Version: " + MicrobotDashboardPlusPlugin.version + "</h3>" +
    "<p>Native Swing monitoring dashboard for your Microbot session. Opens in a floating window outside the client, like RuneLite's Var Inspector.</p>" +
    "<p>A compact summary lives in the right sidebar as a plugin panel. Click <strong>Open Dashboard</strong> to launch the full window.</p>" +
    "<p>No HTTP server, no port conflicts, no external dependencies. Reads game state directly from the client.</p>"
)
public interface MicrobotDashboardPlusConfig extends Config {

    @ConfigSection(name = "Behavior", description = "Window + polling settings", position = 0)
    String behaviorSection = "behavior";

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
}
