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
}
