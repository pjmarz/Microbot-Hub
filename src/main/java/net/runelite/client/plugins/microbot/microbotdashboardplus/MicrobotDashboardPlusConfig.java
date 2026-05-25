package net.runelite.client.plugins.microbot.microbotdashboardplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Configuration for the MicrobotDashboardPlus plugin.
 *
 * <p>@ConfigGroup is "MicrobotDashboardPlus" to namespace these settings cleanly
 * away from the AgentServerPlugin, EventDismissPlus, and other Hub plugins.
 */
@ConfigGroup("MicrobotDashboardPlus")
@ConfigInformation(
    "<h2>Microbot Dashboard Plus</h2>" +
    "<h3>Version: " + MicrobotDashboardPlusPlugin.version + "</h3>" +
    "<p>Browser-based monitoring dashboard for your Microbot session. Embeds the dashboard files inside the plugin JAR and serves them on a local port. Open the URL in any browser; the page polls the Agent Server every few seconds.</p>" +
    "<p><strong>Requirement:</strong> the <code>[M] Agent Server</code> plugin must also be enabled. This plugin proxies <code>/api/*</code> requests to it.</p>" +
    "<p><strong>Default URL:</strong> <code>http://localhost:8088/</code>. Change the port below if you need to avoid a conflict (e.g. if the PowerShell <code>serve.ps1</code> is also running).</p>"
)
public interface MicrobotDashboardPlusConfig extends Config {

    @ConfigSection(name = "Server", description = "HTTP server settings", position = 0)
    String serverSection = "server";

    @ConfigSection(name = "Behavior", description = "Auto-open browser, dev mode, etc.", position = 1)
    String behaviorSection = "behavior";

    @ConfigItem(
            keyName = "serverPort",
            name = "Server port",
            description = "Port for the embedded HTTP server. The dashboard will be at http://localhost:<port>/. Change if 8088 is taken (e.g. serve.ps1 running).",
            position = 0,
            section = serverSection
    )
    @Range(min = 1024, max = 65535)
    default int serverPort() {
        return 8088;
    }

    @ConfigItem(
            keyName = "agentServerPort",
            name = "Agent Server port",
            description = "Port the [M] Agent Server plugin listens on (defaults to 8081). Plugin proxies /api/* requests here.",
            position = 1,
            section = serverSection
    )
    @Range(min = 1024, max = 65535)
    default int agentServerPort() {
        return 8081;
    }

    @ConfigItem(
            keyName = "autoOpenBrowser",
            name = "Auto-open browser on startup",
            description = "When the plugin starts, open the dashboard URL in your default browser automatically.",
            position = 0,
            section = behaviorSection
    )
    default boolean autoOpenBrowser() {
        return true;
    }

    @ConfigItem(
            keyName = "devModePath",
            name = "Dev-mode dashboard path",
            description = "When non-empty, serves dashboard HTML/CSS/JS from this filesystem path instead of the bundled JAR resources. Useful for iterating on dashboard files without rebuilding the plugin. Leave blank for normal (production) use.",
            position = 1,
            section = behaviorSection
    )
    default String devModePath() {
        return "";
    }
}
