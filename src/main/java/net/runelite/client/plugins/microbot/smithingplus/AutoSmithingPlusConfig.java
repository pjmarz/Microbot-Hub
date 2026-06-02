package net.runelite.client.plugins.microbot.smithingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.smithingplus.data.AnvilItem;
import net.runelite.client.plugins.microbot.smithingplus.data.AnvilLocationOption;
import net.runelite.client.plugins.microbot.smithingplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.smithingplus.data.Bars;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;

@ConfigGroup("SmithingPlus")
@ConfigInformation("<h2>Auto Smithing Plus</h2>" +
        "<h3>Version: " + AutoSmithingPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Bar:</strong> the bar tier to smith. Must already be in your bank.</p>" +
        "<p>2. <strong>Item:</strong> what to make at the anvil. Each item consumes a fixed " +
        "number of bars. Higher-bar items = higher XP/hr.</p>" +
        "<p>3. <strong>Anvil:</strong> the anvil to walk to. AUTO_NEAREST mirrors upstream. " +
        "Lumbridge Rusted Anvil is BRONZE BARS ONLY (object ID 39620).</p>" +
        "<p>4. <strong>Max players in area:</strong> hop worlds if more players than this are " +
        "within Distance to Stray. 0 = disabled.</p>" +
        "<p>5. <strong>League mode:</strong> arrow-key press resets the idle-logout timer.</p>" +
        "<p>6. <strong>Preferred bank:</strong> override nearest-by-raw-distance.</p>" +
        "<p>7. <strong>Items to bank / Items to keep:</strong> comma-separated lists; substring " +
        "match on item name. itemsToBank wins; if empty, falls back to deposit-all-except-keep.</p>" +
        "<p>8. <strong>Speed mode:</strong> disables Microbot antiban. Throwaway only.</p>")
public interface AutoSmithingPlusConfig extends Config {

    @ConfigSection(name = "General", description = "General settings", position = 0)
    String generalSection = "general";

    @ConfigSection(name = "Banking", description = "Banking settings", position = 1)
    String bankingSection = "bankingSection";

    @ConfigSection(name = "Dropping", description = "Dropping settings (parity; smithing always banks)", position = 2)
    String droppingSection = "droppingSection";

    // --- General section ---

    @ConfigItem(
            keyName = "selectedBar",
            name = "Bar",
            description = "Which bar tier to smith. Bars must be in your bank.",
            position = 0,
            section = generalSection
    )
    default Bars selectedBar() {
        return Bars.BRONZE;
    }

    @ConfigItem(
            keyName = "selectedItem",
            name = "Item",
            description = "Which item to make. Bronze claws are MEMBERS-ONLY (Cabin Fever quest); F2P picks will refuse to start.",
            position = 1,
            section = generalSection
    )
    default AnvilItem selectedItem() {
        return AnvilItem.DAGGER;
    }

    @ConfigItem(
            keyName = "progressiveSmith",
            name = "Progressive mode",
            description = "Ignore the Item pick and auto-smith the best item your Smithing level can make at the chosen bar tier (skips members items on F2P). Mirrors AutoSmeltingPlus's progressive mode.",
            position = 2,
            section = generalSection
    )
    default boolean progressiveSmith() {
        return false;
    }

    @ConfigItem(
            keyName = "anvilLocation",
            name = "Anvil",
            description = "Walk to and anchor at this anvil. AUTO_NEAREST = require start-near-anvil. Lumbridge Rusted Anvil is bronze-only.",
            position = 2,
            section = generalSection
    )
    default AnvilLocationOption anvilLocation() {
        return AnvilLocationOption.AUTO_NEAREST;
    }

    @ConfigItem(
            keyName = "distanceToStray",
            name = "Distance to stray",
            description = "How far the bot can wander from the anvil tile. Also the player-detection radius for autohop.",
            position = 3,
            section = generalSection
    )
    default int distanceToStray() {
        return 20;
    }

    /**
     * Cycle B v0.2.0 borrow from MiningPlus / WC. Anti-PK / busy-anvil safety.
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
     * Cycle B v0.2.0 borrow from MiningPlus. Defends against the game's idle-logout.
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
     * runs one final deposit pass then shuts down. 0 = disabled.
     */
    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Smithing reaches this level. Deposits inventory first. 0 = disabled.",
            position = 9,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    // v0.5.1: paused config item removed. Pause is now an overlay button toggling
    // Microbot.pauseAllScripts (shared global flag). See AutoSmithingPlusOverlay.

    // --- Banking section ---

    @ConfigItem(
            keyName = "useBank",
            name = "Use bank",
            description = "Standard cycle: bank -> withdraw bars + hammer -> walk to anvil -> smith -> deposit items.",
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
     * Cycle B v0.2.0 borrow. CSV inclusion list (substring match) replaces the v0.1.0
     * hardcoded depositAllExcept(HAMMER, barId) call. Default empty = use itemsToKeep instead.
     */
    @ConfigItem(
            keyName = "itemsToBank",
            name = "Items to bank (comma-separated)",
            description = "Items whose name contains any of these substrings get deposited. Leave empty to deposit everything except Items to keep.",
            position = 2,
            section = bankingSection
    )
    default String itemsToBank() {
        return "";
    }

    /**
     * Cycle B v0.2.0 borrow. Defaults preserve the smithing setup (hammer + selected bar) so the
     * bot never deposits the tools it needs to keep working.
     */
    @ConfigItem(
            keyName = "itemsToKeep",
            name = "Items to keep (comma-separated)",
            description = "Items to never deposit. Default keeps hammer + any bar tier you're smithing.",
            position = 3,
            section = bankingSection
    )
    default String itemsToKeep() {
        return "hammer,bar";
    }

    // --- Dropping section ---

    @ConfigItem(
            keyName = "dropOrder",
            name = "Drop order",
            description = "Reserved for parity with MiningPlus. Smithing always banks; unused at v0.2.0.",
            position = 0,
            section = droppingSection
    )
    default InteractOrder interactOrder() {
        return InteractOrder.STANDARD;
    }
}
