package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-facing dropdown of named furnaces. Each constant carries the display name and the
 * WorldPoint to walk to. AUTO_NEAREST falls back to "find a furnace within 20 tiles of
 * initialPlayerLocation" behavior, which requires the user to start near a furnace.
 *
 * <p>Coordinates are taken from the OSRS Wiki Furnace page and each furnace's individual page.
 */
@Getter
@RequiredArgsConstructor
public enum FurnaceLocationOption {
    AUTO_NEAREST("Auto / nearest", null),

    // F2P
    LUMBRIDGE_FURNACE("Lumbridge", new WorldPoint(3227, 3257, 0)),
    AL_KHARID_FURNACE("Al Kharid", new WorldPoint(3275, 3186, 0)),
    EDGEVILLE_FURNACE("Edgeville", new WorldPoint(3109, 3499, 0)),
    FALADOR_WEST_FURNACE("Falador West", new WorldPoint(2972, 3372, 0)),
    // NOTE: Varrock has anvils only, no furnace.

    // Members
    PORT_PHASMATYS_FURNACE("Port Phasmatys (P2P)", new WorldPoint(3686, 3478, 0)),
    NEITIZNOT_FURNACE("Neitiznot (P2P)", new WorldPoint(2330, 3804, 0)),
    KELDAGRIM_FURNACE("Keldagrim (P2P)", new WorldPoint(2840, 10210, 0)),
    PRIFDDINAS_FURNACE("Prifddinas (P2P)", new WorldPoint(3271, 6101, 0)),
    SHILO_VILLAGE_FURNACE("Shilo Village (P2P)", new WorldPoint(2853, 2954, 0)),
    MOR_UL_REK_FURNACE("Mor Ul Rek (P2P)", new WorldPoint(2434, 5179, 0));

    private final String displayName;
    private final WorldPoint worldPoint;

    @Override
    public String toString() {
        return displayName;
    }
}
