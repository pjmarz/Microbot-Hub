package net.runelite.client.plugins.microbot.runecraftplus;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
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

public class AutoRunecraftPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoRunecraftPlusPlugin plugin;
    private final Client client;
    private final AutoRunecraftPlusConfig config;

    public final ButtonComponent pauseButton;

    @Inject
    AutoRunecraftPlusOverlay(AutoRunecraftPlusPlugin plugin, Client client, AutoRunecraftPlusConfig config) {
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
            Microbot.log("AutoRunecraftPlus: pause button click received -- toggling pauseAllScripts");
            Microbot.pauseAllScripts.set(!Microbot.pauseAllScripts.get());
            if (Microbot.pauseAllScripts.get()) {
                Rs2Walker.setTarget(null);
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(240, 300));

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoRunecraftPlus v" + AutoRunecraftPlusPlugin.version)
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

            AutoRunecraftPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.RUNECRAFT);
                int currentXp = client.getSkillExperience(Skill.RUNECRAFT);
                int xpGained = currentXp - script.getStartSkillXp();
                long runtimeMillis = System.currentTimeMillis() - script.getStartTimeMillis();
                long xpPerHour = (runtimeMillis > 1000) ? (xpGained * 3600000L / runtimeMillis) : 0;

                int levelDelta = currentLevel - script.getStartSkillLevel();
                String levelStr = currentLevel + (levelDelta > 0 ? " (+" + levelDelta + ")" : "");

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runecraft level:")
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
                        .left("Trips:")
                        .right(String.valueOf(script.getActionsCompleted()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: NET = runePrice * runesCrafted - essencePrice * essenceUsed, over runtime.
                // The script counts craft actions, not runes, so we derive both from Runecraft XP:
                // essenceUsed = xpGained / altar.xpPerEssence (exact -- bonus runes give no extra XP),
                // and runesCrafted is estimated as 1 rune per essence (lower bound; the multiple-runes
                // -per-essence bonus at higher levels is not counted, so this under-states profit at
                // high levels). The "~" marks this estimate. runePrice is name-resolved + cached (the
                // altar enum carries only the rune name, not its item id); essencePrice is a direct id
                // lookup. Guards runtime 0 and price 0. NET can be negative (essence dearer than rune).
                long gpPerHour = 0;
                Altars activeAltar = script.getAltar();
                if (activeAltar != null && runtimeMillis > 1000 && activeAltar.getXpPerEssence() > 0) {
                    long essenceUsed = Math.round(xpGained / activeAltar.getXpPerEssence());
                    long runesCrafted = essenceUsed; // lower bound: 1 rune per essence
                    int runePrice = runePrice(activeAltar);
                    int essencePrice = Microbot.getItemManager().getItemPrice(script.getEssenceId());
                    long net = runePrice * runesCrafted - (long) essencePrice * essenceUsed;
                    gpPerHour = net * 3600000L / runtimeMillis;
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

            pauseButton.setText(Microbot.pauseAllScripts.get() ? "Resume" : "Pause");
            panelComponent.getChildren().add(pauseButton);

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatDuration(Duration duration) {
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    // Cache the resolved rune item id so the name search runs only when the active altar changes,
    // not every render frame. search() scans the whole item database; getItemPrice by id is cheap.
    private Altars cachedRuneAltar;
    private int cachedRuneId = 0;

    /**
     * GE price of the rune crafted at {@code altar}. The altar enum carries the rune's display name
     * (e.g. "Fire Rune") but not its item id, so we resolve the id once via the item manager's name
     * search (exact match) and cache it. Returns 0 when no item matches, so the overlay shows GP/hr 0
     * rather than mispricing.
     */
    private int runePrice(Altars altar) {
        if (altar != cachedRuneAltar) {
            cachedRuneAltar = altar;
            cachedRuneId = resolveRuneId(altar);
        }
        return cachedRuneId > 0 ? Microbot.getItemManager().getItemPrice(cachedRuneId) : 0;
    }

    private int resolveRuneId(Altars altar) {
        String runeName = altar.getRuneName();
        if (runeName == null) {
            return 0;
        }
        List<ItemPrice> matches = Microbot.getItemManager().search(runeName);
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        for (ItemPrice match : matches) {
            if (match.getName() != null && match.getName().equalsIgnoreCase(runeName)) {
                return match.getId();
            }
        }
        return 0;
    }
}
