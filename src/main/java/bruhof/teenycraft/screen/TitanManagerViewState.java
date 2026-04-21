package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerStorageSlot;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TitanManagerViewState {
    public static final int PAGE_SIZE = 54;

    private static final String TAG_FIGURE_SEQUENCE = "TitanFigureSequence";

    private TitanManagerTab activeTab = TitanManagerTab.FIGURES;
    private TitanManagerSortMode sortMode = TitanManagerSortMode.NAME;
    private boolean favoritesOnly = false;
    private String searchQuery = "";
    private String figureClassFilter = "";
    private int pageIndex = 0;
    private int totalResults = 0;
    private int pageCount = 1;
    private final List<TitanManagerStorageSlot> visibleSlots = new ArrayList<>();

    public TitanManagerTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(TitanManagerTab activeTab) {
        this.activeTab = activeTab == null ? TitanManagerTab.FIGURES : activeTab;
        if (!sortMode.isSupportedBy(this.activeTab)) {
            sortMode = TitanManagerSortMode.getDefault(this.activeTab);
        }
        pageIndex = 0;
    }

    public TitanManagerSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(TitanManagerSortMode sortMode) {
        TitanManagerSortMode requested = sortMode == null ? TitanManagerSortMode.getDefault(activeTab) : sortMode;
        if (requested.isSupportedBy(activeTab)) {
            this.sortMode = requested;
            this.pageIndex = 0;
        }
    }

    public boolean isFavoritesOnly() {
        return favoritesOnly;
    }

    public void setFavoritesOnly(boolean favoritesOnly) {
        this.favoritesOnly = favoritesOnly;
        this.pageIndex = 0;
    }

    public void toggleFavoritesOnly() {
        setFavoritesOnly(!favoritesOnly);
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = sanitizeSearch(searchQuery);
        this.pageIndex = 0;
    }

    public String getFigureClassFilter() {
        return figureClassFilter;
    }

    public String getFigureClassFilterLabel() {
        return figureClassFilter.isEmpty() ? "All" : figureClassFilter;
    }

    public void setFigureClassFilter(String figureClassFilter) {
        this.figureClassFilter = figureClassFilter == null ? "" : figureClassFilter.trim();
        this.pageIndex = 0;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = Math.max(0, pageIndex);
    }

    public void changePage(int delta) {
        setPageIndex(pageIndex + delta);
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getPageCount() {
        return pageCount;
    }

    public List<TitanManagerStorageSlot> getVisibleSlots() {
        return visibleSlots;
    }

    public TitanManagerStorageSlot getVisibleSlot(int viewSlot) {
        if (viewSlot < 0 || viewSlot >= visibleSlots.size()) {
            return null;
        }
        return visibleSlots.get(viewSlot);
    }

    public void rebuild(ITitanManager titanManager) {
        if (activeTab == TitanManagerTab.FIGURES) {
            List<String> classes = getAvailableFigureClasses(titanManager);
            if (!figureClassFilter.isEmpty() && !classes.contains(figureClassFilter)) {
                figureClassFilter = "";
            }
        } else {
            figureClassFilter = "";
        }

        List<TitanManagerStorageSlot> matches = new ArrayList<>();
        var handler = titanManager.getStorageHandler(activeTab.getStorageSection());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!matchesFilters(titanManager, stack)) {
                continue;
            }
            matches.add(new TitanManagerStorageSlot(activeTab.getStorageSection(), slot));
        }

        matches.sort(buildComparator(titanManager));
        totalResults = matches.size();
        pageCount = Math.max(1, (int) Math.ceil(totalResults / (double) PAGE_SIZE));
        pageIndex = Math.min(Math.max(0, pageIndex), pageCount - 1);

        visibleSlots.clear();
        int start = pageIndex * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        if (start < end) {
            visibleSlots.addAll(matches.subList(start, end));
        }
    }

    public void applySyncSnapshot(TitanManagerTab activeTab,
                                  TitanManagerSortMode sortMode,
                                  boolean favoritesOnly,
                                  String searchQuery,
                                  String figureClassFilter,
                                  int pageIndex,
                                  int totalResults,
                                  int pageCount,
                                  List<TitanManagerStorageSlot> visibleSlots) {
        this.activeTab = activeTab == null ? TitanManagerTab.FIGURES : activeTab;
        this.sortMode = sortMode == null ? TitanManagerSortMode.getDefault(this.activeTab) : sortMode;
        if (!this.sortMode.isSupportedBy(this.activeTab)) {
            this.sortMode = TitanManagerSortMode.getDefault(this.activeTab);
        }
        this.favoritesOnly = favoritesOnly;
        this.searchQuery = sanitizeSearch(searchQuery);
        this.figureClassFilter = figureClassFilter == null ? "" : figureClassFilter.trim();
        this.pageIndex = Math.max(0, pageIndex);
        this.totalResults = Math.max(0, totalResults);
        this.pageCount = Math.max(1, pageCount);
        this.visibleSlots.clear();
        this.visibleSlots.addAll(visibleSlots);
    }

    public void cycleFigureClassFilter(ITitanManager titanManager) {
        List<String> classes = getAvailableFigureClasses(titanManager);
        if (classes.isEmpty()) {
            setFigureClassFilter("");
            return;
        }

        if (figureClassFilter.isEmpty()) {
            setFigureClassFilter(classes.get(0));
            return;
        }

        int index = classes.indexOf(figureClassFilter);
        if (index < 0 || index + 1 >= classes.size()) {
            setFigureClassFilter("");
        } else {
            setFigureClassFilter(classes.get(index + 1));
        }
    }

    public List<String> getAvailableFigureClasses(ITitanManager titanManager) {
        List<String> classes = new ArrayList<>();
        var handler = titanManager.getFigureStorage();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemFigure)) {
                continue;
            }

            String figureClass = ItemFigure.getFigureClass(stack).trim();
            if (!figureClass.isEmpty() && classes.stream().noneMatch(existing -> existing.equalsIgnoreCase(figureClass))) {
                classes.add(figureClass);
            }
        }
        classes.sort(String::compareToIgnoreCase);
        return classes;
    }

    private boolean matchesFilters(ITitanManager titanManager, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (favoritesOnly && !titanManager.isFavorite(stack)) {
            return false;
        }

        if (activeTab == TitanManagerTab.FIGURES && !figureClassFilter.isEmpty()) {
            if (!figureClassFilter.equalsIgnoreCase(ItemFigure.getFigureClass(stack))) {
                return false;
            }
        }

        if (searchQuery.isEmpty()) {
            return true;
        }

        String query = searchQuery.toLowerCase(Locale.ROOT);
        return switch (activeTab) {
            case FIGURES -> matchesFigureSearch(stack, query);
            case CHIPS -> matchesChipSearch(stack, query);
            case ACCESSORIES -> matchesAccessorySearch(stack, query);
        };
    }

    private boolean matchesFigureSearch(ItemStack stack, String query) {
        return contains(stack.getHoverName().getString(), query)
                || contains(ItemFigure.getFigureName(stack), query)
                || contains(ItemFigure.getFigureID(stack), query)
                || contains(ItemFigure.getFigureClass(stack), query);
    }

    private boolean matchesChipSearch(ItemStack stack, String query) {
        if (!(stack.getItem() instanceof ItemChip chip)) {
            return false;
        }
        return contains(stack.getHoverName().getString(), query)
                || contains(chip.getChipId(), query)
                || contains("rank " + ItemChip.getChipRank(stack), query);
    }

    private boolean matchesAccessorySearch(ItemStack stack, String query) {
        if (!(stack.getItem() instanceof ItemAccessory accessory)) {
            return false;
        }
        return contains(stack.getHoverName().getString(), query)
                || contains(accessory.getAccessoryId(), query);
    }

    private Comparator<TitanManagerStorageSlot> buildComparator(ITitanManager titanManager) {
        Comparator<TitanManagerStorageSlot> comparator = switch (sortMode) {
            case LEVEL_DESC -> Comparator.<TitanManagerStorageSlot>comparingInt(ref ->
                    ItemFigure.getLevel(getStack(titanManager, ref))).reversed();
            case LEVEL_ASC -> Comparator.comparingInt(ref ->
                    ItemFigure.getLevel(getStack(titanManager, ref)));
            case NEWEST -> Comparator.<TitanManagerStorageSlot>comparingLong(ref ->
                    getFigureSequence(getStack(titanManager, ref))).reversed();
            case RANK_DESC -> Comparator.<TitanManagerStorageSlot>comparingInt(ref ->
                    ItemChip.getChipRank(getStack(titanManager, ref))).reversed();
            case RANK_ASC -> Comparator.comparingInt(ref ->
                    ItemChip.getChipRank(getStack(titanManager, ref)));
            case NAME -> Comparator.comparing(ref ->
                    getStack(titanManager, ref).getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
        };

        return comparator
                .thenComparing(ref -> getStack(titanManager, ref).getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(TitanManagerStorageSlot::slot);
    }

    private ItemStack getStack(ITitanManager titanManager, TitanManagerStorageSlot ref) {
        return titanManager.getStorageHandler(ref.section()).getStackInSlot(ref.slot());
    }

    private long getFigureSequence(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(TAG_FIGURE_SEQUENCE);
    }

    private String sanitizeSearch(String searchQuery) {
        return Objects.requireNonNullElse(searchQuery, "").trim();
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }
}
