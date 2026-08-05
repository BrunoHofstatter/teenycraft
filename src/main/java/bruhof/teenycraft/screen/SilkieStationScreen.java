package bruhof.teenycraft.screen;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSilkieStationAction;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SilkieStationScreen extends AbstractContainerScreen<SilkieStationMenu> {
    private static final int TARGET_X = 178;
    private static final int TARGET_Y = 34;
    private static final int OFFERING_X = 10;
    private static final int OFFERING_Y = 82;
    private static final int CANDIDATE_X = 10;
    private static final int CANDIDATE_Y = 116;
    private static final int CANDIDATE_COLUMNS = 5;
    private static final int CANDIDATE_ROWS = 2;
    private static final int PROGRESS_X = 214;
    private static final int PROGRESS_Y = 40;
    private static final int PROGRESS_WIDTH = 94;
    private static final int PROGRESS_HEIGHT = 10;
    private static final int ABILITY_ICON_X = 314;
    private static final int ABILITY_ICON_Y = 35;

    private final List<SortButton> sortButtons = new ArrayList<>();
    private EditBox searchBox;
    private Button feedButton;
    private Button clearTargetButton;
    private Button previousBrowserButton;
    private Button nextBrowserButton;
    private Button previousFavoritesButton;
    private Button nextFavoritesButton;
    private SilkieStationTargetRef draggedTarget;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private boolean confirmArmed;
    private String lastStateKey = "";
    private boolean suppressSearchCallback;

    public SilkieStationScreen(SilkieStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 276;
        inventoryLabelX = 80;
        inventoryLabelY = 179;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;

        searchBox = new EditBox(font, leftPos + 118, topPos + 136, 112, 16, Component.literal("Search figures"));
        searchBox.setMaxLength(32);
        searchBox.setHint(Component.literal("Search figures"));
        searchBox.setResponder(query -> {
            if (!suppressSearchCallback) {
                resetConfirmation();
                ModMessages.sendToServer(PacketSilkieStationAction.setSearch(menu.containerId, query));
            }
        });
        addRenderableWidget(searchBox);

        clearTargetButton = addRenderableWidget(Button.builder(Component.literal("X"), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.clearTarget(menu.containerId));
                })
                .bounds(leftPos + 178, topPos + 54, 18, 16).build());

        sortButtons.clear();
        addSortButton(SilkieStationSortMode.RECOMMENDED, "Rec", 118);
        addSortButton(SilkieStationSortMode.CLASS, "Class", 156);
        addSortButton(SilkieStationSortMode.LEVEL, "Level", 194);

        previousBrowserButton = addRenderableWidget(Button.builder(Component.literal("<"), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.setBrowserPage(menu.containerId,
                            menu.getSnapshot().browserPage() - 1));
                }).bounds(leftPos + 10, topPos + 160, 20, 16).build());
        nextBrowserButton = addRenderableWidget(Button.builder(Component.literal(">"), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.setBrowserPage(menu.containerId,
                            menu.getSnapshot().browserPage() + 1));
                }).bounds(leftPos + 80, topPos + 160, 20, 16).build());

        previousFavoritesButton = addRenderableWidget(Button.builder(Component.literal("<"), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.setFavoritePage(menu.containerId,
                            menu.getSnapshot().favoritePage() - 1));
                }).bounds(leftPos + 150, topPos + 46, 18, 16).build());
        nextFavoritesButton = addRenderableWidget(Button.builder(Component.literal(">"), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.setFavoritePage(menu.containerId,
                            menu.getSnapshot().favoritePage() + 1));
                }).bounds(leftPos + 150, topPos + 62, 18, 16).build());

        feedButton = addRenderableWidget(Button.builder(Component.literal("Feed Silkie"), pressed -> {
                    if (!confirmArmed) {
                        confirmArmed = true;
                        updateButtonState();
                        return;
                    }
                    confirmArmed = false;
                    ModMessages.sendToServer(PacketSilkieStationAction.feed(menu.containerId));
                }).bounds(leftPos + 244, topPos + 151, 86, 20).build());
    }

    private void addSortButton(SilkieStationSortMode mode, String label, int x) {
        Button button = Button.builder(Component.literal(label), pressed -> {
                    resetConfirmation();
                    ModMessages.sendToServer(PacketSilkieStationAction.setSort(menu.containerId, mode));
                }).bounds(leftPos + x, topPos + 112, 36, 18).build();
        sortButtons.add(new SortButton(mode, addRenderableWidget(button)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        SilkieStationSnapshot snapshot = menu.getSnapshot();
        suppressSearchCallback = true;
        if (!searchBox.isFocused() && !searchBox.getValue().equals(snapshot.searchQuery())) {
            searchBox.setValue(snapshot.searchQuery());
        }
        suppressSearchCallback = false;

        String stateKey = String.valueOf(snapshot.targetRef()) + snapshot.selectedAbility()
                + snapshot.offerings().stream().map(view -> view.ref().toString()).toList()
                + snapshot.totalPoints();
        if (!stateKey.equals(lastStateKey)) {
            confirmArmed = false;
            lastStateKey = stateKey;
        }
        updateButtonState();
    }

    private void updateButtonState() {
        SilkieStationSnapshot snapshot = menu.getSnapshot();
        for (SortButton sort : sortButtons) {
            sort.button().active = snapshot.sortMode() != sort.mode();
        }
        clearTargetButton.active = snapshot.targetRef() != null;
        previousBrowserButton.active = snapshot.browserPage() > 0;
        nextBrowserButton.active = snapshot.browserPage() + 1 < snapshot.browserPageCount();
        previousFavoritesButton.active = snapshot.favoritePage() > 0;
        nextFavoritesButton.active = snapshot.favoritePage() + 1 < snapshot.favoritePageCount();
        feedButton.active = snapshot.canFeed();
        feedButton.setMessage(Component.literal(confirmArmed ? "CONFIRM FEED" : "Feed Silkie"));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF20242B);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 105, 0xFF38312A);
        graphics.fill(x + 1, y + 107, x + imageWidth - 1, y + 187, 0xFF303640);
        graphics.fill(x + 1, y + 189, x + imageWidth - 1, y + imageHeight - 1, 0xFF463C31);

        drawSlotFrame(graphics, x + TARGET_X, y + TARGET_Y, 0xFFD6B64C);
        if (hasCurrentAbility(menu.getSnapshot())) {
            drawSlotFrame(graphics, x + ABILITY_ICON_X, y + ABILITY_ICON_Y, 0xFFD6B64C);
        }
        for (int index = 0; index < TeenyBalance.GOLDEN_MAX_SACRIFICES; index++) {
            drawSlotFrame(graphics, x + OFFERING_X + index * 20, y + OFFERING_Y, 0xFFAD774B);
        }
        for (int index = 0; index < SilkieStationMenu.BROWSER_PAGE_SIZE; index++) {
            drawSlotFrame(graphics, x + CANDIDATE_X + (index % CANDIDATE_COLUMNS) * 20,
                    y + CANDIDATE_Y + (index / CANDIDATE_COLUMNS) * 20, 0xFF667080);
        }
        for (Slot slot : menu.slots) {
            drawSlotFrame(graphics, x + slot.x - 1, y + slot.y - 1, 0xFF796A58);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        SilkieStationSnapshot snapshot = menu.getSnapshot();
        graphics.drawString(font, title, 8, 6, 0xFFFFE5A0, false);
        graphics.drawString(font, "Active", 10, 15, 0xFFE7DED0, false);
        graphics.drawString(font, "Favorites", 10, 41, 0xFFE7DED0, false);
        graphics.drawString(font, "Drag target here", 144, 10, 0xFFFFD76B, false);
        graphics.drawString(font, "Sacrifices", 10, 73, 0xFFFFD2A6, false);
        graphics.drawString(font, "Available " + snapshot.browserTotal(), 10, 106, 0xFFD5DBE5, false);
        graphics.drawString(font, "Sort", 118, 103, 0xFFD5DBE5, false);
        graphics.drawString(font, (snapshot.browserPage() + 1) + "/" + snapshot.browserPageCount(),
                42, 164, 0xFFD5DBE5, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE7DED0, false);

        if (hasCurrentAbility(snapshot)) {
            SilkieStationAbilityView ability = currentAbility(snapshot);
            graphics.drawString(font, truncate(ability.name(), 16), PROGRESS_X, 28, 0xFFFFD76B, false);
            renderGoldenProgressBar(graphics, ability, snapshot.appliedPoints());
        }

        int summaryX = 238;
        int summaryY = 59;
        graphics.drawString(font, "Base: " + snapshot.basePoints(), summaryX, summaryY, 0xFFF1F1F1, false);
        graphics.drawString(font, "Exact " + snapshot.exactCount() + ": +" + snapshot.exactBonus(),
                summaryX, summaryY + 10, 0xFFFFC75B, false);
        graphics.drawString(font, "Class " + snapshot.classComboCount() + ": +" + snapshot.classBonus(),
                summaryX, summaryY + 20, 0xFF8FD8FF, false);
        int groupY = summaryY + 30;
        for (SilkieStationGroupView group : snapshot.groups()) {
            int color = group.awarded() ? 0xFF9BFF9B : 0xFFAAB1BB;
            String count = group.memberCount() > 0
                    ? group.representedSacrifices() + "/" + Math.max(0, group.memberCount() - 1)
                    : Integer.toString(group.representedSacrifices());
            graphics.drawString(font, truncate(group.groupName(), 10) + " " + count + " +" + group.bonus(),
                    summaryX, groupY, color, false);
            groupY += 9;
            if (groupY > 107) {
                break;
            }
        }
        graphics.drawString(font, "Total: +" + snapshot.totalPoints(), summaryX, 119, 0xFFFFD76B, false);
        graphics.drawString(font, "Applied: +" + snapshot.appliedPoints(), summaryX, 129, 0xFF9BFF9B, false);
        if (snapshot.overflowPoints() > 0) {
            graphics.drawString(font, "Discarded: " + snapshot.overflowPoints(), summaryX, 139, 0xFFFF7B72, false);
        }
        if (snapshot.hasInvestedSacrifice()) {
            graphics.drawString(font, "Invested donor!", 118, 167, 0xFFFF6B63, false);
        }
        if (!snapshot.statusMessage().isBlank()) {
            graphics.drawString(font, truncate(snapshot.statusMessage(), 18), 118, 156,
                    snapshot.statusMessage().contains("discarded") ? 0xFFFFA47A : 0xFFC7CDD7, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCustomFigures(graphics);
        renderCustomTooltip(graphics, mouseX, mouseY);
        if (!draggedStack.isEmpty()) {
            graphics.renderItem(draggedStack, mouseX - 8, mouseY - 8);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderCustomFigures(GuiGraphics graphics) {
        SilkieStationSnapshot snapshot = menu.getSnapshot();
        for (int index = 0; index < snapshot.activeTargets().size(); index++) {
            renderView(graphics, snapshot.activeTargets().get(index), leftPos + 10 + index * 20, topPos + 22, false);
        }
        for (int index = 0; index < snapshot.favoriteTargets().size(); index++) {
            renderView(graphics, snapshot.favoriteTargets().get(index), leftPos + 10 + index * 20, topPos + 48, false);
        }
        if (!snapshot.targetStack().isEmpty()) {
            graphics.renderItem(snapshot.targetStack(), leftPos + TARGET_X + 1, topPos + TARGET_Y + 1);
        }
        if (hasCurrentAbility(snapshot)) {
            graphics.renderItem(createAbilityIcon(snapshot), leftPos + ABILITY_ICON_X + 1,
                    topPos + ABILITY_ICON_Y + 1);
        }
        for (int index = 0; index < snapshot.offerings().size(); index++) {
            renderView(graphics, snapshot.offerings().get(index), leftPos + OFFERING_X + 1 + index * 20,
                    topPos + OFFERING_Y + 1, true);
        }
        for (int index = 0; index < snapshot.candidates().size(); index++) {
            renderView(graphics, snapshot.candidates().get(index), leftPos + CANDIDATE_X + 1
                            + (index % CANDIDATE_COLUMNS) * 20,
                    topPos + CANDIDATE_Y + 1 + (index / CANDIDATE_COLUMNS) * 20, true);
        }
    }

    private void renderView(GuiGraphics graphics, SilkieStationFigureView view, int x, int y, boolean showPoints) {
        graphics.renderItem(view.stack(), x, y);
        if (showPoints) {
            graphics.drawString(font, "+" + view.basePoints(), x + 8, y + 9, 0xFFFFFF72, true);
        }
        if (view.invested()) {
            graphics.drawString(font, "!", x, y, 0xFFFF5147, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            SilkieStationSnapshot snapshot = menu.getSnapshot();
            int index = indexAt(mouseX, mouseY, leftPos + 10, topPos + 22,
                    snapshot.activeTargets().size(), snapshot.activeTargets().size(), 1);
            if (index >= 0) {
                beginTargetDrag(snapshot.activeTargets().get(index));
                return true;
            }
            index = indexAt(mouseX, mouseY, leftPos + 10, topPos + 48,
                    snapshot.favoriteTargets().size(), snapshot.favoriteTargets().size(), 1);
            if (index >= 0) {
                beginTargetDrag(snapshot.favoriteTargets().get(index));
                return true;
            }
            index = indexAt(mouseX, mouseY, leftPos + OFFERING_X, topPos + OFFERING_Y,
                    snapshot.offerings().size(), TeenyBalance.GOLDEN_MAX_SACRIFICES, 1);
            if (index >= 0) {
                resetConfirmation();
                ModMessages.sendToServer(PacketSilkieStationAction.toggleSacrifice(menu.containerId,
                        snapshot.offerings().get(index).ref().slot()));
                return true;
            }
            index = indexAt(mouseX, mouseY, leftPos + CANDIDATE_X, topPos + CANDIDATE_Y,
                    snapshot.candidates().size(), CANDIDATE_COLUMNS, CANDIDATE_ROWS);
            if (index >= 0) {
                resetConfirmation();
                ModMessages.sendToServer(PacketSilkieStationAction.toggleSacrifice(menu.containerId,
                        snapshot.candidates().get(index).ref().slot()));
                return true;
            }
            if (hoveredSlot != null && hoveredSlot.getItem().getItem() instanceof ItemFigure) {
                draggedTarget = new SilkieStationTargetRef(SilkieStationSourceType.PLAYER_INVENTORY,
                        hoveredSlot.getSlotIndex());
                draggedStack = hoveredSlot.getItem().copy();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedTarget != null) {
            if (inside(mouseX, mouseY, leftPos + TARGET_X, topPos + TARGET_Y, 18, 18)) {
                resetConfirmation();
                ModMessages.sendToServer(PacketSilkieStationAction.setTarget(menu.containerId, draggedTarget));
            }
            draggedTarget = null;
            draggedStack = ItemStack.EMPTY;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(mouseX, mouseY, leftPos + CANDIDATE_X, topPos + CANDIDATE_Y, 100, 40)) {
            SilkieStationSnapshot snapshot = menu.getSnapshot();
            int next = snapshot.browserPage() + (delta < 0 ? 1 : -1);
            ModMessages.sendToServer(PacketSilkieStationAction.setBrowserPage(menu.containerId, next));
            resetConfirmation();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void beginTargetDrag(SilkieStationFigureView view) {
        draggedTarget = view.ref();
        draggedStack = view.stack().copy();
    }

    private void renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        SilkieStationSnapshot snapshot = menu.getSnapshot();
        if (hasCurrentAbility(snapshot) && inside(mouseX, mouseY,
                leftPos + ABILITY_ICON_X, topPos + ABILITY_ICON_Y, 18, 18)) {
            SilkieStationAbilityView ability = currentAbility(snapshot);
            graphics.renderComponentTooltip(font, List.of(
                    Component.literal(ability.name()),
                    Component.literal("Golden progress: " + ability.points() + "/" + ability.requirement()),
                    Component.literal("Ability " + (snapshot.selectedAbility() + 1) + " progresses next.")),
                    mouseX, mouseY);
            return;
        }
        SilkieStationFigureView view = viewAt(mouseX, mouseY, snapshot);
        if (view == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(view.stack().getHoverName());
        lines.add(Component.literal("Level " + ItemFigure.getLevel(view.stack())
                + " | " + ItemFigure.getFigureClass(view.stack())));
        if (view.basePoints() > 0) {
            lines.add(Component.literal("+" + view.basePoints() + " points: " + view.reason()));
        }
        if (view.invested()) {
            lines.add(Component.literal("WARNING: progression or a chip will be destroyed."));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private SilkieStationFigureView viewAt(int mouseX, int mouseY, SilkieStationSnapshot snapshot) {
        int index = indexAt(mouseX, mouseY, leftPos + 10, topPos + 22,
                snapshot.activeTargets().size(), snapshot.activeTargets().size(), 1);
        if (index >= 0) return snapshot.activeTargets().get(index);
        index = indexAt(mouseX, mouseY, leftPos + 10, topPos + 48,
                snapshot.favoriteTargets().size(), snapshot.favoriteTargets().size(), 1);
        if (index >= 0) return snapshot.favoriteTargets().get(index);
        index = indexAt(mouseX, mouseY, leftPos + OFFERING_X, topPos + OFFERING_Y,
                snapshot.offerings().size(), TeenyBalance.GOLDEN_MAX_SACRIFICES, 1);
        if (index >= 0) return snapshot.offerings().get(index);
        index = indexAt(mouseX, mouseY, leftPos + CANDIDATE_X, topPos + CANDIDATE_Y,
                snapshot.candidates().size(), CANDIDATE_COLUMNS, CANDIDATE_ROWS);
        if (index >= 0) return snapshot.candidates().get(index);
        return null;
    }

    private int indexAt(double mouseX, double mouseY, int startX, int startY,
                        int itemCount, int columns, int rows) {
        for (int index = 0; index < itemCount && index < columns * rows; index++) {
            int x = startX + (index % columns) * 20;
            int y = startY + (index / columns) * 20;
            if (inside(mouseX, mouseY, x, y, 18, 18)) {
                return index;
            }
        }
        return -1;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF17191E);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, color);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF343840);
    }

    private void renderGoldenProgressBar(GuiGraphics graphics, SilkieStationAbilityView ability, int appliedPoints) {
        int x = PROGRESS_X;
        int y = PROGRESS_Y;
        int innerWidth = PROGRESS_WIDTH - 2;
        graphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, 0xFF15171B);
        graphics.fill(x + 1, y + 1, x + PROGRESS_WIDTH - 1, y + PROGRESS_HEIGHT - 1, 0xFF4A4438);
        if (ability.requirement() > 0) {
            int currentFill = Math.min(innerWidth,
                    Math.round(innerWidth * ability.points() / (float) ability.requirement()));
            int previewPoints = Math.min(ability.requirement(), ability.points() + appliedPoints);
            int previewFill = Math.min(innerWidth,
                    Math.round(innerWidth * previewPoints / (float) ability.requirement()));
            if (previewFill > currentFill) {
                graphics.fill(x + 1 + currentFill, y + 1, x + 1 + previewFill,
                        y + PROGRESS_HEIGHT - 1, 0xFFFFE27A);
            }
            if (currentFill > 0) {
                graphics.fill(x + 1, y + 1, x + 1 + currentFill,
                        y + PROGRESS_HEIGHT - 1, 0xFFD7A928);
            }
        }
        String progress = ability.points() + (appliedPoints > 0 ? " + " + appliedPoints : "")
                + " / " + ability.requirement();
        graphics.drawCenteredString(font, progress, x + PROGRESS_WIDTH / 2, y + 1, 0xFFFFFFFF);
    }

    private boolean hasCurrentAbility(SilkieStationSnapshot snapshot) {
        return snapshot.selectedAbility() >= 0 && snapshot.selectedAbility() < snapshot.abilities().size();
    }

    private SilkieStationAbilityView currentAbility(SilkieStationSnapshot snapshot) {
        return snapshot.abilities().get(snapshot.selectedAbility());
    }

    private ItemStack createAbilityIcon(SilkieStationSnapshot snapshot) {
        SilkieStationAbilityView ability = currentAbility(snapshot);
        ItemStack stack = new ItemStack(switch (snapshot.selectedAbility()) {
            case 1 -> ModItems.ABILITY_2.get();
            case 2 -> ModItems.ABILITY_3.get();
            default -> ModItems.ABILITY_1.get();
        });
        String iconVariant = FigureLoader.getAbilityIconVariant(snapshot.targetStack(), ability.abilityId());
        ItemAbility.initializeAbility(stack, ability.abilityId(), ability.name(), 0, false, iconVariant);
        return stack;
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private void resetConfirmation() {
        confirmArmed = false;
    }

    private record SortButton(SilkieStationSortMode mode, Button button) {
    }
}
