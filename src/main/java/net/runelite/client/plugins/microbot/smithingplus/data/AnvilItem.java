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
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Item list + child IDs: forked verbatim from upstream
 *       {@code chsami/Microbot-Hub/src/main/java/net/runelite/client/plugins/microbot/varrockanvil/enums/AnvilItem.java}
 *       (last verified version 1.0.3 as of 2026-05-14).</li>
 *   <li>Cross-reference: <a href="https://oldschool.runescape.wiki/w/Smithing/Smithing_tables">OSRS Wiki — Smithing tables</a>
 *       (level requirements per item per bar tier, NOT captured here yet; deferred to v0.2.0).</li>
 *   <li>{@code requiredBars} counts: <a href="https://oldschool.runescape.wiki/w/Anvil">OSRS Wiki — Anvil</a>
 *       and per-item wiki pages.</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-14 (Pilot #3 v0.1.0):</b> Forked from {@code varrockanvil/enums/AnvilItem.java}
 *       v1.0.3. 26 items, child IDs and bar counts copied verbatim (dropped MOLTEN_GLASS-style
 *       carryover; trimmed to anvil-only items). Levels NOT included (deferred to v0.2.0; bot
 *       trusts user's Bar+Item combo for v0.1.0).</li>
 *   <li><b>2026-05-14 audit:</b> {@code tools/wiki-audit-anvil-items.ps1} hits the OSRS Wiki
 *       Smithing page. 16 of 26 items confirmed present. The 10 misses (2-hand sword, Claws,
 *       Nails, Oil lamp, Dart tips, Arrowtips, Knives, Bronze wire, Bullseye lamp, Bolts (unf))
 *       are not on the main Smithing summary page; they live on item-specific wiki pages. The
 *       hard data (childId + requiredBars) is forked from VarrockAnvil, which has been live in
 *       production for ages, so we trust those entries. v0.2.0 audit could fetch each
 *       item-specific page to close the gap.</li>
 * </ul>
 *
 * <h2>Known issues found in live testing (track for v0.2.0)</h2>
 * <ul>
 *   <li><b>CLAWS is members-only.</b> Bronze claws (and all higher-tier claws) require completion of
 *       the Cabin Fever members quest. On F2P, claws don't appear in the anvil smithing widget at
 *       all -- selecting AnvilItem.CLAWS as the Item in config will cause the bot to stall on a
 *       "click no-op" because the widget slot doesn't exist. Documented 2026-05-15 from live F2P
 *       smithing test on xitzpjmarz. v0.2.0 should add a {@code membersOnly} flag to AnvilItem
 *       and either grey it out in the dropdown on F2P or refuse to start.</li>
 *   <li><b>Possible other members-only items.</b> Need to F2P-audit the remaining 25 items. Quick
 *       suspect list: ARROWTIPS, DART_TIPS may be F2P; OIL_LAMP / BULLSEYE_LAMP / IRON_SPIT /
 *       BRONZE_WIRE likely F2P (vendor staples); STEEL_STUDS, BOLTS_UNF need verification.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li><b>No smithing-level table.</b> See audit-log note above. Mitigation: deliberate picks
 *       (Bronze Dagger at Smithing 1 always works); v0.2.0 closes the gap.</li>
 *   <li><b>Widget child IDs may drift.</b> If a future microbot/RuneLite version reshuffles the
 *       smithing widget (container 312), every item breaks at once. Mitigation: cite VarrockAnvil
 *       v1.0.3 as last-known-good above. v0.4.0 could add runtime widget-tree verification.</li>
 *   <li><b>Some entries are bar-tier-specific aliases.</b> {@code BRONZE_WIRE} only exists at the
 *       bronze tier; {@code IRON_SPIT} only at iron. Mixing them with the wrong bar tier picks a
 *       different item in that child slot (e.g. clicking child 32 with steel bars makes Studs,
 *       not Iron Spit). Documented per-entry below.</li>
 * </ul>
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
    BRONZE_WIRE("Bronze wire (bronze only) / Iron spit (iron) / Studs (steel)", 32, 1),
    BULLSEYE_LAMP("Bullseye lamp (steel only)", 28, 1),
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
     * v0.2.0: members-only filter. Bronze claws (and all higher-tier claws) require completion of
     * the Cabin Fever members quest, so the smithing widget doesn't show that slot on F2P. Picking
     * a members-only item on F2P would stall the bot. Pre-flight check uses this to refuse-start.
     *
     * <p>For now this is a static-method check (we don't want to mutate every enum constructor).
     * If more members-only items surface, extend this method.
     */
    public static boolean isMembersOnly(AnvilItem item) {
        if (item == null) return false;
        switch (item) {
            // Wiki-verified members-only anvil products (2026-05-31 audit): each confirmed via
            // its item-page infobox "Members: Yes" and the Smithing#Anvil member icon. An F2P
            // account cannot smith these (the widget slot is absent), so the Script's pre-flight
            // refuses to start rather than stalling on a no-op click.
            case CLAWS:
            case DART_TIPS:
            case ARROWTIPS:
            case KNIVES:
            case NAILS:
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
     * that tier. Wiki-verified table (2026-05-31 audit), indexed Bronze/Iron/Steel/Mithril/Adamant/
     * Rune. Drives the Script's level pre-flight and progressive mode. The Script's stall-detection
     * (v0.5.8) stays as a reactive backstop for any residual table error or a drifted widget id.
     */
    public int getRequiredLevel(Bars bar) {
        if (bar == null) return -1;
        int[] levels = levelsByBar(this);
        int idx = bar.ordinal();
        return (idx >= 0 && idx < levels.length) ? levels[idx] : -1;
    }

    /** True if this item is smithable at the given bar tier at all. */
    public boolean isMakeableAt(Bars bar) {
        return getRequiredLevel(bar) >= 0;
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
