// TEMPLATE FILE — see TEMPLATE.md. Replace package "skillplus" with your skill's package and
// rename the class to <Skill>LocationOption (e.g. MineLocationOption, FurnaceLocationOption,
// FishingSpotLocationOption).
//
// This is a user-facing dropdown of named locations for your skill. Each constant carries:
//   - displayName: shown in the config dropdown
//   - locationName: matches the corresponding entry's name in your <Skill>Locations dataset
//                   (the upstream data class your script reads from)
//   - worldPoint:   canonical tile so the bot can walk there even when the dataset lookup
//                   fails (e.g. user picked a wrong-target combo). Always non-null except for
//                   the AUTO_BEST sentinel.
//
// PATTERN REFERENCE: net/runelite/client/plugins/microbot/miningplus/data/MineLocationOption.java
// (in AutoMiningPlus v0.1.11). Copy that as a starting point and substitute your skill's terms.
//
// Why both `locationName` AND `worldPoint`?
//   - The locationName matches a named entry in your <Skill>Locations dataset so resolve() can
//     return a fully-equipped LocationOption with quest/skill/varbit gates.
//   - The worldPoint is the fallback. If the user picks a location that doesn't host the
//     configured target (e.g. ore X at a mine that doesn't have X), resolve() still returns a
//     LocationOption pointed at the worldPoint so the walker takes the user where they asked.
//     The caller checks hostsTarget() and surfaces a status warning.

package net.runelite.client.plugins.microbot.skillplus.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

@Getter
@RequiredArgsConstructor
public enum SkillLocationOption {
    // The AUTO_BEST sentinel falls through to upstream's "nearest accessible by raw distance"
    // helper (e.g. MiningRockLocations.getBestAccessibleLocation(target)). All other entries
    // are explicit user picks.
    AUTO_BEST("Auto / best accessible", null, null),

    // TODO(plus): one constant per named location in your <Skill>Locations dataset.
    //
    // Example shape (from MineLocationOption in AutoMiningPlus):
    //
    //   LUMBRIDGE_SWAMP_WEST_MINE(
    //       "Lumbridge Swamp West Mine",      // displayName (dropdown label)
    //       "Lumbridge Swamp West Mine",      // locationName (matches MiningRockLocations entry)
    //       new WorldPoint(3149, 3148, 0)),   // canonical tile
    //
    //   MINING_GUILD(
    //       "Mining Guild (Members, lvl 60)",
    //       "Mining Guild (Members)",
    //       new WorldPoint(3046, 9756, 0)),
    //
    // The displayName can be different from locationName — use displayName to surface
    // members/quest requirements to the user. locationName MUST equal the exact string
    // the upstream dataset uses, or resolve() will silently miss and fall back.
    PLACEHOLDER("Placeholder — replace me", "TODO replace with dataset entry name", new WorldPoint(0, 0, 0));

    private final String displayName;
    private final String locationName;
    private final WorldPoint worldPoint;

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Returns true if this location is in your <Skill>Locations dataset's list for the given
     * target. Use this in your script's updateActiveTarget() to detect target-vs-location
     * mismatches and surface a UI warning (e.g. "WRONG TARGET: no <target> at <location>").
     *
     * <p>TODO(plus): swap {@code Object target} for your skill's target type (Rocks for mining,
     * Bars for smelting, Fish for fishing, etc.) and call your dataset's
     * {@code getLocationsForTarget(target)} method.
     */
    public boolean hostsTarget(Object target) {
        if (this == AUTO_BEST || target == null || locationName == null) {
            return true;
        }
        // TODO(plus): replace with:
        //   return <Skill>Locations.getLocationsForTarget(target).stream()
        //           .anyMatch(opt -> locationName.equals(opt.getName()));
        return true;
    }

    /**
     * Resolves this dropdown choice to a {@link net.runelite.client.plugins.microbot.miningplus.data.LocationOption}.
     *
     * <p>If the target is hosted at this location, return the canonical LocationOption with all
     * quest/skill/varbit/item gates intact. If the target is NOT hosted, return a minimal
     * LocationOption backed by just the worldPoint so the walker takes the user where they
     * asked; the caller surfaces a wrong-target warning via {@link #hostsTarget}.
     *
     * <p>Return null only for {@link #AUTO_BEST}, signaling the caller should defer to
     * upstream's "best accessible" helper.
     *
     * <p>TODO(plus): adapt the parameter type and dataset lookup. Pattern reference:
     * {@code MineLocationOption.resolve(Rocks rock)} in AutoMiningPlus.
     */
    // public LocationOption resolve(Target target) {
    //     if (this == AUTO_BEST) return null;
    //     if (target != null) {
    //         LocationOption matched = <Skill>Locations.getLocationsForTarget(target).stream()
    //                 .filter(opt -> locationName.equals(opt.getName()))
    //                 .findFirst()
    //                 .orElse(null);
    //         if (matched != null) return matched;
    //     }
    //     return new LocationOption(worldPoint, locationName, false);
    // }
}
