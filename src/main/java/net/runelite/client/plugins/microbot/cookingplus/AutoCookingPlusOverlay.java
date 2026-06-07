package net.runelite.client.plugins.microbot.cookingplus;

import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingItem;
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
 * Stats + Pause overlay for AutoCookingPlus. The Pause button toggles {@link Microbot#pauseAllScripts}.
 * The parent plugin's startUp() must call {@link ButtonComponent#hookMouseListener()} or the click
 * passes through to the game.
 */
public class AutoCookingPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = Color.decode("#77DD77");
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoCookingPlusPlugin plugin;
    private final Client client;
    private final AutoCookingPlusConfig config;

    // public final so the parent Plugin can hook/unhook the mouse listener in startUp()/shutDown().
    public final ButtonComponent pauseButton;

    // GP/hr price cache: the cooked/raw GE prices only change when the active food changes, so the
    // ItemManager lookups are recomputed on food change rather than every render frame.
    private CookingItem cachedPriceItem;
    private int cachedCookedPrice;
    private int cachedRawPrice;

    @Inject
    AutoCookingPlusOverlay(AutoCookingPlusPlugin plugin, Client client, AutoCookingPlusConfig config) {
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
                    .text("AutoCookingPlus v" + AutoCookingPlusPlugin.version)
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

            AutoCookingPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.COOKING);
                int currentXp = client.getSkillExperience(Skill.COOKING);
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
                        .left("Items cooked:")
                        .right(String.valueOf(script.getItemsCooked()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: NET profit per cook = cooked-food GE price minus the raw-food price, times
                // the cooked counter, over runtime. The counter is XP-drop based (exact, no "~");
                // burnt food earns no XP so it is not counted -- net-per-successful-cook is correct
                // and we do not model burn loss. Can be negative when the raw food costs more than
                // the cooked product. Guards runtime 0 and price 0 (shows 0 if either side prices 0,
                // e.g. giant seaweed -> soda ash where the product may not have a GE price).
                long gpPerHour = 0;
                CookingItem activeItem = script.getActiveItem();
                if (activeItem != null && runtimeMillis > 1000) {
                    if (activeItem != cachedPriceItem) {
                        cachedCookedPrice = Microbot.getItemManager().getItemPrice(activeItem.getCookedItemID());
                        cachedRawPrice = Microbot.getItemManager().getItemPrice(activeItem.getRawItemID());
                        cachedPriceItem = activeItem;
                    }
                    if (cachedCookedPrice > 0) {
                        long netPerCook = (long) cachedCookedPrice - cachedRawPrice;
                        gpPerHour = netPerCook * script.getItemsCooked() * 3600000L / runtimeMillis;
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
