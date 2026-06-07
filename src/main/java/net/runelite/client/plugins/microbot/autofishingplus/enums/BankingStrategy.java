package net.runelite.client.plugins.microbot.autofishingplus.enums;

/**
 * How a {@link FishingPlusLocation} clears a full inventory.
 *
 * <ul>
 *   <li>{@link #FULL_BANK} -- walk to the nearest full bank and deposit (Rs2Bank). Use where a
 *       bank is close to the spots (Fishing Guild, Catherby, Piscatoris). Draynor also uses this,
 *       though its bank sits a short walk north of the river net spot.</li>
 *   <li>{@link #DEPOSIT_BOX} -- walk to a specific deposit-box approach tile and use Rs2DepositBox.
 *       Required for Corsair Cove (the box is a long free walk east of the lobster pier).</li>
 *   <li>{@link #DROP} -- drop the catch in place. Fastest XP for spots with no close banking
 *       (Musa Point, Al Kharid, Otto's Grotto barbarian fishing).</li>
 * </ul>
 */
public enum BankingStrategy {
    FULL_BANK,
    DEPOSIT_BOX,
    DROP
}
