package net.runelite.client.plugins.microbot.runecraftplus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Essence the altar trainer crafts with (v0.2.0). Pure and rune essence give identical XP, so pick
 * by cost/availability. Daeyalt gives +50% XP at every standard altar but is members-only and
 * self-mined (Sins of the Father + the Darkmeyer daeyalt mine).
 */
@Getter
@RequiredArgsConstructor
public enum EssenceType {
    RUNE("Rune essence", ItemID.BLANKRUNE),
    PURE("Pure essence", ItemID.BLANKRUNE_HIGH),
    DAEYALT("Daeyalt essence", ItemID.BLANKRUNE_DAEYALT);

    private final String itemName;
    private final int itemId;

    @Override
    public String toString() {
        return itemName;
    }
}
