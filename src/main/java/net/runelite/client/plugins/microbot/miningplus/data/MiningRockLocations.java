package net.runelite.client.plugins.microbot.miningplus.data;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mining rock location dataset. For each ore type, the list of mines where it can be found
 * with their WorldPoints, members/quest gates, and skill requirements.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Mine list and rock-types-per-mine: <a href="https://oldschool.runescape.wiki/w/Mines">OSRS Wiki, Mines</a></li>
 *   <li>Per-mine details: each named mine's own wiki page (e.g.
 *       <a href="https://oldschool.runescape.wiki/w/East_Lumbridge_Swamp_mine">East Lumbridge Swamp mine</a>)
 *       has the canonical "Rocks" table with quantity, level, XP per rock</li>
 *   <li>Quest/skill/varbit requirements: from each mine's wiki page</li>
 * </ul>
 */
public final class MiningRockLocations {

    private MiningRockLocations() {
    }

    /**
     * Gets the best locations for a specific rock/ore type.
     * Locations are ordered by preference (best locations first).
     */
    public static List<LocationOption> getLocationsForRock(Rocks rock) {
        switch (rock) {
            case TIN:
                return getTinRockLocations();
            case COPPER:
                return getCopperRockLocations();
            case CLAY:
                return getClayRockLocations();
            case IRON:
                return getIronRockLocations();
            case SILVER:
                return getSilverRockLocations();
            case COAL:
                return getCoalRockLocations();
            case GOLD:
                return getGoldRockLocations();
            case MITHRIL:
                return getMithrilRockLocations();
            case ADAMANTITE:
                return getAdamantiteRockLocations();
            case RUNITE:
                return getRuniteRockLocations();
            case BASALT:
                return getBasaltRockLocations();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Gets accessible locations for a specific rock type - filters out locations
     * the player cannot access based on quest and skill requirements.
     * Uses streams for efficient filtering.
     */
    public static List<LocationOption> getAccessibleLocationsForRock(Rocks rock) {
        return getLocationsForRock(rock).stream()
                .filter(LocationOption::hasRequirements)
                .collect(Collectors.toList());
    }

    /**
     * Gets the best accessible location for a rock type based on player position.
     * Returns null if no accessible locations are found.
     */
    public static LocationOption getBestAccessibleLocation(Rocks rock) {
        List<LocationOption> accessibleLocations = getAccessibleLocationsForRock(rock);

        if (accessibleLocations.isEmpty()) {
            return null;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null) {
            return accessibleLocations.stream()
                    .min((loc1, loc2) -> Integer.compare(
                            playerLocation.distanceTo(loc1.getWorldPoint()),
                            playerLocation.distanceTo(loc2.getWorldPoint())
                    ))
                    .orElse(accessibleLocations.get(0));
        }

        return accessibleLocations.get(0);
    }

    private static List<LocationOption> getTinRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Lumbridge Swamp EAST Mine - new players' mine, 5 tin + 5 copper rocks
        locations.add(new LocationOption(
                new WorldPoint(3229, 3148, 0),
                "Lumbridge Swamp East Mine", false
        ));

        // Varrock South West Mine
        locations.add(new LocationOption(
                new WorldPoint(3181, 3377, 0),
                "Varrock South West Mine", false
        ));

        // Varrock South East Mine - per OSRS Wiki has tin (6 rocks)
        locations.add(new LocationOption(
                new WorldPoint(3285, 3363, 0),
                "Varrock South East Mine", false
        ));

        // Al Kharid Mine - close to Al Kharid bank
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Barbarian Village Mine - per OSRS Wiki has tin (small count)
        locations.add(new LocationOption(
                new WorldPoint(3081, 3421, 0),
                "Barbarian Village Mine", false
        ));

        // Dwarven Mine - accessible via Falador
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Rimmington Mine - F2P, 2 tin rocks per wiki. Quiet alternative to Lumbridge.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // Edgeville Dungeon - F2P underground, 2 tin per wiki. LOW UTILITY (monsters, far from bank).
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getCopperRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Al Kharid Mine - excellent for beginners, close to bank
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Lumbridge Swamp EAST Mine - new players' mine, 5 copper + 5 tin rocks
        locations.add(new LocationOption(
                new WorldPoint(3229, 3148, 0),
                "Lumbridge Swamp East Mine", false
        ));

        // NOTE: Per OSRS Wiki South-west Varrock mine has no copper rocks; removed from this list.

        // Varrock South East Mine - per OSRS Wiki has copper (9 rocks)
        locations.add(new LocationOption(
                new WorldPoint(3285, 3363, 0),
                "Varrock South East Mine", false
        ));

        // Dwarven Mine
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Rimmington Mine - F2P, 5 copper rocks per wiki. Solid F2P alternative.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // Edgeville Dungeon - F2P underground, 2 copper per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getClayRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Varrock South West Mine - good clay rocks
        locations.add(new LocationOption(
                new WorldPoint(3181, 3377, 0),
                "Varrock South West Mine", false
        ));

        // Dwarven Mine
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Crafting Guild - per OSRS Wiki has clay; requires 40 Crafting + brown apron
        Map<Skill, Integer> craftingGuildClaySkills = new HashMap<>();
        craftingGuildClaySkills.put(Skill.CRAFTING, 40);
        Map<Integer, Integer> craftingGuildClayItems = new HashMap<>();
        craftingGuildClayItems.put(ItemID.BROWN_APRON, 1);
        locations.add(new LocationOption(
                new WorldPoint(2938, 3283, 0),
                "Crafting Guild", false,
                new HashMap<>(),
                craftingGuildClaySkills,
                new HashMap<>(),
                new HashMap<>(),
                craftingGuildClayItems
        ));

        // Rimmington Mine - F2P, 2 clay rocks per wiki.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getIronRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 4 iron rocks. Anchored on the ore field, not the
        // chamber-divider door at (3046, 9756).
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3028, 9737, 0),
                "Mining Guild",
                false,
                new HashMap<>(),
                miningGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Al Kharid Mine - excellent for F2P and low levels
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Varrock South East Mine - close to Varrock east bank
        locations.add(new LocationOption(
                new WorldPoint(3285, 3363, 0),
                "Varrock South East Mine", false
        ));

        // Dwarven Mine - underground location
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Ardougne South East Mine
        locations.add(new LocationOption(
                new WorldPoint(2704, 3330, 0),
                "Ardougne South East Mine", true
        ));

        // Bandit Camp Mine - excellent for higher levels with 16 iron rocks (Members only)
        locations.add(new LocationOption(
                new WorldPoint(3086, 3763, 0),
                "Bandit Camp Mine (Members)",
                true
        ));

        // Varrock South West Mine - per OSRS Wiki has iron (4 rocks)
        locations.add(new LocationOption(
                new WorldPoint(3181, 3377, 0),
                "Varrock South West Mine", false
        ));

        // Rimmington Mine - F2P, 6 iron rocks per wiki. Notable F2P iron spot.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // Edgeville Dungeon - F2P underground, 3 iron per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getSilverRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // NOTE: Per OSRS Wiki South-east Varrock mine has no silver rocks; removed from this list.
        // NOTE: Per OSRS Wiki Dwarven Mine also has no silver rocks; removed from this list.

        // Al Kharid Mine
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Varrock South West Mine - per OSRS Wiki has silver
        locations.add(new LocationOption(
                new WorldPoint(3181, 3377, 0),
                "Varrock South West Mine", false
        ));

        // Crafting Guild - per OSRS Wiki has silver; requires 40 Crafting + brown apron
        Map<Skill, Integer> craftingGuildSilverSkills = new HashMap<>();
        craftingGuildSilverSkills.put(Skill.CRAFTING, 40);
        Map<Integer, Integer> craftingGuildSilverItems = new HashMap<>();
        craftingGuildSilverItems.put(ItemID.BROWN_APRON, 1);
        locations.add(new LocationOption(
                new WorldPoint(2938, 3283, 0),
                "Crafting Guild", false,
                new HashMap<>(),
                craftingGuildSilverSkills,
                new HashMap<>(),
                new HashMap<>(),
                craftingGuildSilverItems
        ));

        // Edgeville Dungeon - F2P underground, 3 silver per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getCoalRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 37 coal rocks, the premier F2P coal spot
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3045, 9741, 0),
                "Mining Guild", false,
                new HashMap<>(),
                miningGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Dwarven Mine - accessible underground location
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Barbarian Village Mine
        locations.add(new LocationOption(
                new WorldPoint(3081, 3421, 0),
                "Barbarian Village Mine", false
        ));

