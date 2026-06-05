package net.runelite.client.plugins.microbot.craftingplus;

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
        name = PluginDescriptor.Mocrosoft + "Auto Crafting Plus",
        description = "Leather, gem cutting, furnace jewellery, amethyst cutting, and amulet stringing on a bank-and-do loop, with stop conditions, target level, and overlay/pause.",
        tags = {"crafting", "leather", "gems", "jewellery", "amethyst", "stringing", "microbot", "plus"},
        authors = {"Mocrosoft", "pjmarz"},
        version = AutoCraftingPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AutoCraftingPlusPlugin/assets/card.png",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AutoCraftingPlusPlugin/assets/icon.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoCraftingPlusPlugin extends Plugin {
    public static final String version = "0.4.0";

    @Inject
    private AutoCraftingPlusConfig config;

    @Provides
    AutoCraftingPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoCraftingPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoCraftingPlusOverlay overlay;

    @Inject
    AutoCraftingPlusScript script;

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

    public AutoCraftingPlusScript getScript() {
        return script;
    }
}
