package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ore enum used as keys in {@link Bars}'s recipe map. Each entry's display name must match
 * the in-game inventory item name (case-insensitive), since {@code Rs2Inventory.hasItemAmount}
 * and {@code Rs2Bank.withdrawX} match against this string.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Ore names: <a href="https://oldschool.runescape.wiki/w/Ore">OSRS Wiki — Ore</a> for
 *       canonical inventory names</li>
 *   <li>Forked from upstream {@code chsami/Microbot-Hub/.../smelting/enums/Ores.java}</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Ores {
    TIN("tin ore"),
    COPPER("copper ore"),
    CLAY("clay"),
    BLURITE("blurite ore"),
    IRON("iron ore"),
    SILVER("silver ore"),
    COAL("coal"),
    GOLD("gold ore"),
    MITHRIL("mithril ore"),
    ADAMANTITE("adamantite ore"),
    RUNITE("runite ore"),
    BUCKET_OF_SAND("bucket of sand"),
    SODA_ASH("soda ash"),;

    private final String name;
    @Override
    public String toString() {
        return name;
    }
}
