package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Smithable items at an anvil. Each item carries the in-game widget child ID and the number of
 * bars consumed per craft.
 *
 * <p>The anvil interaction is widget-driven: clicking an anvil with a hammer + bar in inventory
 * opens the smithing widget (container ID {@code 312}). Each item occupies a child slot; the
 * {@link #getChildId()} maps the item to that slot. Clicking child[childId] with the active bar
 * tier (Bronze/Iron/Steel/etc.) selected smiths that item.
 *
 * <p>Item list, child IDs and bar counts are forked from the upstream {@code varrockanvil} plugin;
 * level requirements and members flags are cross-referenced against the
 * <a href="https://oldschool.runescape.wiki/w/Anvil">OSRS Wiki Anvil page</a>.
 *
 * <p>Some entries are bar-tier-specific aliases: {@code BRONZE_WIRE} shares its child slot with
 * Iron spit and Steel studs, so picking the wrong bar tier produces a different item in that slot.
 */
@Getter
@RequiredArgsConstructor
public enum AnvilItem {
    DAGGER("Dagger", 9, 1),
    SWORD("Sword", 10, 1),
    SCIMITAR("Scimitar", 11, 2),
    LONG_SWORD("Long sword", 12, 2),
    TWO_HAND_SWORD("2-hand sword", 13, 3),
    AXE("Axe", 14, 1),
    MACE("Mace", 15, 1),
    WARHAMMER("Warhammer", 16, 3),
    BATTLE_AXE("Battle axe", 17, 3),
    CLAWS("Claws", 18, 2),
    CHAIN_BODY("Chain body", 19, 3),
    PLATE_LEGS("Plate legs", 20, 3),
    PLATE_SKIRT("Plate skirt", 21, 3),
    PLATE_BODY("Plate body", 22, 5),
    NAILS("Nails", 23, 1),
    MEDIUM_HELM("Medium helm", 24, 1),
    FULL_HELM("Full helm", 25, 2),
    SQUARE_SHIELD("Square shield", 26, 2),
    KITE_SHIELD("Kite shield", 27, 3),
    OIL_LAMP("Oil lamp (bronze only)", 28, 1),
    DART_TIPS("Dart tips", 29, 1),
    ARROWTIPS("Arrowtips", 30, 1),
    KNIVES("Knives", 31, 1),
    BRONZE_WIRE("Wire / spit / studs", 32, 1),
    BULLSEYE_LAMP("Bullseye lamp (steel)", 28, 1),
    BOLTS("Bolts (unf)", 34, 1);

    private final String itemName;
    private final int childId;
    private final int requiredBars;

    public String getName() {
        return itemName;
    }

    @Override
    public String toString() {
        return itemName;
    }

    /**
     * The product's in-game name minus the bar-tier prefix (e.g. "dagger" -> "Bronze dagger",
     * "med helm" -> "Bronze med helm"). Combined with {@link Bars#getProductPrefix()} to look up
     * the finished item's GE price for the overlay GP/hr line. Returns null for items whose
     * product name does not follow the simple "Tier base" pattern (wire/spit/studs, lamps, bolts,
     * dart tips, arrowtips, nails, knives); the overlay then shows GP/hr 0 for those rather than
     * pricing the wrong item.
     */
    public String getProductBaseName() {
        switch (this) {
            case DAGGER:         return "dagger";
            case SWORD:          return "sword";
            case SCIMITAR:       return "scimitar";
            case LONG_SWORD:     return "longsword";
            case TWO_HAND_SWORD: return "2h sword";
            case AXE:            return "axe";
            case MACE:           return "mace";
            case WARHAMMER:      return "warhammer";
            case BATTLE_AXE:     return "battleaxe";
            case CLAWS:          return "claws";
            case CHAIN_BODY:     return "chainbody";
            case PLATE_LEGS:     return "platelegs";
            case PLATE_SKIRT:    return "plateskirt";
            case PLATE_BODY:     return "platebody";
            case MEDIUM_HELM:    return "med helm";
            case FULL_HELM:      return "full helm";
            case SQUARE_SHIELD:  return "sq shield";
            case KITE_SHIELD:    return "kiteshield";
            // Non-standard product names (multi-output, tier-locked, or unusual naming) - priced 0.
            default:             return null;
        }
    }

    /**
     * Members-only filter. Bronze claws (and all higher-tier claws) require completion of the Cabin
     * Fever members quest, so the smithing widget doesn't show that slot on F2P. Picking a
     * members-only item on F2P would stall the bot, so the pre-flight check uses this to
     * refuse-start. Extend this method if more members-only items surface.
     */
    public static boolean isMembersOnly(AnvilItem item) {
        if (item == null) return false;
        switch (item) {
            // Members-only anvil products: an F2P account cannot smith these (the widget slot is
            // absent), so the Script's pre-flight refuses to start rather than stalling on a no-op
            // click. NAILS, DART_TIPS, ARROWTIPS and KNIVES are F2P-smithable in OSRS and are
            // deliberately not listed here.
            case CLAWS:
            case OIL_LAMP:
            case BULLSEYE_LAMP:
            case BRONZE_WIRE: // shared slot also covers Iron spit / Steel studs, both members
            case BOLTS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Smithing level required to smith this item at the given bar tier, or -1 if not makeable at
     * that tier. Indexed Bronze/Iron/Steel/Mithril/Adamant/Rune. Drives the Script's level
     * pre-flight and progressive mode. The Script's stall-detection stays as a reactive backstop
     * for any residual table error or a drifted widget id.
     */
    public int getRequiredLevel(Bars bar) {
        if (bar == null) return -1;
        int[] levels = levelsByBar(this);
        int idx = bar.ordinal();
        return (idx >= 0 && idx < levels.length) ? levels[idx] : -1;
    }

    // Bronze, Iron, Steel, Mithril, Adamant, Rune. -1 = not makeable at that tier.
    private static int[] levelsByBar(AnvilItem item) {
        switch (item) {
            case DAGGER:          return new int[]{ 1, 15, 30, 50, 70, 85};
            case SWORD:           return new int[]{ 4, 19, 34, 54, 74, 89};
            case SCIMITAR:        return new int[]{ 5, 20, 35, 55, 75, 90};
            case LONG_SWORD:      return new int[]{ 6, 21, 36, 56, 76, 91};
            case TWO_HAND_SWORD:  return new int[]{14, 29, 44, 64, 84, 99};
            case AXE:             return new int[]{ 1, 16, 31, 51, 71, 86};
            case MACE:            return new int[]{ 2, 17, 32, 52, 72, 87};
            case WARHAMMER:       return new int[]{ 9, 24, 39, 59, 79, 94};
            case BATTLE_AXE:      return new int[]{10, 25, 40, 60, 80, 95};
            case CLAWS:           return new int[]{13, 28, 43, 63, 83, 98};
            case CHAIN_BODY:      return new int[]{11, 26, 41, 61, 81, 96};
            case PLATE_LEGS:      return new int[]{16, 31, 46, 66, 86, 99};
            case PLATE_SKIRT:     return new int[]{16, 31, 46, 66, 86, 99};
            case PLATE_BODY:      return new int[]{18, 33, 48, 68, 88, 99};
            case NAILS:           return new int[]{ 4, 19, 34, 54, 74, 89};
            case MEDIUM_HELM:     return new int[]{ 3, 18, 33, 53, 73, 88};
            case FULL_HELM:       return new int[]{ 7, 22, 37, 57, 77, 92};
            case SQUARE_SHIELD:   return new int[]{ 8, 23, 38, 58, 78, 93};
            case KITE_SHIELD:     return new int[]{12, 27, 42, 62, 82, 97};
            case DART_TIPS:       return new int[]{ 4, 19, 34, 54, 74, 89};
            case ARROWTIPS:       return new int[]{ 5, 20, 35, 55, 75, 90};
            case KNIVES:          return new int[]{ 7, 22, 37, 57, 77, 92};
            case BOLTS:           return new int[]{ 3, 18, 33, 53, 73, 88};
            // Tier-locked / shared-slot members items (see isMembersOnly). The -1 tiers fall
            // through the level gate (treated as "unknown", not a hard refuse) and rely on
            // stall-detection if a wrong bar is picked.
            case OIL_LAMP:        return new int[]{-1, 26, -1, -1, -1, -1}; // oil lantern frame: iron, 26
            case BRONZE_WIRE:     return new int[]{ 4, 17, 36, -1, -1, -1}; // wire(4)/iron spit(17)/steel studs(36)
            case BULLSEYE_LAMP:   return new int[]{-1, -1, 49, -1, -1, -1}; // bullseye frame: steel, 49
            default:              return new int[]{};
        }
    }

    /**
     * Progressive-mode picker: the best item to smith at the given bar tier for a player of the
     * given Smithing level. "Best" = most bars per craft (most XP per craft, fewest interface
     * clicks), tie-broken by highest level requirement. Skips members-only items unless isMember.
     * Returns null if nothing is makeable (e.g. a bar tier too high for any item at this level).
     */
    public static AnvilItem bestForLevel(Bars bar, int smithingLevel, boolean isMember) {
        AnvilItem best = null;
        for (AnvilItem item : values()) {
            if (!isMember && isMembersOnly(item)) continue;
            int req = item.getRequiredLevel(bar);
            if (req < 0 || req > smithingLevel) continue;
            if (best == null
                    || item.getRequiredBars() > best.getRequiredBars()
                    || (item.getRequiredBars() == best.getRequiredBars()
                        && req > best.getRequiredLevel(bar))) {
                best = item;
            }
        }
        return best;
    }
}
