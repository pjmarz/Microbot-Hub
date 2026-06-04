package net.runelite.client.plugins.microbot.miningplus.data;

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
 *
 * <h2>Audit log</h2>
 * <ul>
 *   <li><b>2026-05-21 (v0.3.3):</b> Added Fadli's Al Kharid Arena bank (F2P, ~75 tiles from
 *       Al Kharid Mine vs ~150 to south-city bank) after Pete called out the gap post-deploy.
 *       Coord (3315, 3242, 0) is approximate, needs in-game verification.</li>
 *   <li><b>2026-05-21 (v0.4.0 -- wiki-driven data expansion):</b> Fixed MINING_GUILD_FALADOR
 *       coord collision with FALADOR_EAST -- new coord (3046, 9760, 0) targets the underground
 *       chest area (stays members-only; F2P side of guild has no bank per wiki). Fixed
 *       AL_KHARID_ARENA coord (was (3315, 3242, 0) which sent the walker to a NW residential
 *       building; corrected to (3383, 3269, 0)). Live verification on xitzpjmarz confirmed 3
 *       banking entities cluster at Emir's Arena: 2 full bank chests at (3382, 3270, 0) and
 *       (3381, 3269, 0), plus a deposit-only box at (3385, 3272, 0). The corrected coord
 *       biases toward the chests, so withdraw-required cycles (smelting, smithing) work at
 *       Fadli too -- not just mining. Added FEROX_ENCLAVE (F2P safe-zone bank at Wilderness
 *       gateway). New audit script {@code tools/wiki-audit-banks.ps1} ships this version.</li>
 *   <li>Re-audit via {@code tools/wiki-audit-banks.ps1} before each minor version bump.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum BankLocationOption {
    AUTO_NEAREST("Auto / nearest", null, false),

    // v0.3.2 (display polish): shortened to fit the RuneLite dropdown width.
    // Critical qualifiers kept in parens; "bank" suffix dropped where redundant.
    LUMBRIDGE_CASTLE("Lumbridge Castle (2F)", new WorldPoint(3208, 3220, 2), false),
    DRAYNOR_VILLAGE("Draynor Village", new WorldPoint(3092, 3243, 0), false),
    AL_KHARID("Al Kharid (10gp toll)", new WorldPoint(3270, 3168, 0), false),
    // v0.3.3: Fadli's bank at Emir's Arena -- F2P, much closer to Al Kharid Mine
    // (~75 tiles from mine vs ~150 to the south-city bank).
    // v0.4.0 (coord fix + entity audit): prior (3315, 3242, 0) landed the walker in a NW
    // residential building far from the arena. Verified live by Pete on xitzpjmarz that 3
    // banking entities cluster here:
    //   - Bank chest #1 at (3382, 3270, 0) -- full bank (deposit + withdraw); matches wiki Fadli map
    //   - Bank chest #2 at (3381, 3269, 0) -- full bank (deposit + withdraw)
    //   - Deposit Box   at (3385, 3272, 0) -- deposit-only
    // Our coord (3383, 3269, 0) sits 1-2 tiles east of both chests; Rs2Bank.openBank() nearest-
    // entity search biases toward a bank chest, so withdraw-required cycles (smelting,
    // smithing) work at Fadli too, not just mining.
    AL_KHARID_ARENA("Al Kharid Arena", new WorldPoint(3383, 3269, 0), false),
    VARROCK_WEST("Varrock West", new WorldPoint(3185, 3437, 0), false),
    VARROCK_EAST("Varrock East", new WorldPoint(3253, 3420, 0), false),
    GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3489, 0), false),
    EDGEVILLE("Edgeville", new WorldPoint(3094, 3492, 0), false),
    FALADOR_WEST("Falador West", new WorldPoint(2945, 3370, 0), false),
    FALADOR_EAST("Falador East", new WorldPoint(3013, 3355, 0), false),
    PORT_SARIM("Port Sarim (depot)", new WorldPoint(3045, 3236, 0), false),
    // v0.4.0: Ferox Enclave bank (F2P safe zone at the Wilderness gateway, lvl 0-16).
    // Wiki: "A banker and bank chest can be found on the western end" of the enclave.
    // Coord (3128, 3637, 0) is wiki-derived approximation -- western end of enclave town.
    // WARNING: Rs2Walker may path through lvl 1-2 Wilderness on approach. Safe inside enclave.
    // Needs in-game verification on xitzpjmarz.
    FEROX_ENCLAVE("Ferox Enclave", new WorldPoint(3128, 3637, 0), false),

    // Members banks
    SEERS_VILLAGE("Seers' Village (P2P)", new WorldPoint(2722, 3493, 0), true),
    CATHERBY("Catherby (P2P)", new WorldPoint(2810, 3441, 0), true),
    ARDOUGNE_NORTH("N. Ardougne (P2P)", new WorldPoint(2616, 3332, 0), true),
    ARDOUGNE_SOUTH("S. Ardougne (P2P)", new WorldPoint(2655, 3283, 0), true),
    BURGH_DE_ROTT("Burgh de Rott (P2P)", new WorldPoint(3494, 3211, 0), true),
    SHILO_VILLAGE("Shilo Village (P2P)", new WorldPoint(2851, 2954, 0), true),
    // v0.4.0 (bug fix): prior coord (3013, 3355, 0) was identical to FALADOR_EAST -- stale
    // seed that silently routed the user to the wrong bank. The Mining Guild bank chest is
    // underground in the members-only section (F2P side has only shops + rocks per wiki).
    // New coord targets the underground chest vicinity near the mining floor
    // ((3046, 9756, 0) in MineLocationOption.MINING_GUILD). Nudged +4 Y toward the chest.
    // Still needs in-game verification on the next P2P session.
    MINING_GUILD_FALADOR("Mining Guild (P2P)", new WorldPoint(3046, 9760, 0), true);

    private final String displayName;
    private final WorldPoint worldPoint;
    private final boolean membersOnly;

    @Override
    public String toString() {
        return displayName;
    }
}
