package net.runelite.client.plugins.microbot.miningplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.miningplus.data.BankLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.LocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.MineLocationOption;
import net.runelite.client.plugins.microbot.miningplus.data.MiningRockLocations;
import net.runelite.client.plugins.microbot.miningplus.data.Rocks;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

enum State {
    MINING,
    RESETTING,
}

@Slf4j
public class AutoMiningPlusScript extends Script {

    private static final int GEM_MINE_UNDERGROUND = 11410;
    State state = State.MINING;
    private static final List<Rocks> PROGRESSIVE_ROCKS = buildProgressiveRocks();
    private Rocks activeRock;
    private LocationOption activeLocation;
    // True when the user picked a specific MineLocationOption that does not host the configured ore.
    // We still walk there per the user's pick, but updateStatus surfaces a clear warning.
    private boolean wrongOreLocation = false;

    // Polish-Cycle 2 v0.3.0: runtime stats tracking (read by AutoMiningPlusOverlay).
    private long startTimeMillis = 0;
    private int startSkillXp = 0;
    private int startSkillLevel = 0;
    private int actionsCompleted = 0;
    // v0.5.12: last-seen Mining XP, for the accurate per-ore counter (one ore per XP increase).
    // Seeded to startSkillXp in run().
    private int lastMiningXp = 0;

    // v0.5.0: set when targetLevel is reached; intercepted before state-flip-back in RESETTING
    // so we shutdown immediately after the cleanup pass (one bank or drop cycle).
    private boolean shutdownAfterCleanup = false;

    public long getStartTimeMillis() { return startTimeMillis; }
    public int getStartSkillXp() { return startSkillXp; }
    public int getStartSkillLevel() { return startSkillLevel; }
    public int getActionsCompleted() { return actionsCompleted; }

