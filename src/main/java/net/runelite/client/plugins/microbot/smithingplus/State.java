// Pattern from AutoMiningPlus / AutoSmeltingPlus: keep the state enum small. Two states (work +
// bank cycle) covers every gathering and stationary skill in the Hub. If you need 3+ phases
// with guard conditions, switch to StateMachineScript<S> instead of Script.

package net.runelite.client.plugins.microbot.smithingplus;

enum State {
    /**
     * At the anvil with bars + hammer, smithing items. Triggered when inventory has at least
     * the {@code requiredBars} for one craft of the selected item AND has a hammer.
     */
    SMITHING,

    /**
     * Inventory is full of smithed items, or we lack the bars/hammer to craft. Walk to bank,
     * deposit smithed items, withdraw bars (and hammer if missing), walk back to anvil.
     */
    RESETTING,
}
