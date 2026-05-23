# Microbot Plus Patterns — Reference

Patterns extracted from existing Hub plugins (AutoMining, AutoWoodcutting, AutoFishing, AutoCooking, AIO Fighter) and from Pilot #1 (AutoMiningPlus). Treat this as a cheat sheet — for deep details, follow the links to canonical docs.

---

## The invariant skeleton

Every Hub plugin has the same four-file frame plus a state enum and an optional `data/` folder:

```
<skillplus>/
├── Auto<Skill>PlusPlugin.java   @PluginDescriptor + Guice + startUp/shutDown
├── Auto<Skill>PlusConfig.java   @ConfigGroup + @ConfigSection + @ConfigItem entries
├── Auto<Skill>PlusScript.java   extends Script (or StateMachineScript), the loop
├── Auto<Skill>PlusOverlay.java  OverlayPanel, status text
├── State.java                   the enum, package-private
└── data/                        skill-specific lookup tables (optional)
```

### Plugin descriptor pattern

```java
@PluginDescriptor(
    name = PluginDescriptor.Mocrosoft + "Auto <Skill> Plus",
    description = "...",
    tags = {"<skill>", "microbot", "plus"},
    version = AutoXPlusPlugin.version,         // String constant elsewhere in the class
    minClientVersion = "2.0.13",               // lowest supported client
    enabledByDefault = PluginConstants.DEFAULT_ENABLED,
    isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class AutoXPlusPlugin extends Plugin {
    public static final String version = "0.1.0";
    @Inject AutoXPlusConfig config;
    @Provides AutoXPlusConfig provideConfig(ConfigManager cm) { return cm.getConfig(AutoXPlusConfig.class); }
    @Inject OverlayManager overlayManager;
    @Inject AutoXPlusOverlay overlay;
    @Inject AutoXPlusScript script;

    @Override protected void startUp() { overlayManager.add(overlay); script.run(config); }
    @Override protected void shutDown() { script.shutdown(); overlayManager.remove(overlay); }
}
```

### Run() lifecycle pattern

```java
public boolean run(AutoXPlusConfig config) {
    // Reset state — critical, static fields persist across plugin toggle cycles
    initialPlayerLocation = null;

    Rs2Antiban.resetAntibanSettings();
    Rs2Antiban.antibanSetupTemplates.apply<Skill>Setup();
    Rs2AntibanSettings.actionCooldownChance = 0.1;

    mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
        try {
            if (!super.run()) return;
            if (!Microbot.isLoggedIn()) return;
            if (Rs2AntibanSettings.actionCooldownActive) return;
            if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

            // Anchor home tile once
            if (initialPlayerLocation == null) {
                initialPlayerLocation = Rs2Player.getWorldLocation();
                if (initialPlayerLocation == null) return;
            }

            switch (state) {
                case ... :
                    ...
                    Rs2Antiban.actionCooldown();
                    Rs2Antiban.takeMicroBreakByChance();
                    break;
                case RESETTING:
                    ...
                    state = State.IDLE;
                    break;
            }
        } catch (Exception ex) { Microbot.log(ex.getMessage()); }
    }, 0, 100, TimeUnit.MILLISECONDS);
    return true;
}
```

## Variant axes by skill family

The skeleton stays the same. What you fill in differs:

| Axis | Gathering | Stationary | Travel-heavy |
|---|---|---|---|
| Primary entity | `Rs2GameObject` (rock, tree, fishing spot) | `Rs2Inventory.combine` / `Rs2Widget` (make-X menu) | `Rs2Walker` waypoints |
| Wait for action | `Rs2Player.waitForXpDrop(Skill.X, true)` | `Rs2Player.waitForXpDrop` or `sleepUntil(Rs2Inventory::hasNoEmptySlots)` | `sleepUntil(player at next waypoint)` |
| Anchor | First tile the player stood on, OR a configured location | Bank tile / range tile | The waypoint sequence itself |
| State count | 2 (IDLE, RESETTING) | 3-4 (BANKING, WALKING, WORKING, RESETTING) | many — use `StateMachineScript` |
| Antiban template | `applyMiningSetup`, `applyWoodcuttingSetup`, `applyFishingSetup`, `applyHunterSetup` | `applyCookingSetup`, `applySmithingSetup`, `applyCraftingSetup`, `applyFletchingSetup` | depends on the dominant activity |
| Banking pattern | `Rs2Bank.walkToBankAndUseBank()` → deposit by name → return | Same, or stationary at a bank tile | Skip — usually no banking |

