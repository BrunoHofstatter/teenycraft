package bruhof.teenycraft.battle;

import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.FigureFormLoader;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Complete server-authoritative resolution of one battle ability slot. */
public record BattleAbilitySlot(
        int slotIndex,
        String sourceAbilityId,
        String effectiveAbilityId,
        AbilityLoader.AbilityData data,
        String costTier,
        int actualManaCost,
        int effectiveManaCost,
        boolean golden,
        String iconVariant
) {
    @Nullable
    public static BattleAbilitySlot resolve(BattleFigure figure, int slotIndex) {
        if (figure == null || slotIndex < 0 || slotIndex >= 3) {
            return null;
        }

        ItemStack sourceStack = figure.getOriginalStack();
        List<String> sourceOrder = ItemFigure.getAbilityOrder(sourceStack);
        if (slotIndex >= sourceOrder.size()) {
            return null;
        }

        String sourceAbilityId = sourceOrder.get(slotIndex);
        String effectiveAbilityId = sourceAbilityId;
        String tier = sourceTier(sourceStack, slotIndex);
        FigureFormLoader.FigureFormData form = figure.getActiveForm();
        if (form != null) {
            effectiveAbilityId = form.resolveAbility(sourceAbilityId);
            if (effectiveAbilityId == null) {
                return null;
            }
            tier = form.resolveCostTier(effectiveAbilityId);
            if (tier == null) {
                return null;
            }
        }

        AbilityLoader.AbilityData data = AbilityLoader.getAbility(effectiveAbilityId);
        if (data == null) {
            return null;
        }

        boolean golden = ItemFigure.isAbilityGolden(sourceStack, sourceAbilityId);
        int actualCost = AbilityCostResolver.resolveActualCost(slotIndex, tier, data, golden);
        int effectiveCost = AbilityCostResolver.resolveEffectiveCost(slotIndex, actualCost);
        String iconVariant = FigureLoader.getAbilityIconVariant(sourceStack, effectiveAbilityId);
        return new BattleAbilitySlot(slotIndex, sourceAbilityId, effectiveAbilityId, data, tier,
                actualCost, effectiveCost, golden, iconVariant);
    }

    private static String sourceTier(ItemStack stack, int slotIndex) {
        List<String> tiers = ItemFigure.getAbilityTiers(stack);
        return slotIndex < tiers.size() ? tiers.get(slotIndex) : "a";
    }
}
