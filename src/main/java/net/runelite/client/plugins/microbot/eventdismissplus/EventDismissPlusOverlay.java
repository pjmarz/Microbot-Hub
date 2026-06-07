package net.runelite.client.plugins.microbot.eventdismissplus;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;

public class EventDismissPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final EventDismissPlusPlugin plugin;

    @Inject
    EventDismissPlusOverlay(EventDismissPlusPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_RIGHT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(220, 120));
            panelComponent.getChildren().clear();

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("EventDismissPlus v" + EventDismissPlusPlugin.version)
                    .color(TITLE_COLOR)
                    .build());

            EventDismissPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Events handled:")
                        .right(String.valueOf(script.getEventsHandled()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                String last = script.getLastEventName();
                if (last != null) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Last event:")
                            .right(last)
                            .rightColor(HIGHLIGHT_COLOR)
                            .build());

                    long sinceMillis = System.currentTimeMillis() - script.getLastEventTime();
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Time since:")
                            .right(formatDuration(Duration.ofMillis(sinceMillis)))
                            .rightColor(NORMAL_TEXT_COLOR)
                            .build());
                } else {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Last event:")
                            .right("(none yet)")
                            .rightColor(NORMAL_TEXT_COLOR)
                            .build());
                }
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("(not running)")
                        .leftColor(NORMAL_TEXT_COLOR)
                        .build());
            }
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0) return String.format("%dh%02dm%02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm%02ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}
