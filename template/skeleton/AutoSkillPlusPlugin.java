// TEMPLATE FILE — copy this to src/main/java/net/runelite/client/plugins/microbot/<skill>plus/
// and replace every occurrence of:
//   - "Skill"  (CamelCase: rename to your skill, e.g. Smithing, Cooking, Fishing)
//   - "skillplus"  (lowercase package: e.g. smithingplus, cookingplus)
//   - "<Skill>Plus" in @PluginDescriptor name string (e.g. "Smithing Plus")
//
// Search-and-replace is safe — no token collisions inside this file.

package net.runelite.client.plugins.microbot.skillplus;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

// TODO(plus): set name, version, minClientVersion. Pin minClientVersion to the lowest microbot
// version that still exposes the APIs you use; Adoptium/Microbot-Hub launchers use it to skip
// plugins that don't match. iconUrl/cardUrl point at chsami.github.io/Microbot-Hub/<PluginName>/assets/.
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Auto Skill Plus",
        description = "TODO(plus): one-line description shown in the plugin panel.",
        tags = {"skill", "microbot", "plus"},
        version = AutoSkillPlusPlugin.version,
        minClientVersion = "2.0.13",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoSkillPlusPlugin extends Plugin {
    public static final String version = "0.1.0";

    @Inject
    private AutoSkillPlusConfig config;

    @Provides
    AutoSkillPlusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoSkillPlusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoSkillPlusOverlay overlay;

    @Inject
    AutoSkillPlusScript script;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
}
