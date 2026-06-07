package net.runelite.client.plugins.microbot.smeltingplus;

enum State {
    /**
     * At the furnace, smelting bars. Triggered when inventory has materials for at least one bar.
     */
    SMELTING,

    /**
     * Inventory doesn't have materials for one more bar. Walk to bank, deposit, withdraw ores
     * (with coal-bag handling on members worlds), walk back to furnace.
     */
    RESETTING,
}
