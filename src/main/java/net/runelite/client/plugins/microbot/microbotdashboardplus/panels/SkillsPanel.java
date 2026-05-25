package net.runelite.client.plugins.microbot.microbotdashboardplus.panels;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.microbotdashboardplus.data.PollSnapshot;
import net.runelite.client.plugins.microbot.microbotdashboardplus.data.XpHistory;
import net.runelite.client.plugins.microbot.microbotdashboardplus.poller.GameStatePoller;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Skills section: 5-column JTable. Skill / Level / XP / +Δ / XP/hr.
 *
 * <ul>
 *     <li>Δ is gain since first observation (set when the poller starts).</li>
 *     <li>XP/hr is extrapolated from the 5-minute rolling window in {@link XpHistory}.</li>
 *     <li>Zero values render muted; positive deltas render green.</li>
 * </ul>
 */
public class SkillsPanel extends DashboardSection {

    private static final NumberFormat NUM = NumberFormat.getIntegerInstance();
    private static final String[] COLUMNS = {"Skill", "Level", "XP", "+Δ", "XP/hr"};
    private static final Skill[] SKILL_ORDER = buildSkillOrder();

    private static Skill[] buildSkillOrder() {
        List<Skill> list = new ArrayList<>();
        for (Skill s : Skill.values()) {
            if (s == Skill.OVERALL) continue;
            list.add(s);
        }
        return list.toArray(new Skill[0]);
    }

    private final SkillsTableModel model;
    private final JTable table;

    public SkillsPanel(GameStatePoller poller) {
        super("Skills", poller);
        setSubtitle("(Δ since session start · XP/hr 5-min rolling)");

        model = new SkillsTableModel();
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
        table.setSelectionBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        header.setFont(FontManager.getRunescapeSmallFont());
        header.setReorderingAllowed(false);

        // Column widths.
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        // Renderers.
        TableCellRenderer rightAlignMono = new MonoRightRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(rightAlignMono);
        table.getColumnModel().getColumn(2).setCellRenderer(rightAlignMono);
        table.getColumnModel().getColumn(3).setCellRenderer(new DeltaRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new RateRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.setPreferredSize(new Dimension(380, 420));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    protected void applySnapshot(PollSnapshot snapshot) {
        if (snapshot == null) return;
        model.update(snapshot, poller.getXpHistory());
    }

    // ------------------------------------------------------------------
    // Table model
    // ------------------------------------------------------------------

    private static final class SkillsTableModel extends AbstractTableModel {
        private final Object[][] rows = new Object[SKILL_ORDER.length][COLUMNS.length];

        SkillsTableModel() {
            for (int i = 0; i < SKILL_ORDER.length; i++) {
                rows[i][0] = capitalize(SKILL_ORDER[i].getName());
                rows[i][1] = "--";
                rows[i][2] = "--";
                rows[i][3] = 0;
                rows[i][4] = 0;
            }
        }

        void update(PollSnapshot snapshot, XpHistory history) {
            for (int i = 0; i < SKILL_ORDER.length; i++) {
                Skill s = SKILL_ORDER[i];
                Integer xp = snapshot.getSkillXp() == null ? null : snapshot.getSkillXp().get(s);
                Integer lvl = snapshot.getSkillLevels() == null ? null : snapshot.getSkillLevels().get(s);
                rows[i][1] = lvl == null ? "--" : NUM.format(lvl);
                rows[i][2] = xp == null ? "--" : NUM.format(xp);
                rows[i][3] = xp == null ? 0 : history.deltaSinceBaseline(s, xp);
                rows[i][4] = history.xpPerHour(s);
            }
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.length; }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public Object getValueAt(int row, int col) { return rows[row][col]; }
        @Override public boolean isCellEditable(int row, int col) { return false; }
        @Override public Class<?> getColumnClass(int col) { return col >= 3 ? Integer.class : String.class; }
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------------
    // Cell renderers
    // ------------------------------------------------------------------

    private static class MonoRightRenderer extends DefaultTableCellRenderer {
        MonoRightRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(FontManager.getRunescapeSmallFont());
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component cmp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            cmp.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            cmp.setForeground(Color.WHITE);
            return cmp;
        }
    }

    private static class DeltaRenderer extends DefaultTableCellRenderer {
        DeltaRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(FontManager.getRunescapeSmallFont());
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component cmp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            int delta = (v instanceof Integer) ? (Integer) v : 0;
            setText(delta > 0 ? "+" + NUM.format(delta) : "0");
            cmp.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            cmp.setForeground(delta > 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.GRAY);
            return cmp;
        }
    }

    private static class RateRenderer extends DefaultTableCellRenderer {
        RateRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(FontManager.getRunescapeSmallFont());
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component cmp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            int rate = (v instanceof Integer) ? (Integer) v : 0;
            setText(rate > 0 ? NUM.format(rate) : "--");
            cmp.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            cmp.setForeground(rate > 0 ? Color.WHITE : Color.GRAY);
            return cmp;
        }
    }
}
