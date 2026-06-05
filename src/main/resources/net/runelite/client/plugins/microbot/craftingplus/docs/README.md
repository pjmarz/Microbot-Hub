# Auto Crafting Plus

![preview](assets/card.png)

Trains Crafting on a fully automated bank-and-do loop. Part of the Microbot "Plus" suite, it covers three activities: leather crafting, gem cutting, and furnace jewellery - with stop conditions, a live overlay, and a clickable Pause button.

---

## Feature Overview

| Feature | Description |
|---------|-------------|
| **Activity** | Choose between Leather, Gem cutting, or Furnace jewellery |
| **Leather item** | Which leather piece to make (gloves through hardleather body) |
| **Gem** | Which gem to cut (Opal through Zenyte) |
| **Jewellery** | Which furnace piece to cast (gold ring through zenyte amulet) |
| **Furnace** | Which furnace and bank to walk between for jewellery (Edgeville, Falador, Port Phasmatys, Mount Karuulm, Zanaris, Shilo Village, or Anywhere) |
| **Stop after (minutes)** | Auto-shutdown after this many minutes of runtime; 0 disables |
| **Stop after (XP gained)** | Auto-shutdown after gaining this much Crafting XP; 0 disables |
| **Target level** | Stop when Crafting reaches this level (banks the inventory first); 0 disables |
| **League mode (anti-AFK)** | Periodically presses an arrow key to reset the idle-logout timer |
| **Speed mode** | Disables Microbot antiban for faster operation - throwaway accounts only |
| **Overlay** | Shows Crafting level (with level delta), XP gained, XP/hr, craft cycles completed, runtime, and target level progress |
| **Pause button** | Click Pause/Resume in the overlay to halt the loop without stopping the plugin |

---

## Requirements

- Microbot RuneLite client
- F2P friendly for leather (level 1+), gem cutting (level 1 for Opal), and plain gold jewellery (level 5+ for gold ring)
- Needle, thread, and the correct leather type pre-banked for leather activity
- Chisel and uncut gems pre-banked for gem cutting
- Gold or silver bar, the matching mould, and (for gem-set pieces) the already-cut gem pre-banked for furnace jewellery
- Membership required for some furnace locations (Port Phasmatys requires Ghosts Ahoy completion; Mount Karuulm, Zanaris, and Shilo Village are members-only areas)
- Members content required for dragonstone, onyx, and zenyte items

---

## How It Works

**Leather crafting:**
1. Set Activity to "Leather" and choose the leather item to make.
2. Bank a needle, thread, and a supply of leather (standard Leather or Hard leather for the hardleather body).
3. Start the plugin near a bank. The bot withdraws needle, thread, and a full inventory of leather, makes the chosen item via the make-all interface, banks the products, and repeats.

**Gem cutting:**
1. Set Activity to "Gem cutting" and choose the gem type.
2. Bank a chisel and a supply of uncut gems.
3. Start the plugin near a bank. The bot withdraws the chisel and uncut gems, cuts the whole inventory, banks, and repeats.

**Furnace jewellery:**
1. Set Activity to "Furnace jewellery", choose the jewellery piece, and pick a Furnace location.
2. Bank the required bar (gold or silver), the matching mould, and - if the piece sets a gem - the already-cut gem. The bot does not cut gems itself.
3. Start the plugin. The bot banks to collect supplies, walks to the furnace, casts the whole batch, walks back to the bank, and repeats.

The bot banks first whenever a stop condition triggers, so your inventory is never left half-processed.

---

## Configuration

Pick the **Activity** first - the other dropdowns only affect the active activity. For leather, set **Leather item**. For gem cutting, set **Gem**. For jewellery, set both **Jewellery** and **Furnace** (Edgeville is the closest free-to-play furnace-to-bank).

Use the stop conditions to cap a session: **Stop after (minutes)** and **Stop after (XP gained)** shut the plugin down after the threshold is crossed; **Target level** banks and stops as soon as your Crafting level reaches the goal. All three default to 0 (disabled); set any one to enable it.

**League mode** sends a periodic arrow-key press to defeat idle-logout, useful for long sessions on accounts without movement breaks. **Speed mode** skips antiban delays for maximum speed - only appropriate on accounts you are willing to risk.

---

## Limitations

- Gem cutting does not cut gems for the jewellery activity. You must pre-bank cut gems yourself before running furnace jewellery with a gem-set piece.
- Amulet stringing is not supported - gold and gem amulets are produced unstrung.
- Walking is always on foot. The plugin does not use teleports or home teleport, so it will not unexpectedly leave a furnace area.
- Port Phasmatys requires completion of the Ghosts Ahoy quest and membership.
- Amethyst tips and glassblowing are not supported in this version.

---
