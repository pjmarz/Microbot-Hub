package net.runelite.client.plugins.microbot.smithingplus.data;

import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;

/**
 * Anvil location dataset. Mirrors {@code FurnaceLocations} from smeltingplus but for anvils.
 *
 * <p>{@link AnvilLocationOption} is the user-facing dropdown facade; this class is internal data
 * backing it.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Regular anvil list: <a href="https://oldschool.runescape.wiki/w/Anvil">OSRS Wiki — Anvil</a>
 *       (the "Locations" section table; object ID 2097)</li>
 *   <li>Variant anvils have their own wiki pages and are NOT in the main Anvil locations table.
 *       Example: <a href="https://oldschool.runescape.wiki/w/Rusted_anvil">Rusted anvil</a>
 *       (object ID 39620, Lumbridge, bronze-only).</li>
 *   <li>Members/F2P status: per anvil's wiki page</li>
 *   <li>Walk-to WorldPoints: derived from sibling buildings where possible (e.g. Lumbridge anvil
 *       shares the Smiths' building with the Lumbridge furnace at (3227, 3257, 0)). The script
 *       resolves the actual anvil tile at runtime via {@code withNameContains("anvil")}.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Variant anvils (Rusted anvil and similar) live on their own wiki pages outside the main
 *       Anvil locations table, so they must be added by hand.</li>
 *   <li>The Lumbridge rusted anvil tile sits behind the furnace (a couple tiles north/north-east,
 *       inside the same building). The runtime name-based query still finds it within the 20-tile
 *       search radius, but the walk path routes to the furnace door and the smithing widget opens
 *       from there.</li>
 * </ul>
 */
public final class AnvilLocations {

    public static final class Anvil {
        public final String name;
        public final WorldPoint worldPoint;
        public final boolean membersOnly;
        public final boolean bronzeOnly;
        public final String notes;

        public Anvil(String name, WorldPoint worldPoint, boolean membersOnly, boolean bronzeOnly, String notes) {
            this.name = name;
            this.worldPoint = worldPoint;
            this.membersOnly = membersOnly;
            this.bronzeOnly = bronzeOnly;
            this.notes = notes;
        }
    }

    private AnvilLocations() {}

    /**
     * Canonical list of named anvils.
     */
    public static List<Anvil> all() {
        return Arrays.asList(
                new Anvil("Lumbridge Rusted Anvil", new WorldPoint(3227, 3258, 0), false, true,
                        "F2P. Rusted anvil in the Smiths' building north of Lumbridge Castle, "
                                + "behind the Lumbridge furnace. BRONZE BARS ONLY (object ID 39620 "
                                + "restricts the widget to bronze items). Closest F2P anvil to a 2F bank."),
                new Anvil("Varrock West Anvil", new WorldPoint(3188, 3426, 0), false, false,
                        "F2P. Regular anvil (object ID 2097) ~10 tiles south of Varrock West bank. "
                                + "Supports all bar tiers. The meta F2P smithing spot."),
                // Horvik's shop in central Varrock. 2 anvils, 31 squares from W bank.
                new Anvil("Varrock Central Anvil", new WorldPoint(3225, 3424, 0), false, false,
                        "F2P. 2 regular anvils in Horvik's armour shop, central Varrock. "
                                + "31 squares from Varrock West bank. "
                                + "Supports all bar tiers. Less crowded than Varrock West."),
                // South of Varrock East bank, west side. 2 anvils, 35 squares from E bank.
                new Anvil("Varrock East Anvil", new WorldPoint(3247, 3410, 0), false, false,
                        "F2P. 2 regular anvils south of Varrock East bank (west side). "
                                + "35 squares from E bank. Supports all bar tiers. "
                                + "Pairs naturally with Varrock East bank."),
                // P2P anvils. The anchor is the town area near the anvil; the runtime 20-tile name
                // query resolves the exact tile.
                new Anvil("Yanille Anvil", new WorldPoint(2614, 3084, 0), true, false,
                        "Members. Anvil in Yanille, near the bank."),
                new Anvil("Seers' Village Anvil", new WorldPoint(2701, 3482, 0), true, false,
                        "Members. Anvil near Seers' Village / Camelot bank."),
                new Anvil("Burthorpe Anvil", new WorldPoint(2899, 3542, 0), true, false,
                        "Members. Anvil by the Warriors' Guild in Burthorpe.")
        );
    }
}
