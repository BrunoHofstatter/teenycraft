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
    private static final String TAG_NICKNAME = "Nickname";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_XP = "XP";

    // Stats Group
    private static final String TAG_STATS = "Stats";
    private static final String TAG_STAT_HP = "Health";
    private static final String TAG_STAT_POWER = "Power";
    private static final String TAG_STAT_DODGE = "Dodge";
    private static final String TAG_STAT_LUCK = "Luck";

    // Equipment
    private static final String TAG_CHIP = "EquippedChip";
    // We will handle Abilities later as a List, but keeping space for them:
    private static final String TAG_ABILITIES = "Abilities";

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
    public static void initializeFigure(ItemStack stack, String figureId, int baseHp, int basePower, int baseDodge, int baseLuck) {
        CompoundTag tag = stack.getOrCreateTag();

        // Identity
        tag.putString(TAG_FIGURE_ID, figureId);
        tag.putString(TAG_NICKNAME, figureId); // Default nickname = ID

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
    }

    // ==========================================
    // 2. GETTERS (Reading the Cold Storage)
    // ==========================================

    public static String getFigureID(ItemStack stack) {
        if (!stack.hasTag()) return "unknown";
        return stack.getTag().getString(TAG_FIGURE_ID);
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

    // ==========================================
    // 3. SETTERS (Writing to Cold Storage)
    // ==========================================

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
        String name = getFigureID(pStack);
        int level = getLevel(pStack);
        pTooltipComponents.add(Component.literal("§6" + name.toUpperCase() + " §e[Lvl " + level + "]"));

        // Stats Display
        pTooltipComponents.add(Component.literal("§cHP: " + getHealth(pStack)));
        pTooltipComponents.add(Component.literal("§4Pow: " + getPower(pStack)));
        pTooltipComponents.add(Component.literal("§3Dodge: " + getDodge(pStack)));
        pTooltipComponents.add(Component.literal("§aLuck: " + getLuck(pStack)));

        // Chip Display
        ItemStack chip = getEquippedChip(pStack);
        if (!chip.isEmpty()) {
            pTooltipComponents.add(Component.literal("§dChip: " + chip.getHoverName().getString()));
        }

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}