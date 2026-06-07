package net.runelite.client.plugins.microbot.woodcuttingplus;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.skills.fletching.data.FletchingItem;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.WoodcuttingPrimaryAction;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.WoodcuttingSecondaryAction;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.WoodcuttingTree;
import net.runelite.client.plugins.microbot.woodcuttingplus.enums.WoodcuttingWalkBack;

@ConfigGroup(AutoWoodcuttingPlusConfig.configGroup)
@ConfigInformation("<h2>Auto Woodcutting Plus</h2>" +
        "<h3>Version: " + AutoWoodcuttingPlusPlugin.version + "</h3>" +
        "<h3>General</h3>" +
        "<p>1. <strong>Enable auto woodcutting:</strong> turn off to keep the Forestry helpers running without cutting trees yourself.</p>" +
        "<p></p>" +
        "<p>2. <strong>Progressive mode:</strong> auto switch to the best tree for your Woodcutting level. Overrides the Tree choice below.</p>" +
        "<p></p>" +
        "<p>3. <strong>Tree:</strong> pick which tree to cut. Ignored while Progressive mode is on.</p>" +
        "<p></p>" +
        "<p>4. <strong>Distance to Stray:</strong> how far in tiles you may wander from your start tile before walking back.</p>" +
        "<p></p>" +
        "<p>5. <strong>Autohop when player detected:</strong> hop worlds when another player comes near.</p>" +
        "<p></p>" +
        "<p>6. <strong>Firemake only:</strong> burn logs where you stand instead of cutting. Start the plugin on your firemaking spot.</p>" +
        "<p></p>" +
        "<p>7. <strong>Woodcut at Hardwood Tree Patch:</strong> cut the hardwood patch trees instead of normal trees.</p>" +
        "<p></p>" +
        "<p>8. <strong>Loot Bird Nests:</strong> pick up bird nests dropped from trees and events.</p>" +
        "<p></p>" +
        "<p>9. <strong>Loot Seeds:</strong> pick up seeds dropped from events.</p>" +
        "<p></p>" +
        "<p>10. <strong>Loot my items only:</strong> only grab loot tagged to you. Use this on an Ironman.</p>" +
        "<p></p>" +
        "<p>11. <strong>Speed mode:</strong> turns off Microbot antiban for a faster but more detectable bot. Throwaway accounts only.</p>" +
        "<p></p>" +
        "<p>12. <strong>Stop conditions:</strong> set Stop after minutes, Stop after XP gained, or a Target level to shut the plugin down automatically. Leave any at 0 to ignore it. The Target level processes your current inventory first.</p>" +
        "<p></p>" +
        "<h3>Inventory management</h3>" +
        "<p>13. <strong>Primary action:</strong> what to do when your inventory fills. Bank logs, Drop logs, Burn logs on the spot, Burn logs at a campfire, or Fletch logs.</p>" +
        "<p></p>" +
        "<p>14. <strong>Fletching type:</strong> which item to fletch. Only used when the primary action is Fletch.</p>" +
        "<p></p>" +
        "<p>15. <strong>Secondary action:</strong> what to do with fletched items. None, Bank, Drop, String and Drop, or String and Bank. Only used after fletching.</p>" +
        "<p></p>" +
        "<p>16. <strong>Drop order:</strong> the order items are dropped when dropping.</p>" +
        "<p></p>" +
        "<p>17. <strong>Additional items to bank:</strong> extra item names to bank, comma separated.</p>" +
        "<p></p>" +
        "<p>18. <strong>Items to keep when dropping:</strong> item names to never drop, comma separated.</p>" +
        "<p></p>" +
        "<p>19. <strong>Walk back:</strong> return to your initial spot or to the last tree you cut.</p>" +
        "<p></p>" +
        "<h3>Forestry</h3>" +
        "<p>20. <strong>Enable forestry:</strong> master switch for the Forestry helpers. Use a Forestry world for best results.</p>" +
        "<p></p>" +
        "<p>21. <strong>Event toggles:</strong> turn each Forestry event on or off on its own. Egg, Entlings, Flowers, Fox, Hives, Leprechaun, Ritual, Root, and Struggling Sapling.</p>")
