package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.Map;

/**
 * Smithing bar recipes — what ores at what quantities produce each bar.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Recipe table: <a href="https://oldschool.runescape.wiki/w/Smithing/Smelting_bars">OSRS Wiki — Smithing/Smelting bars</a></li>
 *   <li>Smithing level requirements: same page</li>
 *   <li>Item IDs: RuneLite client constants ({@code net.runelite.api.gameval.ItemID})</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-14 (Pilot #2 v0.1.0):</b> Forked verbatim from upstream
 *       {@code chsami/Microbot-Hub/.../smelting/enums/Bars.java}. Recipes match the wiki
 *       Smelting bars table as of fork date.</li>
 *   <li><b>2026-05-14 (Cycle A v0.2.0):</b> Ran new {@code tools/wiki-audit-bars.ps1} against
 *       the OSRS Wiki Smithing page. 9 of 9 bars confirmed by name. One fuzzy level warning
 *       on Iron bar (level 15 not found in 200-char window near "Iron bar") is a false
 *       positive caused by table-structure formatting; level 15 verified manually on the wiki.
 *       Required by Cycle A's new progressive-smelt feature, which keys off
 *       {@code getRequiredSmithingLevel()}.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Bars {
    BRONZE("Bronze bar", ItemID.BRONZE_BAR, 1, Map.of(Ores.COPPER, 1, Ores.TIN, 1)),
    BLURITE("Blurite bar", ItemID.BLURITE_BAR,  13, Map.of(Ores.BLURITE, 1)),
    IRON("Iron bar", ItemID.IRON_BAR,  15, Map.of(Ores.IRON, 1)),
    SILVER("Silver bar", ItemID.SILVER_BAR,  20, Map.of(Ores.SILVER, 1)),
    STEEL("Steel bar", ItemID.STEEL_BAR,  30, Map.of(Ores.IRON, 1, Ores.COAL, 2)),
    GOLD("Gold bar", ItemID.GOLD_BAR,  40, Map.of(Ores.GOLD, 1)),
    MITHRIL("Mithril bar", ItemID.MITHRIL_BAR,  50, Map.of(Ores.MITHRIL, 1, Ores.COAL, 4)),
    ADAMANTITE("Adamantite bar", ItemID.ADAMANTITE_BAR,  70, Map.of(Ores.ADAMANTITE, 1, Ores.COAL, 6)),
    RUNITE("Runite bar", ItemID.RUNITE_BAR,  85, Map.of(Ores.RUNITE, 1, Ores.COAL, 8)),
    MOLTEN_GLASS("Molten glass", ItemID.MOLTEN_GLASS,  1, Map.of(Ores.SODA_ASH, 1, Ores.BUCKET_OF_SAND, 1)),;;

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

    public Map<Ores, Integer> getWithdrawalsWithCoalBag(int totalInventorySlots) {
        Map<Ores, Integer> result = new java.util.HashMap<>();

        if (!requiredMaterials.containsKey(Ores.COAL)) {
            return requiredMaterials.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
        }
        int invSlots = totalInventorySlots - 1;
        int coalPerBar = requiredMaterials.get(Ores.COAL);
        int totalMatsPerBar = requiredMaterials.values().stream().mapToInt(Integer::intValue).sum();
        for (int bars = invSlots; bars > 0; bars--) {
            int totalCoal = coalPerBar * bars;
            int coalInInv = Math.max(0, totalCoal - 27);
            int nonCoalMats = bars * totalMatsPerBar - totalCoal;
            int totalUsed = coalInInv + nonCoalMats + 1;
            if (totalUsed <= totalInventorySlots) {
                for (Map.Entry<Ores, Integer> entry : requiredMaterials.entrySet()) {
                    Ores ore = entry.getKey();
                    int totalAmount = entry.getValue() * bars;

                    if (ore == Ores.COAL) {
                        result.put(ore, coalInInv); // only inv coal needed
                    } else {
                        result.put(ore, totalAmount);
                    }
                }
                break;
            }
        }
        return result;
    }
}
