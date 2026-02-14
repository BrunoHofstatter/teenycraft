package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketChangeBox;
import bruhof.teenycraft.networking.PacketSearchTitanManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class TitanManagerScreen extends AbstractContainerScreen<TitanManagerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TeenyCraft.MOD_ID, "textures/gui/titan_manager.png");
    
    private EditBox searchBox;

    public TitanManagerScreen(TitanManagerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Add Previous Box Button
        this.addRenderableWidget(Button.builder(Component.literal("<"), (button) -> {
            changeBox(-1);
        }).pos(x + 7, y + 38).size(20, 14).build());

        // Add Next Box Button
        this.addRenderableWidget(Button.builder(Component.literal(">"), (button) -> {
            changeBox(1);
        }).pos(x + 149, y + 38).size(20, 14).build());
        
        // Add Search Bar
        // Positioned in the middle where "BOX X" label usually is
        this.searchBox = new EditBox(this.font, x + 35, y + 40, 90, 10, Component.literal("Search")); // Shorter to fit page
        this.searchBox.setMaxLength(32);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
    }
    
    private void onSearchChanged(String query) {
        // 1. Update Client Side Menu state (sets isSearchMode)
        this.menu.updateSearch(query);
        // 2. Send packet to server, let server handle logic and sync results back
        ModMessages.sendToServer(new PacketSearchTitanManager(query));
        
        // Temporarily clear results while waiting
        if (!query.isEmpty()) {
            this.menu.setSearchResults(new ArrayList<>());
        }
    }

    private void changeBox(int delta) {
        int newBox = this.menu.getCurrentBox() + delta;
        // 1. Update Client Side for immediate feel
        this.menu.setBox(newBox);
        // 2. Tell Server
        ModMessages.sendToServer(new PacketChangeBox(newBox));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Background
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF222222);

        // Texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        
        // Slot Placeholders
        drawSlotPlaceholder(guiGraphics, x + 61, y + 19);
        drawSlotPlaceholder(guiGraphics, x + 79, y + 19);
        drawSlotPlaceholder(guiGraphics, x + 97, y + 19);
        drawSlotPlaceholder(guiGraphics, x + 133, y + 19);

        // Box Area and Grid
        guiGraphics.fill(x + 7, y + 53, x + 169, y + 163, 0xFF111111);
        for(int row = 0; row < 6; row++) {
            for(int col = 0; col < 9; col++) {
                drawSlotPlaceholder(guiGraphics, x + 7 + col * 18, y + 53 + row * 18);
            }
        }

        // Search Bar / Label Area
        guiGraphics.fill(x + 30, y + 38, x + 146, y + 52, 0xFF333333);
        
        // Label Logic
        // We always keep the search box visible so it can be clicked.
        this.searchBox.setVisible(true);

        // If the box is empty and NOT focused, we draw the "BOX X" label over it.
        // The search box is transparent (no border), so this looks fine.
        if (!this.searchBox.isFocused() && this.searchBox.getValue().isEmpty()) {
            String boxName = "BOX " + (this.menu.getCurrentBox() + 1);
            guiGraphics.drawString(this.font, boxName, x + (imageWidth / 2) - (this.font.width(boxName) / 2), y + 41, 0xFFFFFFFF, false);
        } else {
             // In search mode, show Page Number to the right
             if (!this.searchBox.getValue().isEmpty()) {
                 int currentPage = this.menu.getCurrentBox() + 1;
                 int maxPages = (int) Math.ceil((double)this.menu.getSearchMatchCount() / 54.0);
                 if (maxPages < 1) maxPages = 1;
                 String pageText = currentPage + "/" + maxPages;
                 guiGraphics.drawString(this.font, pageText, x + 130, y + 41, 0xFFAAAAAA, false);
             }
        }

        // Player Inventory
        guiGraphics.fill(x + 7, y + 173, x + 169, y + 250, 0xFF111111);
    }
    
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.searchBox.isFocused()) {
            // Prevent closing with E or Inventory Key
            if (pKeyCode == 256) { // ESC
                 this.searchBox.setFocused(false);
                 return true; // Consume
            }
            // Check if key is bound to inventory
            if (Minecraft.getInstance().options.keyInventory.matches(pKeyCode, pScanCode)) {
                return true; // Consume the key press, do nothing (don't close GUI)
            }
            if (this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private void drawSlotPlaceholder(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, 0xFF333333);
        gui.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gui.fill(x + 1, y + 1, x + 16, y + 16, 0xFF333333);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
