package net.runelite.client.plugins.microbot.smeltingplus;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.smeltingplus.data.Bars;
import net.runelite.client.plugins.microbot.smeltingplus.data.Ores;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Map;

/**
 * Status overlay for AutoSmeltingPlus. Shows current Smithing level (with delta), XP gained,
 * XP/hr, bars smelted, GP/hr, runtime, the target-level line, and a Pause/Resume button.
 */
public class AutoSmeltingPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoSmeltingPlusPlugin plugin;
    private final Client client;
    private final AutoSmeltingPlusConfig config;

    // Pause button toggles Microbot.pauseAllScripts (global flag). Public so the plugin's
    // startUp() can call hookMouseListener().
    public final ButtonComponent pauseButton;

    // Cached net GP per bar (bar price minus input ore cost). Recomputed only when activeBar
    // changes so the overlay does not re-price items on every render frame.
    private Bars cachedNetBar = null;
    private Long cachedNetPerBar = null;

    @Inject
    AutoSmeltingPlusOverlay(AutoSmeltingPlusPlugin plugin, Client client, AutoSmeltingPlusConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();

        pauseButton = new ButtonComponent("Pause");
        pauseButton.setPreferredSize(new Dimension(100, 25));
        pauseButton.setParentOverlay(this);
        pauseButton.setFont(FontManager.getRunescapeBoldFont());
        pauseButton.setOnClick(() -> {
            Microbot.log("AutoSmeltingPlus: pause button click received -- toggling pauseAllScripts");
            Microbot.pauseAllScripts.set(!Microbot.pauseAllScripts.get());
            if (Microbot.pauseAllScripts.get()) {
                // Kill the in-flight walker. Without this, Rs2Walker keeps walking on its own
                // executor after the script main loop pauses.
                Rs2Walker.setTarget(null);
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(240, 300));
            // No clear: preserves the click-target registry for the Pause button.

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoSmeltingPlus v" + AutoSmeltingPlusPlugin.version)
                    .color(TITLE_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(Microbot.status == null ? "Idle" : Microbot.status)
                    .rightColor(HIGHLIGHT_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().left("").build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Statistics")
                    .leftColor(HEADER_COLOR)
                    .build());

            AutoSmeltingPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.SMITHING);
                int currentXp = client.getSkillExperience(Skill.SMITHING);
                int xpGained = currentXp - script.getStartSkillXp();
                long runtimeMillis = System.currentTimeMillis() - script.getStartTimeMillis();
                long xpPerHour = (runtimeMillis > 1000) ? (xpGained * 3600000L / runtimeMillis) : 0;

                int levelDelta = currentLevel - script.getStartSkillLevel();
                String levelStr = currentLevel + (levelDelta > 0 ? " (+" + levelDelta + ")" : "");

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Smithing level:")
                        .right(levelStr)
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("XP gained:")
                        .right(NumberFormat.getInstance().format(xpGained))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("XP/hr:")
                        .right(NumberFormat.getInstance().format(xpPerHour))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Bars smelted:")
                        .right(String.valueOf(script.getActionsCompleted()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: NET profit per bar = bar GE price minus the cost of its input ores, times
                // the exact bar count. Can be negative when ore costs more than the bar. The
                // net-per-bar figure only changes when activeBar changes, so it is cached and
                // recomputed only then rather than priced every render frame.
                long gpPerHour = 0;
                Bars activeBar = script.getActiveBar();
                if (activeBar != null && runtimeMillis > 1000) {
                    Long netPerBar = netPerBarFor(activeBar);
                    if (netPerBar != null) {
                        long barsSmelted = script.getActionsCompleted();
                        gpPerHour = netPerBar * barsSmelted * 3600000L / runtimeMillis;
                    }
                }
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("GP/hr:")
                        .right(NumberFormat.getInstance().format(gpPerHour))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runtime:")
                        .right(formatDuration(Duration.ofMillis(runtimeMillis)))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // Target-level progress line.
                if (config.targetLevel() > 0) {
                    int toGo = Math.max(0, config.targetLevel() - currentLevel);
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Target:")
                            .right(config.targetLevel() + (toGo > 0 ? " (" + toGo + " to go)" : " (reached)"))
                            .rightColor(HIGHLIGHT_COLOR)
                            .build());
                }

            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("(not running)")
                        .leftColor(NORMAL_TEXT_COLOR)
                        .build());
            }

            // Pause button added unconditionally to win the click-bounds registration race
            // against the first render.
            pauseButton.setText(Microbot.pauseAllScripts.get() ? "Resume" : "Pause");
            panelComponent.getChildren().add(pauseButton);

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    /**
     * Net GP per bar (bar price minus input ore cost) for the given bar, or null when the bar
     * has no GE price. Cached and only recomputed when the bar changes.
     */
    private Long netPerBarFor(Bars bar) {
        if (bar == cachedNetBar) {
            return cachedNetPerBar;
        }
        cachedNetBar = bar;
        int barPrice = Microbot.getItemManager().getItemPrice(bar.getId());
        if (barPrice <= 0) {
            cachedNetPerBar = null;
            return null;
        }
        int inputOreCost = 0;
        for (Map.Entry<Ores, Integer> req : bar.getRequiredMaterials().entrySet()) {
            int oreId = req.getKey().getItemId();
            if (oreId > 0) {
                inputOreCost += Microbot.getItemManager().getItemPrice(oreId) * req.getValue();
            }
        }
        cachedNetPerBar = (long) barPrice - inputOreCost;
        return cachedNetPerBar;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
