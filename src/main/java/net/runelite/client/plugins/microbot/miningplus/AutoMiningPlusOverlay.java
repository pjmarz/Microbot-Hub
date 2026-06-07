package net.runelite.client.plugins.microbot.miningplus;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.miningplus.data.Rocks;
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

/**
 * Runtime + XP + ores-mined overlay, with target-level progress and a Pause button.
 *
 * <p>The Pause button toggles {@link Microbot#pauseAllScripts}. A ButtonComponent only fires
 * its click handler once the parent Plugin calls {@code pauseButton.hookMouseListener()} after
 * {@code overlayManager.add()}; that hook lives in {@link AutoMiningPlusPlugin}. Pausing also
 * calls {@code Rs2Walker.setTarget(null)} because WebWalker runs on its own executor and does
 * not honor {@code Microbot.pauseAllScripts}, so an in-flight walk would otherwise continue.
 */
public class AutoMiningPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoMiningPlusPlugin plugin;
    private final Client client;
    private final AutoMiningPlusConfig config;

    // public final so the parent Plugin can call hookMouseListener() / unhookMouseListener()
    // in startUp() / shutDown().
    public final ButtonComponent pauseButton;

    @Inject
    AutoMiningPlusOverlay(AutoMiningPlusPlugin plugin, Client client, AutoMiningPlusConfig config) {
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
            Microbot.pauseAllScripts.set(!Microbot.pauseAllScripts.get());
            if (Microbot.pauseAllScripts.get()) {
                // Kill the in-flight walker. Without this, Rs2Walker keeps walking on its own
                // executor after the script's main loop pauses.
                Rs2Walker.setTarget(null);
                pauseButton.setText("Resume");
            } else {
                pauseButton.setText("Pause");
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(240, 300));
            // Do NOT clear children: that preserves the click-target registry across frames.

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoMiningPlus v" + AutoMiningPlusPlugin.version)
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

            AutoMiningPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.MINING);
                int currentXp = client.getSkillExperience(Skill.MINING);
                int xpGained = currentXp - script.getStartSkillXp();
                long runtimeMillis = System.currentTimeMillis() - script.getStartTimeMillis();
                long xpPerHour = (runtimeMillis > 1000) ? (xpGained * 3600000L / runtimeMillis) : 0;

                int levelDelta = currentLevel - script.getStartSkillLevel();
                String levelStr = currentLevel + (levelDelta > 0 ? " (+" + levelDelta + ")" : "");

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Level:")
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
                        .left("Ores mined:")
                        .right(String.valueOf(script.getActionsCompleted()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: gross profit (mined ore is free). orePrice = GE price of the active ore's
                // raw item, oresMined = the ore counter. Guards runtime 0 and price 0 (gem rocks,
                // basalt and salts have no single priced ore, so oreItemId is 0 -> shows 0).
                long gpPerHour = 0;
                Rocks activeRock = script.getActiveRock();
                if (activeRock != null && activeRock.getOreItemId() > 0 && runtimeMillis > 1000) {
                    int orePrice = Microbot.getItemManager().getItemPrice(activeRock.getOreItemId());
                    if (orePrice > 0) {
                        gpPerHour = (long) script.getActionsCompleted() * orePrice * 3600000L / runtimeMillis;
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

            // Pause button is added unconditionally. Its click handler fires because the parent
            // Plugin's startUp() calls pauseButton.hookMouseListener().
            panelComponent.getChildren().add(pauseButton);
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
