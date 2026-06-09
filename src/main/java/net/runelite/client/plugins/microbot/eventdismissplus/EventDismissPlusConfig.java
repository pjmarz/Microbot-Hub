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
        "<p>Run this alongside any skilling or combat plugin to handle random events for you.</p>" +
        "<p><strong>Default:</strong> dismiss every event with a short human-like delay. Turn on the toggles below to engage the worthwhile ones (Genie lamp, Sandwich Lady, Frog Prince, and more) instead of dismissing.</p>" +
        "<p><strong>Genie lamps:</strong> auto-detects the skill you are training, from XP gained in the last 30 seconds, and uses the lamp there. If it cannot tell, it uses your fallback skill.</p>" +
        "<p><strong>Force lamp skill:</strong> if you train two skills at once and auto-detect guesses wrong, set this to the skill you want. Leave it on Auto detect for normal use.</p>" +
        "<p><strong>Timing:</strong> a response delay range plus a random skip chance keep the reactions human. Tune both in the section below.</p>" +
        "<p>It uses the client's blocking-event system: when an event fires it pauses your other script, handles it, then resumes.</p>")
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
            description = "Minimum delay before dismissing or engaging a random event. Anti-detection: instant dismiss is a bot signal.",
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
            description = "Probability (0-100) of skipping engagement and just dismissing, even when the per-event toggle is ON. Adds variability for antiban: humans don't engage every random event. 0 = never skip (always engage when configured), 100 = always skip.",
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

    @ConfigItem(keyName = "engageSandwichLady", name = "Engage Sandwich Lady", description = "Talk to the Sandwich Lady. The plugin does not pick from the food tray (widget selection isn't modeled), so this typically clicks through and falls back to dismiss. Off = dismiss.", position = 1, section = engagementSection)
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

    @ConfigItem(keyName = "engageDrJekyll", name = "Engage Dr Jekyll", description = "Talk to Dr Jekyll and click through the dialogue to accept whatever potion he gives (game decides based on your herbs). The plugin does not pick a specific potion; if the dialogue stalls it falls back to dismiss. Off = dismiss.", position = 9, section = engagementSection)
    default boolean engageDrJekyll() { return true; }

    // --- Genie lamp handling ---

    @ConfigItem(
            keyName = "forceLampSkill",
            name = "Force lamp skill",
            description = "Pick the skill to apply Genie / Beekeeper / Count Check lamps to. Auto detect = use the active skill (current behavior). Any other choice always uses that skill and ignores auto-detect and the fallback.",
            position = 0,
            section = genieSection
    )
    default LampSkillOverride forceLampSkill() {
        return LampSkillOverride.AUTO_DETECT;
    }

    @ConfigItem(
            keyName = "autoDetectLampSkill",
            name = "Auto-detect lamp skill",
            description = "Apply lamp to the skill that gained the most XP in the last 30 sec. Off = use the fallback skill below. Ignored when Force lamp skill is set to a specific skill.",
            position = 1,
            section = genieSection
    )
    default boolean autoDetectLampSkill() {
        return true;
    }

    @ConfigItem(
            keyName = "fallbackLampSkill",
            name = "Fallback lamp skill",
            description = "Skill to apply the lamp to if auto-detect is off OR if no active skill is detected. Ignored when Force lamp skill is set to a specific skill.",
            position = 2,
            section = genieSection
    )
    default Skill fallbackLampSkill() {
        return Skill.MINING;
    }

    /**
     * Lamp / XP-reward skill selection for Genie, Beekeeper and Count Check.
     *
     * <p>{@link #AUTO_DETECT} keeps the existing behavior exactly: the handler asks the
     * script for the active skill (highest XP delta over the rolling window) and falls back
     * to {@link EventDismissPlusConfig#fallbackLampSkill()} when nothing is detected.
     *
     * <p>Every other constant maps to one {@link Skill}. When the user picks one, the handler
     * uses {@link #getSkill()} directly and skips both auto-detect and the fallback. This is
     * for hybrid-training sessions where auto-detect picks the wrong skill.
     */
    enum LampSkillOverride {
        AUTO_DETECT("Auto detect", null),
        ATTACK("Attack", Skill.ATTACK),
        DEFENCE("Defence", Skill.DEFENCE),
        STRENGTH("Strength", Skill.STRENGTH),
        HITPOINTS("Hitpoints", Skill.HITPOINTS),
        RANGED("Ranged", Skill.RANGED),
        PRAYER("Prayer", Skill.PRAYER),
        MAGIC("Magic", Skill.MAGIC),
        COOKING("Cooking", Skill.COOKING),
        WOODCUTTING("Woodcutting", Skill.WOODCUTTING),
        FLETCHING("Fletching", Skill.FLETCHING),
        FISHING("Fishing", Skill.FISHING),
        FIREMAKING("Firemaking", Skill.FIREMAKING),
        CRAFTING("Crafting", Skill.CRAFTING),
        SMITHING("Smithing", Skill.SMITHING),
        MINING("Mining", Skill.MINING),
        HERBLORE("Herblore", Skill.HERBLORE),
        AGILITY("Agility", Skill.AGILITY),
        THIEVING("Thieving", Skill.THIEVING),
        SLAYER("Slayer", Skill.SLAYER),
        FARMING("Farming", Skill.FARMING),
        RUNECRAFT("Runecraft", Skill.RUNECRAFT),
        HUNTER("Hunter", Skill.HUNTER),
        CONSTRUCTION("Construction", Skill.CONSTRUCTION);

        private final String label;
        private final Skill skill;

        LampSkillOverride(String label, Skill skill) {
            this.label = label;
            this.skill = skill;
        }

        /**
         * @return the forced {@link Skill}, or {@code null} for {@link #AUTO_DETECT}.
         */
        public Skill getSkill() {
            return skill;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
