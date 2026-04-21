package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTeenyCoins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FigureScreenMenu extends AbstractContainerMenu {
    public static final int SLOT_PREVIEW_CHIP = 0;
    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_END = 37;

    public static final int BUTTON_UPGRADE_HEALTH = 0;
    public static final int BUTTON_UPGRADE_POWER = 1;
    public static final int BUTTON_UPGRADE_DODGE = 2;
    public static final int BUTTON_UPGRADE_LUCK = 3;
    public static final int BUTTON_INSTALL_CHIP = 4;
    public static final int BUTTON_APPLY_ORDER_BASE = 1000;

    private final Player player;
    private final InteractionHand boundHand;
    private final int boundMainHandSlot;
    private final SimpleContainer chipPreviewContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            FigureScreenMenu.this.broadcastChanges();
        }
    };

    public FigureScreenMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readEnum(InteractionHand.class), extraData.readInt());
    }

    public FigureScreenMenu(int containerId, Inventory inventory, InteractionHand boundHand, int boundMainHandSlot) {
        super(ModMenuTypes.FIGURE_SCREEN_MENU.get(), containerId);
        this.player = inventory.player;
        this.boundHand = boundHand;
        this.boundMainHandSlot = boundMainHandSlot;

        this.addSlot(new Slot(this.chipPreviewContainer, SLOT_PREVIEW_CHIP, 18, 93) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemChip;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        layoutPlayerInventorySlots(inventory, 44, 186);
    }

    public ItemStack getFigureStack() {
        if (boundHand == InteractionHand.OFF_HAND) {
            return player.getOffhandItem();
        }
        if (boundMainHandSlot < 0 || boundMainHandSlot >= 9) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(boundMainHandSlot);
    }

    public String getCurrentOrderCode() {
        ItemStack figureStack = getFigureStack();
        return ItemFigure.getAbilityOrderCodeFromPool(figureStack, ItemFigure.getAbilityOrder(figureStack));
    }

    public ItemStack getPreviewChipStack() {
        return this.chipPreviewContainer.getItem(SLOT_PREVIEW_CHIP);
    }

    public ItemStack getEquippedChipStack() {
        return ItemFigure.getEquippedChip(getFigureStack());
    }

    public boolean canInstallPreviewChip() {
        return getFigureStack().getItem() instanceof ItemFigure && getPreviewChipStack().getItem() instanceof ItemChip;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStack figureStack = getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            return false;
        }

        ItemFigure.UpgradeChoice choice = ItemFigure.UpgradeChoice.fromButtonId(id);
        if (choice != null) {
            boolean changed = ItemFigure.spendPendingUpgrade(figureStack, choice);
            if (!changed && !player.level().isClientSide()) {
                player.sendSystemMessage(Component.literal("Upgrade choice is not available."));
            }
            if (changed) {
                this.broadcastChanges();
            }
            return changed;
        }

        if (id == BUTTON_INSTALL_CHIP) {
            boolean installed = installPreviewChip();
            if (!installed && !player.level().isClientSide()) {
                player.sendSystemMessage(Component.literal("Place a chip in the install slot first."));
            }
            if (installed) {
                this.broadcastChanges();
            }
            return installed;
        }

        if (id >= BUTTON_APPLY_ORDER_BASE) {
            String requestedOrderCode = Integer.toString(id - BUTTON_APPLY_ORDER_BASE);
            String currentOrderCode = getCurrentOrderCode();
            if (requestedOrderCode.equals(currentOrderCode)) {
                return false;
            }
            if (ItemFigure.getLevel(figureStack) < TeenyBalance.FIGURE_REORDER_MIN_LEVEL) {
                if (!player.level().isClientSide()) {
                    player.sendSystemMessage(Component.literal("This figure needs to reach level " + TeenyBalance.FIGURE_REORDER_MIN_LEVEL + " first."));
                }
                return false;
            }

            boolean spent = player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                    .map(coins -> coins.trySpendCoins(TeenyBalance.FIGURE_REORDER_COST))
                    .orElse(false);
            if (!spent) {
                if (!player.level().isClientSide()) {
                    player.sendSystemMessage(Component.literal("Not enough Teeny Coins."));
                }
                return false;
            }

            ItemFigure.setAbilityOrderFromPoolCode(figureStack, requestedOrderCode);
            if (player instanceof ServerPlayer serverPlayer) {
                player.getCapability(TeenyCoinsProvider.TEENY_COINS).ifPresent(coins ->
                        ModMessages.sendToPlayer(new PacketSyncTeenyCoins(coins.getCoins()), serverPlayer));
            }
            this.broadcastChanges();
            return true;
        }

        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = sourceSlot.getItem().copy();

        if (index == SLOT_PREVIEW_CHIP) {
            if (!this.moveItemStackTo(sourceSlot.getItem(), PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
            if (!(sourceSlot.getItem().getItem() instanceof ItemChip)
                    || !this.moveItemStackTo(sourceSlot.getItem(), SLOT_PREVIEW_CHIP, SLOT_PREVIEW_CHIP + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceSlot.getItem().isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack figureStack = getFigureStack();
        return player.isAlive() && figureStack.getItem() instanceof ItemFigure;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.clearContainer(player, this.chipPreviewContainer);
        }
    }

    private boolean installPreviewChip() {
        ItemStack figureStack = getFigureStack();
        ItemStack previewChip = getPreviewChipStack();
        if (!(figureStack.getItem() instanceof ItemFigure) || !(previewChip.getItem() instanceof ItemChip)) {
            return false;
        }

        ItemFigure.installChip(figureStack, previewChip);
        this.chipPreviewContainer.setItem(SLOT_PREVIEW_CHIP, ItemStack.EMPTY);
        return true;
    }

    private void layoutPlayerInventorySlots(Inventory inventory, int leftCol, int topRow) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(new FigureInventorySlot(inventory, slotIndex, leftCol + col * 18, topRow + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new FigureInventorySlot(inventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    private class FigureInventorySlot extends Slot {
        private final boolean boundSlot;

        private FigureInventorySlot(Inventory inventory, int slotIndex, int x, int y) {
            super(inventory, slotIndex, x, y);
            this.boundSlot = boundHand == InteractionHand.MAIN_HAND && slotIndex == boundMainHandSlot;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !boundSlot && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !boundSlot && super.mayPlace(stack);
        }
    }
}
