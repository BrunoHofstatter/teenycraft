package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.capability.TitanManagerStorageSection;
import bruhof.teenycraft.capability.TitanManagerStorageSlot;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTitanManagerView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class TitanManagerMenu extends AbstractContainerMenu {
    private static final int STORAGE_VIEW_SLOTS = TitanManagerViewState.PAGE_SIZE;
    private static final int PLAYER_INVENTORY_START = 58;
    private static final int PLAYER_INVENTORY_END = 94;

    private final ITitanManager titanManager;
    private final Player player;
    private final TitanManagerViewState viewState = new TitanManagerViewState();
    private final TitanViewHandler titanViewHandler = new TitanViewHandler();

    public TitanManagerMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, inventory.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .orElseThrow(IllegalStateException::new));
    }

    public TitanManagerMenu(int containerId, Inventory inventory, ITitanManager titanManager) {
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
        viewState.rebuild(titanManager);
    }

    public ITitanManager getTitanManager() {
        return titanManager;
    }

    public TitanManagerViewState getViewState() {
        return viewState;
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

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer serverPlayer) {
            viewState.rebuild(titanManager);
            ModMessages.sendToPlayer(PacketSyncTitanManagerView.fromMenu(containerId, viewState), serverPlayer);
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            TitanManagerStorageSection section = getSectionForStack(sourceStack);
            if (section == null) {
                return ItemStack.EMPTY;
            }

            ItemStack remaining = titanManager.insertIntoStorage(section, sourceStack.copy(), false);
            if (remaining.getCount() == sourceStack.getCount()) {
                return ItemStack.EMPTY;
            }
            sourceStack.setCount(remaining.getCount());
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
