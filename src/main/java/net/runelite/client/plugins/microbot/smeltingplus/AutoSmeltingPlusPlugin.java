package net.runelite.client.plugins.microbot.smeltingplus;

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
        name = PluginDescriptor.Mocrosoft + "Auto Smelting Plus",
        description = "Smelts ores into bars at a configured furnace. Pick a Bar and Furnace; the bot walks there, banks ores, smelts, and repeats. Part of the Plus suite.",
        tags = {"smithing", "smelting", "microbot", "plus"},
        authors = {"Vince", "pjmarz"},
        version = AutoSmeltingPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AutoSmeltingPlusPlugin/assets/card.png",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AutoSmeltingPlusPlugin/assets/icon.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoSmeltingPlusPlugin extends Plugin {
    public static final String version = "0.5.15";

    @Inject
    private AutoSmeltingPlusConfig config;

    @Provides
    AutoSmeltingPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoSmeltingPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoSmeltingPlusOverlay overlay;

    @Inject
    AutoSmeltingPlusScript script;

    @Override
    protected void startUp() throws AWTException {
        // v0.5.7: clear any stale pause flag from a previous session.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(overlay);
            // v0.5.6: wires the pause button's setOnClick lambda to RuneLite's mouse event
            // system. Without this, setOnClick is a no-op. See AutoMiningPlusPlugin v0.5.6.
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
    public AutoSmeltingPlusScript getScript() {
        return script;
    }
}
