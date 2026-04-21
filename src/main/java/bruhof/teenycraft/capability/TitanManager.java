package bruhof.teenycraft.capability;

import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class TitanManager implements ITitanManager {
    public static final int SLOT_TEAM_1 = 0;
    public static final int SLOT_TEAM_2 = 1;
    public static final int SLOT_TEAM_3 = 2;

    public static final int FIGURE_STORAGE_SLOTS = 972;
    public static final int CHIP_STORAGE_SLOTS = 972;
    public static final int ACCESSORY_STORAGE_SLOTS = 972;

    private static final String TAG_TEAM = "Team";
    private static final String TAG_EQUIPPED_ACCESSORY = "EquippedAccessory";
    private static final String TAG_FIGURE_STORAGE = "FigureStorage";
    private static final String TAG_CHIP_STORAGE = "ChipStorage";
    private static final String TAG_ACCESSORY_STORAGE = "AccessoryStorage";
    private static final String TAG_VANILLA_ITEMS = "VanillaItems";
    private static final String TAG_NEXT_FIGURE_SEQUENCE = "NextFigureSequence";
    private static final String TAG_FAVORITE = "TitanFavorite";
    private static final String TAG_FIGURE_SEQUENCE = "TitanFigureSequence";

    private static final int LEGACY_SLOT_ACCESSORY = 3;
    private static final int LEGACY_STORAGE_START = 4;
    private static final int LEGACY_TOTAL_STORAGE_SLOTS = 972;
    private static final int LEGACY_TOTAL_SLOTS = LEGACY_STORAGE_START + LEGACY_TOTAL_STORAGE_SLOTS;

    private final ItemStackHandler teamHandler = new ItemStackHandler(TEAM_SIZE) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (!(stack.getItem() instanceof ItemFigure)) {
                return false;
            }

            String incomingId = ItemFigure.getFigureID(stack);
            for (int i = 0; i < TEAM_SIZE; i++) {
                if (i == slot) {
                    continue;
                }

                ItemStack existing = getStackInSlot(i);
                if (!existing.isEmpty() && ItemFigure.getFigureID(existing).equals(incomingId)) {
                    return false;
                }
            }
            return true;
        }
    };

    private final ItemStackHandler equippedAccessoryHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ItemAccessory;
        }
    };

    private final ItemStackHandler figureStorage = createStorageHandler(TitanManagerStorageSection.FIGURES, FIGURE_STORAGE_SLOTS);
    private final ItemStackHandler chipStorage = createStorageHandler(TitanManagerStorageSection.CHIPS, CHIP_STORAGE_SLOTS);
    private final ItemStackHandler accessoryStorage = createStorageHandler(TitanManagerStorageSection.ACCESSORIES, ACCESSORY_STORAGE_SLOTS);

    private ListTag vanillaItems = new ListTag();
    private long nextFigureSequence = 1L;

    @Override
    public ItemStackHandler getTeamHandler() {
        return teamHandler;
    }

    @Override
    public ItemStackHandler getEquippedAccessoryHandler() {
        return equippedAccessoryHandler;
    }

    @Override
    public ItemStackHandler getFigureStorage() {
        return figureStorage;
    }

    @Override
    public ItemStackHandler getChipStorage() {
        return chipStorage;
    }

    @Override
    public ItemStackHandler getAccessoryStorage() {
        return accessoryStorage;
    }

    @Override
    public ItemStackHandler getStorageHandler(TitanManagerStorageSection section) {
        return switch (section) {
            case FIGURES -> figureStorage;
            case CHIPS -> chipStorage;
            case ACCESSORIES -> accessoryStorage;
        };
    }

    @Override
    public ItemStack getTeamStack(int slot) {
        if (slot < 0 || slot >= teamHandler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return teamHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack getEquippedAccessory() {
        return equippedAccessoryHandler.getStackInSlot(0);
    }

    @Override
    public boolean isFavorite(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_FAVORITE);
    }

    @Override
    public void setFavorite(ItemStack stack, boolean favorite) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (favorite) {
            tag.putBoolean(TAG_FAVORITE, true);
        } else {
            tag.remove(TAG_FAVORITE);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    @Override
    public boolean toggleFavorite(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean favorite = !isFavorite(stack);
        setFavorite(stack, favorite);
        return favorite;
    }

    @Override
    public ItemStack insertIntoStorage(TitanManagerStorageSection section, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        ItemStackHandler handler = getStorageHandler(section);
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public int findFirstInsertSlot(TitanManagerStorageSection section, ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }

        ItemStackHandler handler = getStorageHandler(section);
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.insertItem(i, stack, true).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void copyFrom(ITitanManager oldStore) {
        CompoundTag nbt = new CompoundTag();
        oldStore.saveNBTData(nbt);
        loadNBTData(nbt);
    }

    @Override
    public boolean isTeamValid() {
        return !teamHandler.getStackInSlot(SLOT_TEAM_1).isEmpty();
    }

    @Override
    public void saveVanillaInventory(Player player) {
        if (hasSavedVanillaInventory()) {
            player.getInventory().clearContent();
            return;
        }

        this.vanillaItems = new ListTag();
        player.getInventory().save(this.vanillaItems);
        player.getInventory().clearContent();
    }

    @Override
    public void restoreVanillaInventory(Player player) {
        if (this.vanillaItems != null && !this.vanillaItems.isEmpty()) {
            player.getInventory().load(this.vanillaItems);
            this.vanillaItems = new ListTag();
        }
    }

    @Override
    public boolean hasSavedVanillaInventory() {
        return this.vanillaItems != null && !this.vanillaItems.isEmpty();
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.put(TAG_TEAM, teamHandler.serializeNBT());
        tag.put(TAG_EQUIPPED_ACCESSORY, equippedAccessoryHandler.serializeNBT());
        tag.put(TAG_FIGURE_STORAGE, figureStorage.serializeNBT());
        tag.put(TAG_CHIP_STORAGE, chipStorage.serializeNBT());
        tag.put(TAG_ACCESSORY_STORAGE, accessoryStorage.serializeNBT());
        tag.putLong(TAG_NEXT_FIGURE_SEQUENCE, nextFigureSequence);
        if (this.vanillaItems != null && !this.vanillaItems.isEmpty()) {
            tag.put(TAG_VANILLA_ITEMS, this.vanillaItems);
        }
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        clearAllHandlers();

        if (tag.contains(TAG_TEAM)) {
            teamHandler.deserializeNBT(tag.getCompound(TAG_TEAM));
            equippedAccessoryHandler.deserializeNBT(tag.getCompound(TAG_EQUIPPED_ACCESSORY));
            figureStorage.deserializeNBT(tag.getCompound(TAG_FIGURE_STORAGE));
            chipStorage.deserializeNBT(tag.getCompound(TAG_CHIP_STORAGE));
            accessoryStorage.deserializeNBT(tag.getCompound(TAG_ACCESSORY_STORAGE));
            nextFigureSequence = Math.max(1L, tag.getLong(TAG_NEXT_FIGURE_SEQUENCE));
        } else if (tag.contains("inventory")) {
            migrateLegacyInventory(tag.getCompound("inventory"));
        }

        if (tag.contains(TAG_VANILLA_ITEMS)) {
            this.vanillaItems = tag.getList(TAG_VANILLA_ITEMS, 10);
        } else {
            this.vanillaItems = new ListTag();
        }

        normalizeFigureSequence();
    }

    private ItemStackHandler createStorageHandler(TitanManagerStorageSection section, int size) {
        return new ItemStackHandler(size) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSection(section, stack);
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, prepareForStorage(section, stack, false));
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, prepareForStorage(section, stack, simulate), simulate);
            }
        };
    }

    private boolean isItemValidForSection(TitanManagerStorageSection section, ItemStack stack) {
        return switch (section) {
            case FIGURES -> stack.getItem() instanceof ItemFigure;
            case CHIPS -> stack.getItem() instanceof ItemChip;
            case ACCESSORIES -> stack.getItem() instanceof ItemAccessory;
        };
    }

    private ItemStack prepareForStorage(TitanManagerStorageSection section, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack prepared = stack.copy();
        if (!simulate && section == TitanManagerStorageSection.FIGURES) {
            ensureFigureSequence(prepared);
        }
        return prepared;
    }

    private void ensureFigureSequence(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemFigure)) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_FIGURE_SEQUENCE)) {
            tag.putLong(TAG_FIGURE_SEQUENCE, nextFigureSequence++);
        }
    }

    private void clearAllHandlers() {
        clearHandler(teamHandler);
        clearHandler(equippedAccessoryHandler);
        clearHandler(figureStorage);
        clearHandler(chipStorage);
        clearHandler(accessoryStorage);
        nextFigureSequence = 1L;
    }

    private void clearHandler(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private void migrateLegacyInventory(CompoundTag inventoryTag) {
        ItemStackHandler legacy = new ItemStackHandler(LEGACY_TOTAL_SLOTS);
        legacy.deserializeNBT(inventoryTag);

        for (int i = 0; i < TEAM_SIZE; i++) {
            teamHandler.setStackInSlot(i, legacy.getStackInSlot(i).copy());
        }

        equippedAccessoryHandler.setStackInSlot(0, legacy.getStackInSlot(LEGACY_SLOT_ACCESSORY).copy());

        for (int i = 0; i < LEGACY_TOTAL_STORAGE_SLOTS; i++) {
            ItemStack stack = legacy.getStackInSlot(LEGACY_STORAGE_START + i);
            if (!stack.isEmpty()) {
                TitanManagerStorageSection section = getSectionForStack(stack);
                if (section != null) {
                    insertIntoStorage(section, stack.copy(), false);
                }
            }
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

    private void normalizeFigureSequence() {
        long maxSequence = 0L;
        for (int i = 0; i < figureStorage.getSlots(); i++) {
            ItemStack stack = figureStorage.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag itemTag = stack.getTag();
            if (itemTag == null || !itemTag.contains(TAG_FIGURE_SEQUENCE)) {
                ensureFigureSequence(stack);
                itemTag = stack.getTag();
            }

            if (itemTag != null) {
                maxSequence = Math.max(maxSequence, itemTag.getLong(TAG_FIGURE_SEQUENCE));
            }
        }

        nextFigureSequence = Math.max(maxSequence + 1L, nextFigureSequence);
    }
}
