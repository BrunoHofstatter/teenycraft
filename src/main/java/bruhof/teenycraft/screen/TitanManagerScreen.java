package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.TeenyCoinsProvider;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TitanManagerScreen extends AbstractContainerScreen<TitanManagerMenu> {
    private static final int PANEL_BG = 0xFF1C1F26;
    private static final int PANEL_LIGHT = 0xFF2D3544;
    private static final int PANEL_DARK = 0xFF141820;
    private static final int PANEL_ACCENT = 0xFF4DAA7F;
    private static final int PANEL_ACCENT_MUTED = 0xFF31584A;
    private static final int TEXT_MAIN = 0xFFE8ECF1;
    private static final int TEXT_MUTED = 0xFF9CA7B5;
    private static final int TEXT_GOLD = 0xFFF1C96B;

    private static final int TEAM_SLOT_COUNT = 3;
    private static final int TEAM_SLOT_START = 0;
    private static final int ACCESSORY_SLOT_INDEX = 3;
    private static final int STORAGE_SLOT_START = 4;
    private static final int STORAGE_SLOT_END = 58;

    private EditBox searchBox;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<SortButton> sortButtons = new ArrayList<>();
    private Button favoritesButton;
    private Button classFilterButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private boolean suppressSearchCallback = false;

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
