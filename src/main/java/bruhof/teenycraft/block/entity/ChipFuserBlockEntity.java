package bruhof.teenycraft.block.entity;

import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.chip.ChipFusionRegistry;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTeenyCoins;
import bruhof.teenycraft.screen.ChipFuserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class ChipFuserBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_LEFT = 0;
    public static final int SLOT_RIGHT = 1;
    public static final int SLOT_RESULT = 2;

    private boolean resultReady = false;
    private boolean suppressPreviewUpdate = false;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_RESULT) {
                return false;
            }
            return stack.getItem() instanceof ItemChip;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!suppressPreviewUpdate && slot != SLOT_RESULT && !resultReady && (getLevel() == null || !getLevel().isClientSide())) {
                updatePreview();
            }
            setChanged();
        }
    };

    public ChipFuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIP_FUSER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isResultReady() {
        return resultReady;
    }

    public void setResultReady(boolean resultReady) {
        this.resultReady = resultReady;
    }

    public int getPreviewCost() {
        return ChipFusionRegistry.getPreviewCost(inventory.getStackInSlot(SLOT_LEFT), inventory.getStackInSlot(SLOT_RIGHT));
    }

    public ItemStack getPreviewResult() {
        if (resultReady) {
            return inventory.getStackInSlot(SLOT_RESULT);
        }
        return ChipFusionRegistry.getPreviewResult(inventory.getStackInSlot(SLOT_LEFT), inventory.getStackInSlot(SLOT_RIGHT));
    }

    public boolean canFuse(Player player) {
        if (player == null || resultReady) {
            return false;
        }

        ItemStack preview = getPreviewResult();
        if (preview.isEmpty()) {
            return false;
        }

        return player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                .map(coins -> ChipFusionRegistry.canAfford(coins, inventory.getStackInSlot(SLOT_LEFT), inventory.getStackInSlot(SLOT_RIGHT)))
                .orElse(false);
    }

    public boolean fuse(Player player) {
        if (!canFuse(player)) {
            return false;
        }

        ItemStack result = ChipFusionRegistry.getPreviewResult(inventory.getStackInSlot(SLOT_LEFT), inventory.getStackInSlot(SLOT_RIGHT));
        int cost = getPreviewCost();
        if (result.isEmpty() || cost < 0) {
            return false;
        }

        boolean spent = player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                .map(coins -> coins.trySpendCoins(cost))
                .orElse(false);
        if (!spent) {
            return false;
        }

        player.getCapability(TeenyCoinsProvider.TEENY_COINS).ifPresent(coins -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ModMessages.sendToPlayer(new PacketSyncTeenyCoins(coins.getCoins()), serverPlayer);
            }
        });

        suppressPreviewUpdate = true;
        inventory.extractItem(SLOT_LEFT, 1, false);
        inventory.extractItem(SLOT_RIGHT, 1, false);
        inventory.setStackInSlot(SLOT_RESULT, result);
        suppressPreviewUpdate = false;
        resultReady = true;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    public boolean canTakeResult() {
        return resultReady && !inventory.getStackInSlot(SLOT_RESULT).isEmpty();
    }

    public void onResultTaken() {
        if (!resultReady) {
            return;
        }
        resultReady = false;
        updatePreview();
        setChanged();
    }

    public void updatePreview() {
        if (resultReady) {
            return;
        }
        suppressPreviewUpdate = true;
        inventory.setStackInSlot(SLOT_RESULT, ChipFusionRegistry.getPreviewResult(
                inventory.getStackInSlot(SLOT_LEFT), inventory.getStackInSlot(SLOT_RIGHT)));
        suppressPreviewUpdate = false;
        setChanged();
    }

    public void dropStoredItems() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (slot == SLOT_RESULT && !resultReady) {
                continue;
            }

            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.teenycraft.chip_fuser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChipFuserMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putBoolean("ResultReady", resultReady);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        resultReady = tag.getBoolean("ResultReady");
        if (!resultReady && (this.level == null || !this.level.isClientSide())) {
            updatePreview();
        }
    }
}
