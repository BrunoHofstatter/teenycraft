package bruhof.teenycraft.item.custom;

import bruhof.teenycraft.chip.ChipRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemChip extends Item {
    private static final String TAG_CHIP_RANK = "ChipRank";

    private final String chipId;

    public ItemChip(Properties properties, String chipId) {
        super(properties.stacksTo(1));
        this.chipId = chipId;
    }

    public String getChipId() {
        return chipId;
    }

    public static int getChipRank(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CHIP_RANK)) {
            return 1;
        }
        return Math.max(1, tag.getInt(TAG_CHIP_RANK));
    }

    public static void setChipRank(ItemStack stack, int rank) {
        if (stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putInt(TAG_CHIP_RANK, Math.max(1, rank));
    }

    public static ItemStack createStack(Item item, int rank) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof ItemChip) {
            setChipRank(stack, rank);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        int rank = ChipRegistry.getRank(stack);
        return Component.translatable(this.getDescriptionId()).append(Component.literal(" " + toRoman(rank)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int rank = ChipRegistry.getRank(stack);
        if (rank > 0) {
            tooltip.add(Component.literal("\u00A77Rank " + rank));
        }
        tooltip.add(Component.literal("\u00A78Open a figure screen to install"));
        tooltip.add(Component.literal("\u00A78Installing a new chip destroys the old one"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static String toRoman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "I";
        };
    }
}
