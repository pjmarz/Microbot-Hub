package net.runelite.client.plugins.microbot.skillplus.data;

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
 * <p>WorldPoint coordinates are taken from OSRS Wiki bank-booth tiles. Members-only
 * banks are flagged with {@link #isMembersOnly()} so the script can warn or fall back
 * if running on a F2P world.
 */
@Getter
@RequiredArgsConstructor
public enum BankLocationOption {
    AUTO_NEAREST("Auto / nearest bank", null, false),

    LUMBRIDGE_CASTLE("Lumbridge Castle bank (2F, free)", new WorldPoint(3208, 3220, 2), false),
    DRAYNOR_VILLAGE("Draynor Village bank", new WorldPoint(3092, 3243, 0), false),
    AL_KHARID("Al Kharid bank (10 gp toll without Prince Ali Rescue)", new WorldPoint(3270, 3168, 0), false),
    VARROCK_WEST("Varrock West bank", new WorldPoint(3185, 3437, 0), false),
    VARROCK_EAST("Varrock East bank", new WorldPoint(3253, 3420, 0), false),
    GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3489, 0), false),
    EDGEVILLE("Edgeville bank", new WorldPoint(3094, 3492, 0), false),
    FALADOR_WEST("Falador West bank", new WorldPoint(2945, 3370, 0), false),
    FALADOR_EAST("Falador East bank", new WorldPoint(3013, 3355, 0), false),
    PORT_SARIM("Port Sarim bank (deposit box only on ground)", new WorldPoint(3045, 3236, 0), false),

    // Members banks
    SEERS_VILLAGE("Seers' Village bank (Members)", new WorldPoint(2722, 3493, 0), true),
    CATHERBY("Catherby bank (Members)", new WorldPoint(2810, 3441, 0), true),
    ARDOUGNE_NORTH("North Ardougne bank (Members)", new WorldPoint(2616, 3332, 0), true),
    ARDOUGNE_SOUTH("South Ardougne bank (Members)", new WorldPoint(2655, 3283, 0), true),
    BURGH_DE_ROTT("Burgh de Rott bank (Members)", new WorldPoint(3494, 3211, 0), true),
    SHILO_VILLAGE("Shilo Village bank (Members, Shilo Village quest)", new WorldPoint(2851, 2954, 0), true),
    MINING_GUILD_FALADOR("Mining Guild Falador entrance (Members)", new WorldPoint(3013, 3355, 0), true);

    private final String displayName;
    private final WorldPoint worldPoint;
    private final boolean membersOnly;

    @Override
    public String toString() {
        return displayName;
    }
}
