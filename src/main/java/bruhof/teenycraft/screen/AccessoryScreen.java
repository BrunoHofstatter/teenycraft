package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.accessory.AccessoryMilestoneDefinition;
import bruhof.teenycraft.accessory.AccessoryMilestoneRegistry;
import bruhof.teenycraft.accessory.AccessoryPresentation;
import bruhof.teenycraft.accessory.AccessoryProgression;
import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import bruhof.teenycraft.capability.IAccessoryMastery;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AccessoryScreen extends AbstractContainerScreen<AccessoryScreenMenu> {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 252;
    private static final int CONTENT_LEFT = 10;
    private static final int CONTENT_RIGHT = PANEL_WIDTH - 10;
    private static final int TIER_START_Y = 59;
    private static final int UNLOCKED_ROW_HEIGHT = 27;
    private static final int CURRENT_ROW_HEIGHT = 41;

    private Button purchaseButton;

    public AccessoryScreen(AccessoryScreenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.titleLabelX = 40;
        this.titleLabelY = 10;
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        this.purchaseButton = addRenderableWidget(Button.builder(Component.empty(), button -> purchaseNextTier())
                .bounds(this.leftPos + 166, this.topPos + 224, 154, 20)
                .build());
        updateButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF171B22);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF272E38);
        guiGraphics.fill(x + 8, y + 7, x + imageWidth - 8, y + 53, 0xFF202630);
        guiGraphics.fill(x + 9, y + 8, x + imageWidth - 9, y + 52, 0xFF303846);

        ItemStack accessoryStack = createAccessoryStack();
        if (!accessoryStack.isEmpty()) {
            guiGraphics.fill(x + 14, y + 13, x + 36, y + 35, 0xFF171B22);
            guiGraphics.fill(x + 15, y + 14, x + 35, y + 34, 0xFF465164);
            guiGraphics.renderItem(accessoryStack, x + 17, y + 16);
        }

        ProgressView progress = getProgressView();
        int rowY = y + TIER_START_Y;
        for (int tier = 1; tier <= progress.visibleTier(); tier++) {
            boolean currentChallenge = tier == progress.nextTier();
            int rowHeight = currentChallenge ? CURRENT_ROW_HEIGHT : UNLOCKED_ROW_HEIGHT;
            int border = currentChallenge ? 0xFFD9B64A : 0xFF4A5566;
            int fill = currentChallenge ? 0xFF353329 : 0xFF222832;
            guiGraphics.fill(x + CONTENT_LEFT, rowY, x + CONTENT_RIGHT, rowY + rowHeight - 2, border);
            guiGraphics.fill(x + CONTENT_LEFT + 1, rowY + 1, x + CONTENT_RIGHT - 1, rowY + rowHeight - 3, fill);

            if (currentChallenge && progress.definition() != null) {
                int barX = x + 205;
                int barY = rowY + 5;
                int barWidth = 105;
                guiGraphics.fill(barX, barY, barX + barWidth, barY + 7, 0xFF15191F);
                long target = Math.max(1L, progress.definition().target());
                int filled = (int) Math.round(barWidth * Mth.clamp(progress.milestoneProgress() / (double) target, 0.0, 1.0));
                guiGraphics.fill(barX, barY, barX + filled, barY + 7,
                        progress.milestoneComplete() ? 0xFF68C47B : 0xFFD9B64A);
            }
            rowY += rowHeight;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String accessoryId = menu.getAccessoryId();
        ProgressView progress = getProgressView();

        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFF1E9D5, false);
        String role = AccessoryPresentation.role(accessoryId);
        guiGraphics.drawString(font, role, CONTENT_RIGHT - font.width(role), 11, 0xFFD9B64A, false);
        List<net.minecraft.util.FormattedCharSequence> descriptionLines =
                font.split(Component.literal(AccessoryPresentation.description(accessoryId)), 270);
        for (int i = 0; i < Math.min(2, descriptionLines.size()); i++) {
            guiGraphics.drawString(font, descriptionLines.get(i), 40, 25 + i * 10, 0xFFB7C1CF, false);
        }

        int rowY = TIER_START_Y;
        for (int tier = 1; tier <= progress.visibleTier(); tier++) {
            boolean currentChallenge = tier == progress.nextTier();
            if (!currentChallenge) {
                String heading = "Tier " + tier + "  UNLOCKED";
                if (tier > 1) {
                    AccessoryMilestoneDefinition pastDefinition = AccessoryMilestoneRegistry.get(
                            AccessoryProgression.milestoneIdForUnlock(accessoryId, tier));
                    heading += " - " + AccessoryPresentation.milestone(pastDefinition);
                }
                drawTrimmed(guiGraphics, heading, 17, rowY + 4, 296, 0xFF8FE39A);
                drawTrimmed(guiGraphics, AccessoryPresentation.tierUpgrade(accessoryId, tier), 17, rowY + 15,
                        296, 0xFFB7C1CF);
                rowY += UNLOCKED_ROW_HEIGHT;
                continue;
            }

            String progressText = progress.definition() == null
                    ? "UNAVAILABLE"
                    : progress.milestoneProgress() + "/" + progress.definition().target();
            int statusColor = progress.milestoneComplete() ? 0xFF8FE39A : 0xFFD9B64A;
            guiGraphics.drawString(font, "Tier " + tier + "  " + progressText, 17, rowY + 4, statusColor, false);
            drawTrimmed(guiGraphics, "Milestone: " + AccessoryPresentation.milestone(progress.definition()),
                    17, rowY + 16, 296, 0xFFE1D7BD);
            drawTrimmed(guiGraphics, "Upgrade: " + AccessoryPresentation.tierUpgrade(accessoryId, tier),
                    17, rowY + 27, 296, 0xFFB7C1CF);
            rowY += CURRENT_ROW_HEIGHT;
        }

        String coins = "TC " + NumberFormat.getIntegerInstance(Locale.US).format(progress.coins());
        guiGraphics.drawString(font, coins, 12, 231, 0xFFD9B64A, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (mouseX >= leftPos + 14 && mouseX < leftPos + 36 && mouseY >= topPos + 13 && mouseY < topPos + 35) {
            ItemStack stack = createAccessoryStack();
            if (!stack.isEmpty()) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
    }

    private void purchaseNextTier() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    AccessoryScreenMenu.BUTTON_PURCHASE_NEXT_TIER);
        }
    }

    private void updateButtonState() {
        if (purchaseButton == null) {
            return;
        }

        ProgressView progress = getProgressView();
        purchaseButton.visible = progress.nextTier() <= AccessoryProgression.MAX_TIER;
        if (!purchaseButton.visible) {
            return;
        }

        int cost = TeenyBalance.getAccessoryTierUpgradeCost(progress.nextTier());
        if (progress.definition() == null) {
            purchaseButton.setMessage(Component.literal("Milestone unavailable"));
            purchaseButton.active = false;
        } else if (!progress.milestoneComplete()) {
            purchaseButton.setMessage(Component.literal("Complete milestone"));
            purchaseButton.active = false;
        } else if (!bruhof.teenycraft.accessory.AccessoryMasteryService.isTierPurchaseImplemented(
                menu.getAccessoryId(), progress.nextTier())) {
            purchaseButton.setMessage(Component.literal("Mastery upgrade unavailable"));
            purchaseButton.active = false;
        } else if (progress.coins() < cost) {
            purchaseButton.setMessage(Component.literal("Need " + cost + " TC"));
            purchaseButton.active = false;
        } else {
            purchaseButton.setMessage(Component.literal("Unlock Tier " + progress.nextTier() + " - " + cost + " TC"));
            purchaseButton.active = true;
        }
    }

    private ProgressView getProgressView() {
        int tier = AccessoryProgression.MIN_TIER;
        long milestoneProgress = 0;
        boolean milestoneComplete = false;
        int coins = 0;
        String accessoryId = menu.getAccessoryId();

        if (minecraft != null && minecraft.player != null) {
            IAccessoryMastery mastery = minecraft.player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                    .orElse(null);
            if (mastery != null) {
                tier = mastery.getTier(accessoryId);
                milestoneProgress = mastery.getMilestoneProgress(accessoryId);
                milestoneComplete = mastery.isCurrentMilestoneComplete(accessoryId);
            }
            coins = minecraft.player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                    .map(handler -> handler.getCoins())
                    .orElse(0);
        }

        int nextTier = tier + 1;
        int visibleTier = Math.min(AccessoryProgression.MAX_TIER, nextTier);
        AccessoryMilestoneDefinition definition = nextTier <= AccessoryProgression.MAX_TIER
                ? AccessoryMilestoneRegistry.get(AccessoryProgression.milestoneIdForUnlock(accessoryId, nextTier))
                : null;
        return new ProgressView(tier, nextTier, visibleTier, milestoneProgress, milestoneComplete, coins, definition);
    }

    private ItemStack createAccessoryStack() {
        ResourceLocation itemId = new ResourceLocation(TeenyCraft.MOD_ID, "accessory_" + menu.getAccessoryId());
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void drawTrimmed(GuiGraphics guiGraphics, String text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, font.plainSubstrByWidth(text, width), x, y, color, false);
    }

    private record ProgressView(
            int tier,
            int nextTier,
            int visibleTier,
            long milestoneProgress,
            boolean milestoneComplete,
            int coins,
            AccessoryMilestoneDefinition definition
    ) {
    }
}
