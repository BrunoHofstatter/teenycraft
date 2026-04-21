package bruhof.teenycraft.item.custom;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.chip.ChipRegistry;
import bruhof.teenycraft.screen.FigureScreenMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemFigure extends Item {

    // NBT Keys (The "Labels" for our data)
    private static final String TAG_FIGURE_ID = "FigureID"; // e.g., "robin", "cyborg"
    private static final String TAG_NAME = "Name";
    private static final String TAG_DESC = "Description";
    private static final String TAG_CLASS = "FigureClass";
    private static final String TAG_GROUPS = "FigureGroups"; // List of strings
    private static final String TAG_PRICE = "Price";

    private static final String TAG_NICKNAME = "Nickname";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_XP = "XP";
    private static final String TAG_PENDING_UPGRADE_POINTS = "PendingUpgradePoints";
    private static final String TAG_LAST_UPGRADE = "LastUpgrade";

    // Stats Group
    private static final String TAG_STATS = "Stats";
    private static final String TAG_STAT_HP = "Health";
    private static final String TAG_STAT_POWER = "Power";
    private static final String TAG_STAT_DODGE = "Dodge";
    private static final String TAG_STAT_LUCK = "Luck";

    // Equipment & Abilities
    private static final String TAG_CHIP = "EquippedChip";
    private static final String TAG_ABILITIES = "Abilities"; // List of ability IDs
    private static final String TAG_ABILITY_ORDER = "AbilityOrder"; // Ordered list of IDs
    private static final String TAG_ABILITY_TIERS = "AbilityTiers"; // List of cost letters (a, b, c, d, e)
    private static final String TAG_GOLDEN_PROGRESS = "GoldenProgress"; // Compound of ID -> Percentage

    public ItemFigure(Properties pProperties) {
        super(pProperties.stacksTo(1)); // Figures are unstackable
    }

    public enum UpgradeChoice {
        HEALTH(TAG_STAT_HP, "HP", 'H', TeenyBalance.UPGRADE_GAIN_HP),
        POWER(TAG_STAT_POWER, "Power", 'P', TeenyBalance.UPGRADE_GAIN_POWER),
        DODGE(TAG_STAT_DODGE, "Dodge", 'D', TeenyBalance.UPGRADE_GAIN_DODGE),
        LUCK(TAG_STAT_LUCK, "Luck", 'L', TeenyBalance.UPGRADE_GAIN_LUCK);

        private final String statKey;
        private final String label;
        private final char code;
        private final int gain;

        UpgradeChoice(String statKey, String label, char code, int gain) {
            this.statKey = statKey;
            this.label = label;
            this.code = code;
            this.gain = gain;
        }

        public String getStatKey() {
            return statKey;
        }

        public String getLabel() {
            return label;
        }

        public char getCode() {
            return code;
        }

        public int getGain() {
            return gain;
        }

        public static @Nullable UpgradeChoice fromStatKey(String statKey) {
            for (UpgradeChoice value : values()) {
                if (value.statKey.equals(statKey)) {
                    return value;
                }
            }
            return null;
        }

        public static @Nullable UpgradeChoice fromButtonId(int buttonId) {
            if (buttonId < 0 || buttonId >= values().length) {
                return null;
            }
            return values()[buttonId];
        }
    }

    public record XpGainResult(int levelsGained, int newLevel, int remainingXp, int pendingUpgradePoints) { }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack figureStack = player.getItemInHand(hand);
        boolean isBattling = player.getCapability(BattleStateProvider.BATTLE_STATE)
                .map(state -> state.isBattling())
                .orElse(false);
        if (isBattling) {
            return InteractionResultHolder.pass(figureStack);
        }

        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int boundSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;

            NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) -> new FigureScreenMenu(containerId, inventory, hand, boundSlot),
                            Component.translatable("container.teenycraft.figure_screen")
                    ),
                    buffer -> {
                        buffer.writeEnum(hand);
                        buffer.writeInt(boundSlot);
                    });

            
            return InteractionResultHolder.sidedSuccess(figureStack, level.isClientSide());
        }

        return InteractionResultHolder.sidedSuccess(figureStack, level.isClientSide());
    }

    // ==========================================
    // 1. INITIALIZATION (Birth of a Figure)
    // ==========================================
    /**
     * Call this when you spawn a new figure (e.g., from a Mystery Box or Starter).
     * It writes the initial NBT data so the item isn't "empty".
     */
    public static void initializeFigure(ItemStack stack, String figureId, String name, String desc, String figureClass, List<String> groups, int price, int baseHp, int basePower, int baseDodge, int baseLuck, List<String> abilities, List<String> abilityTiers) {
        CompoundTag tag = stack.getOrCreateTag();

        // Identity
        tag.putString(TAG_FIGURE_ID, figureId);
        tag.putString(TAG_NAME, name);
        tag.putString(TAG_DESC, desc);
        tag.putString(TAG_CLASS, figureClass);
        tag.putInt(TAG_PRICE, price);
        tag.putString(TAG_NICKNAME, name); // Default nickname = Name

        // Groups
        net.minecraft.nbt.ListTag groupsList = new net.minecraft.nbt.ListTag();
        for (String group : groups) {
            groupsList.add(net.minecraft.nbt.StringTag.valueOf(group));
        }
        tag.put(TAG_GROUPS, groupsList);

        // Progression
        tag.putInt(TAG_LEVEL, 1);
        tag.putInt(TAG_XP, 0);
        tag.putInt(TAG_PENDING_UPGRADE_POINTS, 0);

        // Stats (Nested Compound for organization)
        CompoundTag stats = new CompoundTag();
        stats.putInt(TAG_STAT_HP, baseHp);
        stats.putInt(TAG_STAT_POWER, basePower);
        stats.putInt(TAG_STAT_DODGE, baseDodge);
        stats.putInt(TAG_STAT_LUCK, baseLuck);
        tag.put(TAG_STATS, stats);

        // Abilities
        net.minecraft.nbt.ListTag abilitiesList = new net.minecraft.nbt.ListTag();
        for (String ability : abilities) {
            abilitiesList.add(net.minecraft.nbt.StringTag.valueOf(ability));
        }
        tag.put(TAG_ABILITIES, abilitiesList);
        tag.put(TAG_ABILITY_ORDER, abilitiesList.copy()); // Initial order is the default list

        // Ability Tiers (Cost letters)
        net.minecraft.nbt.ListTag tiersList = new net.minecraft.nbt.ListTag();
        for (String tier : abilityTiers) {
            tiersList.add(net.minecraft.nbt.StringTag.valueOf(tier));
        }
        tag.put(TAG_ABILITY_TIERS, tiersList);

        // Golden Progress (Empty map initially)
        tag.put(TAG_GOLDEN_PROGRESS, new CompoundTag());
    }

    /**
     * Creates a fresh figure item using the Scaling System.
     */
    public static ItemStack create(ItemStack stack, String id, String name, String desc, String figureClass, List<String> groups, int price, float hpScale, float powerScale, float dodgeScale, float luckScale, List<String> abilities, List<String> abilityTiers) {
        int finalHp = (int) (hpScale * TeenyBalance.UPGRADE_GAIN_HP);
        int finalPower = (int) (powerScale * TeenyBalance.UPGRADE_GAIN_POWER);
        int finalDodge = (int) (dodgeScale * TeenyBalance.UPGRADE_GAIN_DODGE);
        int finalLuck = (int) (luckScale * TeenyBalance.UPGRADE_GAIN_LUCK);

        initializeFigure(stack, id, name, desc, figureClass, groups, price, finalHp, finalPower, finalDodge, finalLuck, abilities, abilityTiers);
        return stack;
    }

    // ==========================================
    // 2. GETTERS (Reading the Cold Storage)
    // ==========================================

    public static String getFigureID(ItemStack stack) {
        if (!stack.hasTag()) return "unknown";
        return stack.getTag().getString(TAG_FIGURE_ID);
    }

    public static String getFigureName(ItemStack stack) {
        if (!stack.hasTag()) return "Unknown Figure";
        return stack.getTag().getString(TAG_NAME);
    }

    public static String getFigureClass(ItemStack stack) {
        if (!stack.hasTag()) return "None";
        return stack.getTag().getString(TAG_CLASS);
    }

    public static int getPrice(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt(TAG_PRICE);
    }

    public static int getLevel(ItemStack stack) {
        // Default to Level 1 if something is wrong
        return stack.hasTag() ? Math.max(1, stack.getTag().getInt(TAG_LEVEL)) : 1;
    }

    public static int getXp(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getInt(TAG_XP)) : 0;
    }

    public static int getPendingUpgradePoints(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getInt(TAG_PENDING_UPGRADE_POINTS)) : 0;
    }

    public static int getXpRequiredForNextLevel(ItemStack stack) {
        return TeenyBalance.getFigureXpRequired(getLevel(stack));
    }

    public static @Nullable UpgradeChoice getLastUpgrade(ItemStack stack) {
        if (!stack.hasTag()) {
            return null;
        }
        return UpgradeChoice.fromStatKey(stack.getTag().getString(TAG_LAST_UPGRADE));
    }

    public static List<UpgradeChoice> getAvailableUpgradeChoices(ItemStack stack) {
        List<UpgradeChoice> choices = new ArrayList<>();
        if (getPendingUpgradePoints(stack) <= 0) {
            return choices;
        }

        UpgradeChoice blocked = getLastUpgrade(stack);
        for (UpgradeChoice choice : UpgradeChoice.values()) {
            if (choice != blocked) {
                choices.add(choice);
            }
        }
        return choices;
    }

    public static int getStat(ItemStack stack, String statKey) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_STATS)) return 0;
        return stack.getTag().getCompound(TAG_STATS).getInt(statKey);
    }

    // Convenience methods for specific stats
    public static int getHealth(ItemStack stack) { return getStat(stack, TAG_STAT_HP); }
    public static int getPower(ItemStack stack) { return getStat(stack, TAG_STAT_POWER); }
    public static int getDodge(ItemStack stack) { return getStat(stack, TAG_STAT_DODGE); }
    public static int getLuck(ItemStack stack) { return getStat(stack, TAG_STAT_LUCK); }

    // Golden Progress
    public static float getGoldenProgress(ItemStack stack, String abilityId) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_GOLDEN_PROGRESS)) return 0.0f;
        CompoundTag golden = stack.getTag().getCompound(TAG_GOLDEN_PROGRESS);
        return golden.getFloat(abilityId);
    }

    public static boolean isAbilityGolden(ItemStack stack, String abilityId) {
        return getGoldenProgress(stack, abilityId) >= 1.0f; // 1.0 = 100%
    }

    public static ArrayList<String> getAbilities(ItemStack stack) {
        ArrayList<String> abilities = new ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains(TAG_ABILITIES)) {
            return abilities;
        }

        net.minecraft.nbt.ListTag list = stack.getTag().getList(TAG_ABILITIES, 8);
        for (int i = 0; i < list.size(); i++) {
            abilities.add(list.getString(i));
        }
        return abilities;
    }

    // Ability Order
    public static ArrayList<String> getAbilityOrder(ItemStack stack) {
        ArrayList<String> order = new ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains(TAG_ABILITY_ORDER)) {
            return getAbilities(stack);
        }
        net.minecraft.nbt.ListTag list = stack.getTag().getList(TAG_ABILITY_ORDER, 8); // 8 is StringTag
        for (int i = 0; i < list.size(); i++) {
            order.add(list.getString(i));
        }
        return new ArrayList<>(sanitizeAbilityOrder(stack, order));
    }

    public static ArrayList<String> getAbilityTiers(ItemStack stack) {
        ArrayList<String> tiers = new ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains(TAG_ABILITY_TIERS)) return tiers;
        net.minecraft.nbt.ListTag list = stack.getTag().getList(TAG_ABILITY_TIERS, 8);
        for (int i = 0; i < list.size(); i++) {
            tiers.add(list.getString(i));
        }
        return tiers;
    }

    // ==========================================
    // 3. SETTERS (Writing to Cold Storage)
    // ==========================================

    public static void setLevel(ItemStack stack, int level) {
        if (!stack.hasTag()) return;
        level = Math.max(1, Math.min(level, TeenyBalance.MAX_LEVEL));
        stack.getTag().putInt(TAG_LEVEL, level);
        stack.getTag().putInt(TAG_XP, 0); // Reset XP for the new level
        stack.getTag().putInt(TAG_PENDING_UPGRADE_POINTS, 0);
    }

    public static void resetToFactory(ItemStack stack) {
        String id = getFigureID(stack);
        ItemStack freshStack = bruhof.teenycraft.util.FigureLoader.getFigureStack(id);
        if (!freshStack.isEmpty() && freshStack.hasTag()) {
            stack.setTag(freshStack.getTag().copy());
        }
    }

    public static void applyUpgrades(ItemStack stack, String upgradeCode) {
        if (!stack.hasTag()) return;
        String code = upgradeCode.toUpperCase();
        String lastStat = null;
        for (char c : code.toCharArray()) {
            switch (c) {
                case 'H' -> {
                    upgradeStat(stack, TAG_STAT_HP, TeenyBalance.UPGRADE_GAIN_HP);
                    lastStat = TAG_STAT_HP;
                }
                case 'P' -> {
                    upgradeStat(stack, TAG_STAT_POWER, TeenyBalance.UPGRADE_GAIN_POWER);
                    lastStat = TAG_STAT_POWER;
                }
                case 'D' -> {
                    upgradeStat(stack, TAG_STAT_DODGE, TeenyBalance.UPGRADE_GAIN_DODGE);
                    lastStat = TAG_STAT_DODGE;
                }
                case 'L' -> {
                    upgradeStat(stack, TAG_STAT_LUCK, TeenyBalance.UPGRADE_GAIN_LUCK);
                    lastStat = TAG_STAT_LUCK;
                }
            }
        }
        if (lastStat != null) {
            stack.getTag().putString(TAG_LAST_UPGRADE, lastStat);
        }
    }

    public static void setGolden(ItemStack stack, int abilitySlot, boolean active) {
        if (!stack.hasTag()) return;
        java.util.ArrayList<String> order = getAbilityOrder(stack);
        // Slot is 1-based from command, convert to 0-based
        int index = abilitySlot - 1;
        if (index >= 0 && index < order.size()) {
            setGoldenProgress(stack, order.get(index), active ? 1.0f : 0.0f);
        }
    }

    public static void setGoldenProgress(ItemStack stack, String abilityId, float value) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_GOLDEN_PROGRESS)) tag.put(TAG_GOLDEN_PROGRESS, new CompoundTag());

        CompoundTag golden = tag.getCompound(TAG_GOLDEN_PROGRESS);
        golden.putFloat(abilityId, Math.max(0.0f, Math.min(1.0f, value)));
    }

    public static void setAbilityOrderString(ItemStack stack, String orderCode) {
        setAbilityOrderFromPoolCode(stack, orderCode);
    }

    public static void updateGoldenProgress(ItemStack stack, String abilityId, float increment) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_GOLDEN_PROGRESS)) tag.put(TAG_GOLDEN_PROGRESS, new CompoundTag());
        
        CompoundTag golden = tag.getCompound(TAG_GOLDEN_PROGRESS);
        float current = golden.getFloat(abilityId);
        golden.putFloat(abilityId, Math.min(1.0f, current + increment));
    }

    public static boolean setAbilityOrderFromPoolCode(ItemStack stack, String orderCode) {
        if (!stack.hasTag()) return false;
        ArrayList<String> pool = getAbilities(stack);
        if (pool.isEmpty()) return false;

        List<String> newOrder = new ArrayList<>();
        for (char c : orderCode.toCharArray()) {
            if (!Character.isDigit(c)) {
                continue;
            }

            int index = Character.getNumericValue(c) - 1;
            if (index >= 0 && index < pool.size()) {
                String abilityId = pool.get(index);
                if (!newOrder.contains(abilityId)) {
                    newOrder.add(abilityId);
                }
            }
        }

        setAbilityOrder(stack, newOrder);
        return true;
    }

    public static String getAbilityOrderCodeFromPool(ItemStack stack, List<String> order) {
        ArrayList<String> pool = getAbilities(stack);
        StringBuilder builder = new StringBuilder();
        for (String abilityId : sanitizeAbilityOrder(stack, order)) {
            int index = pool.indexOf(abilityId);
            if (index >= 0) {
                builder.append(index + 1);
            }
        }
        return builder.toString();
    }

    public static void setAbilityOrder(ItemStack stack, List<String> newOrder) {
        if (!stack.hasTag()) return;
        List<String> sanitizedOrder = sanitizeAbilityOrder(stack, newOrder);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (String id : sanitizedOrder) {
            list.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        stack.getTag().put(TAG_ABILITY_ORDER, list);
    }

    public static XpGainResult addXp(ItemStack stack, int amount) {
        if (!stack.hasTag() || amount <= 0) {
            return new XpGainResult(0, getLevel(stack), getXp(stack), getPendingUpgradePoints(stack));
        }
        CompoundTag tag = stack.getTag();

        int currentXp = tag.getInt(TAG_XP);
        int currentLevel = tag.getInt(TAG_LEVEL);
        int pendingPoints = tag.getInt(TAG_PENDING_UPGRADE_POINTS);
        int levelsGained = 0;

        if (currentLevel >= TeenyBalance.MAX_LEVEL) {
            return new XpGainResult(0, currentLevel, currentXp, pendingPoints);
        }

        currentXp += amount;

        while (currentLevel < TeenyBalance.MAX_LEVEL) {
            int xpNeeded = TeenyBalance.getFigureXpRequired(currentLevel);
            if (xpNeeded <= 0 || currentXp < xpNeeded) {
                break;
            }

            currentXp -= xpNeeded;
            currentLevel++;
            pendingPoints++;
            levelsGained++;
        }

        tag.putInt(TAG_LEVEL, currentLevel);
        tag.putInt(TAG_XP, currentXp);
        tag.putInt(TAG_PENDING_UPGRADE_POINTS, pendingPoints);
        return new XpGainResult(levelsGained, currentLevel, currentXp, pendingPoints);
    }

    public static boolean spendPendingUpgrade(ItemStack stack, UpgradeChoice choice) {
        if (!stack.hasTag() || choice == null) {
            return false;
        }
        if (getPendingUpgradePoints(stack) <= 0 || !getAvailableUpgradeChoices(stack).contains(choice)) {
            return false;
        }

        upgradeStat(stack, choice.getStatKey(), choice.getGain());
        stack.getTag().putInt(TAG_PENDING_UPGRADE_POINTS, getPendingUpgradePoints(stack) - 1);
        stack.getTag().putString(TAG_LAST_UPGRADE, choice.getStatKey());
        return true;
    }

    // Use this when the player selects a stat upgrade in the GUI
    public static void upgradeStat(ItemStack stack, String statKey, int amount) {
        if (!stack.hasTag()) return;
        CompoundTag stats = stack.getTag().getCompound(TAG_STATS);
        int current = stats.getInt(statKey);
        stats.putInt(statKey, current + amount);
        stack.getTag().put(TAG_STATS, stats);
    }

    private static List<String> sanitizeAbilityOrder(ItemStack stack, List<String> requestedOrder) {
        ArrayList<String> pool = getAbilities(stack);
        List<String> sanitized = new ArrayList<>();

        for (String abilityId : requestedOrder) {
            if (pool.contains(abilityId) && !sanitized.contains(abilityId)) {
                sanitized.add(abilityId);
            }
        }

        for (String abilityId : pool) {
            if (!sanitized.contains(abilityId)) {
                sanitized.add(abilityId);
            }
        }

        return sanitized;
    }

    /**
     * Calculates the "Fresh" damage for an ability based on current stats and balance values.
     * Formula: mana_cost * base_damage_permana * power * multiplier
     * @param stack The figure ItemStack
     * @param slotIndex 0-2 (representing abilities 1, 2, and 3)
     */
    public static int calculateAbilityDamage(ItemStack stack, int slotIndex) {
        List<bruhof.teenycraft.util.FigurePreviewHelper.AbilityPreview> previews =
                bruhof.teenycraft.util.FigurePreviewHelper.buildAbilityPreviews(stack);
        if (slotIndex < 0 || slotIndex >= previews.size()) {
            return 0;
        }
        return previews.get(slotIndex).damage();
    }

    // ==========================================
    // 4. CHIP SYSTEM
    // ==========================================
    public static void installChip(ItemStack figureStack, ItemStack chipStack) {
        CompoundTag figureTag = figureStack.getOrCreateTag();
        ItemStack storedChip = chipStack.copy();
        storedChip.setCount(1);
        CompoundTag chipData = new CompoundTag();
        storedChip.save(chipData);
        figureTag.put(TAG_CHIP, chipData);
    }

    public static ItemStack getEquippedChip(ItemStack figureStack) {
        if (!figureStack.hasTag() || !figureStack.getTag().contains(TAG_CHIP)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(figureStack.getTag().getCompound(TAG_CHIP));
    }

    public static boolean hasEquippedChip(ItemStack figureStack) {
        return !getEquippedChip(figureStack).isEmpty();
    }

    public static void clearChip(ItemStack figureStack) {
        if (!figureStack.hasTag()) return;
        figureStack.getTag().remove(TAG_CHIP);
    }

    public static int getEquippedChipRank(ItemStack figureStack) {
        return ChipRegistry.getRank(getEquippedChip(figureStack));
    }

    // ==========================================
    // 5. VISUALS (Tooltip)
    // ==========================================
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (!pStack.hasTag()) {
            pTooltipComponents.add(Component.literal("§7Empty Figure Data"));
            return;
        }

        // Header: "Robin (Lvl 5) 250/500 xp"
        String name = getFigureName(pStack);
        String fClass = getFigureClass(pStack);
        int level = getLevel(pStack);
        int xp = getXp(pStack);
        int xpNeeded = getXpRequiredForNextLevel(pStack);
        int pendingUpgrades = getPendingUpgradePoints(pStack);

        String xpText = xpNeeded > 0 ? (xp + "/" + xpNeeded + " xp") : "MAX";
        pTooltipComponents.add(Component.literal("§6" + name.toUpperCase() + " §e[Lvl " + level + "] §8" + xpText));
        pTooltipComponents.add(Component.literal("§7Class: §f" + fClass));
        if (pendingUpgrades > 0) {
            pTooltipComponents.add(Component.literal("§aPending upgrades: §f" + pendingUpgrades));
        }

        if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            pTooltipComponents.add(Component.literal(""));
            pTooltipComponents.add(Component.literal("§d--- ABILITIES ---"));
            
            List<bruhof.teenycraft.util.FigurePreviewHelper.AbilityPreview> abilityPreviews =
                    bruhof.teenycraft.util.FigurePreviewHelper.buildAbilityPreviews(pStack);

            for (bruhof.teenycraft.util.FigurePreviewHelper.AbilityPreview preview : abilityPreviews) {
                String color = preview.golden() ? "§6" : "§9";
                String abilityText = color + "• " + preview.name();
                if (preview.damage() > 0) {
                    abilityText += " - " + preview.damage();
                } else if (preview.heal() > 0) {
                    abilityText += " - " + preview.heal();
                }

                pTooltipComponents.add(Component.literal(abilityText));
            }

            // Chip Display (Moved inside Ctrl)
            ItemStack chip = getEquippedChip(pStack);
            if (!chip.isEmpty()) {
                pTooltipComponents.add(Component.literal(""));
                pTooltipComponents.add(Component.literal("§3Chip: §f" + chip.getHoverName().getString()));
            }
        } else {
            // Stats Display
            pTooltipComponents.add(Component.literal("§cHP: " + getHealth(pStack)));
            pTooltipComponents.add(Component.literal("§dPow: " + getPower(pStack)));
            pTooltipComponents.add(Component.literal("§bDodge: " + getDodge(pStack)));
            pTooltipComponents.add(Component.literal("§eLuck: " + getLuck(pStack)));

            pTooltipComponents.add(Component.literal(""));
            pTooltipComponents.add(Component.literal("§8[Hold CTRL for Details]"));
        }

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
