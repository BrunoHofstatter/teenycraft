package bruhof.teenycraft.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public interface ITitanManager {
    int TEAM_SIZE = 3;

    ItemStackHandler getTeamHandler();

    ItemStackHandler getEquippedAccessoryHandler();

    ItemStackHandler getFigureStorage();

    ItemStackHandler getChipStorage();

    ItemStackHandler getAccessoryStorage();

    ItemStackHandler getStorageHandler(TitanManagerStorageSection section);

    ItemStack getTeamStack(int slot);

    ItemStack getEquippedAccessory();

    boolean isFavorite(ItemStack stack);

    void setFavorite(ItemStack stack, boolean favorite);

    boolean toggleFavorite(ItemStack stack);

    ItemStack insertIntoStorage(TitanManagerStorageSection section, ItemStack stack, boolean simulate);

    int findFirstInsertSlot(TitanManagerStorageSection section, ItemStack stack);

    void copyFrom(ITitanManager oldStore);

    boolean isTeamValid();

    void saveNBTData(CompoundTag tag);

    void loadNBTData(CompoundTag tag);

    void saveVanillaInventory(Player player);

    void restoreVanillaInventory(Player player);

    boolean hasSavedVanillaInventory();
}
