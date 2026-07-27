package bruhof.teenycraft.screen;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SilkieStationSnapshot(
        SilkieStationTargetRef targetRef,
        ItemStack targetStack,
        List<SilkieStationFigureView> activeTargets,
        List<SilkieStationFigureView> favoriteTargets,
        int favoritePage,
        int favoritePageCount,
        List<SilkieStationFigureView> offerings,
        List<SilkieStationFigureView> candidates,
        int browserPage,
        int browserPageCount,
        int browserTotal,
        SilkieStationSortMode sortMode,
        String searchQuery,
        List<SilkieStationAbilityView> abilities,
        int selectedAbility,
        int basePoints,
        int exactCount,
        int exactBonus,
        int classComboCount,
        int classBonus,
        List<SilkieStationGroupView> groups,
        int totalPoints,
        int appliedPoints,
        int overflowPoints,
        boolean hasInvestedSacrifice,
        boolean canFeed,
        String statusMessage
) {
    public SilkieStationSnapshot {
        targetStack = targetStack.copy();
        activeTargets = List.copyOf(activeTargets);
        favoriteTargets = List.copyOf(favoriteTargets);
        offerings = List.copyOf(offerings);
        candidates = List.copyOf(candidates);
        abilities = List.copyOf(abilities);
        groups = List.copyOf(groups);
        searchQuery = searchQuery == null ? "" : searchQuery;
        statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public static SilkieStationSnapshot empty() {
        return new SilkieStationSnapshot(null, ItemStack.EMPTY, List.of(), List.of(), 0, 1,
                List.of(), List.of(), 0, 1, 0, SilkieStationSortMode.RECOMMENDED, "",
                List.of(), -1, 0, 0, 0, 0, 0, List.of(), 0, 0, 0,
                false, false, "Select a target figure.");
    }
}
