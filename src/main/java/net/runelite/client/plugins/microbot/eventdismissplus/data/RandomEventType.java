package net.runelite.client.plugins.microbot.eventdismissplus.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Identifies which random event NPCs grant an XP lamp that needs the skill-picker dialogue
 * flow (Genie / Beekeeper / Count Check). Everything else either gets a simple
 * accept-and-acknowledge click-through or falls through to {@code npc.click("Dismiss")}.
 *
 * <p>NPC names match the in-game display name as exposed by {@code Rs2NpcModel.getName()}.
 * Beekeeper is matched as both "Bee keeper" and "Beekeeper" to cover the in-game spelling.
 */
public final class RandomEventType {

    private static final Set<String> LAMP_NPCS = new HashSet<>(Arrays.asList(
            "genie",
            "bee keeper",
            "beekeeper",
            "count check"));

    private RandomEventType() {}

    /**
     * True when the named NPC grants an XP lamp that requires the skill-picker widget flow
     * (Genie / Beekeeper / Count Check). False (including for unknown names) means the event
     * is handled with a plain dialogue click-through or dismissed.
     */
    public static boolean givesLamp(String name) {
        if (name == null) return false;
        return LAMP_NPCS.contains(name.toLowerCase());
    }
}
