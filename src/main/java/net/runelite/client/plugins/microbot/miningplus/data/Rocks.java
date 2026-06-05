package net.runelite.client.plugins.microbot.miningplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum Rocks {
    // oreItemId is the raw item the rock yields, used by the overlay for the GP/hr line via the
    // GE price. 0 means "no single priced ore" (gem rocks yield mixed gems; basalt and salts have
    // no standard GE price), so the overlay shows GP/hr 0 for those rather than guessing.
    TIN("tin rocks", 1, ItemID.TIN_ORE),
    COPPER("copper rocks", 1, ItemID.COPPER_ORE),
    CLAY("clay rocks", 1, ItemID.CLAY),
    IRON("iron rocks", 15, ItemID.IRON_ORE),
    SILVER("silver rocks", 20, ItemID.SILVER_ORE),
    COAL("coal rocks", 30, ItemID.COAL),
    GOLD("gold rocks", 40, ItemID.GOLD_ORE),
    GEM("gem rocks", 40, 0),
    MITHRIL("mithril rocks", 55, ItemID.MITHRIL_ORE),
    ADAMANTITE("adamantite rocks", 70, ItemID.ADAMANTITE_ORE),
    BASALT("Basalt rocks", 72, 0),
    URT_SALT("Urt salt rocks", 72, 0),
    EFH_SALT("Efh salt rocks", 72, 0),
    TE_SALT("Te salt rocks", 72, 0),
    RUNITE("runite rocks", 85, ItemID.RUNITE_ORE),
    NONE("None", 1, 0);

    private final String name;
    private final int miningLevel;
    private final int oreItemId;

    @Override
    public String toString() {
        return name;
    }

    public boolean hasRequiredLevel() {
        return Rs2Player.getSkillRequirement(Skill.MINING, this.miningLevel);
    }
}
