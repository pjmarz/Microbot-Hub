# Auto Runecraft Plus

Runs essence to a runecrafting altar and crafts runes, on a loop: bank -> walk to altar -> enter ->
craft -> exit -> bank. Part of the Microbot "Plus" suite.

Forked from the `chillRunecraft` base and wrapped in the standard Plus layer (stop conditions,
target level with a clean banking shutdown, runtime/XP overlay, pause button, speed mode, league
anti-AFK).

## Setup

- Bank the **talisman or tiara** for your chosen altar and your **essence**. The bot withdraws/equips
  the tiara (or carries the talisman) automatically and tops up essence each trip.
- Pick the **Altar** in config. Air / Earth / Water / Fire / Body are F2P-runnable; Nature is members.
- Pick the **Essence**: Rune, Pure, or Daeyalt. Pure and rune give identical XP (pick by cost); daeyalt
  is +50% XP (members, self-mined).
- **Use pouches** (members): bank your essence pouches and the bot fills/empties them each trip. If you
  are on the **Lunar spellbook**, it also repairs degraded pouches via NPC Contact (Dark Mage).
- Press start near anywhere; the bot routes to the nearest bank first if it's missing gear/essence.

## Settings

- **Altar** - which altar to craft at.
- **Essence** - Rune / Pure / Daeyalt.
- **Use pouches** - fill/empty/repair essence pouches each trip (members-only).
- **Stop after (minutes)** / **Stop after (XP gained)** / **Target level** - auto-shutdown thresholds.
  Target level deposits the inventory before stopping.
- **League mode (anti-AFK)** - periodic arrow-key press to defeat the idle-logout.
- **Speed mode** - disables antiban for a faster, more detectable pace. Throwaway accounts only.

## Roadmap

- **v0.3.0:** more runner altars (Cosmic / Chaos / Law / Death) and an AUTO best-unlocked picker.
- **v0.4.0:** lava and combination runes (binding necklace; Magic Imbue at 82 Magic).
- **v0.5.0:** profit/hr overlay; daeyalt mining hybrid.

The broad altar loop stays the scope here; GOTR, Ourania/ZMI, Abyss, Astral, and Arceuus blood/soul
have dedicated Hub plugins and are not duplicated.
