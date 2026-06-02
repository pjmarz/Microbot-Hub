package net.runelite.client.plugins.microbot.miningplus;

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
        name = PluginDescriptor.Mocrosoft + "Auto Mining Plus",
        description = "Mines and banks ores. Pick a mine location and the bot walks there before mining.",
        tags = {"mining", "microbot", "skilling", "plus"},
        version = AutoMiningPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoMiningPlusPlugin extends Plugin {
    public static final String version = "0.5.13";
    @Inject
    private AutoMiningPlusConfig config;
    @Provides
    AutoMiningPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoMiningPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoMiningPlusOverlay autoMiningOverlay;

    @Inject
    AutoMiningPlusScript autoMiningScript;


    @Override
    protected void startUp() throws AWTException {
        // v0.5.7: clear any stale pause flag from a previous session. Matches AIOFighterPlugin:132.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(autoMiningOverlay);
            // v0.5.6: critical -- ButtonComponent.setOnClick stores the lambda, but
            // hookMouseListener() is what actually wires it to RuneLite's mouse event system.
            // Without this, the pause button looks rendered but clicks pass through to the game.
            // Pattern copied from AIOFighterPlugin.startUp().
            autoMiningOverlay.pauseButton.hookMouseListener();
        }
        autoMiningScript.run(config);
    }

    protected void shutDown() {
        // v0.5.7: clear flag so other plugins enabled after us don't inherit our paused state.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        autoMiningScript.shutdown();
        if (autoMiningOverlay != null) {
            autoMiningOverlay.pauseButton.unhookMouseListener();
        }
        overlayManager.remove(autoMiningOverlay);
    }

    /** Polish-Cycle 2 v0.3.0: overlay reads script stats via this getter. */
    public AutoMiningPlusScript getScript() {
        return autoMiningScript;
    }
}
