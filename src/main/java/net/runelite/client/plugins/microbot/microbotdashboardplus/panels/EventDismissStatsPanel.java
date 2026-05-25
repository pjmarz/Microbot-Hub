package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event Dismiss stats section.
 *
 * <p>v0.2.1: reads aggregated counts from
 * {@code ~/.runelite/eventdismissplus-events.csv} via the poller. Each row is
 * one event name (Genie, Strange Plant, etc.) with engaged / dismissed /
 * declined / errors / total counts. Rows sorted by total descending.
 */
public class EventDismissStatsPanel extends DashboardSection {

    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();
    private static final String[] COLUMNS = {"Event", "Engaged", "Dismissed", "Declined", "Errors", "Total"};

    private final EventStatsTableModel model = new EventStatsTableModel();
    private final JTable table;

    public EventDismissStatsPanel(GameStatePoller poller) {
        super("Event Dismiss Stats", poller);

        table = new JTable(model);
        table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        table.setForeground(Color.WHITE);
        table.setFont(FontManager.getRunescapeSmallFont());
        table.setRowHeight(18);
        table.setGridColor(ColorScheme.MEDIUM_GRAY_COLOR);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setFillsViewportHeight(true);
        table.setOpaque(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        header.setFont(FontManager.getRunescapeSmallFont());
        header.setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        for (int i = 1; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(70);
            table.getColumnModel().getColumn(i).setCellRenderer(new NumRenderer(i == 5));
        }
        table.getColumnModel().getColumn(0).setCellRenderer(new TextRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setPreferredSize(new Dimension(780, 140));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    protected void applySnapshot(PollSnapshot snapshot) {
        if (snapshot == null) return;
        List<PollSnapshot.EventDismissStatRow> rows = snapshot.getEventDismissStats();
        if (rows == null) rows = Collections.emptyList();
        model.update(rows);
        int total = rows.stream().mapToInt(PollSnapshot.EventDismissStatRow::getTotal).sum();
        setSubtitle(rows.isEmpty()
                ? "(no events logged yet)"
                : "(" + rows.size() + " event types, " + NUM.format(total) + " total)");
    }

    // ------------------------------------------------------------------

    private static final class EventStatsTableModel extends AbstractTableModel {
        private List<PollSnapshot.EventDismissStatRow> rows = new ArrayList<>();

        void update(List<PollSnapshot.EventDismissStatRow> newRows) {
            this.rows = new ArrayList<>(newRows);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) { return col == 0 ? String.class : Integer.class; }

        @Override
        public Object getValueAt(int row, int col) {
            PollSnapshot.EventDismissStatRow r = rows.get(row);
            switch (col) {
                case 0: return r.getEventName();
                case 1: return r.getEngaged();
                case 2: return r.getDismissed();
                case 3: return r.getDeclined();
                case 4: return r.getErrors();
                case 5: return r.getTotal();
                default: return "";
            }
        }
    }

    private static class TextRenderer extends DefaultTableCellRenderer {
        TextRenderer() { setFont(FontManager.getRunescapeSmallFont()); }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component cmp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            cmp.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            cmp.setForeground(Color.WHITE);
            return cmp;
        }
    }

    private static class NumRenderer extends DefaultTableCellRenderer {
        private final boolean isTotal;
        NumRenderer(boolean isTotal) {
            this.isTotal = isTotal;
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(FontManager.getRunescapeSmallFont());
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component cmp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            int n = (v instanceof Integer) ? (Integer) v : 0;
            setText(NUM.format(n));
            cmp.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            if (isTotal) {
                cmp.setForeground(Color.WHITE);
            } else {
                cmp.setForeground(n > 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.GRAY);
            }
            return cmp;
        }
    }
}
