package net.runelite.client.plugins.microbot.eventdismissplus.data;

/**
 * Catalog of random event NPC types that EventDismissPlus engages with explicitly.
 *
 * <p>We don't enumerate the full ~25 random event types -- we only catalog the ones we
 * engage with high-value actions for. Everything else falls through to the universal
 * {@code npc.click("Dismiss")} action confirmed available on all random event NPCs.
 *
 * <p>NPC names match the in-game display name as exposed by
 * {@code Rs2NpcModel.getName()}. Some events have ambiguous wiki spellings (Bee keeper
 * vs Beekeeper); we include both as fallbacks in the lookup.
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Main page: <a href="https://oldschool.runescape.wiki/w/Random_events">OSRS Wiki - Random events</a></li>
 *   <li>Per-event NPC name + reward: each event's individual wiki page</li>
 *   <li>Existing reference impl: {@code eventdismiss/DismissNpcEvent.java} uses
 *       {@code Rs2Npc.getRandomEventNPC()} which returns models with {@code .getName()}
 *       matching these strings.</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-22 (v0.1.0):</b> Initial catalog of 10 engagement targets. Names cited
 *       from OSRS Wiki research (see plan file). Strange Plant deliberately excluded --
 *       it's a GameObject, not an NPC, handled separately by StrangePlantHandler.
 *       Prison Pete deferred to v0.2.0 (balloon-animal minigame, more than a dialogue).</li>
 *   <li><b>2026-05-23 (v0.2.0):</b> Catalog-only entries added for the Mime random event NPCs
 *       (Niles / Miles / Giles). Pete encountered Niles during soak; default-dismiss handled
 *       it cleanly. Full engagement (mimic 5 emotes) requires animation recognition; deferred
 *       to v0.3.0 with the OCR-based events. Adding the names here documents we know about
 *       them without changing the engagement switch behavior (they're not in {@code
 *       shouldEngage} switch, so {@code default: return false} dismisses).</li>
 *   <li><b>2026-05-23 (v0.2.0):</b> Prison Pete + Freaky Forester still deferred. Both
 *       require multi-step engagement (balloon pop / chicken-catching mini-quest) that's
 *       higher complexity than v0.2.0's "light polish" theme. Bundled into v0.3.0 with OCR.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li>Beekeeper NPC name not explicitly confirmed by wiki research (disambiguation page).
 *       We match both "Bee keeper" and "Beekeeper" via two enum entries. Smoke test should
 *       resolve which is the actual in-game name; the unused entry can be dropped in v0.1.1.</li>
 *   <li>Dr. Jekyll wiki page shows "Dr Jekyll" (no period); we match both with/without.</li>
 *   <li>Wiki pages don't quote exact dialogue text -- click-by-click flow may need adjustment
 *       on first smoke test, especially the Genie skill picker widget.</li>
 * </ul>
 */
public enum RandomEventType {
    GENIE("Genie", true),
    SANDWICH_LADY("Sandwich lady", false),
    DRUNKEN_DWARF("Drunken Dwarf", false),
    MYSTERIOUS_OLD_MAN("Mysterious Old Man", false),
    BEEKEEPER_TWO_WORDS("Bee keeper", true),        // wiki disambig spelling
    BEEKEEPER_ONE_WORD("Beekeeper", true),          // alt spelling fallback
    COUNT_CHECK("Count Check", true),
    FROG_PRINCE("Frog Prince", false),              // appears for female players
    FROG_PRINCESS("Frog Princess", false),          // appears for male players
    RICK_TURPENTINE("Rick Turpentine", false),
    DR_JEKYLL_NO_PERIOD("Dr Jekyll", false),
    DR_JEKYLL_PERIOD("Dr. Jekyll", false),
    // v0.2.0: Mime event NPCs. One of these three fires per Mime event;
    // player must mimic 5 emotes the leader performs. Catalog-only -- the
    // engagement switch in RandomEventNpcHandler doesn't include them, so
    // default-dismiss handles them cleanly. Full engagement is v0.3.0+ scope
    // (requires animation recognition for the emote-mimic loop).
    MIME_NILES("Niles", false),
    MIME_MILES("Miles", false),
    MIME_GILES("Giles", false);

    private final String npcName;
    private final boolean givesLamp;

    RandomEventType(String npcName, boolean givesLamp) {
        this.npcName = npcName;
        this.givesLamp = givesLamp;
    }

    public String getNpcName() {
        return npcName;
    }

    /**
     * True when this event grants an XP lamp that requires the skill-picker widget flow
     * (Genie / Beekeeper / Count Check). False for events that grant items directly.
     */
    public boolean givesLamp() {
        return givesLamp;
    }

    /**
     * Looks up the enum value matching a given NPC name (case-insensitive). Returns null if
     * the name doesn't correspond to any engagement target -- the handler should dismiss in
     * that case.
     */
    public static RandomEventType fromNpcName(String name) {
        if (name == null) return null;
        for (RandomEventType type : values()) {
            if (type.npcName.equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}
