package bruhof.teenycraft.battle.effect;

import java.util.HashMap;
import java.util.Map;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.battle.effect.BattleEffect.EffectType;
import bruhof.teenycraft.battle.effect.BattleEffect.EffectCategory;

import bruhof.teenycraft.capability.BattleState;

public class EffectRegistry {
    private static final Map<String, BattleEffect> REGISTRY = new HashMap<>();

    public static void register(BattleEffect effect) {
        REGISTRY.put(effect.getId(), effect);
    }

    public static BattleEffect get(String id) {
        return REGISTRY.get(id);
    }

    public static void init() {
        // Register Effects Here
        register(new PowerUpEffect());
        register(new PowerDownEffect());
        register(new CleanseEffect());
        register(new KissEffect());
        register(new StunEffect());
        register(new CleanseImmunityEffect());
        register(new BarFillEffect());
        register(new DanceEffect());
        register(new CurseEffect());
        register(new GroupHealEffect());
        register(new WaffleEffect());
        register(new HealEffect());
        register(new SelfShockEffect());
        register(new FreezeEffect());
        register(new FreezeMovementEffect());
        register(new TofuSpawnEffect());
    }

    // ==========================================
    // CONCRETE IMPLEMENTATIONS
    // ==========================================

    public static class TofuSpawnEffect extends BattleEffect {
        public TofuSpawnEffect() { super("tofu_spawn", EffectType.INSTANT, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            state.spawnTofu(magnitude);
            return false;
        }
    }

    public static class FreezeEffect extends BattleEffect {
        public FreezeEffect() { super("freeze", EffectType.INSTANT, EffectCategory.CONTROL); }
        
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            
            // Mana Burn logic (Immediate)
            float current = state.getCurrentMana();
            float burnAmount = current * (magnitude / 100.0f);
            state.consumeMana((int) burnAmount);
            
            // Apply persistent movement lock
            state.applyEffect("freeze_movement", duration, 0); 
            
            return false; // Instant
        }
    }

    public static class FreezeMovementEffect extends BattleEffect {
        public FreezeMovementEffect() { super("freeze_movement", EffectType.DURATION, EffectCategory.CONTROL); }
    }

    public static class WaffleEffect extends BattleEffect {
        public WaffleEffect() { super("waffle", EffectType.DURATION, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }
    
    public static class HealEffect extends BattleEffect {
        public HealEffect() { super("heal", EffectType.INSTANT, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            target.modifyHp(magnitude);
            return false;
        }
    }
    
    public static class SelfShockEffect extends BattleEffect {
        public SelfShockEffect() { super("self_shock", EffectType.INSTANT, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            target.modifyHp(-magnitude);
            return false;
        }
    }

    public static class GroupHealEffect extends BattleEffect {
        public GroupHealEffect() { super("group_heal", EffectType.INSTANT, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            
            java.util.List<BattleFigure> team = state.getTeam();
            java.util.List<BattleFigure> alive = new java.util.ArrayList<>();
            for (BattleFigure f : team) {
                if (f.getCurrentHp() > 0) alive.add(f);
            }
            
            if (!alive.isEmpty()) {
                int[] splits = bruhof.teenycraft.battle.damage.DistributionHelper.split(magnitude, alive.size());
                for (int i = 0; i < alive.size(); i++) {
                    alive.get(i).modifyHp(splits[i]);
                }
            }
            return false;
        }
    }

    public static class CurseEffect extends BattleEffect {
        public CurseEffect() { super("curse", EffectType.DURATION, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }

    public static class DanceEffect extends BattleEffect {
        public DanceEffect() { super("dance", EffectType.DURATION, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }
    }

    public static class BarFillEffect extends BattleEffect {
        public BarFillEffect() { super("bar_fill", EffectType.INSTANT, EffectCategory.SPECIAL); }

        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            state.addMana(magnitude);
            return false; 
        }
    }

    public static class PowerUpEffect extends BattleEffect {
        public PowerUpEffect() { super("power_up", EffectType.INFINITE, EffectCategory.BUFF); }
        
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            // Power Up Stacks Magnitude!
            // We use the 'magnitude' parameter as the stack amount.
            // BattleFigure logic handles the actual stat modification via 'activeEffectMagnitudes'.
            // Here we just confirm it's allowed.
            if (state.hasEffect("kiss")) return false; // Kiss blocks Buffs
            return true;
        }
        
        @Override
        public boolean onAttack(IBattleState state, BattleFigure attacker) {
            return true; // Consume on attack
        }
    }
    
    public static class PowerDownEffect extends BattleEffect {
        public PowerDownEffect() { super("power_down", EffectType.INFINITE, EffectCategory.DEBUFF); }

        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false; // Cleanse blocks Debuffs
            return true;
        }
        
        @Override
        public boolean onAttack(IBattleState state, BattleFigure attacker) {
            return true; // Consume on attack
        }
    }

    public static class CleanseEffect extends BattleEffect {
        public CleanseEffect() { super("cleanse", EffectType.INSTANT, EffectCategory.SPECIAL); }

        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
             if (state.hasEffect("kiss")) return false; // Kiss blocks Cleanse usage?
             
             // 1. Remove Debuffs
             state.removeEffectsByCategory(EffectCategory.DEBUFF);
             state.removeEffectsByCategory(EffectCategory.CONTROL);
             
             // 2. Apply Immunity (Lasting Effect)
             // We apply a SEPARATE effect for immunity
             state.applyEffect("cleanse_immunity", duration, 0); 
             
             return true; 
        }
    }
    
    public static class KissEffect extends BattleEffect {
        public KissEffect() { super("kiss", EffectType.DURATION, EffectCategory.DEBUFF); }
        
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }
    
    public static class StunEffect extends BattleEffect {
        public StunEffect() { super("stun", EffectType.DURATION, EffectCategory.CONTROL); }
        
         @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }
    
    public static class CleanseImmunityEffect extends BattleEffect {
        public CleanseImmunityEffect() { super("cleanse_immunity", EffectType.DURATION, EffectCategory.SPECIAL); }
    }
}
