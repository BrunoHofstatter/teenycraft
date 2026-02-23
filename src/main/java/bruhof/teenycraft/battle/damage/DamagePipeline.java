package bruhof.teenycraft.battle.damage;

import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.battle.BattleFigure;
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
        public int hitCount = 1;
        public boolean isGroupDamage = false;
        public boolean canCrit = false;
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
    }
    
    public static class MitigationResult {
        public int finalDamage;
        public boolean isDodged;
        public boolean isBlocked; // Shield
        public boolean isCritical;
        public int dodgeReduction;
        public int critBonus;
        
        public MitigationResult(int dmg, boolean dodged, boolean blocked, boolean critical) {
            this.finalDamage = dmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
            this.isCritical = critical;
        }

        public MitigationResult(int dmg, boolean dodged, boolean blocked, boolean critical, int dodgeRed, int critB) {
            this.finalDamage = dmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
            this.isCritical = critical;
            this.dodgeReduction = dodgeRed;
            this.critBonus = critB;
        }
    }

    /**
     * Calculates the outgoing damage package from an attacker using an ability.
     * Does NOT apply mitigation (Defense/Shield) yet, as that depends on the victim.
     */
    public static DamageResult calculateOutput(IBattleState state, BattleFigure attacker, int slotIndex) {
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
        
        return calculateOutput(state, attacker, data, manaCost);
    }

    public static DamageResult calculateOutput(IBattleState state, BattleFigure attacker, AbilityData data, int manaCost) {
        // 1. If damageTier is 0, it's a pure utility move.
        // We skip calculation to preserve Power Up/Down and Luck bags for real hits.
        if (data.damageTier == 0) {
            return new DamageResult(0, 1, false, false);
        }

        // 2. Base Damage Calculation (Using HOT stats from BattleFigure)
        // Formula: Power * ManaCost * BaseConstant * TierMultiplier
        int power = attacker.getPowerStat();
        float damageMultiplier = TeenyBalance.getDamageMultiplier(data.damageTier);
        
        // Calculate Raw Damage
        float rawDamage = power * manaCost * TeenyBalance.BASE_DAMAGE_PERMANA * damageMultiplier;
        
        // Add Flat Damage Modifications (Effects from State)
        if (state != null) {
            int bonus = state.getEffectMagnitude("flat_damage_up") + state.getEffectMagnitude("power_up");
            int malus = state.getEffectMagnitude("flat_damage_down") + state.getEffectMagnitude("power_down");
            rawDamage += (bonus - malus);
        }
        
        // 3. Critical Hit Readiness
        // We no longer roll here to allow multi-hit independent rolls.
        DamageResult result = new DamageResult(Math.round(rawDamage));
        result.canCrit = true; // Most abilities can crit

        // 4. Trait Processing (Multi-Hit, Group, etc.)
        for (TraitData trait : data.traits) {
            if ("multi_hit".equals(trait.id) && !trait.params.isEmpty()) {
                result.hitCount = Math.max(1, trait.params.get(0).intValue());
            }
            if ("group_damage".equals(trait.id)) {
                result.isGroupDamage = true;
            }
        }
        
        return result;
    }
    
    public static MitigationResult calculateMitigation(IBattleState state, BattleFigure victim, BattleFigure attacker, DamageResult incoming, AbilityData data, boolean isGolden) {
        int initialDmg = incoming.baseDamagePerHit;
        int critBonus = 0;
        boolean isCrit = false;

        // 1. Critical Hit Roll (Per Hit)
        if (incoming.canCrit && attacker != null && attacker.tryCrit()) {
            isCrit = true;
            float luckVal = attacker.getLuckStat();
            float critMult = (luckVal / 100.0f * TeenyBalance.LUCK_BALANCE_MULTIPLIER) + TeenyBalance.BASE_LUCK_MULTIPLIER;
            int critDmg = Math.round(initialDmg * critMult);
            critBonus = critDmg - initialDmg;
            initialDmg = critDmg;
        }
        
        MitigationResult res = calculateMitigationInternal(state, victim, initialDmg, data, isGolden);
        return new MitigationResult(res.finalDamage, res.isDodged, res.isBlocked, isCrit, res.dodgeReduction, critBonus);
    }

    public static MitigationResult calculatePoisonTick(IBattleState victimState, BattleFigure victim, BattleFigure attacker, float baseTickDamage) {
        int initialDmg = Math.round(baseTickDamage);
        int critBonus = 0;
        boolean isCrit = false;

        // 1. Roll for Crit (Attacker Context)
        if (attacker != null && attacker.tryCrit()) {
            isCrit = true;
            float luckVal = attacker.getLuckStat();
            float critMult = (luckVal / 100.0f * TeenyBalance.LUCK_BALANCE_MULTIPLIER) + TeenyBalance.BASE_LUCK_MULTIPLIER;
            int critDmg = Math.round(initialDmg * critMult);
            critBonus = critDmg - initialDmg;
            initialDmg = critDmg;
        }

        // 2. Standard Mitigation (Internal)
        MitigationResult mit = calculateMitigationInternal(victimState, victim, initialDmg, null, false);
        return new MitigationResult(mit.finalDamage, mit.isDodged, mit.isBlocked, isCrit, mit.dodgeReduction, critBonus);
    }

    private static MitigationResult calculateMitigationInternal(IBattleState state, BattleFigure victim, int initialDmg, AbilityData data, boolean isGolden) {
        int finalDmg = initialDmg;
        boolean isDodged = false;
        int dodgeReduction = 0;
        
        boolean undodgeable = false;
        if (data != null) {
            // Check Base Traits
            for (TraitData t : data.traits) {
                if ("undodgeable".equals(t.id)) {
                    undodgeable = true;
                    break;
                }
            }
            
            // Check Golden Traits
            if (!undodgeable && isGolden && data.goldenBonus != null) {
                for (String bonus : data.goldenBonus) {
                    if ("trait:undodgeable".equals(bonus)) {
                        undodgeable = true;
                        break;
                    }
                }
            }
        }

        // 1. Defense Multipliers (Relative)
        if (state != null) {
            int defUp = state.getEffectMagnitude("defense_up");
            int defDown = state.getEffectMagnitude("defense_down");
            
            if (defUp > 0) {
                float mult = 1.0f - (defUp / 100.0f);
                finalDmg = (int) (finalDmg * Math.max(0, mult));
            }
            if (defDown > 0) {
                float mult = 1.0f + (defDown / 100.0f);
                finalDmg = (int) (finalDmg * mult);
            }
        }

        // 2. Shield Check (Absolute Negation) - Check BEFORE Dodge so it can skip dodge roll
        boolean isShielded = false;
        if (state != null && state.hasEffect("shield")) {
             state.removeEffect("shield");
             isShielded = true;
             if (!undodgeable) {
                 finalDmg = 0;
             }
        }

        // 3. Dodge Check (Absolute) - ONLY if there is damage to dodge
        int bagModifier = 0;
        float smokeMult = 0;
        bruhof.teenycraft.battle.effect.EffectInstance smoke = null;
        
        if (state != null && state.hasEffect("dodge_smoke")) {
            smoke = state.getEffectInstance("dodge_smoke");
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
        }
        
        // Consume Dodge Smoke charge (every attack)
        if (smoke != null) {
            smoke.magnitude--;
            if (smoke.magnitude <= 0) {
                state.removeEffect("dodge_smoke");
            }
        }
        
        return new MitigationResult(finalDmg, isDodged, isShielded, false, dodgeReduction, 0);
    }
}
