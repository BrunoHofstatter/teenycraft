package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.util.AbilityLoader;

public class EffectCalculator {

    /**
     * Generic Magnitude Calculation
     * Formula: Power * ManaCost * BaseDamagePerMana * Param * BaseConstant
     */
    public static int calculateGenericMagnitude(BattleFigure caster, int manaCost, float paramMultiplier, float baseConstant) {
        int power = caster.getPowerStat();
        float value = power * manaCost * TeenyBalance.BASE_DAMAGE_PERMANA * paramMultiplier * baseConstant;
        return Math.round(value);
    }

    public static int calculatePowerUpMagnitude(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateGenericMagnitude(caster, manaCost, paramMultiplier, TeenyBalance.BASE_POWER_UP_PERMANA);
    }
    
    public static int calculatePowerDownMagnitude(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateGenericMagnitude(caster, manaCost, paramMultiplier, TeenyBalance.BASE_POWER_DOWN_PERMANA);
    }
    
    public static int calculateHealMagnitude(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateGenericMagnitude(caster, manaCost, paramMultiplier, TeenyBalance.BASE_HEAL_PERMANA);
    }
    
    public static int calculateManaMagnitude(BattleFigure caster, AbilityLoader.AbilityData data, int manaCost, float paramMultiplier) {
        // Formula: Mana * BaseManaFill * Param
        // No stats (Power) or Tier Multipliers involved.
        float value = manaCost * TeenyBalance.BASE_MANA_FILL_PERMANA * paramMultiplier;
        
        return Math.round(value);
    }

    public static int calculateBarDepleteMagnitude(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        // Formula: Mana * Param * BaseDeplete * (1 + Luck * BasePerLuck)
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMultiplier = 1.0f + (luck * TeenyBalance.BAR_DEPLETE_PERLUCK);
        float value = manaCost * paramMultiplier * TeenyBalance.BAR_DEPLETE_PERMANA * luckMultiplier;
        return Math.round(value);
    }
    
    public static int calculateSelfShockDamage(BattleFigure caster, int manaCost, float paramMultiplier) {
        // Formula: Mana * MaxHP * Param * BaseSelfShock
        float value = manaCost * caster.getMaxHp() * paramMultiplier * TeenyBalance.BASE_SELF_SHOCK_PERMANA;
        return Math.round(value);
    }
    
