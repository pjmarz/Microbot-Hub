package net.runelite.client.plugins.microbot.smeltingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-selectable preferred bank. Defaults to {@link #AUTO_NEAREST} which preserves
 * {@code Rs2Bank.walkToBankAndUseBank()} behavior (nearest bank by raw distance, which often
 * picks toll-gated banks like Al Kharid over slightly-further free banks).
 *
 * <p>Pick a specific bank to override. The script walks to {@link #getWorldPoint()} and opens
 * the bank there. WorldPoint coordinates are taken from OSRS Wiki bank-booth tiles.
 */
@Getter
@RequiredArgsConstructor
public enum BankLocationOption {
    AUTO_NEAREST("Auto / nearest", null),

    LUMBRIDGE_CASTLE("Lumbridge Castle (2F)", new WorldPoint(3208, 3220, 2)),
    DRAYNOR_VILLAGE("Draynor Village", new WorldPoint(3092, 3243, 0)),
    AL_KHARID("Al Kharid (10gp toll)", new WorldPoint(3270, 3168, 0)),
    // Fadli's bank at Emir's Arena: F2P, much closer to Al Kharid Mine. The coord sits 1-2 tiles
    // east of two bank chests; Rs2Bank.openBank() biases toward a chest, so withdraw-required
    // cycles work here too, not just deposits.
    AL_KHARID_ARENA("Al Kharid Arena", new WorldPoint(3383, 3269, 0)),
    VARROCK_WEST("Varrock West", new WorldPoint(3185, 3437, 0)),
    VARROCK_EAST("Varrock East", new WorldPoint(3253, 3420, 0)),
    GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3489, 0)),
    EDGEVILLE("Edgeville", new WorldPoint(3094, 3492, 0)),
    FALADOR_WEST("Falador West", new WorldPoint(2945, 3370, 0)),
    FALADOR_EAST("Falador East", new WorldPoint(3013, 3355, 0)),
    PORT_SARIM("Port Sarim (depot)", new WorldPoint(3045, 3236, 0)),
    // Ferox Enclave bank (F2P safe zone at the Wilderness gateway). WARNING: Rs2Walker may path
    // through low-level Wilderness on approach. Safe inside the enclave.
    FEROX_ENCLAVE("Ferox Enclave", new WorldPoint(3128, 3637, 0)),

    // Members banks
    SEERS_VILLAGE("Seers' Village (P2P)", new WorldPoint(2722, 3493, 0)),
    CATHERBY("Catherby (P2P)", new WorldPoint(2810, 3441, 0)),
    ARDOUGNE_NORTH("N. Ardougne (P2P)", new WorldPoint(2616, 3332, 0)),
    ARDOUGNE_SOUTH("S. Ardougne (P2P)", new WorldPoint(2655, 3283, 0)),
    BURGH_DE_ROTT("Burgh de Rott (P2P)", new WorldPoint(3494, 3211, 0)),
    SHILO_VILLAGE("Shilo Village (P2P)", new WorldPoint(2851, 2954, 0)),
    // The Mining Guild bank chest is underground in the members-only section (the F2P side has
    // only shops and rocks). Coord targets the underground chest vicinity near the mining floor.
    MINING_GUILD_FALADOR("Mining Guild (P2P)", new WorldPoint(3046, 9760, 0));

    private final String displayName;
    private final WorldPoint worldPoint;

    @Override
    public String toString() {
        return displayName;
    }
}
