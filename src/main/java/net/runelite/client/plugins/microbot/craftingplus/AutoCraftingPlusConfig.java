package net.runelite.client.plugins.microbot.craftingplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("CraftingPlus")
@ConfigInformation("<h2>Auto Crafting Plus</h2>" +
        "<h3>Version: " + AutoCraftingPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Activity:</strong> Leather (needle + thread + leather, free to play from " +
        "level 1), Gem cutting (chisel + uncut gems, from 20 for sapphire), Furnace jewellery " +
        "(gold or silver bar + mould, plus a cut gem for gem rings and necklaces), Amethyst cutting " +
        "(chisel + amethyst into bolt tips, arrowtips, javelin heads, or dart tips, from 83), or " +
        "Amulet stringing (ball of wool on an unstrung amulet, from level 1).</p>" +
        "<p>2. <strong>Leather item / Gem / Jewellery / Amethyst product / Amulet:</strong> what to " +
        "make for the chosen activity.</p>" +
        "<p>3. <strong>Dragonhide:</strong> in the Leather activity, set this to a d'hide piece " +
        "(green from 57, up to black) to sew dragonhide armour instead of soft leather. Needs the " +
        "matching dragon leather in the bank. Leave it on None to make soft leather.</p>" +
        "<p>4. <strong>Progressive:</strong> when on, picks the highest item your Crafting level " +
        "allows that also has materials in the bank, and re-checks at every bank trip. Covers " +
        "Leather (soft and dragonhide together) and Gem cutting. It does not change Jewellery, so " +
        "pick the jewellery piece yourself.</p>" +
        "<p>5. <strong>Furnace:</strong> which furnace and bank to use for jewellery (Edgeville is " +
        "the free to play default).</p>" +
        "<p>6. <strong>Stop after / Target level:</strong> auto-shutdown thresholds. Target level " +
        "banks first.</p>" +
        "<p>7. <strong>League mode:</strong> periodic arrow-key press to beat the idle-logout.</p>" +
        "<p>8. <strong>Speed mode:</strong> turns off Microbot antiban. Throwaway accounts only.</p>")
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
            description = "Which soft leather item to make (Leather activity). Ignored if Dragonhide is set or Progressive is on.",
            position = 1,
            section = generalSection
    )
    default Leather leatherProduct() {
        return Leather.LEATHER_GLOVES;
    }

    @ConfigItem(
            keyName = "dragonLeather",
            name = "Dragonhide",
            description = "Sew dragonhide armour instead of soft leather. Needs the matching dragon leather banked. None = make soft leather. Ignored if Progressive is on.",
            position = 2,
            section = generalSection
    )
    default DragonLeather dragonLeather() {
        return DragonLeather.NONE;
    }

    @ConfigItem(
            keyName = "gemType",
            name = "Gem",
            description = "Which gem to cut (Gem cutting activity). Ignored if Progressive is on.",
            position = 3,
            section = generalSection
    )
    default Gems gemType() {
        return Gems.SAPPHIRE;
    }

    @ConfigItem(
            keyName = "jewellery",
            name = "Jewellery",
            description = "Which piece to cast (Furnace jewellery activity). Gold/silver bar + mould; gem pieces also need the cut gem banked. Progressive does not change this.",
            position = 4,
            section = generalSection
    )
    default Jewelry jewellery() {
        return Jewelry.GOLD_RING;
    }

    @ConfigItem(
            keyName = "amethystProduct",
            name = "Amethyst product",
            description = "Which product to cut amethyst into (Amethyst cutting activity). Bolt tips need 83, arrowtips 85, javelin heads 87, dart tips 89.",
            position = 5,
            section = generalSection
    )
    default AmethystProduct amethystProduct() {
        return AmethystProduct.BOLT_TIPS;
    }

    @ConfigItem(
            keyName = "stringAmulet",
            name = "Amulet",
            description = "Which unstrung amulet to string with wool (Amulet stringing activity). Gold is free to play; the gem amulets are members only. No level requirement.",
            position = 6,
            section = generalSection
    )
    default StringAmulet stringAmulet() {
        return StringAmulet.GOLD;
    }

    @ConfigItem(
            keyName = "progressiveCraft",
            name = "Progressive",
            description = "Auto-pick the highest item your Crafting level allows with materials in the bank. Re-checks each bank trip. Covers Leather (soft + dragonhide) and Gem cutting. No effect on Jewellery, Amethyst, or Stringing.",
            position = 7,
            section = generalSection
    )
    default boolean progressiveCraft() {
        return false;
    }

    @ConfigItem(
            keyName = "furnaceLocation",
            name = "Furnace",
            description = "Which furnace + bank to use for jewellery. Edgeville is the closest F2P furnace-to-bank.",
            position = 8,
            section = generalSection
    )
    default CraftingLocation furnaceLocation() {
        return CraftingLocation.EDGEVILLE;
    }

    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 9,
            section = generalSection
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Crafting XP. 0 = no limit.",
            position = 10,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Crafting reaches this level. Banks the inventory first. 0 = disabled.",
            position = 11,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses an arrow key to reset the idle-logout timer.",
            position = 12,
            section = generalSection
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban. Faster, more pattern-detectable. Throwaway only.",
            position = 13,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }
}
