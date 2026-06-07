package net.runelite.client.plugins.microbot.eventdismissplus.events;

import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusConfig;
import net.runelite.client.plugins.microbot.eventdismissplus.EventDismissPlusScript;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/**
 * BlockingEvent for the Strange Plant random event. Unlike other random events, this is a
 * {@code GameObject} (not an NPC), so {@code Rs2Npc.getRandomEventNPC()} doesn't catch it.
 *
 * <p>Detection: query the tile-object cache for {@link net.runelite.api.ObjectID#STRANGE_PLANT}.
 * When found within a small radius of the player AND engagement is enabled, click "Pick" to
 * harvest the fruit (30% run-energy restore).
 *
 * <p>If engagement is disabled, this handler returns false from {@code validate()} so the
 * BlockingEventManager doesn't interrupt anything for it. The plant despawns on its own
 * after ~60 seconds.
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Harvests via the "Pick" action (the standard Strange Plant option). If the click cannot be
 *       made it logs and bails rather than blocking.</li>
 * </ul>
 */
public class StrangePlantHandler implements BlockingEvent {

    private static final int STRANGE_PLANT_OBJECT_ID = ObjectID.STRANGE_PLANT;
    private static final int SEARCH_RADIUS_TILES = 20;

    private final EventDismissPlusConfig config;
    private final EventDismissPlusScript script;

    public StrangePlantHandler(EventDismissPlusConfig config, EventDismissPlusScript script) {
        this.config = config;
        this.script = script;
    }

    private Rs2TileObjectModel findStrangePlant() {
        // BlockingEventManager calls validate() during login, before the local player exists, where
        // Rs2Player.getWorldLocation() itself throws (refreshLocalPlayer NPEs on the null player).
        // Short-circuit when not logged in and swallow that login-race exception so it doesn't spam
        // the BlockingEvent error log.
        if (!Microbot.isLoggedIn()) return null;
        WorldPoint playerLoc;
        try {
            playerLoc = Rs2Player.getWorldLocation();
        } catch (Exception ignored) {
            return null;
        }
        if (playerLoc == null) return null;
        return Microbot.getRs2TileObjectCache().query()
                .within(playerLoc, SEARCH_RADIUS_TILES)
                .where(t -> t.getId() == STRANGE_PLANT_OBJECT_ID)
                .nearestOnClientThread();
    }

    @Override
    public boolean validate() {
        if (!config.engageStrangePlant()) return false;
        return findStrangePlant() != null;
    }

    @Override
    public boolean execute() {
        var plant = findStrangePlant();
        if (plant == null) return true;

        // Variable response delay (anti-detection)
        int delayMin = Math.max(0, config.responseDelayMin());
        int delayMax = Math.max(delayMin, config.responseDelayMax());
        if (delayMax > 0) {
            Global.sleep(Rs2Random.between(delayMin, delayMax));
        }

        plant = findStrangePlant();
        if (plant == null) return true;

        try {
            if (!plant.click("Pick")) {
                Microbot.log("StrangePlantHandler: 'Pick' action failed on object id "
                        + STRANGE_PLANT_OBJECT_ID + ". Investigate action verb.");
                return true;
            }
            Global.sleepUntil(() -> findStrangePlant() == null, 4000);
            script.recordEventHandled("Strange Plant");
        } catch (Exception ex) {
            Microbot.log("StrangePlantHandler error: " + ex.getMessage());
        }

        return !validate();
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.LOWEST;
    }
}
