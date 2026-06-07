package net.runelite.client.plugins.microbot.cookingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingItem;
import net.runelite.client.plugins.microbot.cookingplus.enums.CookingLocation;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("AutoCookingPlus")
@ConfigInformation("<h2>Auto Cooking Plus</h2>" +
        "<h3>Version: " + AutoCookingPlusPlugin.version + "</h3>" +
        "<h3>General</h3>" +
        "<p>1. <strong>Item to cook:</strong> the raw food to cook. Keep a stack of it in your bank before you start.</p>" +
        "<p></p>" +
        "<p>2. <strong>Progressive cook:</strong> ignores the Item to cook pick and uses the best food your Cooking level allows. It is re checked each bank trip.</p>" +
        "<p></p>" +
        "<p>3. <strong>Location:</strong> the range or fire to walk to and cook at. Stand near a bank with a range close by for the smoothest loop.</p>" +
        "<p></p>" +
        "<p>4. <strong>Use nearest location:</strong> ignores the Location pick and uses the closest valid spot for your food.</p>" +
        "<p></p>" +
        "<p>5. <strong>Drop burnt items:</strong> drops any burnt food before banking so it does not pile up.</p>" +
        "<p></p>" +
        "<p>6. <strong>Drop order:</strong> the order to drop burnt food in.</p>" +
        "<p></p>" +
        "<p>7. <strong>League mode:</strong> presses an arrow key now and then to reset the idle logout timer.</p>" +
        "<p></p>" +
        "<p>8. <strong>Speed mode:</strong> turns off Microbot antiban for a faster pace. This is more detectable, so use throwaway accounts only.</p>" +
        "<p></p>" +
        "<h3>Stop conditions</h3>" +
        "<p>9. <strong>Stop after minutes:</strong> ends the session after that much runtime.</p>" +
        "<p></p>" +
        "<p>10. <strong>Stop after XP:</strong> ends after gaining that much Cooking XP.</p>" +
        "<p></p>" +
        "<p>11. <strong>Target level:</strong> ends when Cooking reaches that level.</p>" +
        "<p></p>" +
        "<p>12. <strong>Stop after cooked:</strong> ends after cooking that many items.</p>" +
        "<p></p>" +
        "<p>Set any stop value to 0 to ignore it. The bot banks or drops its load once before it stops.</p>")
public interface AutoCookingPlusConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String GENERAL_SECTION = "general";

    @ConfigSection(
            name = "Stop conditions",
            description = "Auto-shutdown thresholds (0 = disabled)",
            position = 1
    )
    String STOP_SECTION = "stop";

    // ---- GENERAL ----
    @ConfigItem(
            keyName = "cookingItem",
            name = "Item to cook",
            description = "Raw food to cook. Ignored if Progressive cook is on.",
            position = 0,
            section = GENERAL_SECTION
    )
    default CookingItem cookingItem() {
        return CookingItem.RAW_SHRIMP;
    }

    @ConfigItem(
            keyName = "progressiveCook",
            name = "Progressive cook",
            description = "Auto-pick the best food your Cooking level allows. Overrides the Item to cook dropdown.",
            position = 1,
            section = GENERAL_SECTION
    )
    default boolean progressiveCook() {
        return false;
    }

    @ConfigItem(
            keyName = "cookingLocation",
            name = "Location",
            description = "Walk to and cook at this range or fire. Ignored if Use nearest location is on.",
            position = 2,
            section = GENERAL_SECTION
    )
    default CookingLocation cookingLocation() {
        return CookingLocation.COOKS_KITCHEN;
    }

    @ConfigItem(
            keyName = "useNearestLocation",
            name = "Use nearest location",
            description = "Use the closest valid cooking spot for your food. Overrides the Location dropdown.",
            position = 3,
            section = GENERAL_SECTION
    )
    default boolean useNearestLocation() {
        return false;
    }

    @ConfigItem(
            keyName = "shouldDropBurntItems",
            name = "Drop burnt items",
            description = "Drop burnt food before banking instead of depositing it.",
            position = 4,
            section = GENERAL_SECTION
    )
    default boolean shouldDropBurntItems() {
        return true;
    }

    @ConfigItem(
            keyName = "dropOrder",
            name = "Drop order",
            description = "Order to drop burnt food in.",
            position = 5,
            section = GENERAL_SECTION
    )
    default InteractOrder getDropOrder() {
        return InteractOrder.STANDARD;
    }

    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses an arrow key to reset the idle timer.",
            position = 6,
            section = GENERAL_SECTION
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban. Faster bot, more pattern-detectable. Throwaway only.",
            position = 7,
            section = GENERAL_SECTION
    )
    default boolean speedMode() {
        return false;
    }

    // ---- STOP CONDITIONS ----
    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 0,
            section = STOP_SECTION
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Cooking XP. 0 = no limit.",
            position = 1,
            section = STOP_SECTION
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Cooking reaches this level. Banks or drops the load first. 0 = disabled.",
            position = 2,
            section = STOP_SECTION
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterCooked",
            name = "Stop after (cooked)",
            description = "Stop after cooking this many items (counts one per Cooking XP drop). 0 = disabled.",
            position = 3,
            section = STOP_SECTION
    )
    default int stopAfterCooked() {
        return 0;
    }
}
