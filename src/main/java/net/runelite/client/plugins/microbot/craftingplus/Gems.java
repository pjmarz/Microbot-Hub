package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cuttable gems with their Crafting level requirements. Amethyst tip-making is a different mechanic
 * (make-X widget); it has its own activity and lives in AmethystProduct + the AMETHYST activity.
 */
@Getter
@RequiredArgsConstructor
public enum Gems {
    OPAL("Opal", 1),
    JADE("Jade", 13),
    RED_TOPAZ("Red Topaz", 16),
    SAPPHIRE("Sapphire", 20),
    EMERALD("Emerald", 27),
    RUBY("Ruby", 34),
    DIAMOND("Diamond", 43),
    DRAGONSTONE("Dragonstone", 55),
    ONYX("Onyx", 67),
    ZENYTE("Zenyte", 89);

    private final String name;
    private final int levelRequired;

    @Override
    public String toString() {
        return name;
    }
}
