package bruhof.teenycraft.battle;

import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.executor.BattleAbilityExecution;
import bruhof.teenycraft.battle.executor.BattleDamageResolver;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.atomic.AtomicInteger;

public class AbilityExecutor {
    private static final AtomicInteger ACCESSORY_REACTION_ID = new AtomicInteger(1);

    private AbilityExecutor() {
    }

    public static int nextAccessoryReactionId() {
        return ACCESSORY_REACTION_ID.getAndIncrement();
    }

    public static void executeAttack(LivingEntity attacker, BattleFigure figure, int slotIndex, Entity target) {
        BattleAbilityExecution.executeAttack(attacker, figure, slotIndex, target);
    }

    public static void executeAction(LivingEntity attacker, BattleFigure figure, int slotIndex) {
        BattleAbilityExecution.executeAction(attacker, figure, slotIndex);
    }

    public static void executeDebugCast(IBattleState casterState, LivingEntity caster, BattleFigure figure,
                                        AbilityLoader.AbilityData data, int manaCost, boolean isGolden, LivingEntity enemy) {
        BattleAbilityExecution.executeDebugCast(casterState, caster, figure, data, manaCost, isGolden, enemy);
    }

    public static void executeTofu(LivingEntity caster, IBattleState state) {
        BattleAbilityExecution.executeTofu(caster, state);
    }

    public static void finishCharge(IBattleState state, LivingEntity attacker) {
        BattleAbilityExecution.finishCharge(state, attacker);
    }

    public static void tickBlueChannel(IBattleState state, LivingEntity attacker) {
        BattleAbilityExecution.tickBlueChannel(state, attacker);
    }

    public static void resolveProjectile(IBattleState state, LivingEntity caster, IBattleState.PendingProjectile projectile) {
        BattleAbilityExecution.resolveProjectile(state, caster, projectile);
    }

    public static void announceDamage(LivingEntity attacker, Entity targetEntity, BattleFigure victim,
                                      DamagePipeline.MitigationResult mitigation, String source) {
        BattleDamageResolver.announceDamage(attacker, targetEntity, victim, mitigation, source);
    }

    public static int applyDamageToFigure(IBattleState attackerState, LivingEntity attacker, LivingEntity targetEntity, IBattleState victimState,
                                          BattleFigure victimFigure, DamagePipeline.DamageResult result, AbilityLoader.AbilityData data, int manaCost,
                                          boolean isGolden, boolean isReflected, boolean isPetFire, int accessoryReactionId,
                                          boolean canTriggerBirdarang) {
        return BattleDamageResolver.applyDamageToFigure(
                attackerState,
                attacker,
                targetEntity,
                victimState,
                victimFigure,
                result,
                data,
                manaCost,
                isGolden,
                isReflected,
                isPetFire,
                accessoryReactionId,
                canTriggerBirdarang
        );
    }
}
