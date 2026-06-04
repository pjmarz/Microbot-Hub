package net.runelite.client.plugins.microbot.runecraftplus;

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
        name = PluginDescriptor.Mocrosoft + "Auto Runecraft Plus",
        description = "Runs essence to an altar and crafts runes, with pouches (fill/empty/repair) and rune/pure/daeyalt essence. Stop conditions, target level, overlay/pause.",
        tags = {"runecraft", "runecrafting", "rc", "microbot", "plus"},
        version = AutoRunecraftPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoRunecraftPlusPlugin extends Plugin {
    public static final String version = "0.2.0";

    @Inject
    private AutoRunecraftPlusConfig config;

    @Provides
    AutoRunecraftPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoRunecraftPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoRunecraftPlusOverlay overlay;

    @Inject
    AutoRunecraftPlusScript script;

    @Override
    protected void startUp() throws AWTException {
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(overlay);
            overlay.pauseButton.hookMouseListener();
        }
        script.run(config);
    }

    @Override
    protected void shutDown() {
        Microbot.pauseAllScripts.compareAndSet(true, false);
        script.shutdown();
        if (overlay != null) {
            overlay.pauseButton.unhookMouseListener();
        }
        overlayManager.remove(overlay);
    }

    public AutoRunecraftPlusScript getScript() {
        return script;
    }
}
