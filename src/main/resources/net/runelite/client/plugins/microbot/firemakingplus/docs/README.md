# Auto Firemaking Plus

Trains Firemaking on a bank-and-do loop, part of the Microbot "Plus" suite. v0.1.0 offers two
selectable methods.

## Methods

**Forester's Campfire** (default, AFK).
- Stand near a bank (the **Grand Exchange** is ideal). The bot finds a campfire/fire nearby, or
  lights one with a tinderbox if none exists, then adds your logs to it until the inventory is
  empty, banks, and repeats. No walking once it's going.

**Line firemaking** (higher XP/hr).
- Lights logs in a horizontal line stepping west, walks back, banks, repeats. Start in an open area
  next to a bank. Ports the proven line-finding logic with a guard so it does not get stuck on a
  blocked row.

## Setup

- Bank your **logs** and a **tinderbox**. Pick the **Log type** (or turn on **Progressive** to burn
  the best logs your level allows). F2P: Logs (1) → Oak (15) → Willow (30) → Maple (45) → Yew (60).
- Press start near a bank (GE recommended).

## Settings

- **Method** - Forester's Campfire or Line firemaking.
- **Log type** / **Progressive** - what to burn.
- **Scan radius** - Line firemaking only: how far to search for an open line.
- **Stop after (minutes)** / **(XP gained)** / **Target level** - auto-shutdown thresholds (banks first).
- **League mode (anti-AFK)** / **Speed mode** - as in the rest of the suite.

## Roadmap

- **v0.2.0:** named-location picker (incl. an explicit Grand Exchange spot) + line crowd/blocked polish.
- **v0.3.0:** profit/hr overlay.
- Wintertodt stays out of scope (dedicated minigame).

Banking stays on foot (teleports disabled) so it never home-teleports away from the bank.
