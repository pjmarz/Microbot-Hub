# Auto Woodcutting Plus

![preview](assets/card.png)

Auto Woodcutting Plus chops trees, handles inventory, completes Forestry events, and optionally fletches logs - all without touching the keyboard. It is part of the Microbot "Plus" suite of skilling plugins, sharing the same live overlay, stop conditions, and antiban controls as the other suite members.

---

## Feature Overview

| Feature | Description |
|---------|-------------|
| **Tree picker** | Choose any of 28 tree types - from regular logs at level 1 through Redwood at level 90 |
| **Progressive mode** | Automatically switches to the highest-level tree your Woodcutting level unlocks |
| **AUTO location** | The bot walks itself to the nearest suitable spot for the chosen tree, accounting for quest and skill gates |
| **Primary action** | Choose what happens when your inventory fills: Bank logs, Drop logs, Burn logs, Burn at campfire, or Fletch logs |
| **Fletching** | When primary action is FLETCH, pick the item to make (arrow shafts, etc.) and a secondary action (drop or bank the result) |
| **String bows** | Optionally strings unstrung bows after fletching if bowstring is in inventory |
| **Firemake-only mode** | Burns logs in place without cutting - useful for Firemaking XP at the Grand Exchange or similar spots |
| **Hardwood Tree Patch** | Redirect cutting to the Hardwood Tree Patch |
| **Forestry events** | Automatically participates in all nine Forestry events when on a Forestry world |
| **Loot bird nests** | Picks up bird nests that drop during woodcutting and events |
| **Loot seeds** | Picks up seeds from Forestry event rewards |
| **Loot my items only** | Restricts looting to your own drops - useful for Ironman accounts |
| **Autohop on player** | Hops worlds when another player walks near your chopping spot |
| **Distance to stray** | Limits how far in tiles the bot may wander from its start tile before walking back |
| **Walk back** | Choose whether to return to your original start tile or the last tree location after banking |
| **Stop after (minutes)** | Auto-shutdown after a set runtime. 0 = no limit |
| **Stop after (XP gained)** | Auto-shutdown after gaining a set amount of Woodcutting XP. 0 = no limit |
| **Target level** | Stop when Woodcutting reaches a chosen level, banking or dropping first |
| **Speed mode** | Disables Microbot antiban (action delays, micro-breaks, Bezier mouse paths). Throwaway accounts only |

---

## Requirements

- Microbot RuneLite client
- An axe in your inventory or equipped (the bot does not buy or retrieve one)
- F2P friendly - regular trees, oak, willow, and yew (Lumbridge/Falador) work on free worlds
- Members required for teak, maple (Seers'), mahogany, blisterwood, magic, and redwood
- Several locations are quest-gated: Ape Atoll teak/mahogany requires Monkey Madness I; Mos Le'Harmless teak/mahogany requires Cabin Fever; Corsair Cove maple/yew requires The Corsair Curse and Dragon Slayer I; Darkmeyer blisterwood requires Sins of the Father; the Woodcutting Guild requires level 60 Woodcutting
- Forestry events: use a Forestry world for Forestry XP boosts and loot (not required but recommended)
- Fletching: bring a knife; bow-stringing also needs bowstring

---

## How It Works

1. Equip or carry an axe. Place your character near trees or at a bank, then configure and start the plugin.
2. The bot identifies the target tree. If AUTO location is active, it walks to the nearest accessible spot for that tree type.
3. It chops until the inventory is full, then performs the primary action: banking, dropping, burning, or fletching.
4. After handling the inventory, it walks back to the chopping spot (either your original start tile or the last tree location, depending on your Walk Back setting) and resumes.
5. If Forestry is enabled, the bot pauses chopping whenever a Forestry event spawns nearby, completes the event interaction, then returns to cutting. The overlay shows the active event name and a running count of events completed.
6. Bird nests and seeds on the ground are looted automatically if their toggles are on.
7. Stop conditions are checked each cycle - the bot shuts itself down when runtime, XP, or target level is reached.

---

## Configuration

Set your **Tree** in the General section, or enable **Progressive mode** to let the bot pick the best tree automatically. The **Distance to Stray** default is 20 tiles - lower this if you want the bot to stay tight to a single cluster.

For inventory handling, set **Primary action** to DROP for fastest XP (no walking), BANK to keep the logs, or FLETCH to train Fletching at the same time. If you choose FLETCH, also set **Fletching type** and **Secondary action**.

In the Forestry section, toggle **Enable forestry** to activate event handling. Each of the nine event types (Egg, Entlings, Flowers, Fox, Hives, Leprechaun, Ritual, Root, Struggling Sapling) can be toggled individually. All nine default to on except Flowers, which defaults to off.

The overlay shows current level, XP gained, XP/hr, logs chopped, runtime, current tree (with "Progressive" label when applicable), and a Pause/Resume button. When Forestry is enabled, the overlay also shows the active event name and total events completed.

---

## Limitations

- The bot does not buy or equip an axe - you must provide one before starting
- Firemake-only mode is tested at the Grand Exchange north-east corner; other spots may work but are not guaranteed
- Quest-gated locations are silently skipped if you have not completed the required quest - the bot falls back to the next accessible location
- Flowers event defaults to off because its interaction is inconsistent; enable it at your own discretion
- Progressive mode selects trees by level requirement only - it does not factor in local availability or banking distance
- Speed mode removes antiban protections - use only on disposable accounts

---

## Credits

- Forestry event handling by Yuof and TaF
- Forked from the Microbot Auto Woodcutting plugin (Mocrosoft); Plus layer by pjmarz
