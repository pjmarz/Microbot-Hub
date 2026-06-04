package net.runelite.client.plugins.microbot.craftingplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ItemID;

/**
 * Furnace jewellery products. Copied from the crafting/jewelry base (craftingplus is its own source
 * set), with the enchant-spell field dropped because AutoCraftingPlus does not enchant. Each entry
 * carries the gold/silver bar, the gem set into it (or {@link Gem#NONE}), the mould tool, and the
 * Crafting level required.
 */
@Getter
@RequiredArgsConstructor
public enum Jewelry {

    GOLD_RING("gold ring", ItemID.GOLD_RING, Gem.NONE, ItemID.RING_MOULD, JewelryType.GOLD, 5),
    GOLD_NECKLACE("gold necklace", ItemID.GOLD_NECKLACE, Gem.NONE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 6),
    GOLD_BRACELET("gold bracelet", ItemID.GOLD_BRACELET, Gem.NONE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 7),
    GOLD_AMULET("gold amulet", ItemID.GOLD_AMULET_U, Gem.NONE, ItemID.AMULET_MOULD, JewelryType.GOLD, 8),
    TIARA("tiara", ItemID.TIARA, Gem.NONE, ItemID.TIARA_MOULD, JewelryType.SILVER, 23),
    UNSTRUNG_SYMBOL("holy symbol", ItemID.UNSTRUNG_SYMBOL, Gem.NONE, ItemID.HOLY_MOULD, JewelryType.SILVER, 16),
    OPAL_RING("opal ring", ItemID.OPAL_RING, Gem.OPAL, ItemID.RING_MOULD, JewelryType.SILVER, 1),
    OPAL_NECKLACE("opal necklace", ItemID.OPAL_NECKLACE, Gem.OPAL, ItemID.NECKLACE_MOULD, JewelryType.SILVER, 16),
    OPAL_BRACELET("opal bracelet", ItemID.OPAL_BRACELET, Gem.OPAL, ItemID.BRACELET_MOULD, JewelryType.SILVER, 22),
    OPAL_AMULET("opal amulet", ItemID.OPAL_AMULET_U, Gem.OPAL, ItemID.AMULET_MOULD, JewelryType.SILVER, 27),
    JADE_RING("jade ring", ItemID.JADE_RING, Gem.JADE, ItemID.RING_MOULD, JewelryType.SILVER, 13),
    JADE_NECKLACE("jade necklace", ItemID.JADE_NECKLACE, Gem.JADE, ItemID.NECKLACE_MOULD, JewelryType.SILVER, 25),
    JADE_BRACELET("jade bracelet", ItemID.JADE_BRACELET, Gem.JADE, ItemID.BRACELET_MOULD, JewelryType.SILVER, 29),
    JADE_AMULET("jade amulet", ItemID.JADE_AMULET_U, Gem.JADE, ItemID.AMULET_MOULD, JewelryType.SILVER, 34),
    TOPAZ_RING("topaz ring", ItemID.TOPAZ_RING, Gem.RED_TOPAZ, ItemID.RING_MOULD, JewelryType.SILVER, 16),
    TOPAZ_NECKLACE("topaz necklace", ItemID.TOPAZ_NECKLACE, Gem.RED_TOPAZ, ItemID.NECKLACE_MOULD, JewelryType.SILVER, 32),
    TOPAZ_BRACELET("topaz bracelet", ItemID.TOPAZ_BRACELET, Gem.RED_TOPAZ, ItemID.BRACELET_MOULD, JewelryType.SILVER, 38),
    TOPAZ_AMULET("topaz amulet", ItemID.TOPAZ_AMULET_U, Gem.RED_TOPAZ, ItemID.AMULET_MOULD, JewelryType.SILVER, 45),
    SAPPHIRE_RING("sapphire ring", ItemID.SAPPHIRE_RING, Gem.SAPPHIRE, ItemID.RING_MOULD, JewelryType.GOLD, 20),
    SAPPHIRE_NECKLACE("sapphire necklace", ItemID.SAPPHIRE_NECKLACE, Gem.SAPPHIRE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 22),
    SAPPHIRE_BRACELET("sapphire bracelet", ItemID.SAPPHIRE_BRACELET_11072, Gem.SAPPHIRE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 23),
    SAPPHIRE_AMULET("sapphire amulet", ItemID.SAPPHIRE_AMULET_U, Gem.SAPPHIRE, ItemID.AMULET_MOULD, JewelryType.GOLD, 24),
    EMERALD_RING("emerald ring", ItemID.EMERALD_RING, Gem.EMERALD, ItemID.RING_MOULD, JewelryType.GOLD, 27),
    EMERALD_NECKLACE("emerald necklace", ItemID.EMERALD_NECKLACE, Gem.EMERALD, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 29),
    EMERALD_BRACELET("emerald bracelet", ItemID.EMERALD_BRACELET, Gem.EMERALD, ItemID.BRACELET_MOULD, JewelryType.GOLD, 30),
    EMERALD_AMULET("emerald amulet", ItemID.EMERALD_AMULET_U, Gem.EMERALD, ItemID.AMULET_MOULD, JewelryType.GOLD, 31),
    RUBY_RING("ruby ring", ItemID.RUBY_RING, Gem.RUBY, ItemID.RING_MOULD, JewelryType.GOLD, 34),
    RUBY_NECKLACE("ruby necklace", ItemID.RUBY_NECKLACE, Gem.RUBY, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 40),
    RUBY_BRACELET("ruby bracelet", ItemID.RUBY_BRACELET, Gem.RUBY, ItemID.BRACELET_MOULD, JewelryType.GOLD, 42),
    RUBY_AMULET("ruby amulet", ItemID.RUBY_AMULET_U, Gem.RUBY, ItemID.AMULET_MOULD, JewelryType.GOLD, 50),
    DIAMOND_RING("diamond ring", ItemID.DIAMOND_RING, Gem.DIAMOND, ItemID.RING_MOULD, JewelryType.GOLD, 43),
    DIAMOND_NECKLACE("diamond necklace", ItemID.DIAMOND_NECKLACE, Gem.DIAMOND, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 56),
    DIAMOND_BRACELET("diamond bracelet", ItemID.DIAMOND_BRACELET, Gem.DIAMOND, ItemID.BRACELET_MOULD, JewelryType.GOLD, 58),
    DIAMOND_AMULET("diamond amulet", ItemID.DIAMOND_AMULET_U, Gem.DIAMOND, ItemID.AMULET_MOULD, JewelryType.GOLD, 70),
    DRAGONSTONE_RING("dragonstone ring", ItemID.DRAGONSTONE_RING, Gem.DRAGONSTONE, ItemID.RING_MOULD, JewelryType.GOLD, 55),
    DRAGON_NECKLACE("dragon necklace", ItemID.DRAGON_NECKLACE, Gem.DRAGONSTONE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 72),
    DRAGONSTONE_BRACELET("dragon bracelet", ItemID.DRAGONSTONE_BRACELET, Gem.DRAGONSTONE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 74),
    DRAGONSTONE_AMULET("dragonstone amulet", ItemID.DRAGONSTONE_AMULET_U, Gem.DRAGONSTONE, ItemID.AMULET_MOULD, JewelryType.GOLD, 80),
    ONYX_RING("onyx ring", ItemID.ONYX_RING, Gem.ONYX, ItemID.RING_MOULD, JewelryType.GOLD, 67),
    ONYX_NECKLACE("onyx necklace", ItemID.ONYX_NECKLACE, Gem.ONYX, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 82),
    ONYX_BRACELET("onyx bracelet", ItemID.ONYX_BRACELET, Gem.ONYX, ItemID.BRACELET_MOULD, JewelryType.GOLD, 84),
    ONYX_AMULET("onyx amulet", ItemID.ONYX_AMULET_U, Gem.ONYX, ItemID.AMULET_MOULD, JewelryType.GOLD, 90),
    ZENYTE_RING("zenyte ring", ItemID.ZENYTE_RING, Gem.ZENYTE, ItemID.RING_MOULD, JewelryType.GOLD, 89),
    ZENYTE_NECKLACE("zenyte necklace", ItemID.ZENYTE_NECKLACE, Gem.ZENYTE, ItemID.NECKLACE_MOULD, JewelryType.GOLD, 92),
    ZENYTE_BRACELET("zenyte bracelet", ItemID.ZENYTE_BRACELET, Gem.ZENYTE, ItemID.BRACELET_MOULD, JewelryType.GOLD, 95),
    ZENYTE_AMULET("zenyte amulet", ItemID.ZENYTE_AMULET_U, Gem.ZENYTE, ItemID.AMULET_MOULD, JewelryType.GOLD, 98);

    private final String itemName;
    private final int itemID;
    private final Gem gem;
    private final int toolItemID;
    private final JewelryType jewelryType;
    private final int levelRequired;

    @Override
    public String toString() {
        return itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
    }
}
