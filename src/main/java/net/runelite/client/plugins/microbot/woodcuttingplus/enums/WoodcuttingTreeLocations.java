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
 *
 * <p>WorldPoints and resource counts are sourced from the OSRS Wiki
 * (<a href="https://oldschool.runescape.wiki/w/Tree">Tree</a> and per-tree pages), with quest
 * gates from the per-quest wiki pages. Resource counts influence location ranking via
 * {@code calculateResourceEfficiencyScore}.</p>
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
        
        // Lumbridge - great for beginners, close to bank.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3192, 3223, 0),
                "Lumbridge General Trees",false,5
        ));

        // Grand Exchange area - convenient banking.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3151, 3231, 0),
                "Grand Exchange Trees",false,4
        ));

        // Varrock East - multiple trees.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3227, 3457, 0),
                "Varrock East Trees",false,4
        ));

        // Falador Trees.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3002, 3374, 0),
                "Falador Trees",false,3
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getOakTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Lumbridge area - best for low levels.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3190, 3247, 0),
                "Lumbridge Oak Trees",false,3
        ));

        // Varrock West Bank area.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3085, 3481, 0),
                "Varrock West Oak Trees",false,3
        ));

        // Draynor Village.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3103, 3279, 0),
                "Draynor Village Oak Trees",false,2
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getWillowTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Port Sarim - excellent with nearby deposit box.
        // Wiki (Willow tree): 22 willows in southern Port Sarim.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3059, 3253, 0),
                "Port Sarim Willow Trees",false,22
        ));

        // Draynor Village - popular location.
        // Wiki (Willow tree): 5 willows south of Draynor bank.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3088, 3235, 0),
                "Draynor Village Willow Trees",false,5
        ));

        // Barbarian Outpost.
        // Wiki (Willow tree): 8 willows at the Barbarian Outpost.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2532, 3565, 0),
                "Barbarian Outpost Willow Trees",false,8
        ));
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getTeakTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Castle Wars area - very popular for teaks.
        // Wiki: a single teak tree sits south-west of Castle Wars.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2335, 3048, 0),
                "Castle Wars Teak Trees",true,1
        ));
        
        // Ape Atoll - requires quest
        Map<Quest, QuestState> apeAtollQuests = new HashMap<>();
        apeAtollQuests.put(Quest.MONKEY_MADNESS_I, QuestState.FINISHED);
        // Wiki (Teak tree): 3 teaks on the south-east side of Ape Atoll.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2774, 2697, 0),
                "Ape Atoll Teak Trees",true,3,
                apeAtollQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Mos Le'Harmless - requires quest
        Map<Quest, QuestState> mosLeHarmlessQuests = new HashMap<>();
        mosLeHarmlessQuests.put(Quest.CABIN_FEVER, QuestState.FINISHED);
        // Wiki (Teak tree): 2 teaks on the islands north of Mos Le'Harmless.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3832, 3067, 0),
                "Mos Le'Harmless Teak Trees",true,2,
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
        // Dragon Slayer I, both F2P).
        Map<Quest, QuestState> corsairQuests = new HashMap<>();
        corsairQuests.put(Quest.THE_CORSAIR_CURSE, QuestState.FINISHED);
        corsairQuests.put(Quest.DRAGON_SLAYER_I, QuestState.FINISHED);
        // Wiki (Maple tree): 4 maples in the Corsair Cove Resource Area.
        ResourceLocationOption corsairMaple = new ResourceLocationOption(
                new WorldPoint(2479, 2896, 0),
                "Corsair Cove Maple Trees", false, 4,
                corsairQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );
        // Bank here via the nearby Corsair Cove deposit box, not a far full bank.
        corsairMaple.setUseDepositBox(true);
        locations.add(corsairMaple);

        // Seers' Village - very popular location (members).
        // Wiki (Maple tree): 9 maples around Seers' Village (four north, three south, plus more).
        locations.add(new ResourceLocationOption(
                new WorldPoint(2720, 3465, 0),
                "Seers' Village Maple Trees" ,true,9
        ));

        return locations;
    }
    
    private static List<ResourceLocationOption> getMahoganyTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Ape Atoll - requires quest
        Map<Quest, QuestState> apeAtollQuests = new HashMap<>();
        apeAtollQuests.put(Quest.MONKEY_MADNESS_I, QuestState.FINISHED);
        // Wiki (Mahogany tree): 2 mahoganies on Ape Atoll.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2716, 2710, 0),
                "Ape Atoll Mahogany Trees",
                true,2,
                apeAtollQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Mos Le'Harmless - requires quest
        Map<Quest, QuestState> mosLeHarmlessQuests = new HashMap<>();
        mosLeHarmlessQuests.put(Quest.CABIN_FEVER, QuestState.FINISHED);
        // Wiki (Mahogany tree): 2 mahoganies on the islands north of Mos Le'Harmless.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3824, 3053, 0),
                "Mos Le'Harmless Mahogany Trees",
                true,2,
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
        // Wiki (Woodcutting Guild / Yew tree): 17 yews inside the guild.
        locations.add(new ResourceLocationOption(
                new WorldPoint(1591, 3483, 0),
                "Woodcutting Guild Yew Trees",
                true,17,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
                
        ));
        
        // Falador.
        // Wiki (Yew tree): 4 yews along the southern Falador wall.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3052, 3272, 0),
                "Falador Yew Trees",
                false,4
        ));

        // Lumbridge.
        // Wiki (Yew tree): 4 yews west of Lumbridge.
        locations.add(new ResourceLocationOption(
                new WorldPoint(3165, 3220, 0),
                "Lumbridge Yew Trees",false,4
        ));

        // Seers' Village.
        // Wiki (Yew tree) locations table lists 3 for Seers' Village. The pay-to-play training
        // guide describes 8 yews south of the flax field; using the conservative table value.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2711, 3463, 0),
                "Seers' Village Yew Trees",true,3
        ));

        // Corsair Cove Resource Area - F2P (quest-gated: The Corsair Curse + Dragon Slayer I).
        Map<Quest, QuestState> corsairQuests = new HashMap<>();
        corsairQuests.put(Quest.THE_CORSAIR_CURSE, QuestState.FINISHED);
        corsairQuests.put(Quest.DRAGON_SLAYER_I, QuestState.FINISHED);
        ResourceLocationOption corsairYew = new ResourceLocationOption(
                new WorldPoint(2473, 2888, 0),
                "Corsair Cove Yew Trees", false, 3,
                corsairQuests,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );
        // Bank here via the nearby Corsair Cove deposit box, not a far full bank.
        corsairYew.setUseDepositBox(true);
        locations.add(corsairYew);

        return locations;
    }

    private static List<ResourceLocationOption> getBlisterwoodTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Darkmeyer - requires Sins of the Father
        Map<Quest, QuestState> darkmeberQuests = new HashMap<>();
        darkmeberQuests.put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
        // Wiki: there is a single blisterwood tree in the Darkmeyer Arboretum.
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
        // Wiki (Magic tree / Woodcutting Guild): 8 magic trees inside the guild.
        locations.add(new ResourceLocationOption(
                new WorldPoint(1610, 3443, 0),
                "Woodcutting Guild Magic Trees",true,8,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        // Sorcerer's Tower.
        // Wiki (Magic tree): 4 magic trees in the vicinity of the Sorcerer's Tower.
        locations.add(new ResourceLocationOption(
                new WorldPoint(2704, 3397, 0),
                "Sorcerer's Tower Magic Tree",true,4
        ));
        
        
        
        return locations;
    }
    
    private static List<ResourceLocationOption> getRedwoodTreeLocations() {
        List<ResourceLocationOption> locations = new ArrayList<>();
        
        // Woodcutting Guild - requires 60 Woodcutting
        Map<Skill, Integer> wcGuildSkills = new HashMap<>();
        wcGuildSkills.put(Skill.WOODCUTTING, 60);
        // Wiki (Woodcutting Guild / Redwood tree): 2 redwood trees inside the guild.
        locations.add(new ResourceLocationOption(
                new WorldPoint(1569, 3493, 0),
                "Woodcutting Guild Redwood Trees",
                true,2,
                new HashMap<>(),
                wcGuildSkills,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ));
        
        return locations;
    }
}