    public static int calculateEffectDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier, float basePerMana, float basePerLuck) {
        // Formula: Mana * Param * BasePerMana * (1 + Luck * BasePerLuck)
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMultiplier = 1.0f + (luck * basePerLuck);
        float value = manaCost * paramMultiplier * basePerMana * luckMultiplier;
        return Math.round(value * 20); // Convert Seconds to Ticks
    }

    public static int calculateStunDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.STUN_DURATION_PERMANA, TeenyBalance.STUN_DURATION_PERLUCK);
    }
    
    public static int calculateDanceDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.DANCE_DURATION_PERMANA, TeenyBalance.DANCE_DURATION_PERLUCK);
    }
    
    public static int calculateCurseDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.CURSE_DURATION_PERMANA, TeenyBalance.CURSE_DURATION_PERLUCK);
    }
    
    public static int calculateWaffleDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.WAFFLE_DURATION_PERMANA, TeenyBalance.WAFFLE_DURATION_PERLUCK);
    }

    public static int calculateCleanseDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.CLEANSE_DURATION_PERMANA, TeenyBalance.CLEANSE_DURATION_PERLUCK);
    }
    
    public static int calculateKissDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.KISS_DURATION_PERMANA, TeenyBalance.KISS_DURATION_PERLUCK);
    }
    
    public static int calculateShieldDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.SHIELD_DURATION_PERMANA, TeenyBalance.SHIELD_DURATION_PERLUCK);
    }
    
    public static int calculateDodgeSmokeDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.DODGE_SMOKE_DURATION_PERMANA, TeenyBalance.DODGE_SMOKE_DURATION_PERLUCK);
    }

    public static int calculateDefenseUpDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.DEFENSE_UP_DURATION_PERMANA, TeenyBalance.DEFENSE_UP_DURATION_PERLUCK);
    }

    public static int calculateDefenseDownDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.DEFENSE_DOWN_DURATION_PERMANA, TeenyBalance.DEFENSE_DOWN_DURATION_PERLUCK);
    }

    public static int calculateRootDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.ROOT_DURATION_PERMANA, TeenyBalance.ROOT_DURATION_PERLUCK);
    }

    public static int calculateDisableDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.DISABLE_DURATION_PERMANA, TeenyBalance.DISABLE_DURATION_PERLUCK);
    }

    public static int calculateShockAmount(int manaCost, float paramMultiplier) {
        return (int) (TeenyBalance.SHOCK_BASE_AMOUNT + (manaCost * TeenyBalance.SHOCK_AMOUNT_PERMANA));
    }

    public static int calculateShockInterval(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float m = Math.max(1, manaCost);
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f - (luck * TeenyBalance.SHOCK_INTERVAL_PERLUCK);
        float base = ((1.0f / m) * TeenyBalance.SHOCK_INTERVAL_PERMANA) + TeenyBalance.SHOCK_BASE_INTERVAL;
        return (int) (base * paramMultiplier * luckMult);
    }

    public static int calculateShockLength(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float base = manaCost * TeenyBalance.SHOCK_DURATION_PERMANA;
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f + (luck * TeenyBalance.SHOCK_DURATION_PERLUCK);
        return (int) (base * paramMultiplier * luckMult);
    }

    public static int calculatePoisonAmount(int manaCost, float paramMultiplier) {
        return (int) (TeenyBalance.POISON_BASE_AMOUNT + (manaCost * TeenyBalance.POISON_AMOUNT_PERMANA));
    }

    public static int calculatePoisonInterval(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float m = Math.max(1, manaCost);
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f - (luck * TeenyBalance.POISON_INTERVAL_PERLUCK);
        float base = ((1.0f / m) * TeenyBalance.POISON_INTERVAL_PERMANA) + TeenyBalance.POISON_BASE_INTERVAL;
        return (int) (base * paramMultiplier * luckMult);
    }

    public static int calculateRadioAmount(int manaCost, float paramMultiplier) {
        return (int) (TeenyBalance.RADIO_BASE_AMOUNT + (manaCost * TeenyBalance.RADIO_AMOUNT_PERMANA) * paramMultiplier);
    }

    public static int calculateRadioInterval(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float m = Math.max(1, manaCost);
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f - (luck * TeenyBalance.RADIO_INTERVAL_PERLUCK);
        float base = ((1.0f / m) * TeenyBalance.RADIO_INTERVAL_PERMANA) + TeenyBalance.RADIO_BASE_INTERVAL;
        return (int) (base * paramMultiplier * luckMult);
    }

    public static float calculateRawBaseDamage(BattleFigure caster, IBattleState state, int manaCost, int damageTier) {
        float mult = TeenyBalance.getDamageMultiplier(damageTier);
        float base = caster.getPowerStat() * manaCost * TeenyBalance.BASE_DAMAGE_PERMANA * mult;
        
        if (state != null) {
            int bonus = state.getEffectMagnitude("power_up") + state.getEffectMagnitude("flat_damage_up");
            int malus = state.getEffectMagnitude("power_down") + state.getEffectMagnitude("flat_damage_down");
            base += (bonus - malus);
        }
        
        return Math.max(0, base);
    }

    public static int calculateLuckUpMagnitude(int manaCost, float paramMultiplier) {
        return Math.round((manaCost * TeenyBalance.LUCK_UP_PERCENT_PERMANA * paramMultiplier) + TeenyBalance.LUCK_UP_BASE_PERCENT);
    }

    public static int calculateLuckUpDuration(int manaCost, float paramMultiplier) {
        return Math.round((manaCost * TeenyBalance.LUCK_UP_DURATION_PERMANA * paramMultiplier) + TeenyBalance.LUCK_UP_BASE_DURATION);
    }

    public static int calculateCutenessDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.CUTENESS_DURATION_PERMANA, TeenyBalance.CUTENESS_DURATION_PERLUCK);
    }

    public static int calculateCutenessPercent(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f + (TeenyBalance.CUTENESS_PERCENT_PERLUCK * luck);
        float value = manaCost * TeenyBalance.CUTENESS_PERCENT_PERMANA * paramMultiplier * luckMult;
        return Math.round(value);
    }

    public static int calculateReflectDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.REFLECT_DURATION_PERMANA, TeenyBalance.REFLECT_DURATION_PERLUCK);
    }

    public static int calculateReflectDefense(int manaCost, float paramMultiplier) {
        float reduction = manaCost * TeenyBalance.REFLECT_DEFENSE_PERMANA * paramMultiplier;
        // The magnitude is the % of damage KEPT (e.g. 0.3 means take 30% damage)
        float kept = 1.0f - reduction;
        return Math.round(Math.max(0, kept) * 100);
    }

    public static int calculateReflectDamage(BattleFigure caster, int manaCost, float paramMultiplier) {
        float value = manaCost * caster.getPowerStat() * TeenyBalance.REFLECT_DAMAGE_PERMANA * paramMultiplier;
        return Math.round(value);
    }

    public static int calculateDefenseMagnitude(int manaCost, float paramMultiplier) {
        return (int) ((TeenyBalance.BASE_DEFENSE_MAG + (manaCost * TeenyBalance.DEFENSE_MAG_PERMANA)) * paramMultiplier);
    }

    public static int calculateFreezeMagnitude(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        float base = manaCost * paramMultiplier * TeenyBalance.FREEZE_PERCENTAGE_PERMANA;
        float luck = caster.getEffectiveStat(bruhof.teenycraft.battle.StatType.LUCK, state);
        float luckMult = 1.0f + (luck * TeenyBalance.FREEZE_PERCENTAGE_PERLUCK);
        return (int) Math.min(base * luckMult, TeenyBalance.FREEZE_PERCENTAGE_MAX);
    }
    
    public static int calculateFreezeDuration(BattleFigure caster, IBattleState state, int manaCost, float paramMultiplier) {
        // Param index 1 is for duration in user prompt
        return calculateEffectDuration(caster, state, manaCost, paramMultiplier, TeenyBalance.FREEZE_DURATION_PERMANA, TeenyBalance.FREEZE_DURATION_PERLUCK);
    }
}
