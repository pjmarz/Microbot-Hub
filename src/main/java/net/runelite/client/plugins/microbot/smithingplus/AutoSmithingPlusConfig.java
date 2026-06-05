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
        "<h3>General</h3>" +
        "<p>1. <strong>Bar:</strong> the bar tier to smith. The bars must already be in your bank.</p>" +
        "<p></p>" +
        "<p>2. <strong>Item:</strong> what to make at the anvil. Each item uses a fixed number of bars, and bigger items give more XP per hour. Some picks are members only and will refuse to start on free worlds.</p>" +
        "<p></p>" +
        "<p>3. <strong>Progressive mode:</strong> ignore the Item pick and smith the best item your Smithing level can make at the chosen bar tier. It skips members items on free worlds.</p>" +
        "<p></p>" +
        "<p>4. <strong>Anvil:</strong> the anvil to walk to and anchor at. Auto nearest picks the closest. The Lumbridge rusted anvil takes bronze bars only.</p>" +
        "<p></p>" +
        "<p>5. <strong>Distance to stray:</strong> how far the bot may wander from the anvil tile. This is also the radius used to count nearby players for world hopping.</p>" +
        "<p></p>" +
        "<p>6. <strong>Max players in area:</strong> hop worlds when more players than this are within Distance to stray. Set 0 to never hop.</p>" +
        "<p></p>" +
        "<p>7. <strong>League mode:</strong> presses an arrow key now and then to reset the idle logout timer.</p>" +
        "<p></p>" +
        "<p>8. <strong>Speed mode:</strong> disables Microbot antiban for a faster but more detectable bot. Use throwaway accounts only.</p>" +
        "<p></p>" +
        "<p>9. <strong>Stop conditions:</strong> the bot shuts down when any limit you set is reached. Stop after minutes caps runtime, Stop after XP caps Smithing XP gained, and Target level stops once your Smithing level reaches it. Each value of 0 means no limit. The bot deposits its inventory before stopping.</p>" +
        "<p></p>" +
        "<h3>Banking</h3>" +
        "<p>10. <strong>Use bank:</strong> run the full loop of bank, withdraw bars and hammer, walk to the anvil, smith, then deposit the finished items.</p>" +
        "<p></p>" +
        "<p>11. <strong>Preferred bank:</strong> override the closest by distance choice with a specific bank.</p>" +
        "<p></p>" +
        "<p>12. <strong>Items to bank and keep:</strong> two comma separated lists matched against item names. Items to bank wins. Leave it empty to deposit everything except your keep list. Keep defaults hold the hammer and any bar you are smithing.</p>" +
        "<p></p>" +
        "<h3>Dropping</h3>" +
        "<p>13. <strong>Drop order:</strong> not used. Smithing always banks the finished items.</p>")
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
            keyName = "hammerOnToolBelt",
            name = "Hammer on tool belt",
            description = "Tick this if your hammer lives on the tool belt instead of the inventory. The bot then stops requiring (and withdrawing) a loose hammer. Leave unticked to use a normal inventory hammer.",
            position = 3,
            section = generalSection
    )
    default boolean hammerOnToolBelt() {
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
     * When Smithing level reaches this value, the script runs one final deposit pass
     * then shuts down. 0 disables the check.
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

    // Pause is an overlay button that toggles the shared Microbot.pauseAllScripts flag.
    // See AutoSmithingPlusOverlay.

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
     * Comma separated inclusion list matched as substrings against item names.
     * Leave empty to fall back to the keep list instead.
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
     * Defaults preserve the smithing setup (hammer plus selected bar) so the bot never
     * deposits the tools it needs to keep working.
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
            description = "Not used. Smithing always banks the finished items.",
            position = 0,
            section = droppingSection
    )
    default InteractOrder interactOrder() {
        return InteractOrder.STANDARD;
    }
}
