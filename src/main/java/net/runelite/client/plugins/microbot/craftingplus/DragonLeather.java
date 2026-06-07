package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Dragonhide armour pieces (needle + thread + dragon leather -&gt; product via the make-X interface).
 *
 * <p>The make-X interface lists the colour's pieces and each is keyboard-selectable by a digit:
 * {@code menuEntry} is the number key that picks this piece (body=1, vambraces=2, chaps=3), pressed
 * once the dialog opens.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DragonLeather {
    NONE("", 0, 0, 0, '0'),

    GREEN_DHIDE_VAMBRACES("Green d'hide vambraces", ItemID.DRAGON_LEATHER, ItemID.DRAGON_VAMBRACES, 57, '2'),
    GREEN_DHIDE_CHAPS("Green d'hide chaps", ItemID.DRAGON_LEATHER, ItemID.DRAGONHIDE_CHAPS, 60, '3'),
    GREEN_DHIDE_BODY("Green d'hide body", ItemID.DRAGON_LEATHER, ItemID.DRAGONHIDE_BODY, 63, '1'),

    BLUE_DHIDE_VAMBRACES("Blue d'hide vambraces", ItemID.DRAGON_LEATHER_BLUE, ItemID.BLUE_DRAGON_VAMBRACES, 66, '2'),
    BLUE_DHIDE_CHAPS("Blue d'hide chaps", ItemID.DRAGON_LEATHER_BLUE, ItemID.BLUE_DRAGONHIDE_CHAPS, 68, '3'),
    BLUE_DHIDE_BODY("Blue d'hide body", ItemID.DRAGON_LEATHER_BLUE, ItemID.BLUE_DRAGONHIDE_BODY, 71, '1'),

    RED_DHIDE_VAMBRACES("Red d'hide vambraces", ItemID.DRAGON_LEATHER_RED, ItemID.RED_DRAGON_VAMBRACES, 73, '2'),
    RED_DHIDE_CHAPS("Red d'hide chaps", ItemID.DRAGON_LEATHER_RED, ItemID.RED_DRAGONHIDE_CHAPS, 75, '3'),
    RED_DHIDE_BODY("Red d'hide body", ItemID.DRAGON_LEATHER_RED, ItemID.RED_DRAGONHIDE_BODY, 77, '1'),

    BLACK_DHIDE_VAMBRACES("Black d'hide vambraces", ItemID.DRAGON_LEATHER_BLACK, ItemID.BLACK_DRAGON_VAMBRACES, 79, '2'),
    BLACK_DHIDE_CHAPS("Black d'hide chaps", ItemID.DRAGON_LEATHER_BLACK, ItemID.BLACK_DRAGONHIDE_CHAPS, 82, '3'),
    BLACK_DHIDE_BODY("Black d'hide body", ItemID.DRAGON_LEATHER_BLACK, ItemID.BLACK_DRAGONHIDE_BODY, 84, '1');

    /** Product name as shown in the make-X interface. */
    private final String name;
    /** The dragon leather material item id (green/blue/red/black). */
    private final int leatherId;
    /** The finished armour piece item id (the product). */
    private final int itemId;
    private final int levelRequired;
    /** The number key pressed to select this piece in the make-X dialog (body=1, vambraces=2, chaps=3). */
    private final char menuEntry;

    /** Dragon leather pieces consumed per craft: body = 3, chaps = 2, everything else = 1. */
    public int getLeatherPerCraft() {
        if (name.contains("body")) {
            return 3;
        }
        if (name.contains("chaps")) {
            return 2;
        }
        return 1;
    }

    @Override
    public String toString() {
        return name;
    }
}
