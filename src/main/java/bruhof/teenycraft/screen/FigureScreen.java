package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import bruhof.teenycraft.util.FigurePreviewHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FigureScreen extends AbstractContainerScreen<FigureScreenMenu> {
    private static final int PANEL_WIDTH = 286;
    private static final int PANEL_HEIGHT = 282;

    private static final int SUMMARY_TOP = 8;
    private static final int CHIP_ROW_TOP = 82;
    private static final int ABILITY_SECTION_TOP = 122;
    private static final int ABILITY_START_Y = 134;
    private static final int ABILITY_ROW_HEIGHT = 18;
    private static final int INVENTORY_TOP = 194;

    private static final int EQUIPPED_CHIP_X = 112;
    private static final int EQUIPPED_CHIP_Y = 93;

    private final List<Button> upgradeButtons = new ArrayList<>();
    private final List<Button> moveUpButtons = new ArrayList<>();
    private final List<Button> moveDownButtons = new ArrayList<>();
    private Button installChipButton;
    private Button applyOrderButton;

    private List<String> draftOrder = new ArrayList<>();
    private String lastServerOrderCode = "";

    public FigureScreen(FigureScreenMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelX = 44;
        this.inventoryLabelY = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.upgradeButtons.clear();
        this.moveUpButtons.clear();
        this.moveDownButtons.clear();
        syncDraftOrderFromServer();

        int baseX = this.leftPos;
        int baseY = this.topPos;

        int[] buttonIds = {
                FigureScreenMenu.BUTTON_UPGRADE_HEALTH,
                FigureScreenMenu.BUTTON_UPGRADE_POWER,
                FigureScreenMenu.BUTTON_UPGRADE_DODGE,
                FigureScreenMenu.BUTTON_UPGRADE_LUCK
        };
        String[] labels = {"Health", "Power", "Dodge", "Luck"};
        int[][] positions = {
                {178, 35},
                {230, 35},
                {178, 57},
                {230, 57}
        };

        for (int i = 0; i < buttonIds.length; i++) {
            final int buttonId = buttonIds[i];
            Button button = this.addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> submitMenuButton(buttonId))
                    .bounds(baseX + positions[i][0], baseY + positions[i][1], 48, 18)
                    .build());
            this.upgradeButtons.add(button);
        }

        this.installChipButton = this.addRenderableWidget(Button.builder(Component.literal("INSTALL"),
                        b -> submitMenuButton(FigureScreenMenu.BUTTON_INSTALL_CHIP))
                .bounds(baseX + 42, baseY + 91, 56, 20)
                .build());
        this.installChipButton.setTooltip(Tooltip.create(Component.literal("Install the preview chip. If a chip is already equipped, it will be destroyed.")));

        for (int i = 0; i < 3; i++) {
            final int index = i;
            int buttonY = baseY + ABILITY_START_Y + i * ABILITY_ROW_HEIGHT + 1;
            Button up = this.addRenderableWidget(Button.builder(Component.literal("^"), b -> moveDraftAbility(index, -1))
                    .bounds(baseX + 179, buttonY, 11, 16)
                    .build());
            Button down = this.addRenderableWidget(Button.builder(Component.literal("v"), b -> moveDraftAbility(index, 1))
                    .bounds(baseX + 192, buttonY, 11, 16)
                    .build());
            this.moveUpButtons.add(up);
            this.moveDownButtons.add(down);
        }

        this.applyOrderButton = this.addRenderableWidget(Button.builder(Component.literal("APPLY"), b -> applyDraftOrder())
                .bounds(baseX + 210, baseY + 134, 62, 20)
                .build());

        updateButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncDraftOrderFromServer();
        updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1D2229);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF2A313A);

        drawSection(guiGraphics, x + 8, y + SUMMARY_TOP, x + this.imageWidth - 8, y + 76, 0xFF222831, 0xFF313843);
        drawSection(guiGraphics, x + 8, y + CHIP_ROW_TOP, x + this.imageWidth - 8, y + 116, 0xFF222831, 0xFF313843);
        drawSection(guiGraphics, x + 8, y + ABILITY_SECTION_TOP, x + 205, y + 186, 0xFF222831, 0xFF313843);
        drawSection(guiGraphics, x + 210, y + ABILITY_SECTION_TOP, x + this.imageWidth - 8, y + 186, 0xFF222831, 0xFF313843);
        drawSection(guiGraphics, x + 36, y + 182, x + this.imageWidth - 8, y + this.imageHeight - 8, 0xFF242224, 0xFF4C4034);

        drawXpBar(guiGraphics, x + 40, y + 35);
        drawMenuSlotFrames(guiGraphics);
        drawStaticSlotFrame(guiGraphics, x + 13, y + 15);
        drawStaticSlotFrame(guiGraphics, x + EQUIPPED_CHIP_X, y + EQUIPPED_CHIP_Y);
        drawAbilityRowFrames(guiGraphics);

        ItemStack figureStack = this.menu.getFigureStack();
        if (!figureStack.isEmpty()) {
            guiGraphics.renderItem(figureStack, x + 14, y + 16);
        }

        ItemStack equippedChip = this.menu.getEquippedChipStack();
        if (!equippedChip.isEmpty()) {
            guiGraphics.renderItem(equippedChip, x + EQUIPPED_CHIP_X + 1, y + EQUIPPED_CHIP_Y + 1);
        }

        if (figureStack.getItem() instanceof ItemFigure) {
            List<FigurePreviewHelper.AbilityPreview> abilityPreviews = FigurePreviewHelper.buildAbilityPreviews(figureStack, draftOrder);
            for (int i = 0; i < abilityPreviews.size(); i++) {
                ItemStack abilityIcon = createAbilityIconStack(abilityPreviews.get(i), i);
                guiGraphics.renderItem(abilityIcon, x + 12, y + ABILITY_START_Y + i * ABILITY_ROW_HEIGHT + 1);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack figureStack = this.menu.getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            return;
        }

        int coins = 0;
        if (this.minecraft != null && this.minecraft.player != null) {
            coins = this.minecraft.player.getCapability(TeenyCoinsProvider.TEENY_COINS)
                    .map(handler -> handler.getCoins())
                    .orElse(0);
        }

        int level = ItemFigure.getLevel(figureStack);
        int xp = ItemFigure.getXp(figureStack);
        int xpNeeded = ItemFigure.getXpRequiredForNextLevel(figureStack);
        int pendingUpgrades = ItemFigure.getPendingUpgradePoints(figureStack);
        FigurePreviewHelper.FigureStatsPreview statsPreview = FigurePreviewHelper.buildStatPreview(figureStack);
        List<FigurePreviewHelper.AbilityPreview> abilityPreviews = FigurePreviewHelper.buildAbilityPreviews(figureStack, draftOrder);

        String figureName = ItemFigure.getFigureName(figureStack);
        String figureClass = ItemFigure.getFigureClass(figureStack);
        int nameX = 40;
        int nameWidth = this.font.width(figureName);
        guiGraphics.drawString(this.font, figureName, nameX, 8, 0xFFECE6D5, false);
        drawClassBadge(guiGraphics, figureClass, nameX + nameWidth + 6, 7);

        guiGraphics.drawString(this.font, "Lvl " + level, 40, 24, 0xFFD9B64A, false);
        guiGraphics.drawString(this.font, xpNeeded > 0 ? (xp + "/" + xpNeeded + " XP") : "MAX LEVEL", 80, 24, 0xFFB8C2CF, false);

        drawStatLine(guiGraphics, "HP", statsPreview.hp(), 40, 49, 0xFFE16A6A);
        drawStatLine(guiGraphics, "PWR", statsPreview.power(), 112, 49, 0xFFD98CC7);
        drawStatLine(guiGraphics, "DDG", statsPreview.dodge(), 40, 63, 0xFF74D0E3);
        drawStatLine(guiGraphics, "LUK", statsPreview.luck(), 112, 63, 0xFFF0CF61);

        String coinsText = "TC " + NumberFormat.getIntegerInstance(Locale.US).format(coins);
        guiGraphics.drawString(this.font, coinsText, this.imageWidth - 12 - this.font.width(coinsText), 8, 0xFFD9B64A, false);
        guiGraphics.drawString(this.font, "Upgrades", 178, 16, 0xFFECE6D5, false);
        guiGraphics.drawString(this.font, pendingUpgrades > 0 ? "Pending: " + pendingUpgrades : "No pending points", 178, 25, pendingUpgrades > 0 ? 0xFF8FE388 : 0xFF9AA2AF, false);

        guiGraphics.drawString(this.font, "Chip", 8, 82, 0xFFECE6D5, false);
        guiGraphics.drawString(this.font, "Equipped", EQUIPPED_CHIP_X - 4, 82, 0xFFB8C2CF, false);

        ItemStack equippedChip = this.menu.getEquippedChipStack();
        if (equippedChip.isEmpty()) {
            guiGraphics.drawString(this.font, "No chip equipped", 136, 97, 0xFF9AA2AF, false);
        } else {
            String chipName = this.font.plainSubstrByWidth(equippedChip.getHoverName().getString(), 130);
            guiGraphics.drawString(this.font, chipName, 136, 97, 0xFF9ED0FF, false);
        }

        guiGraphics.drawString(this.font, "Abilities", 8, 122, 0xFFECE6D5, false);
        guiGraphics.drawString(this.font, "Reorder", 210, 122, 0xFFECE6D5, false);
        String costText = "Cost: " + TeenyBalance.FIGURE_REORDER_COST + " TC";
        guiGraphics.drawString(this.font, costText, 210, 160, 0xFFD9B64A, false);

        for (int i = 0; i < abilityPreviews.size(); i++) {
            FigurePreviewHelper.AbilityPreview preview = abilityPreviews.get(i);
            int rowY = ABILITY_START_Y + i * ABILITY_ROW_HEIGHT;
            int nameColor = preview.golden() ? 0xFFF0CF61 : 0xFFECE6D5;
            String statText = getAbilityStatText(preview);

            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(preview.name(), 88), 30, rowY + 5, nameColor, false);
            guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(statText, 58), 112, rowY + 5, 0xFFB8C2CF, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFECE6D5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    private void updateButtonState() {
        ItemStack figureStack = this.menu.getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            return;
        }

        List<ItemFigure.UpgradeChoice> availableChoices = ItemFigure.getAvailableUpgradeChoices(figureStack);
        for (int i = 0; i < this.upgradeButtons.size(); i++) {
            Button button = this.upgradeButtons.get(i);
            ItemFigure.UpgradeChoice choice = ItemFigure.UpgradeChoice.fromButtonId(i);
            button.visible = ItemFigure.getPendingUpgradePoints(figureStack) > 0;
            button.active = choice != null && availableChoices.contains(choice);
        }

        this.installChipButton.active = this.menu.canInstallPreviewChip();

        boolean reorderUnlocked = ItemFigure.getLevel(figureStack) >= TeenyBalance.FIGURE_REORDER_MIN_LEVEL;
        for (int i = 0; i < this.moveUpButtons.size(); i++) {
            this.moveUpButtons.get(i).visible = i < draftOrder.size();
            this.moveDownButtons.get(i).visible = i < draftOrder.size();
            this.moveUpButtons.get(i).active = reorderUnlocked && i > 0;
            this.moveDownButtons.get(i).active = reorderUnlocked && i < draftOrder.size() - 1;
        }

        String currentServerCode = this.menu.getCurrentOrderCode();
        String draftCode = ItemFigure.getAbilityOrderCodeFromPool(figureStack, draftOrder);
        this.applyOrderButton.active = reorderUnlocked && !draftCode.equals(currentServerCode);
    }

    private void syncDraftOrderFromServer() {
        ItemStack figureStack = this.menu.getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            this.draftOrder = new ArrayList<>();
            this.lastServerOrderCode = "";
            return;
        }

        String currentServerCode = this.menu.getCurrentOrderCode();
        if (this.draftOrder.isEmpty() || !currentServerCode.equals(this.lastServerOrderCode)) {
            this.draftOrder = new ArrayList<>(ItemFigure.getAbilityOrder(figureStack));
            this.lastServerOrderCode = currentServerCode;
        }
    }

    private void submitMenuButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void moveDraftAbility(int index, int delta) {
        int target = index + delta;
        if (index < 0 || index >= this.draftOrder.size() || target < 0 || target >= this.draftOrder.size()) {
            return;
        }

        String ability = this.draftOrder.remove(index);
        this.draftOrder.add(target, ability);
        updateButtonState();
    }

    private void applyDraftOrder() {
        ItemStack figureStack = this.menu.getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            return;
        }

        String orderCode = ItemFigure.getAbilityOrderCodeFromPool(figureStack, draftOrder);
        if (orderCode.isEmpty()) {
            return;
        }
        submitMenuButton(FigureScreenMenu.BUTTON_APPLY_ORDER_BASE + Integer.parseInt(orderCode));
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack figureStack = this.menu.getFigureStack();
        if (!(figureStack.getItem() instanceof ItemFigure)) {
            return;
        }

        if (isMouseWithin(mouseX, mouseY, this.leftPos + 14, this.topPos + 16, 16, 16)) {
            guiGraphics.renderTooltip(this.font, figureStack, mouseX, mouseY);
            return;
        }

        if (renderInstalledChipTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        }

        if (renderClassBadgeTooltip(guiGraphics, mouseX, mouseY, ItemFigure.getFigureClass(figureStack))) {
            return;
        }

        if (renderReorderTooltip(guiGraphics, mouseX, mouseY, ItemFigure.getLevel(figureStack))) {
            return;
        }

        renderAbilityRowTooltip(guiGraphics, mouseX, mouseY, figureStack);
    }

    private boolean renderInstalledChipTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack equippedChip = this.menu.getEquippedChipStack();
        if (equippedChip.isEmpty()) {
            return false;
        }

        String chipName = this.font.plainSubstrByWidth(equippedChip.getHoverName().getString(), 130);
        boolean overSlot = isMouseWithin(mouseX, mouseY, this.leftPos + EQUIPPED_CHIP_X + 1, this.topPos + EQUIPPED_CHIP_Y + 1, 16, 16);
        boolean overName = isMouseWithin(mouseX, mouseY, this.leftPos + 136, this.topPos + 97, this.font.width(chipName), 10);
        if (overSlot || overName) {
            guiGraphics.renderTooltip(this.font, equippedChip, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private boolean renderClassBadgeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, String figureClass) {
        int badgeX = this.leftPos + getClassBadgeX();
        int badgeY = this.topPos + 7;
        int badgeWidth = getClassBadgeWidth(figureClass);
        if (badgeWidth <= 0 || !isMouseWithin(mouseX, mouseY, badgeX, badgeY, badgeWidth, 12)) {
            return false;
        }

        guiGraphics.renderTooltip(this.font, Component.literal(figureClass), mouseX, mouseY);
        return true;
    }

    private boolean renderReorderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int level) {
        String costText = "Cost: " + TeenyBalance.FIGURE_REORDER_COST + " TC";
        boolean hoverCost = isMouseWithin(mouseX, mouseY, this.leftPos + 210, this.topPos + 160, this.font.width(costText), 10);
        boolean hoverApply = isMouseWithin(mouseX, mouseY, this.applyOrderButton.getX(), this.applyOrderButton.getY(),
                this.applyOrderButton.getWidth(), this.applyOrderButton.getHeight());
        if (!hoverCost && !hoverApply) {
            return false;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Reorder cost: " + TeenyBalance.FIGURE_REORDER_COST + " Teeny Coins"));
        if (level < TeenyBalance.FIGURE_REORDER_MIN_LEVEL) {
            tooltip.add(Component.literal("Requires Level " + TeenyBalance.FIGURE_REORDER_MIN_LEVEL));
        }
        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        return true;
    }

    private void renderAbilityRowTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, ItemStack figureStack) {
        List<FigurePreviewHelper.AbilityPreview> abilityPreviews = FigurePreviewHelper.buildAbilityPreviews(figureStack, draftOrder);
        for (int i = 0; i < abilityPreviews.size(); i++) {
            int rowX = this.leftPos + 8;
            int rowY = this.topPos + ABILITY_START_Y + i * ABILITY_ROW_HEIGHT;
            if (!isMouseWithin(mouseX, mouseY, rowX, rowY, 196, 18)) {
                continue;
            }

            FigurePreviewHelper.AbilityPreview preview = abilityPreviews.get(i);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal((preview.golden() ? "\u00A76" : "\u00A7f") + preview.name()));
            tooltip.add(Component.literal("Mana: " + preview.manaCost()));
            if (preview.damage() > 0) {
                tooltip.add(Component.literal("Damage: " + preview.damage()));
            }
            if (preview.heal() > 0) {
                tooltip.add(Component.literal("Heal: " + preview.heal()));
            }

            appendWrappedTooltip(tooltip, preview.description(), 220, "\u00A77");
            if (!preview.goldenDescription().isBlank()) {
                tooltip.add(Component.literal("\u00A7eGolden"));
                appendWrappedTooltip(tooltip, preview.goldenDescription(), 220, "\u00A7e");
            }

            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private void appendWrappedTooltip(List<Component> tooltip, String text, int maxWidth, String prefix) {
        if (text == null || text.isBlank()) {
            return;
        }

        for (String line : wrapText(text.trim(), maxWidth)) {
            tooltip.add(Component.literal(prefix + line));
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            String line = this.font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty() || line.length() == remaining.length()) {
                lines.add(remaining);
                break;
            }

            int splitIndex = line.lastIndexOf(' ');
            if (splitIndex > 0) {
                line = line.substring(0, splitIndex);
            }

            lines.add(line);
            remaining = remaining.substring(line.length()).trim();
        }
        return lines;
    }

    private ItemStack createAbilityIconStack(FigurePreviewHelper.AbilityPreview preview, int slotIndex) {
        ItemStack abilityStack = new ItemStack(switch (slotIndex) {
            case 1 -> ModItems.ABILITY_2.get();
            case 2 -> ModItems.ABILITY_3.get();
            default -> ModItems.ABILITY_1.get();
        });
        ItemAbility.initializeAbility(abilityStack, preview.abilityId(), preview.name(), preview.damage(), preview.golden());
        return abilityStack;
    }

    private String getAbilityStatText(FigurePreviewHelper.AbilityPreview preview) {
        if (preview.damage() > 0) {
            return "DMG " + preview.damage();
        }
        if (preview.heal() > 0) {
            return "HEAL " + preview.heal();
        }
        return "MANA " + preview.manaCost();
    }

    private void drawSection(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int outerColor, int innerColor) {
        guiGraphics.fill(x1, y1, x2, y2, outerColor);
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, innerColor);
    }

    private void drawXpBar(GuiGraphics guiGraphics, int x, int y) {
        ItemStack figureStack = this.menu.getFigureStack();
        int xp = ItemFigure.getXp(figureStack);
        int xpNeeded = ItemFigure.getXpRequiredForNextLevel(figureStack);
        int width = 118;

        guiGraphics.fill(x, y, x + width, y + 8, 0xFF171B21);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 7, 0xFF2C3440);
        if (xpNeeded > 0) {
            int fillWidth = Math.max(0, Math.min(width - 2, Math.round((xp / (float) xpNeeded) * (width - 2))));
            guiGraphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 7, 0xFFD9B64A);
        }
    }

    private void drawAbilityRowFrames(GuiGraphics guiGraphics) {
        int x = this.leftPos + 8;
        int y = this.topPos + ABILITY_START_Y;
        for (int i = 0; i < 3; i++) {
            int rowY = y + i * ABILITY_ROW_HEIGHT;
            guiGraphics.fill(x, rowY, x + 196, rowY + 18, 0xFF1D1F24);
            guiGraphics.fill(x + 1, rowY + 1, x + 195, rowY + 17, 0xFF2A2E36);
        }
    }

    private void drawMenuSlotFrames(GuiGraphics guiGraphics) {
        for (Slot slot : this.menu.slots) {
            drawStaticSlotFrame(guiGraphics, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
        }
    }

    private void drawStaticSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1D1F24);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8E97);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF545964);
    }

    private void drawStatLine(GuiGraphics guiGraphics, String label, FigurePreviewHelper.StatPreview preview, int x, int y, int color) {
        String valueText = label + " " + preview.finalValue();
        guiGraphics.drawString(this.font, valueText, x, y, color, false);

        if (preview.delta() != 0) {
            String deltaText = (preview.delta() > 0 ? "+" : "") + preview.delta();
            int deltaColor = preview.delta() > 0 ? 0xFF8FE388 : 0xFFE16A6A;
            guiGraphics.drawString(this.font, deltaText, x + this.font.width(valueText) + 4, y, deltaColor, false);
        }
    }

    private void drawClassBadge(GuiGraphics guiGraphics, String figureClass, int x, int y) {
        if (figureClass == null || figureClass.isBlank()) {
            return;
        }

        int width = getClassBadgeWidth(figureClass);
        guiGraphics.fill(x, y, x + width, y + 12, 0xFF39465A);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 11, 0xFF51627A);
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(figureClass, 84), x + 4, y + 2, 0xFFECE6D5, false);
    }

    private int getClassBadgeX() {
        ItemStack figureStack = this.menu.getFigureStack();
        return 40 + this.font.width(ItemFigure.getFigureName(figureStack)) + 6;
    }

    private int getClassBadgeWidth(String figureClass) {
        if (figureClass == null || figureClass.isBlank()) {
            return 0;
        }
        return Math.min(92, this.font.width(this.font.plainSubstrByWidth(figureClass, 84)) + 8);
    }

    private boolean isMouseWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
