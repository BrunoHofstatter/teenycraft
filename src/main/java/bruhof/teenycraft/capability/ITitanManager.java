package bruhof.teenycraft.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public interface ITitanManager {
    ItemStackHandler getInventory();
    void copyFrom(ITitanManager oldStore);
    boolean isTeamValid();
    
    // For saving/loading to NBT
    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
    
    // Vanilla Inventory Vault
    void saveVanillaInventory(net.minecraft.world.entity.player.Player player);
    void restoreVanillaInventory(net.minecraft.world.entity.player.Player player);
    boolean hasSavedVanillaInventory();
}
