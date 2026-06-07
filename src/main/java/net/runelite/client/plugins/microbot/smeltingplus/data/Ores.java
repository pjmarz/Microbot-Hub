package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Ore enum used as keys in {@link Bars}'s recipe map. Each entry's display name must match
 * the in-game inventory item name (case-insensitive), since {@code Rs2Inventory.hasItemAmount}
 * and {@code Rs2Bank.withdrawX} match against this string.
 *
 * <p>Ore names follow the <a href="https://oldschool.runescape.wiki/w/Ore">OSRS Wiki</a> canonical
 * inventory names. Item IDs come from RuneLite client constants
 * ({@code net.runelite.api.gameval.ItemID}) and are used by the overlay to price the input ores
 * for the net GP/hr line.
 */
@Getter
@RequiredArgsConstructor
public enum Ores {
    TIN("tin ore", ItemID.TIN_ORE),
    COPPER("copper ore", ItemID.COPPER_ORE),
    BLURITE("blurite ore", ItemID.BLURITE_ORE),
    IRON("iron ore", ItemID.IRON_ORE),
    SILVER("silver ore", ItemID.SILVER_ORE),
    COAL("coal", ItemID.COAL),
    GOLD("gold ore", ItemID.GOLD_ORE),
    MITHRIL("mithril ore", ItemID.MITHRIL_ORE),
    ADAMANTITE("adamantite ore", ItemID.ADAMANTITE_ORE),
    RUNITE("runite ore", ItemID.RUNITE_ORE);

    private final String name;
    private final int itemId;
    @Override
    public String toString() {
        return name;
    }
}
