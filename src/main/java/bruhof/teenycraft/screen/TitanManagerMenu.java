package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TitanManagerMenu extends AbstractContainerMenu {
    private final ITitanManager titanManager;
    private final Player player;
    
    // Box Navigation
    private int currentBox = 0;
    
    // Search Mode
    private boolean isSearchMode = false;
    private List<Integer> searchMatches = new ArrayList<>();
    private String lastQuery = "";

    public TitanManagerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.getCapability(TitanManagerProvider.TITAN_MANAGER).orElseThrow(IllegalStateException::new));
    }

    public TitanManagerMenu(int pContainerId, Inventory inv, ITitanManager titanManager) {
        super(ModMenuTypes.TITAN_MANAGER_MENU.get(), pContainerId);
        this.titanManager = titanManager;
        this.player = inv.player;

        IItemHandler inventory = titanManager.getInventory();

        // 1. Team Slots (0-2)
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_1, 62, 20));
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_2, 80, 20));
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_3, 98, 20));

        // 2. Accessory Slot (3)
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_ACCESSORY, 134, 20));

        // 3. Storage Slots
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndexInBox = col + row * 9;
                this.addSlot(new TitanBoxSlot(inventory, slotIndexInBox, 8 + col * 18, 54 + row * 18));
            }
        }

        layoutPlayerInventorySlots(inv, 8, 174);
    }

    // =========================================
    // NAVIGATION & SEARCH
    // =========================================

    public void setBox(int boxIndex) {
        if (!isSearchMode) {
            if (boxIndex >= 0 && boxIndex < 18) {
                this.currentBox = boxIndex;
                System.out.println("SetBox to " + currentBox + " on " + (player.level().isClientSide() ? "Client" : "Server"));
            }
        } else {
            // In search mode, "setBox" acts as "setPage"
            int maxPages = (int) Math.ceil(searchMatches.size() / 54.0);
            if (maxPages == 0) maxPages = 1;
            
            if (boxIndex >= 0 && boxIndex < maxPages) {
                this.currentBox = boxIndex;
            }
        }
        // Force update slots
        this.broadcastChanges();
    }

    public void updateSearch(String query) {
        this.lastQuery = query.toLowerCase().trim();
        this.isSearchMode = !this.lastQuery.isEmpty();
        this.currentBox = 0; // Reset to page 1

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Server Side Search Logic
            List<Integer> matches = new ArrayList<>();
            if (isSearchMode) {
                IItemHandler inv = titanManager.getInventory();
                // Scan only storage slots (4 to 975)
                for (int i = 0; i < TitanManager.TOTAL_STORAGE_SLOTS; i++) {
                    int globalIndex = TitanManager.STORAGE_START + i;
                    ItemStack stack = inv.getStackInSlot(globalIndex);
                    if (!stack.isEmpty()) {
                        String name = stack.getHoverName().getString().toLowerCase();
                        // Also check FigureID if possible for technical search
                        String id = (stack.getItem() instanceof ItemFigure) ? ItemFigure.getFigureID(stack).toLowerCase() : "";
                        
                        if (name.contains(this.lastQuery) || id.contains(this.lastQuery)) {
                            matches.add(globalIndex);
                        }
                    }
                }
            }
            // Send results to client
            this.searchMatches = matches;
            bruhof.teenycraft.networking.ModMessages.sendToPlayer(new bruhof.teenycraft.networking.PacketSyncSearchResults(matches), serverPlayer);
        } else {
            // Client Side: Wait for packet
            // We can preemptively clear matches to show loading state if desired
            if (isSearchMode) {
                this.searchMatches.clear(); 
            }
        }
        this.broadcastChanges();
    }
    
    public void setSearchResults(List<Integer> matches) {
        this.searchMatches = matches;
        this.broadcastChanges();
    }
    
    public int getSearchMatchCount() {
        return searchMatches.size();
    }

    public int getCurrentBox() {
        return currentBox;
    }
    
    public boolean isSearchMode() {
        return isSearchMode;
    }

    // =========================================
    // CUSTOM SLOT
    // =========================================

    private class TitanBoxSlot extends Slot {
        private final int slotIndexInBox;
        private final IItemHandler itemHandler;

        public TitanBoxSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(new net.minecraft.world.SimpleContainer(0), index, xPosition, yPosition); 
            this.itemHandler = itemHandler;
            this.slotIndexInBox = index;
        }

        @Override
        public int getSlotIndex() {
            if (isSearchMode) {
                // Search Logic
                int matchIndex = (currentBox * 54) + slotIndexInBox;
                if (matchIndex < searchMatches.size()) {
                    return searchMatches.get(matchIndex);
                }
                return 0; // Fallback
            } else {
                // Normal Box Logic
                return TitanManager.STORAGE_START + (currentBox * 54) + slotIndexInBox;
            }
        }

        @Override
        public boolean isActive() {
            if (isSearchMode) {
                int matchIndex = (currentBox * 54) + slotIndexInBox;
                return matchIndex < searchMatches.size();
            }
            return true;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (isSearchMode) return false;
            return itemHandler.isItemValid(getSlotIndex(), stack);
        }

        @Override
        public @NotNull ItemStack getItem() {
            int index = getSlotIndex();
            // Validate range to prevent crashes
            if (index < 0 || index >= itemHandler.getSlots()) return ItemStack.EMPTY;
            return itemHandler.getStackInSlot(index);
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            int index = getSlotIndex();
            if (index >= 0 && index < itemHandler.getSlots()) {
                ((net.minecraftforge.items.ItemStackHandler) itemHandler).setStackInSlot(index, stack);
                this.setChanged();
            }
        }

        @Override
        public void setChanged() {
            // No-op or custom logic
        }

        @Override
        public int getMaxStackSize() {
            return itemHandler.getSlotLimit(getSlotIndex());
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
             int index = getSlotIndex();
             if (index < 0 || index >= itemHandler.getSlots()) return ItemStack.EMPTY;
             return itemHandler.extractItem(index, amount, false);
        }
        
        @Override
        public boolean mayPickup(Player playerIn) {
             if (isSearchMode && !isActive()) return false;
             // Can always pick up if slot is valid
             return !getItem().isEmpty();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 4) {
            // Team/Accessory -> Box
            if (!this.moveItemStackTo(sourceStack, 4, 58, false)) {
                if (!this.moveItemStackTo(sourceStack, 58, 94, true)) return ItemStack.EMPTY;
            }
        } else if (index < 58) {
            // Box -> Inventory
            if (!this.moveItemStackTo(sourceStack, 58, 94, true)) return ItemStack.EMPTY;
        } else {
            // Inventory -> Box
            // If Searching, we force fail (or prioritize Team slots only) to avoid losing items in hidden slots
            if (isSearchMode) {
                if (!this.moveItemStackTo(sourceStack, 0, 3, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(sourceStack, 4, 58, false)) {
                    if (!this.moveItemStackTo(sourceStack, 0, 3, false)) {
                         if (!this.moveItemStackTo(sourceStack, 3, 4, false)) return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    private void layoutPlayerInventorySlots(Inventory inventory, int leftCol, int topRow) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, j + i * 9 + 9, leftCol + j * 18, topRow + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, leftCol + i * 18, topRow + 58));
        }
    }
}
