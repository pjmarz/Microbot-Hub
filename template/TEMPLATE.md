# Skill Plus Template — Authoring Guide

A scaffold for building "Plus" variants of upstream Microbot Hub plugins. Use this when the upstream plugin works but is missing UX features you want (auto-travel, more config knobs, alternative behaviors). Pilot #1 for this template is AutoMiningPlus.

## Before you start: required reading

These canonical docs are the source of truth, not this file. Read them once per new plugin:

- `https://raw.githubusercontent.com/chsami/Microbot/main/AGENTS.md` — the non-negotiable rules
- `https://raw.githubusercontent.com/chsami/Microbot/main/CLAUDE.md` — AI-author overview
- `https://raw.githubusercontent.com/chsami/Microbot/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/AGENTS.md` — plugin & script lifecycle
- `https://raw.githubusercontent.com/chsami/Microbot/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md` — cache/query patterns (v2.1.0)
- `https://raw.githubusercontent.com/chsami/Microbot/main/docs/entity-guides/items.md` and `movement.md` — per-entity footguns with real bug post-mortems
- `chsami/Microbot-Hub/CLAUDE.md` (in this repo) — Hub-specific conventions

OSRS Wiki (`oldschool.runescape.wiki`) is the primary source for item IDs, NPC IDs, world point coordinates. Bookmark and grep when populating any `data/` file.

## Decision matrix: is this template right for your skill?

| Skill family | Use this template? | Notes |
|---|---|---|
| Gathering (mining, woodcutting, fishing, hunter) | Yes — direct fit | This is what the skeleton's `IDLE → RESETTING` shape is for. |
| Stationary (cooking, smelting, fletching, crafting) | Yes — small variant | Replace the gathering interaction with `Rs2Inventory.combine(...)` or `Rs2Widget` clicks. Skip the location-walk piece. |
| Travel-heavy (herbrun, dailies, slayer between mobs) | Maybe — extend to StateMachineScript | A 2-state machine is too thin. Consider `StateMachineScript<State>` for multi-waypoint flows. |
| Combat (AIO Fighter, slayer combat) | No | Submodule structure + combat targeting + prayer flicking + food management is incompatible. Build your own template if you need one. |
| Minigames (Pest Control, Tempoross) | Maybe | Game-specific UI hooks usually dominate. Read the upstream plugin first. |

## File-by-file walkthrough

The `template/skeleton/` folder mirrors the layout your new plugin's package should have. After copying:

1. **`AutoSkillPlusPlugin.java`** — the entry. Edit `@PluginDescriptor` fields:
   - `name` — `PluginDescriptor.Mocrosoft + "Auto <Skill> Plus"` (the Mocrosoft prefix is a `PluginDescriptor` constant, not a typo).
   - `version` — bump per the version roadmap below.
   - `minClientVersion` — lowest microbot version that still exposes every API you use. When in doubt, copy from the upstream plugin you're forking from.
   - `tags`, `description` — short, scannable. Tags help users find your plugin.
2. **`AutoSkillPlusConfig.java`** — config items. The `@ConfigGroup` string MUST be unique across the Hub (e.g. `"MiningPlus"`, `"CookingPlus"`). Sharing it with an upstream plugin causes cross-pollution of saved settings.
3. **`AutoSkillPlusScript.java`** — the loop. Read the rules comment at the top before editing. The skeleton has a `IDLE → RESETTING` shape; expand the `State` enum as needed.
4. **`AutoSkillPlusOverlay.java`** — minimal status renderer. The skeleton just pulls `Microbot.status`, which is the cheapest way to surface what the script is doing.
5. **`State.java`** — state machine values. Keep ≤ 5 states; switch to `StateMachineScript` if you need more.
6. **`data/SkillLocationOption.java`** — facade enum for a named-location dropdown. Reuse the AutoMiningPlus pattern (`MineLocationOption` → `MiningRockLocations`).
7. **`data/SkillItem.java`** — optional, for skills with raw/processed item recipes.

If your skill doesn't need item or location lookups (e.g. a thin plugin with no banking), delete the `data/` folder.

## Set up your dev tree

```powershell
# Clone the Hub fork (if you haven't already)
git clone https://github.com/chsami/Microbot-Hub.git $env:USERPROFILE\src\Microbot-Hub

# Copy the skeleton to a new plugin package — replace <skill> with your skill name (lowercase)
$skill = "cooking"  # ← change this
$dest = "$env:USERPROFILE\src\Microbot-Hub\src\main\java\net\runelite\client\plugins\microbot\${skill}plus"
Copy-Item -Recurse "$env:USERPROFILE\src\Microbot-Hub\template\skeleton" $dest

# Rename the .java files: AutoSkillPlus<X>.java → Auto<Skill>Plus<X>.java
Get-ChildItem $dest -Filter "AutoSkill*.java" -Recurse | ForEach-Object {
    Rename-Item $_.FullName ($_.Name -replace "AutoSkill", "Auto$($skill.Substring(0,1).ToUpper())$($skill.Substring(1))")
}
```

