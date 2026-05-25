package net.runelite.client.plugins.microbot.microbotdashboardplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.plugins.microbot.microbotdashboardplus.window.DashboardWindow;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * MicrobotDashboardPlus -- Pilot #6 of the Skill Plus Template (SPT) lineage.
 *
 * <p>v0.2.0 (Swing rewrite): the dashboard now opens in a native floating
 * RuneLite window instead of a browser tab. A compact sidebar panel lives in
 * the right-hand toolbar; clicking <strong>Open Dashboard</strong> launches
 * the full {@link DashboardWindow}.
 *
 * <h2>What changed from v0.1.x</h2>
 * <ul>
 *     <li>No more embedded HTTP server. No port binding, no firewall prompts.</li>
 *     <li>No dependency on the {@code [M] Agent Server} plugin. Reads game state
 *         in-process via {@link Microbot#getClient()} + Rs2 utility APIs.</li>
 *     <li>No more browser tab lifecycle headaches (auto-close, sea-of-tabs, etc.).</li>
 *     <li>Dashboard HTML/CSS/JS deleted from the JAR.</li>
 * </ul>
 *
 * <h2>v0.2.0 known limitations</h2>
 * <ul>
 *     <li>XP-over-time chart not yet ported (deferred to v0.2.1; Java2D paint).</li>
 *     <li>Active scripts list is heuristic (enumerates enabled plugins).</li>
 *     <li>Watchdog status reads as "unavailable" until v0.2.x reconnects the
 *         disk log reader.</li>
 * </ul>
 */
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Microbot Dashboard Plus",
        description = "Native Swing monitoring dashboard for your Microbot session. Floating window plus a compact sidebar panel. No HTTP, no Agent Server dependency.",
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

    public static final String version = "0.2.0";

    @Inject
    private MicrobotDashboardPlusConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    private GameStatePoller poller;
    private DashboardPanel sidebarPanel;
    private NavigationButton navButton;
    private DashboardWindow window;

    @Provides
    MicrobotDashboardPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MicrobotDashboardPlusConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        // 1. Build the poller. Single-thread executor; starts immediately.
        poller = new GameStatePoller();
        poller.setNpcMaxDistance(config.npcMaxDistance());
        poller.start(config.pollIntervalSeconds());

        // 2. Build the floating window (hidden until shown).
        window = new DashboardWindow(poller);

        // 3. Build the sidebar panel + register it.
        sidebarPanel = new DashboardPanel(poller, this::showWindow);
        navButton = NavigationButton.builder()
                .tooltip("Microbot Dashboard Plus")
                .icon(buildPlaceholderIcon())
                .priority(7)
                .panel(sidebarPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        // 4. Optional auto-open on enable.
        if (config.autoOpenDashboard()) {
            showWindow();
        }

        Microbot.log("MicrobotDashboardPlus v" + version + " started (native Swing mode)");
    }

    @Override
    protected void shutDown() {
        if (window != null) {
            window.disposeWindow();
            window = null;
        }
        if (sidebarPanel != null) {
            sidebarPanel.detach();
            sidebarPanel = null;
        }
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (poller != null) {
            poller.stop();
            poller = null;
        }
        Microbot.log("MicrobotDashboardPlus v" + version + " stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!"MicrobotDashboardPlus".equals(event.getGroup())) return;
        if (poller == null) return;

        switch (event.getKey()) {
            case "pollIntervalSeconds":
                // Cheap restart of the scheduled task; safe while listeners
                // remain registered (they retain their references).
                poller.stop();
                poller.start(config.pollIntervalSeconds());
                break;
            case "npcMaxDistance":
                poller.setNpcMaxDistance(config.npcMaxDistance());
                poller.refreshNow();
                break;
            default:
                // autoOpenDashboard only matters at startUp; no live action.
                break;
        }
    }

    private void showWindow() {
        if (window == null) return;
        SwingUtilities.invokeLater(window::showOrFocus);
    }

    /**
     * Programmatic 16x16 RuneLite-green "D" icon. Avoids needing a PNG
     * resource for v0.2.0; v1.0.0 ships a proper icon.
     */
    private static BufferedImage buildPlaceholderIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(0x00AA00));
            g.fillRoundRect(0, 0, 16, 16, 4, 4);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString("D", 4, 13);
        } finally {
            g.dispose();
        }
        return img;
    }
}
