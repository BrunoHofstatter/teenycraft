package bruhof.teenycraft.item.custom;

import bruhof.teenycraft.TeenyBalance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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

    // Ability Order
    public static java.util.ArrayList<String> getAbilityOrder(ItemStack stack) {
        java.util.ArrayList<String> order = new java.util.ArrayList<>();
        if (!stack.hasTag() || !stack.getTag().contains(TAG_ABILITY_ORDER)) return order;
        net.minecraft.nbt.ListTag list = stack.getTag().getList(TAG_ABILITY_ORDER, 8); // 8 is StringTag
        for (int i = 0; i < list.size(); i++) {
            order.add(list.getString(i));
        }
        return order;
    }

    public static java.util.ArrayList<String> getAbilityTiers(ItemStack stack) {
        java.util.ArrayList<String> tiers = new java.util.ArrayList<>();
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

    public static void updateGoldenProgress(ItemStack stack, String abilityId, float increment) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_GOLDEN_PROGRESS)) tag.put(TAG_GOLDEN_PROGRESS, new CompoundTag());
        
        CompoundTag golden = tag.getCompound(TAG_GOLDEN_PROGRESS);
        float current = golden.getFloat(abilityId);
        golden.putFloat(abilityId, Math.min(1.0f, current + increment));
    }

    public static void setAbilityOrder(ItemStack stack, java.util.List<String> newOrder) {
        if (!stack.hasTag()) return;
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (String id : newOrder) {
            list.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        stack.getTag().put(TAG_ABILITY_ORDER, list);
    }

    public static void addXp(ItemStack stack, int amount) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();

        int currentXp = tag.getInt(TAG_XP);
        int currentLevel = tag.getInt(TAG_LEVEL);

        // Check Master Value for Max Level
        if (currentLevel >= TeenyBalance.MAX_LEVEL) return;

        // Simple Level Up Logic (Placeholder - we will make this complex later)
        // For now: XP needed = Level * 100
        int xpNeeded = currentLevel * 100;

        currentXp += amount;

        // Check for Level Up
        if (currentXp >= xpNeeded) {
            currentXp -= xpNeeded;
            tag.putInt(TAG_LEVEL, currentLevel + 1);
            // NOTE: We do NOT auto-add stats here.
            // The "Level Up GUI" will handle adding stats manually later.
        }

        tag.putInt(TAG_XP, currentXp);
    }

    // Use this when the player selects a stat upgrade in the GUI
    public static void upgradeStat(ItemStack stack, String statKey, int amount) {
        if (!stack.hasTag()) return;
        CompoundTag stats = stack.getTag().getCompound(TAG_STATS);
        int current = stats.getInt(statKey);
        stats.putInt(statKey, current + amount);
        stack.getTag().put(TAG_STATS, stats);
    }

    /**
     * Calculates the "Fresh" damage for an ability based on current stats and balance values.
     * Formula: mana_cost * base_damage_permana * power * multiplier
     * @param stack The figure ItemStack
     * @param slotIndex 0-2 (representing abilities 1, 2, and 3)
     */
    public static int calculateAbilityDamage(ItemStack stack, int slotIndex) {
        int power = getPower(stack);

        java.util.ArrayList<String> order = getAbilityOrder(stack);
        if (slotIndex >= order.size()) return 0;

        String abilityId = order.get(slotIndex);
        bruhof.teenycraft.util.AbilityLoader.AbilityData data = bruhof.teenycraft.util.AbilityLoader.getAbility(abilityId);
        if (data == null || data.hitType.equalsIgnoreCase("none")) return 0;

        java.util.ArrayList<String> tiers = getAbilityTiers(stack);
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);

        float damageMultiplier = TeenyBalance.getDamageMultiplier(data.damageTier);

        float rawDamage = manaCost * TeenyBalance.BASE_DAMAGE_PERMANA * power * damageMultiplier;
        return Math.round(rawDamage);
    }

    // ==========================================
    // 4. CHIP SYSTEM
    // ==========================================
    public static void installChip(ItemStack figureStack, ItemStack chipStack) {
        CompoundTag figureTag = figureStack.getOrCreateTag();
        // Serialize the chip item into the figure's NBT
        CompoundTag chipData = new CompoundTag();
        chipStack.save(chipData);
        figureTag.put(TAG_CHIP, chipData);
    }

    public static ItemStack getEquippedChip(ItemStack figureStack) {
        if (!figureStack.hasTag() || !figureStack.getTag().contains(TAG_CHIP)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(figureStack.getTag().getCompound(TAG_CHIP));
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

        // Header: "Robin (Lvl 5)"
        String name = getFigureName(pStack);
        String fClass = getFigureClass(pStack);
        int level = getLevel(pStack);
        pTooltipComponents.add(Component.literal("§6" + name.toUpperCase() + " §e[Lvl " + level + "]"));
        pTooltipComponents.add(Component.literal("§7Class: §f" + fClass));

        if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            pTooltipComponents.add(Component.literal(""));
            pTooltipComponents.add(Component.literal("§d--- ABILITIES ---"));
            
            java.util.ArrayList<String> abilityOrder = getAbilityOrder(pStack);
            java.util.ArrayList<String> abilityTiers = getAbilityTiers(pStack);
            int power = getPower(pStack);

            for (int i = 0; i < abilityOrder.size(); i++) {
                String abilityId = abilityOrder.get(i);
                bruhof.teenycraft.util.AbilityLoader.AbilityData data = bruhof.teenycraft.util.AbilityLoader.getAbility(abilityId);
                
                if (data != null) {
                    boolean isGolden = isAbilityGolden(pStack, abilityId);
                    String color = isGolden ? "§6" : "§9";
                    String abilityText = color + "• " + data.name;

                    // Use the centralized calculation hook
                    int finalDamage = calculateAbilityDamage(pStack, i);
                    if (finalDamage > 0) {
                        abilityText += " - " + finalDamage;
                    }
                    
                    pTooltipComponents.add(Component.literal(abilityText));
                }
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