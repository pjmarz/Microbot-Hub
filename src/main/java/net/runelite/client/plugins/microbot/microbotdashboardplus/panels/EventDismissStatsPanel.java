package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Event Dismiss stats section.
 *
 * <p>v0.2.0 ships as a "data not yet wired" placeholder. The EventDismissPlus
 * v0.2.0 CSV log exists on disk, but the in-process reader is deferred to a
 * v0.2.x follow-up commit. Spans full width.
 */
public class EventDismissStatsPanel extends DashboardSection {

    private final JLabel placeholder;

    public EventDismissStatsPanel(GameStatePoller poller) {
        super("Event Dismiss Stats", poller);
        setSubtitle("(reader not yet wired in v0.2.0)");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setPreferredSize(new Dimension(780, 120));

        placeholder = new JLabel("EventDismissPlus CSV reader lands in a v0.2.x follow-up.");
        placeholder.setForeground(Color.GRAY);
        placeholder.setFont(FontManager.getRunescapeSmallFont());
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(placeholder, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    @Override
    protected void applySnapshot(PollSnapshot snapshot) {
        // Nothing to update from the in-process snapshot yet.
        // The CSV-on-disk reader will hook in once it lands.
        if (snapshot == null) return;
        placeholder.setForeground(snapshot.isLoggedIn() ? Color.GRAY : ColorScheme.MEDIUM_GRAY_COLOR);
    }
}
