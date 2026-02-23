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
        register(new BarDepleteEffect());
        register(new ShieldEffect());
        register(new DispelEffect());
        register(new DodgeSmokeEffect());
        register(new DefenseUpEffect());
        register(new DefenseDownEffect());
        register(new RootEffect());
        register(new ShockEffect());
        register(new DisableEffect(0));
        register(new DisableEffect(1));
        register(new DisableEffect(2));
        register(new PoisonEffect());
        register(new HealthRadioEffect());
        register(new PowerRadioEffect());
    }

    // ==========================================
    // CONCRETE IMPLEMENTATIONS
    // ==========================================

    public static class HealthRadioEffect extends PeriodicBattleEffect {
        public HealthRadioEffect() { super("health_radio", EffectCategory.BUFF); }
        
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }

        @Override
        protected void onPeriodicTick(IBattleState state, BattleFigure target, int remainingDuration, EffectInstance inst) {
            int finalTickMag = getSmartSplitValue(inst, remainingDuration);
            if (finalTickMag > 0) {
                target.modifyHp(finalTickMag);
            }
        }
    }

    public static class PowerRadioEffect extends PeriodicBattleEffect {
        public PowerRadioEffect() { super("power_radio", EffectCategory.BUFF); }

        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }

        @Override
        protected void onPeriodicTick(IBattleState state, BattleFigure target, int remainingDuration, EffectInstance inst) {
            int finalTickMag = getSmartSplitValue(inst, remainingDuration);
            if (finalTickMag > 0) {
                state.applyEffect("power_up", -1, finalTickMag);
            }
        }
    }

    public static class PoisonEffect extends PeriodicBattleEffect {
        public PoisonEffect() { super("poison", EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }

        @Override
        protected void onPeriodicTick(IBattleState state, BattleFigure target, int remainingDuration, EffectInstance inst) {
            BattleFigure attackerFigure = null;
            net.minecraft.server.level.ServerPlayer attackerPlayer = null;
            net.minecraft.world.entity.LivingEntity victimEntity = null;

            if (inst.casterUUID != null && state instanceof BattleState bs) {
                // Caster vs Victim Logic
                if (inst.casterUUID.equals(state.getOpponentEntityUUID())) {
                    // The opponent poisoned the player
                    attackerFigure = state.getActiveOpponent();
                    victimEntity = state.getPlayerEntity();
                    attackerPlayer = state.getPlayerEntity(); // In PVE, notify the player they were hit
                } else {
                    // The player poisoned the opponent
                    attackerFigure = state.getActiveFigure();
                    attackerPlayer = state.getPlayerEntity();
                    victimEntity = state.getOpponentEntity(attackerPlayer.serverLevel());
                }
            }

            bruhof.teenycraft.battle.damage.DamagePipeline.MitigationResult mit = 
                bruhof.teenycraft.battle.damage.DamagePipeline.calculatePoisonTick(state, target, attackerFigure, inst.power);

            if (mit.finalDamage > 0) {
                target.modifyHp(-mit.finalDamage);
            }
            
            // Announce Poison Damage
            bruhof.teenycraft.battle.AbilityExecutor.announceDamage(attackerPlayer, victimEntity, target, mit, "Poison");

            // Visual feedback (Flash only, no knockback) if not dodged
            if (!mit.isDodged && victimEntity != null) {
                // Only flash if the target is currently active
                if (target == state.getActiveFigure() || target == state.getActiveOpponent()) {
                    victimEntity.handleEntityEvent((byte)2); // Red Flash
                    victimEntity.level().playSound(null, victimEntity.getX(), victimEntity.getY(), victimEntity.getZ(), 
                        net.minecraft.sounds.SoundEvents.PLAYER_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
                }
            }
        }
    }

    public static class ShockEffect extends PeriodicBattleEffect {
        public ShockEffect() { super("shock", EffectCategory.CONTROL); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }

        @Override
        protected void onPeriodicTick(IBattleState state, BattleFigure target, int remainingDuration, EffectInstance inst) {
            int shockLength = (int) inst.power;
            state.applyEffect("stun", shockLength, 1);
            state.applyEffect("freeze_movement", shockLength, 0);
        }
    }

    public static class RootEffect extends BattleEffect {
        public RootEffect() { super("root", EffectType.DURATION, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }

    public static class DisableEffect extends BattleEffect {
        public DisableEffect(int index) { super("disable_" + index, EffectType.DURATION, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }

    public static class DefenseUpEffect extends BattleEffect {
        public DefenseUpEffect() { super("defense_up", EffectType.DURATION, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }
    }

    public static class DefenseDownEffect extends BattleEffect {
        public DefenseDownEffect() { super("defense_down", EffectType.DURATION, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            return true;
        }
    }

    public static class DodgeSmokeEffect extends BattleEffect {
        public DodgeSmokeEffect() { super("dodge_smoke", EffectType.DURATION, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }
    }

    public static class DispelEffect extends BattleEffect {
        public DispelEffect() { super("dispel", EffectType.INSTANT, EffectCategory.DEBUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            // Check for Cleanse Immunity (which blocks dispels)
            if (state.hasEffect("cleanse_immunity")) return false;
            
            // Remove all positive buffs from the target
            state.removeEffectsByCategory(EffectCategory.BUFF);
            return false; // Instant
        }
    }

    public static class ShieldEffect extends BattleEffect {
        public ShieldEffect() { super("shield", EffectType.DURATION, EffectCategory.BUFF); }
        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("kiss")) return false;
            return true;
        }
    }

    public static class BarDepleteEffect extends BattleEffect {
        public BarDepleteEffect() { super("bar_deplete", EffectType.INSTANT, EffectCategory.DEBUFF); }

        @Override
        public boolean onApply(IBattleState state, BattleFigure target, int duration, int magnitude) {
            if (state.hasEffect("cleanse_immunity")) return false;
            state.consumeMana(magnitude);
            return false; 
        }
    }

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
        public boolean canStackMagnitude() { return true; }

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
        public boolean canStackMagnitude() { return true; }

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
            
            // Kiss removes all existing buffs
            state.removeEffectsByCategory(EffectCategory.BUFF);
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
