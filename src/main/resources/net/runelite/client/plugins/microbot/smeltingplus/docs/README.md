# Auto Smelting Plus

![preview](assets/card.png)

Auto Smelting Plus automates the furnace smelting loop in Old School RuneScape, training Smithing by banking ores, walking to a furnace, smelting a full inventory of bars, and repeating. It is part of the Microbot "Plus" suite of skill training plugins.

---

## Feature Overview

| Feature | Description |
|---------|-------------|
| **Bar picker** | Choose which bar to smelt: Bronze, Blurite, Iron, Silver, Steel, Gold, Mithril, Adamantite, or Runite. The required ore recipe is applied automatically. |
| **Progressive smelt** | Ignores the bar dropdown and auto-selects the highest bar your Smithing level and bank stock can support, re-evaluated every banking trip. |
| **Furnace picker** | Walk directly to a named furnace. Includes four F2P furnaces and six members furnaces. AUTO_NEAREST finds the closest furnace to where you stand at start. |
| **Preferred bank** | Override the nearest-by-distance bank with a specific bank location. |
| **Items to bank** | Comma-separated substring list. Items whose names match any entry are deposited. Defaults to `bar`, which catches all smelted bars. |
| **Items to keep** | Comma-separated list of items to never deposit. Defaults preserve your coal bag, ring of forging, and goldsmithing gauntlets. |
| **Max players in area** | Hop worlds automatically when more than this many players are within the stray-distance radius. Set to 0 to disable. |
| **Distance to stray** | How far the bot is allowed to wander from the furnace tile. Also the player-detection radius for world-hopping. Default is 20 tiles. |
| **Stop after (minutes)** | Auto-shutdown after this many minutes of runtime. 0 means no limit. |
| **Stop after (XP gained)** | Auto-shutdown after gaining this much Smithing XP. 0 means no limit. |
| **Target level** | Stop when Smithing reaches this level. Deposits any bars in the inventory first, then shuts down. 0 means disabled. |
| **Speed mode** | Disables Microbot antiban for a faster pace. Intended for throwaway accounts only. |
| **League mode (anti-AFK)** | Periodically presses an arrow key to reset the idle logout timer. Useful in League worlds. |
| **Live overlay** | Shows current status, Smithing level and level gained, XP gained, XP/hr, smelt cycles completed, and runtime. Includes a Pause/Resume button. |

---

## Requirements

- Microbot RuneLite client
- Smithing level appropriate for your chosen bar (Bronze: 1, Iron: 15, Silver: 20, Steel: 30, Gold: 40, Mithril: 50, Adamantite: 70, Runite: 85)
- Sufficient ores in the bank for your chosen bar
- F2P friendly - four F2P furnaces are included (Lumbridge, Al Kharid, Edgeville, Falador West)
- Members-only furnaces (Port Phasmatys, Neitiznot, Keldagrim, Prifddinas, Shilo Village, Mor Ul Rek) require a membership and may require quest completion for access

---

## How It Works

1. Configure your bar, furnace, and banking options in the plugin panel.
2. Position your character near a bank (or near your chosen furnace if using AUTO_NEAREST).
3. Enable the plugin. The bot opens the bank, withdraws the required ores, and closes the bank.
4. The bot walks to the furnace and opens the smelt interface.
5. It selects your chosen bar and smelts a full inventory.
6. Once smelting is complete, the bot walks back to the bank, deposits the bars, and withdraws fresh ores.
7. Steps 3 to 6 repeat until a stop condition is met or you click Pause/Resume in the overlay.

If Progressive smelt is enabled, the bar selection step is skipped - the bot instead checks your current Smithing level and bank stock each trip and smelts the best bar it can.

---

## Configuration

The **Bar** dropdown and **Furnace** dropdown are the two settings you must set before starting. Leave **Furnace** on AUTO_NEAREST if you are already standing near a furnace; select a specific furnace if you want the bot to walk to it from anywhere.

The **Items to bank** and **Items to keep** fields control what gets deposited each cycle. The default `bar` entry in Items to bank deposits all smelted bars. Add extra substrings (comma-separated) if you carry other items that should be banked. The keep list defaults protect your coal bag and smelting gear.

Stop conditions give you hands-off control over session length. Set **Stop after (minutes)**, **Stop after (XP gained)**, or **Target level** to any non-zero value and the bot will shut itself down cleanly when the threshold is reached.

---

## Limitations

- Glassblowing (Bucket of sand + Soda ash) is not supported. It uses a different furnace interaction than standard bar smelting.
- Cannonball smelting is out of scope - use the dedicated AutoCannonballSmelter plugin for that.
- Jewellery moulding (rings, necklaces, amulets) is not supported here - use the upstream crafting/jewelry plugin.
- Members furnaces may require quest prerequisites before they are accessible (for example, Port Phasmatys requires Ghosts Ahoy, Prifddinas requires Song of the Elves).
- AUTO_NEAREST requires you to start within range of a furnace. If you start far from any furnace, select a specific furnace from the dropdown instead.

---
