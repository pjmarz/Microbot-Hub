package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.Consumer;

/**
 * Base for every dashboard section panel. Wires up:
 * <ul>
 *     <li>A consistent header row (title + optional muted subtitle / controls).</li>
 *     <li>A BorderLayout body with the section content centered.</li>
 *     <li>Auto-registration with the {@link GameStatePoller} on construct and
 *         {@link #detach()} cleanup on plugin shutdown.</li>
 * </ul>
 *
 * <p>Subclasses override {@link #applySnapshot(PollSnapshot)} to update their
 * own components on the EDT.
 */
public abstract class DashboardSection extends JPanel {

    protected final GameStatePoller poller;
    private final Consumer<PollSnapshot> snapshotListener;
    private final JPanel headerLeft;
    private final JLabel subtitleLabel;
    private final JPanel headerRight;

    protected DashboardSection(String title, GameStatePoller poller) {
        super(new BorderLayout(0, 4));
        this.poller = poller;
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        // Header.
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerLeft.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLeft.add(titleLabel);

        subtitleLabel = new JLabel("");
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setFont(FontManager.getRunescapeSmallFont());
        headerLeft.add(subtitleLabel);

        header.add(headerLeft, BorderLayout.WEST);

        headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerRight.setOpaque(false);
        header.add(headerRight, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        snapshotListener = this::applySnapshot;
        poller.addListener(snapshotListener);
    }

    public void detach() {
        poller.removeListener(snapshotListener);
    }

    protected void setSubtitle(String text) {
        subtitleLabel.setText(text == null ? "" : text);
    }

    /** Add an in-header control (right-aligned), e.g. a JSpinner or "X tiles" label. */
    protected void addHeaderControl(Component c) {
        headerRight.add(c);
    }

    /** Called on every snapshot. Always invoked on the EDT. */
    protected abstract void applySnapshot(PollSnapshot snapshot);
}
