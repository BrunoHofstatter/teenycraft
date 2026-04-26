package bruhof.teenycraft.battle.damage;

import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.FigureClassType;
import bruhof.teenycraft.battle.StatType;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.AbilityLoader.AbilityData;
import bruhof.teenycraft.util.AbilityLoader.TraitData;
import bruhof.teenycraft.TeenyBalance;

import java.util.ArrayList;
import java.util.List;

public class DamagePipeline {

    public static class DamageResult {
        public int baseDamagePerHit;
        public int classBonusDamagePerHit = 0;
        public int hitCount = 1;
        public boolean isGroupDamage = false;
        public boolean canCrit = false;
        public boolean classBonusEligible = false;
        public boolean undodgeable = false;
        public float knockback = 0.5f; // Default vanilla-ish knockback
        public List<String> effects = new ArrayList<>();
        
        // Constructor for simple use
        public DamageResult(int dmg) {
            this.baseDamagePerHit = dmg;
        }

        public DamageResult(int dmg, int count, boolean group, boolean canCrit) {
            this.baseDamagePerHit = dmg;
            this.hitCount = count;
            this.isGroupDamage = group;
            this.canCrit = canCrit;
        }

        public int getTotalDamagePerHit() {
            return baseDamagePerHit + classBonusDamagePerHit;
        }
    }
    
    public static class MitigationResult {
        public int finalDamage;
        public int finalBaseDamage;
        public int finalClassBonusDamage;
        public boolean isDodged;
        public boolean isBlocked; // Shield
        public boolean isCritical;
        public int dodgeReduction;
        public int critBonus;
        
        public MitigationResult(int dmg, boolean dodged, boolean blocked, boolean critical) {
            this.finalBaseDamage = dmg;
            this.finalClassBonusDamage = 0;
            this.finalDamage = dmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
            this.isCritical = critical;
        }

        public MitigationResult(int dmg, boolean dodged, boolean blocked, boolean critical, int dodgeRed, int critB) {
            this.finalBaseDamage = dmg;
            this.finalClassBonusDamage = 0;
            this.finalDamage = dmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
            this.isCritical = critical;
            this.dodgeReduction = dodgeRed;
            this.critBonus = critB;
        }

        public MitigationResult(int dmg, int baseDmg, int classBonusDmg, boolean dodged, boolean blocked, boolean critical,
                                int dodgeRed, int critB) {
            this.finalDamage = dmg;
            this.finalBaseDamage = baseDmg;
            this.finalClassBonusDamage = classBonusDmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
            this.isCritical = critical;
            this.dodgeReduction = dodgeRed;
            this.critBonus = critB;
        }
    }

    private static class MitigationBreakdown {
        private int finalDamage;
        private int finalBaseDamage;
        private boolean isDodged;
        private boolean isBlocked;
        private int dodgeReduction;

        private MitigationBreakdown(int finalDamage, int finalBaseDamage, boolean isDodged, boolean isBlocked, int dodgeReduction) {
            this.finalDamage = finalDamage;
            this.finalBaseDamage = finalBaseDamage;
            this.isDodged = isDodged;
            this.isBlocked = isBlocked;
            this.dodgeReduction = dodgeReduction;
        }
    }

    /**
     * Calculates the outgoing damage package from an attacker using an ability.
     * Does NOT apply mitigation (Defense/Shield) yet, as that depends on the victim.
     */
    public static DamageResult calculateOutput(IBattleState attackerState, BattleFigure attacker, int slotIndex) {
        return calculateOutput(attackerState, attacker, slotIndex, false);
    }

    public static DamageResult calculateOutput(IBattleState attackerState, BattleFigure attacker, int slotIndex, boolean isGolden) {
        // 1. Get Ability Data
        java.util.ArrayList<String> order = ItemFigure.getAbilityOrder(attacker.getOriginalStack());
        if (slotIndex >= order.size()) return new DamageResult(0);
        
        String abilityId = order.get(slotIndex);
        AbilityData data = AbilityLoader.getAbility(abilityId);
        if (data == null) return new DamageResult(0);

        // Get Mana Cost
        java.util.ArrayList<String> tiers = ItemFigure.getAbilityTiers(attacker.getOriginalStack());
        String tierLetter = (slotIndex < tiers.size()) ? tiers.get(slotIndex) : "a";
        int manaCost = TeenyBalance.getManaCost(slotIndex + 1, tierLetter);
        
        return calculateOutput(attackerState, attacker, data, manaCost, isGolden);
    }