## Banking patterns

```java
// Standard "walk, bank, walk back" — what AutoMining and AutoMiningPlus do:
if (!Rs2Bank.isOpen()) {
    if (!Rs2Bank.walkToBankAndUseBank()) return;
    return;
}
Rs2Bank.depositAll(item -> itemNames.contains(item.getName().toLowerCase()));
if (!Rs2Bank.closeBank()) return;
Rs2Walker.walkTo(initialPlayerLocation, config.distanceToStray());
```

### Deposit-filter auto-augment (the coal gotcha)

The default `itemsToBank = "ore"` filter matches Tin ore, Iron ore, etc. via substring. But it
misses Coal, Clay, Basalt — those items have no "ore" suffix. Result: deposit predicate
matches zero items, bot closes the bank empty, walks back to the mine, inventory still full,
flips back to RESETTING, oscillates forever.

Fix: at deposit time, always append the active entity's first word to the filter list:

```java
List<String> filterNames = new ArrayList<>(itemNamesFromConfig);
if (activeRock != null && activeRock.getName() != null) {
    String firstWord = activeRock.getName().split("\\s+")[0].toLowerCase();
    if (!firstWord.isEmpty() && !filterNames.contains(firstWord)) {
        filterNames.add(firstWord);
    }
}
Rs2Bank.depositAll(i -> i.getName() != null
        && filterNames.stream().anyMatch(item -> i.getName().toLowerCase().contains(item)));
```

`Rocks.COAL.getName()` returns "coal rocks" — first word "coal" then substring-matches the
inventory item "Coal". Works for all rocks (Tin/Iron/Mithril/etc. all match via their first
word too). Same pattern for trees ("oak tree" → "oak" → "Oak logs"), fish, etc.

Belt-and-suspenders: also expand the default `itemsToBank` to include common item-name
fragments your skill can produce. See AutoMiningPlus v0.4.2 default: `"ore, uncut, coal, clay,
basalt, essence, ash, shard, geode, salt, limestone, granite, sandstone, amethyst, pay-dirt"`.

Deposit-box variant for sites that have one (e.g. gem mine underground):

```java
if (Rs2DepositBox.openDepositBox()) {
    Rs2DepositBox.depositAll();
    Rs2DepositBox.closeDepositBox();
}
```

Bank + reach for a specific item (e.g. clay bracelet):

```java
Rs2Bank.walkToBankAndUseBank();
Rs2Bank.depositAll();
if (Rs2Bank.hasItem(11074)) Rs2Bank.withdrawAndEquip(11074);
Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemNames, anchorPoint, 0, strayDistance);
```

## Run-loop control patterns (v0.5.0)

Shipped across all four Plus plugins as v0.5.0. AIO Fighter has analogous features (per-skill target levels, Pause button); Pete asked us to mirror them.

### Target level cleanup-then-shutdown

User sets a target level for the primary skill. When reached, run one bank-or-drop cleanup pass, then shutdown. Don't leave the player with a full inventory.

Add a `shutdownAfterCleanup` boolean field to the script. Reset in `run()`. In the main tick, after stopAfter* checks:

```java
if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
            Microbot.getClient().getRealSkillLevel(Skill.X)).orElse(startSkillLevel);
    if (currentLevel >= config.targetLevel()) {
        Microbot.log("Reached targetLevel. Cleanup pass before shutdown.");
        shutdownAfterCleanup = true;
        if (Rs2Inventory.isEmpty()) {
            super.shutdown();
            return;
        }
        state = State.RESETTING;
    }
}
```

In the RESETTING branch, after deposit/drop work completes and right before the state-flip-back to the working state:

```java
if (shutdownAfterCleanup) {
    Microbot.log("targetLevel cleanup complete. Shutting down.");
    super.shutdown();
    return;
}
state = State.WORKING;
```

The flag prevents the targetLevel check from re-firing while cleanup is in flight. The intercept ensures we shut down at the right moment — not mid-walk-back, not before the deposit, not after the working state has resumed.

For state machines with deposit-then-withdraw flow (Smelting, Smithing), insert the intercept right after the deposit step, before any withdraw work:

```java
depositByCsv(config);
sleepUntil(() -> !Rs2Inventory.isFull(), 3000);

if (shutdownAfterCleanup) {
    Rs2Bank.closeBank();
    super.shutdown();
    return;
}

// ... withdraw step happens here ...
```

