package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.capability.TitanManagerStorageSection;
import bruhof.teenycraft.capability.TitanManagerStorageSlot;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.group.FigureGroupDefinition;
import bruhof.teenycraft.group.FigureGroupResolver;
import bruhof.teenycraft.group.GroupComboEffectRegistry;
import bruhof.teenycraft.group.GroupComboEffectSpec;
import bruhof.teenycraft.util.FigureGroupLoader;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTitanManagerView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TitanManagerMenu extends AbstractContainerMenu {
    public static final int BUTTON_OPEN_DETAILS_BASE = 2000;
    private static final int STORAGE_VIEW_SLOTS = TitanManagerViewState.PAGE_SIZE;
    private static final int TEAM_SLOT_START = 0;
    private static final int TEAM_SLOT_END = ITitanManager.TEAM_SIZE;
    private static final int EQUIPPED_ACCESSORY_SLOT = 3;
    private static final int STORAGE_SLOT_START = 4;
    private static final int STORAGE_SLOT_END = STORAGE_SLOT_START + STORAGE_VIEW_SLOTS;
    private static final int PLAYER_INVENTORY_START = 58;
    private static final int PLAYER_INVENTORY_END = 94;

    private final ITitanManager titanManager;
    private final Player player;
    private final TitanManagerViewState viewState = new TitanManagerViewState();
    private final TitanViewHandler titanViewHandler = new TitanViewHandler();
    private List<GroupComboOption> comboOptions = List.of();
    private String effectiveComboGroupId = "";
    private boolean comboAutomatic = true;
    private int leadTeamSlot = 0;

    public TitanManagerMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .orElseThrow(IllegalStateException::new), TitanManagerReturnState.read(extraData));
    }

    public TitanManagerMenu(int containerId, Inventory inventory, ITitanManager titanManager) {
        this(containerId, inventory, titanManager, TitanManagerReturnState.defaults());
    }

    private TitanManagerMenu(int containerId,
                             Inventory inventory,
                             ITitanManager titanManager,
                             TitanManagerReturnState initialState) {
        super(ModMenuTypes.TITAN_MANAGER_MENU.get(), containerId);
        this.titanManager = titanManager;
        this.player = inventory.player;

        addSlot(new SlotItemHandler(titanManager.getTeamHandler(), 0, 56, 18));
        addSlot(new SlotItemHandler(titanManager.getTeamHandler(), 1, 74, 18));
        addSlot(new SlotItemHandler(titanManager.getTeamHandler(), 2, 92, 18));
        addSlot(new SlotItemHandler(titanManager.getEquippedAccessoryHandler(), 0, 118, 18));

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + (row * 9);
                addSlot(new SlotItemHandler(titanViewHandler, slot, 39 + (col * 18), 66 + (row * 18)));
            }
        }

        layoutPlayerInventorySlots(inventory, 39, 183);
        initialState.applyTo(viewState);
        viewState.rebuild(titanManager);
        rebuildComboState();
    }

    public static void open(ServerPlayer player) {
        open(player, TitanManagerReturnState.defaults());
    }

    public static void open(ServerPlayer player, TitanManagerReturnState initialState) {
        TitanManagerReturnState safeState = initialState == null
                ? TitanManagerReturnState.defaults()
                : initialState;
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(manager ->
                NetworkHooks.openScreen(player,
                        new SimpleMenuProvider(
                                (containerId, inventory, menuPlayer) ->
                                        new TitanManagerMenu(containerId, inventory, manager, safeState),
                                Component.translatable("container.teenycraft.titan_manager")),
                        safeState::write));
    }

    public ITitanManager getTitanManager() {
        return titanManager;
    }

    public TitanManagerViewState getViewState() {
        return viewState;
    }

    public List<GroupComboOption> getComboOptions() {
        return comboOptions;
    }

    public String getEffectiveComboGroupId() {
        return effectiveComboGroupId;
    }

    public boolean isComboAutomatic() {
        return comboAutomatic;
    }

    public int getLeadTeamSlot() {
        return leadTeamSlot;
    }

    public void setComboGroup(String groupId) {
        rebuildComboState();
        if (groupId == null || groupId.isBlank()) {
            titanManager.setSelectedComboGroupId("");
        } else if (comboOptions.stream().anyMatch(option -> option.id().equals(groupId))) {
            titanManager.setSelectedComboGroupId(groupId);
        }
        rebuildComboState();
    }

    public void cycleLeadTeamSlot() {
        int current = titanManager.getLeadTeamSlot();
        for (int offset = 1; offset <= ITitanManager.TEAM_SIZE; offset++) {
            int candidate = (current + offset) % ITitanManager.TEAM_SIZE;
            if (!titanManager.getTeamStack(candidate).isEmpty()) {
                titanManager.setLeadTeamSlot(candidate);
                break;
            }
        }
        rebuildComboState();
    }

    public void setActiveTab(TitanManagerTab tab) {
        viewState.setActiveTab(tab);
    }

    public void setSortMode(TitanManagerSortMode sortMode) {
        viewState.setSortMode(sortMode);
    }

    public void setSearchQuery(String query) {
        viewState.setSearchQuery(query);
    }

    public void setPageIndex(int pageIndex) {
        viewState.setPageIndex(pageIndex);
    }

    public void changePage(int delta) {
        viewState.changePage(delta);
    }

    public void toggleFavoritesOnly() {
        viewState.toggleFavoritesOnly();
    }

    public void cycleFigureClassFilter() {
        viewState.cycleFigureClassFilter(titanManager);
    }

    public void toggleFavorite(int viewSlot) {
        TitanManagerStorageSlot ref = viewState.getVisibleSlot(viewSlot);
        if (ref == null) {
            return;
        }

        ItemStack stack = titanManager.getStorageHandler(ref.section()).getStackInSlot(ref.slot());
        titanManager.toggleFavorite(stack);
    }

    public void applySyncedViewState(TitanManagerTab activeTab,
                                     TitanManagerSortMode sortMode,
                                     boolean favoritesOnly,
                                     String searchQuery,
                                     String figureClassFilter,
                                     int pageIndex,
                                     int totalResults,
                                     int pageCount,
                                     java.util.List<TitanManagerStorageSlot> visibleSlots) {
        viewState.applySyncSnapshot(activeTab, sortMode, favoritesOnly, searchQuery, figureClassFilter,
                pageIndex, totalResults, pageCount, visibleSlots);
    }

    public void applySyncedComboState(List<GroupComboOption> options,
                                      String effectiveGroupId,
                                      boolean automatic,
                                      int leadSlot) {
        comboOptions = List.copyOf(options);
        effectiveComboGroupId = effectiveGroupId == null ? "" : effectiveGroupId;
        comboAutomatic = automatic;
        leadTeamSlot = Math.max(0, Math.min(ITitanManager.TEAM_SIZE - 1, leadSlot));
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer serverPlayer) {
            viewState.rebuild(titanManager);
            rebuildComboState();
            ModMessages.sendToPlayer(PacketSyncTitanManagerView.fromMenu(containerId, viewState, this), serverPlayer);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int slotIndex = id - BUTTON_OPEN_DETAILS_BASE;
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            ItemStack stack = slots.get(slotIndex).getItem();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }

            TitanManagerReturnState returnState = TitanManagerReturnState.capture(viewState);
            if (stack.getItem() instanceof ItemFigure) {
                FigureItemLocation location = getFigureLocation(slotIndex);
                if (location != null) {
                    FigureScreenMenu.open(serverPlayer, location, returnState);
                    return true;
                }
            } else if (stack.getItem() instanceof ItemAccessory accessory) {
                AccessoryScreenMenu.open(serverPlayer, accessory.getAccessoryId(), returnState);
                return true;
            }
            return false;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();
        boolean moved;

        if (index >= TEAM_SLOT_START && index < TEAM_SLOT_END) {
            moved = moveToStorage(sourceStack, TitanManagerStorageSection.FIGURES);
        } else if (index == EQUIPPED_ACCESSORY_SLOT) {
            moved = moveToStorage(sourceStack, TitanManagerStorageSection.ACCESSORIES);
        } else if (index >= STORAGE_SLOT_START && index < STORAGE_SLOT_END) {
            moved = quickMoveFromStorage(sourceStack);
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            moved = quickMoveFromPlayerInventory(sourceStack);
        } else {
            moved = false;
        }

        if (!moved || sourceStack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        broadcastChanges();
        return copy;
    }

    public boolean depositCarriedStack() {
        ItemStack carried = getCarried();
        TitanManagerStorageSection section = getSectionForStack(carried);
        if (carried.isEmpty() || section == null) {
            return false;
        }

        ItemStack remaining = titanManager.insertIntoStorage(section, carried.copy(), false);
        if (remaining.getCount() == carried.getCount()) {
            player.displayClientMessage(Component.literal("That Titan Manager storage section is full."), true);
            return false;
        }

        setCarried(remaining);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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

    private TitanManagerStorageSection getSectionForStack(ItemStack stack) {
        if (stack.getItem() instanceof ItemFigure) {
            return TitanManagerStorageSection.FIGURES;
        }
        if (stack.getItem() instanceof ItemChip) {
            return TitanManagerStorageSection.CHIPS;
        }
        if (stack.getItem() instanceof ItemAccessory) {
            return TitanManagerStorageSection.ACCESSORIES;
        }
        return null;
    }

    private FigureItemLocation getFigureLocation(int menuSlotIndex) {
        if (menuSlotIndex >= TEAM_SLOT_START && menuSlotIndex < TEAM_SLOT_END) {
            return FigureItemLocation.titanTeam(menuSlotIndex - TEAM_SLOT_START);
        }
        if (menuSlotIndex >= STORAGE_SLOT_START && menuSlotIndex < STORAGE_SLOT_END) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(menuSlotIndex - STORAGE_SLOT_START);
            return ref != null && ref.section() == TitanManagerStorageSection.FIGURES
                    ? FigureItemLocation.titanStorage(ref.slot())
                    : null;
        }
        if (menuSlotIndex >= PLAYER_INVENTORY_START && menuSlotIndex < PLAYER_INVENTORY_END) {
            return FigureItemLocation.playerInventory(slots.get(menuSlotIndex).getContainerSlot());
        }
        return null;
    }

    private boolean quickMoveFromStorage(ItemStack sourceStack) {
        if (sourceStack.getItem() instanceof ItemFigure) {
            return moveItemStackTo(sourceStack, TEAM_SLOT_START, TEAM_SLOT_END, false)
                    || moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true);
        }
        if (sourceStack.getItem() instanceof ItemAccessory) {
            return moveItemStackTo(sourceStack, EQUIPPED_ACCESSORY_SLOT, EQUIPPED_ACCESSORY_SLOT + 1, false)
                    || moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true);
        }
        return sourceStack.getItem() instanceof ItemChip
                && moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true);
    }

    private boolean quickMoveFromPlayerInventory(ItemStack sourceStack) {
        if (sourceStack.getItem() instanceof ItemFigure) {
            if (moveItemStackTo(sourceStack, TEAM_SLOT_START, TEAM_SLOT_END, false)) {
                return true;
            }
            return moveToStorage(sourceStack, TitanManagerStorageSection.FIGURES);
        }
        if (sourceStack.getItem() instanceof ItemAccessory) {
            if (moveItemStackTo(sourceStack, EQUIPPED_ACCESSORY_SLOT, EQUIPPED_ACCESSORY_SLOT + 1, false)) {
                return true;
            }
            return moveToStorage(sourceStack, TitanManagerStorageSection.ACCESSORIES);
        }
        return sourceStack.getItem() instanceof ItemChip
                && moveToStorage(sourceStack, TitanManagerStorageSection.CHIPS);
    }

    private boolean moveToStorage(ItemStack sourceStack, TitanManagerStorageSection section) {
        int originalCount = sourceStack.getCount();
        ItemStack remaining = titanManager.insertIntoStorage(section, sourceStack.copy(), false);
        sourceStack.setCount(remaining.getCount());
        return remaining.getCount() < originalCount;
    }

    private void rebuildComboState() {
        ItemStack first = titanManager.getTeamStack(0);
        ItemStack second = titanManager.getTeamStack(1);
        List<FigureGroupDefinition> eligible = first.isEmpty() || second.isEmpty()
                ? List.of()
                : FigureGroupLoader.getSharedGroups(ItemFigure.getFigureID(first), ItemFigure.getFigureID(second));

        List<GroupComboOption> rebuilt = new ArrayList<>(eligible.size());
        for (FigureGroupDefinition group : eligible) {
            List<GroupComboEffectOption> effects = new ArrayList<>();
            for (String effectId : group.comboEffectIds()) {
                GroupComboEffectSpec effect = GroupComboEffectRegistry.get(effectId);
                if (effect != null) {
                    effects.add(new GroupComboEffectOption(effect.id(), effect.label(), effect.description(), effect.iconId()));
                }
            }
            rebuilt.add(new GroupComboOption(group.id(), group.name(), group.priority(), effects));
        }
        comboOptions = List.copyOf(rebuilt);
        String selected = titanManager.getSelectedComboGroupId();
        String selectedToValidate = selected;
        if (player instanceof ServerPlayer && selected != null && !selected.isBlank()
                && eligible.stream().noneMatch(group -> group.id().equals(selectedToValidate))) {
            titanManager.setSelectedComboGroupId("");
            selected = "";
        }
        comboAutomatic = selected == null || selected.isBlank();
        effectiveComboGroupId = first.isEmpty() || second.isEmpty()
                ? ""
                : FigureGroupResolver.resolve(ItemFigure.getFigureID(first), ItemFigure.getFigureID(second), selected)
                .map(FigureGroupDefinition::id).orElse("");
        leadTeamSlot = titanManager.getLeadTeamSlot();
        if (player instanceof ServerPlayer && titanManager.getTeamStack(leadTeamSlot).isEmpty()) {
            for (int slot = 0; slot < ITitanManager.TEAM_SIZE; slot++) {
                if (!titanManager.getTeamStack(slot).isEmpty()) {
                    titanManager.setLeadTeamSlot(slot);
                    leadTeamSlot = slot;
                    break;
                }
            }
        }
    }

    private class TitanViewHandler implements IItemHandlerModifiable {
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref != null) {
                titanManager.getStorageHandler(ref.section()).setStackInSlot(ref.slot(), stack);
                return;
            }

            if (!stack.isEmpty()) {
                titanManager.insertIntoStorage(viewState.getActiveTab().getStorageSection(), stack, false);
            }
        }

        @Override
        public int getSlots() {
            return STORAGE_VIEW_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref == null) {
                return ItemStack.EMPTY;
            }
            return titanManager.getStorageHandler(ref.section()).getStackInSlot(ref.slot());
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref != null) {
                return titanManager.getStorageHandler(ref.section()).insertItem(ref.slot(), stack, simulate);
            }
            return titanManager.insertIntoStorage(viewState.getActiveTab().getStorageSection(), stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref == null) {
                return ItemStack.EMPTY;
            }
            return titanManager.getStorageHandler(ref.section()).extractItem(ref.slot(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref != null) {
                return titanManager.getStorageHandler(ref.section()).getSlotLimit(ref.slot());
            }
            return titanManager.getStorageHandler(viewState.getActiveTab().getStorageSection()).getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            TitanManagerStorageSlot ref = viewState.getVisibleSlot(slot);
            if (ref != null) {
                return titanManager.getStorageHandler(ref.section()).isItemValid(ref.slot(), stack);
            }
            return titanManager.findFirstInsertSlot(viewState.getActiveTab().getStorageSection(), stack) >= 0;
        }
    }
}
