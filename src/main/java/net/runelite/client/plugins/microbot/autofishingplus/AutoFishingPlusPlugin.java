package net.runelite.client.plugins.microbot.autofishingplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Auto Fishing Plus",
        description = "Fishes, banks/deposit-boxes/drops, with a location picker, stop conditions and stats. Part of the Plus suite.",
        tags = {"fishing", "microbot", "skilling", "plus"},
        authors = {"AI Agent", "pjmarz"},
        version = AutoFishingPlusPlugin.version,
        minClientVersion = "2.0.13",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AutoFishingPlusPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AutoFishingPlusPlugin/assets/card.png",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoFishingPlusPlugin extends Plugin {
    public static final String version = "0.2.3";

    @Inject
    private AutoFishingPlusConfig config;

    @Provides
    AutoFishingPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoFishingPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoFishingPlusOverlay fishingOverlay;

    @Inject
    AutoFishingPlusScript fishingScript;

    @Override
    protected void startUp() throws AWTException {
        // Clear any stale pause flag from a previous session (matches AIO Fighter / mining-plus).
        Microbot.pauseAllScripts.compareAndSet(true, false);
        if (overlayManager != null) {
            overlayManager.add(fishingOverlay);
            // Critical: hookMouseListener() wires the Pause button's click into RuneLite's mouse
            // event system. Without it the button renders but clicks pass through to the game
            // (the v0.5.6 mining lesson).
            fishingOverlay.pauseButton.hookMouseListener();
        }
        fishingScript.run(config);
    }

    @Override
    protected void shutDown() {
        // Clear the flag so plugins started after us don't inherit our paused state.
        Microbot.pauseAllScripts.compareAndSet(true, false);
        fishingScript.shutdown();
        if (fishingOverlay != null) {
            fishingOverlay.pauseButton.unhookMouseListener();
        }
        overlayManager.remove(fishingOverlay);
    }

    /** Overlay + dashboard read script stats via this getter. */
    public AutoFishingPlusScript getScript() {
        return fishingScript;
    }

    public int getXpGained() {
        if (fishingScript == null) return 0;
        return Microbot.getClient().getSkillExperience(Skill.FISHING) - fishingScript.getStartSkillXp();
    }

    public long getRuntimeMillis() {
        if (fishingScript == null || fishingScript.getStartTimeMillis() == 0) return 0;
        return System.currentTimeMillis() - fishingScript.getStartTimeMillis();
    }

    public String getFormattedRuntime() {
        long millis = getRuntimeMillis();
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public int getXpPerHour() {
        long runtime = getRuntimeMillis();
        if (runtime == 0) return 0;
        return (int) (getXpGained() * 3600000.0 / runtime);
    }

    public int getFishCaught() {
        return fishingScript == null ? 0 : fishingScript.getFishCaught();
    }
}
