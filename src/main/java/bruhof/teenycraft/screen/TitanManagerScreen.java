package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.capability.TitanManagerStorageSlot;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketTitanManagerAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TitanManagerScreen extends AbstractContainerScreen<TitanManagerMenu> {
    private static final ResourceLocation COMBO_BANNER = new ResourceLocation(
            TeenyCraft.MOD_ID, "textures/gui/group_combo_banner.png");
    private static final int PANEL_BG = 0xFF1C1F26;
    private static final int PANEL_LIGHT = 0xFF2D3544;
    private static final int PANEL_DARK = 0xFF141820;
    private static final int PANEL_ACCENT = 0xFF4DAA7F;
    private static final int PANEL_ACCENT_MUTED = 0xFF31584A;
    private static final int TEXT_MAIN = 0xFFE8ECF1;
    private static final int TEXT_MUTED = 0xFF9CA7B5;
    private static final int TEXT_GOLD = 0xFFF1C96B;
    private static final int COMBO_ACCENT = 0xFFB078E6;
    private static final int COMBO_ACCENT_DARK = 0xFF57376E;

    private static final int TEAM_SLOT_COUNT = 3;
    private static final int TEAM_SLOT_START = 0;
    private static final int ACCESSORY_SLOT_INDEX = 3;
    private static final int STORAGE_SLOT_START = 4;
    private static final int STORAGE_SLOT_END = 58;
    private static final int STORAGE_GRID_X = 34;
    private static final int STORAGE_GRID_Y = 61;
    private static final int STORAGE_GRID_WIDTH = 9 * 18;
    private static final int STORAGE_GRID_HEIGHT = 6 * 18;

    private EditBox searchBox;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<SortButton> sortButtons = new ArrayList<>();
    private Button favoritesButton;
    private Button classFilterButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private boolean suppressSearchCallback = false;
    private boolean comboOverlayOpen = false;
    private int comboOverlayPage = 0;

    private static final int COMBO_HELP_X = 135;
    private static final int COMBO_HELP_Y = 16;
    private static final int COMBO_HELP_SIZE = 9;
    private static final int COMBO_ROWS_PER_PAGE = 6;

    public TitanManagerScreen(TitanManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
        this.imageHeight = 256;
        this.inventoryLabelY = 174;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 10;
        titleLabelY = 6;

        int x = leftPos;
        int y = topPos;

        searchBox = new EditBox(font, x + 9, y + 43, 60, 14, Component.literal("Search"));
        searchBox.setMaxLength(32);
        searchBox.setBordered(false);
        searchBox.setTextColor(TEXT_MAIN);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        tabButtons.clear();
        tabButtons.add(addRenderableWidget(makeTabButton(x + 144, y + 10, 28, TitanManagerTab.FIGURES)));
        tabButtons.add(addRenderableWidget(makeTabButton(x + 174, y + 10, 28, TitanManagerTab.CHIPS)));
        tabButtons.add(addRenderableWidget(makeTabButton(x + 204, y + 10, 28, TitanManagerTab.ACCESSORIES)));

        sortButtons.clear();
        addSortButton(makeSortButton(x + 75, y + 40, 24, TitanManagerSortMode.NAME, "Name"));
        addSortButton(makeSortButton(x + 101, y + 40, 24, TitanManagerSortMode.LEVEL_DESC, "Lvl-"));
        addSortButton(makeSortButton(x + 127, y + 40, 24, TitanManagerSortMode.LEVEL_ASC, "Lvl+"));
        addSortButton(makeSortButton(x + 153, y + 40, 24, TitanManagerSortMode.NEWEST, "New"));
        addSortButton(makeSortButton(x + 101, y + 40, 24, TitanManagerSortMode.RANK_DESC, "Rk-"));
        addSortButton(makeSortButton(x + 127, y + 40, 24, TitanManagerSortMode.RANK_ASC, "Rk+"));

        favoritesButton = addRenderableWidget(Button.builder(Component.literal("Fav"), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.toggleFavoritesOnly(menu.containerId)))
                .pos(x + 179, y + 40)
                .size(24, 16)
                .build());
        favoritesButton.setTooltip(Tooltip.create(Component.literal("Middle mouse button favorites an item")));

        classFilterButton = addRenderableWidget(Button.builder(Component.literal("All"), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.cycleFigureClass(menu.containerId)))
                .pos(x + 205, y + 40)
                .size(28, 16)
                .build());

        prevPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.setPage(menu.containerId,
                                menu.getViewState().getPageIndex() - 1)))
                .pos(x + 9, y + 165)
                .size(20, 16)
                .build());

        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.setPage(menu.containerId,
                                menu.getViewState().getPageIndex() + 1)))
                .pos(x + 57, y + 165)
                .size(20, 16)
                .build());

        syncWidgetsFromState();
    }

    private Button makeTabButton(int x, int y, int width, TitanManagerTab tab) {
        return Button.builder(Component.literal(tab.getLabel().substring(0, Math.min(3, tab.getLabel().length()))), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.setTab(menu.containerId, tab)))
                .pos(x, y)
                .size(width, 16)
                .build();
    }

    private SortButton makeSortButton(int x, int y, int width, TitanManagerSortMode sortMode, String label) {
        return new SortButton(sortMode, Button.builder(Component.literal(label), button ->
                        ModMessages.sendToServer(PacketTitanManagerAction.setSort(menu.containerId, sortMode)))
                .pos(x, y)
                .size(width, 16)
                .build());
    }

    private void addSortButton(SortButton sortButton) {
        sortButtons.add(sortButton);
        addRenderableWidget(sortButton.button);
    }

    private void onSearchChanged(String query) {
        if (suppressSearchCallback) {
            return;
        }
        ModMessages.sendToServer(PacketTitanManagerAction.setSearch(menu.containerId, query));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        searchBox.tick();
        syncWidgetsFromState();
    }

    private void syncWidgetsFromState() {
        TitanManagerViewState viewState = menu.getViewState();

        suppressSearchCallback = true;
        if (!searchBox.isFocused() && !searchBox.getValue().equals(viewState.getSearchQuery())) {
            searchBox.setValue(viewState.getSearchQuery());
        }
        suppressSearchCallback = false;

        for (int i = 0; i < tabButtons.size(); i++) {
            TitanManagerTab tab = TitanManagerTab.values()[i];
            Button button = tabButtons.get(i);
            button.active = viewState.getActiveTab() != tab;
        }

        for (SortButton sortButton : sortButtons) {
            boolean supported = sortButton.sortMode.isSupportedBy(viewState.getActiveTab());
            sortButton.button.visible = supported;
            sortButton.button.active = supported && viewState.getSortMode() != sortButton.sortMode;
        }

        favoritesButton.setMessage(Component.literal(viewState.isFavoritesOnly() ? "*Fav" : "Fav"));
        classFilterButton.visible = viewState.getActiveTab() == TitanManagerTab.FIGURES;
        classFilterButton.setMessage(Component.literal(truncateLabel(viewState.getFigureClassFilterLabel(), 4)));
        prevPageButton.active = viewState.getPageIndex() > 0;
        nextPageButton.active = viewState.getPageIndex() + 1 < viewState.getPageCount();
    }

    private String truncateLabel(String label, int length) {
        if (label == null || label.isEmpty()) {
            return "All";
        }
        if (label.length() <= length) {
            return label;
        }
        return label.substring(0, length);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_BG);
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + 34, PANEL_DARK);
        guiGraphics.fill(x + 4, y + 36, x + imageWidth - 4, y + 58, PANEL_DARK);
        guiGraphics.fill(x + 4, y + 62, x + imageWidth - 4, y + 176, PANEL_LIGHT);
        guiGraphics.fill(x + 4, y + 180, x + imageWidth - 4, y + imageHeight - 4, PANEL_DARK);

        drawSectionFrame(guiGraphics, x + 48, y + 14, 56, 26);
        drawSectionFrame(guiGraphics, x + 110, y + 14, 24, 26);
        drawStorageGrid(guiGraphics, x + 34, y + 61);

        guiGraphics.drawString(font, "TEAM", x + 49, y + 6, TEXT_MUTED, false);
        guiGraphics.drawString(font, "ACC", x + 111, y + 6, TEXT_MUTED, false);
        guiGraphics.drawString(font, "Search", x + 9, y + 36, TEXT_MUTED, false);
        guiGraphics.drawString(font, "Storage", x + 9, y + 61, TEXT_MUTED, false);

        int coins = minecraft != null && minecraft.player != null
                ? minecraft.player.getCapability(TeenyCoinsProvider.TEENY_COINS).map(handler -> handler.getCoins()).orElse(0)
                : 0;
        guiGraphics.drawString(font, "Coins", x + 9, y + 8, TEXT_MUTED, false);
        guiGraphics.drawString(font, NumberFormat.getIntegerInstance(Locale.US).format(coins), x + 9, y + 18, TEXT_GOLD, false);

        TitanManagerViewState viewState = menu.getViewState();
        String pageText = (viewState.getPageIndex() + 1) + "/" + viewState.getPageCount();
        guiGraphics.drawString(font, pageText, x + 33, y + 170, TEXT_MAIN, false);

        drawComboSlotIndicator(guiGraphics, x, y);
        drawLeadMarker(guiGraphics, x, y);
    }

    private void drawComboSlotIndicator(GuiGraphics guiGraphics, int x, int y) {
        int firstX = x + 55;
        int secondRight = x + 92;
        guiGraphics.fill(firstX, y + 16, secondRight, y + 17, COMBO_ACCENT_DARK);
        guiGraphics.fill(firstX, y + 36, secondRight, y + 38, COMBO_ACCENT);
        guiGraphics.fill(firstX, y + 16, firstX + 1, y + 38, COMBO_ACCENT);
        guiGraphics.fill(secondRight - 1, y + 16, secondRight, y + 38, COMBO_ACCENT);

        int helpX = x + COMBO_HELP_X;
        int helpY = y + COMBO_HELP_Y;
        guiGraphics.fill(helpX + 2, helpY, helpX + 7, helpY + COMBO_HELP_SIZE, COMBO_ACCENT_DARK);
        guiGraphics.fill(helpX, helpY + 2, helpX + COMBO_HELP_SIZE, helpY + 7, COMBO_ACCENT_DARK);
        guiGraphics.drawString(font, "?", helpX + 2, helpY, TEXT_MAIN, false);
    }

    private void drawLeadMarker(GuiGraphics guiGraphics, int x, int y) {
        int slot = menu.getLeadTeamSlot();
        int markerX = x + 56 + slot * 18 + 10;
        int markerY = y + 14;
        guiGraphics.fill(markerX + 2, markerY, markerX + 7, markerY + 9, PANEL_ACCENT_MUTED);
        guiGraphics.fill(markerX, markerY + 2, markerX + 9, markerY + 7, PANEL_ACCENT_MUTED);
        guiGraphics.drawString(font, "1", markerX + 2, markerY, TEXT_MAIN, false);
    }

    private void drawSectionFrame(GuiGraphics guiGraphics, int x1, int y1, int width, int height) {
        guiGraphics.fill(x1, y1, x1 + width, y1 + height, PANEL_LIGHT);
        guiGraphics.fill(x1 + 1, y1 + 1, x1 + width - 1, y1 + height - 1, PANEL_DARK);
    }

    private void drawStorageGrid(GuiGraphics guiGraphics, int x, int y) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + (col * 18);
                int slotY = y + (row * 18);
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, PANEL_DARK);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF262D39);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, playerInventoryTitle, 39, inventoryLabelY, TEXT_MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (comboOverlayOpen) {
            return handleComboOverlayClick(mouseX, mouseY, button);
        }

        ItemStack carried = menu.getCarried();
        if (button == 0 && isStorable(carried)
                && isWithin(mouseX, mouseY, leftPos + STORAGE_GRID_X, topPos + STORAGE_GRID_Y,
                STORAGE_GRID_WIDTH, STORAGE_GRID_HEIGHT)) {
            ModMessages.sendToServer(PacketTitanManagerAction.depositCarried(menu.containerId));
            return true;
        }

        if (button == 0 && isWithin(mouseX, mouseY, leftPos + COMBO_HELP_X, topPos + COMBO_HELP_Y,
                COMBO_HELP_SIZE, COMBO_HELP_SIZE)) {
            comboOverlayOpen = true;
            comboOverlayPage = 0;
            return true;
        }

        int leadMarkerX = leftPos + 56 + menu.getLeadTeamSlot() * 18 + 10;
        if (button == 0 && isWithin(mouseX, mouseY, leadMarkerX, topPos + 14, 9, 9)) {
            ModMessages.sendToServer(PacketTitanManagerAction.cycleLeadSlot(menu.containerId));
            return true;
        }
        if (button == 1 && hoveredSlot != null
                && (hoveredSlot.getItem().getItem() instanceof ItemFigure
                || hoveredSlot.getItem().getItem() instanceof ItemAccessory)) {
            int slotListIndex = menu.slots.indexOf(hoveredSlot);
            if (slotListIndex >= 0 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        TitanManagerMenu.BUTTON_OPEN_DETAILS_BASE + slotListIndex);
                return true;
            }
        }
        if (button == 2 && hoveredSlot != null) {
            int slotListIndex = menu.slots.indexOf(hoveredSlot);
            if (slotListIndex >= STORAGE_SLOT_START && slotListIndex < STORAGE_SLOT_END) {
                ModMessages.sendToServer(PacketTitanManagerAction.toggleFavorite(menu.containerId,
                        slotListIndex - STORAGE_SLOT_START));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (comboOverlayOpen) {
            int pageCount = getComboOverlayPageCount();
            comboOverlayPage = Math.max(0, Math.min(pageCount - 1, comboOverlayPage + (delta < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
            if (searchBox.keyPressed(keyCode, scanCode, modifiers) || searchBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderFavoriteMarkers(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (comboOverlayOpen) {
            renderComboOverlay(guiGraphics, mouseX, mouseY);
        } else {
            renderComboTooltips(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderComboTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + COMBO_HELP_X, topPos + COMBO_HELP_Y,
                COMBO_HELP_SIZE, COMBO_HELP_SIZE)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Group combo"));
            GroupComboOption active = getEffectiveCombo();
            if (active == null) {
                lines.add(Component.literal("Slots 1 and 2 do not share a group."));
            } else {
                lines.add(Component.literal(active.name() + (menu.isComboAutomatic() ? " (automatic)" : "")));
                for (GroupComboEffectOption effect : active.effects()) {
                    lines.add(Component.literal(effect.description()));
                }
            }
            lines.add(Component.literal("Click to choose a shared group."));
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }

        int leadMarkerX = leftPos + 56 + menu.getLeadTeamSlot() * 18 + 10;
        if (isWithin(mouseX, mouseY, leadMarkerX, topPos + 14, 9, 9)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.literal("First appearance"),
                    Component.literal("Click to move the opening marker.")), mouseX, mouseY);
        }
    }

    private void renderComboOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelX = leftPos + 18;
        int panelY = topPos + 36;
        int panelWidth = 204;
        int panelHeight = 176;
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xB0000000);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_DARK);
        guiGraphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, PANEL_LIGHT);
        guiGraphics.fill(panelX + 5, panelY + 5, panelX + panelWidth - 5, panelY + 27, COMBO_ACCENT_DARK);
        guiGraphics.blit(COMBO_BANNER, panelX + 75, panelY + 6, 70, 20,
                0.0f, 0.0f, 1536, 683, 1536, 683);
        guiGraphics.drawString(font, "GROUP COMBO", panelX + 10, panelY + 12, TEXT_MAIN, false);
        guiGraphics.drawString(font, "x", panelX + panelWidth - 15, panelY + 11, TEXT_MAIN, false);

        drawComboRow(guiGraphics, panelX + 8, panelY + 33, panelWidth - 16,
                "Automatic", "Priority, then alphabetical", menu.isComboAutomatic(), mouseX, mouseY);

        List<GroupComboOption> options = menu.getComboOptions();
        int start = comboOverlayPage * COMBO_ROWS_PER_PAGE;
        for (int row = 0; row < COMBO_ROWS_PER_PAGE; row++) {
            int index = start + row;
            if (index >= options.size()) {
                break;
            }
            GroupComboOption option = options.get(index);
            String effects = option.effects().stream().map(GroupComboEffectOption::label)
                    .reduce((left, right) -> left + ", " + right).orElse("No bonus yet");
            drawComboRow(guiGraphics, panelX + 8, panelY + 53 + row * 18, panelWidth - 16,
                    option.name(), effects, !menu.isComboAutomatic() && option.id().equals(menu.getEffectiveComboGroupId()),
                    mouseX, mouseY);
        }

        if (options.isEmpty()) {
            guiGraphics.drawCenteredString(font, "Put two figures with a shared group", panelX + panelWidth / 2,
                    panelY + 78, TEXT_MUTED);
            guiGraphics.drawCenteredString(font, "in the first two team slots.", panelX + panelWidth / 2,
                    panelY + 90, TEXT_MUTED);
        }

        int pageCount = getComboOverlayPageCount();
        guiGraphics.drawString(font, "<", panelX + 12, panelY + panelHeight - 15,
                comboOverlayPage > 0 ? TEXT_MAIN : TEXT_MUTED, false);
        guiGraphics.drawCenteredString(font, (comboOverlayPage + 1) + "/" + pageCount,
                panelX + panelWidth / 2, panelY + panelHeight - 15, TEXT_MUTED);
        guiGraphics.drawString(font, ">", panelX + panelWidth - 18, panelY + panelHeight - 15,
                comboOverlayPage + 1 < pageCount ? TEXT_MAIN : TEXT_MUTED, false);
    }

    private void drawComboRow(GuiGraphics guiGraphics, int x, int y, int width, String title, String detail,
                              boolean selected, int mouseX, int mouseY) {
        boolean hovered = isWithin(mouseX, mouseY, x, y, width, 16);
        guiGraphics.fill(x, y, x + width, y + 16,
                selected ? COMBO_ACCENT_DARK : hovered ? 0xFF3A4353 : PANEL_DARK);
        if (selected) {
            guiGraphics.fill(x, y, x + 3, y + 16, COMBO_ACCENT);
        }
        guiGraphics.drawString(font, truncateToWidth(title, 84), x + 6, y + 4, TEXT_MAIN, false);
        guiGraphics.drawString(font, truncateToWidth(detail, 88), x + 98, y + 4, TEXT_MUTED, false);
    }

    private boolean handleComboOverlayClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        int panelX = leftPos + 18;
        int panelY = topPos + 36;
        int panelWidth = 204;
        int panelHeight = 176;
        if (isWithin(mouseX, mouseY, panelX + panelWidth - 22, panelY + 5, 18, 22)
                || !isWithin(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            comboOverlayOpen = false;
            return true;
        }
        if (isWithin(mouseX, mouseY, panelX + 8, panelY + 33, panelWidth - 16, 16)) {
            ModMessages.sendToServer(PacketTitanManagerAction.setComboAuto(menu.containerId));
            return true;
        }
        int start = comboOverlayPage * COMBO_ROWS_PER_PAGE;
        for (int row = 0; row < COMBO_ROWS_PER_PAGE; row++) {
            if (isWithin(mouseX, mouseY, panelX + 8, panelY + 53 + row * 18, panelWidth - 16, 16)) {
                int index = start + row;
                if (index < menu.getComboOptions().size()) {
                    ModMessages.sendToServer(PacketTitanManagerAction.setComboGroup(menu.containerId,
                            menu.getComboOptions().get(index).id()));
                }
                return true;
            }
        }
        int pageCount = getComboOverlayPageCount();
        if (isWithin(mouseX, mouseY, panelX + 6, panelY + panelHeight - 22, 24, 20)) {
            comboOverlayPage = Math.max(0, comboOverlayPage - 1);
        } else if (isWithin(mouseX, mouseY, panelX + panelWidth - 30, panelY + panelHeight - 22, 24, 20)) {
            comboOverlayPage = Math.min(pageCount - 1, comboOverlayPage + 1);
        }
        return true;
    }

    private int getComboOverlayPageCount() {
        return Math.max(1, (menu.getComboOptions().size() + COMBO_ROWS_PER_PAGE - 1) / COMBO_ROWS_PER_PAGE);
    }

    private GroupComboOption getEffectiveCombo() {
        return menu.getComboOptions().stream()
                .filter(option -> option.id().equals(menu.getEffectiveComboGroupId()))
                .findFirst().orElse(null);
    }

    private String truncateToWidth(String text, int width) {
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean isStorable(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ItemFigure
                || stack.getItem() instanceof ItemChip
                || stack.getItem() instanceof ItemAccessory);
    }

    private void renderFavoriteMarkers(GuiGraphics guiGraphics) {
        for (int slotListIndex = STORAGE_SLOT_START; slotListIndex < STORAGE_SLOT_END; slotListIndex++) {
            Slot slot = menu.slots.get(slotListIndex);
            TitanManagerStorageSlot ref = menu.getViewState().getVisibleSlot(slotListIndex - STORAGE_SLOT_START);
            if (ref == null) {
                continue;
            }

            ItemStack stack = menu.getTitanManager().getStorageHandler(ref.section()).getStackInSlot(ref.slot());
            if (!stack.isEmpty() && menu.getTitanManager().isFavorite(stack)) {
                guiGraphics.drawString(font, "*", leftPos + slot.x + 12, topPos + slot.y + 1, PANEL_ACCENT, false);
            }
        }
    }

    private record SortButton(TitanManagerSortMode sortMode, Button button) {
    }
}
