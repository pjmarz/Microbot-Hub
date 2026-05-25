package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Watchdog section: agent-server watchdog status, last event, last restart,
 * total restarts. v0.2.0 reports "unavailable" until the disk-log reader
 * reconnects in a follow-up patch.
 */
public class WatchdogPanel extends DashboardSection {

    private final JLabel status = mkValue();
    private final JLabel lastEvent = mkValue();
    private final JLabel lastRestart = mkValue();
    private final JLabel totalRestarts = mkValue();

    public WatchdogPanel(GameStatePoller poller) {
        super("Watchdog", poller);
        add(buildGrid(), java.awt.BorderLayout.CENTER);
    }

    private JPanel buildGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);

        int row = 0;
        addRow(grid, c, row++, "Status", status);
        addRow(grid, c, row++, "Last event", lastEvent);
        addRow(grid, c, row++, "Last restart", lastRestart);
        addRow(grid, c, row, "Total restarts", totalRestarts);

        return grid;
    }

    private static void addRow(JPanel grid, GridBagConstraints c, int row, String label, JLabel value) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        lbl.setFont(FontManager.getRunescapeSmallFont());
        grid.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1.0;
        grid.add(value, c);
    }

    private static JLabel mkValue() {
        JLabel l = new JLabel("--");
        l.setForeground(Color.WHITE);
        l.setFont(FontManager.getRunescapeSmallFont());
        return l;
    }

    @Override
    protected void applySnapshot(PollSnapshot snapshot) {
        if (snapshot == null || snapshot.getWatchdog() == null) {
            status.setText("--");
            return;
        }
        PollSnapshot.WatchdogStatus w = snapshot.getWatchdog();
        status.setText(w.getStatus() == null ? "--" : w.getStatus());
        switch (w.getStatus() == null ? "" : w.getStatus()) {
            case "ok":     status.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR); break;
            case "warn":   status.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR); break;
            case "bad":    status.setForeground(ColorScheme.PROGRESS_ERROR_COLOR); break;
            default:       status.setForeground(Color.GRAY);
        }
        lastEvent.setText(safe(w.getLastEventText()));
        lastRestart.setText(safe(w.getLastRestartText()));
        totalRestarts.setText(Integer.toString(w.getTotalRestarts()));
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "--" : s;
    }
}
