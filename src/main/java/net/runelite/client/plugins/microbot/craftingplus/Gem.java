package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

/**
 * Gems used by furnace jewellery (a cut gem set into a gold bar). Kept in this package because
 * craftingplus builds as its own source set and cannot reference another package's classes. Distinct
 * from {@link Gems}, which is the gem-CUTTING activity's level table.
 */
@Getter
@RequiredArgsConstructor
public enum Gem {
    NONE("", 0, 0),
    OPAL("opal", ItemID.OPAL, 1),
    JADE("jade", ItemID.JADE, 13),
    RED_TOPAZ("red topaz", ItemID.RED_TOPAZ, 16),
    SAPPHIRE("sapphire", ItemID.SAPPHIRE, 20),
    EMERALD("emerald", ItemID.EMERALD, 27),
    RUBY("ruby", ItemID.RUBY, 34),
    DIAMOND("diamond", ItemID.DIAMOND, 43),
    DRAGONSTONE("dragonstone", ItemID.DRAGONSTONE, 55),
    ONYX("onyx", ItemID.ONYX, 67),
    ZENYTE("zenyte", ItemID.ZENYTE, 89);

    private final String cutItemName;
    private final int cutItemID;
    private final int levelRequired;
}
