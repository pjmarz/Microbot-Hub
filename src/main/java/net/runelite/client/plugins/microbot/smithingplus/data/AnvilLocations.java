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
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-14 (Pilot #3 v0.1.0):</b> Initial entries from {@code tools/wiki-audit-anvils.ps1}
 *       run against the wiki. Two F2P anvils included: Varrock West (regular anvil, coord verified
 *       from upstream VarrockAnvil's {@code (3188, 3426, 0)}) and Lumbridge (Rusted anvil at object
 *       ID 39620, bronze-only). Walk-to coord for Lumbridge uses the adjacent Smiths' building
 *       furnace tile as an approximation; the actual anvil tile is resolved at runtime.</li>
 *   <li><b>Wiki-page blind spot caught (2026-05-14):</b> First pass missed Lumbridge anvil because
 *       the OSRS Wiki Anvil "Locations" table doesn't include variant anvils like the Rusted
 *       anvil. Audit script ({@code wiki-audit-anvils.ps1}) only parses that main table. User
 *       caught this. Mitigation: also check Rusted_anvil + other variant pages individually in
 *       the audit script (deferred to v0.2.0 audit hardening).</li>
 *   <li><b>2026-05-21 (v0.4.0 -- wiki-driven data expansion):</b> Added Varrock Central (Horvik's,
 *       2 anvils, 31 sq from W bank) and Varrock East (2 anvils south of E bank, 35 sq) per
 *       wiki Anvil "Locations" table. Both F2P, all-bar, no quest. Coords are wiki-derived
 *       approximations -- needs in-game verification. Other F2P entries from the wiki list
 *       (Camdozaal, Giants' Plateau, Dwarven Mine, Draynor sewer, Mudskipper Pt, Corsair Cove
 *       Dungeon, Wilderness Ruins) deferred to v0.5.0 -- they're either quest-gated, far from
 *       any bank, or in dangerous areas.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li><b>Variant anvil pages not auto-audited.</b> Rusted anvil + any future variants (e.g.
 *       Workbench, Tribal anvil, Imcando hammer-restricted entries) live on their own wiki pages
 *       outside the main Anvil locations table. v0.1.0 covers Rusted manually; v0.2.0 should
 *       enumerate all variant anvil pages and check each.</li>
 *   <li><b>Lumbridge walk-to coord is approximate.</b> Uses the furnace tile (3227, 3257, 0).
 *       Confirmed 2026-05-15 by live testing: the actual rusted anvil tile sits BEHIND the
 *       furnace (a couple tiles north/north-east of the furnace, inside the same building). The
 *       runtime name-based query still finds the anvil within the 20-tile search radius, but the
 *       walk path is suboptimal -- bot routes to the furnace door and the smithing widget opens
 *       from there. v0.2.0 fix: bump WorldPoint to the actual anvil tile (estimated around
 *       (3228, 3259, 0)); requires in-game right-click-tile inspection to nail down.</li>
 *   <li><b>F2P regular anvils unlisted:</b> Varrock East (2 anvils south of bank), Varrock
 *       Central (Horvik's, 2 anvils, far from any bank), Ruins of Camdozaal (2 anvils), Falador
 *       (Doric's hut, requires Doric's Quest), Giants' Plateau, Dwarven Mine, Mudskipper Point,
 *       Draynor Village sewer, Corsair Cove Dungeon, Wilderness Western Ruins. Add at v0.2.0+
 *       after in-game coord verification.</li>
 *   <li><b>Members anvils unlisted:</b> Yanille, Seers' Village, Burthorpe, Keldagrim, Prifddinas,
 *       and ~40 more. Curate at v0.2+.</li>
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
     * Canonical list of named anvils. v0.1.0 only includes two coord-verified F2P entries.
     */
    public static List<Anvil> all() {
        return Arrays.asList(
                // v0.2.1: WorldPoint history -> (3227, 3257, 0) v0.1.0 / (3228, 3259, 0) v0.2.0 /
                // (3227, 3258, 0) v0.2.1. v0.2.0 attempt bailed to exterior NW corner (likely
                // unreachable tile inside a wall). v0.2.1 uses same X as proven furnace tile, +1 Y.
                new Anvil("Lumbridge Rusted Anvil", new WorldPoint(3227, 3258, 0), false, true,
                        "F2P. Rusted anvil in the Smiths' building north of Lumbridge Castle, "
                                + "behind the Lumbridge furnace. BRONZE BARS ONLY (object ID 39620 "
                                + "restricts the widget to bronze items). Closest F2P anvil to a 2F bank."),
                new Anvil("Varrock West Anvil", new WorldPoint(3188, 3426, 0), false, false,
                        "F2P. Regular anvil (object ID 2097) ~10 tiles south of Varrock West bank. "
                                + "Supports all bar tiers. The meta F2P smithing spot. Coord verified "
                                + "against upstream VarrockAnvil plugin."),
                // v0.4.0: Horvik's shop in central Varrock. 2 anvils, 31 sq from W bank per wiki.
                // Coord (3225, 3424, 0) is wiki-derived approximation -- needs in-game verification.
                new Anvil("Varrock Central Anvil", new WorldPoint(3225, 3424, 0), false, false,
                        "F2P. 2 regular anvils in Horvik's armour shop, central Varrock. "
                                + "31 squares from Varrock West bank per wiki Anvil page. "
                                + "Supports all bar tiers. Less crowded than Varrock West."),
                // v0.4.0: South of Varrock East bank, west side. 2 anvils, 35 sq from E bank per wiki.
                // Coord (3247, 3410, 0) is wiki-derived approximation -- needs in-game verification.
                new Anvil("Varrock East Anvil", new WorldPoint(3247, 3410, 0), false, false,
                        "F2P. 2 regular anvils south of Varrock East bank (west side). "
                                + "35 squares from E bank per wiki Anvil page. Supports all bar tiers. "
                                + "Pairs naturally with Varrock East bank.")
        );
    }
}
