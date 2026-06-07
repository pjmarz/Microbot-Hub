package net.runelite.client.plugins.microbot.runecraftplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("RunecraftPlus")
@ConfigInformation("<h2>Auto Runecraft Plus</h2>" +
        "<h3>Version: " + AutoRunecraftPlusPlugin.version + "</h3>" +
        "<p>1. <strong>Altar:</strong> the altar to craft at. The bot banks, walks there, enters, crafts, and returns. Air, Earth, Water, Fire and Body run in F2P; Nature is members.</p>" +
        "<p></p>" +
        "<p>2. <strong>Essence:</strong> Rune, Pure, or Daeyalt. Pure and rune give identical XP (pick by cost); daeyalt is 50% more XP (members, self mined). Keep your talisman or tiara and essence in the bank.</p>" +
        "<p></p>" +
        "<p>3. <strong>Use pouches:</strong> fills and empties essence pouches each trip, and repairs degraded ones via NPC Contact if you are on the Lunar spellbook. Pouches are members only.</p>" +
        "<p></p>" +
        "<p>4. <strong>Stop after / Target level:</strong> auto shutdown thresholds. Target level banks first.</p>" +
        "<p></p>" +
        "<p>5. <strong>League mode:</strong> presses an arrow key to defeat the idle logout.</p>" +
        "<p></p>" +
        "<p>6. <strong>Speed mode:</strong> disables Microbot antiban for a faster, more detectable pace. Throwaway accounts only.</p>" +
        "<p></p>" +
        "<p>7. <strong>Combo rune:</strong> members only. Leave on None for normal single-rune crafting. When set to a combination rune the bot ignores the Altar choice above, walks to the correct element altar, wears a binding necklace for a guaranteed bind, crafts, and re-equips a fresh necklace whenever the worn one crumbles. Keep these in your bank: pure essence, the secondary element's runes, the secondary element's talisman, and a stack of binding necklaces. Mist needs level 6 (Air altar, Water), Dust 10 (Earth altar, Air), Mud 13 (Earth altar, Water), Smoke 15 (Fire altar, Air), Steam 19 (Fire altar, Water), Lava 23 (Fire altar, Earth). Pouches are turned off in combo mode. Magic Imbue is not used yet.</p>")
public interface AutoRunecraftPlusConfig extends Config {

    @ConfigSection(name = "General", description = "General settings", position = 0)
    String generalSection = "general";

    @ConfigSection(name = "Combo runes", description = "Combination-rune crafting (members)", position = 1, closedByDefault = true)
    String comboSection = "combo";

    @ConfigItem(
            keyName = "altar",
            name = "Altar",
            description = "Which altar to craft runes at.",
            position = 0,
            section = generalSection
    )
    default Altars altar() {
        return Altars.AIR_ALTAR;
    }

    @ConfigItem(
            keyName = "essenceType",
            name = "Essence",
            description = "Which essence to craft with. Pure and Rune give identical XP; Daeyalt is +50% (members).",
            position = 1,
            section = generalSection
    )
    default EssenceType essenceType() {
        return EssenceType.PURE;
    }

    @ConfigItem(
            keyName = "usePouches",
            name = "Use pouches",
            description = "Fill/empty essence pouches each trip (and repair via NPC Contact on Lunar). Members-only.",
            position = 2,
            section = generalSection
    )
    default boolean usePouches() {
        return true;
    }

    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 3,
            section = generalSection
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Runecraft XP. 0 = no limit.",
            position = 4,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Runecraft reaches this level. Banks the inventory first. 0 = disabled.",
            position = 5,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "leagueMode",
            name = "League mode (anti-AFK)",
            description = "Periodically presses an arrow key to reset the idle-logout timer.",
            position = 6,
            section = generalSection
    )
    default boolean leagueMode() {
        return false;
    }

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban. Faster, more pattern-detectable. Throwaway only.",
            position = 7,
            section = generalSection
    )
    default boolean speedMode() {
        return false;
    }

    @ConfigItem(
            keyName = "comboRune",
            name = "Combo rune",
            description = "Combination rune to craft. None keeps normal single-rune crafting (uses the Altar above). "
                    + "Any other choice overrides the Altar, carries the secondary runes plus binding necklaces, and binds "
                    + "at the correct altar. Members only.",
            position = 0,
            section = comboSection
    )
    default ComboRune comboRune() {
        return ComboRune.NONE;
    }

    @ConfigItem(
            keyName = "spareBindingNecklaces",
            name = "Spare necklaces",
            description = "How many binding necklaces to keep in the inventory as spares in addition to the one worn. "
                    + "A worn necklace lasts 16 altar clicks, so a small buffer covers a long trip. Ignored when Combo rune is None.",
            position = 1,
            section = comboSection
    )
    default int spareBindingNecklaces() {
        return 3;
    }
}
