package bruhof.teenycraft.screen;

import bruhof.teenycraft.block.ModBlocks;
import bruhof.teenycraft.block.entity.ChipFuserBlockEntity;
import bruhof.teenycraft.item.custom.ItemChip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ChipFuserMenu extends AbstractContainerMenu {
    private final ChipFuserBlockEntity blockEntity;
    private final ContainerData data;

    public ChipFuserMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData.readBlockPos()));
    }

    public ChipFuserMenu(int containerId, Inventory playerInventory, ChipFuserBlockEntity blockEntity) {
        super(ModMenuTypes.CHIP_FUSER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) {
                    return blockEntity.isResultReady() ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    blockEntity.setResultReady(value != 0);
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.addDataSlots(this.data);

        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), ChipFuserBlockEntity.SLOT_LEFT, 42, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemChip;
            }
        });

        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), ChipFuserBlockEntity.SLOT_RIGHT, 78, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemChip;
            }
        });

        this.addSlot(new SlotItemHandler(blockEntity.getInventory(), ChipFuserBlockEntity.SLOT_RESULT, 144, 28) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return blockEntity.canTakeResult();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.onResultTaken();
            }
        });

        layoutPlayerInventorySlots(playerInventory, 25, 84);
    }

    private static ChipFuserBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ChipFuserBlockEntity blockEntity) {
            return blockEntity;
        }
        throw new IllegalStateException("Missing Chip Fuser block entity at " + pos);
    }

    public ChipFuserBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getPreviewCost() {
        return blockEntity.getPreviewCost();
    }

    public boolean isResultReady() {
        return this.data.get(0) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            boolean fused = blockEntity.fuse(player);
            if (fused) {
                this.broadcastChanges();
            }
            return fused;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index == ChipFuserBlockEntity.SLOT_RESULT) {
            if (!this.moveItemStackTo(sourceStack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onTake(player, sourceStack);
        } else if (index < 3) {
            if (!this.moveItemStackTo(sourceStack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (sourceStack.getItem() instanceof ItemChip) {
                if (!this.moveItemStackTo(sourceStack, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                this.blockEntity.getLevel(), this.blockEntity.getBlockPos()), player, ModBlocks.CHIP_FUSER.get());
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
