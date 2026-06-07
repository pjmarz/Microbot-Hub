package net.runelite.client.plugins.microbot.smithingplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * User-selectable preferred bank. Defaults to {@link #AUTO_NEAREST} which preserves
 * upstream {@code Rs2Bank.walkToBankAndUseBank()} behavior (nearest bank by raw distance,
 * which often picks toll-gated banks like Al Kharid over slightly-further free banks).
 *
 * <p>Pick a specific bank to override. The script walks to {@link #getWorldPoint()}
 * and opens the bank there.
 *
 * <p>Copied from the {@code miningplus} / {@code smeltingplus} skill-agnostic dataset.
 * Coordinates from OSRS Wiki bank-booth tiles.
 */
@Getter
@RequiredArgsConstructor
public enum BankLocationOption {
    AUTO_NEAREST("Auto / nearest", null, false),

    LUMBRIDGE_CASTLE("Lumbridge Castle (2F)", new WorldPoint(3208, 3220, 2), false),
    DRAYNOR_VILLAGE("Draynor Village", new WorldPoint(3092, 3243, 0), false),
    AL_KHARID("Al Kharid (10gp toll)", new WorldPoint(3270, 3168, 0), false),
    // Fadli's bank at Emir's Arena: F2P, much closer to Al Kharid Mine. Three banking entities
    // cluster here:
    //   - Bank chest #1 at (3382, 3270, 0): full bank (deposit + withdraw)
    //   - Bank chest #2 at (3381, 3269, 0): full bank (deposit + withdraw)
    //   - Deposit Box   at (3385, 3272, 0): deposit-only
    // This coord sits 1-2 tiles east of both chests; Rs2Bank.openBank() nearest-entity search
    // biases toward a bank chest, so withdraw-required cycles (smelting, smithing) work here too,
    // not just mining.
    AL_KHARID_ARENA("Al Kharid Arena", new WorldPoint(3383, 3269, 0), false),
    VARROCK_WEST("Varrock West", new WorldPoint(3185, 3437, 0), false),
    VARROCK_EAST("Varrock East", new WorldPoint(3253, 3420, 0), false),
    GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3489, 0), false),
    EDGEVILLE("Edgeville", new WorldPoint(3094, 3492, 0), false),
    FALADOR_WEST("Falador West", new WorldPoint(2945, 3370, 0), false),
    FALADOR_EAST("Falador East", new WorldPoint(3013, 3355, 0), false),
    PORT_SARIM("Port Sarim (depot)", new WorldPoint(3045, 3236, 0), false),
    // Ferox Enclave bank (F2P safe zone at the Wilderness gateway, lvl 0-16). A banker and bank
    // chest sit on the western end of the enclave.
    // WARNING: Rs2Walker may path through lvl 1-2 Wilderness on approach. Safe inside enclave.
    FEROX_ENCLAVE("Ferox Enclave", new WorldPoint(3128, 3637, 0), false),

    // Members banks
    SEERS_VILLAGE("Seers' Village (P2P)", new WorldPoint(2722, 3493, 0), true),
    CATHERBY("Catherby (P2P)", new WorldPoint(2810, 3441, 0), true),
    ARDOUGNE_NORTH("N. Ardougne (P2P)", new WorldPoint(2616, 3332, 0), true),
    ARDOUGNE_SOUTH("S. Ardougne (P2P)", new WorldPoint(2655, 3283, 0), true),
    BURGH_DE_ROTT("Burgh de Rott (P2P)", new WorldPoint(3494, 3211, 0), true),
    SHILO_VILLAGE("Shilo Village (P2P)", new WorldPoint(2851, 2954, 0), true),
    // The Mining Guild bank chest is underground in the members-only section (the F2P side has
    // only shops + rocks). This coord targets the underground chest vicinity near the mining floor
    // ((3046, 9756, 0) in MineLocationOption.MINING_GUILD), nudged +4 Y toward the chest.
    MINING_GUILD_FALADOR("Mining Guild (P2P)", new WorldPoint(3046, 9760, 0), true);

    private final String displayName;
    private final WorldPoint worldPoint;
    private final boolean membersOnly;

    @Override
    public String toString() {
        return displayName;
    }
}
