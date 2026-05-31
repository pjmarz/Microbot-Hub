package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-facing dropdown of named furnaces. Mirrors {@code MineLocationOption} from
 * AutoMiningPlus. Each constant carries the display name and the canonical WorldPoint to
 * walk to. AUTO_NEAREST falls back to "find a furnace within 20 tiles of initialPlayerLocation"
 * behavior (which requires the user to start near a furnace, mirroring upstream AutoSmelting).
 *
 * <p>Furnace data is backed by {@link FurnaceLocations}; coordinates verified against the
 * OSRS Wiki and re-audited by {@code tools/wiki-audit-furnaces.ps1} before each v1.0 release.
 *
 * <p>Note: unlike MineLocationOption, there's no "hosts target X" notion for furnaces — any
 * furnace can smelt any bar. The {@code hostsTarget()} method returns true for parity.
 */
@Getter
@RequiredArgsConstructor
public enum FurnaceLocationOption {
    AUTO_NEAREST("Auto / nearest", null, null, false),

    // v0.3.1 display polish: shortened. Pairing hints moved to plugin description docs.
    // F2P
    LUMBRIDGE_FURNACE("Lumbridge", "Lumbridge Furnace",
            new WorldPoint(3227, 3257, 0), false),
    AL_KHARID_FURNACE("Al Kharid", "Al Kharid Furnace",
            new WorldPoint(3275, 3186, 0), false),
    EDGEVILLE_FURNACE("Edgeville", "Edgeville Furnace",
            new WorldPoint(3109, 3499, 0), false),
    FALADOR_WEST_FURNACE("Falador West", "Falador West Furnace",
            new WorldPoint(2972, 3372, 0), false),
    // NOTE: Varrock has anvils only, no furnace. Removed VARROCK_WEST_FURNACE from v0.1.0.

    // Members
    PORT_PHASMATYS_FURNACE("Port Phasmatys (P2P)", "Port Phasmatys Furnace",
            new WorldPoint(3686, 3478, 0), true),
    NEITIZNOT_FURNACE("Neitiznot (P2P)", "Neitiznot Furnace",
            new WorldPoint(2330, 3804, 0), true),
    KELDAGRIM_FURNACE("Keldagrim (P2P)", "Keldagrim Furnace",
            new WorldPoint(2840, 10210, 0), true),
    PRIFDDINAS_FURNACE("Prifddinas (P2P)", "Prifddinas Furnace",
            new WorldPoint(3271, 6101, 0), true),
    SHILO_VILLAGE_FURNACE("Shilo Village (P2P)", "Shilo Village Furnace",
            new WorldPoint(2853, 2954, 0), true),
    MOR_UL_REK_FURNACE("Mor Ul Rek (P2P)", "Mor Ul Rek Furnace",
            new WorldPoint(2434, 5179, 0), true);

    private final String displayName;
    private final String locationName;
    private final WorldPoint worldPoint;
    private final boolean membersOnly;

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Stub for skeleton-interface compatibility. Furnaces can smelt any bar, so this always
     * returns true (except for the AUTO_NEAREST sentinel which has no fixed WorldPoint).
     */
    public boolean hostsTarget(Object target) {
        return this != AUTO_NEAREST;
    }
}
