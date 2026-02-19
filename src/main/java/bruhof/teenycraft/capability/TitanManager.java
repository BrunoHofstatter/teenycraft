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
            // We need to ensure the data is saved.
            // Since this handler is inside a Capability, typically the persistence is handled automatically
            // when the Chunk/Player saves, IF the game knows it needs saving.
            // For now, we can leave this or add a callback if we notice persistence issues.
        }
    };

    private net.minecraft.nbt.ListTag vanillaItems = new net.minecraft.nbt.ListTag();

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
    public void saveVanillaInventory(net.minecraft.world.entity.player.Player player) {
        if (hasSavedVanillaInventory()) {
            player.getInventory().clearContent();
            return;
        }
        this.vanillaItems = new net.minecraft.nbt.ListTag();
        player.getInventory().save(this.vanillaItems);
        player.getInventory().clearContent();
    }

    @Override
    public void restoreVanillaInventory(net.minecraft.world.entity.player.Player player) {
        if (this.vanillaItems != null && !this.vanillaItems.isEmpty()) {
            player.getInventory().load(this.vanillaItems);
            this.vanillaItems = new net.minecraft.nbt.ListTag();
        }
    }

    @Override
    public boolean hasSavedVanillaInventory() {
        return this.vanillaItems != null && !this.vanillaItems.isEmpty();
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        if (this.vanillaItems != null && !this.vanillaItems.isEmpty()) {
            tag.put("VanillaItems", this.vanillaItems);
        }
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        CompoundTag invTag = tag.getCompound("inventory");
        inventory.deserializeNBT(invTag);
        
        if (tag.contains("VanillaItems")) {
            this.vanillaItems = tag.getList("VanillaItems", 10); // 10 = CompoundTag
        }
    }
}
