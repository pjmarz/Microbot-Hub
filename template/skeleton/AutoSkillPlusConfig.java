// TEMPLATE FILE — see TEMPLATE.md. Replace "Skill" / "skillplus" / "SkillPlus" tokens.

package net.runelite.client.plugins.microbot.skillplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.skillplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.skillplus.data.SkillLocationOption;

// TODO(plus): the @ConfigGroup string MUST be unique across the Hub. Convention: <SkillName>Plus,
// e.g. "MiningPlus", "SmeltingPlus", "FishingPlus". Sharing it with an upstream plugin will
// stomp the user's saved settings for both.
@ConfigGroup("SkillPlus")
@ConfigInformation("<h2>Auto Skill Plus</h2>" +
        "<h3>Version: " + AutoSkillPlusPlugin.version + "</h3>" +
        "<p>TODO(plus): one-line HTML description per config item, numbered.</p>")
public interface AutoSkillPlusConfig extends Config {

    @ConfigSection(name = "General", description = "General settings", position = 0)
    String generalSection = "general";

    @ConfigSection(name = "Banking", description = "Banking settings", position = 1)
    String bankingSection = "bankingSection";

    // --- General section ---

    // TODO(plus): replace `primaryTarget()` below with your skill's primary enum (Rocks, Bars,
    // Fish, Logs, Food, etc.). This is the "what am I doing" knob.

    @ConfigItem(
            keyName = "primaryTarget",
            name = "Target",
            description = "What to gather/process. Replace this stub with a domain enum.",
            position = 0,
            section = generalSection
    )
    default String primaryTarget() {
        return "TODO";
    }

    /**
     * Pattern from AutoMiningPlus v0.1.0: a user-facing dropdown of named locations for the
     * skill. {@link SkillLocationOption} provides the enum; the script's updateActiveTarget()
     * resolves the choice to a concrete LocationOption with all quest/skill gates intact.
     */
    @ConfigItem(
            keyName = "skillLocation",
            name = "Location",
            description = "Walk to and anchor at this location before starting. AUTO_BEST picks the closest accessible location for the chosen target.",
            position = 1,
            section = generalSection
    )
    default SkillLocationOption skillLocation() {
        return SkillLocationOption.AUTO_BEST;
    }

    @ConfigItem(
            keyName = "distanceToStray",
            name = "Distance to stray",
            description = "How far the bot will wander from the anchor tile, in game tiles. Also the threshold for the 'walked too far, return to anchor' check.",
            position = 2,
            section = generalSection
    )
    default int distanceToStray() {
        return 20;
    }

    /**
     * Pattern from AutoMiningPlus v0.1.9. Disables Microbot's antiban via a single flag flip
     * in the script's run() method. Throwaway-only; do not enable on accounts you care about.
     *
     * <p>Don't add per-flag antiban controls to your plugin — the user already has the global
     * Antiban panel for that. See PATTERNS.md "Don't duplicate the Antiban panel" section.
     */
    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban (action cooldowns, micro-breaks, Bezier mouse paths). Faster bot, more pattern-detectable. Throwaway accounts only.",
            position = 3,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }

    // --- Banking section ---

    @ConfigItem(
            keyName = "useBank",
            name = "Use bank",
            description = "When inventory fills, walk to the configured bank (or nearest if AUTO_NEAREST), deposit, walk back.",
            position = 0,
            section = bankingSection
    )
    default boolean useBank() {
        return false;
    }

    /**
     * Pattern from AutoMiningPlus v0.1.4. Lets the user override the upstream
     * Rs2Bank.walkToBankAndUseBank() "nearest by raw distance" heuristic, which often picks
     * toll-gated or otherwise undesirable banks. {@link BankLocationOption} is skill-agnostic
     * and ships with 17 F2P + P2P banks pre-modeled.
     */
    @ConfigItem(
            keyName = "bankLocation",
            name = "Preferred bank",
            description = "Bank to use when inventory fills. AUTO_NEAREST = closest by raw distance (often picks toll-gated banks over slightly-further free ones).",
            position = 1,
            section = bankingSection
    )
    default BankLocationOption bankLocation() {
        return BankLocationOption.AUTO_NEAREST;
    }

    @ConfigItem(
            keyName = "itemsToBank",
            name = "Items to bank (comma-separated)",
            description = "Substrings matched case-insensitively against inventory item names at the bank. e.g. 'ore,uncut' deposits ores and uncut gems.",
            position = 2,
            section = bankingSection
    )
    default String itemsToBank() {
        return "TODO";
    }

    @ConfigItem(
            keyName = "itemsToKeep",
            name = "Items to keep (comma-separated)",
            description = "Substrings to keep in inventory when dropping (power-mining style). Default includes the tool. e.g. 'pickaxe' for mining.",
            position = 3,
            section = bankingSection
    )
    default String itemsToKeep() {
        return "TODO";
    }
}
