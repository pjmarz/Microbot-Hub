package net.runelite.client.plugins.microbot.smeltingplus.data;

import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;

/**
 * Furnace location dataset. Mirrors the {@code MiningRockLocations} pattern from AutoMiningPlus
 * but for furnaces — entries are simple named WorldPoints since smelting doesn't have a
 * per-bar dataset variation the way mining has rocks-per-ore.
 *
 * <p>The {@link FurnaceLocationOption} enum is the user-facing dropdown facade; this class
 * is internal data backing it.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Furnace list: <a href="https://oldschool.runescape.wiki/w/Furnace">OSRS Wiki — Furnace</a>
 *       (the "Locations" section table)</li>
 *   <li>Members/F2P status: same page's table</li>
 *   <li>Exact WorldPoint coordinates: each furnace's individual wiki page where available,
 *       in-game verification otherwise. The Furnace page itself does NOT provide tile coords.</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-14 (v0.1.1 + v0.1.2 audit):</b> Confirmed via
 *       {@code tools/wiki-audit-furnaces.ps1} — all 10 entries below have a wiki counterpart.
 *       Coordinates spot-checked: Lumbridge added at (3227, 3257, 0) per the Smiths' building
 *       location north of Lumbridge Castle. Earlier "Varrock West Furnace" entry (v0.1.0)
 *       removed in v0.1.1 — Varrock has anvils only, no furnace.</li>
 *   <li>Re-audit before each minor version bump that touches this file.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li>~20 members-only and quest-locked furnaces (Burgh de Rott, Mount Karuulm, Auburnvale,
 *       Cam Torum, Civitas illa Fortis, Darkmeyer, Dorgesh-Kaan, East Ardougne, Entrana,
 *       Grimstone Dungeon, Lassar Undercity, Lovakengj, Piscatoris, Rellekka, Salvager
 *       Overlook, Tal Teklan, The Forsaken Tower, The Onyx Crest, The Summer Shore, The Great
 *       Conch, Tyras Camp, Wilderness Resource Area, Wilderness Level 28, Zanaris, Enakhra's
 *       Temple, Underground Pass). Add in v0.2+ when targeting members content.</li>
 * </ul>
 */
public final class FurnaceLocations {

    public static final class Furnace {
        public final String name;
        public final WorldPoint worldPoint;
        public final boolean membersOnly;
        public final String notes;

        public Furnace(String name, WorldPoint worldPoint, boolean membersOnly, String notes) {
            this.name = name;
            this.worldPoint = worldPoint;
            this.membersOnly = membersOnly;
            this.notes = notes;
        }
    }

    private FurnaceLocations() {}

    /**
     * Canonical list of named furnaces. Order roughly by accessibility / popularity.
     * Each entry is a verified furnace tile (the tile the player should stand on / next to
     * when clicking the furnace object).
     */
    public static List<Furnace> all() {
        return Arrays.asList(
                // F2P
                new Furnace("Lumbridge Furnace", new WorldPoint(3227, 3257, 0), false,
                        "F2P. Inside the Smiths' building north of Lumbridge Castle. Lumbridge Castle bank (2F) is the nearest bank, ~35 tiles south and one staircase up."),
                new Furnace("Al Kharid Furnace", new WorldPoint(3275, 3186, 0), false,
                        "F2P. South of Al Kharid bank. Free, no toll if you only enter the furnace area."),
                new Furnace("Edgeville Furnace", new WorldPoint(3109, 3499, 0), false,
                        "F2P. Northwest of Edgeville bank, ~10 tiles. Closest free F2P furnace + bank pair."),
                new Furnace("Falador West Furnace", new WorldPoint(2972, 3372, 0), false,
                        "F2P. Falador west bank is the nearest bank. Decent F2P option."),
                // NOTE: Varrock has anvils only, NO furnace. Verified against OSRS Wiki Varrock page.

                // Members
                new Furnace("Port Phasmatys Furnace", new WorldPoint(3686, 3478, 0), true,
                        "Members. Requires Ectophial / Ghosts Ahoy completion for fast access."),
                new Furnace("Neitiznot Furnace", new WorldPoint(2330, 3804, 0), true,
                        "Members. Fremennik Isles area, bank nearby. Iceberg quest area."),
                new Furnace("Keldagrim Furnace", new WorldPoint(2840, 10210, 0), true,
                        "Members. Reached via The Giant Dwarf quest. Has Blast Furnace minigame too."),
                new Furnace("Prifddinas Furnace", new WorldPoint(3271, 6101, 0), true,
                        "Members. Requires Song of the Elves. Best F2P-flavored layout but P2P."),
                new Furnace("Shilo Village Furnace", new WorldPoint(2853, 2954, 0), true,
                        "Members. Requires Shilo Village. Furnace + bank in same building."),
                new Furnace("Mor Ul Rek Furnace", new WorldPoint(2434, 5179, 0), true,
                        "Members. Inside Mor Ul Rek (TzHaar city) via the Karamja volcano / Fight Cave. P2P content (corrected from a wrong F2P flag in v0.5.9).")
        );
    }
}