    public boolean run(AutoMiningPlusConfig config) {
        initialPlayerLocation = null;
        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();
        Rs2AntibanSettings.actionCooldownChance = 0.1;

        // Polish-Cycle 2 v0.3.0: seed stats trackers from client thread.
        startTimeMillis = System.currentTimeMillis();
        startSkillXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getSkillExperience(Skill.MINING)).orElse(0);
        startSkillLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getRealSkillLevel(Skill.MINING)).orElse(1);
        actionsCompleted = 0;
        lastMiningXp = startSkillXp; // v0.5.12: seed accurate ore counter
        shutdownAfterCleanup = false; // v0.5.0: reset target-level cleanup flag on startup

        // Speed mode: flip Microbot's master antiban switch off. Every check inside
        // Rs2Antiban.actionCooldown(), takeMicroBreakByChance(), naturalMouseMovement() etc.
        // short-circuits when antibanEnabled is false. Faster bot, more pattern-detectable.
        // Throwaway-only.
        if (config.speedMode()) {
            Rs2AntibanSettings.antibanEnabled = false;
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;

                // v0.5.1: pause check using global Microbot.pauseAllScripts (shared across all
                // plugins). Toggled via the overlay's Pause button. Stats keep accumulating
                // naturally -- runtime grows, XP/hr trends down. Unpause and the flow resumes.
                if (Microbot.pauseAllScripts.get()) {
                    Microbot.status = "[PAUSED]";
                    return;
                }

                // v0.5.12: accurate ore counter. Read Mining XP once per tick; each increase is one
                // ore obtained (one ore per successful mine, any ore type). The 100ms tick is well
                // below ore cadence (2.4s+), so one increment per XP drop is exact. Replaces the
                // v0.3.0 per-click "approximate" counter -- drives the overlay "Ores mined" stat AND
                // the stopAfterOres target. Reused for the stopAfterXp check below.
                int currentMiningXp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                        Microbot.getClient().getSkillExperience(Skill.MINING)).orElse(lastMiningXp);
                if (currentMiningXp > lastMiningXp) {
                    actionsCompleted++;
                    lastMiningXp = currentMiningXp;
                }

                // Polish-Cycle 2 v0.3.0: stopAfterMinutes / stopAfterXp threshold check.
                if (config.stopAfterMinutes() > 0
                        && (System.currentTimeMillis() - startTimeMillis) / 60000 >= config.stopAfterMinutes()) {
                    Microbot.log("AutoMiningPlus: reached stopAfterMinutes (" + config.stopAfterMinutes()
                            + " min). Shutting down.");
                    super.shutdown();
                    return;
                }
                if (config.stopAfterXp() > 0 && currentMiningXp - startSkillXp >= config.stopAfterXp()) {
                    Microbot.log("AutoMiningPlus: reached stopAfterXp (" + (currentMiningXp - startSkillXp)
                            + " XP). Shutting down.");
                    super.shutdown();
                    return;
                }

                // v0.5.0: target-level check. When Mining hits the target, run one cleanup
                // cycle (bank or drop) then shutdown. The shutdownAfterCleanup flag is checked
                // at the end of RESETTING just before the state-flip-back to MINING.
                if (config.targetLevel() > 0 && !shutdownAfterCleanup) {
                    int currentLevel = Microbot.getClientThread().runOnClientThreadOptional(() ->
                            Microbot.getClient().getRealSkillLevel(Skill.MINING)).orElse(startSkillLevel);
                    if (currentLevel >= config.targetLevel()) {
                        Microbot.log("AutoMiningPlus: reached targetLevel (" + currentLevel + " >= "
                                + config.targetLevel() + "). Banking/dropping inventory before shutdown.");
                        shutdownAfterCleanup = true;
                        if (Rs2Inventory.isEmpty()) {
                            super.shutdown();
                            return;
                        }
                        state = State.RESETTING;
                    }
                }

                // v0.5.12: stop-after-ores target. When the accurate ore counter hits the target,
                // run one cleanup pass (bank, or drop if UseBank off) then shutdown -- same flow as
                // targetLevel, so your ore ends up banked rather than left in the pack. 0 = disabled.
                if (config.stopAfterOres() > 0 && !shutdownAfterCleanup
                        && actionsCompleted >= config.stopAfterOres()) {
                    Microbot.log("AutoMiningPlus: reached stopAfterOres (" + actionsCompleted + " >= "
                            + config.stopAfterOres() + "). Banking/dropping inventory before shutdown.");
                    shutdownAfterCleanup = true;
                    if (Rs2Inventory.isEmpty()) {
                        super.shutdown();
                        return;
                    }
                    state = State.RESETTING;
                }

                if (config.leagueMode() && Rs2Player.checkIdleLogout(Rs2Random.between(500, 1500))) {
                    int[] arrowKeys = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
                    Rs2Keyboard.keyPress(arrowKeys[Rs2Random.between(0, arrowKeys.length - 1)]);
                }
                if (Rs2AntibanSettings.actionCooldownActive) return;
                if (initialPlayerLocation == null) {
                    initialPlayerLocation = Rs2Player.getWorldLocation();
                }

                // Skip cycle if we don't have a valid location
                if (initialPlayerLocation == null) {
                    return;
                }

                updateActiveRock(config);

                // Inventory-full check happens BEFORE the anchor pull so that a user who toggles
                // the script with a full inventory (e.g. mid-banking) goes straight to RESETTING
                // instead of being walked all the way to the mine just to walk straight back.
                if (Rs2Inventory.isFull() && state != State.RESETTING) {
                    state = State.RESETTING;
                }

                // Only re-anchor toward the mine while we're actively MINING. During RESETTING
                // the deposit branch is intentionally walking the player away (to the bank); if
                // we let this check run, it overrides the bank route as soon as the walker
                // arrives at the bank tile and the player stops moving.
                if (state == State.MINING && ensureConfiguredLocation(config)) {
                    return;
                }

                if (activeRock == null || !activeRock.hasRequiredLevel()) {
                    Microbot.log("You do not have the required mining level to mine this ore.");
                    return;
                }

                if (Rs2Equipment.isWearing("Dragon pickaxe"))
                    Rs2Combat.setSpecState(true, 1000);

                if (Rs2Player.isMoving() || Rs2Player.isAnimating()) return;

                //code to change worlds if there are too many players in the distance to stray tiles
                int maxPlayers = config.maxPlayersInArea();
                if (maxPlayers > 0) {
                    WorldPoint localLocation = Rs2Player.getWorldLocation();
                    long nearbyPlayers = Microbot.getClientThread().runOnClientThreadOptional(() ->
                                    Microbot.getClient().getTopLevelWorldView().players().stream()
                                            .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                                            .filter(p -> {
                                                if (config.distanceToStray() == 0) {
                                                    // Only count players standing on the same exact tile
                                                    return p.getWorldLocation().equals(localLocation);
                                                }
                                                // Count players within distanceToStray
                                                return p.getWorldLocation().distanceTo(localLocation) <= config.distanceToStray();
                                            })
                                            // filter if players are using mining animation
                                            .filter(p -> p.getAnimation() != -1)
                                            .count())
                            .orElse(0L);

                    if (nearbyPlayers >= maxPlayers) {
                        Microbot.status = "Too many players nearby. Hopping...";
                        Rs2Random.waitEx(3200, 800); // Delay to avoid UI locking

                        int world = Login.getRandomWorld(Rs2Player.isMember());
                        boolean hopped = Microbot.hopToWorld(world);
                        if (hopped) {
                            Microbot.status = "Hopped to world: " + world;
                            return; // Exit current cycle after hop
                        }
                    }
                }


                switch (state) {
                    case MINING:
                        if (Rs2Inventory.isFull()) {
                            state = State.RESETTING;
                            return;
                        }

                        if (activeRock == null) {
                            return;
                        }

                        // v0.3.1 switched from Rs2GameObject.findReachableObject (slow,
                        // reachability churn on crowded mines) to the v2.x Queryable cache for
                        // speed, but that dropped the reachability check: the nearest rock by raw
                        // distance can sit behind a P2P door we cannot path to (e.g. mithril at the
                        // Mining Guild). v0.5.13 keeps the fast cache query but filters to rocks
                        // with a reachable adjacent tile. We compute the reachable tiles once per
                        // tick via a strict BFS flood-fill from the mining anchor (the same origin
                        // the within() filter uses, so a strayed player can't shrink the set), with
                        // ignoreCollision=false so closed doors and walls stop the fill. The radius
                        // pads distanceToStray to allow path detours around obstacles. One BFS per
                        // tick, not one per rock, so no churn on crowded mines.
                        final Set<WorldPoint> reachable = (initialPlayerLocation == null)
                                ? Collections.emptySet()
                                : Rs2Tile.getReachableTilesFromTile(initialPlayerLocation, config.distanceToStray() + 12, false).keySet();

                        net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel rock =
                                Microbot.getRs2TileObjectCache().query()
                                        .within(initialPlayerLocation, config.distanceToStray())
                                        .withName(activeRock.getName())
                                        .where(r -> hasReachableAdjacent(r.getWorldLocation(), reachable))
                                        .nearestOnClientThread();

                        if (rock == null) {
                            // No rock found in stray range. If we've actually drifted off the
                            // mine, walk back. Otherwise just wait — neighboring miners
                            // may free a rock soon.
                            WorldPoint loc = Rs2Player.getWorldLocation();
                            if (loc != null && initialPlayerLocation != null
                                    && loc.distanceTo(initialPlayerLocation) > config.distanceToStray()) {
                                Microbot.status = "Walking back to mining location...";
                                Rs2Walker.walkTo(initialPlayerLocation, config.distanceToStray());
                            }
                            return;
                        }

                        if (rock.click("Mine")) {
                            // v0.5.12: ore counting moved to the accurate XP-drop detector at the top
                            // of the tick. The old per-click "approximate" actionsCompleted++ was here.
                            // Wait up to 1.2 sec for the swing to start. Don't wait for an
                            // XP drop — when the rock is depleted or another miner taps it
                            // first, Rs2Player.waitForXpDrop(skill, true) blocks for its full
                            // 5-10 sec timeout with no XP, wasting time. The outer loop's
                            // isAnimating() check pauses subsequent ticks while the swing
                            // plays out; once animation ends we pick a new rock.
                            Global.sleepUntil(Rs2Player::isAnimating, 1200);
                            Rs2Antiban.actionCooldown();
                            Rs2Antiban.takeMicroBreakByChance();
                        }
                        break;
                    case RESETTING:
                        List<String> itemNames = Arrays.stream(config.itemsToBank().split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());

                        if (config.useBank()) {
                            if (config.clayBracelet() && config.ORE() == Rocks.CLAY) {
                                if (!Rs2Bank.walkToBankAndUseBank()) {
                                    return;
                                }

                                // deposit all non-locked items to make room for bracelet of clay
                                Rs2Bank.depositAll();
                                if (Rs2Bank.hasItem(11074)) {
                                    Rs2Bank.withdrawAndEquip(11074);
                                }
                                else {
                                    log.info("You don't have any more bracelet of clays");
                                }
                                Rs2Bank.bankItemsAndWalkBackToOriginalPosition(itemNames, initialPlayerLocation, 0, config.distanceToStray());
                            }
                            else if (activeRock == Rocks.GEM && Rs2Player.getWorldLocation().getRegionID() == GEM_MINE_UNDERGROUND) {
                                if (Rs2DepositBox.openDepositBox()) {
                                    if (Rs2Inventory.contains("Open gem bag")) {
                                        Rs2Inventory.interact("Open gem bag", "Empty");
                                        Rs2DepositBox.depositAllExcept("Open gem bag");
                                    } else {
                                        Rs2DepositBox.depositAll();
                                    }
                                    Rs2DepositBox.closeDepositBox();
                                }
                            } else if (Rocks.BASALT == activeRock) {
                                if (Rs2Walker.walkTo(2872, 3935, 0)) {
                                    Rs2Inventory.useItemOnNpc(ItemID.BASALT, NpcID.MY2ARM_SNOWFLAKE);
                                    Rs2Walker.walkTo(2841, 10339, 0);
                                }
                            } else {
                                if (!Rs2Bank.isOpen()) {
                                    if (!walkToConfiguredBank(config)) {
                                        return;
                                    }
                                    return;
                                }

                                // v0.4.1 fix: auto-include the active rock's first-word as a filter
                                // term. Most OSRS ores match the user's default "ore" filter (Tin ore,
                                // Iron ore, etc. all contain "ore"), but Coal / Clay / Basalt are named
                                // literally "Coal" / "Clay" / "Basalt" without an "ore" suffix and slip
                                // through. The result was an oscillation: bot arrives at bank, deposit
                                // predicate matches nothing, bank closes empty, walks back to mine,
                                // inv still full, flips back to RESETTING, loops forever.
                                // Rocks.getName() returns e.g. "coal rocks" -- first word "coal" then
                                // substring-matches the inventory item "Coal".
                                List<String> filterNames = new ArrayList<>(itemNames);
                                if (activeRock != null && activeRock.getName() != null) {
                                    String firstWord = activeRock.getName().split("\\s+")[0].toLowerCase();
                                    if (!firstWord.isEmpty() && !filterNames.contains(firstWord)) {
                                        filterNames.add(firstWord);
                                    }
                                }

                                if (filterNames.isEmpty()) {
                                    Rs2Bank.depositAll();
                                } else {
                                    Rs2Bank.depositAll(i ->
                                            i.getName() != null &&
                                                    filterNames.stream().anyMatch(item -> i.getName().toLowerCase().contains(item)));
                                }

                                if (!Rs2Bank.closeBank())
                                    return;

                                Rs2Walker.walkTo(initialPlayerLocation, config.distanceToStray());
                            }

                        } else {
                            Rs2Inventory.dropAllExcept(false, config.interactOrder(), Arrays.stream(config.itemsToKeep().split(",")).map(String::trim).toArray(String[]::new));
                        }

                        // v0.5.0: targetLevel cleanup done -- shutdown before flipping back to MINING.
                        if (shutdownAfterCleanup) {
                            Microbot.log("AutoMiningPlus: targetLevel cleanup complete. Shutting down.");
                            super.shutdown();
                            return;
                        }

                        state = State.MINING;
                        break;
                }
            } catch (Exception ex) {
                Microbot.log(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    /**
     * True when at least one cardinally-adjacent tile of {@code rockTile} is in the reachable
     * set. Ore rocks occupy a blocked tile, so we test the tiles a miner could stand on. Used to
     * skip rocks behind doors or walls the player cannot path to (v0.5.13 mithril at the Mining
     * Guild fix). {@code reachable} is the strict BFS flood-fill from the mining anchor, so an
     * empty set (no anchor yet) filters everything out and the caller falls back to wait/walk-back.
     */
    private static boolean hasReachableAdjacent(WorldPoint rockTile, Set<WorldPoint> reachable) {
        if (rockTile == null || reachable.isEmpty()) {
            return false;
        }
        return reachable.contains(rockTile.dx(1))
                || reachable.contains(rockTile.dx(-1))
                || reachable.contains(rockTile.dy(1))
                || reachable.contains(rockTile.dy(-1));
    }

    private static List<Rocks> buildProgressiveRocks() {
        List<Rocks> rocks = new ArrayList<>(Arrays.asList(
                Rocks.TIN,
                Rocks.IRON,
                Rocks.COAL,
                Rocks.GOLD,
                Rocks.MITHRIL,
                Rocks.ADAMANTITE,
                Rocks.RUNITE
        ));
        return rocks;
    }

    private void updateActiveRock(AutoMiningPlusConfig config) {
        Rocks previousRock = activeRock;
        LocationOption previousLocation = activeLocation;

        if (config.progressiveMode()) {
            activeRock = PROGRESSIVE_ROCKS.stream()
                    .filter(Rocks::hasRequiredLevel)
                    .max(Comparator.comparingInt(Rocks::getMiningLevel))
                    .orElse(PROGRESSIVE_ROCKS.get(0));
        } else {
            activeRock = config.ORE();
        }

        MineLocationOption choice = config.mineLocation();
        activeLocation = resolveLocation(choice, activeRock);
        wrongOreLocation = choice != null
                && choice != MineLocationOption.AUTO_BEST
                && !choice.hostsRock(activeRock);
        if (wrongOreLocation) {
            log.warn("AutoMiningPlus: {} does not host {}. Walking there anyway; mining will idle until you switch ore or location.",
                    choice.name(), activeRock != null ? activeRock.name() : "null");
        }
        // Re-anchor the script's "home tile" to the resolved mine's WorldPoint each tick.
        // Without this, initialPlayerLocation stays pinned to wherever the user toggled the
        // script — bank lobby, login spawn, anywhere — and the MINING-case "walked too far
        // from start" check drags the player back to that spawn tile instead of letting them
        // mine at the configured location.
        if (activeLocation != null && activeLocation.getWorldPoint() != null) {
            initialPlayerLocation = activeLocation.getWorldPoint();
        }
        updateStatus();
    }

    private LocationOption resolveLocation(MineLocationOption choice, Rocks rock) {
        if (choice != null && choice != MineLocationOption.AUTO_BEST) {
            LocationOption resolved = choice.resolve(rock);
            if (resolved != null) {
                return resolved;
            }
        }
        return MiningRockLocations.getBestAccessibleLocation(rock);
    }

    /**
     * Routes to the player's configured bank when {@link AutoMiningPlusConfig#bankLocation()} is
     * set to a specific location; otherwise falls back to upstream's "nearest by raw distance"
     * behavior via {@link Rs2Bank#walkToBankAndUseBank()}.
     *
     * <p>Returns true when the bank is open and ready to deposit, false while still in transit.
     */
    private boolean walkToConfiguredBank(AutoMiningPlusConfig config) {
        BankLocationOption choice = config.bankLocation();
        if (choice == null || choice == BankLocationOption.AUTO_NEAREST) {
            return Rs2Bank.walkToBankAndUseBank();
        }

        WorldPoint bankPoint = choice.getWorldPoint();
        if (bankPoint == null) {
            return Rs2Bank.walkToBankAndUseBank();
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return false;
        }

        // Only x/y proximity; the bank chest itself handles plane via stairs/transports inside Rs2Walker.
        if (playerLocation.getX() != bankPoint.getX() || playerLocation.getY() != bankPoint.getY()
                || playerLocation.getPlane() != bankPoint.getPlane()) {
            if (playerLocation.distanceTo(bankPoint) > 4) {
                if (!Rs2Player.isMoving()) {
                    Rs2Walker.walkTo(bankPoint, 4);
                }
                return false;
            }
        }

        return Rs2Bank.openBank();
    }

    private boolean ensureConfiguredLocation(AutoMiningPlusConfig config) {
        if (activeLocation == null || activeLocation.getWorldPoint() == null) {
            return false;
        }

        WorldPoint targetPoint = activeLocation.getWorldPoint();

        // Only update initialPlayerLocation if it's null
        // Don't update just because player is far away (e.g., at bank) - that breaks return-to-location
        if (initialPlayerLocation == null) {
            initialPlayerLocation = targetPoint;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return true;
        }

        // Honor the user's distanceToStray as the "close enough to the anchor" tolerance.
        // The upstream cap of 5 tiles was holdover from progressive mode and caused yo-yo
        // walking: the mining loop finds rocks up to distanceToStray tiles away, walks to
        // them, then this method dragged the player back to the anchor between mines.
        int acceptableDistance = Math.max(1, config.distanceToStray());
        int distanceToTarget = playerLocation.distanceTo(targetPoint);
        if (distanceToTarget > acceptableDistance) {
            if (Rs2Player.isMoving()) {
                return true;
            }

            Rs2Walker.walkTo(targetPoint, 3);
            return true;
        }

        return false;
    }

    private void updateStatus() {
        String oreName = activeRock != null ? activeRock.getName() : "Unknown";
        String locationName = (activeLocation != null && activeLocation.getName() != null)
                ? activeLocation.getName()
                : "current area";
        if (wrongOreLocation) {
            Microbot.status = "WRONG ORE: no " + oreName + " at " + locationName
                    + " — change ore or location";
        } else {
            Microbot.status = "Mining " + oreName + " @ " + locationName;
        }
    }
}
