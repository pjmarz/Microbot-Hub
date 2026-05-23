package net.runelite.client.plugins.microbot.eventdismissplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.eventdismissplus.events.RandomEventNpcHandler;
import net.runelite.client.plugins.microbot.eventdismissplus.events.StrangePlantHandler;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

/**
 * EventDismissPlus -- Pilot #5 of the Skill Plus Template (SPT) lineage.
 *
 * <p>Random event handling as a companion plugin. Registers two {@link
 * net.runelite.client.plugins.microbot.BlockingEvent}s with Microbot's global event manager:
 * <ul>
 *   <li>{@link RandomEventNpcHandler} -- for NPC-based events (Genie, Sandwich Lady, etc.)</li>
 *   <li>{@link StrangePlantHandler} -- for the Strange Plant GameObject</li>
 * </ul>
 *
 * <p>When either event's {@code validate()} returns true, Microbot interrupts whatever script
 * is currently running, executes the handler, then resumes. Same pattern WoodcuttingPlus uses
 * for its 9 Forestry events. User enables this plugin alongside any other plugin to benefit.
 *
 * <p>v0.1.0 (Tier B): variable response delays, 10 engaged event types, Genie lamp completion
 * with auto-detected active skill, Strange Plant pickup. Everything else dismissed via the
 * universal {@code npc.click("Dismiss")} action.
 */
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Event Dismiss Plus",
        description = "Plus fork of EventDismiss. Random event handling with variable delays, Genie lamp auto-skill detection, and engagement with 10 high-value events. Companion plugin: enable alongside any other plugin.",
        tags = {"random", "events", "antiban", "plus"},
        authors = {"Mocrosoft", "Pete (Plus fork)"},
        version = EventDismissPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class EventDismissPlusPlugin extends Plugin {
    public static final String version = "0.1.0";

    @Inject
    private EventDismissPlusConfig config;

    @Inject
    private EventDismissPlusScript script;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private EventDismissPlusOverlay overlay;

    private RandomEventNpcHandler npcHandler;
    private StrangePlantHandler plantHandler;

    @Provides
    EventDismissPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(EventDismissPlusConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        script.run(config);

        npcHandler = new RandomEventNpcHandler(config, script);
        Microbot.getBlockingEventManager().add(npcHandler);

        plantHandler = new StrangePlantHandler(config, script);
        Microbot.getBlockingEventManager().add(plantHandler);
    }

    @Override
    protected void shutDown() {
        if (npcHandler != null) {
            Microbot.getBlockingEventManager().remove(npcHandler);
            npcHandler = null;
        }
        if (plantHandler != null) {
            Microbot.getBlockingEventManager().remove(plantHandler);
            plantHandler = null;
        }
        script.shutdown();
        if (overlayManager != null) {
            overlayManager.remove(overlay);
        }
    }

    public EventDismissPlusScript getScript() {
        return script;
    }
}
