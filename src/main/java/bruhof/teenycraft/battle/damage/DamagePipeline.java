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
        public boolean isCritical = false;
        public float knockback = 0.5f; // Default vanilla-ish knockback
        public List<String> effects = new ArrayList<>();
        
        // Constructor for simple use
        public DamageResult(int dmg) {
            this.baseDamagePerHit = dmg;
        }

        public DamageResult(int dmg, int count, boolean group, boolean crit) {
            this.baseDamagePerHit = dmg;
            this.hitCount = count;
            this.isGroupDamage = group;
            this.isCritical = crit;
        }
    }
    
    public static class MitigationResult {
        public int finalDamage;
        public boolean isDodged;
        public boolean isBlocked; // Shield
        
        public MitigationResult(int dmg, boolean dodged, boolean blocked) {
            this.finalDamage = dmg;
            this.isDodged = dodged;
            this.isBlocked = blocked;
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
        
        // 3. Critical Hit Check (Luck)
        boolean isCrit = attacker.tryCrit();
        if (isCrit) {
            float luckVal = attacker.getLuckStat(); // Stat from figure
            if (state != null) {
                 luckVal += state.getEffectMagnitude("luck_up"); // Add luck buff
            }
            float critMult = (luckVal / 100.0f * TeenyBalance.LUCK_BALANCE_MULTIPLIER) + TeenyBalance.BASE_LUCK_MULTIPLIER;
            rawDamage *= critMult;
        }

        DamageResult result = new DamageResult(Math.round(rawDamage));
        result.isCritical = isCrit;

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
    
    public static MitigationResult calculateMitigation(IBattleState state, BattleFigure victim, DamageResult incoming, AbilityData data, boolean isGolden) {
        int finalDmg = incoming.baseDamagePerHit;
        boolean isDodged = false;
        
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

        // 1. Dodge Check
        if (!undodgeable && victim.tryDodge()) {
            isDodged = true;
            int mitigation = victim.getDodgeStat();
            finalDmg = Math.max(0, finalDmg - mitigation);
        }
        
        // 2. Shield Check (To be implemented with effects)
        if (state != null && state.hasEffect("shield")) {
             // Logic: Consume shield charge, return 0 damage
             // For now, simple boolean check
             // return new MitigationResult(0, false, true);
        }
        
        return new MitigationResult(finalDmg, isDodged, false);
    }
}
