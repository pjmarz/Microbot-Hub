# Auto Smithing Plus

![preview](assets/card.png)

Auto Smithing Plus automates the full bank-to-anvil-to-bank Smithing loop. Pick a bar tier, an item to forge, and an anvil; the bot collects bars from the bank, smiths a full inventory, deposits the finished goods, and repeats. It is part of the Microbot "Plus" suite and shares the same overlay, stop conditions, and antiban options as its sister skills.

---

## Feature Overview

| Feature | Description |
|---------|-------------|
| **Bar picker** | Bronze, Iron, Steel, Mithril, Adamantite, or Runite. Bars must be in your bank. |
| **Item picker** | 26 anvil items from Dagger (1 bar) to Plate Body (5 bars). |
| **Anvil picker** | Named anvils including Lumbridge (bronze-only), Varrock West/Central/East, Yanille, Seers' Village, and Burthorpe. AUTO_NEAREST anchors at the closest anvil within range. |
| **Level pre-flight** | Refuses to start if your Smithing level is too low, a members-only item is selected on F2P, or a non-bronze bar is paired with the Lumbridge Rusted Anvil. |
| **Progressive mode** | Ignores the Item picker and forges the best item your current Smithing level allows at the chosen bar tier. Skips members items on F2P. |
| **Preferred bank** | Override the nearest-bank default from a list of F2P and members bank locations. |
| **Items to bank / keep** | Comma-separated substring lists controlling what gets deposited. Defaults keep your hammer and bars. |
| **Max players in area** | Hop worlds if too many players are nearby. 0 disables hopping. |
| **Stop after (minutes)** | Auto-shutdown after a set runtime. 0 = no limit. |
| **Stop after (XP gained)** | Auto-shutdown after gaining a set amount of Smithing XP. 0 = no limit. |
| **Target level** | Deposits inventory and shuts down when Smithing reaches this level. 0 = disabled. |
| **League mode** | Presses an arrow key periodically to reset the idle-logout timer. |
| **Speed mode** | Disables Microbot antiban. Faster rates, more detectable pattern. Throwaway accounts only. |
| **Live overlay** | Smithing level (with delta), XP gained, XP/hr, smith cycles, runtime, and a Pause/Resume button. |

---

## Requirements

- Microbot RuneLite client
- Bars of your chosen tier in your bank; a hammer in your bank or inventory
- Smithing level high enough for your chosen item and bar combination (the bot states the exact requirement if you fall short)
- F2P friendly for most items and all F2P anvils; members account required for members-only items (Claws, Dart tips, Arrowtips, Knives, Nails, Wire/Spit/Studs, Oil lamp, Bullseye lamp, Bolts) and members anvils (Yanille, Seers' Village, Burthorpe)

---

## How It Works

1. Set Bar, Item, and Anvil in the config panel, then start the plugin.
2. The bot runs a pre-flight check and refuses to start if the config is invalid (level too low, members item on F2P, non-bronze bar at Lumbridge Rusted Anvil).
3. With Progressive mode on, the bot picks the highest-XP item your level can make at the chosen bar tier, and updates it as you level up.
4. The bot walks to your preferred bank, withdraws bars and a hammer, then walks to the anvil.
5. At the anvil it opens the smithing widget and clicks "Make All" for the selected item.
6. Once the inventory is empty it returns to the bank, deposits finished items, and withdraws the next stack.
7. The loop continues until a stop condition fires or you click Stop.

---

## Configuration

The three core pickers are **Bar**, **Item**, and **Anvil** in the General section. Enable **Progressive mode** to let the bot choose the best item automatically as you train. Use **Preferred bank** if the nearest-bank default picks a toll-gated bank you want to avoid. Extend **Items to keep** (default `hammer,bar`) with any other item substrings you never want deposited. Stop conditions (minutes, XP, target level) can be combined; the first to trigger wins.

---

## Limitations

- Members-only items (Claws, Dart tips, Arrowtips, Knives, Nails, Wire/Spit/Studs, Oil lamp, Bullseye lamp, Bolts) cannot be smithed on F2P. The bot refuses to start with these selected.
- The Lumbridge Rusted Anvil accepts bronze bars only.
- Toolbelt hammers are not detected; keep a regular hammer in your bank or inventory.
- The Wire/Spit/Studs slot is shared across Bronze (wire), Iron (spit), and Steel (studs). Using it with the wrong bar tier picks a different item in that slot.
- Members anvils (Yanille, Seers' Village, Burthorpe) require a members account.
