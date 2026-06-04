package net.runelite.client.plugins.microbot.autofishingplus.enums;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * User-facing dropdown of named fishing locations for the Plus layer.
 *
 * <p>{@link #AUTO} keeps the base behaviour: fish the nearest spot of the chosen {@link Fish}
 * to wherever the player is standing, and bank/drop per the Use Bank toggle. Every other entry
 * walks the player to {@link #worldPoint} first and clears the inventory with its own
 * {@link BankingStrategy}.
 *
 * <p>All coordinates are reused from the base {@code FishingSpotLocation} enum (already shipped
 * in the client) or, for the Corsair Cove deposit box, captured in-game via the agent server.
 * Nothing here is sourced from memory.
 *
 * <p>The {@link Fish} a location offers is NOT enforced -- the player still picks the fish.
 * If the chosen fish is not available at the chosen location the spot finder simply idles and
 * the overlay status flags it. Pick a fish your location offers.
 */
@Getter
public enum FishingPlusLocation {
    // worldPoint = walk-to fishing anchor; bankPoint = deposit-box / bank approach tile
    // (null bankPoint => use the nearest full bank via Rs2Bank).
    AUTO("Auto (nearest spot)", null, null, BankingStrategy.DROP, false),

    // ---- F2P ----
    // Corsair Cove Resource Area lobster pier (base enum MYTHS_GUILD_NORTH). Deposit box captured
    // in-game at (2569,2862). Requires The Corsair Curse + Dragon Slayer I for the free walk
    // (no fare, no NPC/object interaction). The flagship F2P loop -- long but free bank walk.
    CORSAIR_COVE("Corsair Cove (F2P)",
            new WorldPoint(2456, 2893, 0), new WorldPoint(2569, 2862, 0),
            BankingStrategy.DEPOSIT_BOX, false),
    MUSA_POINT("Musa Point (F2P)",
            new WorldPoint(2925, 3179, 0), null,
            BankingStrategy.DROP, false),
    DRAYNOR_VILLAGE("Draynor Village (F2P)",
            new WorldPoint(3084, 3228, 0), null,
            BankingStrategy.FULL_BANK, false),
    AL_KHARID("Al Kharid (F2P)",
            new WorldPoint(3274, 3140, 0), null,
            BankingStrategy.DROP, false),

    // ---- P2P ----
    FISHING_GUILD("Fishing Guild (P2P 68)",
            new WorldPoint(2604, 3423, 0), null,
            BankingStrategy.FULL_BANK, true),
    CATHERBY("Catherby (P2P)",
            new WorldPoint(2836, 3431, 0), null,
            BankingStrategy.FULL_BANK, true),
    PISCATORIS("Piscatoris (P2P 62)",
            new WorldPoint(2308, 3700, 0), null,
            BankingStrategy.FULL_BANK, true),
    OTTOS_GROTTO("Otto's Grotto (P2P 48)",
            new WorldPoint(2500, 3509, 0), null,
            BankingStrategy.DROP, true);

    private final String displayName;
    private final WorldPoint worldPoint;
    private final WorldPoint bankPoint;
    private final BankingStrategy bankingStrategy;
    private final boolean membersOnly;

    FishingPlusLocation(String displayName, WorldPoint worldPoint, WorldPoint bankPoint,
                        BankingStrategy bankingStrategy, boolean membersOnly) {
        this.displayName = displayName;
        this.worldPoint = worldPoint;
        this.bankPoint = bankPoint;
        this.bankingStrategy = bankingStrategy;
        this.membersOnly = membersOnly;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
