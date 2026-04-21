package bruhof.teenycraft.chip;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.capability.ITeenyCoins;
import bruhof.teenycraft.item.custom.ItemChip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class ChipFusionRegistry {
    private record SpecialFusionRecipe(String leftChipId, String rightChipId, String outputChipId, int[] costByRank) {
        boolean matches(String left, String right) {
            return (leftChipId.equals(left) && rightChipId.equals(right)) || (leftChipId.equals(right) && rightChipId.equals(left));
        }

        int getCostForRank(int rank) {
            return getRankValue(costByRank, rank);
        }
    }

    private static final List<SpecialFusionRecipe> SPECIAL_RECIPES = List.of(
            new SpecialFusionRecipe("tough_guy", "smokescreen", "tough_smokescreen",
                    TeenyBalance.CHIP_SPECIAL_TOUGH_SMOKESCREEN_COST_BY_RANK)
    );

    private ChipFusionRegistry() {
    }

    public static ItemStack getPreviewResult(ItemStack left, ItemStack right) {
        if (canNormalFuse(left, right)) {
            ItemStack result = left.copy();
            result.setCount(1);
            ItemChip.setChipRank(result, ChipRegistry.getRank(left) + 1);
            return result;
        }

        SpecialFusionRecipe recipe = findSpecialRecipe(left, right);
        if (recipe != null) {
            int rank = ChipRegistry.getRank(left);
            Item outputItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(TeenyCraft.MOD_ID + ":chip_" + recipe.outputChipId));
            if (outputItem instanceof ItemChip) {
                return ItemChip.createStack(outputItem, rank);
            }
        }

        return ItemStack.EMPTY;
    }

    public static int getPreviewCost(ItemStack left, ItemStack right) {
        if (canNormalFuse(left, right)) {
            ChipSpec spec = ChipRegistry.get(left);
            return spec != null ? spec.getFusionCostForCurrentRank(ChipRegistry.getRank(left)) : -1;
        }

        SpecialFusionRecipe recipe = findSpecialRecipe(left, right);
        if (recipe != null) {
            return recipe.getCostForRank(ChipRegistry.getRank(left));
        }

        return -1;
    }

    public static boolean canAfford(ITeenyCoins coins, ItemStack left, ItemStack right) {
        int cost = getPreviewCost(left, right);
        return cost >= 0 && coins != null && coins.getCoins() >= cost;
    }

    public static boolean canFuse(ItemStack left, ItemStack right) {
        return canNormalFuse(left, right) || findSpecialRecipe(left, right) != null;
    }

    private static boolean canNormalFuse(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (!(left.getItem() instanceof ItemChip) || !(right.getItem() instanceof ItemChip)) {
            return false;
        }
        if (!ItemStack.isSameItemSameTags(left, right)) {
            return false;
        }

        ChipSpec spec = ChipRegistry.get(left);
        if (spec == null) {
            return false;
        }

        int rank = ChipRegistry.getRank(left);
        return rank < spec.getMaxRank();
    }

    private static SpecialFusionRecipe findSpecialRecipe(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return null;
        }
        if (!(left.getItem() instanceof ItemChip leftChip) || !(right.getItem() instanceof ItemChip rightChip)) {
            return null;
        }

        if (ItemStack.isSameItemSameTags(left, right)) {
            return null;
        }

        int leftRank = ChipRegistry.getRank(left);
        int rightRank = ChipRegistry.getRank(right);
        if (leftRank <= 0 || leftRank != rightRank) {
            return null;
        }

        for (SpecialFusionRecipe recipe : SPECIAL_RECIPES) {
            if (recipe.matches(leftChip.getChipId(), rightChip.getChipId())) {
                ChipSpec outputSpec = ChipRegistry.get(recipe.outputChipId());
                if (outputSpec != null && leftRank <= outputSpec.getMaxRank()) {
                    return recipe;
                }
            }
        }

        return null;
    }

    private static int getRankValue(int[] valuesByRank, int rank) {
        if (valuesByRank == null || valuesByRank.length == 0) {
            return 0;
        }
        int index = Math.max(0, Math.min(valuesByRank.length - 1, rank - 1));
        return valuesByRank[index];
    }
}