public interface AutoWoodcuttingPlusConfig extends Config {
    String configGroup = "WoodcuttingPlus";
    @ConfigSection(
            name = "General",
            description = "General",
            position = 0
    )
    String generalSection = "general";
    @ConfigSection(
            name = "Inventory management",
            description = "Configure how to handle full inventory",
            position = 1
    )
    String inventorySection = "inventory";

    @ConfigSection(
            name = "Forestry",
            description = "Forestry events",
            position = 2,
            closedByDefault = true
    )
    String forestrySection = "forestry";

    @ConfigItem(
            keyName = "enableWoodcutting",
            name = "Enable auto woodcutting",
            description = "Turn off to keep forestry helpers active without cutting trees automatically",
            position = 0,
            section = generalSection
    )
    default boolean enableWoodcutting() {
        return true;
    }

    @ConfigItem(
            keyName = "progressiveMode",
            name = "Progressive mode",
            description = "Automatically switch to the best tree based on your Woodcutting level",
            position = 1,
            section = generalSection
    )
    default boolean progressiveMode() {
        return false;
    }

    @ConfigItem(
            keyName = "Tree",
            name = "Tree",
            description = "Choose the tree (ignored when Progressive mode is enabled)",
            position = 2,
            section = generalSection
    )
    default WoodcuttingTree TREE() {
        return WoodcuttingTree.TREE;
    }

    @ConfigItem(
            keyName = "DistanceToStray",
            name = "Distance to Stray",
            description = "Set how far you can travel from your initial position in tiles",
            position = 3,
            section = generalSection
    )
    default int distanceToStray() {
        return 20;
    }

    @ConfigItem(
            keyName = "Hop",
            name = "Autohop when player detected",
            description = "Auto hop when a nearby player is detected",
            position = 4,
            section = generalSection
    )
    default boolean hopWhenPlayerDetected() {
        return false;
    }

    @ConfigItem(
            keyName = "Firemake",
            name = "Firemake only",
            description = "Turns into an Auto Firemaker only mode , start plugin initially at desired firemaking starting position , tested only at GE - North East ",
            position = 5,
            section = generalSection
    )
    default boolean firemakeOnly() {
        return false;
    }

    @ConfigItem(
            keyName = "HardwoodTreePatch",
            name = "Woodcut at Hardwood Tree Patch",
            description = "Woodcut at Hardwood Tree Patch",
            position = 6,
            section = generalSection
    )
    default boolean HardwoodTreePatch() {
        return false;
    }

    @ConfigItem(
            keyName = "LootNests",
            name = "Loot Bird Nests",
            description = "Loot bird nests from trees and events",
            position = 7,
            section = generalSection
    )
    default boolean lootBirdNests() { return true; }

    @ConfigItem(
            keyName = "LootSeeds",
            name = "Loot Seeds",
            description = "Loot seeds from events",
            position = 8,
            section = generalSection
    )
    default boolean lootSeeds() { return true; }

    @ConfigItem(
            keyName = "LootMyItemsOnly",
            name = "Loot my items only",
            description = "Only loot your items (Ironman)",
            position = 9,
            section = generalSection
    )
    default boolean lootMyItemsOnly() { return false;}

    @ConfigItem(
            keyName = "speedMode",
            name = "Speed mode (less antiban)",
            description = "Disables Microbot's antiban (action cooldowns, micro-breaks, Bezier mouse paths). Faster bot, more pattern-detectable. Throwaway accounts only.",
            position = 10,
            section = generalSection
    )
    default boolean speedMode() { return false; }

    @ConfigItem(
            keyName = "stopAfterMinutes",
            name = "Stop after (minutes)",
            description = "Auto-shutdown after this many minutes of runtime. 0 = no limit.",
            position = 11,
            section = generalSection
    )
    default int stopAfterMinutes() {
        return 0;
    }

