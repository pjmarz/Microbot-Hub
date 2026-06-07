package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Amethyst cutting products. Amethyst (item {@link ItemID#AMETHYST}) is cut with a chisel; the
 * production / make-X dialog then lists the product to make. Levels and yields are from the OSRS
 * Wiki (oldschool.runescape.wiki/w/Amethyst): each cut grants 60 Crafting XP and consumes one
 * amethyst.
 *
 * <p>productName is the exact label the make-X dialog shows (used to select the product). productId
 * is the resulting inventory item (used for banking the output). yieldPerAmethyst is the count of
 * products one amethyst makes (used for the GP/hr estimate).</p>
 */
@Getter
@RequiredArgsConstructor
public enum AmethystProduct {
    BOLT_TIPS("Bolt tips", "Amethyst bolt tips", ItemID.XBOWS_BOLT_TIPS_AMETHYST, 83, 15),
    ARROWTIPS("Arrowtips", "Amethyst arrowtips", ItemID.AMETHYST_ARROWHEADS, 85, 15),
    JAVELIN_HEADS("Javelin heads", "Amethyst javelin heads", ItemID.AMETHYST_JAVELIN_HEAD, 87, 5),
    DART_TIPS("Dart tips", "Amethyst dart tips", ItemID.AMETHYST_DART_TIP, 89, 8);

    private final String label;
    /** Exact product name as shown in the make-X / production dialog. */
    private final String productName;
    /** Resulting inventory item id, for banking the output. */
    private final int productId;
    private final int levelRequired;
    /** How many products one amethyst yields, for the GP/hr estimate. */
    private final int yieldPerAmethyst;

    @Override
    public String toString() {
        return label;
    }
}
