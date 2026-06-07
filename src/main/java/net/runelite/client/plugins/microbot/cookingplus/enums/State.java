package net.runelite.client.plugins.microbot.cookingplus.enums;

/**
 * State machine for AutoCookingPlus.
 *
 * <ul>
 *   <li>{@link #COOKING} - at a range/fire with raw food, cook it all.</li>
 *   <li>{@link #WALKING} - travel to the chosen cooking spot.</li>
 *   <li>{@link #BANKING} - deposit cooked food, withdraw a fresh load of raw food.</li>
 *   <li>{@link #DROPPING_BURNT} - drop burnt food before banking (when enabled).</li>
 *   <li>{@link #RESETTING} - a stop condition fired; do one clean deposit/drop pass then shut down.</li>
 * </ul>
 */
public enum State {
    COOKING,
    WALKING,
    BANKING,
    DROPPING_BURNT,
    RESETTING,
}