### Overlay button pattern (critical: `hookMouseListener()` is required)

The Plus suite v0.5.0 originally shipped Pause as a config-panel checkbox. v0.5.1 refactored it to an overlay button (mirrors AIO Fighter's UX). That took 6 versions to get right because of a single missing API call.

`ButtonComponent` is a Microbot extension over RuneLite's standard overlay components. Its `setOnClick(Runnable)` only **stores** the lambda — it does NOT wire it into the mouse event dispatch. You must explicitly call `hookMouseListener()` from the parent Plugin's `startUp()` AFTER `overlayManager.add(overlay)`, and `unhookMouseListener()` in `shutDown()`.

```java
// In the Overlay constructor:
pauseButton = new ButtonComponent("Pause");
pauseButton.setPreferredSize(new Dimension(100, 25));
pauseButton.setParentOverlay(this);
pauseButton.setFont(FontManager.getRunescapeBoldFont());
pauseButton.setOnClick(() -> { /* your handler */ });

// In the Plugin's startUp():
overlayManager.add(overlay);
overlay.pauseButton.hookMouseListener();   // <-- the missing call

// In the Plugin's shutDown():
overlay.pauseButton.unhookMouseListener();
overlayManager.remove(overlay);
```

The `pauseButton` field must be **public final** on the overlay so the Plugin can reach it.

**Symptom when the call is missing**: button renders visually, but clicks pass straight through to the game canvas. Hover over the overlay and the RuneLite right-click menu shows the GAME tile's options ("Walk here") rather than overlay options. The `setOnClick` lambda never fires.

Reference implementations that have this correct: `AIOFighterPlugin.startUp()` (lines 143-144), `ClueSolverPlugin.startUp()`, `ExamplePlugin.startUp()`. The Plus suite mirrored this in v0.5.6 across all 4 plugins.

### Global pause via `Microbot.pauseAllScripts`

`Microbot.pauseAllScripts` is a global `AtomicBoolean`. Any plugin can toggle it. Every Plus plugin script reads it at the top of each tick and early-exits when true. The overlay button just flips this flag — click pause on any one Plus plugin's overlay and all enabled scripts pause together.

This is the standard Microbot pattern (AIO Fighter uses it). Don't create a per-plugin pause flag; defer to the global one.

### Pause without nuking session metrics

User toggles pause (via overlay button → `Microbot.pauseAllScripts`). The tick early-exits without doing any work or transitioning state. `startTimeMillis` / `startSkillXp` / `actionsCompleted` are NOT reset. Toggle off and the existing flow picks back up.

```java
mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
    try {
        if (!super.run()) return;
        if (!Microbot.isLoggedIn()) return;

        if (config.paused()) {
            Microbot.status = "[PAUSED]";  // overlay shows the prefix automatically
            return;
        }
        // ... rest of tick
    } catch (Exception ex) { Microbot.log(ex.getMessage()); }
}, 0, 100, TimeUnit.MILLISECONDS);
```

Runtime keeps ticking during pause (overlay's `runtimeMillis` keeps growing). XP/hr naturally trends down because no XP is gained. That's correct behavior — the pause is honest about elapsed time.

For an overlay that reads its own state enum (e.g. AutoWoodcuttingPlusOverlay reads `woodcuttingScriptState`) instead of `Microbot.status`, prefix in the overlay:

```java
String displayStatus = config.paused()
        ? "[PAUSED]"
        : plugin.getScript().getCurrentState().toString();
```

### Why both Stop-after and Target-level exist

They're different stop signals:

- `stopAfterMinutes` / `stopAfterXp` — runtime / XP-gained thresholds. Anti-detection pattern (defeats uniform-session-length signal).
- `targetLevel` — skill-progression target. "Get me to level X, then quit cleanly."

Whichever trips first wins. Both honor the same shutdown flow if you adapt the patterns above for stopAfter* too (most plugins haven't — they shutdown immediately on stopAfter, leaving the inventory full).

## Walking patterns

```java
// One-shot walk to a known WorldPoint
Rs2Walker.walkTo(targetPoint, tolerance);  // tolerance in tiles

// Long-distance with web walker (use this for cross-region travel)
// Note: cache nulls don't auto-walk in Queryable API — verify reachability first.

// Pre-flight reach check
if (!locationOption.canReach()) {
    // Either walk to it or fall back to something closer
}
```

## Antiban setup defaults (frozen per skill)

Each `apply<Skill>Setup()` template flips certain antiban knobs. You typically don't need to override them, but if you must:

```java
Rs2AntibanSettings.actionCooldownChance = 0.1;     // 10% chance per action
Rs2AntibanSettings.microBreakChance = 0.1;          // micro-break frequency
Rs2AntibanSettings.microBreakDurationLow = 3;       // minutes
Rs2AntibanSettings.microBreakDurationHigh = 15;     // minutes — long for "looks human"; lower for XP rate
Rs2AntibanSettings.takeMicroBreaks = true;          // master switch
Rs2AntibanSettings.usePlayStyle = true;             // play-style persona randomization
```

Power users (on throwaway accounts) sometimes set `microBreakDurationHigh = 3` or disable `takeMicroBreaks` entirely for XP-rate maxing at the cost of detection risk.

## Per-action calls

```java
Rs2Antiban.actionCooldown();            // probabilistic small pause after a successful action
Rs2Antiban.takeMicroBreakByChance();    // probabilistic medium break (minutes)
```

## Don't duplicate the Antiban panel in your plugin

The Microbot client ships an Antiban plugin (the duck-icon panel) that exposes every flag in `Rs2AntibanSettings` as a UI control, plus "Universal Antiban" mode to share one config across all scripts. **Resist the urge to add Plugin-level antiban configuration UI to a new Plus plugin** — you'd be duplicating the panel and creating two sources of truth.

Best practice for a Plus plugin: call the matching `apply<Skill>Setup()` template at `startUp()` (we already do this in the skeleton), expose at most a single boolean "Speed mode" config that does the equivalent of unchecking "Enabled" globally:

```java
if (config.speedMode()) {
    Rs2AntibanSettings.antibanEnabled = false;   // master switch — disables every per-action check
}
```

That's the entire body. Microbot's `actionCooldown()`, `takeMicroBreakByChance()`, `naturalMouseMovement()`, etc. all short-circuit when this flag is false. Anything fancier than this (e.g. fine-grained per-flag tuning) belongs in the user's Antiban panel, not in your plugin code.

AutoMiningPlus's `speedMode` (v0.1.9) is exactly this pattern. Use it as the reference.

## Configuration patterns

### Sections + items

```java
@ConfigGroup("<SkillName>Plus")  // MUST be unique per plugin
public interface AutoXPlusConfig extends Config {
    @ConfigSection(name = "General", position = 0)
    String generalSection = "general";

    @ConfigItem(
        keyName = "target",
        name = "Target",
        description = "...",
        position = 0,
        section = generalSection
    )
    default TargetEnum target() { return TargetEnum.DEFAULT; }
}
```

### Comma-separated string lists

Common idiom for "items to bank / keep":

```java
default String itemsToBank() { return "ore,gem"; }

// In script:
List<String> itemNames = Arrays.stream(config.itemsToBank().split(","))
    .map(String::trim).map(String::toLowerCase)
    .filter(s -> !s.isEmpty())
    .collect(Collectors.toList());
```

## Tribal knowledge (documented gaps in Microbot)

These are NOT in the canonical docs. Encode them in your plugin or open issues upstream.

### Instance coordinates

Tithe farm, gauntlet, theatre, and other instanced regions expose TWO coordinate planes:
- Overworld "logical" plane (used for the minimap)
- Instance template plane (returned by `Rs2Player.getWorldLocation()`)

Reverse-engineer detection from `TitheFarmingScript`, `GauntletPlugin`. The pattern is to check if the player's `getRegionID()` matches a known instance region constant.

### Quest gates

No first-class API. To gate a feature on a completed quest:
- Read a varbit: `Microbot.getVarbitValue(varbitId)` for quest progress vars
- Use `Rs2Player.getQuestState(Quest.X)` for canonical state (NOT_STARTED / IN_PROGRESS / FINISHED) — what `LocationOption.hasRequirements()` already uses

### Member-only worlds

No auto-fallback. Check `Rs2Player.isMember()` and either skip the feature or warn the user. Members-only `LocationOption` entries already encode `membersOnly = true`; your enum facade should respect that via `hasRequirements()`.

### Cache null safety

The Queryable API (`Microbot.getRs2NpcCache().query()`, etc.) does NOT auto-walk to distant targets. Null returns can silently mask "target out of range" — distinct from "target doesn't exist." When debugging, hit the Agent Server at `localhost:8081/objects` or `/npcs` to see what the client actually has cached.

### Static state leaks

Fields declared `static` in your script class persist across plugin enable/disable cycles. Always reset them in `run()` BEFORE scheduling the executor:

```java
public boolean run(Config config) {
    initialPlayerLocation = null;   // reset!
    activeTarget = null;            // reset!
    state = State.IDLE;             // reset!
    // ... then schedule
}
```

### ButtonComponent click silently no-op without `hookMouseListener()`

The Plus suite's v0.5.1 → v0.5.6 journey to make a Pause button clickable consumed 6 iterations because the symptom (button renders, clicks pass through) is identical to several unrelated bugs (children-list clear wiping the registry, race condition between overlay add and script start, etc.).

The fix turned out to be a single missing call: `pauseButton.hookMouseListener()` in the parent Plugin's `startUp()`. Without it, `setOnClick` is a no-op.

Encoded in PATTERNS.md "Overlay button pattern" section above. Surface this for any future pilot adding overlay buttons — it's not discoverable from the ButtonComponent's API surface (there's no IllegalStateException when you forget; clicks just silently pass through).

### Stale coordinate cascade

Discovered v0.4.0 in MiningPlus: `MINING_GUILD_FALADOR` in BankLocationOption had coord `(3013, 3355, 0)` — IDENTICAL to FALADOR_EAST. The entry had been seeded from someone's memory or a bad-paste and never re-verified. Picking "Mining Guild" silently sent the walker to Falador East's booth.

A second case landed during the same cycle: `AL_KHARID_ARENA` (Fadli's bank) had a wrong coord `(3315, 3242, 0)` that sent the walker to a NW residential building, nowhere near the arena. Pete caught it post-deploy via screenshot of the wrong landing. Wiki cross-check + live in-game verification corrected it to `(3383, 3269, 0)` at the actual deposit chest tile.

Once one stale coord surfaces, assume others exist. The fix isn't just to repair the one bug — it's to re-walk every entry in the affected enum on the next throwaway session and record verification in the data file's audit log header. The audit log is what makes future-you (or a contributor) trust the entry without re-walking.

Mitigation pattern:

1. Live-verify each new coord by toggling the plugin on at a known anchor (e.g. Lumbridge spawn) with the target entry configured. Confirm Rs2Walker lands within 3 tiles of the intended booth/object and the entity is clickable from there.
2. Mark verification in the audit log: `<b>YYYY-MM-DD (vX.Y.Z):</b> Coord verified live`.
3. For any new entry with a wiki-only coord (no in-game test yet), flag in the inline comment `// needs in-game verification on next session`.

Avoid: shipping coords pulled from "memory" — mine OR Claude's. Wiki page or live verification only. The `tools/wiki-audit-<skill>.ps1` scripts catch name-level discrepancies but cannot catch coord drift; live verification is the only safety net for coords.

### Action cooldown blocks the whole loop

`Rs2AntibanSettings.actionCooldownActive = true` causes the loop to return early at every tick. Effects:
- The bot literally cannot act during the cooldown
- Default cooldown durations are tuned conservatively for account longevity
- If you observe 30-45 sec idle gaps and your `waitForXpDrop` shouldn't have timed out, the action cooldown is the most likely culprit

### Mouse-position interference

When the user moves the mouse inside the game viewport while a script is running, hover-targeting state in the client can interrupt the script's pending `sleepUntil` calls. The log line `Interrupted waiting for client thread` typically means the user moved the mouse mid-action. Either run hands-off or accept the throughput hit.

## Random event handling (companion plugin pattern)

Random events (Genie, Sandwich Lady, Strange Plant, etc.) interrupt skilling. They need handling for two reasons: bots get stuck when an NPC blocks the next interaction, and accounts that NEVER engage with randoms are statistically flagged by Jagex detection (per community consensus across OSBot / DreamBot forums + Reddit r/2007scape).

Microbot exposes the canonical detection API: **`Rs2Npc.getRandomEventNPC()`** returns the current random event NPC (or null). The client maintains the full NPC ID list internally, so you don't hardcode the ~25 event types.

**Don't integrate random event handling into each skill plugin.** Use Microbot's global `BlockingEvent` framework via `Microbot.getBlockingEventManager()`. Build one **companion plugin** that registers a `BlockingEvent`, then enable it alongside any skill plugin. The manager interrupts the running script when an event fires, then resumes. Same pattern WoodcuttingPlus already uses for its 9 Forestry events.

### Reference implementation: EventDismissPlus

Pilot #5 forks the existing `eventdismiss/` plugin into `eventdismissplus/` with these enhancements:

- Variable response delay (2-5s default, configurable). **Instant dismiss is a bot signal** — anti-detection requires randomization.
- Per-event toggles: engage vs dismiss. Engagement gives free XP lamps (Genie / Beekeeper / Count Check), free food (Sandwich Lady), free items (Drunken Dwarf, Rick Turpentine, Dr Jekyll, Mysterious Old Man, Frog Token).
- Active-skill detection for lamps: 30-second rolling window of `client.getSkillExperience(Skill.*)` deltas. Apply lamp to whichever skill gained most XP recently. No cross-plugin coupling needed.
- Separate `BlockingEvent` for Strange Plant (a GameObject, not an NPC — `Rs2Npc.getRandomEventNPC()` doesn't catch it).
- `BlockingEventPriority.LOWEST` so Forestry events (or any future higher-priority handler) win conflicts.

```java
public class RandomEventNpcHandler implements BlockingEvent {
    @Override
    public boolean validate() {
        Rs2NpcModel npc = getRandomEventNpc();
        return npc != null && npc.hasLineOfSight();
    }

    @Override
    public boolean execute() {
        Rs2NpcModel npc = getRandomEventNpc();
        if (npc == null) return true;

        // Variable delay first (anti-detection)
        Global.sleep(Rs2Random.between(config.responseDelayMin(), config.responseDelayMax()));

        if (shouldEngage(npc.getName())) {
            engage(npc, npc.getName());
        } else {
            npc.click("Dismiss");  // universal post-2014 reform action
        }
        return !validate();
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.LOWEST;
    }
}
```

### When to build a new companion plugin

Companion plugin = global behavioral handler that augments any other plugin. Examples beyond random events:

- BreakHandler (long-uptime break scheduling)
- WorldHopper (intelligent hopping beyond per-plugin `maxPlayersInArea`)
- DeathRecovery (auto-grave-pickup)

If the behavior is global (not skill-specific) and benefits multiple plugins, write a companion plugin instead of integrating per-plugin. Cleaner architecture, less code duplication, easier to maintain.

## References

- AutoMiningPlus (Pilot #1) — `src/main/java/net/runelite/client/plugins/microbot/miningplus/`
- AutoMining (upstream reference) — `src/main/java/net/runelite/client/plugins/microbot/mining/`
- AutoWoodcutting (richer reference) — `src/main/java/net/runelite/client/plugins/microbot/woodcutting/`
- ExamplePlugin (minimal stub) — `src/main/java/net/runelite/client/plugins/microbot/example/`
- Pest Control (canonical reference per Hub CLAUDE.md) — `src/main/java/net/runelite/client/plugins/microbot/pestcontrol/`

## Data validation against OSRS Wiki (REQUIRED before shipping v0.1.0)

The upstream Hub's data files often disagree with the actual game. **AND I (Claude) have stale or incorrect game knowledge in training data.** Both AutoMiningPlus and AutoSmeltingPlus had data bugs caught on day 1 of testing that the wiki audit would have caught upfront.

Real examples:
- AutoMiningPlus v0.1.2: Lumbridge East/West tin+copper ↔ mithril+adamantite swap. User caught it by playing.
- AutoMiningPlus v0.1.3 audit: 15 total location-rock discrepancies (12 missing rocks, 3 phantom rocks). All in data that had been "trusted" from upstream + my pattern-matching from memory.
- AutoSmeltingPlus v0.1.1: phantom "Varrock West Furnace" (Varrock has anvils, no furnace). I made it up from memory. User caught it.

**The audit happens BEFORE shipping v0.1.0, not before v1.0.** This is the hard discipline rule. Treat the audit tool as a build gate, like tests.

Reuse the pattern in `tools/wiki-audit-<skill>.ps1`:

1. Hashtable of `our_name -> [rock_set]` mirroring the Java enum entries
2. Hashtable of `our_name -> wiki_page_title` (case + word order matters; the wiki uses "South-east Varrock mine" while our data uses "Varrock South East Mine")
3. For each page, fetch wikitext via `https://oldschool.runescape.wiki/api.php?action=parse&page=<title>&prop=wikitext&format=json`
4. Within the `==Rocks==` section (or equivalent for non-mining skills), grep for `[[<RockName> rock`
5. Diff and report

The OSRS Wiki API (MediaWiki) is the canonical source — community-maintained, fast post-patch updates, structured `==Section==` headers throughout. Don't trust the wiki page's prose for rock lists; use the rocks table specifically.

**Subsection regex gotcha:** when carving out `==Rocks==`, a naive `(?===|\Z)` stops at `===Subsection===` too because `==` is a prefix of `===`. Use `(?=\n==[^=]|\Z)` to require sibling-level headers only.

For non-mining skills, the wiki has analogous tables: fishing spots, food items, smithing bars. Each pilot defines its own audit; the shape is the same.

### Mandatory citation header for every data/*.java file

Every Java file with hardcoded game knowledge ships with a Source-of-Truth header citing the OSRS Wiki page and an audit log. This forces an explicit moment of "I checked this against the wiki" rather than "I wrote this from memory and trust myself."

Required shape (adapt for your skill):

```java
/**
 * <one-line description of what this file models>
 *
 * <h2>Source of truth</h2>
 * <ul>
 *   <li>Primary list: <a href="https://oldschool.runescape.wiki/w/<Page>">OSRS Wiki — <Page></a></li>
 *   <li>Per-entry detail: each entry's individual wiki page (e.g. <link to one>)</li>
 *   <li>Item/NPC IDs: RuneLite client constants ({@code net.runelite.api.gameval.ItemID})</li>
 * </ul>
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>YYYY-MM-DD (vX.Y.Z):</b> Audited via {@code tools/wiki-audit-<skill>.ps1}.
 *       Result: <N> OK / <M> mismatch / <K> not found. Corrections applied: <summary>.</li>
 *   <li>Re-audit before each minor version bump that touches this file.</li>
 * </ul>
 *
 * <h2>Known gaps</h2>
 * <ul>
 *   <li>List anything intentionally not in the dataset (members-only, quest-locked, etc.)</li>
 * </ul>
 */
```

This isn't busywork. The audit log is what makes a future-you (or another contributor) trust the data without re-running the audit themselves. AutoMiningPlus's `MiningRockLocations.java` has this header retroactively after v0.1.3; AutoSmeltingPlus's `FurnaceLocations.java` has it from v0.1.2 onward.

### Adapting the wiki-audit tool to your skill

`tools/wiki-audit-mines.ps1` is the reference. Clone it per pilot — don't try to make one universal audit, just copy and adjust for each skill's data shape. About 10 minutes of edits per skill.

What to change in the cloned script:

1. **`$ourData` hashtable** — replace mine names + rock-type sets with your skill's entity:
   - Smelting: `"Al Kharid Furnace" = @("Bronze", "Iron", "Steel", ...)`
   - Cooking: `"Lumbridge Range" = @("Shrimp", "Trout", ...)` (or you may not need locations at all if the recipe data is the audit target)
   - Fishing: `"Lumbridge Swamp" = @("Shrimp", "Anchovies")`
2. **`$wikiTitles` map** — internal name → OSRS Wiki page title. Word order and capitalization differ between our data and the wiki (`"Varrock South East Mine"` in our data vs `"South-east Varrock mine"` on the wiki). Worth scripting a `Get-WikiPageGuess` lookup that tries 2-3 patterns.
3. **`$rockCanonical` map** — replace rock canonicalization with your skill's entity canonicalization. For smelting this is the Bars enum; for fishing the Fish enum; for cooking maybe Food.
4. **The section regex** — `==Rocks==` for mining, `==Smelting==` or `==Bars==` for smelting (verify by hitting one OSRS Wiki page first), `==Food==` for cooking, etc. The section-header trick `(?=\n==[^=]|\Z)` from above still applies.
5. **The grep pattern inside the section** — `\[\[<EntityName> rock` for mining. For smelting it's typically `\[\[<BarName> bar`. For cooking `\[\[<Food>` (no suffix). Verify by inspecting one wiki page's `action=parse&prop=wikitext` output.

Save as `tools/wiki-audit-<skill>.ps1`. Run it before shipping v1.0 of your pilot. If it finds 5+ discrepancies (Pilot #1 found 15), bump a `v0.x.y` to fix them and re-audit.

The pattern is upstream-PR-worthy too. If your audit finds upstream data bugs, file them as a separate fix-data PR against `chsami/Microbot-Hub` alongside your plugin PR.
