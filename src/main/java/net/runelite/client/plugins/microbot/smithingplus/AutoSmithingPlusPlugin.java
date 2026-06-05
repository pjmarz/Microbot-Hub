package net.runelite.client.plugins.microbot.smithingplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Auto Smithing Plus",
        description = "Smiths bars into items at a configured anvil. Pick a Bar, Item and Anvil; the bot walks there, banks bars, smiths, and repeats. Part of the Plus suite.",
        tags = {"smithing", "anvil", "microbot", "plus"},
        authors = {"StickToTheScript", "pjmarz"},
        version = AutoSmithingPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AutoSmithingPlusPlugin/assets/card.png",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AutoSmithingPlusPlugin/assets/icon.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoSmithingPlusPlugin extends Plugin {
    public static final String version = "0.6.4";

    @Inject
    private AutoSmithingPlusConfig config;

    @Provides
    AutoSmithingPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoSmithingPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoSmithingPlusOverlay overlay;

    @Inject
    AutoSmithingPlusScript script;

    @Override
    protected void startUp() throws AWTException {
        // v0.5.7: clear any stale pause flag from a previous session.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(overlay);
            // v0.5.6: see AutoMiningPlusPlugin v0.5.6 -- hookMouseListener is what actually
            // wires setOnClick to RuneLite's mouse events.
            overlay.pauseButton.hookMouseListener();
        }
        script.run(config);
    }

    @Override
    protected void shutDown() {
        // v0.5.7: clear flag so other plugins enabled after us don't inherit our paused state.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        script.shutdown();
        if (overlay != null) {
            overlay.pauseButton.unhookMouseListener();
        }
        overlayManager.remove(overlay);
    }

    /** overlay reads script stats via this getter. */
    public AutoSmithingPlusScript getScript() {
        return script;
    }
}