    public static DamageResult calculateOutput(IBattleState attackerState, BattleFigure attacker, AbilityData data, int manaCost) {
        return calculateOutput(attackerState, attacker, data, manaCost, false);
    }

    public static DamageResult calculateOutput(IBattleState attackerState, BattleFigure attacker, AbilityData data, int manaCost, boolean isGolden) {
        // 1. If damageTier is 0, it's a pure utility move.
        // We skip calculation to preserve Power Up/Down and Luck bags for real hits.
        if (data.damageTier == 0) {
            return new DamageResult(0, 1, false, false);
        }

        // 2. Base Damage Calculation (Using HOT stats from BattleFigure)
        // Formula: Power * ManaCost * BaseConstant * TierMultiplier
        int power = attacker.getEffectiveStat(StatType.POWER, attackerState);
        float damageMultiplier = TeenyBalance.getDamageMultiplier(data.damageTier);
        
        // Calculate Raw Damage
        float rawDamage = power * manaCost * TeenyBalance.BASE_DAMAGE_PERMANA * damageMultiplier;
        
        // --- TRAIT MODIFIERS (Activate & Charge Up) ---
        if (data.traits != null) {
            for (TraitData t : data.traits) {
                if ("activate".equals(t.id)) {
                    float required = t.params.isEmpty() ? 2.0f : t.params.get(0);
                    rawDamage *= (required * TeenyBalance.ACTIVATE_DAMAGE_MULT);
                } else if ("charge_up".equals(t.id)) {
                    float seconds = t.params.isEmpty() ? 1.0f : t.params.get(0);
                    rawDamage *= (seconds * TeenyBalance.CHARGE_UP_MULT_PER_SEC);
                }
            }
        }

        // --- SURPRISE LOGIC ---
        boolean hasSurprise = false;
        if (isGolden && data.hasGoldenBonus(AbilityLoader.GoldenBonusScope.TRAIT, "surprise")) {
            hasSurprise = true;
        } else if (data.traits != null) {
            for (AbilityLoader.TraitData t : data.traits) {
                if ("surprise".equals(t.id)) {
                    hasSurprise = true;
                    break;
                }
            }
        }
        
        if (hasSurprise) {
            int baseInt = Math.round(rawDamage);
            int variance = Math.round(baseInt * TeenyBalance.SURPRISE_DAMAGE_VARIANCE);
            if (variance > 0) {
                // Pick a random integer offset in [-variance, variance]
                // For variance 2, this gives: random(5) - 2 -> {-2, -1, 0, 1, 2}
                int offset = (int)(Math.random() * (variance * 2 + 1)) - variance;
                rawDamage = baseInt + offset;
            }
        }

        // Add Flat Damage Modifications (Effects from State)
        rawDamage += attacker.getEffectiveStat(StatType.FLAT_DAMAGE, attackerState);
        
        // 3. Critical Hit Readiness
        // We no longer roll here to allow multi-hit independent rolls.
        DamageResult result = new DamageResult(Math.round(rawDamage));
        result.canCrit = true; // Most abilities can crit
        result.classBonusEligible = true;

        // 4. Trait Processing
        bruhof.teenycraft.battle.trait.TraitRegistry.triggerPipelineHooks(data.traits, result);
        
        return result;
    }
    
