package net.runelite.client.plugins.microbot.craftingplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("CraftingPlus")
@ConfigInformation("<h2>Auto Crafting Plus</h2>" +
        "<h3>Version: " + AutoCraftingPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Activity:</strong> Leather (needle + thread + leather, F2P from level 1), " +
        "Gem cutting (chisel + uncut gems, from 20 for sapphire), or Furnace jewellery (gold/silver " +
        "bar + mould, plus a cut gem for gem rings/necklaces/etc.).</p>" +
        "<p>2. <strong>Leather item / Gem / Jewellery:</strong> what to make for the chosen activity.</p>" +
        "<p>3. <strong>Furnace:</strong> which furnace+bank to use for jewellery (Edgeville is the F2P default).</p>" +
        "<p>4. <strong>Stop after / Target level:</strong> auto-shutdown thresholds. Target level banks first.</p>" +
        "<p>5. <strong>League mode:</strong> periodic arrow-key press to defeat the idle-logout.</p>" +
        "<p>6. <strong>Speed mode:</strong> disables Microbot antiban. Throwaway accounts only.</p>")
public interface AutoCraftingPlusConfig extends Config {

    @ConfigSection(name = "General", description = "General settings", position = 0)
    String generalSection = "general";

    @ConfigItem(
            keyName = "activity",
            name = "Activity",
            description = "Leather crafting or gem cutting.",
            position = 0,
            section = generalSection
    )
    default Activity activity() {
        return Activity.LEATHER;
    }

    @ConfigItem(
            keyName = "leatherProduct",
            name = "Leather item",
            description = "Which leather item to make (Leather activity).",
            position = 1,
            section = generalSection
    )
    default Leather leatherProduct() {
        return Leather.LEATHER_GLOVES;
    }

    @ConfigItem(
            keyName = "gemType",
            name = "Gem",
            description = "Which gem to cut (Gem cutting activity).",
            position = 2,
            section = generalSection
    )
    default Gems gemType() {
        return Gems.SAPPHIRE;
    }

    @ConfigItem(
            keyName = "jewellery",
            name = "Jewellery",
            description = "Which piece to cast (Furnace jewellery activity). Gold/silver bar + mould; gem pieces also need the cut gem banked.",
            position = 3,
            section = generalSection
    )
    default Jewelry jewellery() {
        return Jewelry.GOLD_RING;
    }

    @ConfigItem(
            keyName = "furnaceLocation",
            name = "Furnace",
            description = "Which furnace + bank to use for jewellery. Edgeville is the closest F2P furnace-to-bank.",
            position = 4,
            section = generalSection
    )
    default CraftingLocation furnaceLocation() {
        return CraftingLocation.EDGEVILLE;
    }

    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 5,
            section = generalSection
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Crafting XP. 0 = no limit.",
            position = 6,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Crafting reaches this level. Banks the inventory first. 0 = disabled.",
            position = 7,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses an arrow key to reset the idle-logout timer.",
            position = 8,
            section = generalSection
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban. Faster, more pattern-detectable. Throwaway only.",
            position = 9,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }
}
