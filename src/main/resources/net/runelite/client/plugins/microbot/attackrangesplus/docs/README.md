# Attack Ranges Plus

Draws an outline on the ground showing the tiles you can actually attack, clipped to **line of
sight** so it molds around walls and obstacles. Useful for PvP positioning (knowing exactly what
you can hit, and from where you can be hit).

Originally ported from [ry-java/AttackRanges2](https://github.com/ry-java/AttackRanges2) and rebuilt
for the Microbot client.

## How it works

- **Your range** is auto-detected from your equipped weapon and selected attack style using
  Microbot's maintained `Rs2Combat.getAttackRange()`. That means accurate per-weapon ranges,
  long-range handling, the spell range while autocasting, halberds at 2 tiles, and melee at 1.
- **Line-of-sight clipping**: the Chebyshev (king-move) attack square is reduced to only the tiles
  you have line of sight to, matching where shots actually connect, then outlined along its edge.

## Settings

- **Attack style**:
  - **Auto (detect)** - recommended; uses your real weapon/style.
  - **Melee (1) / Ranged (7) / Magic (10)** - fixed previews for forcing a style.
- **Line color** - your outline color.
- **Fill area** / **Fill color** - optional shaded fill. Off by default: the fill is repainted every
  frame and costs FPS at large ranges (magic). The outline alone is cheap.
- **Show overlay** - *Always*, *In PvP areas* (Wilderness, PvP/Deadman worlds, PvP-flagged zones),
  or *Wilderness only*.
- **Show target's range** / **Target line color** - also outline the attack range of the player you
  are fighting (the actor you are interacting with).

## PvM vs PvP

- **Your own range works everywhere**, including against NPCs/bosses (it is based on your equipped
  weapon and style).
- **Target's range is players only.** RuneLite does not expose an NPC's attack range or style, so
  the target outline cannot be drawn for monsters without hardcoding per-boss data.

## Notes / limitations

- **Target's range is approximate.** Another player's attack-style varbits are not readable, so the
  target outline uses their equipped weapon's *base* reach (looked up from the same weapon data
  Microbot uses). It omits the long-range modifier and cannot tell whether a staff holder is casting.
- Auto magic detection relies on an autocast spell being set. If you click-cast without autocast,
  use the **Magic** style override.
- Line of sight is computed in world coordinates; not designed for instanced areas (irrelevant for
  the Wilderness / PvP).
