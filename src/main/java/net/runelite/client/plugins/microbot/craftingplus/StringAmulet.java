package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Amulets that can be strung with a ball of wool. Using wool on an unstrung amulet "(u)" opens a
 * make-X / production dialog; the whole batch is strung at once (4 Crafting XP each, no level
 * requirement for any tier). Gold is free to play; the gem amulets are members only.
 *
 * <p>unstrungId is the "(u)" amulet withdrawn + consumed; strungId is the strung result, banked as
 * output. Ids are from net.runelite.api.gameval.ItemID.</p>
 */
@Getter
@RequiredArgsConstructor
public enum StringAmulet {
    GOLD("Gold amulet", ItemID.UNSTRUNG_GOLD_AMULET, ItemID.STRUNG_GOLD_AMULET),
    SAPPHIRE("Sapphire amulet", ItemID.UNSTRUNG_SAPPHIRE_AMULET, ItemID.STRUNG_SAPPHIRE_AMULET),
    EMERALD("Emerald amulet", ItemID.UNSTRUNG_EMERALD_AMULET, ItemID.STRUNG_EMERALD_AMULET),
    RUBY("Ruby amulet", ItemID.UNSTRUNG_RUBY_AMULET, ItemID.STRUNG_RUBY_AMULET),
    DIAMOND("Diamond amulet", ItemID.UNSTRUNG_DIAMOND_AMULET, ItemID.STRUNG_DIAMOND_AMULET),
    DRAGONSTONE("Dragonstone amulet", ItemID.UNSTRUNG_DRAGONSTONE_AMULET, ItemID.STRUNG_DRAGONSTONE_AMULET);

    private final String label;
    /** The unstrung "(u)" amulet: withdrawn and consumed. */
    private final int unstrungId;
    /** The strung amulet: the output, banked at the end of a trip. */
    private final int strungId;

    @Override
    public String toString() {
        return label;
    }
}
