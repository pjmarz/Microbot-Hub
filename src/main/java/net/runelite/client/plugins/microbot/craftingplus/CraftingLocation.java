package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;

/**
 * Furnace + adjacent bank for furnace jewellery. EDGEVILLE is the F2P default (bank and furnace a
 * few tiles apart). ANYWHERE uses the nearest bank/furnace.
 */
@Getter
@RequiredArgsConstructor
public enum CraftingLocation {

    EDGEVILLE(new WorldPoint(3109, 3499, 0), BankLocation.EDGEVILLE),
    FALADOR(new WorldPoint(2975, 3369, 0), BankLocation.FALADOR_WEST),
    PORT_PHASMATYS(new WorldPoint(3687, 3479, 0), BankLocation.PORT_PHASMATYS),
    MOUNT_KARUULM(new WorldPoint(1324, 3808, 0), BankLocation.MOUNT_KARUULM),
    ZANARIS(new WorldPoint(2401, 4473, 0), BankLocation.ZANARIS),
    SHILO_VILLAGE(new WorldPoint(2856, 2967, 0), BankLocation.SHILO_VILLAGE),
    ANYWHERE(null, null);

    private final WorldPoint furnaceLocation;
    private final BankLocation bankLocation;

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
    }
}
