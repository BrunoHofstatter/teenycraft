package bruhof.teenycraft.util;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.effect.EffectCalculator;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FigurePreviewHelper {

    public record StatPreview(int baseValue, int finalValue, int delta) { }

    public record FigureStatsPreview(StatPreview hp, StatPreview power, StatPreview dodge, StatPreview luck) { }

    public record AbilityPreview(String abilityId, String name, String description, String goldenDescription,
                                 String tierLetter, int manaCost, int damage, int heal, boolean golden) { }

    private FigurePreviewHelper() {
    }

    public static FigureStatsPreview buildStatPreview(ItemStack stack) {
        ChipExecutor.ResolvedBattleStats resolved = ChipExecutor.resolveBattleStats(stack);
        return new FigureStatsPreview(
                new StatPreview(ItemFigure.getHealth(stack), resolved.maxHp, resolved.maxHp - ItemFigure.getHealth(stack)),
                new StatPreview(ItemFigure.getPower(stack), resolved.power, resolved.power - ItemFigure.getPower(stack)),
                new StatPreview(ItemFigure.getDodge(stack), resolved.dodge, resolved.dodge - ItemFigure.getDodge(stack)),
                new StatPreview(ItemFigure.getLuck(stack), resolved.luck, resolved.luck - ItemFigure.getLuck(stack))
        );
    }

    public static List<AbilityPreview> buildAbilityPreviews(ItemStack stack) {
        return buildAbilityPreviews(stack, ItemFigure.getAbilityOrder(stack));
    }

    public static List<AbilityPreview> buildAbilityPreviews(ItemStack stack, List<String> requestedOrder) {
        List<String> order = sanitizeOrder(stack, requestedOrder);
        ArrayList<String> tiers = ItemFigure.getAbilityTiers(stack);
        BattleFigure previewFigure = new BattleFigure(stack.copy());
        List<AbilityPreview> previews = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < order.size(); slotIndex++) {
            String abilityId = order.get(slotIndex);
            AbilityLoader.AbilityData data = AbilityLoader.getAbility(abilityId);
            if (data == null) {
                continue;
            }

            boolean isGolden = ItemFigure.isAbilityGolden(stack, abilityId);
            String tierLetter = slotIndex < tiers.size() ? tiers.get(slotIndex) : "a";
            int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
            int effectiveManaCost = TeenyBalance.getEffectiveManaCost(slotIndex + 1, tierLetter);
            int damage = calculateDamagePreview(previewFigure, data, effectiveManaCost);
            int heal = calculateHealPreview(previewFigure, data, effectiveManaCost, isGolden);

            previews.add(new AbilityPreview(
                    abilityId,
                    data.name,
                    data.description,
                    data.goldenDescription,
                    tierLetter,
                    manaCost,
                    damage,
                    heal,
                    isGolden
            ));
        }

        return previews;
    }

    private static int calculateDamagePreview(BattleFigure previewFigure, AbilityLoader.AbilityData data, int manaCost) {
        if (data.damageTier <= 0) {
            return 0;
        }

        float rawDamage = previewFigure.getPowerStat() * manaCost * TeenyBalance.BASE_DAMAGE_PERMANA
                * TeenyBalance.getDamageMultiplier(data.damageTier);

        if (data.traits != null) {
            for (AbilityLoader.TraitData trait : data.traits) {
                if ("activate".equals(trait.id)) {
                    float required = trait.params.isEmpty() ? 2.0f : trait.params.get(0);
                    rawDamage *= (required * TeenyBalance.ACTIVATE_DAMAGE_MULT);
                } else if ("charge_up".equals(trait.id)) {
                    float seconds = trait.params.isEmpty() ? 1.0f : trait.params.get(0);
                    rawDamage *= (seconds * TeenyBalance.CHARGE_UP_MULT_PER_SEC);
                }
            }
        }

        return Math.max(0, Math.round(rawDamage));
    }

    private static int calculateHealPreview(BattleFigure previewFigure, AbilityLoader.AbilityData data, int manaCost, boolean isGolden) {
        int totalHeal = 0;

        for (AbilityLoader.EffectData effect : data.effectsOnSelf) {
            totalHeal += calculateHealForEffect(previewFigure, manaCost, effect.id, effect.params);
        }
        for (AbilityLoader.EffectData effect : data.effectsOnOpponent) {
            totalHeal += calculateHealForEffect(previewFigure, manaCost, effect.id, effect.params);
        }

        if (isGolden) {
            for (AbilityLoader.GoldenBonusData goldenBonus : data.parsedGoldenBonus) {
                totalHeal += calculateGoldenHealBonus(previewFigure, manaCost, goldenBonus);
            }
        }

        return totalHeal;
    }

    private static int calculateHealForEffect(BattleFigure previewFigure, int manaCost, String effectId, List<Float> params) {
        return switch (effectId) {
            case "heal", "group_heal" -> {
                float magnitude = params.isEmpty() ? 1.0f : params.get(0);
                yield EffectCalculator.calculateHealMagnitude(previewFigure, manaCost, magnitude);
            }
            case "health_radio" -> {
                float magnitude = params.size() > 1 ? params.get(1) : 1.0f;
                yield EffectCalculator.calculateHealMagnitude(previewFigure, manaCost, magnitude);
            }
            default -> 0;
        };
    }

    private static int calculateGoldenHealBonus(BattleFigure previewFigure, int manaCost, AbilityLoader.GoldenBonusData bonus) {
        if (bonus.scope() == AbilityLoader.GoldenBonusScope.TRAIT) {
            return 0;
        }
        return calculateHealForEffect(previewFigure, manaCost, bonus.targetId(), bonus.params());
    }

    private static List<String> sanitizeOrder(ItemStack stack, List<String> requestedOrder) {
        ArrayList<String> pool = ItemFigure.getAbilities(stack);
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
}
