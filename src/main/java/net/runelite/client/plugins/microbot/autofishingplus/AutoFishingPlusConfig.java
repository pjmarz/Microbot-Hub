package net.runelite.client.plugins.microbot.autofishingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.autofishingplus.enums.Fish;
import net.runelite.client.plugins.microbot.autofishingplus.enums.FishingPlusLocation;
import net.runelite.client.plugins.microbot.autofishingplus.enums.HarpoonType;

@ConfigGroup("AutoFishingPlus")
@ConfigInformation("<h2>Auto Fishing Plus</h2>" +
        "<h3>Version: " + AutoFishingPlusPlugin.version + "</h3>" +
        "<h3>General</h3>" +
        "<p>1. <strong>Fish to catch:</strong> pick the fish type to fish. Make sure your chosen Location actually offers it.</p>" +
        "<p></p>" +
        "<p>2. <strong>Location:</strong> walk to and fish a named spot. AUTO fishes the nearest spot of your chosen fish to where you stand. One of the named spots is Corsair Cove, a free lobster spot.</p>" +
        "<p></p>" +
        "<p>3. <strong>Use bank (AUTO only):</strong> on the AUTO location, turn this on to bank the catch instead of dropping it. Named spots ignore it and use their own deposit box, bank, or drop strategy.</p>" +
        "<p></p>" +
        "<p>4. <strong>Cook fish:</strong> after fishing, cook the raw fish on a nearby fire or range. Leave it off to keep raw fish. Only useful where a fire or range is in reach.</p>" +
        "<p></p>" +
        "<p>5. <strong>Harpoon spec:</strong> pick the harpoon whose special attack you want fired while fishing, for faster catches. Choose Crystal, Dragon, or Infernal harpoon, or None to never spec.</p>" +
        "<p></p>" +
        "<h3>Stop conditions</h3>" +
        "<p>6. <strong>Stop after:</strong> auto shutdown by minutes of runtime, Fishing XP gained, target Fishing level, or fish caught. Set any to 0 to disable that limit. The bot deposits or drops its catch once before stopping.</p>")
public interface AutoFishingPlusConfig extends Config {

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
            keyName = "fishToCatch",
            name = "Fish to catch",
            description = "Choose the fish type to catch",
            position = 0,
            section = GENERAL_SECTION
    )
    default Fish fishToCatch() {
        return Fish.SHRIMP_AND_ANCHOVIES;
    }

    @ConfigItem(
            keyName = "fishingLocation",
            name = "Location",
            description = "Walk to and fish this named spot before starting. AUTO fishes the nearest spot of your chosen fish to where you stand. Named locations carry their own banking strategy (deposit box / bank / drop).",
            position = 1,
            section = GENERAL_SECTION
    )
    default FishingPlusLocation fishingLocation() {
        return FishingPlusLocation.AUTO;
    }

    @ConfigItem(
            keyName = "useBank",
            name = "Use bank (AUTO only)",
            description = "For the AUTO location: bank the catch (walk to nearest bank and back) instead of dropping it. Ignored for named locations, which use their own strategy.",
            position = 2,
            section = GENERAL_SECTION
    )
    default boolean useBank() {
        return false;
    }

    @ConfigItem(
            keyName = "cookFish",
            name = "Cook fish",
            description = "Cook fish after fishing if a fire/range is nearby",
            position = 3,
            section = GENERAL_SECTION
    )
    default boolean cookFish() {
        return false;
    }

    @ConfigItem(
            keyName = "harpoonSpec",
            name = "Harpoon spec",
            description = "Choose the harpoon type for special attacks",
            position = 4,
            section = GENERAL_SECTION
    )
    default HarpoonType harpoonSpec() {
        return HarpoonType.NONE;
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
            description = "Auto-shutdown after gaining this much Fishing XP. 0 = no limit.",
            position = 1,
            section = STOP_SECTION
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Fishing reaches this level. Deposits/drops the catch first. 0 = disabled.",
            position = 2,
            section = STOP_SECTION
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterFish",
            name = "Stop after (fish caught)",
            description = "Stop after catching this many fish (counts one per Fishing XP drop), depositing/dropping the catch first. 0 = disabled.",
            position = 3,
            section = STOP_SECTION
    )
    default int stopAfterFish() {
        return 0;
    }
}
