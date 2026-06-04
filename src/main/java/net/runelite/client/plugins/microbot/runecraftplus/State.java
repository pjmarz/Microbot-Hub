package net.runelite.client.plugins.microbot.runecraftplus;

/**
 * Runecraft loop phases. BANKING doubles as the reset/deposit step (the "RESETTING" of the suite
 * pattern). targetLevel cleanup routes through BANKING for a final deposit before shutdown.
 */
enum State
{
    BANKING,
    WALKING_TO_ALTAR,
    ENTERING_ALTAR,
    CRAFTING,
    EXITING_ALTAR
}
