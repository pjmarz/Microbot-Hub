package net.runelite.client.plugins.microbot.cookingplus;

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
        name = PluginDescriptor.Mocrosoft + "Auto Cooking Plus",
        description = "Cooks food at a range or fire, banks between loads, with a location picker, stop conditions and live stats. Part of the Plus suite.",
        tags = {"cooking", "microbot", "skilling", "plus"},
        authors = {"George", "pjmarz"},
        version = AutoCookingPlusPlugin.version,
        minClientVersion = "2.0.13",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AutoCookingPlusPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AutoCookingPlusPlugin/assets/card.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoCookingPlusPlugin extends Plugin {
    public static final String version = "0.1.2";

    @Inject
    private AutoCookingPlusConfig config;

    @Provides
    AutoCookingPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoCookingPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoCookingPlusOverlay cookingOverlay;

    @Inject
    private AutoCookingPlusScript cookingScript;

    @Override
    protected void startUp() throws AWTException {
        // Clear any stale pause flag from a previous session (matches the rest of the Plus suite).
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(cookingOverlay);
            // Critical: hookMouseListener() wires the Pause button's click into RuneLite's mouse
            // event system. Without it the button renders but clicks pass through to the game.
            cookingOverlay.pauseButton.hookMouseListener();
        }
        cookingScript.run(config);
    }

    @Override
    protected void shutDown() {
        // Clear the flag so plugins started after us don't inherit our paused state.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        cookingScript.shutdown();
        if (cookingOverlay != null) {
            cookingOverlay.pauseButton.unhookMouseListener();
        }
        overlayManager.remove(cookingOverlay);
    }

    /** The overlay reads script stats via this getter. */
    public AutoCookingPlusScript getScript() {
        return cookingScript;
    }
}
