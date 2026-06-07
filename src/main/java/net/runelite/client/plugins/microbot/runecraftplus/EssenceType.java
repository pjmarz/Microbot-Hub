package net.runelite.client.plugins.microbot.runecraftplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Essence the altar trainer crafts with. Pure and rune essence give identical XP, so pick
 * by cost/availability. Daeyalt gives +50% XP at every standard altar but is members-only and
 * self-mined (Sins of the Father + the Darkmeyer daeyalt mine).
 */
@Getter
@RequiredArgsConstructor
public enum EssenceType {
    RUNE("Rune essence", ItemID.BLANKRUNE, 1.0),
    PURE("Pure essence", ItemID.BLANKRUNE_HIGH, 1.0),
    DAEYALT("Daeyalt essence", ItemID.BLANKRUNE_DAEYALT, 1.5);

    private final String itemName;
    private final int itemId;
    // XP multiplier per essence relative to pure/rune essence. Daeyalt grants +50% XP per essence,
    // so the same XP gained corresponds to fewer essence consumed.
    private final double xpMultiplier;

    @Override
    public String toString() {
        return itemName;
    }
}
