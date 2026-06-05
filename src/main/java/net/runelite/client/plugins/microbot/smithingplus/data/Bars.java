package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.Map;

/**
 * Smithing bar definitions. AutoSmithingPlus uses this enum as the user-facing bar selector
 * (Bronze / Iron / Steel / Mithril / Adamantite / Runite). The recipe map ({@code requiredMaterials})
 * is retained from the smelting fork even though smithing doesn't consume ores; harmless at
 * runtime and keeps the data layer aligned with smeltingplus for future cross-plugin work.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Bar names + IDs: <a href="https://oldschool.runescape.wiki/w/Smithing/Smelting_bars">OSRS Wiki — Smelting bars</a></li>
 *   <li>Smithing level requirements (to smelt): same wiki page</li>
 *   <li>Item IDs: RuneLite client constants ({@code net.runelite.api.gameval.ItemID})</li>
 *   <li>Copied from {@code smeltingplus/data/Bars.java}</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>Origin:</b> Copied from smeltingplus/data/Bars.java with package
 *       rename. smeltingplus's copy was audited at v0.1.1 (no corrections). Dropped MOLTEN_GLASS
 *       entry (only smelted, not smithed at an anvil). Re-audit if upstream smelting bumps the
 *       smelting table or if OSRS publishes a smithing rework.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li>The {@code requiredMaterials} ore recipe is irrelevant for smithing (anvil work consumes
 *       bars + hammer). Kept to avoid forking the Bars contract; harmless at runtime.</li>
 *   <li>Smithing-level-to-FORGE varies per {@code AnvilItem} per bar (e.g. Bronze Dagger = lvl 1,
 *       Rune Plate body = lvl 99) and is not in this file. Deferred to v0.2.0's
 *       {@code AnvilItem.requiredLevelByBar} table.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Bars {
    BRONZE("Bronze bar", ItemID.BRONZE_BAR, 1, Map.of(Ores.COPPER, 1, Ores.TIN, 1)),
    IRON("Iron bar", ItemID.IRON_BAR,  15, Map.of(Ores.IRON, 1)),
    STEEL("Steel bar", ItemID.STEEL_BAR,  30, Map.of(Ores.IRON, 1, Ores.COAL, 2)),
    MITHRIL("Mithril bar", ItemID.MITHRIL_BAR,  50, Map.of(Ores.MITHRIL, 1, Ores.COAL, 4)),
    ADAMANTITE("Adamantite bar", ItemID.ADAMANTITE_BAR,  70, Map.of(Ores.ADAMANTITE, 1, Ores.COAL, 6)),
    RUNITE("Runite bar", ItemID.RUNITE_BAR,  85, Map.of(Ores.RUNITE, 1, Ores.COAL, 8));

    private final String name;
    private final int id;
    private final int requiredSmithingLevel;
    private final Map<Ores, Integer> requiredMaterials;

    @Override
    public String toString() {
        return name;
    }
    public int getId() { return id; }

    public int maxBarsForFullInventory() {
        int amountForOneBar = requiredMaterials.values().stream().reduce(0, Integer::sum);
        return Rs2Inventory.capacity() / amountForOneBar;
    }

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
