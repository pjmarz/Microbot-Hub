package net.runelite.client.plugins.microbot.runecraftplus;

/**
 * Runecraft loop phases. BANKING doubles as the reset/deposit step; targetLevel cleanup routes
 * through BANKING for a final deposit before shutdown.
 */
enum State
{
    BANKING,
    WALKING_TO_ALTAR,
    ENTERING_ALTAR,
    CRAFTING,
    EXITING_ALTAR
}
