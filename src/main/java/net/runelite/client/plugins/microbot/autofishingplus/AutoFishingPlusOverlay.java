package net.runelite.client.plugins.microbot.autofishingplus;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
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
 * Stats + Pause overlay for AutoFishingPlus. The Pause button toggles
 * {@link Microbot#pauseAllScripts}; the parent plugin's startUp() must call
 * {@link ButtonComponent#hookMouseListener()} or the click passes through to the game.
 */
public class AutoFishingPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = Color.decode("#77DD77");
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoFishingPlusPlugin plugin;
    private final Client client;
    private final AutoFishingPlusConfig config;

    // Cached GE price of the raw catch, refreshed while it is in the pack so GP/hr survives banking.
    private int cachedFishPrice = 0;

    // public final so the parent Plugin can hook/unhook the mouse listener in startUp()/shutDown().
    public final ButtonComponent pauseButton;

    @Inject
    AutoFishingPlusOverlay(AutoFishingPlusPlugin plugin, Client client, AutoFishingPlusConfig config) {
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
                // Kill any in-flight walker target so a bank/spot walk stops on pause.
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

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoFishingPlus v" + AutoFishingPlusPlugin.version)
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

            AutoFishingPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.FISHING);
                int currentXp = client.getSkillExperience(Skill.FISHING);
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
                        .left("Fish caught:")
                        .right(String.valueOf(script.getFishCaught()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // Profit estimate: cache the catch GE price while it's in the pack so GP/hr
                // survives after depositing. Approximate for mixed catches (uses whatever catch
                // item is currently held). Skip burnt states, which have no meaningful value.
                if (config.fishToCatch() != null) {
                    for (String n : config.fishToCatch().getItemNames()) {
                        if (!n.startsWith("Burnt") && Rs2Inventory.hasItem(n)) {
                            int p = Microbot.getItemManager().getItemPrice(Rs2Inventory.get(n).getId());
                            if (p > 0) { cachedFishPrice = p; break; }
                        }
                    }
                }
                long gpPerHour = (runtimeMillis > 1000 && cachedFishPrice > 0)
                        ? ((long) script.getFishCaught() * cachedFishPrice * 3600000L / runtimeMillis) : 0;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("GP/hr (est):")
                        .right("~" + NumberFormat.getInstance().format(gpPerHour))
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
                    if (toGo > 0 && xpPerHour > 0) {
                        long xpRemaining = Math.max(0, Experience.getXpForLevel(config.targetLevel()) - currentXp);
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("ETA:")
                                .right(formatDuration(Duration.ofMillis(xpRemaining * 3600000L / xpPerHour)))
                                .rightColor(HIGHLIGHT_COLOR)
                                .build());
                    }
                }
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("(not running)")
                        .leftColor(NORMAL_TEXT_COLOR)
                        .build());
            }

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