        // Lumbridge Swamp WEST Mine - 7 coal rocks + 5 mithril + 2 adamant (post-2006 layout)
        locations.add(new LocationOption(
                new WorldPoint(3149, 3148, 0),
                "Lumbridge Swamp West Mine", false
        ));

        // Al Kharid Mine - per OSRS Wiki has coal (3 rocks)
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Ardougne South East Mine - per OSRS Wiki has coal (members only)
        locations.add(new LocationOption(
                new WorldPoint(2704, 3330, 0),
                "Ardougne South East Mine", true
        ));

        // Bandit Camp Mine - per OSRS Wiki has coal (members only)
        locations.add(new LocationOption(
                new WorldPoint(3086, 3763, 0),
                "Bandit Camp Mine (Members)", true
        ));

        // Heroes' Guild Mine - per OSRS Wiki has coal; requires Heroes' Quest
        Map<Quest, QuestState> heroesCoalQuests = new HashMap<>();
        heroesCoalQuests.put(Quest.HEROES_QUEST, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2916, 3506, 0),
                "Heroes' Guild Mine", true,
                heroesCoalQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Neitiznot / Central Fremennik Isles - per OSRS Wiki has coal; requires Fremennik Isles
        Map<Quest, QuestState> neitiznotCoalQuests = new HashMap<>();
        neitiznotCoalQuests.put(Quest.THE_FREMENNIK_ISLES, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2335, 3808, 0),
                "Neitiznot Mine", true,
                neitiznotCoalQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Seers' Village Coal Trucks - efficient for banking
        locations.add(new LocationOption(
                new WorldPoint(2569, 3462, 0),
                "Seers' Village Coal Trucks", true
        ));