    public static MitigationResult calculateMitigation(IBattleState victimState, BattleFigure victim, IBattleState attackerState, BattleFigure attacker, DamageResult incoming, AbilityData data, boolean isGolden) {
        int initialBaseDamage = incoming.baseDamagePerHit;
        int initialTotalDamage = incoming.getTotalDamagePerHit();
        int critBonus = 0;
        boolean isCrit = false;

        // 1. Critical Hit Roll (Per Hit)
        if (initialTotalDamage > 0 && incoming.canCrit && attacker != null && attacker.tryCrit(attackerState)) {
            isCrit = true;
            float luckVal = attacker.getEffectiveStat(StatType.LUCK, attackerState);
            float critMult = (luckVal / 100.0f * TeenyBalance.LUCK_BALANCE_MULTIPLIER) + TeenyBalance.BASE_LUCK_MULTIPLIER;
            int critTotalDamage = Math.round(initialTotalDamage * critMult);
            int critBaseDamage = Math.round(initialBaseDamage * critMult);
            critBonus = critTotalDamage - initialTotalDamage;
            initialTotalDamage = critTotalDamage;
            initialBaseDamage = Math.min(critBaseDamage, initialTotalDamage);
        }
        
        MitigationBreakdown res = calculateMitigationInternal(
                victimState,
                victim,
                initialTotalDamage,
                initialBaseDamage,
                incoming.undodgeable,
                data,
                isGolden
        );
        
        // 2. Flight Evasion Check
        if (victimState != null && victimState.hasEffect("flight") && !incoming.isGroupDamage) {
            res.isDodged = true;
            res.dodgeReduction = res.finalDamage;
            res.finalDamage = 0;
            res.finalBaseDamage = 0;
        }

        int finalBaseDamage = Math.min(res.finalBaseDamage, res.finalDamage);
        int finalClassBonusDamage = Math.max(0, res.finalDamage - finalBaseDamage);
        return new MitigationResult(
                res.finalDamage,
                finalBaseDamage,
                finalClassBonusDamage,
                res.isDodged,
                res.isBlocked,
                isCrit,
                res.dodgeReduction,
                critBonus
        );
    }

    public static MitigationResult calculatePoisonTick(IBattleState victimState, BattleFigure victim, BattleFigure attacker, IBattleState attackerState, float baseTickDamage) {
        int initialDamage = Math.round(baseTickDamage);
        int critBonus = 0;
        boolean isCrit = false;

        // 1. Roll for Crit (Attacker Context)
        if (initialDamage > 0 && attacker != null && attacker.tryCrit(attackerState)) {
            isCrit = true;
            float luckVal = attacker.getLuckStat(); // Fallback to base stat if attackerState is null (which it shouldn't be for long)
            if (attackerState != null) luckVal = attacker.getEffectiveStat(StatType.LUCK, attackerState);
            
            float critMult = (luckVal / 100.0f * TeenyBalance.LUCK_BALANCE_MULTIPLIER) + TeenyBalance.BASE_LUCK_MULTIPLIER;
            int critDmg = Math.round(initialDamage * critMult);
            critBonus = critDmg - initialDamage;
            initialDamage = critDmg;
        }

        // 2. Standard Mitigation (Internal)
        MitigationBreakdown mit = calculateMitigationInternal(victimState, victim, initialDamage, initialDamage, false, null, false);
        return new MitigationResult(mit.finalDamage, mit.finalBaseDamage, 0, mit.isDodged, mit.isBlocked, isCrit, mit.dodgeReduction, critBonus);
    }

    public static DamageResult[] splitIntoHitResults(DamageResult result) {
        int[] hitSplits = DistributionHelper.split(result.baseDamagePerHit, result.hitCount);
        DamageResult[] splitResults = new DamageResult[hitSplits.length];
        for (int i = 0; i < hitSplits.length; i++) {
            DamageResult split = new DamageResult(hitSplits[i], 1, result.isGroupDamage, result.canCrit);
            split.classBonusDamagePerHit = (i == hitSplits.length - 1) ? result.classBonusDamagePerHit : 0;
            split.classBonusEligible = result.classBonusEligible && i == hitSplits.length - 1;
            split.undodgeable = result.undodgeable;
            split.knockback = result.knockback;
            split.effects.addAll(result.effects);
            splitResults[i] = split;
        }
        return splitResults;
    }

    public static void populateClassBonus(DamageResult result, BattleFigure attacker, BattleFigure victim) {
        if (result == null || attacker == null || victim == null || !result.classBonusEligible) {
            return;
        }
        if (result.classBonusDamagePerHit > 0 || result.baseDamagePerHit <= 0) {
            return;
        }

        FigureClassType attackerClass = attacker.getFigureClass();
        FigureClassType victimClass = victim.getFigureClass();
        if (!attackerClass.hasAdvantageOver(victimClass)) {
            return;
        }

        result.classBonusDamagePerHit = Math.max(1, Math.round(result.baseDamagePerHit * TeenyBalance.CLASS_ADVANTAGE_BONUS_MULT));
    }

