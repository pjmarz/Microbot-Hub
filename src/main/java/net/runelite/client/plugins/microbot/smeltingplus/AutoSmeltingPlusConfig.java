package net.runelite.client.plugins.microbot.smeltingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.smeltingplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.smeltingplus.data.Bars;
import net.runelite.client.plugins.microbot.smeltingplus.data.FurnaceLocationOption;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("SmeltingPlus")
@ConfigInformation("<h2>Auto Smelting Plus</h2>" +
        "<h3>Version: " + AutoSmeltingPlusPlugin.version + "</h3>" +
        "<h3>General</h3>" +
        "<p>1. <strong>Bar:</strong> the bar to smelt. This sets the required ore mix for you. Lower bars take 1 ore. Higher bars take ore plus coal, for example steel is 1 iron and 2 coal.</p>" +
        "<p></p>" +
        "<p>2. <strong>Progressive smelt:</strong> ignores the Bar pick and auto selects the highest bar your Smithing level and bank stock can support. It is re checked every bank trip.</p>" +
        "<p></p>" +
        "<p>3. <strong>Furnace:</strong> the furnace to walk to and stay near. AUTO NEAREST uses the closest one. Stand near a furnace at start and restart to switch.</p>" +
        "<p></p>" +
        "<p>4. <strong>Distance to stray:</strong> how far the bot may wander from the furnace tile. This is also the radius used to count nearby players for world hopping.</p>" +
        "<p></p>" +
        "<p>5. <strong>Max players in area:</strong> hop worlds if more than this many other players are within Distance to stray. Set 0 to never hop.</p>" +
        "<p></p>" +
        "<p>6. <strong>League mode:</strong> presses an arrow key now and then to reset the idle logout timer.</p>" +
        "<p></p>" +
        "<p>7. <strong>Speed mode:</strong> turns off Microbot antiban for a faster pace. This is more detectable, so use throwaway accounts only.</p>" +
        "<p></p>" +
        "<p>8. <strong>Stop conditions:</strong> the bot shuts down when any limit you set is hit. Stop after minutes ends after that much runtime. Stop after XP ends after that much Smithing XP. Target level ends when Smithing reaches that level and deposits first. Set any to 0 to ignore it.</p>" +
        "<p></p>" +
        "<h3>Banking</h3>" +
        "<p>9. <strong>Use bank:</strong> runs the normal cycle of bank, withdraw ores, walk to the furnace, smelt, then walk back.</p>" +
        "<p></p>" +
        "<p>10. <strong>Preferred bank:</strong> overrides the nearest by distance pick. AUTO NEAREST uses the closest one.</p>" +
        "<p></p>" +
        "<p>11. <strong>Items to bank and Items to keep:</strong> comma separated name matches. Items to bank deposits anything whose name contains a listed word. Items to keep is never deposited. If Items to bank is empty the bot deposits everything except your keep list.</p>" +
        "<p></p>" +
        "<h3>Dropping</h3>" +
        "<p>12. <strong>Drop order:</strong> the order to drop items when not banking. Smelting always banks, so this is unused for now and kept for parity.</p>")
public interface AutoSmeltingPlusConfig extends Config {

    @ConfigSection(name = "General", description = "General settings", position = 0)
    String generalSection = "general";

    @ConfigSection(name = "Banking", description = "Banking settings", position = 1)
    String bankingSection = "bankingSection";

    @ConfigSection(name = "Dropping", description = "Dropping settings (mostly for parity; smelting deposits, doesn't drop)", position = 2)
    String droppingSection = "droppingSection";

    // --- General section ---

    @ConfigItem(
            keyName = "selectedBar",
            name = "Bar",
            description = "Which bar to smelt. Ignored if Progressive smelt is on.",
            position = 0,
            section = generalSection
    )
    default Bars selectedBar() {
        return Bars.BRONZE;
    }