        // Edgeville Dungeon - F2P underground, 6 coal rocks (notable F2P count).
        // Caveat: dense monsters and a long path to the nearest bank (Edgeville). Slower than Dwarven Mine.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getGoldRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Dwarven Mine
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Al Kharid Mine
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Crafting Guild - requires 40 Crafting
        Map<Skill, Integer> craftingGuildSkills = new HashMap<>();
        craftingGuildSkills.put(Skill.CRAFTING, 40);
        Map<Integer, Integer> craftingGuildItems = new HashMap<>();
        craftingGuildItems.put(ItemID.BROWN_APRON, 1);
        locations.add(new LocationOption(
                new WorldPoint(2938, 3283, 0),
                "Crafting Guild", false,
                new HashMap<>(),
                craftingGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                craftingGuildItems
        ));

        // Rimmington Mine - F2P, 2 gold rocks. The best F2P gold spot until access to the
        // Crafting Guild is acquired.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getMithrilRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 5 mithril rocks, chamber-1 cluster co-located with coal/iron,
        // no door transit. Tiles: (3046,9733)(3047,9733)(3050,9738)(3052,9739)(3053,9737); the
        // anchor is the cluster center, all 5 within ~7 tiles.
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3050, 9738, 0),
                "Mining Guild", false,
                new HashMap<>(),
                miningGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Al Kharid Mine
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Dwarven Mine
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Lumbridge Swamp WEST Mine - 5 mithril + 2 adamant + 7 coal (post-2006 layout)
        locations.add(new LocationOption(
                new WorldPoint(3149, 3148, 0),
                "Lumbridge Swamp West Mine", false
        ));

        // Bandit Camp Mine - per OSRS Wiki has mithril (members only)
        locations.add(new LocationOption(
                new WorldPoint(3086, 3763, 0),
                "Bandit Camp Mine (Members)", true
        ));

        // Heroes' Guild Mine - per OSRS Wiki has mithril; requires Heroes' Quest
        Map<Quest, QuestState> heroesMithrilQuests = new HashMap<>();
        heroesMithrilQuests.put(Quest.HEROES_QUEST, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2916, 3506, 0),
                "Heroes' Guild Mine", true,
                heroesMithrilQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Edgeville Dungeon - F2P underground, 1 mithril per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getAdamantiteRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 2 adamantite rocks, north chamber past the door at 3046,9756 (needs 70 Mining to mine)
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3042, 9772, 0),
                "Mining Guild", false,
                new HashMap<>(),
                miningGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Al Kharid Mine
        locations.add(new LocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine", false
        ));

        // Lumbridge Swamp WEST Mine - 2 adamantite + 5 mithril + 7 coal (post-2006 layout)
        locations.add(new LocationOption(
                new WorldPoint(3149, 3148, 0),
                "Lumbridge Swamp West Mine", false
        ));

        // Dwarven Mine - per OSRS Wiki has adamantite (1 rock)
        locations.add(new LocationOption(
                new WorldPoint(3034, 9822, 0),
                "Dwarven Mine", false
        ));

        // Bandit Camp Mine - per OSRS Wiki has adamantite (members only)
        locations.add(new LocationOption(
                new WorldPoint(3086, 3763, 0),
                "Bandit Camp Mine (Members)", true
        ));

        // Heroes' Guild Mine - per OSRS Wiki has adamantite; requires Heroes' Quest
        Map<Quest, QuestState> heroesAdamantiteQuests = new HashMap<>();
        heroesAdamantiteQuests.put(Quest.HEROES_QUEST, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2916, 3506, 0),
                "Heroes' Guild Mine", true,
                heroesAdamantiteQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // NOTE: Per OSRS Wiki Central Fremennik Isles mine (Neitiznot) does not host adamantite; removed from this list.

        // Edgeville Dungeon - F2P underground, 2 adamantite per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getRuniteRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild - best runite location with 2 rocks
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3046, 9756, 0),
                "Mining Guild", true,
                new HashMap<>(),
                miningGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Heroes' Guild Mine - requires Heroes' Quest and Quest Points
        Map<Quest, QuestState> heroesQuests = new HashMap<>();
        heroesQuests.put(Quest.HEROES_QUEST, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2916, 3506, 0),
                "Heroes' Guild Mine", true,
                heroesQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Neitiznot Mine - requires The Fremennik Isles quest
        Map<Quest, QuestState> neitiznotQuests = new HashMap<>();
        neitiznotQuests.put(Quest.THE_FREMENNIK_ISLES, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2335, 3808, 0),
                "Neitiznot Mine", true,
                neitiznotQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Lava Maze Runite Mine - Wilderness, dangerous but accessible
        locations.add(new LocationOption(
                new WorldPoint(3058, 3884, 0),
                "Lava Maze Runite Mine (Wilderness)", false
        ));

        return locations;
    }

    private static List<LocationOption> getBasaltRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Weiss Basalt Mine - requires Making Friends with My Arm quest
        Map<Quest, QuestState> weissQuests = new HashMap<>();
        weissQuests.put(Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2857, 3937, 0),
                "Weiss Basalt Mine", true,
                weissQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        return locations;
    }
}
