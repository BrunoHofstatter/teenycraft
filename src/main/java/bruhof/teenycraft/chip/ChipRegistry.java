package bruhof.teenycraft.chip;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.custom.ItemChip;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ChipRegistry {
    private static final Map<String, ChipSpec> REGISTRY = new HashMap<>();

    static {
        register(ChipSpec.builder("tough_guy", 3)
                .addFlatStat(ChipStatType.POWER, TeenyBalance.CHIP_TOUGH_GUY_POWER_BY_RANK)
                .addFlatStat(ChipStatType.MAX_HP, TeenyBalance.CHIP_TOUGH_GUY_HP_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_TOUGH_GUY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("smokescreen", 3)
                .addFlatStat(ChipStatType.DODGE, TeenyBalance.CHIP_SMOKESCREEN_DODGE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_SMOKESCREEN_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("power", 3)
                .addFlatStat(ChipStatType.POWER, TeenyBalance.CHIP_POWER_POWER_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_POWER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("health", 3)
                .addFlatStat(ChipStatType.MAX_HP, TeenyBalance.CHIP_HEALTH_HP_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_HEALTH_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("luck", 3)
                .addFlatStat(ChipStatType.LUCK, TeenyBalance.CHIP_LUCK_LUCK_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_LUCK_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("ninja_skills", 3)
                .addFlatStat(ChipStatType.DODGE, TeenyBalance.CHIP_NINJA_SKILLS_DODGE_BY_RANK)
                .addFlatStat(ChipStatType.MAX_HP, TeenyBalance.CHIP_NINJA_SKILLS_HP_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_NINJA_SKILLS_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("loaded_dice", 3)
                .addFlatStat(ChipStatType.LUCK, TeenyBalance.CHIP_LOADED_DICE_LUCK_BY_RANK)
                .addFlatStat(ChipStatType.POWER, TeenyBalance.CHIP_LOADED_DICE_POWER_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_LOADED_DICE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("tough_stuff", 3)
                .addFlatStat(ChipStatType.MAX_HP, TeenyBalance.CHIP_TOUGH_STUFF_HP_BY_RANK)
                .setExactStat(ChipStatType.DODGE, TeenyBalance.CHIP_TOUGH_STUFF_DODGE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_TOUGH_STUFF_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("lucky_hearts", 3)
                .onCritHit(ChipSpec.Action.healSelfMaxHpPct(TeenyBalance.CHIP_LUCKY_HEARTS_HEAL_PCT_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_LUCKY_HEARTS_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("mana_steal_duck", 3)
                .onCritHit(ChipSpec.Action.stealManaOpponent(TeenyBalance.CHIP_MANA_STEAL_DUCK_MANA_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_MANA_STEAL_DUCK_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("healthy_dodge", 3)
                .onDodge(ChipSpec.Action.healSelfFlat(TeenyBalance.CHIP_HEALTHY_DODGE_HEAL_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_HEALTHY_DODGE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("dodgy_healthy_dodge", 3)
                .inherit("smokescreen", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("healthy_dodge", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("second_chance", 3)
                .secondChanceSurviveHp(TeenyBalance.CHIP_SECOND_CHANCE_SURVIVE_HP_BY_RANK)
                .secondChanceDebuffDuration(TeenyBalance.CHIP_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK)
                .secondChanceSlowMagnitude(TeenyBalance.CHIP_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_SECOND_CHANCE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("speedy_dodge", 3)
                .onDodge(ChipSpec.Action.applyEffectSelf("speed_up", TeenyBalance.CHIP_SPEEDY_DODGE_DURATION_BY_RANK,
                        TeenyBalance.CHIP_SPEEDY_DODGE_SPEED_PCT_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_SPEEDY_DODGE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("dodgy_speedy_dodge", 3)
                .inherit("smokescreen", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("speedy_dodge", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("healthy_second_chance", 3)
                .secondChanceSurviveHp(TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_SURVIVE_HP_BY_RANK)
                .secondChanceDebuffDuration(TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK)
                .secondChanceSlowMagnitude(TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_HEALTHY_SECOND_CHANCE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("lucky_healthy_duck", 3)
                .inherit("luck", TeenyBalance.CHIP_SPECIAL_LUCKY_HEALTHY_DUCK_LUCK_SCALE)
                .inherit("lucky_hearts", TeenyBalance.CHIP_SPECIAL_LUCKY_HEALTHY_DUCK_HEALTHY_DUCK_SCALE)
                .fusionCosts(TeenyBalance.CHIP_LUCKY_HEALTHY_DUCK_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("lucky_steal_duck", 3)
                .inherit("luck", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("mana_steal_duck", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("insta_cast_chance", 3)
                .extraInstantCastChance(TeenyBalance.CHIP_INSTA_CAST_CHANCE_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_INSTA_CAST_CHANCE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("fast_cast", 3)
                .chargeSpeedBonus(TeenyBalance.CHIP_FAST_CAST_SPEED_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_FAST_CAST_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("fast_draw", 3)
                .projectileSpeedBonus(TeenyBalance.CHIP_FAST_DRAW_SPEED_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_FAST_DRAW_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("fast_drawcast", 3)
                .inherit("fast_draw", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("fast_cast", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("clean_entry", 3)
                .onFirstAppearance(ChipSpec.Action.applyEffectSelf("cleanse", TeenyBalance.CHIP_CLEAN_ENTRY_DURATION_BY_RANK, TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_CLEAN_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("beastly_entry", 3)
                .onFirstAppearance(ChipSpec.Action.applyEffectSelf("power_up", TeenyBalance.CHIP_INFINITE_DURATION_BY_RANK, TeenyBalance.CHIP_BEASTLY_ENTRY_POWER_UP_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_BEASTLY_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("curse_entry", 3)
                .onFirstAppearance(ChipSpec.Action.applyEffectOpponent("curse", TeenyBalance.CHIP_CURSE_ENTRY_DURATION_BY_RANK,
                        TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_CURSE_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("dance_entry", 3)
                .onFirstAppearance(ChipSpec.Action.applyEffectSelf("dance", TeenyBalance.CHIP_DANCE_DURATION_BY_RANK, TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_DANCE_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("mana_boost", 3)
                .onFirstAppearance(ChipSpec.Action.addManaSelf(TeenyBalance.CHIP_MANA_BOOST_MANA_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_MANA_BOOST_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("momentum", 3)
                .onKill(ChipSpec.Action.addManaSelf(TeenyBalance.CHIP_MOMENTUM_MANA_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_MOMENTUM_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("victory_dance", 3)
                .onKill(ChipSpec.Action.applyEffectSelf("dance", TeenyBalance.CHIP_VICTORY_DANCE_DURATION_BY_RANK, TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_VICTORY_DANCE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("powered_beastly_entry", 3)
                .inherit("power", TeenyBalance.CHIP_SPECIAL_POWERED_BEASTLY_ENTRY_POWER_SCALE)
                .inherit("beastly_entry", TeenyBalance.CHIP_SPECIAL_POWERED_BEASTLY_ENTRY_BEASTLY_ENTRY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_POWERED_BEASTLY_ENTRY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("energetic_battery", 3)
                .passiveBatteryChargeBonus(TeenyBalance.CHIP_ENERGETIC_BATTERY_PASSIVE_CHARGE_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_ENERGETIC_BATTERY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("death_energy", 3)
                .onFaint(ChipSpec.Action.addBatterySelf(TeenyBalance.CHIP_DEATH_ENERGY_BATTERY_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_DEATH_ENERGY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("self_explosion", 3)
                .onFaint(ChipSpec.Action.dealDamageOpponent(TeenyBalance.CHIP_SELF_EXPLOSION_DAMAGE_BY_RANK, true))
                .fusionCosts(TeenyBalance.CHIP_SELF_EXPLOSION_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("last_laugh", 3)
                .onFaint(ChipSpec.Action.applyRandomEffectOpponent(
                        ChipSpec.EffectSpec.of("curse", TeenyBalance.CHIP_LAST_LAUGH_CURSE_DURATION_BY_RANK, TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK),
                        ChipSpec.EffectSpec.of("shock", TeenyBalance.CHIP_LAST_LAUGH_SHOCK_DURATION_BY_RANK,
                                TeenyBalance.CHIP_LAST_LAUGH_SHOCK_INTERVAL_BY_RANK, TeenyBalance.CHIP_LAST_LAUGH_SHOCK_POWER_BY_RANK),
                        ChipSpec.EffectSpec.of("poison", TeenyBalance.CHIP_LAST_LAUGH_POISON_DURATION_BY_RANK,
                                TeenyBalance.CHIP_LAST_LAUGH_POISON_INTERVAL_BY_RANK, TeenyBalance.CHIP_LAST_LAUGH_POISON_POWER_BY_RANK),
                        ChipSpec.EffectSpec.of("freeze", TeenyBalance.CHIP_LAST_LAUGH_FREEZE_DURATION_BY_RANK,
                                TeenyBalance.CHIP_LAST_LAUGH_FREEZE_BURN_PCT_BY_RANK),
                        ChipSpec.EffectSpec.of("waffle", TeenyBalance.CHIP_LAST_LAUGH_WAFFLE_DURATION_BY_RANK,
                                TeenyBalance.CHIP_ONE_MAGNITUDE_BY_RANK)
                ))
                .fusionCosts(TeenyBalance.CHIP_LAST_LAUGH_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("necromancer", 3)
                .onKill(ChipSpec.Action.summonPetSelf(TeenyBalance.CHIP_NECROMANCER_DURATION_BY_RANK, TeenyBalance.CHIP_NECROMANCER_DAMAGE_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_NECROMANCER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("vampire", 3)
                .onKill(ChipSpec.Action.healSelfMaxHpPct(TeenyBalance.CHIP_VAMPIRE_HEAL_PCT_BY_RANK))
                .fusionCosts(TeenyBalance.CHIP_VAMPIRE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("regenerative_cuteness", 3)
                .activeRegenInterval(TeenyBalance.CHIP_REGENERATIVE_CUTENESS_INTERVAL_BY_RANK)
                .activeRegenHeal(TeenyBalance.CHIP_REGENERATIVE_CUTENESS_HEAL_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_REGENERATIVE_CUTENESS_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("tofu_lover", 3)
                .tofuChanceBonus(TeenyBalance.CHIP_TOFU_LOVER_TOFU_CHANCE_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_TOFU_LOVER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("lucky_tofu_lover", 3)
                .inherit("luck", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("tofu_lover", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("hello_nurse", 3)
                .abilityHealBonus(TeenyBalance.CHIP_HELLO_NURSE_HEAL_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_HELLO_NURSE_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("team_medic", 3)
                .teamMedicBenchHealPct(TeenyBalance.CHIP_TEAM_MEDIC_BENCH_HEAL_PCT_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_TEAM_MEDIC_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("pointy", 3)
                .onDamaged(ChipSpec.Action.dealDamageOpponent(TeenyBalance.CHIP_POINTY_DAMAGE_BY_RANK, false))
                .fusionCosts(TeenyBalance.CHIP_POINTY_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("pointy_health", 3)
                .inherit("health", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("pointy", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("stun_resister", 3)
                .directStunDurationReduction(TeenyBalance.CHIP_STUN_RESISTER_DURATION_REDUCTION_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_STUN_RESISTER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("finisher", 3)
                .finisherHpThreshold(TeenyBalance.CHIP_FINISHER_HP_THRESHOLD_BY_RANK)
                .finisherDamageBonus(TeenyBalance.CHIP_FINISHER_DAMAGE_BONUS_BY_RANK)
                .fusionCosts(TeenyBalance.CHIP_FINISHER_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("finishing_momentum", 3)
                .inherit("finisher", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("momentum", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());

        register(ChipSpec.builder("powered_finisher", 3)
                .inherit("power", TeenyBalance.CHIP_STANDARD_HYBRID_PRIMARY_SCALE)
                .inherit("finisher", TeenyBalance.CHIP_STANDARD_HYBRID_SECONDARY_SCALE)
                .fusionCosts(TeenyBalance.CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK)
                .build());
    }

    public static void init() {
        // Intentional no-op. Called during mod bootstrap to make registry initialization explicit.
    }

    private static void register(ChipSpec spec) {
        REGISTRY.put(spec.getId(), spec);
    }

    public static ChipSpec get(String id) {
        return REGISTRY.get(id);
    }

    public static ChipSpec get(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemChip itemChip)) {
            return null;
        }
        return get(itemChip.getChipId());
    }

    public static int getRank(ItemStack stack) {
        ChipSpec spec = get(stack);
        if (spec == null) {
            return 0;
        }
        return spec.clampRank(ItemChip.getChipRank(stack));
    }

    public static Collection<ChipSpec> getAll() {
        return REGISTRY.values();
    }
}
