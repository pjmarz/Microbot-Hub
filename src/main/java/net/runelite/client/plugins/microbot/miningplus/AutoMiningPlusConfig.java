package net.runelite.client.plugins.microbot.miningplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.miningplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.MineLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.Rocks;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("MiningPlus")
@ConfigInformation("<h2>Auto Mining Plus</h2>" +
        "<h3>Version: "+ AutoMiningPlusPlugin.version + "</h3>" +
        "<h3>General</h3>" +
        "<p>1. <strong>Ore:</strong> Pick the ore you want to mine. Default is Tin.</p>" +
        "<p></p>" +
        "<p>2. <strong>Mine location:</strong> Walk to and stay at this mine before mining. AUTO BEST picks the closest reachable mine for your ore. AUTO BEST does not hold underground mines such as Mining Guild or Dwarven Mine once you bank on the surface, so pick those by name.</p>" +
        "<p></p>" +
        "<p>3. <strong>Progressive mode:</strong> Let the bot choose the best ore for your current Mining level instead of the fixed Ore choice.</p>" +
        "<p></p>" +
        "<p>4. <strong>Distance to stray:</strong> How far in tiles the bot may roam from where it started. Default is 20 tiles.</p>" +
        "<p></p>" +
        "<p>5. <strong>Max players in area:</strong> Hop worlds when more players than this are nearby. Set 0 to never hop.</p>" +
        "<p></p>" +
        "<p>6. <strong>League mode:</strong> Taps a key now and then to reset the idle timer so you are not logged out for inactivity.</p>" +
        "<p></p>" +
        "<p>7. <strong>Speed mode:</strong> Turns off cooldowns, micro breaks, and curved mouse paths for faster mining. It looks more bot like, so use it only on throwaway accounts.</p>" +
        "<p></p>" +
        "<p>8. <strong>Stop conditions:</strong> Four optional limits that shut the bot down once met: Stop after minutes, Stop after XP gained, Target level, and Stop after ores mined. Set any to 0 to disable it. When a limit is reached the bot banks the inventory first, or drops it if Use bank is off.</p>" +
        "<p></p>" +
        "<h3>Banking</h3>" +
        "<p>9. <strong>Use bank:</strong> Bank when the inventory fills, then walk back to where you started. Default is off.</p>" +
        "<p></p>" +
        "<p>10. <strong>Preferred bank:</strong> Which bank to use. AUTO NEAREST picks the closest bank by raw distance, which can favor Al Kharid over Lumbridge for the East Lumbridge mine because the toll gate is not counted.</p>" +
        "<p></p>" +
        "<p>11. <strong>Items to bank:</strong> Comma separated list of items to deposit. The default covers common mined items, and the bot also adds the current ore name at deposit time.</p>" +
        "<p></p>" +
        "<p>12. <strong>Use clay bracelet:</strong> Withdraw and wear a bracelet of clay. Start the script with one already on.</p>" +
        "<p></p>" +
        "<h3>Dropping</h3>" +
        "<p>13. <strong>Drop order:</strong> The order the bot uses when dropping items.</p>" +
        "<p></p>" +
        "<p>14. <strong>Items to keep:</strong> Comma separated list of items never to drop. Default keeps your pickaxe.</p>")

public interface AutoMiningPlusConfig extends Config {
    @ConfigSection(
            name = "General",
            description = "General",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Dropping",
            description = "Dropping settings",
            position = 1
    )
    String droppingSection = "droppingSection";

    @ConfigSection(
            name = "Banking",
            description = "Banking settings",
            position = 2
    )
    String bankingSection = "bankingSection";

    @ConfigItem(
            keyName = "Ore",
            name = "Ore",
            description = "Choose the ore",
            position = 0,
            section = generalSection
    )
    default Rocks ORE()
    {
        return Rocks.TIN;
    }

