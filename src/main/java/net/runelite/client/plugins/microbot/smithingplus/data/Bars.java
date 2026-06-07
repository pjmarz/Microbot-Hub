package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Smithing bar definitions. AutoSmithingPlus uses this enum as the user-facing bar selector
 * (Bronze / Iron / Steel / Mithril / Adamantite / Runite).
 *
 * <p>Bar names and smithing level requirements are from the
 * <a href="https://oldschool.runescape.wiki/w/Smithing/Smelting_bars">OSRS Wiki Smelting bars
 * page</a>; item IDs are RuneLite client constants ({@code net.runelite.api.gameval.ItemID}).
 */
@Getter
@RequiredArgsConstructor
public enum Bars {
    BRONZE("Bronze bar", ItemID.BRONZE_BAR, 1),
    IRON("Iron bar", ItemID.IRON_BAR, 15),
    STEEL("Steel bar", ItemID.STEEL_BAR, 30),
    MITHRIL("Mithril bar", ItemID.MITHRIL_BAR, 50),
    ADAMANTITE("Adamantite bar", ItemID.ADAMANTITE_BAR, 70),
    RUNITE("Runite bar", ItemID.RUNITE_BAR, 85);

    private final String name;
    private final int id;
    private final int requiredSmithingLevel;

    @Override
    public String toString() {
        return name;
    }
    public int getId() { return id; }

    /**
     * In-game name prefix for smithed products at this tier (e.g. "Bronze dagger", "Rune
     * platebody"). Note products use "Adamant" and "Rune", not the bar's "Adamantite"/"Runite".
     * Combined with {@link AnvilItem#getProductBaseName()} to look up the product's GE price for
     * the overlay GP/hr line.
     */
    public String getProductPrefix() {
        switch (this) {
            case BRONZE:     return "Bronze";
            case IRON:       return "Iron";
            case STEEL:      return "Steel";
            case MITHRIL:    return "Mithril";
            case ADAMANTITE: return "Adamant";
            case RUNITE:     return "Rune";
            default:         return null;
        }
    }
}
