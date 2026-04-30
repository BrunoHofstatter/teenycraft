package bruhof.teenycraft.battle.executor;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.accessory.AccessoryExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.StatType;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.chip.ChipExecutor;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BattleDamageResolver {
    private BattleDamageResolver() {
    }

    public static DamageResult buildDamageResult(BattleAbilityContext context) {
        DamageResult result = DamagePipeline.calculateOutput(
                context.state(),
                context.figure(),
                context.data(),
                context.effectiveManaCost(),
                context.isGolden()
        );

        if (context.hasOpponentEffect("remote_mine")) {
            result.baseDamagePerHit = 0;
        }
        return result;
    }

    public static int applyDamage(BattleAbilityContext context, LivingEntity target, DamageResult result,
                                  boolean isPetFire, int accessoryReactionId, boolean canTriggerBirdarang) {
        IBattleState victimState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);

        if (victimState != null && victimState.isBattling()) {
            return applyDamageToFigure(
                    context.state(),
                    context.attacker(),
                    target,
                    victimState,
                    victimState.getActiveFigure(),
                    result,
                    context.data(),
                    context.effectiveManaCost(),
                    context.isGolden(),
                    false,
                    isPetFire,
                    accessoryReactionId,
                    canTriggerBirdarang
            );
        }

        target.hurt(context.attacker().damageSources().mobAttack(context.attacker()), (float) result.baseDamagePerHit);
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§cDealt " + result.baseDamagePerHit + " dmg!"));
        }
        return result.baseDamagePerHit;
    }

    public static int applyDamageToFigure(IBattleState attackerState, LivingEntity attacker, LivingEntity targetEntity, IBattleState victimState,
                                          BattleFigure victimFigure, DamageResult result, AbilityLoader.AbilityData data, int effectiveManaCost,
                                          boolean isGolden, boolean isReflected, boolean isPetFire, int accessoryReactionId,
                                          boolean canTriggerBirdarang) {
        if (victimState == null || victimFigure == null) {
            return 0;
        }

        return applyDamageToFigure(new DamageApplicationContext(
                attackerState,
                attacker,
                targetEntity,
                victimState,
                victimFigure,
                result,
                data,
                effectiveManaCost,
                isGolden,
                isReflected,
                isPetFire,
                accessoryReactionId,
                canTriggerBirdarang
        ));
    }

    public static void announceDamage(LivingEntity attacker, Entity targetEntity, BattleFigure victim,
                                      DamagePipeline.MitigationResult mitigation, String source) {
        if (victim == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Â§7[").append(source).append("] ");
        sb.append("Â§cHit ").append(victim.getNickname()).append(": ");
        sb.append("Â§f").append(mitigation.finalDamage).append("Â§c Damage");

        if (mitigation.isDodged && mitigation.dodgeReduction > 0) {
            sb.append(" Â§e(-").append(mitigation.dodgeReduction).append(" Dodged!)");
        }
        if (mitigation.isCritical && mitigation.critBonus > 0) {
            sb.append(" Â§cÂ§l(+").append(mitigation.critBonus).append(" Crit!)");
        }

        sb.append(" Â§7(").append(victim.getCurrentHp()).append(" HP left)");

        if (mitigation.finalClassBonusDamage > 0) {
            sb.append(" §a(+").append(mitigation.finalClassBonusDamage).append(" Class!)");
        }

        Component msg = Component.literal(sb.toString());
        if (attacker instanceof ServerPlayer sp) {
            sp.sendSystemMessage(msg);
        }

        if (targetEntity instanceof ServerPlayer victimPlayer && victimPlayer != attacker) {
            victimPlayer.sendSystemMessage(msg);
        }
    }

    private static int applyDamageToFigure(DamageApplicationContext context) {
        BattleFigure attackerFigure = context.attackerState() != null ? context.attackerState().getActiveFigure() : null;
        DamagePipeline.populateClassBonus(context.result(), attackerFigure, context.victimFigure());
        stripFlightForGroupDamage(context);

        DamagePipeline.MitigationResult mitigation = DamagePipeline.calculateMitigation(
                context.victimState(),
                context.victimFigure(),
                context.attackerState(),
                attackerFigure,
                context.result(),
                context.data(),
                context.isGolden()
        );

        int instantDamage = resolveInstantDamage(context, mitigation);
        announceBlockState(context, mitigation);

        IBattleState.CombatMutationResult mutation = null;
        if (instantDamage > 0) {
            instantDamage = AccessoryExecutor.onIncomingDamage(
                    context.victimState(),
                    context.targetEntity(),
                    context.victimFigure(),
                    context.attacker(),
                    instantDamage,
                    context.accessoryReactionId(),
                    context.canTriggerBirdarang()
            );
            mutation = context.victimState().applyCombatFigureDelta(
                    context.victimFigure(),
                    -instantDamage,
                    combatSource(context.attackerState(), context.attacker(), attackerFigure)
            );

            if (mitigation.isCritical && context.attackerState() != null && context.attacker() != null && attackerFigure != null) {
                ChipExecutor.onCritHit(context.attackerState(), context.attacker(), attackerFigure, context.victimState(), context.targetEntity());
            }

            triggerPetFollowUps(context, instantDamage, attackerFigure);
            triggerReflectFollowUps(context, instantDamage, attackerFigure);
        }

        if (instantDamage > 0 || mitigation.isDodged || mitigation.isBlocked) {
            announceDamage(context.attacker(), context.targetEntity(), context.victimFigure(), mitigation,
                    context.data() != null ? context.data().name : "Attack");
        }

        if (mutation != null) {
            context.victimState().resolveCombatFigureDelta(mutation);
        }

        if (!mitigation.isDodged && !context.isReflected()) {
            applyOpponentEffects(context);
            applyVanillaHitFeedback(context, instantDamage);
        }

        return instantDamage;
    }

    private static void stripFlightForGroupDamage(DamageApplicationContext context) {
        if (context.result().isGroupDamage && context.victimState().hasEffect("flight")) {
            context.victimState().removeEffect("flight");
            if (context.targetEntity() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§bFlight broken by Group Damage!"));
            }
        }
    }

    private static int resolveInstantDamage(DamageApplicationContext context, DamagePipeline.MitigationResult mitigation) {
        int instantDamage = mitigation.finalDamage;
        if (context.data() != null && context.data().effectsOnOpponent != null) {
            for (AbilityLoader.EffectData effect : context.data().effectsOnOpponent) {
                if ("poison".equals(effect.id)) {
                    return 0;
                }
            }
        }
        return instantDamage;
    }

    private static void announceBlockState(DamageApplicationContext context, DamagePipeline.MitigationResult mitigation) {
        if (!mitigation.isBlocked) {
            return;
        }

        if (mitigation.finalDamage > 0) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§bÂ§lSHIELD PIERCED!"));
            }
            if (context.targetEntity() instanceof ServerPlayer tp) {
                tp.sendSystemMessage(Component.literal("Â§cÂ§lSHIELD PIERCED!"));
            }
            return;
        }

        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§bBLOCKED!"));
        }
    }

    private static void triggerPetFollowUps(DamageApplicationContext context, int instantDamage, @Nullable BattleFigure attackerFigure) {
        if (instantDamage <= 0 || context.isPetFire() || context.attackerState() == null || attackerFigure == null) {
            return;
        }

        firePetSlot(context, "pet_slot_1", "pet_fire_1", "Â§bÂ§lPET 1! Â§fFired for ");
        firePetSlot(context, "pet_slot_2", "pet_fire_2", "Â§bÂ§lPET 2! Â§fFired for ");
    }

    private static void firePetSlot(DamageApplicationContext context, String effectId, String cooldownId, String messagePrefix) {
        if (!context.attackerState().hasEffect(effectId)) {
            return;
        }

        int internalCooldown = context.attackerState().getInternalCooldown(cooldownId);
        if (internalCooldown > 0) {
            return;
        }

        int petMagnitude = context.attackerState().getEffectMagnitude(effectId);
        DamageResult petResult = new DamageResult(petMagnitude, 1, false, false);
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(messagePrefix + petMagnitude));
        }

        applyDamageToFigure(
                context.attackerState(),
                context.attacker(),
                context.targetEntity(),
                context.victimState(),
                context.victimFigure(),
                petResult,
                null,
                0,
                false,
                false,
                true,
                context.accessoryReactionId(),
                false
        );
        context.attackerState().setInternalCooldown(cooldownId, TeenyBalance.PET_FIRE_COOLDOWN);
    }

    private static void triggerReflectFollowUps(DamageApplicationContext context, int instantDamage, @Nullable BattleFigure attackerFigure) {
        if (instantDamage <= 0 || context.isReflected() || attackerFigure == null) {
            return;
        }

        int cutenessPercent = context.victimFigure().getEffectiveStat(StatType.REFLECT_PERCENT, context.victimState());
        if (cutenessPercent > 0) {
            int reflectedBase = Math.round(instantDamage * (cutenessPercent / 100.0f));
            if (reflectedBase > 0) {
                DamageResult reflectResult = new DamageResult(reflectedBase);
                reflectResult.canCrit = true;
                reflectResult.classBonusEligible = true;
                if (context.targetEntity() instanceof ServerPlayer tp) {
                    tp.sendSystemMessage(Component.literal("Â§dÂ§lREFLECTED! Â§fDealt " + reflectedBase + " back!"));
                }
                applyDamageToFigure(
                        context.victimState(),
                        context.targetEntity(),
                        context.attacker(),
                        context.attackerState(),
                        attackerFigure,
                        reflectResult,
                        null,
                        0,
                        false,
                        true,
                        false,
                        context.accessoryReactionId(),
                        false
                );
            }
        }

        if (context.victimState().hasEffect("reflect")) {
            bruhof.teenycraft.battle.effect.EffectInstance reflect = context.victimState().getEffectInstance("reflect");
            if (reflect != null && reflect.power > 0) {
                float reductionMult = reflect.magnitude / 100.0f;
                float preReflectDamage = reductionMult > 0 ? (instantDamage / reductionMult) : instantDamage;

                int reflectedBase = Math.round(preReflectDamage * (reflect.power / 100.0f));
                if (reflectedBase > 0) {
                    DamageResult reflectResult = new DamageResult(reflectedBase);
                    reflectResult.canCrit = true;
                    reflectResult.classBonusEligible = true;
                    if (context.targetEntity() instanceof ServerPlayer tp) {
                        tp.sendSystemMessage(Component.literal("Â§bÂ§lREFLECTED! Â§fDealt " + reflectedBase + " back!"));
                    }
                    applyDamageToFigure(
                            context.victimState(),
                            context.targetEntity(),
                            context.attacker(),
                            context.attackerState(),
                            attackerFigure,
                            reflectResult,
                            null,
                            0,
                            false,
                            true,
                            false,
                            context.accessoryReactionId(),
                            false
                    );
                    context.victimState().removeEffect("reflect");
                }
            }
        }
    }

    private static void applyOpponentEffects(DamageApplicationContext context) {
        if (context.data() == null) {
            return;
        }

        BattleFigure attackingFigure = context.attackerState() != null ? context.attackerState().getActiveFigure() : null;
        for (AbilityLoader.EffectData effect : context.data().effectsOnOpponent) {
            EffectApplierRegistry.getValidated(effect.id).apply(
                    context.attackerState(),
                    context.attacker(),
                    attackingFigure,
                    context.data(),
                    context.effectiveManaCost(),
                    effect.params,
                    context.targetEntity()
            );
        }

        if (!context.isGolden()) {
            return;
        }

        for (AbilityLoader.GoldenBonusData goldenBonus : context.data().getGoldenBonuses(AbilityLoader.GoldenBonusScope.OPPONENT)) {
            EffectApplierRegistry.getValidated(goldenBonus.targetId()).apply(
                    context.attackerState(),
                    context.attacker(),
                    attackingFigure,
                    context.data(),
                    context.effectiveManaCost(),
                    goldenBonus.params(),
                    context.targetEntity()
            );
        }
    }

    private static void applyVanillaHitFeedback(DamageApplicationContext context, int instantDamage) {
        if (instantDamage <= 0) {
            return;
        }

        context.targetEntity().hurt(context.attacker().damageSources().mobAttack(context.attacker()), 0.01f);
        if (context.targetEntity() instanceof Player) {
            context.targetEntity().setHealth(context.targetEntity().getMaxHealth());
        }
    }

    @Nullable
    private static IBattleState.CombatMutationSource combatSource(@Nullable IBattleState attackerState, @Nullable LivingEntity attacker,
                                                                  @Nullable BattleFigure attackerFigure) {
        if (attackerState == null || attacker == null || attackerFigure == null) {
            return null;
        }
        return new IBattleState.CombatMutationSource(attackerState, attacker, attackerFigure);
    }

    private record DamageApplicationContext(@Nullable IBattleState attackerState, LivingEntity attacker, LivingEntity targetEntity,
                                            IBattleState victimState, BattleFigure victimFigure, DamageResult result,
                                            @Nullable AbilityLoader.AbilityData data, int effectiveManaCost, boolean isGolden,
                                            boolean isReflected, boolean isPetFire, int accessoryReactionId,
                                            boolean canTriggerBirdarang) {
    }
}
