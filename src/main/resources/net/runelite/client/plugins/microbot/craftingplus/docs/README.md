# Auto Crafting Plus

Trains Crafting on a bank-and-do loop, part of the Microbot "Plus" suite. v0.2.0 does **leather
crafting**, **gem cutting**, and **furnace jewellery**.

## Activities

**Leather** (F2P, from level 1) - the standard low-level trainer.
- Bank a **needle**, **thread**, and **leather** (cowhide Leather, or Hard leather for hardleather body).
- Pick the **Leather item**: gloves (1) → boots (7) → cowl (9) → vambraces (11) → body (14) →
  chaps (18) → hardleather body (28).
- The bot withdraws needle + thread + a full inventory of leather, makes the chosen item, banks the
  products, and repeats.

**Gem cutting** (from level 20 for sapphire).
- Bank a **chisel** and **uncut gems**. Pick the **Gem** (Opal 1 … Diamond 43 are F2P).
- Withdraws chisel + uncut gems, cuts them, banks, repeats.

**Furnace jewellery** (gold from level 5; gem pieces by gem level).
- Bank a **gold or silver bar**, the matching **mould**, and (for gem pieces) the **cut gem**.
- Pick the **Jewellery** piece and the **Furnace** location (Edgeville is the closest F2P
  furnace-to-bank). The bot banks, walks to the furnace, casts the whole batch, banks, repeats.
- Gold rings/necklaces/bracelets/amulets are F2P; silver tiaras/symbols and gem pieces follow.

## Settings

- **Activity** - Leather, Gem cutting, or Furnace jewellery.
- **Leather item** / **Gem** / **Jewellery** - what to make.
- **Furnace** - furnace + bank to use for jewellery.
- **Stop after (minutes)** / **(XP gained)** / **Target level** - auto-shutdown thresholds (banks first).
- **League mode (anti-AFK)** / **Speed mode** - as in the rest of the suite.

## Roadmap

- **v0.3.0:** wire in the base's glassblowing, spinning, battlestaves, and d'hide leather + snakeskin.
- **v0.4.0:** progressive tiering, amethyst tips, amulet stringing, profit/hr.
- **v0.5.0:** superglass make (Magic 77), pottery (wheel + kiln).

Banking stays on foot (teleports disabled) so it never home-teleports away from the bank.
