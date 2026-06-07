package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ore enum retained from the smelting fork as keys in {@link Bars}'s recipe map. Smithing does
 * not consume ores (anvil work uses bars + hammer); dropping this enum would force divergence
 * from the smeltingplus data layer.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Ore names: <a href="https://oldschool.runescape.wiki/w/Ore">OSRS Wiki — Ore</a></li>
 *   <li>Copied from {@code smeltingplus/data/Ores.java}</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Ores {
    TIN("tin ore"),
    COPPER("copper ore"),
    IRON("iron ore"),
    COAL("coal"),
    MITHRIL("mithril ore"),
    ADAMANTITE("adamantite ore"),
    RUNITE("runite ore");

    private final String name;
    @Override
    public String toString() {
        return name;
    }
}
