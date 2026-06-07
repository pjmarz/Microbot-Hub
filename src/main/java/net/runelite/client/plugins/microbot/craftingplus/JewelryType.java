package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

/**
 * The bar a piece of jewellery is cast from.
 */
@Getter
@RequiredArgsConstructor
public enum JewelryType {
    GOLD(ItemID.GOLD_BAR),
    SILVER(ItemID.SILVER_BAR);

    private final int itemID;
}
