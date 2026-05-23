// TEMPLATE FILE — see TEMPLATE.md. Replace package "skillplus" with your skill's package.
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

package net.runelite.client.plugins.microbot.skillplus;

enum State {
    /**
     * The skill's primary work loop is running (mining a rock, smelting a bar, cooking
     * a fish, etc.). Rename to your skill's verb at copy time.
     */
    ACTIVE,

    /**
     * Inventory full (or some other reset trigger), bot is in the deposit-and-return cycle.
     * Almost universal across skills — keep this name as-is.
     */
    RESETTING,
}
