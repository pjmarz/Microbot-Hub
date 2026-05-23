package net.runelite.client.plugins.microbot.eventdismissplus;

import net.runelite.api.Skill;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * @ConfigGroup is "EventDismissPlus" (not "EventDismiss") to avoid cross-pollination with the
 * upstream eventdismiss/ plugin's saved settings.
 */
@ConfigGroup("EventDismissPlus")
@ConfigInformation("<h2>Event Dismiss Plus</h2>" +
        "<h3>Version: " + EventDismissPlusPlugin.version + "</h3>" +
        "<p>Companion plugin -- enable alongside any skill plugin (Mining/Smelting/Smithing/WC, AIO Fighter, etc.) to handle random events automatically.</p>" +
        "<p><strong>Default behavior:</strong> dismiss everything with a 2-5 second human-like delay. Engage with high-value events (Genie lamps, food drops, Frog Token, etc.) per the toggles below.</p>" +
        "<p><strong>Genie lamps</strong> auto-detect the skill you're currently training (via XP delta tracking over a 30-second rolling window) and apply there. Falls back to the configured skill if auto-detect finds nothing.</p>" +
        "<p>Uses Microbot's global BlockingEvent framework -- interrupts any running script when an event fires, then resumes.</p>")
public interface EventDismissPlusConfig extends Config {

    @ConfigSection(name = "General", description = "Response delay + general settings", position = 0)
    String generalSection = "general";

    @ConfigSection(name = "High-value engagement", description = "Per-event toggles. Off = dismiss instead of engage.", position = 1)
    String engagementSection = "engagement";

    @ConfigSection(name = "Genie lamp handling", description = "How to apply Genie / Beekeeper / Count Check lamps", position = 2)
    String genieSection = "genie";

    // --- General ---

    @ConfigItem(
            keyName = "responseDelayMin",
            name = "Response delay min (ms)",
            description = "Minimum delay before dismissing or engaging a random event. Anti-detection -- instant dismiss is a bot signal.",
            position = 0,
            section = generalSection
    )
    @Range(min = 0, max = 10000)
    default int responseDelayMin() {
        return 2000;
    }

    @ConfigItem(
            keyName = "responseDelayMax",
            name = "Response delay max (ms)",
            description = "Maximum delay before dismissing or engaging. Random uniform between min and max.",
            position = 1,
            section = generalSection
    )
    @Range(min = 0, max = 10000)
    default int responseDelayMax() {
        return 5000;
    }

    @ConfigItem(
            keyName = "globalSkipChance",
            name = "Random Skip Chance %",
            description = "v0.2.0: probability (0-100) of skipping engagement and just dismissing, even when the per-event toggle is ON. Adds variability for antiban -- humans don't engage every random event. 0 = never skip (always engage when configured), 100 = always skip.",
            position = 2,
            section = generalSection
    )
    @Range(min = 0, max = 100)
    default int globalSkipChance() {
        return 0;
    }

    // --- Per-event engagement toggles ---

    @ConfigItem(keyName = "engageGenie", name = "Engage Genie", description = "Rub lamp + apply to active skill. Off = dismiss.", position = 0, section = engagementSection)
    default boolean engageGenie() { return true; }

    @ConfigItem(keyName = "engageSandwichLady", name = "Engage Sandwich Lady", description = "Accept food offering (1/64 chance for 542k gp stale baguette). Off = dismiss.", position = 1, section = engagementSection)
    default boolean engageSandwichLady() { return true; }

    @ConfigItem(keyName = "engageDrunkenDwarf", name = "Engage Drunken Dwarf", description = "Accept beer + kebab. Off = dismiss.", position = 2, section = engagementSection)
    default boolean engageDrunkenDwarf() { return true; }

    @ConfigItem(keyName = "engageMysteriousOldMan", name = "Engage Mysterious Old Man", description = "Accept gift (coins, gems, key halves). Off = dismiss.", position = 3, section = engagementSection)
    default boolean engageMysteriousOldMan() { return true; }

    @ConfigItem(keyName = "engageStrangePlant", name = "Engage Strange Plant", description = "Pick fruit for 30% energy restore. Off = ignore.", position = 4, section = engagementSection)
    default boolean engageStrangePlant() { return true; }

    @ConfigItem(keyName = "engageBeekeeper", name = "Engage Beekeeper", description = "Claim XP lamp (same as Genie). Off = dismiss.", position = 5, section = engagementSection)
    default boolean engageBeekeeper() { return true; }

    @ConfigItem(keyName = "engageCountCheck", name = "Engage Count Check", description = "Claim XP lamp (requires Bank PIN on account). Off = dismiss.", position = 6, section = engagementSection)
    default boolean engageCountCheck() { return true; }

    @ConfigItem(keyName = "engageFrog", name = "Engage Frog Prince/Princess", description = "Kiss for Frog Token (redeemable for outfit/lamp). Off = dismiss.", position = 7, section = engagementSection)
    default boolean engageFrog() { return true; }

    @ConfigItem(keyName = "engageRickTurpentine", name = "Engage Rick Turpentine", description = "Accept loot (avg ~551gp, rare crystal key half). Off = dismiss.", position = 8, section = engagementSection)
    default boolean engageRickTurpentine() { return true; }

    @ConfigItem(keyName = "engageDrJekyll", name = "Engage Dr Jekyll", description = "Accept potion (matches clean herb in inv, or Strength(2) if no herb). Off = dismiss.", position = 9, section = engagementSection)
    default boolean engageDrJekyll() { return true; }

    // --- Genie lamp handling ---

    @ConfigItem(
            keyName = "autoDetectLampSkill",
            name = "Auto-detect lamp skill",
            description = "Apply lamp to the skill that gained the most XP in the last 30 sec. Off = use the fallback skill below.",
            position = 0,
            section = genieSection
    )
    default boolean autoDetectLampSkill() {
        return true;
    }

    @ConfigItem(
            keyName = "fallbackLampSkill",
            name = "Fallback lamp skill",
            description = "Skill to apply the lamp to if auto-detect is off OR if no active skill is detected.",
            position = 1,
            section = genieSection
    )
    default Skill fallbackLampSkill() {
        return Skill.MINING;
    }
}
