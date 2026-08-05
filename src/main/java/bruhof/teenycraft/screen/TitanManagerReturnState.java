package bruhof.teenycraft.screen;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The menu-local view settings needed to return from an item detail screen
 * without losing the player's place in the Titan Manager.
 */
public record TitanManagerReturnState(TitanManagerTab activeTab,
                                      TitanManagerSortMode sortMode,
                                      boolean favoritesOnly,
                                      String searchQuery,
                                      String figureClassFilter,
                                      int pageIndex) {
    public TitanManagerReturnState {
        activeTab = activeTab == null ? TitanManagerTab.FIGURES : activeTab;
        sortMode = sortMode == null ? TitanManagerSortMode.getDefault(activeTab) : sortMode;
        searchQuery = truncate(searchQuery, 32);
        figureClassFilter = truncate(figureClassFilter, 64);
        pageIndex = Math.max(0, pageIndex);
    }

    public static TitanManagerReturnState defaults() {
        return new TitanManagerReturnState(
                TitanManagerTab.FIGURES,
                TitanManagerSortMode.getDefault(TitanManagerTab.FIGURES),
                false,
                "",
                "",
                0);
    }

    public static TitanManagerReturnState capture(TitanManagerViewState viewState) {
        return new TitanManagerReturnState(
                viewState.getActiveTab(),
                viewState.getSortMode(),
                viewState.isFavoritesOnly(),
                viewState.getSearchQuery(),
                viewState.getFigureClassFilter(),
                viewState.getPageIndex());
    }

    public static TitanManagerReturnState read(FriendlyByteBuf buffer) {
        return new TitanManagerReturnState(
                buffer.readEnum(TitanManagerTab.class),
                buffer.readEnum(TitanManagerSortMode.class),
                buffer.readBoolean(),
                buffer.readUtf(32),
                buffer.readUtf(64),
                buffer.readVarInt());
    }

    public static TitanManagerReturnState readOptional(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? read(buffer) : null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(activeTab);
        buffer.writeEnum(sortMode);
        buffer.writeBoolean(favoritesOnly);
        buffer.writeUtf(searchQuery, 32);
        buffer.writeUtf(figureClassFilter, 64);
        buffer.writeVarInt(pageIndex);
    }

    public static void writeOptional(FriendlyByteBuf buffer, TitanManagerReturnState state) {
        buffer.writeBoolean(state != null);
        if (state != null) {
            state.write(buffer);
        }
    }

    public void applyTo(TitanManagerViewState viewState) {
        viewState.setActiveTab(activeTab);
        viewState.setSortMode(sortMode);
        viewState.setFavoritesOnly(favoritesOnly);
        viewState.setSearchQuery(searchQuery);
        viewState.setFigureClassFilter(figureClassFilter);
        viewState.setPageIndex(pageIndex);
    }

    private static String truncate(String value, int maxLength) {
        String safeValue = value == null ? "" : value;
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }
}
