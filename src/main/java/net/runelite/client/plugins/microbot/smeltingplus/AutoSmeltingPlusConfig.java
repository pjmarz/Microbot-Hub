package net.runelite.client.plugins.microbot.smeltingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.smeltingplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.smeltingplus.data.Bars;
import net.runelite.client.plugins.microbot.smeltingplus.data.FurnaceLocationOption;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("SmeltingPlus")
@ConfigInformation("<h2>Auto Smelting Plus</h2>" +
        "<h3>Version: " + AutoSmeltingPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Bar:</strong> the bar to smelt. Sets the required ore mix automatically. Lower bars take 1 ore; higher bars take ore plus coal (e.g. steel is 1 iron and 2 coal).</p>" +
        "<p></p>" +
        "<p>2. <strong>Progressive smelt:</strong> ignores the Bar pick and auto selects the highest bar your Smithing level and bank stock can support, re-checked each banking trip.</p>" +
        "<p></p>" +
        "<p>3. <strong>Furnace:</strong> the furnace to walk to. AUTO_NEAREST uses the closest (stand near a furnace at start, restart to switch).</p>" +
        "<p></p>" +
        "<p>4. <strong>Max players in area:</strong> hop worlds if more than this many other players are within Distance to Stray. 0 disables hopping.</p>" +
        "<p></p>" +
        "<p>5. <strong>League mode:</strong> presses an arrow key to reset the idle logout timer.</p>" +
        "<p></p>" +
        "<p>6. <strong>Preferred bank:</strong> override the nearest by distance choice.</p>" +
        "<p></p>" +
        "<p>7. <strong>Items to bank / keep:</strong> comma separated lists matched on item name. Items to bank wins; if empty it deposits all except your keep list.</p>" +
        "<p></p>" +
        "<p>8. <strong>Speed mode:</strong> disables Microbot antiban for a faster pace. Throwaway accounts only.</p>")
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
     * Cycle A v0.2.0 borrow from WC's progressiveMode. Auto-picks the highest-tier bar where
     * Smithing level >= bar.requiredSmithingLevel AND the bank has the required ores.
     * Re-evaluated each bank trip.
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
            description = "Walk to and anchor at this furnace. AUTO_NEAREST = require start-near-furnace (upstream behavior).",
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
     * Cycle A v0.2.0 borrow from MiningPlus / WC. Anti-PK / busy-furnace safety.
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
     * Cycle A v0.2.0 borrow from MiningPlus. Defends against the game's 5-min idle-logout.
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
     * Polish-Cycle 2 v0.3.0: runtime/XP threshold for auto-shutdown.
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
     * v0.5.0: target-level threshold. When Smithing level reaches this value, the script
     * flips to RESETTING for a deposit pass, then shuts down. 0 = disabled.
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

    // v0.5.1: paused config item removed. Pause is now an overlay button toggling
    // Microbot.pauseAllScripts (shared global flag). See AutoSmeltingPlusOverlay.

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
     * Cycle A v0.2.0 borrow. CSV inclusion list (substring match on item name) replaces the
     * v0.1.1 hardcoded depositAllExcept(COAL_BAG_ID) call.
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
     * Cycle A v0.2.0 borrow. Exclusion list when itemsToBank is empty; also a safety net so
     * critical items aren't deposited even if the user widens the bank filter.
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
     * Cycle A v0.2.0 borrow. Unused at v0.2.0 (smelting always banks); reserved for a future
     * "drop bars" mode and for cross-plugin config parity.
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
