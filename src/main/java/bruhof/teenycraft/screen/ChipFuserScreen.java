package bruhof.teenycraft.screen;

import bruhof.teenycraft.block.entity.ChipFuserBlockEntity;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.text.NumberFormat;
import java.util.Locale;

public class ChipFuserScreen extends AbstractContainerScreen<ChipFuserMenu> {
    private Button fuseButton;

    public ChipFuserScreen(ChipFuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 206;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;

        this.fuseButton = this.addRenderableWidget(Button.builder(Component.literal("FUSE"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(this.leftPos + 166, this.topPos + 24, 34, 24).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.fuseButton != null) {
            boolean validPreview = !this.menu.getSlot(ChipFuserBlockEntity.SLOT_RESULT).getItem().isEmpty()
                    && !this.menu.isResultReady();
            this.fuseButton.active = validPreview;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.fill(x, y, x + this.imageWidth, y + 70, 0xFF2B2F36);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 69, 0xFF3A404A);

        guiGraphics.fill(x, y + 72, x + this.imageWidth, y + this.imageHeight, 0xFF2B2F36);
        guiGraphics.fill(x + 1, y + 73, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF4A4034);

        drawSlotFrame(guiGraphics, this.menu.getSlot(0));
        drawSlotFrame(guiGraphics, this.menu.getSlot(1));
        drawSlotFrame(guiGraphics, this.menu.getSlot(2));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFECE6D5, false);

        int coins = 0;
        if (this.minecraft != null && this.minecraft.player != null) {
            coins = this.minecraft.player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                    .map(handler -> handler.getCoins())
                    .orElse(0);
        }

        int cost = this.menu.getPreviewCost();
        String coinsText = "Coins: " + NumberFormat.getIntegerInstance(Locale.US).format(coins);
        String costText = cost >= 0 ? "Cost: " + cost : "Cost: --";

        guiGraphics.drawString(this.font, coinsText, 8, 16, 0xFFD9B64A, false);
        guiGraphics.drawString(this.font, costText, 8, 28, 0xFFECE6D5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFECE6D5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawSlotFrame(GuiGraphics guiGraphics, Slot slot) {
        int x = this.leftPos + slot.x - 1;
        int y = this.topPos + slot.y - 1;
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1D1F24);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8E97);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF545964);
    }
}
