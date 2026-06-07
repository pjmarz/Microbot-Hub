package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Soft/cowhide leather products (needle + thread + leather -&gt; product via the make-X interface),
 * plus hardleather body. All cowhide products use 1 leather each.
 */
@Getter
@RequiredArgsConstructor
public enum Leather {
    LEATHER_GLOVES("Leather gloves", 1, ItemID.LEATHER, "Leather"),
    LEATHER_BOOTS("Leather boots", 7, ItemID.LEATHER, "Leather"),
    LEATHER_COWL("Leather cowl", 9, ItemID.LEATHER, "Leather"),
    LEATHER_VAMBRACES("Leather vambraces", 11, ItemID.LEATHER, "Leather"),
    LEATHER_BODY("Leather body", 14, ItemID.LEATHER, "Leather"),
    LEATHER_CHAPS("Leather chaps", 18, ItemID.LEATHER, "Leather"),
    HARDLEATHER_BODY("Hardleather body", 28, ItemID.HARD_LEATHER, "Hard leather");

    /** Item/product name as shown in the make-X interface (used to select it). */
    private final String productName;
    private final int levelRequired;
    /** The leather material item id (cowhide Leather, or Hard leather). */
    private final int materialId;
    private final String materialName;

    @Override
    public String toString() {
        return productName;
    }
}
