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

import net.minecraft.client.gui.components.ImageButton;

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

        // Add Previous Box Button (<)
        // Texture: 177, 0 | Hover: 177, 14 | Size: 20x14
        this.addRenderableWidget(new ImageButton(x + 7, y + 38, 20, 14, 177, 0, 14, TEXTURE, (button) -> {
            changeBox(-1);
        }));

        // Add Next Box Button (>)
        // Texture: 197, 0 | Hover: 197, 14 | Size: 20x14
        this.addRenderableWidget(new ImageButton(x + 149, y + 38, 20, 14, 197, 0, 14, TEXTURE, (button) -> {
            changeBox(1);
        }));
        
        // Add Search Bar
        // Positioned in the middle where "PAGE X" label usually is
        this.searchBox = new EditBox(this.font, x + 35, y + 40, 90, 10, Component.literal("Search")); // Shorter to fit page
        this.searchBox.setMaxLength(32);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        
        // --- SORT BUTTONS ---
        int startX = x + 8;
        int startY = y + 18;
        int btnW = 12;
        int btnH = 12;

        // A-Z (A)
        // Texture: 177, 28 | Hover: 177, 40 (Diff 12) | Size: 12x12
        ImageButton sortA = new ImageButton(startX, startY, btnW, btnH, 177, 28, 12, TEXTURE, (btn) -> {
            ModMessages.sendToServer(new bruhof.teenycraft.networking.PacketSortTitanManager(bruhof.teenycraft.networking.PacketSortTitanManager.SortType.ALPHABETICAL));
        });
        sortA.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort A-Z")));
        this.addRenderableWidget(sortA);

        // Level Desc (v) (Strongest)
        // Texture: 189, 28 | Hover: 189, 40 (Diff 12) | Size: 12x12
        ImageButton sortDesc = new ImageButton(startX + 14, startY, btnW, btnH, 189, 28, 12, TEXTURE, (btn) -> {
            ModMessages.sendToServer(new bruhof.teenycraft.networking.PacketSortTitanManager(bruhof.teenycraft.networking.PacketSortTitanManager.SortType.LEVEL_DESC));
        });
        sortDesc.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort Level (High->Low)")));
        this.addRenderableWidget(sortDesc);

        // Level Asc (^) (Weakest)
        // Texture: 201, 28 | Hover: 201, 40 (Diff 12) | Size: 12x12
        ImageButton sortAsc = new ImageButton(startX + 28, startY, btnW, btnH, 201, 28, 12, TEXTURE, (btn) -> {
            ModMessages.sendToServer(new bruhof.teenycraft.networking.PacketSortTitanManager(bruhof.teenycraft.networking.PacketSortTitanManager.SortType.LEVEL_ASC));
        });
        sortAsc.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort Level (Low->High)")));
        this.addRenderableWidget(sortAsc);
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

        // Texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        
        // Label Logic
        // We always keep the search box visible so it can be clicked.
        this.searchBox.setVisible(true);

        // If the box is empty and NOT focused, we draw the "PAGE X" label over it.
        // The search box is transparent (no border), so this looks fine.
        if (!this.searchBox.isFocused() && this.searchBox.getValue().isEmpty()) {
            String boxName = "PAGE " + (this.menu.getCurrentBox() + 1);
            guiGraphics.drawString(this.font, boxName, x + (imageWidth / 2) - (this.font.width(boxName) / 2), y + 41, 0xFF404040, false);
        } else {
             // In search mode, show Page Number to the right
             if (!this.searchBox.getValue().isEmpty()) {
                 int currentPage = this.menu.getCurrentBox() + 1;
                 int maxPages = (int) Math.ceil((double)this.menu.getSearchMatchCount() / 54.0);
                 if (maxPages < 1) maxPages = 1;
                 String pageText = currentPage + "/" + maxPages;
                 guiGraphics.drawString(this.font, pageText, x + 128, y + 41, 0xFF404040, false);
             }
        }
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
