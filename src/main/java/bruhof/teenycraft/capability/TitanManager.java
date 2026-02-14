package bruhof.teenycraft.capability;

import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class TitanManager implements ITitanManager {
    public static final int SLOT_TEAM_1 = 0;
    public static final int SLOT_TEAM_2 = 1;
    public static final int SLOT_TEAM_3 = 2;
    public static final int SLOT_ACCESSORY = 3;
    public static final int STORAGE_START = 4;
    public static final int TOTAL_STORAGE_SLOTS = 972; // 18 Boxes * 54 Slots
    public static final int TOTAL_SLOTS = STORAGE_START + TOTAL_STORAGE_SLOTS;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Team Slots Logic
            if (slot >= SLOT_TEAM_1 && slot <= SLOT_TEAM_3) {
                if (!(stack.getItem() instanceof ItemFigure)) return false;

                // Unique Check: Ensure this figure ID isn't already in another team slot
                String incomingId = ItemFigure.getFigureID(stack);
                for (int i = SLOT_TEAM_1; i <= SLOT_TEAM_3; i++) {
                    if (i == slot) continue; // Skip self
                    ItemStack existing = this.getStackInSlot(i);
                    if (!existing.isEmpty() && ItemFigure.getFigureID(existing).equals(incomingId)) {
                        return false; // Duplicate found!
                    }
                }
                return true;
            }
            if (slot == SLOT_ACCESSORY) {
                // TODO: Check for ItemAccessory when implemented
                return true; 
            }
            // Storage accepts figures and accessories
            return stack.getItem() instanceof ItemFigure || true; // Allow all for now, filter later
        }

        @Override
        protected void onContentsChanged(int slot) {
            // Logic to mark dirty if needed
        }
    };

    @Override
    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void copyFrom(ITitanManager oldStore) {
        CompoundTag nbt = new CompoundTag();
        oldStore.saveNBTData(nbt);
        this.loadNBTData(nbt);
    }

    @Override
    public boolean isTeamValid() {
        return !inventory.getStackInSlot(SLOT_TEAM_1).isEmpty();
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        CompoundTag invTag = tag.getCompound("inventory");
        // Safety check: If the saved data is smaller than our current TOTAL_SLOTS,
        // we need to resize it or at least handle the loading carefully.
        // ItemStackHandler.deserializeNBT will naturally fill up to its current size.
        inventory.deserializeNBT(invTag);
    }
}
