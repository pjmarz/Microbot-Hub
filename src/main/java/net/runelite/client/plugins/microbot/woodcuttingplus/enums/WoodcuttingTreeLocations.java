package net.runelite.client.plugins.microbot.woodcuttingplus.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.woodcuttingplus.ResourceLocationOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Per-tree location data: each tree type maps to a list of {@link ResourceLocationOption}
 * entries with hardcoded WorldPoints, member flags, quest/skill gates, and a resource count.
 * The 407-line table covers ~200 locations across 41 trees.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Tree location WorldPoints: <a href="https://oldschool.runescape.wiki/w/Tree">OSRS Wiki — Tree</a>
 *       (and per-tree pages for tier-specific locations)</li>
 *   <li>Quest gates: per-quest wiki pages (e.g. Monkey Madness I for Ape Atoll teak)</li>
 *   <li>Forked from upstream {@code chsami/Microbot-Hub/.../woodcutting/enums/WoodcuttingTreeLocations.java} v1.8.3</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>Origin:</b> Forked verbatim from upstream. No coord-level
 *       audit performed at v0.1.0; this file is treated as authoritative on the assumption
 *       that v1.8.3 has been live in production. Coord audit deferred to v0.2.0 (when we
 *       expose the named-location dropdown via {@code TreeLocationOption}).</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li><b>WorldPoint coords not wiki-audited at v0.1.0.</b> If upstream had any drift relative
 *       to current in-game tile positions, we inherit it. v0.2.0 audit will cross-check.</li>
 *   <li><b>Quest gates verified only via upstream's hasRequirements() logic.</b> If a quest is
 *       renamed or a varbit ID drifts, we silently lose access to that location.</li>
 *   <li><b>Resource counts (numberOfResources) are upstream's best estimates,</b> not wiki-
 *       sourced. They influence location ranking via {@code calculateResourceEfficiencyScore}.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public class WoodcuttingTreeLocations {
    
    /**
     * Gets the best locations for a specific tree type.
     * Locations are ordered by preference (best locations first).
     */
    public static List<ResourceLocationOption> getLocationsForTree(WoodcuttingTree tree) {
        switch (tree) {
            case TREE:
                return getRegularTreeLocations();
            case OAK:
                return getOakTreeLocations();
            case WILLOW:
                return getWillowTreeLocations();
            case TEAK_TREE:
                return getTeakTreeLocations();
            case MAPLE:
                return getMapleTreeLocations();
            case MAHOGANY:
                return getMahoganyTreeLocations();
            case YEW:
                return getYewTreeLocations();
            case BLISTERWOOD:
                return getBlisterwoodTreeLocations();
            case MAGIC:
                return getMagicTreeLocations();
            case REDWOOD:
                return getRedwoodTreeLocations();
            case EVERGREEN_TREE:
            case DEAD_TREE:
                return getRegularTreeLocations(); // Same as regular trees
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Gets accessible locations for a specific tree type - filters out locations 
     * the player cannot access based on quest and skill requirements.
     * Uses streams for efficient filtering.
     */
    public static List<ResourceLocationOption> getAccessibleLocationsForTree(WoodcuttingTree tree) {
        return getLocationsForTree(tree).stream()
                .filter(ResourceLocationOption::hasRequirements)
                .collect(Collectors.toList());
    }
    
     /**
     * Gets the best accessible resource location for a tree type with minimum resource requirements.
     * Prioritizes accessible locations, then resource count, then proximity to player.
     * 
     * @param tree The tree type to find locations for
     * @param minResources Minimum number of tree spawns required
     * @return The best location meeting criteria, or null if none found
     */    
    public static ResourceLocationOption getBestAccessibleResourceLocation(WoodcuttingTree tree, int minResources) {
        List<ResourceLocationOption> accessibleLocations = getAccessibleLocationsForTree(tree);
        
        if (accessibleLocations.isEmpty()) {
            return null;
        }
        
        // Filter by minimum resource requirements
        List<ResourceLocationOption> suitableLocations = accessibleLocations.stream()
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
    
    /**
     * Gets the best accessible location for a tree type based on player position.
     * Returns null if no accessible locations are found.
     */
    public static ResourceLocationOption getBestAccessibleLocation(WoodcuttingTree tree) {
        List<ResourceLocationOption> accessibleLocations = getAccessibleLocationsForTree(tree);
        
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
    
    private static List<ResourceLocationOption> getRegularTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Lumbridge - great for beginners, close to bank
        locations.add(new ResourceLocationOption(
                new WorldPoint(3192, 3223, 0),
                "Lumbridge General Trees",false,5
        ));
        
        // Grand Exchange area - convenient banking
        locations.add(new ResourceLocationOption(
                new WorldPoint(3151, 3231, 0),
                "Grand Exchange Trees",false,1
        ));
        
        // Varrock East - multiple trees
        locations.add(new ResourceLocationOption(
                new WorldPoint(3227, 3457, 0), 
                "Varrock East Trees",false,1
        ));
        
        // Falador Trees
        locations.add(new ResourceLocationOption(
                new WorldPoint(3002, 3374, 0), 
                "Falador Trees",false,1
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getOakTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Lumbridge area - best for low levels
        locations.add(new ResourceLocationOption(
                new WorldPoint(3190, 3247, 0), 
                "Lumbridge Oak Trees",false,1
        ));
        
        // Varrock West Bank area
        locations.add(new ResourceLocationOption(
                new WorldPoint(3085, 3481, 0), 
                "Varrock West Oak Trees",false,1
        ));
        
        // Draynor Village
        locations.add(new ResourceLocationOption(
                new WorldPoint(3103, 3279, 0), 
                "Draynor Village Oak Trees",false,1
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getWillowTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Port Sarim - excellent with nearby deposit box
        locations.add(new ResourceLocationOption(
                new WorldPoint(3059, 3253, 0), 
                "Port Sarim Willow Trees",false,1
        ));
        
        // Draynor Village - popular location
        locations.add(new ResourceLocationOption(
                new WorldPoint(3088, 3235, 0), 
                "Draynor Village Willow Trees",false,1
        ));
        
        // Barbarian Outpost
        locations.add(new ResourceLocationOption(
                new WorldPoint(2532, 3565, 0), 
                "Barbarian Outpost Willow Trees",false,1
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getTeakTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Castle Wars area - very popular for teaks
        locations.add(new ResourceLocationOption(
                new WorldPoint(2335, 3048, 0), 
                "Castle Wars Teak Trees",true,1
        ));
        
        // Ape Atoll - requires quest
        Map<Quest, QuestState> apeAtollQuests = new HashMap<>();
        apeAtollQuests.put(Quest.MONKEY_MADNESS_I, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(2774, 2697, 0),
                "Ape Atoll Teak Trees",true,1,
                apeAtollQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Mos Le'Harmless - requires quest
        Map<Quest, QuestState> mosLeHarmlessQuests = new HashMap<>();
        mosLeHarmlessQuests.put(Quest.CABIN_FEVER, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(3832, 3067, 0), 
                "Mos Le'Harmless Teak Trees",true,1,
                mosLeHarmlessQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getMapleTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();

        // Corsair Cove Resource Area - the only F2P maple spot. Quest-gated (The Corsair Curse +
        // Dragon Slayer I, both F2P). Maple trees (object 10832) captured live via the agent server
        // at (2477-2481, 2895-2899); anchor sits in the cluster.
        Map<Quest, QuestState> corsairQuests = new HashMap<>();
        corsairQuests.put(Quest.THE_CORSAIR_CURSE, QuestState.FINISHED);
        corsairQuests.put(Quest.DRAGON_SLAYER_I, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(2479, 2896, 0),
                "Corsair Cove Maple Trees", false, 3,
                corsairQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        // Seers' Village - very popular location (members)
        locations.add(new ResourceLocationOption(
                new WorldPoint(2720, 3465, 0),
                "Seers' Village Maple Trees" ,true,1
        ));

        return locations;
    }
    
    private static List<ResourceLocationOption> getMahoganyTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Ape Atoll - requires quest
        Map<Quest, QuestState> apeAtollQuests = new HashMap<>();
        apeAtollQuests.put(Quest.MONKEY_MADNESS_I, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(2716, 2710, 0), 
                "Ape Atoll Mahogany Trees",
                true,1,
                apeAtollQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Mos Le'Harmless - requires quest
        Map<Quest, QuestState> mosLeHarmlessQuests = new HashMap<>();
        mosLeHarmlessQuests.put(Quest.CABIN_FEVER, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(3824, 3053, 0), 
                "Mos Le'Harmless Mahogany Trees",
                true,1,
                mosLeHarmlessQuests,
              new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getYewTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Woodcutting Guild - requires 60 Woodcutting
        Map<Skill, Integer> wcGuildSkills = new HashMap<>();
        wcGuildSkills.put(Skill.WOODCUTTING, 60);
        locations.add(new ResourceLocationOption(
                new WorldPoint(1591, 3483, 0), 
                "Woodcutting Guild Yew Trees",
                true,1,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
                
        ));
        
        // Falador
        locations.add(new ResourceLocationOption(
                new WorldPoint(3052, 3272, 0), 
                "Falador Yew Trees",
                false,1
        ));
        
        // Lumbridge
        locations.add(new ResourceLocationOption(
                new WorldPoint(3165, 3220, 0), 
                "Lumbridge Yew Trees",false,1
        ));
        
        // Seers' Village
        locations.add(new ResourceLocationOption(
                new WorldPoint(2711, 3463, 0),
                "Seers' Village Yew Trees",true,1
        ));

        // Corsair Cove Resource Area - F2P (quest-gated: The Corsair Curse + Dragon Slayer I).
        // Yew trees (object 10822) captured live via the agent server at (2471-2477, 2886-2891).
        Map<Quest, QuestState> corsairQuests = new HashMap<>();
        corsairQuests.put(Quest.THE_CORSAIR_CURSE, QuestState.FINISHED);
        corsairQuests.put(Quest.DRAGON_SLAYER_I, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(2473, 2888, 0),
                "Corsair Cove Yew Trees", false, 3,
                corsairQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));

        return locations;
    }

    private static List<ResourceLocationOption> getBlisterwoodTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Darkmeyer - requires Sins of the Father
        Map<Quest, QuestState> darkmeberQuests = new HashMap<>();
        darkmeberQuests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
        locations.add(new ResourceLocationOption(
                new WorldPoint(3631, 3362, 0), 
                "Darkmeyer Blisterwood Trees",
                true,1,
                darkmeberQuests,
                  new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getMagicTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Woodcutting Guild - requires 60 Woodcutting
        Map<Skill, Integer> wcGuildSkills = new HashMap<>();
        wcGuildSkills.put(Skill.WOODCUTTING, 60);
        locations.add(new ResourceLocationOption(
                new WorldPoint(1610, 3443, 0), 
                "Woodcutting Guild Magic Trees",true,1,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Sorcerer's Tower
        locations.add(new ResourceLocationOption(
                new WorldPoint(2704, 3397, 0), 
                "Sorcerer's Tower Magic Tree",true,1
        ));
        
        
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getRedwoodTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Woodcutting Guild - requires 60 Woodcutting
        Map<Skill, Integer> wcGuildSkills = new HashMap<>();
        wcGuildSkills.put(Skill.WOODCUTTING, 60);
        locations.add(new ResourceLocationOption(
                new WorldPoint(1569, 3493, 0), 
                "Woodcutting Guild Redwood Trees",
                true,1,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        return locations;
    }
}
