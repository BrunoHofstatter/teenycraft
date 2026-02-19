package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
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
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_1, 62, 19));
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_2, 80, 19));
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_TEAM_3, 98, 19));

        // 2. Accessory Slot (3)
        this.addSlot(new SlotItemHandler(inventory, TitanManager.SLOT_ACCESSORY, 134, 19));

        // 3. Storage Slots (4-57)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndexInBox = col + row * 9;
                this.addSlot(new TitanBoxSlot(inventory, slotIndexInBox, 8 + col * 18, 54 + row * 18));
            }
        }
        
        // 4. Deposit Slot (58)
        this.addSlot(new DepositSlot(152, 18));

        layoutPlayerInventorySlots(inv, 8, 174);
    }

    // =========================================
    // NAVIGATION & SEARCH
    // =========================================

    public void setBox(int boxIndex) {
        if (!isSearchMode) {
            if (boxIndex >= 0 && boxIndex < 18) {
                this.currentBox = boxIndex;
            }
        } else {
            // In search mode, "setBox" acts as "setPage"
            int maxPages = (int) Math.ceil(searchMatches.size() / 54.0);
            if (maxPages == 0) maxPages = 1;
            
            if (boxIndex >= 0 && boxIndex < maxPages) {
                this.currentBox = boxIndex;
            }
        }
        this.broadcastChanges();
    }

    public void updateSearch(String query) {
        this.lastQuery = query.toLowerCase().trim();
        this.isSearchMode = !this.lastQuery.isEmpty();
        this.currentBox = 0; 

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Server Side Search Logic
            List<Integer> matches = new ArrayList<>();
            if (isSearchMode) {
                IItemHandler inv = titanManager.getInventory();
                for (int i = 0; i < TitanManager.TOTAL_STORAGE_SLOTS; i++) {
                    int globalIndex = TitanManager.STORAGE_START + i;
                    ItemStack stack = inv.getStackInSlot(globalIndex);
                    if (!stack.isEmpty()) {
                        String name = stack.getHoverName().getString().toLowerCase();
                        String id = (stack.getItem() instanceof ItemFigure) ? ItemFigure.getFigureID(stack).toLowerCase() : "";
                        if (name.contains(this.lastQuery) || id.contains(this.lastQuery)) {
                            matches.add(globalIndex);
                        }
                    }
                }
            }
            this.searchMatches = matches;
            bruhof.teenycraft.networking.ModMessages.sendToPlayer(new bruhof.teenycraft.networking.PacketSyncSearchResults(matches), serverPlayer);
        } else {
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
    // DEPOSIT SLOT (Smart Overflow)
    // =========================================
    private class DepositSlot extends Slot {
        public DepositSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof ItemFigure;
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            if (stack.isEmpty()) {
                super.set(stack);
                return;
            }
            
            // Try to move to main storage immediately
            IItemHandler handler = titanManager.getInventory();
            boolean moved = false;
            
            for (int i = 0; i < TitanManager.TOTAL_STORAGE_SLOTS; i++) {
                int slot = TitanManager.STORAGE_START + i;
                if (handler.insertItem(slot, stack, true).isEmpty()) { // Check if fits
                    handler.insertItem(slot, stack, false); // Do it
                    moved = true;
                    break;
                }
            }
            
            // Clear this slot regardless, so it acts as a "hole"
            // If moved=false (full), we ideally shouldn't have accepted it?
            // But set() is called after logic.
            // Actually, if we clear it here, and it wasn't moved, it is deleted!
            // So only clear if moved.
            
            if (moved) {
                super.set(ItemStack.EMPTY); 
                
                // Force Sync
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(cap -> {
                        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                        cap.saveNBTData(nbt);
                        bruhof.teenycraft.networking.ModMessages.sendToPlayer(new bruhof.teenycraft.networking.PacketSyncTitanData(nbt), serverPlayer);
                    });
                }
            } else {
                // Storage Full: Leave it in this slot? 
                // Or bounce it back? 
                // For now, if full, it stays in this slot (Slot 58). 
                super.set(stack);
            }
        }
    }

    // =========================================
    // CUSTOM BOX SLOT
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
                int matchIndex = (currentBox * 54) + slotIndexInBox;
                if (matchIndex < searchMatches.size()) {
                    return searchMatches.get(matchIndex);
                }
                return 0; 
            } else {
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
             return !getItem().isEmpty();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // 0-3: Team/Accessory
        // 4-57: Visible Box (54 slots)
        // 58: Deposit Slot
        // 59-85: Inventory
        // 86-94: Hotbar

        if (index < 4) {
             // Team -> Visible Box -> Deposit -> Inventory
             if (!this.moveItemStackTo(sourceStack, 4, 58, false)) {
                 if (!this.moveItemStackTo(sourceStack, 58, 59, false)) { 
                     if (!this.moveItemStackTo(sourceStack, 59, 95, true)) return ItemStack.EMPTY;
                 }
             }
        } else if (index < 58) {
             // Box -> Inventory
             if (!this.moveItemStackTo(sourceStack, 59, 95, true)) return ItemStack.EMPTY;
        } else if (index == 58) {
             // Deposit -> Inventory
             if (!this.moveItemStackTo(sourceStack, 59, 95, true)) return ItemStack.EMPTY;
        } else {
             // Inventory -> Titan Manager
             // 1. Try Visible Box
             if (!this.moveItemStackTo(sourceStack, 4, 58, false)) {
                 // 2. Try Team
                 if (!this.moveItemStackTo(sourceStack, 0, 3, false)) {
                     // 3. Try Accessory
                     if (!this.moveItemStackTo(sourceStack, 3, 4, false)) {
                         // 4. Try Deposit Slot (Auto Overflow)
                         if (!this.moveItemStackTo(sourceStack, 58, 59, false)) {
                             return ItemStack.EMPTY;
                         }
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
