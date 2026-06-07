# Event Dismiss Plus

![preview](assets/card.png)

Event Dismiss Plus handles OSRS random events automatically while any other bot runs. It is part of the Microbot "Plus" suite. Enable it alongside a skill plugin - Mining, Woodcutting, Smelting, AIO Fighter, or any other - and it intercepts every random event, dismisses the ones that offer nothing useful, and engages the high-value ones for free XP, food, and items.

---

## Feature Overview

| Feature | Description |
|---------|-------------|
| **Auto-dismiss everything** | Any random event NPC not covered by an engagement toggle is right-click dismissed |
| **Variable response delay** | Waits a configurable random delay (ms) before acting - instant dismissal is a bot signal |
| **Random skip chance** | Randomly dismisses an engageable event by a configured percentage for extra variability |
| **Genie lamp auto-skill detection** | Tracks XP over a 30-second rolling window and applies lamps to the skill currently gaining XP |
| **Fallback lamp skill** | Fixed skill used for lamps when auto-detect is off or finds nothing |
| **Sandwich Lady** | Accepts the food offering (1-in-64 chance for a rare stale baguette worth ~542k gp) |
| **Drunken Dwarf** | Accepts beer and kebab |
| **Mysterious Old Man** | Accepts the gift (coins, gems, or key halves); declines the Maze variant |
| **Frog Prince / Princess** | Kisses the frog for a Frog Token (redeemable for an outfit or lamp) |
| **Rick Turpentine** | Accepts loot (average ~551 gp, rare crystal key half) |
| **Dr Jekyll** | Accepts the potion (matches a clean herb in inventory, or Strength(2) if none) |
| **Strange Plant harvest** | Clicks "Pick" on the Strange Plant game object for a 30% run-energy restore |
| **Beekeeper + Count Check** | Claims XP lamps through the same skill-picker dialogue flow as Genie |
| **Overlay** | Shows total events handled, the last event name, and time since that event |

---

## Requirements

- Microbot RuneLite client
- No skill level or quest prerequisites
- Members or free-to-play: most random events appear in both; Genie and Beekeeper are members-only but the plugin handles whichever appear
- No specific location required

---

## How It Works

Event Dismiss Plus registers two passive handlers with Microbot's global blocking-event framework. When a random event fires, Microbot pauses whatever script is running, hands control to this plugin, then resumes automatically.

1. Enable Event Dismiss Plus from the Microbot plugin list alongside your chosen skill or combat plugin.
2. Set your response delay range (default: 2000-5000 ms). Leave the defaults unless you have a specific reason to change them.
3. Toggle engagement on or off per event type in the "High-value engagement" section. All default to on.
4. In "Genie lamp handling", leave Auto-detect lamp skill on to apply lamps to whatever skill is gaining XP. Set a fallback skill for low-activity sessions.
5. Optionally set a Random Skip Chance (0-100%) to occasionally dismiss an engageable event for extra variability.
6. Start your main skill plugin. Event Dismiss Plus runs silently alongside it.

When an event appears, the plugin waits a random delay within your configured range, then dismisses or engages based on your toggles. Lamp events walk the full dialogue - Continue clicks, the "Yes please" confirmation, then the skill picker. Other engageable events click through their dialogue until closed. If engagement fails to despawn the NPC, the plugin falls back to Dismiss.

---

## Configuration

**General**

- Response delay min / max (ms): random wait before each action; range 0-10000, defaults 2000/5000.
- Random Skip Chance %: chance to dismiss instead of engage even when a toggle is on. 0 = always engage; 100 = always dismiss.

**High-value engagement**

Individual on/off toggles for Genie, Sandwich Lady, Drunken Dwarf, Mysterious Old Man, Strange Plant, Beekeeper, Count Check, Frog Prince/Princess, Rick Turpentine, and Dr Jekyll. All default to on. Off = dismiss that event.

**Genie lamp handling**

- Auto-detect lamp skill: on by default. Uses the skill with the most XP gained in the last 30 seconds.
- Fallback lamp skill: dropdown of all skills, defaults to Mining.

---

## Limitations

- Mime events (Niles, Miles, Giles) require emote-mimicking and are dismissed automatically; full engagement is a future addition.
- Prison Pete (balloon-pop mini-quest) and Freaky Forester (chicken-catching) are dismissed automatically; both are deferred.
- Mysterious Old Man's Maze variant is declined rather than engaged.
- Count Check requires a Bank PIN on your account for the lamp dialogue to complete.
- Strange Plant is detected within 20 tiles of your player. If your script moves you away before the handler fires, the plant despawns on its own after ~60 seconds.
- This plugin does not train any skill. Always enable it alongside a second plugin.
