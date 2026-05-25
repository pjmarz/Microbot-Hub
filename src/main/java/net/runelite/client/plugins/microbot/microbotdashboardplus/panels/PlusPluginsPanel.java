package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;

/**
 * Plus Plugins quick start / stop section. Grid of compact rows, each
 * containing the plugin display name + an action button (Start when stopped,
 * Stop when running). Spans both columns of the dashboard grid.
 */
@Slf4j
public class PlusPluginsPanel extends DashboardSection {

    private final JPanel grid;

    public PlusPluginsPanel(GameStatePoller poller) {
        super("Plus Plugins", poller);
        setSubtitle("(quick start / stop)");

        grid = new JPanel(new GridLayout(0, 3, 6, 4));
        grid.setOpaque(false);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setPreferredSize(new Dimension(780, 120));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    protected void applySnapshot(PollSnapshot snapshot) {
        if (snapshot == null) return;
        List<PollSnapshot.PlusPluginStatus> rows = snapshot.getPlusPlugins();
        rebuildGrid(rows == null ? Collections.emptyList() : rows);
    }

    private void rebuildGrid(List<PollSnapshot.PlusPluginStatus> rows) {
        grid.removeAll();
        if (rows.isEmpty()) {
            JLabel empty = new JLabel("No Plus plugins detected");
            empty.setForeground(Color.GRAY);
            empty.setFont(FontManager.getRunescapeSmallFont());
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            grid.add(empty);
        } else {
            for (PollSnapshot.PlusPluginStatus p : rows) {
                grid.add(makeRow(p));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel makeRow(PollSnapshot.PlusPluginStatus p) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        // v0.3.2: dropped active-state border tint. The button color (red Stop
        // / green Start) is enough signal; the colored border was a redundant
        // double-indicator that confused some users.
        row.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

        JLabel name = new JLabel(p.getDisplayName());
        name.setForeground(p.isActive() ? Color.WHITE : Color.LIGHT_GRAY);
        name.setFont(FontManager.getRunescapeSmallFont());
        name.setBorder(new javax.swing.border.EmptyBorder(2, 6, 2, 0));
        row.add(name, BorderLayout.CENTER);

        JButton btn = new JButton(p.isActive() ? "Stop" : "Start");
        btn.setFont(FontManager.getRunescapeSmallFont());
        btn.setForeground(Color.WHITE);
        btn.setBackground(p.isActive() ? new Color(0x5c2929) : new Color(0x2c4e2c));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(60, 20));
        btn.addActionListener(e -> togglePlugin(p));
        row.add(btn, BorderLayout.EAST);

        return row;
    }

    private void togglePlugin(PollSnapshot.PlusPluginStatus p) {
        try {
            Plugin target = null;
            for (Plugin plugin : Microbot.getPluginManager().getPlugins()) {
                if (plugin != null && plugin.getClass().getName().equals(p.getPluginClassName())) {
                    target = plugin;
                    break;
                }
            }
            if (target == null) {
                log.warn("toggle: plugin {} not found", p.getPluginClassName());
                return;
            }
            if (p.isActive()) Microbot.stopPlugin(target);
            else Microbot.startPlugin(target);
            poller.refreshNow();
        } catch (Throwable t) {
            log.warn("toggle plugin failed for {}: {}", p.getPluginClassName(), t.getMessage(), t);
        }
    }

    // Avoid unused-import lint.
    @SuppressWarnings("unused")
    private static Component noop() { return null; }
}
