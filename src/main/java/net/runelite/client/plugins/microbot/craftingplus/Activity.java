package net.runelite.client.plugins.microbot.craftingplus;

/**
 * Which crafting activity AutoCraftingPlus performs. Furnace jewellery and the other base
 * activities are added in later versions.
 */
public enum Activity {
    LEATHER("Leather"),
    GEM_CUTTING("Gem cutting"),
    JEWELLERY("Furnace jewellery"),
    AMETHYST("Amethyst cutting"),
    STRINGING("Amulet stringing");

    private final String label;

    Activity(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