Then in your editor, replace tokens within each file:
- `Skill` → `Cooking` (CamelCase)
- `skillplus` → `cookingplus` (lowercase package suffix)
- `Auto Skill Plus` (the descriptor name string) → `Auto Cooking Plus`

## Build it

Two prerequisites:

- **JDK 11 (Adoptium Temurin) installed at a discoverable path.** The Hub targets `TARGET_JDK_VERSION = 11` exactly. Gradle's toolchain auto-detection finds JDK 11 in standard install paths even if your `JAVA_HOME` points to a different version. Install via winget:
  ```powershell
  winget install -e --id EclipseAdoptium.Temurin.11.JDK --silent --accept-package-agreements --accept-source-agreements
  ```
  After install, the JDK lives at `C:\Program Files\Eclipse Adoptium\jdk-11.0.x.x-hotspot\`. If gradle complains "No matching toolchains found for requested specification: {languageVersion=11, vendor=ADOPTIUM}", verify the install path exists.
- **Microbot client jar.** Either let Gradle fetch the latest from `microbot.cloud` (default), or pass `-PmicrobotClientPath=<path>` to use a local jar (faster, avoids network).

```powershell
# JAVA_HOME can be 11 or 17 — gradle's toolchain block reads its own JDK selection from
# project-config.gradle, not from JAVA_HOME. Set JAVA_HOME to whichever you have; gradle
# will auto-select JDK 11 for the actual compile.
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"  # or 11; either works
cd $env:USERPROFILE\src\Microbot-Hub
.\gradlew.bat AutoCookingPlusPluginJar -PpluginList=AutoCookingPlusPlugin `
  -PmicrobotClientPath="C:\Tools\Microbot\microbot-2.6.0.jar"
```

The resulting jar lands at `build/libs/AutoCookingPlusPlugin-<version>.jar`.

### Build gotcha — REQUIRED build.gradle fixes (Pilot #1 learning)

The Hub's `compileJava` task (default `main` source set) scans every plugin folder. As of late 2025 this is broken: ActionReplay, Agility, and several other upstream plugins fail to compile against current microbot versions (e.g. microbot 2.6.0 removed `Rs2NpcModel`, breaking any plugin that imports it). This poisons the whole `main` compile.

**Without these fixes, building ANY single Plus plugin will fail** because gradle compiles `main` (all plugins) before your plugin's jar task runs.

Apply the fixes to `build.gradle` in your fork — they're small and idempotent.

**Fix 1:** After the `validPlugins` discovery (inside the `afterEvaluate` block), exclude every plugin folder from the `main` source set:

```groovy
allPlugins.each { plugin ->
    sourceSets.main.java.exclude("net/runelite/client/plugins/microbot/${plugin.dir.name}/**")
}
```

This makes `main` only contain shared top-level code (`PluginConstants.java` and a few helpers). Each plugin then compiles only in its own per-plugin source set, which has its own classpath and won't drag in incompatible upstream code.

**Fix 2:** The default per-plugin jar task pulls classes from `sourceSets.main.output`. With Fix 1 applied, `main` no longer contains your plugin's classes — the jar would build empty. Add a configure block that also sources from the plugin's own source set output:

```groovy
def jarTask = tasks.register("${plugin.name}Jar", ShadowJar, project.ext.getPluginJarTaskConfig(plugin, pluginConfigName))
jarTask.configure {
    from(sourceSets["${plugin.sourceSetName}"].output)
}
```

Both fixes are upstream-PR-worthy. Without them, every Pilot author rediscovers this on day 1.

## Install it

Drop the jar into the Microbot client's plugin directory:

```powershell
Copy-Item build\libs\AutoCookingPlusPlugin-*.jar $env:USERPROFILE\.runelite\microbot-plugins\
```

Then restart the Microbot client (close the running RuneLite + relaunch from the Microbot Launcher). The Plugin Hub panel will pick up the new jar on next startup.

## v0.1.0 ship gate (do NOT skip)

Before declaring v0.1.0 done, all of these must be true. Treat the list as a checklist:

