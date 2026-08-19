package bruhof.teenycraft.battle;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.util.AbilityLoader;

public final class AbilityCostResolver {
    public static final String REDUCED_MANA_COST_TRAIT = "reduced_mana_cost";

    private AbilityCostResolver() {
    }

    public static int resolveActualCost(int slotIndex, String tier, AbilityLoader.AbilityData data, boolean golden) {
        int baseCost = TeenyBalance.getManaCost(slotIndex + 1, tier);
        if (golden && data != null
                && data.hasGoldenBonus(AbilityLoader.GoldenBonusScope.TRAIT, REDUCED_MANA_COST_TRAIT)) {
            int refund = Math.round(baseCost * TeenyBalance.GOLDEN_TRANSFORM_MANA_REFUND_PERCENT);
            return Math.max(1, baseCost - refund);
        }
        return baseCost;
    }

    public static int resolveEffectiveCost(int slotIndex, int actualCost) {
        return TeenyBalance.getEffectiveManaCost(actualCost, slotIndex + 1);
    }
}
