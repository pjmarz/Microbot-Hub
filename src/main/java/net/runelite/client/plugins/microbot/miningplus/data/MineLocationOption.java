package net.runelite.client.plugins.microbot.miningplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-facing dropdown of named mining locations.
 *
 * <p>Each constant carries a display name, a lookup name matching a
 * {@link LocationOption#getName()} entry in {@link MiningRockLocations}, and the
 * canonical {@link WorldPoint} for that mine. {@link #resolve(Rocks)} returns
 * a fully-equipped {@link LocationOption} (with quest/skill/varbit gates) if the
 * configured ore is actually hosted at this mine; otherwise it returns a minimal
 * {@link LocationOption} backed only by the WorldPoint, so the caller still walks
 * the player to the location they picked. Pair with {@link #hostsRock(Rocks)} to
 * surface a clear warning when the ore-vs-location combination is mismatched.
 */
@Getter
@RequiredArgsConstructor
public enum MineLocationOption {
    AUTO_BEST("Auto / best", null, null),

    // v0.3.2 display polish: shortened displayName to fit dropdown width.
    // locationName field (2nd arg) MUST match MiningRockLocations entries -- DO NOT shorten.
    LUMBRIDGE_SWAMP_WEST_MINE("Lumbridge Swamp W", "Lumbridge Swamp West Mine",
            new WorldPoint(3149, 3148, 0)),
    VARROCK_SOUTH_WEST_MINE("Varrock SW", "Varrock South West Mine",
            new WorldPoint(3181, 3377, 0)),
    AL_KHARID_MINE("Al Kharid", "Al Kharid Mine",
            new WorldPoint(3296, 3315, 0)),
    DWARVEN_MINE("Dwarven Mine", "Dwarven Mine",
            new WorldPoint(3034, 9822, 0)),
    MINING_GUILD("Mining Guild (P2P 60)", "Mining Guild (Members)",
            new WorldPoint(3046, 9756, 0)),
    VARROCK_SOUTH_EAST_MINE("Varrock SE", "Varrock South East Mine",
            new WorldPoint(3285, 3363, 0)),
    ARDOUGNE_SOUTH_EAST_MINE("Ardougne SE (P2P)", "Ardougne South East Mine",
            new WorldPoint(2704, 3330, 0)),
    BANDIT_CAMP_MINE("Bandit Camp (P2P)", "Bandit Camp Mine (Members)",
            new WorldPoint(3086, 3763, 0)),
    BARBARIAN_VILLAGE_MINE("Barbarian Village", "Barbarian Village Mine",
            new WorldPoint(3081, 3421, 0)),
    SEERS_COAL_TRUCKS("Seers' Coal Trucks (P2P)", "Seers' Village Coal Trucks",
            new WorldPoint(2569, 3462, 0)),
    CRAFTING_GUILD("Crafting Guild (40 Craft)", "Crafting Guild",
            new WorldPoint(2938, 3283, 0)),
    SHILO_VILLAGE_GEM_MINE("Shilo Gem (P2P)", "Shilo Village Gem Mine",
            new WorldPoint(2824, 2997, 0)),
    LUMBRIDGE_SWAMP_EAST_MINE("Lumbridge Swamp E", "Lumbridge Swamp East Mine",
            new WorldPoint(3229, 3148, 0)),
    NEITIZNOT_MINE("Neitiznot (P2P)", "Neitiznot Mine",
            new WorldPoint(2335, 3808, 0)),
    HEROES_GUILD_MINE("Heroes' Guild (P2P)", "Heroes' Guild Mine",
            new WorldPoint(2916, 3506, 0)),
    LAVA_MAZE_RUNITE_MINE("Lava Maze Rune (Wild!)", "Lava Maze Runite Mine (Wilderness)",
            new WorldPoint(3058, 3884, 0)),
    WEISS_BASALT_MINE("Weiss Basalt (P2P)", "Weiss Basalt Mine",
            new WorldPoint(2857, 3937, 0)),
    // v0.4.0: Rimmington Mine -- F2P, 2 tin + 5 copper + 6 iron + 2 clay + 2 gold rocks.
    // Per wiki: "north-east of Rimmington, west of Port Sarim, south-west of Falador".
    // No aggressive monsters. Coord (2978, 3236, 0) is wiki-derived approximation -- verify in-game.
    RIMMINGTON_MINE("Rimmington", "Rimmington Mine",
            new WorldPoint(2978, 3236, 0)),
    // v0.4.0: Edgeville Dungeon Mine -- F2P, in the non-Wilderness southern half of the dungeon.
    // 2 tin + 2 copper + 3 iron + 3 silver + 6 coal + 1 mithril + 2 adamantite rocks per wiki.
    // LOW UTILITY: wiki notes "almost never used due to large amount of monsters and bank distance".
    // Coord (3088, 9870, 0) is wiki-derived approximation -- underground, verify in-game.
    EDGEVILLE_DUNGEON_MINE("Edgeville Dgn (under)", "Edgeville Dungeon Mine",
            new WorldPoint(3088, 9870, 0));

    private final String displayName;
    private final String locationName;
    private final WorldPoint worldPoint;

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Returns true if this location is in {@link MiningRockLocations}'s list for the given rock.
     * Use this to detect ore-vs-location mismatches and surface a UI warning.
     */
    public boolean hostsRock(Rocks rock) {
        if (this == AUTO_BEST || rock == null || locationName == null) {
            return true;
        }
        return MiningRockLocations.getLocationsForRock(rock).stream()
                .anyMatch(opt -> locationName.equals(opt.getName()));
    }

    /**
     * Resolves this dropdown choice to a {@link LocationOption}.
     *
     * <p>If the configured ore is hosted at this location, returns the canonical
     * {@code LocationOption} with full quest/skill/varbit/item requirements.
     *
     * <p>If the ore is NOT hosted (e.g. user picked Lumbridge East with Ore = TIN, but East
     * only hosts mithril/adamantite), returns a minimal {@code LocationOption} backed by
     * just the {@link WorldPoint}. Caller still walks the player there. {@link #hostsRock}
     * lets the caller render a status warning.
     *
     * <p>Returns null only for {@link #AUTO_BEST}, signaling the caller should defer to
     * {@link MiningRockLocations#getBestAccessibleLocation(Rocks)}.
     */
    public LocationOption resolve(Rocks rock) {
        if (this == AUTO_BEST) {
            return null;
        }
        if (rock != null) {
            LocationOption matched = MiningRockLocations.getLocationsForRock(rock).stream()
                    .filter(opt -> locationName.equals(opt.getName()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return new LocationOption(worldPoint, locationName, false);
    }
}