    @ConfigItem(
            keyName = "stopAfterXp",
            name = "Stop after (XP gained)",
            description = "Auto-shutdown after gaining this much Woodcutting XP. 0 = no limit.",
            position = 12,
            section = generalSection
    )
    default int stopAfterXp() {
        return 0;
    }

    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop when Woodcutting reaches this level. Processes the current inventory first. 0 = disabled.",
            position = 13,
            section = generalSection
    )
    default int targetLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "PrimaryAction",
            name = "Primary action",
            description = "What to do when inventory is full",
            position = 0,
            section = inventorySection
    )
    default WoodcuttingPrimaryAction primaryAction() {
        return WoodcuttingPrimaryAction.DROP;
    }

    @ConfigItem(
            keyName = "FletchingType",
            name = "Fletching type",
            description = "Type of item to fletch (only applies if primary action is FLETCH)",
            position = 1,
            section = inventorySection
    )
    default FletchingItem fletchingType() {
        return FletchingItem.ARROW_SHAFT;
    }

    @ConfigItem(
            keyName = "SecondaryAction",
            name = "Secondary action",
            description = "What to do with fletched items (only applies after fletching)",
            position = 2,
            section = inventorySection
    )
    default WoodcuttingSecondaryAction secondaryAction() {
        return WoodcuttingSecondaryAction.DROP;
    }



    @ConfigItem(
            keyName = "dropOrder",
            name = "Drop order",
            description = "Order to drop items",
            position = 3,
            section = inventorySection
    )
    default InteractOrder interactOrder() {
        return InteractOrder.STANDARD;
    }
    @ConfigItem(
            keyName = "ItemsToBank",
            name = "Additional items to bank",
            description = "Extra items to bank (comma separated)",
            position = 4,
            section = inventorySection
    )
    default String itemsToBank() {
        return "logs,sturdy beehive parts,petal garland,golden pheasant egg,pheasant tail feathers,fox whistle,key,nest,fruit";
    }
    @ConfigItem(
            keyName = "ItemsToKeep",
            name = "Items to keep when dropping",
            description = "Items to keep in inventory (comma separated)",
            position = 5,
            section = inventorySection
    )
    default String itemsToKeep() {
        return "axe,tinderbox,knife,bowstring,crystal shard,demon tear,petal garland,golden pheasant egg,pheasant tail feathers,fox whistle,key, Anima-infused bark";
    }

    @ConfigItem(
            keyName = "WalkBack",
            name = "Walk back",
            description = "Walk back to initial spot or last cut down",
            position = 6,
            section = inventorySection
    )
    default WoodcuttingWalkBack walkBack() {
        return WoodcuttingWalkBack.LAST_LOCATION;
    }

    @ConfigItem(
            keyName = "enableForestry",
            name = "Enable forestry",
            description = "Enable forestry features",
            position = 0,
            section = forestrySection
    )
    default boolean enableForestry() {
        return false;
    }

     @ConfigItem(
             keyName = "eggEvent",
             name = "Enable Egg Event",
             description = "Enable the Egg forestry event",
             position = 1,
             section = forestrySection
     )
     default boolean eggEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "entlingsEvent",
             name = "Enable Entlings Event",
             description = "Enable the Entlings forestry event",
             position = 2,
             section = forestrySection
     )
     default boolean entlingsEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "flowersEvent",
             name = "Enable Flowers Event",
             description = "Enable the Flowers forestry event",
             position = 3,
             section = forestrySection,
             hidden = false
     )
     default boolean flowersEvent() {
         return false;
     }

     @ConfigItem(
             keyName = "foxEvent",
             name = "Enable Fox Event",
             description = "Enable the Fox forestry event",
             position = 4,
             section = forestrySection
     )
     default boolean foxEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "hivesEvent",
             name = "Enable Hives Event",
             description = "Enable the Hives forestry event",
             position = 5,
             section = forestrySection
     )
     default boolean hivesEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "leprechaunEvent",
             name = "Enable Leprechaun Event",
             description = "Enable the Leprechaun forestry event",
             position = 6,
             section = forestrySection
     )
     default boolean leprechaunEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "ritualEvent",
             name = "Enable Ritual Event",
             description = "Enable the Ritual forestry event",
             position = 7,
             section = forestrySection
     )
     default boolean ritualEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "rootEvent",
             name = "Enable Root Event",
             description = "Enable the Root forestry event",
             position = 8,
             section = forestrySection
     )
     default boolean rootEvent() {
         return true;
     }

     @ConfigItem(
             keyName = "saplingEvent",
             name = "Enable Struggling Sapling Event",
             description = "Enable the Struggling Sapling forestry event",
             position = 9,
             section = forestrySection
     )
     default boolean saplingEvent() {
         return true;
     }
}
