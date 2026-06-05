package net.runelite.client.plugins.microbot.runecraftplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Combination-rune recipes for the binding-necklace method (v0.3.0).
 *
 * <p>A combination rune fuses two elements. You craft it at one element's altar while carrying the
 * OTHER element's runes plus pure essence, with a binding necklace worn for a guaranteed bind. A worn
 * necklace makes every bind succeed; without one each essence has only a 50% chance and failures
 * vanish with no XP, so the necklace is mandatory for this bot.</p>
 *
 * <p>The binding-necklace recipe needs FOUR things in addition to entering the altar:</p>
 * <ol>
 *   <li>Pure essence (one consumed per rune)</li>
 *   <li>The secondary element's runes (one consumed per rune, so equal counts of essence and runes)</li>
 *   <li>The secondary element's talisman, which is consumed once per altar click regardless of how
 *       many runes that click produces (so a single talisman covers a whole inventory batch)</li>
 *   <li>A worn binding necklace, which loses one charge per altar click and crumbles after 16</li>
 * </ol>
 *
 * <p>Entering the altar still uses the altar element's own talisman or tiara, handled by the existing
 * spine via {@link Altars#getTalismanName()} / {@link Altars#getTiaraName()}. The SECONDARY talisman
 * here is a separate, additionally consumed item.</p>
 *
 * <p>Each recipe is symmetric: every combo can be crafted at either of its two element altars, just
 * swapping which secondary element you carry. To keep the bot deterministic we fix one canonical
 * altar per combo (the {@link Altars} value below) and the matching secondary element. All six
 * standard elemental combos only ever need the Air, Earth, Water and Fire altars this plugin already
 * supports, so none are skipped. Combos that would need an altar we do not have do not exist as
 * standard elemental combination runes, so there is nothing to skip there.</p>
 *
 * <p>Recipes confirmed from the OSRS Wiki recipe templates (Mist/Dust/Mud/Smoke/Steam/Lava rune
 * pages). Chosen canonical altar -&gt; secondary element, level:</p>
 * <ul>
 *   <li>Mist  (Air+Water),  level 6,  Air Altar,   secondary Water (Water rune + Water talisman)</li>
 *   <li>Dust  (Air+Earth),  level 10, Earth Altar, secondary Air   (Air rune + Air talisman)</li>
 *   <li>Mud   (Water+Earth), level 13, Earth Altar, secondary Water (Water rune + Water talisman)</li>
 *   <li>Smoke (Air+Fire),   level 15, Fire Altar,  secondary Air   (Air rune + Air talisman)</li>
 *   <li>Steam (Water+Fire), level 19, Fire Altar,  secondary Water (Water rune + Water talisman)</li>
 *   <li>Lava  (Earth+Fire), level 23, Fire Altar,  secondary Earth (Earth rune + Earth talisman)</li>
 * </ul>
 *
 * <p>Item ids are the standard in-client gameval values. Runes: Fire 554, Water 555, Air 556,
 * Earth 557. Talismans: Air 1438, Earth 1440, Fire 1442, Water 1444.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ComboRune {

    NONE("None (single rune)", null, null, 0, null, 0, 1),

    MIST("Mist rune", Altars.AIR_ALTAR, "Water rune", ItemID.WATERRUNE, "Water talisman", ItemID.WATER_TALISMAN, 6),
    DUST("Dust rune", Altars.EARTH_ALTAR, "Air rune", ItemID.AIRRUNE, "Air talisman", ItemID.AIR_TALISMAN, 10),
    MUD("Mud rune", Altars.EARTH_ALTAR, "Water rune", ItemID.WATERRUNE, "Water talisman", ItemID.WATER_TALISMAN, 13),
    SMOKE("Smoke rune", Altars.FIRE_ALTAR, "Air rune", ItemID.AIRRUNE, "Air talisman", ItemID.AIR_TALISMAN, 15),
    STEAM("Steam rune", Altars.FIRE_ALTAR, "Water rune", ItemID.WATERRUNE, "Water talisman", ItemID.WATER_TALISMAN, 19),
    LAVA("Lava rune", Altars.FIRE_ALTAR, "Earth rune", ItemID.EARTHRUNE, "Earth talisman", ItemID.EARTH_TALISMAN, 23);

    private final String displayName;
    /** The altar to craft at. {@code null} for {@link #NONE}. */
    private final Altars altar;
    /** Display name of the secondary rune carried alongside pure essence (one consumed per rune). {@code null} for NONE. */
    private final String secondaryRuneName;
    /** Item id of that secondary rune. 0 for NONE. */
    private final int secondaryRuneId;
    /** Display name of the secondary talisman, consumed once per altar click. {@code null} for NONE. */
    private final String secondaryTalismanName;
    /** Item id of that secondary talisman. 0 for NONE. */
    private final int secondaryTalismanId;
    /** Minimum Runecraft level to craft this combo. */
    private final int levelRequired;

    public boolean isCombo() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
