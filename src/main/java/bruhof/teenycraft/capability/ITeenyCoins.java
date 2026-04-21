package bruhof.teenycraft.capability;

import net.minecraft.nbt.CompoundTag;

public interface ITeenyCoins {
    int getCoins();
    void setCoins(int amount);
    void addCoins(int delta);
    boolean trySpendCoins(int amount);
    void copyFrom(ITeenyCoins oldStore);
    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
}
