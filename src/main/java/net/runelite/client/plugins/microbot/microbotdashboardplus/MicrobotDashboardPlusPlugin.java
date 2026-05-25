package net.runelite.client.plugins.microbot.microbotdashboardplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * MicrobotDashboardPlus -- Pilot #6 of the Skill Plus Template (SPT) lineage.
 *
 * <p>First non-skilling Plus plugin. Companion plugin that embeds the Agent
 * Server browser dashboard into the Microbot client as a Hub plugin. Solves
 * the "two PowerShell windows running" UX of the {@code tools/agentserver/}
 * setup.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>On {@code startUp()}, starts an embedded HTTP server on the configured
 *       port (default 8088). The server serves the dashboard HTML/CSS/JS from
 *       JAR resources at
 *       {@code /net/runelite/client/plugins/microbot/microbotdashboardplus/dashboard/}.</li>
 *   <li>Reverse-proxies {@code /api/*} requests to the Agent Server plugin
 *       (port 8081 by default), attaching the {@code X-Agent-Token} header read
 *       from {@code ~/.runelite/.agent-token}.</li>
 *   <li>Also serves {@code /watchdog-log}, {@code /eventdismiss-log}, and
 *       {@code /history/log} directly from disk, same as the PowerShell
 *       {@code serve.ps1}.</li>
 *   <li>On {@code shutDown()}, stops the HTTP server cleanly.</li>
 * </ul>
 *
 * <h2>Requires</h2>
 * <ul>
 *   <li>The {@code [M] Agent Server} plugin must be enabled. This plugin
 *       proxies through it for game-state data.</li>
 * </ul>
 *
 * <p>v0.1.0 (this version): scaffold + embedded server + JAR-resource serve +
 * disk-mode override. Future:
 * <ul>
 *   <li>v0.2.0 - direct in-process Agent Server calls (skip HTTP)</li>
 *   <li>v0.2.0 - true global pause via {@code Microbot.pauseAllScripts.set(...)}</li>
 *   <li>v0.3.0 - WebSocket push for sub-100ms realtime</li>
 * </ul>
 */
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Microbot Dashboard Plus",
        description = "Browser-based monitoring dashboard for your Microbot session. Embeds the dashboard files in the plugin JAR and serves them on a local port (default http://localhost:8088/). Pilot #6 of the Skill Plus Template lineage; first non-skilling Plus plugin.",
        tags = {"dashboard", "monitoring", "microbot", "plus"},
        authors = {"Mocrosoft", "Pete (Plus fork)"},
        version = MicrobotDashboardPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class MicrobotDashboardPlusPlugin extends Plugin {

    public static final String version = "0.1.0";

    @Inject
    private MicrobotDashboardPlusConfig config;

    private DashboardHttpServer server;

    @Provides
    MicrobotDashboardPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MicrobotDashboardPlusConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        try {
            server = new DashboardHttpServer(config);
            server.start();

            String dashboardUrl = "http://localhost:" + config.serverPort() + "/";
            Microbot.log("MicrobotDashboardPlus: dashboard available at " + dashboardUrl);

            if (config.autoOpenBrowser()) {
                openInBrowser(dashboardUrl);
            }
        } catch (IOException ex) {
            log.error("MicrobotDashboardPlus: failed to start HTTP server on port "
                    + config.serverPort() + ": " + ex.getMessage(), ex);
            Microbot.log("MicrobotDashboardPlus: HTTP server failed to start - "
                    + ex.getMessage() + ". Try changing the port in plugin config.");
            // Don't rethrow -- let the plugin show as enabled but report the error.
            // User can disable + re-enable after fixing port.
        }
    }

    @Override
    protected void shutDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                Microbot.log("MicrobotDashboardPlus: opened browser to " + url);
            } else {
                Microbot.log("MicrobotDashboardPlus: Desktop.browse not supported; open "
                        + url + " manually");
            }
        } catch (Exception ex) {
            // Auto-open is a convenience; not a fatal error. Plugin still serves the URL.
            Microbot.log("MicrobotDashboardPlus: auto-open failed (" + ex.getMessage()
                    + "). Open " + url + " manually.");
        }
    }
}