    @ConfigItem(
            keyName = "mineLocation",
            name = "Mine location",
            description = "Walk to and anchor at this mine before starting. AUTO_BEST picks the closest accessible mine for the chosen ore by raw coordinate distance. Note it does NOT hold underground mines (Mining Guild, Dwarven Mine) once you bank on the surface (the underground coordinate reads as ~6400 tiles away, so a surface mine always wins the distance check). For underground mines, select the mine explicitly instead of AUTO_BEST.",
            position = 1,
            section = generalSection
    )
    default MineLocationOption mineLocation() {
        return MineLocationOption.AUTO_BEST;
    }

    @ConfigItem(
            keyName = "progressiveMode",
            name = "Progressive mode",
            description = "Automatically select the best ore for our level",
            position = 2,
            section = generalSection
    )
    default boolean progressiveMode()
    {
        return false;
    }

    @ConfigItem(
            keyName = "DistanceToStray",
            name = "Distance to Stray",
            description = "Set how far you can travel from your initial position in tiles",
            position = 3,
            section = generalSection
    )
    default int distanceToStray()
    {
        return 20;
    }

    @ConfigItem(
            keyName = "maxPlayersInArea",
            name = "Max players in area",
            description = "If more players than this are nearby, hop worlds. 0 = disable",
            position = 4,
            section = generalSection
    )
    default int maxPlayersInArea() {
        return 0;
    }

    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses a key to reset the idle timer so you never get logged out",
            position = 5,
            section = generalSection
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables action cooldowns, micro-breaks, and Bezier-curve mouse paths. Faster mining at the cost of looking more bot-like. Recommended only for throwaway accounts.",
            position = 6,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }

    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 7,
            section = generalSection
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Mining XP. 0 = no limit.",
            position = 8,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Mining reaches this level. Banks inventory (or drops if UseBank off) first. 0 = disabled.",
            position = 9,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterOres",
            name = "Stop after (ores mined)",
            description = "Stop after mining this many ores, banking the inventory first (or dropping if UseBank is off). Counts actual ore obtained (one per XP drop), not mine attempts. 0 = disabled.",
            position = 10,
            section = generalSection
    )
    default int stopAfterOres() {
        return 0;
    }

    @ConfigItem(
            keyName = "UseBank",
            name = "UseBank",
            description = "Use bank and walk back to original location",
            position = 0,
            section = bankingSection
    )
    default boolean useBank()
    {
        return false;
    }

    @ConfigItem(
            keyName = "BankLocation",
            name = "Preferred bank",
            description = "Bank to use when inventory fills. AUTO_NEAREST = closest bank by raw distance (often picks Al Kharid over Lumbridge for the East Lumbridge mine because the toll-gate distance is not weighted).",
            position = 1,
            section = bankingSection
    )
    default BankLocationOption bankLocation() {
        return BankLocationOption.AUTO_NEAREST;
    }

    @ConfigItem(
            keyName = "ItemsToBank",
            name = "Items to bank (Comma seperated)",
            description = "Items to bank",
            position = 2,
            section = bankingSection
    )
    default String itemsToBank() {
        // Covers mineables whose item name has no "ore" suffix (coal, clay, basalt, gems, essence,
        // ash, shards, geodes, salts, and single-item rocks like granite or amethyst). The script
        // also adds the active rock's first word to this filter at deposit time, so a user override
        // still deposits correctly.
        return "ore, uncut, coal, clay, basalt, essence, ash, shard, geode, salt, limestone, granite, sandstone, amethyst, pay-dirt";
    }

    @ConfigItem(
            keyName = "clayBracelet",
            name = "Use Clay Bracelet",
            description = "Withdraw and equip bracelet of clay. Start script with bracelet on.",
            position = 3,
            section = bankingSection
    )
    default boolean clayBracelet() {
        return false;
    }

    @ConfigItem(
            keyName = "dropOrder",
            name = "Drop Order",
            description = "Order for dropping items",
            position = 0,
            section = droppingSection
    )
    default InteractOrder interactOrder() {
        return InteractOrder.STANDARD;
    }

    @ConfigItem(
            keyName = "itemsToKeep",
            name = "Items to keep (Comma seperated)",
            description = "Items to keep when dropping ore",
            position = 1,
            section = droppingSection
    )
    default String itemsToKeep() {
        return "pickaxe";
    }
}