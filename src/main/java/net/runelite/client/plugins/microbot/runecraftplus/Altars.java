package net.runelite.client.plugins.microbot.runecraftplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * Altar data carried over from the chillRunecraft base (authoritative in-client coords + object IDs).
 * Each Plus plugin builds as its own source set, so this is copied into the package rather than
 * referenced across packages.
 */
@Getter
@RequiredArgsConstructor
public enum Altars
{
    AIR_ALTAR("Air Altar", ObjectID.AIR_ALTAR, 34813, 34748, new WorldPoint(2990, 3291, 0), "Air tiara", "Air Talisman", "Air Rune", ItemID.AIR_TALISMAN, false, 5.0),
    EARTH_ALTAR("Earth Altar", ObjectID.EARTH_ALTAR, 34816, 34751, new WorldPoint(3302, 3470, 0), "Earth tiara", "Earth Talisman", "Earth Rune", ItemID.EARTH_TALISMAN, false, 6.5),
    WATER_ALTAR("Water Altar", ObjectID.WATER_ALTAR, 34815, 34750, new WorldPoint(3190, 3162, 0), "Water tiara", "Water Talisman", "Water Rune", ItemID.WATER_TALISMAN, false, 6.0),
    FIRE_ALTAR("Fire Altar", ObjectID.FIRE_ALTAR, 34817, 34752, new WorldPoint(3309, 3252, 0), "Fire tiara", "Fire Talisman", "Fire Rune", ItemID.FIRE_TALISMAN, false, 7.0),
    BODY_ALTAR("Body Altar", ObjectID.BODY_ALTAR, 34818, 34753, new WorldPoint(3053, 3443, 0), "Body tiara", "Body Talisman", "Body Rune", ItemID.BODY_TALISMAN, false, 7.5),
    NATURE_ALTAR("Nature Altar (P2P)", ObjectID.NATURE_ALTAR, 34821, 34756, new WorldPoint(2872, 3015, 0), "Nature tiara", "Nature Talisman", "Nature Rune", ItemID.NATURE_TALISMAN, true, 9.0);

    private final String altarName;
    private final int altarID;
    private final int altarRuinsID;
    private final int portalID;
    private final WorldPoint altarWorldPoint;
    private final String tiaraName;
    private final String talismanName;
    private final String runeName;
    private final int talismanID;
    private final boolean membersOnly;
    // Base Runecraft XP per essence at this altar (OSRS Wiki). Independent of level: the bonus
    // multiple-runes-per-essence at higher levels grant no extra XP, so XP / xpPerEssence yields the
    // exact essence consumed regardless of level. Used by the overlay to derive essence used and the
    // (lower-bound) rune count for GP/hr.
    private final double xpPerEssence;

    @Override
    public String toString()
    {
        return altarName;
    }
}
