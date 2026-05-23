//
// Pattern from AutoMiningPlus: keep the enum small. Two states (work + bank cycle) covers
// every gathering and stationary skill in the Hub. If you need 3+ phases with guard
// conditions, switch to StateMachineScript<S> instead of Script.
//
// Per-skill naming convention:
//   - Mining → MINING / RESETTING
//   - Smelting → SMELTING / WITHDRAWING / RESETTING
//   - Cooking → COOKING / RESETTING
//   - Fishing → FISHING / RESETTING
//   - Fletching → FLETCHING / RESETTING
//
// The skeleton uses generic ACTIVE/RESETTING. Rename ACTIVE to your skill's verb at copy time.

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
