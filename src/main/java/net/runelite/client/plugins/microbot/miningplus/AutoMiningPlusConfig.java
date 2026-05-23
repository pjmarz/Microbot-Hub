package net.runelite.client.plugins.microbot.miningplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.miningplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.MineLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.Rocks;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("MiningPlus")
@ConfigInformation("<h2>Auto Mining Plus</h2>" +
        "<h3>Version: "+ AutoMiningPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Ore Selection:</strong> Choose the type of ore you wish to mine. The default ore is <em>TIN</em>.</p>" +
        "<p></p>"+
        "<p>2. <strong>Mine Location:</strong> Pick a named mine and the bot walks there before mining. <em>AUTO_BEST</em> picks the closest accessible mine for the chosen ore (matches upstream AutoMining behavior).</p>" +
        "<p></p>"+
        "<p>3. <strong>Distance to Stray:</strong> Set the maximum distance in tiles that the bot can travel from its initial position. The default distance is <em>20 tiles</em>.</p>" +
        "<p></p>"+
        "<p>4. <strong>Banking Option:</strong> Enable or disable the use of a bank. If enabled, the bot will walk back to the original location after banking. The default setting is <em>disabled</em>.</p>" +
        "<p></p>"+
        "<p>5. <strong>Items to Bank:</strong> Specify the items to be banked, separated by commas. The default value is <em>'ore'</em>.</p>"+
        "<p></p>"+
        "<p>6. <strong>Basalt:</strong> If mining basalt, ensure UseBank is checked and it will automatically note at Snowflake</em>.</p>")

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
            description = "Walk to and anchor at this mine before starting. AUTO_BEST picks the closest accessible mine for the chosen ore.",
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

    /**
     * Polish-Cycle 2 (v0.3.0): runtime/XP threshold for auto-shutdown. Disrupts the uniform-
     * session-length signal that Jagex's bot detection model loves.
     */
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

    /**
     * v0.5.0: target-level threshold. When Mining level reaches this value, the script flips
     * to RESETTING for one cleanup cycle (bank or drop, per useBank), then shuts down.
     * 0 = disabled. AIO Fighter-style.
     */
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

    // v0.5.1: paused config item removed. Pause is now an overlay button (see
    // AutoMiningPlusOverlay) that toggles Microbot.pauseAllScripts (global AtomicBoolean).
    // Click pause on any Plus plugin's overlay and all scripts pause together.
    // Mirrors AIO Fighter's UX.

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
        // v0.4.1: expanded from "ore" alone to cover the OSRS-mineables that DON'T have an
        // "ore" suffix in their item name -- Coal, Clay, Basalt, and gem variants. Without
        // this, mining coal/clay/basalt with default settings caused a bank<->mine oscillation
        // because the deposit predicate matched zero items.
        // v0.4.2: wiki-driven sweep of the full Mining/Mineable_items table -- list now also
        // catches:
        //   essence    -> Rune essence, Pure essence, Dense essence block, Ancient essence
        //   ash        -> Volcanic ash
        //   shard      -> Barronite shards, Daeyalt shard
        //   geode      -> Rubium geode
        //   salt       -> Urt/Efh/Te salts
        //   limestone, granite, sandstone, amethyst, pay-dirt -> single-item rocks
        // Script also auto-augments this filter with the active rock's first word at deposit
        // time, so once a Rock is in the enum the bot deposits correctly even if the user
        // overrode this default. This list is belt-and-suspenders for users on custom filters
        // AND a forward-compat safety net for rock types not yet wired into the Rocks enum.
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