- [ ] Every `data/*.java` file has a **Source-of-Truth citation header** in its class Javadoc, with a wiki URL and an audit log entry. See PATTERNS.md "Mandatory citation header" for the exact shape.
- [ ] An audit script exists at `tools/wiki-audit-<skill>.ps1`, cloned from `tools/wiki-audit-mines.ps1` and adapted for your skill's data. Coordinates and exact IDs may not be auditable from the wiki — but names, members/F2P status, and existence of locations always are.
- [ ] **The audit script has been RUN and passes** (or every flagged discrepancy is either fixed or explicitly documented in the citation header's "Known gaps" section). No exceptions for "I'll fix it in v0.2."
- [ ] The plugin builds. The jar lives at `build/libs/Auto<Skill>PlusPlugin-0.1.0.jar`.
- [ ] One end-to-end smoke test session run (≥10 min) without obvious bugs.

**Why this is here:** Pilot #1 (mining) shipped v0.1.0 with bad data — Lumbridge East/West tin↔mithril were swapped, plus 14 other discrepancies. We found them by playing the game, fixing them across v0.1.2 and v0.1.3. Pilot #2 (smelting) shipped v0.1.0 with a phantom "Varrock West Furnace" that doesn't exist in the game. I made it up from memory. The user caught both.

The audit at v0.1.0 catches name-level data lies upfront so you (and your users) don't waste in-game time reproducing bugs that a 30-second script run would have surfaced.

## Recommended post-v0.1.0 patterns (v0.5.0 milestone)

Once your pilot is past v0.1.0 and you're polishing, these patterns have been validated across four shipped pilots (Mining, Smelting, Smithing, Woodcutting). Not required — your plugin will work without them — but heavily recommended. Each is documented in PATTERNS.md with example code.

- **`speedMode` antiban single-flag.** One boolean config. In `run()`: `if (config.speedMode()) Rs2AntibanSettings.antibanEnabled = false;`. Throwaway-friendly XP-rate maxing without per-flag tuning UI.
- **`stopAfterMinutes` + `stopAfterXp` thresholds.** Two int configs (default 0 = disabled). Check at top of each tick; `super.shutdown()` when met. Defeats the uniform-session-length signal that Jagex's detection model loves.
- **`targetLevel` + cleanup-then-shutdown** (v0.5.0). Int config. When primary skill reaches value, set a `shutdownAfterCleanup` flag and force the state machine into RESETTING. Intercept the flag at the end of RESETTING (just before the state-flip-back) and shut down. Mirrors AIO Fighter's per-skill target.
- **Pause via overlay button + `Microbot.pauseAllScripts`** (v0.5.1+). Mirrors AIO Fighter's pattern. Click the overlay button to toggle a global `AtomicBoolean`; every Plus plugin's script reads it and early-exits when true. **CRITICAL**: call `pauseButton.hookMouseListener()` in the Plugin's `startUp()` after `overlayManager.add()`. Without that call the button renders but clicks pass through. See PATTERNS.md "Overlay button pattern" for the full template.
- **`maxPlayersInArea` autohop.** Anti-PK / anti-crowd. Walk + smelt locations are particularly susceptible. Use `Microbot.hopToWorld()` (2.6.2 rewrite handles all the world-switch edge cases).
- **`leagueMode` anti-AFK.** Periodic arrow-key press resets the game's 5-min idle-logout. Critical for long unattended runs.
- **CSV `itemsToBank` / `itemsToKeep`.** Substring-match deposit/keep lists. Defaults that catch the common case but auto-augment with the active entity's first word (see next pattern).
- **Deposit-filter auto-augment.** When mining coal / clay / basalt, the item name doesn't match the default `"ore"` filter and the bot oscillates bank↔mine. Fix: always append `activeRock.getName().split(" ")[0].toLowerCase()` to the filter list at deposit time. Generic across rocks; ships in MiningPlus v0.4.1.
- **Runtime stats overlay.** Uptime + XP gained + XP/hr + action counter + level delta. Read fields from the script via getters; render in an `OverlayPanel`. Add a "Target: X (Y to go)" line when targetLevel > 0.
- **Companion plugin: EventDismissPlus** (Pilot #5, v0.1.0+). Random events are global — don't integrate per-plugin. Document EventDismissPlus as a recommended companion in your plugin's CHANGELOG so users know to enable it. See `template/PATTERNS.md` "Random event handling" for the BlockingEvent companion pattern.

A pilot graduating to v1.0.0 (upstream PR) should have all of these.

## Versioning convention

Semver-flavored. `0.x` = pre-stable, breaking changes welcome. `1.0` = "I'd recommend this as a daily driver and submit it upstream."

Each minor version is one ship cycle: implement → build → install → test for a session → revise → next.

- `v0.1.0` — MVP: the smallest possible useful behavior. One feature only. **Hard ship gate** (see below).
- `v0.2.0` to `v0.4.0` — refinements, polish, edge cases, secondary features. The polish-cycle borrows from WC live here.
- `v0.5.0` — Target Level + Pause cross-plugin baseline. The four shipped pilots are aligned at v0.5.0 as of 2026-05-22.
- `v0.6.0+` — skill-specific deep features (e.g. teleport-gated rocks for Mining, progressive smith data table for Smithing).
- `v1.0.0` — stable, ≥20 hours throwaway-account uptime without major issues, recommended-patterns checklist complete, ready to PR.

## When and how to PR upstream

Once a plugin hits v1.0.0 and has been used on a throwaway for a real amount of time:

1. Open a PR against `chsami/Microbot-Hub` on the `development` branch (per Hub CLAUDE.md).
2. Title format: `feat: Add <PluginName> for <description>`.
3. Include: screenshot of the plugin panel, screenshot of in-game overlay, plugin's own `docs/README.md` under `src/main/resources/.../<pluginname>/docs/`.
4. Maintainer reviews and either merges to `development` (then to `main` for release) or asks for changes.

If your changes touch shared infrastructure (like our `build.gradle` source-set fix), that's a separate PR with a clear "fix:" prefix.