    private static MitigationBreakdown calculateMitigationInternal(IBattleState victimState, BattleFigure victim, int initialDmg,
                                                                   int initialBaseDamage, boolean isUndodgeable, AbilityData data,
                                                                   boolean isGolden) {
        int finalDmg = initialDmg;
        int finalBaseDamage = Math.min(initialBaseDamage, initialDmg);
        boolean isDodged = false;
        int dodgeReduction = 0;
        
        boolean undodgeable = isUndodgeable;
        if (data != null && !undodgeable && isGolden) {
            undodgeable = data.hasGoldenBonus(AbilityLoader.GoldenBonusScope.TRAIT, "undodgeable");
        }

        // 1. Defense Multipliers (Relative)
        int defMod = victim.getEffectiveStat(StatType.DEFENSE_PERCENT, victimState);
        if (defMod != 0) {
            finalDmg = applyPercentModifier(finalDmg, defMod);
            finalBaseDamage = applyPercentModifier(finalBaseDamage, defMod);
            finalBaseDamage = Math.min(finalBaseDamage, finalDmg);
        }

        // 1.5 Reflect Reduction (Multiplicative)
        if (victimState != null && victimState.hasEffect("reflect")) {
            int reflectMag = victimState.getEffectMagnitude("reflect");
            finalDmg = applyRatioModifier(finalDmg, reflectMag);
            finalBaseDamage = applyRatioModifier(finalBaseDamage, reflectMag);
            finalBaseDamage = Math.min(finalBaseDamage, finalDmg);
        }
        finalBaseDamage = Math.min(finalBaseDamage, finalDmg);

        // 2. Shield Check (Absolute Negation) - Check BEFORE Dodge so it can skip dodge roll
        boolean isShielded = false;
        if (victimState != null && victimState.hasEffect("shield")) {
             victimState.removeEffect("shield");
             isShielded = true;
             if (!undodgeable) {
                 finalDmg = 0;
                 finalBaseDamage = 0;
             }
        }

        // 3. Dodge Check (Absolute) - ONLY if there is damage to dodge
        int bagModifier = 0;
        float smokeMult = 0;
        bruhof.teenycraft.battle.effect.EffectInstance smoke = null;
        
        if (victimState != null && victimState.hasEffect("dodge_smoke")) {
            smoke = victimState.getEffectInstance("dodge_smoke");
            if (smoke != null) {
                bagModifier = (int)(smoke.power * TeenyBalance.DODGE_SMOKE_BAGSIZE_PERMANA);
                smokeMult = smoke.power * TeenyBalance.DODGE_SMOKE_MULT_PERMANA;
            }
        }

        if (!undodgeable && finalDmg > 0 && victim.tryDodge(bagModifier)) {
            isDodged = true;
            int mitigation = victim.getDodgeStat();
            
            // Apply Dodge Smoke Multiplier to mitigation
            if (smokeMult > 0) {
                mitigation = (int)(mitigation * (1.0f + smokeMult));
            }
            
            int preDodge = finalDmg;
            finalDmg = Math.max(0, finalDmg - mitigation);
            dodgeReduction = preDodge - finalDmg;
            finalBaseDamage = Math.max(0, finalBaseDamage - mitigation);
            finalBaseDamage = Math.min(finalBaseDamage, finalDmg);
        }
        
        // Consume Dodge Smoke charge (every attack)
        if (smoke != null) {
            smoke.magnitude--;
            if (smoke.magnitude <= 0) {
                victimState.removeEffect("dodge_smoke");
            }
        }
        
        return new MitigationBreakdown(finalDmg, finalBaseDamage, isDodged, isShielded, dodgeReduction);
    }

    private static int applyPercentModifier(int damage, int percentModifier) {
        float mult = 1.0f - (percentModifier / 100.0f);
        return (int) (damage * Math.max(0, mult));
    }

    private static int applyRatioModifier(int damage, int percentKept) {
        return (int) (damage * (percentKept / 100.0f));
    }
}
