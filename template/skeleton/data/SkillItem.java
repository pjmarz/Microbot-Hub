// TEMPLATE FILE — see TEMPLATE.md. Drop this file if your skill doesn't have item/recipe data.
// Use for: cooking (raw → cooked), smelting (ore → bar), fletching (logs → bows), etc.
//
// Compare to Rocks.java (in mining/data) which models gathering targets with a level gate.

package net.runelite.client.plugins.microbot.skillplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum SkillItem {
    // TODO(plus): one constant per item. For recipes, store inputId + outputId + level.
    // Example shape for cooking:
    //   SHRIMP("Raw shrimp", 317, "Shrimp", 315, 1),
    //   TROUT("Raw trout", 335, "Trout", 333, 15),
    //
    // For smelting bars:
    //   BRONZE_BAR("Tin ore", 438, "Bronze bar", 2349, 1, "Copper ore", 436),
    //
    // Keep "TODO" first so a fresh template compiles.
    TODO("Placeholder", 1);

    private final String displayName;
    private final int requiredLevel;

    @Override
    public String toString() {
        return displayName;
    }

    public boolean hasRequiredLevel() {
        // TODO(plus): replace Skill.MINING with your skill enum.
        return Rs2Player.getSkillRequirement(Skill.MINING, requiredLevel);
    }
}
