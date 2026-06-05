package net.runelite.client.plugins.microbot.smithingplus;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.smithingplus.data.AnvilItem;
import net.runelite.client.plugins.microbot.smithingplus.data.Bars;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.http.api.item.ItemPrice;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.List;

/**
 * rebuilt overlay matching AutoWoodcuttingPlus standard.
 * Shows runtime, Smithing XP gained, XP/hr, smith cycles, current level + delta, status.
 */
public class AutoSmithingPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoSmithingPlusPlugin plugin;
    private final Client client;
    private final AutoSmithingPlusConfig config; // v0.5.0

    // v0.5.1: Pause button toggles Microbot.pauseAllScripts (global flag).
    // v0.5.6: public final so AutoSmithingPlusPlugin.startUp() can call hookMouseListener().
    public final ButtonComponent pauseButton;

    @Inject
    AutoSmithingPlusOverlay(AutoSmithingPlusPlugin plugin, Client client, AutoSmithingPlusConfig config) {
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
            Microbot.log("AutoSmithingPlus: pause button click received -- toggling pauseAllScripts");
            Microbot.pauseAllScripts.set(!Microbot.pauseAllScripts.get());
            if (Microbot.pauseAllScripts.get()) {
                // v0.5.7: kill in-flight walker. Matches AIO Fighter (AIOFighterInfoOverlay:39).
                // Without this, Rs2Walker keeps walking on its own executor after the script
                // main loop pauses.
                Rs2Walker.setTarget(null);
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(240, 300));
            // v0.5.2: no clear -- preserves click-target registry for the Pause button.

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoSmithingPlus v" + AutoSmithingPlusPlugin.version)
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

            AutoSmithingPlusScript script = plugin.getScript();
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
                        .left("Smith cycles:")
                        .right(String.valueOf(script.getActionsCompleted()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: NET profit per item = product GE price minus the bars it consumes. The
                // counter is smith cycles; each "Smith All" works a full inventory of bars (27
                // slots after the hammer), so items per cycle is roughly 27 / bars-per-item. Can be
                // negative when bars cost more than the product. Guards runtime 0 and price 0; the
                // product price is 0 for items whose name doesn't follow the "Tier base" pattern.
                // The "~" marks it an estimate (cycle->item conversion).
                long gpPerHour = 0;
                Bars activeBar = script.getActiveBar();
                AnvilItem activeItem = script.getActiveItem();
                if (activeBar != null && activeItem != null && runtimeMillis > 1000
                        && activeItem.getRequiredBars() > 0) {
                    int productPrice = productPrice(activeBar, activeItem);
                    int barPrice = Microbot.getItemManager().getItemPrice(activeBar.getId());
                    if (productPrice > 0) {
                        long netPerItem = (long) productPrice - (long) barPrice * activeItem.getRequiredBars();
                        long itemsSmithed = (long) script.getActionsCompleted()
                                * (27 / activeItem.getRequiredBars());
                        gpPerHour = netPerItem * itemsSmithed * 3600000L / runtimeMillis;
                    }
                }
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("GP/hr:")
                        .right("~" + NumberFormat.getInstance().format(gpPerHour))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runtime:")
                        .right(formatDuration(Duration.ofMillis(runtimeMillis)))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // v0.5.0: target-level progress line.
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

            // v0.5.4: Pause button added unconditionally to win the click-bounds registration
            // race against the first render. See AutoMiningPlusOverlay v0.5.4 comment for detail.
            pauseButton.setText(Microbot.pauseAllScripts.get() ? "Resume" : "Pause");
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

    // Cache the resolved product item id so the name search runs only when the active bar/item
    // changes, not every render frame. search() scans the whole item database; getItemPrice by id
    // is a cheap lookup.
    private Bars cachedProductBar;
    private AnvilItem cachedProductItem;
    private int cachedProductId = 0;

    /**
     * GE price of the product smithed from {@code bar} at {@code item}. AnvilItem carries no
     * product item id (it is widget-child driven), so we build the in-game name from the bar's
     * product prefix + the item's base name (e.g. "Bronze dagger") and resolve it once via the
     * item manager's name search (exact match), then cache the id. Returns 0 when the name can't be
     * built or no item matches, so the overlay shows GP/hr 0 rather than mispricing.
     */
    private int productPrice(Bars bar, AnvilItem item) {
        if (bar != cachedProductBar || item != cachedProductItem) {
            cachedProductBar = bar;
            cachedProductItem = item;
            cachedProductId = resolveProductId(bar, item);
        }
        return cachedProductId > 0 ? Microbot.getItemManager().getItemPrice(cachedProductId) : 0;
    }

    private int resolveProductId(Bars bar, AnvilItem item) {
        String prefix = bar.getProductPrefix();
        String base = item.getProductBaseName();
        if (prefix == null || base == null) {
            return 0;
        }
        String fullName = prefix + " " + base;
        List<ItemPrice> matches = Microbot.getItemManager().search(fullName);
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        for (ItemPrice match : matches) {
            if (match.getName() != null && match.getName().equalsIgnoreCase(fullName)) {
                return match.getId();
            }
        }
        return 0;
    }
}