    /**
     * Auto-picks the highest-tier bar where Smithing level >= bar.requiredSmithingLevel
     * AND the bank has the required ores. Re-evaluated each bank trip.
     */
    @ConfigItem(
            keyName = "progressiveSmelt",
            name = "Progressive smelt",
            description = "Auto-pick highest-tier bar you can smelt (level + bank stock). Overrides the Bar dropdown.",
            position = 1,
            section = generalSection
    )
    default boolean progressiveSmelt() {
        return false;
    }

    @ConfigItem(
            keyName = "furnaceLocation",
            name = "Furnace",
            description = "Walk to and anchor at this furnace. AUTO NEAREST means you must start near a furnace.",
            position = 2,
            section = generalSection
    )
    default FurnaceLocationOption furnaceLocation() {
        return FurnaceLocationOption.AUTO_NEAREST;
    }

    @ConfigItem(
            keyName = "distanceToStray",
            name = "Distance to stray",
            description = "How far the bot can wander from the furnace tile. Also the player-detection radius for autohop.",
            position = 3,
            section = generalSection
    )
    default int distanceToStray() {
        return 20;
    }

    /**
     * Anti-PK and busy-furnace safety: hop worlds when the area gets crowded.
     */
    @ConfigItem(
            keyName = "maxPlayersInArea",
            name = "Max players in area",
            description = "Hop worlds if more players than this are within Distance to Stray. 0 = disable.",
            position = 4,
            section = generalSection
    )
    default int maxPlayersInArea() {
        return 0;
    }

    /**
     * Defends against the game's 5-minute idle-logout.
     */
    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses an arrow key to reset the idle timer.",
            position = 5,
            section = generalSection
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban. Faster bot, more pattern-detectable. Throwaway only.",
            position = 6,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }

    /**
     * Runtime threshold for auto-shutdown.
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
            description = "Auto-shutdown after gaining this much Smithing XP. 0 = no limit.",
            position = 8,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    /**
     * Target-level threshold. When Smithing level reaches this value, the script
     * does a final deposit pass, then shuts down. 0 = disabled.
     */
    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Smithing reaches this level. Deposits the inventory first. 0 = disabled.",
            position = 9,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    // Pause is an overlay button toggling Microbot.pauseAllScripts (shared global flag).
    // See AutoSmeltingPlusOverlay.

    // --- Banking section ---

    @ConfigItem(
            keyName = "useBank",
            name = "Use bank",
            description = "Standard smelting cycle: bank -> withdraw ores -> walk to furnace -> smelt -> walk back.",
            position = 0,
            section = bankingSection
    )
    default boolean useBank() {
        return true;
    }

    @ConfigItem(
            keyName = "bankLocation",
            name = "Preferred bank",
            description = "AUTO_NEAREST = closest by raw distance.",
            position = 1,
            section = bankingSection
    )
    default BankLocationOption bankLocation() {
        return BankLocationOption.AUTO_NEAREST;
    }

    /**
     * CSV inclusion list, substring match on item name.
     */
    @ConfigItem(
            keyName = "itemsToBank",
            name = "Items to bank (comma-separated)",
            description = "Items whose name contains any of these substrings get deposited. Default 'bar' catches Bronze/Iron/etc. bar. Leave empty to deposit everything except Items to keep.",
            position = 2,
            section = bankingSection
    )
    default String itemsToBank() {
        return "bar";
    }

    /**
     * Exclusion list when itemsToBank is empty; also a safety net so critical items
     * aren't deposited even if the user widens the bank filter.
     */
    @ConfigItem(
            keyName = "itemsToKeep",
            name = "Items to keep (comma-separated)",
            description = "Items to never deposit. Defaults preserve coal bag and smelting gear.",
            position = 3,
            section = bankingSection
    )
    default String itemsToKeep() {
        return "coal bag,ring of forging,gauntlets of goldsmithing";
    }

    // --- Dropping section ---

    /**
     * Unused for now (smelting always banks); reserved for a future "drop bars" mode
     * and for cross-plugin config parity.
     */
    @ConfigItem(
            keyName = "dropOrder",
            name = "Drop order",
            description = "Order to drop items when not banking. Currently unused; reserved for parity.",
            position = 0,
            section = droppingSection
    )
    default InteractOrder interactOrder() {
        return InteractOrder.STANDARD;
    }
}
