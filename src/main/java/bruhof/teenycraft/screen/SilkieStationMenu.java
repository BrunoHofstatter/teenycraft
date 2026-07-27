package bruhof.teenycraft.screen;

import bruhof.teenycraft.block.ModBlocks;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.golden.GoldenSacrificeCalculator;
import bruhof.teenycraft.group.FigureGroupDefinition;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncSilkieStation;
import bruhof.teenycraft.networking.PacketSyncTitanData;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.FigureGroupLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SilkieStationMenu extends AbstractContainerMenu {
    public static final int BROWSER_PAGE_SIZE = 10;
    public static final int FAVORITE_PAGE_SIZE = 7;

    private static final GoldenSacrificeCalculator.GroupAccess GROUP_ACCESS = new GoldenSacrificeCalculator.GroupAccess() {
        @Override
        public Set<String> groupsForFigure(String figureId) {
            return FigureGroupLoader.getGroupIdsForFigure(figureId);
        }

        @Override
        public Set<String> membersForGroup(String groupId) {
            FigureGroupDefinition group = FigureGroupLoader.getGroup(groupId);
            return group == null ? Set.of() : group.figureIds();
        }

        @Override
        public String nameForGroup(String groupId) {
            FigureGroupDefinition group = FigureGroupLoader.getGroup(groupId);
            return group == null ? groupId : group.name();
        }
    };

    private final Player player;
    private final ITitanManager titanManager;
    private final BlockPos stationPos;
    private final ContainerLevelAccess access;
    private final List<SilkieStationTargetRef> sacrificeRefs = new ArrayList<>();

    private SilkieStationTargetRef targetRef;
    private SilkieStationSortMode sortMode = SilkieStationSortMode.RECOMMENDED;
    private String searchQuery = "";
    private int browserPage;
    private int favoritePage;
    private int selectedAbility = -1;
    private String statusMessage = "Select a target figure.";
    private boolean syncRequested = true;
    private SilkieStationSnapshot snapshot = SilkieStationSnapshot.empty();

    public SilkieStationMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(),
                inventory.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                        .orElseThrow(IllegalStateException::new));
    }

    public SilkieStationMenu(int containerId, Inventory inventory, BlockPos stationPos, ITitanManager titanManager) {
        super(ModMenuTypes.SILKIE_STATION_MENU.get(), containerId);
        this.player = inventory.player;
        this.titanManager = titanManager;
        this.stationPos = stationPos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), stationPos);
        layoutPlayerInventorySlots(inventory, 80, 190);
    }

    public SilkieStationSnapshot getSnapshot() {
        return snapshot;
    }

    public void applySnapshot(SilkieStationSnapshot snapshot) {
        this.snapshot = snapshot == null ? SilkieStationSnapshot.empty() : snapshot;
    }

    public void selectTarget(SilkieStationTargetRef requested) {
        ItemStack stack = resolve(requested);
        if (!isEligibleTarget(requested, stack)) {
            statusMessage = "That target is no longer available.";
            requestSync();
            return;
        }
        if (getIncompleteAbilityIndexes(stack).isEmpty()) {
            statusMessage = "That figure already has every golden ability.";
            requestSync();
            return;
        }

        targetRef = requested;
        sacrificeRefs.clear();
        selectedAbility = getIncompleteAbilityIndexes(stack).get(0);
        browserPage = 0;
        statusMessage = "";
        requestSync();
    }

    public void clearTarget() {
        targetRef = null;
        sacrificeRefs.clear();
        selectedAbility = -1;
        browserPage = 0;
        statusMessage = "Select a target figure.";
        requestSync();
    }

    public void toggleSacrifice(int storageSlot) {
        SilkieStationTargetRef ref = new SilkieStationTargetRef(SilkieStationSourceType.STORAGE, storageSlot);
        if (sacrificeRefs.remove(ref)) {
            statusMessage = "";
            requestSync();
            return;
        }
        if (sacrificeRefs.size() >= bruhof.teenycraft.TeenyBalance.GOLDEN_MAX_SACRIFICES) {
            statusMessage = "Silkie can eat at most five figures at once.";
            requestSync();
            return;
        }
        ItemStack stack = resolve(ref);
        if (!isEligibleSacrifice(ref, stack)) {
            statusMessage = "That figure cannot be sacrificed.";
            requestSync();
            return;
        }
        sacrificeRefs.add(ref);
        statusMessage = "";
        requestSync();
    }

    public void setSortMode(SilkieStationSortMode sortMode) {
        this.sortMode = sortMode == null ? SilkieStationSortMode.RECOMMENDED : sortMode;
        this.browserPage = 0;
        requestSync();
    }

    public void setSearchQuery(String searchQuery) {
        String sanitized = searchQuery == null ? "" : searchQuery.trim();
        this.searchQuery = sanitized.length() > 32 ? sanitized.substring(0, 32) : sanitized;
        this.browserPage = 0;
        requestSync();
    }

    public void setBrowserPage(int page) {
        this.browserPage = Math.max(0, page);
        requestSync();
    }

    public void setFavoritePage(int page) {
        this.favoritePage = Math.max(0, page);
        requestSync();
    }

    public void feed() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean battling = player.getCapability(BattleStateProvider.BATTLE_STATE)
                .map(state -> state.isBattling()).orElse(false);
        if (battling) {
            statusMessage = "Silkie Station cannot be used during battle.";
            requestSync();
            return;
        }

        ItemStack target = resolve(targetRef);
        List<String> abilities = ItemFigure.getAbilities(target);
        List<Integer> incomplete = getIncompleteAbilityIndexes(target);
        if (!isEligibleTarget(targetRef, target) || incomplete.isEmpty()) {
            statusMessage = "The target is no longer available.";
            requestSync();
            return;
        }
        selectedAbility = incomplete.get(0);
        String abilityId = abilities.get(selectedAbility);
        if (sacrificeRefs.isEmpty()) {
            statusMessage = "Choose at least one sacrifice.";
            requestSync();
            return;
        }

        List<ItemStack> sacrifices = new ArrayList<>(sacrificeRefs.size());
        ItemStackHandler storage = titanManager.getFigureStorage();
        for (SilkieStationTargetRef ref : sacrificeRefs) {
            ItemStack stack = resolve(ref);
            if (!isEligibleSacrifice(ref, stack)
                    || storage.extractItem(ref.slot(), 1, true).isEmpty()) {
                statusMessage = "A selected sacrifice changed. Nothing was consumed.";
                requestSync();
                return;
            }
            sacrifices.add(stack);
        }

        GoldenSacrificeCalculator.Result result = calculate(target, sacrifices);
        if (result.totalPoints() <= 0) {
            statusMessage = "The selected figures provide no golden progress.";
            requestSync();
            return;
        }

        int applied = ItemFigure.addGoldenPoints(target, abilityId, result.totalPoints());
        int overflow = result.totalPoints() - applied;
        for (SilkieStationTargetRef ref : List.copyOf(sacrificeRefs)) {
            storage.extractItem(ref.slot(), 1, false);
        }
        sacrificeRefs.clear();

        incomplete = getIncompleteAbilityIndexes(target);
        if (!incomplete.isEmpty() && ItemFigure.isAbilityGolden(target, abilityId)) {
            selectedAbility = incomplete.get(0);
        } else if (incomplete.isEmpty()) {
            selectedAbility = -1;
        }
        statusMessage = overflow > 0
                ? "Applied " + applied + " points; " + overflow + " excess points were discarded."
                : "Applied " + applied + " golden points.";

        CompoundTag titanData = new CompoundTag();
        titanManager.saveNBTData(titanData);
        ModMessages.sendToPlayer(new PacketSyncTitanData(titanData), serverPlayer);
        requestSync();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer && syncRequested) {
            snapshot = buildSnapshot();
            ModMessages.sendToPlayer(new PacketSyncSilkieStation(containerId, snapshot), serverPlayer);
            syncRequested = false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SILKIE_STATION.get());
    }

    private SilkieStationSnapshot buildSnapshot() {
        ItemStack target = resolve(targetRef);
        if (!isEligibleTarget(targetRef, target)) {
            targetRef = null;
            sacrificeRefs.clear();
            target = ItemStack.EMPTY;
        }

        List<SilkieStationFigureView> activeTargets = buildActiveTargets();
        Page<SilkieStationFigureView> favoriteTargets = buildFavoriteTargets();
        List<SilkieStationFigureView> offerings = buildOfferings(target);
        Page<SilkieStationFigureView> candidates = buildCandidates(target);
        List<SilkieStationAbilityView> abilities = buildAbilities(target);

        GoldenSacrificeCalculator.Result result = target.isEmpty()
                ? GoldenSacrificeCalculator.calculate(null, List.of(), GROUP_ACCESS)
                : calculate(target, offerings.stream().map(SilkieStationFigureView::stack).toList());
        List<SilkieStationGroupView> groupViews = result.groupBreakdowns().stream()
                .map(group -> new SilkieStationGroupView(group.groupId(), group.groupName(),
                        group.distinctSacrificeCount(), group.memberCount(), group.complete(),
                        group.awardedBonus(), result.awardedGroup() != null
                        && result.awardedGroup().groupId().equals(group.groupId())))
                .toList();

        int current = 0;
        int requirement = 0;
        if (selectedAbility >= 0 && selectedAbility < abilities.size()) {
            SilkieStationAbilityView selected = abilities.get(selectedAbility);
            current = selected.points();
            requirement = selected.requirement();
        }
        int remaining = Math.max(0, requirement - current);
        int applied = Math.min(remaining, result.totalPoints());
        int overflow = Math.max(0, result.totalPoints() - applied);
        boolean invested = offerings.stream().anyMatch(SilkieStationFigureView::invested);
        boolean canFeed = !target.isEmpty() && !offerings.isEmpty() && requirement > current;

        return new SilkieStationSnapshot(targetRef, target, activeTargets, favoriteTargets.items(),
                favoriteTargets.page(), favoriteTargets.pageCount(), offerings, candidates.items(),
                candidates.page(), candidates.pageCount(), candidates.total(), sortMode, searchQuery,
                abilities, selectedAbility, result.basePoints(), result.exactCount(), result.exactBonus(),
                result.classComboCount(), result.classBonus(), groupViews, result.totalPoints(), applied,
                overflow, invested, canFeed, statusMessage);
    }

    private List<SilkieStationFigureView> buildActiveTargets() {
        List<SilkieStationFigureView> result = new ArrayList<>();
        for (int slot = 0; slot < ITitanManager.TEAM_SIZE; slot++) {
            ItemStack stack = titanManager.getTeamStack(slot);
            if (stack.getItem() instanceof ItemFigure) {
                result.add(targetView(new SilkieStationTargetRef(SilkieStationSourceType.TEAM, slot), stack));
            }
        }
        return result;
    }

    private Page<SilkieStationFigureView> buildFavoriteTargets() {
        List<SilkieStationFigureView> all = new ArrayList<>();
        ItemStackHandler storage = titanManager.getFigureStorage();
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemFigure && titanManager.isFavorite(stack)) {
                all.add(targetView(new SilkieStationTargetRef(SilkieStationSourceType.STORAGE, slot), stack));
            }
        }
        all.sort(Comparator.comparing((SilkieStationFigureView view) -> ItemFigure.getFigureName(view.stack()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(view -> ItemFigure.getLevel(view.stack())));
        return page(all, favoritePage, FAVORITE_PAGE_SIZE, true);
    }

    private List<SilkieStationFigureView> buildOfferings(ItemStack target) {
        List<SilkieStationFigureView> result = new ArrayList<>();
        sacrificeRefs.removeIf(ref -> !isEligibleSacrifice(ref, resolve(ref)));
        if (target.isEmpty()) {
            return result;
        }
        for (SilkieStationTargetRef ref : sacrificeRefs) {
            ItemStack stack = resolve(ref);
            result.add(sacrificeView(ref, stack, target));
        }
        return result;
    }

    private Page<SilkieStationFigureView> buildCandidates(ItemStack target) {
        if (target.isEmpty()) {
            return new Page<>(List.of(), 0, 1, 0);
        }
        List<SilkieStationFigureView> all = new ArrayList<>();
        ItemStackHandler storage = titanManager.getFigureStorage();
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            SilkieStationTargetRef ref = new SilkieStationTargetRef(SilkieStationSourceType.STORAGE, slot);
            ItemStack stack = storage.getStackInSlot(slot);
            if (!isEligibleSacrifice(ref, stack) || sacrificeRefs.contains(ref) || !matchesSearch(stack)) {
                continue;
            }
            all.add(sacrificeView(ref, stack, target));
        }
        all.sort(candidateComparator(target));
        return page(all, browserPage, BROWSER_PAGE_SIZE, false);
    }

    private List<SilkieStationAbilityView> buildAbilities(ItemStack target) {
        if (target.isEmpty()) {
            selectedAbility = -1;
            return List.of();
        }
        List<SilkieStationAbilityView> result = new ArrayList<>();
        for (String abilityId : ItemFigure.getAbilities(target)) {
            AbilityLoader.AbilityData data = AbilityLoader.getAbility(abilityId);
            String name = data == null || data.name == null ? abilityId : data.name;
            int points = ItemFigure.getGoldenPoints(target, abilityId);
            int requirement = ItemFigure.getGoldenRequirement(target, abilityId);
            result.add(new SilkieStationAbilityView(abilityId, name, points, requirement,
                    requirement > 0 && points >= requirement));
        }
        selectedAbility = -1;
        for (int i = 0; i < result.size(); i++) {
            if (!result.get(i).golden()) {
                selectedAbility = i;
                break;
            }
        }
        return result;
    }

    private SilkieStationFigureView targetView(SilkieStationTargetRef ref, ItemStack stack) {
        return new SilkieStationFigureView(ref, stack, GoldenSacrificeCalculator.Affinity.UNRELATED,
                0, "Target", false);
    }

    private SilkieStationFigureView sacrificeView(SilkieStationTargetRef ref, ItemStack stack, ItemStack target) {
        GoldenSacrificeCalculator.Result result = calculate(target, List.of(stack));
        GoldenSacrificeCalculator.DonorBreakdown donor = result.donors().get(0);
        String reason = switch (donor.affinity()) {
            case EXACT -> "Exact duplicate";
            case GROUP -> donor.sharedGroupIds().stream().map(GROUP_ACCESS::nameForGroup)
                    .sorted().reduce((left, right) -> left + ", " + right).orElse("Shared group");
            case CLASS -> "Same class";
            case UNRELATED -> "Unrelated";
        };
        return new SilkieStationFigureView(ref, stack, donor.affinity(), donor.basePoints(), reason,
                isInvested(stack));
    }

    private GoldenSacrificeCalculator.Result calculate(ItemStack target, List<ItemStack> sacrifices) {
        GoldenSacrificeCalculator.FigureData targetData = figureData(target);
        List<GoldenSacrificeCalculator.FigureData> donorData = sacrifices.stream()
                .map(this::figureData).toList();
        return GoldenSacrificeCalculator.calculate(targetData, donorData, GROUP_ACCESS);
    }

    private GoldenSacrificeCalculator.FigureData figureData(ItemStack stack) {
        return new GoldenSacrificeCalculator.FigureData(ItemFigure.getFigureID(stack), ItemFigure.getFigureClass(stack));
    }

    private Comparator<SilkieStationFigureView> candidateComparator(ItemStack target) {
        Comparator<SilkieStationFigureView> levelThenName = Comparator
                .comparingInt((SilkieStationFigureView view) -> ItemFigure.getLevel(view.stack()))
                .thenComparing(view -> ItemFigure.getFigureName(view.stack()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(view -> view.ref().slot());
        if (!searchQuery.isBlank() || sortMode == SilkieStationSortMode.LEVEL) {
            return levelThenName;
        }
        if (sortMode == SilkieStationSortMode.CLASS) {
            String targetClass = ItemFigure.getFigureClass(target);
            return Comparator.<SilkieStationFigureView, Integer>comparing(view ->
                            ItemFigure.getFigureClass(view.stack()).equalsIgnoreCase(targetClass) ? 0 : 1)
                    .thenComparing(view -> ItemFigure.getFigureClass(view.stack()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(levelThenName);
        }
        return Comparator.comparingInt((SilkieStationFigureView view) -> view.affinity().ordinal())
                .thenComparing(levelThenName);
    }

    private boolean matchesSearch(ItemStack stack) {
        if (searchQuery.isBlank()) {
            return true;
        }
        String query = searchQuery.toLowerCase(Locale.ROOT);
        if (ItemFigure.getFigureName(stack).toLowerCase(Locale.ROOT).contains(query)
                || ItemFigure.getFigureID(stack).toLowerCase(Locale.ROOT).contains(query)
                || ItemFigure.getFigureClass(stack).toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        return FigureGroupLoader.getGroupsForFigure(ItemFigure.getFigureID(stack)).stream()
                .anyMatch(group -> group.name().toLowerCase(Locale.ROOT).contains(query)
                        || group.id().toLowerCase(Locale.ROOT).contains(query));
    }

    private boolean isEligibleSacrifice(SilkieStationTargetRef ref, ItemStack stack) {
        if (ref == null || ref.sourceType() != SilkieStationSourceType.STORAGE
                || !(stack.getItem() instanceof ItemFigure)) {
            return false;
        }
        if (targetRef != null && targetRef.equals(ref)) {
            return false;
        }
        return !titanManager.isFavorite(stack);
    }

    private boolean isEligibleTarget(SilkieStationTargetRef ref, ItemStack stack) {
        if (ref == null || !(stack.getItem() instanceof ItemFigure)) {
            return false;
        }
        return switch (ref.sourceType()) {
            case TEAM -> ref.slot() >= 0 && ref.slot() < ITitanManager.TEAM_SIZE;
            case STORAGE -> ref.slot() >= 0
                    && ref.slot() < titanManager.getFigureStorage().getSlots()
                    && titanManager.isFavorite(stack);
            case PLAYER_INVENTORY -> ref.slot() >= 0
                    && ref.slot() < player.getInventory().items.size();
        };
    }

    private boolean isInvested(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemFigure)) {
            return false;
        }
        if (ItemFigure.getLevel(stack) > 1 || ItemFigure.getPendingUpgradePoints(stack) > 0
                || ItemFigure.hasEquippedChip(stack)) {
            return true;
        }
        for (String abilityId : ItemFigure.getAbilities(stack)) {
            if (ItemFigure.getGoldenPoints(stack, abilityId) > 0) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> getIncompleteAbilityIndexes(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemFigure)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        List<String> abilities = ItemFigure.getAbilities(stack);
        for (int index = 0; index < abilities.size(); index++) {
            if (!ItemFigure.isAbilityGolden(stack, abilities.get(index))) {
                result.add(index);
            }
        }
        return result;
    }

    private ItemStack resolve(SilkieStationTargetRef ref) {
        if (ref == null || ref.slot() < 0) {
            return ItemStack.EMPTY;
        }
        return switch (ref.sourceType()) {
            case TEAM -> ref.slot() < ITitanManager.TEAM_SIZE
                    ? titanManager.getTeamStack(ref.slot()) : ItemStack.EMPTY;
            case STORAGE -> ref.slot() < titanManager.getFigureStorage().getSlots()
                    ? titanManager.getFigureStorage().getStackInSlot(ref.slot()) : ItemStack.EMPTY;
            case PLAYER_INVENTORY -> ref.slot() < player.getInventory().getContainerSize()
                    ? player.getInventory().getItem(ref.slot()) : ItemStack.EMPTY;
        };
    }

    private <T> Page<T> page(List<T> all, int requestedPage, int pageSize, boolean favorite) {
        int pageCount = Math.max(1, (int) Math.ceil(all.size() / (double) pageSize));
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        if (favorite) {
            favoritePage = page;
        } else {
            browserPage = page;
        }
        int start = page * pageSize;
        int end = Math.min(all.size(), start + pageSize);
        return new Page<>(List.copyOf(all.subList(start, end)), page, pageCount, all.size());
    }

    private void layoutPlayerInventorySlots(Inventory inventory, int leftCol, int topRow) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    private void requestSync() {
        syncRequested = true;
    }

    private record Page<T>(List<T> items, int page, int pageCount, int total) {
    }
}
