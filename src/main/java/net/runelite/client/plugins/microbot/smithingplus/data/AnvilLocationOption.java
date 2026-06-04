package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-facing dropdown of named anvils. Mirrors {@code FurnaceLocationOption} from smeltingplus.
 * Each constant carries the display name and the canonical walk-to WorldPoint; the script
 * resolves the actual anvil tile at runtime via a name-based object query.
 *
 * <p>{@link #AUTO_NEAREST} falls back to "find an anvil within 20 tiles of initialPlayerLocation"
 * behavior (requires the user to start near an anvil, mirroring upstream VarrockAnvil's
 * stand-at-anvil-or-restart behavior).
 *
 * <p>Anvil dataset is backed by {@link AnvilLocations}; coordinates audited by
 * {@code tools/wiki-audit-anvils.ps1} before each minor version bump.
 *
 * <p>Important: variant anvils (e.g. Lumbridge's Rusted Anvil, object ID 39620) restrict the
 * smithing widget to bronze items only. Picking a non-bronze bar at such an anvil will leave
 * the bot stalled with the widget showing greyed slots. The {@link #isBronzeOnly()} flag exposes
 * this; v0.2.0 will add a pre-flight check.
 */
@Getter
@RequiredArgsConstructor
public enum AnvilLocationOption {
    AUTO_NEAREST("Auto / nearest", null, null, false, false),

    // v0.3.1 display polish: shortened. Bronze-only kept as critical reminder.
    // F2P
    // v0.2.1: WorldPoint walked through three guesses:
    //   v0.1.0: (3227, 3257, 0) -- furnace tile; Rs2Walker reaches but anvil is "behind" it.
    //   v0.2.0: (3228, 3259, 0) -- bumped +1 east + 2 north; Rs2Walker bailed to NW exterior corner
    //                              (likely tried to path into a wall, fell back to nearest reachable).
    //   v0.2.1: (3227, 3258, 0) -- same X as the proven-walkable furnace tile, +1 Y north. Should
    //                              land inside the building one tile north of the furnace.
    // If still off, ask the user to right-click the anvil tile in-game and report the X,Y,plane.
    LUMBRIDGE_RUSTED("Lumbridge (bronze)",
            "Lumbridge Rusted Anvil", new WorldPoint(3227, 3258, 0), false, true),
    VARROCK_WEST("Varrock West",
            "Varrock West Anvil", new WorldPoint(3188, 3426, 0), false, false),

    // v0.4.0: Varrock Central -- 2 anvils in Horvik's armour shop, F2P, all-bar, 31 sq from W bank.
    // Per OSRS Wiki Anvil page. Coord (3225, 3424, 0) is wiki-derived approximation -- verify in-game.
    VARROCK_CENTRAL("Varrock Central",
            "Varrock Central Anvil", new WorldPoint(3225, 3424, 0), false, false),

    // v0.4.0: Varrock East -- 2 anvils just south of the east bank on the west side, F2P, all-bar,
    // 35 sq from E bank. Per OSRS Wiki Anvil page. Coord (3247, 3410, 0) is wiki-derived
    // approximation -- verify in-game.
    VARROCK_EAST("Varrock East",
            "Varrock East Anvil", new WorldPoint(3247, 3410, 0), false, false),

    // v0.6.0: P2P anvils. osrsmap-confirmed towns; anchors approximate (runtime 20-tile finder
    // resolves the exact anvil tile; CONFIRM in-game). Keldagrim + Prifddinas deferred -- osrsmap
    // cannot render those regions (Keldagrim underground clamps; Prifddinas y~6100 renders black).
    YANILLE("Yanille (P2P)",
            "Yanille Anvil", new WorldPoint(2614, 3084, 0), true, false),
    SEERS_VILLAGE("Seers' Village (P2P)",
            "Seers' Village Anvil", new WorldPoint(2701, 3482, 0), true, false),
    BURTHORPE("Burthorpe (P2P)",
            "Burthorpe Anvil", new WorldPoint(2899, 3542, 0), true, false);

    // v0.5.0+: Camdozaal (quest-gated F2P), Giants' Plateau (F2P, far from bank), Mudskipper Pt,
    // Draynor sewer, Doric's hut (Doric's Quest), Corsair Cove Dungeon (P2P quest);
    // members anvils (Yanille, Seers, Keldagrim, Prifddinas, Burthorpe), after in-game coord
    // verification.

    private final String displayName;
    private final String locationName;
    private final WorldPoint worldPoint;
    private final boolean membersOnly;
    private final boolean bronzeOnly;

    @Override
    public String toString() {
        return displayName;
    }

    public boolean hostsTarget(Object target) {
        return this != AUTO_NEAREST;
    }
}
