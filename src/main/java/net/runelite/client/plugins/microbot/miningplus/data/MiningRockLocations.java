package net.runelite.client.plugins.microbot.miningplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
 * Mining rock location dataset — for each ore type, the list of mines where it can be found
 * with their WorldPoints, members/quest gates, and skill requirements.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Mine list and rock-types-per-mine: <a href="https://oldschool.runescape.wiki/w/Mines">OSRS Wiki — Mines</a></li>
 *   <li>Per-mine details: each named mine's own wiki page (e.g.
 *       <a href="https://oldschool.runescape.wiki/w/East_Lumbridge_Swamp_mine">East Lumbridge Swamp mine</a>)
 *       has the canonical "Rocks" table with quantity, level, XP per rock</li>
 *   <li>Quest/skill/varbit requirements: from each mine's wiki page</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-14 (v0.1.3):</b> Full audit via {@code tools/wiki-audit-mines.ps1} found
 *       15 discrepancies and applied corrections. Notable: Lumbridge East/West tin↔mithril
 *       swap (v0.1.2), Al Kharid Gem rock removal, Dwarven Silver removal, Varrock SW Copper
 *       removal, Heroes' Guild + Bandit Camp + several others got missing rocks added.
 *       Final audit at v0.1.3: 17 OK / 0 mismatch / 0 not found.</li>
 *   <li><b>2026-05-21 (v0.4.0 -- wiki-driven data expansion):</b> Added F2P entries surfaced by
 *       comprehensive OSRS Wiki re-audit. Rimmington Mine (F2P, 2 tin + 5 copper + 6 iron +
 *       2 clay + 2 gold rocks per wiki; "best F2P gold spot pre-Crafting Guild"). Edgeville
 *       Dungeon Mine (F2P underground; 2 tin + 2 copper + 3 iron + 3 silver + 6 coal + 1 mithril
 *       + 2 adamantite rocks; wiki notes "almost never used due to monsters + bank distance" so
 *       flagged LOW UTILITY in inline comments). Coords are wiki-derived approximations -- needs
 *       in-game verification on next throwaway session. Trigger: Fadli's bank gap surfaced
 *       post-deploy in v0.3.3; this audit catches similar gaps proactively.</li>
 *   <li><b>2026-05-28 (v0.5.8 -- Mining Guild F2P correction, closes the v0.4.0 deferred verification):</b>
 *       The Mining Guild entries (iron/coal/mithril/adamant) were all coord {@code (3046, 9756)} and
 *       flagged members-only. Live-verification on a 60-Mining F2P account (dev-tool tile hovers +
 *       agent server {@code /state}) found {@code (3046, 9756)} is the chamber-divider DOOR, ~16 tiles
 *       north of the iron/coal field -- the walker was routed to the door, never the rocks. Corrected to
 *       verified per-ore tiles: IRON {@code (3028, 9737)}, COAL {@code (3045, 9741)} [chamber 1, south of
 *       door], MITHRIL {@code (3037, 9773)}, ADAMANT {@code (3042, 9772)} [chamber 2, north of door].
 *       Flipped membersOnly false (60 Mining is the only gate). Rock counts corrected to F2P-actual
 *       (4 iron / 37 coal / 5 mithril / 2 adamant) from the inflated members-combined numbers.
 *       NOTE: mithril/adamant are in chamber 2 behind the door -- depends on Rs2Walker handling that
 *       door; verify in a soak. The RUNITE Mining Guild entry is left at the old coord/members flag
 *       (runite is not in the F2P area and not in the members guild per wiki; likely a phantom entry,
 *       unverifiable without a members account -- flagged for a future members-side audit).</li>
 *   <li>Re-audit before each minor version bump that touches this file.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public class MiningRockLocations {

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
            case GEM:
                return getGemRockLocations();
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

    /**
     * Gets the best locations for a specific rock type with resource information.
     * Locations are ordered by preference (best locations first).
     * Returns ResourceLocationOption instances with rock count data.
     */
    public static List<ResourceLocationOption> getResourceLocationsForRock(Rocks rock) {
        switch (rock) {
            case TIN:
                return getTinRockResourceLocations();
            case COPPER:
                return getCopperRockResourceLocations();
            case IRON:
                return getIronRockResourceLocations();
            case COAL:
                return getCoalRockResourceLocations();
            // Add other rock types as needed
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Gets accessible resource locations for a specific rock type - filters out locations
     * the player cannot access based on quest and skill requirements.
     */
    public static List<ResourceLocationOption> getAccessibleResourceLocationsForRock(Rocks rock) {
        return getResourceLocationsForRock(rock).stream()
                .filter(ResourceLocationOption::hasRequirements)
                .collect(Collectors.toList());
    }

    /**
     * Gets the best accessible resource location for a rock type with minimum resource requirements.
     * Prioritizes accessible locations, then resource count, then proximity to player.
     */
    public static ResourceLocationOption getBestAccessibleResourceLocation(Rocks rock, int minResources) {
        List<ResourceLocationOption> accessibleLocations = getAccessibleResourceLocationsForRock(rock);

        if (accessibleLocations.isEmpty()) {
            return null;
        }

        // Filter by minimum resource requirements
        List<net.runelite.client.plugins.microbot.miningplus.data.ResourceLocationOption> suitableLocations = accessibleLocations.stream()
                .filter(location -> location.hasMinimumResources(minResources))
                .collect(Collectors.toList());

        if (suitableLocations.isEmpty()) {
            // If no locations meet minimum requirements, use the best available
            suitableLocations = accessibleLocations;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null) {
            return suitableLocations.stream()
                    .max((loc1, loc2) -> Double.compare(
                            loc1.calculateResourceEfficiencyScore(playerLocation),
                            loc2.calculateResourceEfficiencyScore(playerLocation)
                    ))
                    .orElse(suitableLocations.get(0));
        }

        // If no player location, prefer locations with more resources
        return suitableLocations.stream()
                .max((loc1, loc2) -> Integer.compare(
                        loc1.getNumberOfResources(),
                        loc2.getNumberOfResources()
                ))
                .orElse(suitableLocations.get(0));
    }

    // Example resource location methods - showing tin and copper as examples
    private static List<ResourceLocationOption> getTinRockResourceLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();

        // Lumbridge Swamp West Mine - great for beginners, close to bank (estimated 4-5 tin rocks)
        locations.add(new ResourceLocationOption(
                new WorldPoint(3149, 3148, 0),
                "Lumbridge Swamp West Mine",
                false, // F2P location
                5 // Number of tin rock spawns
        ));

        // Al Kharid Mine - close to Al Kharid bank (estimated 3-4 tin rocks)
        locations.add(new ResourceLocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine",
                false, // F2P location
                4 // Number of tin rock spawns
        ));

        return locations;
    }

    private static List<ResourceLocationOption> getCopperRockResourceLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();

        // Al Kharid Mine - excellent for beginners, close to bank (estimated 6-7 copper rocks)
        locations.add(new ResourceLocationOption(
                new WorldPoint(3296, 3315, 0),
                "Al Kharid Mine",
                false, // F2P location
                7 // Number of copper rock spawns
        ));

        // Lumbridge Swamp West Mine - good for F2P (estimated 4-5 copper rocks)
        locations.add(new ResourceLocationOption(
                new WorldPoint(3149, 3148, 0),
                "Lumbridge Swamp West Mine",
                false, // F2P location
                5 // Number of copper rock spawns
        ));

        return locations;
    }

    private static List<ResourceLocationOption> getIronRockResourceLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();

        // Add iron rock locations with resource counts
        // This would be expanded with actual iron mining locations

        return locations;
    }

    private static List<ResourceLocationOption> getCoalRockResourceLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();

        // Add coal rock locations with resource counts
        // This would be expanded with actual coal mining locations

        return locations;
    }

    private static List<LocationOption> getTinRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Lumbridge Swamp EAST Mine - new players' mine, 5 tin + 5 copper rocks (post-2006 layout)
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

        // v0.4.0: Rimmington Mine - F2P, 2 tin rocks per wiki. Quiet alternative to Lumbridge.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // v0.4.0: Edgeville Dungeon - F2P underground, 2 tin per wiki. LOW UTILITY (monsters, far from bank).
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

        // Lumbridge Swamp EAST Mine - new players' mine, 5 copper + 5 tin rocks (post-2006 layout)
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

        // v0.4.0: Rimmington Mine - F2P, 5 copper rocks per wiki. Solid F2P alternative.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // v0.4.0: Edgeville Dungeon - F2P underground, 2 copper per wiki. LOW UTILITY.
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

        // v0.4.0: Rimmington Mine - F2P, 2 clay rocks per wiki.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getIronRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 4 iron rocks (v0.5.8: live-verified F2P; old coord 3046,9756 was the chamber-divider door, not ore)
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

        // v0.4.0: Rimmington Mine - F2P, 6 iron rocks per wiki. Notable F2P iron spot.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        // v0.4.0: Edgeville Dungeon - F2P underground, 3 iron per wiki. LOW UTILITY.
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

        // v0.4.0: Edgeville Dungeon - F2P underground, 3 silver per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getCoalRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 37 coal rocks, the premier F2P coal spot (v0.5.8: live-verified F2P)
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

        // v0.4.0: Edgeville Dungeon - F2P underground, 6 COAL rocks per wiki (notable F2P count!).
        // Caveat: monsters dense + long path to nearest bank (Edgeville). Slower than Dwarven Mine.
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

        // v0.4.0: Rimmington Mine - F2P, 2 gold rocks per wiki. Wiki: "the best way to mine gold
        // ore until access to the Crafting Guild is acquired" -- this is the F2P gold spot.
        locations.add(new LocationOption(
                new WorldPoint(2978, 3236, 0),
                "Rimmington Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getGemRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Shilo Village Gem Mine - requires Shilo Village quest
        Map<Quest, QuestState> shiloQuests = new HashMap<>();
        shiloQuests.put(Quest.SHILO_VILLAGE, QuestState.FINISHED);
        locations.add(new LocationOption(
                new WorldPoint(2824, 2997, 0),
                "Shilo Village Gem Mine", true,
                shiloQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // NOTE: Per OSRS Wiki Al Kharid mine does not host gem rocks; removed from this list.

        return locations;
    }

    private static List<LocationOption> getMithrilRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 5 mithril rocks, north chamber past the door at 3046,9756 (v0.5.8: live-verified F2P)
        Map<Skill, Integer> miningGuildSkills = new HashMap<>();
        miningGuildSkills.put(Skill.MINING, 60);
        locations.add(new LocationOption(
                new WorldPoint(3037, 9773, 0),
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

        // v0.4.0: Edgeville Dungeon - F2P underground, 1 mithril per wiki. LOW UTILITY.
        locations.add(new LocationOption(
                new WorldPoint(3088, 9870, 0),
                "Edgeville Dungeon Mine", false
        ));

        return locations;
    }

    private static List<LocationOption> getAdamantiteRockLocations() {
        List<LocationOption> locations = new ArrayList<>();

        // Mining Guild F2P area - 2 adamantite rocks, north chamber past the door at 3046,9756 (v0.5.8: live-verified F2P; needs 70 Mining to mine)
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

        // v0.4.0: Edgeville Dungeon - F2P underground, 2 adamantite per wiki. LOW UTILITY.
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
