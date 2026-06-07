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
 * <p>Anvil dataset is backed by {@link AnvilLocations}.
 *
 * <p>Important: variant anvils (e.g. Lumbridge's Rusted Anvil, object ID 39620) restrict the
 * smithing widget to bronze items only. Picking a non-bronze bar at such an anvil will leave
 * the bot stalled with the widget showing greyed slots. The {@link #isBronzeOnly()} flag exposes
 * this.
 */
@Getter
@RequiredArgsConstructor
public enum AnvilLocationOption {
    AUTO_NEAREST("Auto / nearest", null, null, false, false),

    // F2P
    LUMBRIDGE_RUSTED("Lumbridge (bronze)",
            "Lumbridge Rusted Anvil", new WorldPoint(3227, 3258, 0), false, true),
    VARROCK_WEST("Varrock West",
            "Varrock West Anvil", new WorldPoint(3188, 3426, 0), false, false),

    // Varrock Central: 2 anvils in Horvik's armour shop, F2P, all-bar, 31 squares from W bank.
    VARROCK_CENTRAL("Varrock Central",
            "Varrock Central Anvil", new WorldPoint(3225, 3424, 0), false, false),

    // Varrock East: 2 anvils just south of the east bank on the west side, F2P, all-bar,
    // 35 squares from E bank.
    VARROCK_EAST("Varrock East",
            "Varrock East Anvil", new WorldPoint(3247, 3410, 0), false, false),

    // P2P anvils. The anchor is the town area near the anvil; the runtime 20-tile finder resolves
    // the exact anvil tile.
    YANILLE("Yanille (P2P)",
            "Yanille Anvil", new WorldPoint(2614, 3084, 0), true, false),
    SEERS_VILLAGE("Seers' Village (P2P)",
            "Seers' Village Anvil", new WorldPoint(2701, 3482, 0), true, false),
    BURTHORPE("Burthorpe (P2P)",
            "Burthorpe Anvil", new WorldPoint(2899, 3542, 0), true, false);

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
