package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Ore enum used as keys in {@link Bars}'s recipe map. Each entry's display name must match
 * the in-game inventory item name (case-insensitive), since {@code Rs2Inventory.hasItemAmount}
 * and {@code Rs2Bank.withdrawX} match against this string.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Ore names: <a href="https://oldschool.runescape.wiki/w/Ore">OSRS Wiki — Ore</a> for
 *       canonical inventory names</li>
 *   <li>Item IDs: RuneLite client constants ({@code net.runelite.api.gameval.ItemID}); used by the
 *       overlay to price the input ores for the net GP/hr line</li>
 *   <li>Forked from upstream {@code chsami/Microbot-Hub/.../smelting/enums/Ores.java}</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Ores {
    TIN("tin ore", ItemID.TIN_ORE),
    COPPER("copper ore", ItemID.COPPER_ORE),
    CLAY("clay", ItemID.CLAY),
    BLURITE("blurite ore", ItemID.BLURITE_ORE),
    IRON("iron ore", ItemID.IRON_ORE),
    SILVER("silver ore", ItemID.SILVER_ORE),
    COAL("coal", ItemID.COAL),
    GOLD("gold ore", ItemID.GOLD_ORE),
    MITHRIL("mithril ore", ItemID.MITHRIL_ORE),
    ADAMANTITE("adamantite ore", ItemID.ADAMANTITE_ORE),
    RUNITE("runite ore", ItemID.RUNITE_ORE),
    // Glassblowing inputs are not used by any current smeltable bar recipe (molten glass dropped
    // in v0.5.9); id 0 so the overlay never prices them.
    BUCKET_OF_SAND("bucket of sand", 0),
    SODA_ASH("soda ash", ItemID.SODA_ASH),;

    private final String name;
    private final int itemId;
    @Override
    public String toString() {
        return name;
    }
}
