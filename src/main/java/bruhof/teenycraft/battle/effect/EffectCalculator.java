package bruhof.teenycraft.battle.effect;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
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
    
    public static int calculateHealMagnitude(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateGenericMagnitude(caster, manaCost, paramMultiplier, TeenyBalance.BASE_HEAL_PERMANA);
    }
    
    public static int calculateManaMagnitude(BattleFigure caster, AbilityLoader.AbilityData data, int manaCost, float paramMultiplier) {
        // Formula: Mana * BaseManaFill * Param
        // No stats (Power) or Tier Multipliers involved.
        float value = manaCost * TeenyBalance.BASE_MANA_FILL_PERMANA * paramMultiplier;
        
        return Math.round(value);
    }
    
    public static int calculateSelfShockDamage(BattleFigure caster, int manaCost, float paramMultiplier) {
        // Formula: Mana * MaxHP * Param * BaseSelfShock
        float value = manaCost * caster.getMaxHp() * paramMultiplier * TeenyBalance.BASE_SELF_SHOCK_PERMANA;
        return Math.round(value);
    }
    
    public static int calculateEffectDuration(BattleFigure caster, int manaCost, float paramMultiplier, float basePerMana, float basePerLuck) {
        // Formula: Mana * Param * BasePerMana * (1 + Luck * BasePerLuck)
        float luck = caster.getLuckStat();
        float luckMultiplier = 1.0f + (luck * basePerLuck);
        float value = manaCost * paramMultiplier * basePerMana * luckMultiplier;
        return Math.round(value * 20); // Convert Seconds to Ticks
    }

    public static int calculateStunDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.STUN_DURATION_PERMANA, TeenyBalance.STUN_DURATION_PERLUCK);
    }
    
    public static int calculateDanceDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.DANCE_DURATION_PERMANA, TeenyBalance.DANCE_DURATION_PERLUCK);
    }
    
    public static int calculateCurseDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.CURSE_DURATION_PERMANA, TeenyBalance.CURSE_DURATION_PERLUCK);
    }
    
    public static int calculateWaffleDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.WAFFLE_DURATION_PERMANA, TeenyBalance.WAFFLE_DURATION_PERLUCK);
    }

    public static int calculateCleanseDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.CLEANSE_DURATION_PERMANA, TeenyBalance.CLEANSE_DURATION_PERLUCK);
    }
    
    public static int calculateKissDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.KISS_DURATION_PERMANA, TeenyBalance.KISS_DURATION_PERLUCK);
    }

    public static int calculateFreezeMagnitude(BattleFigure caster, int manaCost, float paramMultiplier) {
        float base = manaCost * paramMultiplier * TeenyBalance.FREEZE_PERCENTAGE_PERMANA;
        float luckMult = 1.0f + (caster.getLuckStat() * TeenyBalance.FREEZE_PERCENTAGE_PERLUCK);
        return (int) Math.min(base * luckMult, TeenyBalance.FREEZE_PERCENTAGE_MAX);
    }
    
    public static int calculateFreezeDuration(BattleFigure caster, int manaCost, float paramMultiplier) {
        // Param index 1 is for duration in user prompt
        return calculateEffectDuration(caster, manaCost, paramMultiplier, TeenyBalance.STUN_DURATION_PERMANA, TeenyBalance.STUN_DURATION_PERLUCK);
    }
}
