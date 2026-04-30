package bruhof.teenycraft.battle.executor;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.battle.trait.TraitRegistry;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BattleAbilityExecution {
    private BattleAbilityExecution() {
    }

    public static void executeAttack(LivingEntity attacker, BattleFigure figure, int slotIndex, Entity targetEntity) {
        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        attacker.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            BattleAbilityContext context = BattleAbilityContext.create(state, attacker, figure, slotIndex);
            if (context == null || !context.isMelee() || !validateUse(context)) {
                return;
            }

            attemptImmediateCast(context, target, true, true);
        });
    }

    public static void executeAction(LivingEntity attacker, BattleFigure figure, int slotIndex) {
        attacker.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            BattleAbilityContext context = BattleAbilityContext.create(state, attacker, figure, slotIndex);
            if (context == null || context.isMelee() || !validateUse(context)) {
                return;
            }

            if (tryDetonateMineAgainstPairedOpponent(context)) {
                return;
            }

            LivingEntity target = context.isRanged() ? BattleTargeting.getConeTarget(context) : attacker;
            if (context.isRanged() && tryDetonateMineOnResolvedTarget(context, target)) {
                return;
            }

            if (context.isRanged() && target == null && !context.hasTrait("charge_up")) {
                failNoTarget(context);
                return;
            }

            if (context.isRanged() && context.data().raycastDelayTier > 0) {
                scheduleProjectile(context, target);
                return;
            }

            attemptImmediateCast(context, target, true, true);
        });
    }

    public static void executeDebugCast(IBattleState casterState, LivingEntity caster, BattleFigure figure,
                                        AbilityLoader.AbilityData data, int manaCost, boolean isGolden, LivingEntity enemy) {
        LivingEntity target = "none".equalsIgnoreCase(data.hitType) ? caster : enemy;
        if (target == null) {
            if (caster instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cCast failed: No target determined!"));
            }
            return;
        }

        BattleAbilityContext context = BattleAbilityContext.createResolved(casterState, caster, figure, -1, data, manaCost, isGolden);
        runResolvedCast(context, target, false, false, false);
    }

    public static void executeTofu(LivingEntity caster, IBattleState state) {
        if (state.getLockedSlot() != -1) {
            if (caster instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cCannot use Tofu right now!"));
            }
            return;
        }

        float virtualMana = state.getCurrentTofuMana();
        if (virtualMana <= 0) {
            return;
        }

        state.spawnTofu(0);
        if (caster instanceof ServerPlayer sp) {
            state.refreshPlayerInventory(sp);
        }

        String[] selfEffects = {"power_up", "heal", "bar_fill", "cleanse", "dance"};
        String[] opponentEffects = {"freeze", "stun", "waffle"};

        boolean selfTarget = Math.random() < (5.0 / 8.0);
        String effectId = selfTarget
                ? selfEffects[(int) (Math.random() * selfEffects.length)]
                : opponentEffects[(int) (Math.random() * opponentEffects.length)];

        float multiplier = switch (effectId) {
            case "heal" -> TeenyBalance.TOFU_HEAL_MULT;
            case "power_up" -> TeenyBalance.TOFU_POWER_UP_MULT;
            case "bar_fill" -> TeenyBalance.TOFU_BAR_FILL_MULT;
            case "dance" -> TeenyBalance.TOFU_DANCE_MULT;
            case "cleanse" -> TeenyBalance.TOFU_CLEANSE_MULT;
            case "stun" -> TeenyBalance.TOFU_STUN_MULT;
            case "freeze" -> TeenyBalance.TOFU_FREEZE_MULT;
            case "waffle" -> TeenyBalance.TOFU_WAFFLE_MULT;
            default -> 1.0f;
        };

        BattleFigure active = state.getActiveFigure();
        if (active == null) {
            return;
        }

        if (caster instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§6Â§lTOFU USED! Â§fIt was: Â§e" + effectId.toUpperCase()));
        }

        if (selfTarget) {
            EffectApplierRegistry.getValidated(effectId).apply(
                    state,
                    caster,
                    active,
                    null,
                    (int) virtualMana,
                    Collections.singletonList(multiplier),
                    caster
            );
            return;
        }

        LivingEntity target = BattleTargeting.getPairedOpponent(caster, state, 15.0);
        if (target != null) {
            EffectApplierRegistry.getValidated(effectId).apply(
                    state,
                    caster,
                    active,
                    null,
                    (int) virtualMana,
                    Collections.singletonList(multiplier),
                    target
            );
        }
    }

    public static void finishCharge(IBattleState state, LivingEntity attacker) {
        AbilityLoader.AbilityData data = state.getPendingAbility();
        int slotIndex = state.getPendingSlot();
        boolean isGolden = state.isPendingGolden();
        UUID targetUUID = state.getPendingTargetUUID();

        state.cancelCharge();

        BattleFigure figure = state.getActiveFigure();
        if (figure == null || data == null) {
            return;
        }

        LivingEntity target = resolveChargeTarget(state, attacker, figure, slotIndex, data, targetUUID, isGolden);
        if (target == null && !"none".equalsIgnoreCase(data.hitType)) {
            if (attacker instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cCharge failed: Target lost!"));
            }
            return;
        }

        BattleAbilityContext context = BattleAbilityContext.createResolved(
                state,
                attacker,
                figure,
                slotIndex,
                data,
                BattleAbilityContext.resolveActualManaCost(figure, slotIndex),
                BattleAbilityContext.resolveEffectiveManaCost(figure, slotIndex),
                isGolden
        );
        runResolvedCast(context, target, false, true, false);
    }

    public static void tickBlueChannel(IBattleState state, LivingEntity attacker) {
        if (shouldEndBlueChannel(state)) {
            endBlueChannel(state, attacker);
            return;
        }

        int ticksRemaining = state.getBlueChannelTicks();
        int interval = state.getBlueChannelInterval();
        float totalMana = state.getBlueChannelTotalMana();
        int totalTicks = state.getBlueChannelTotalTicks();

        float manaPerTick = totalMana / totalTicks;
        state.consumeMana(manaPerTick);

        int ticksActive = totalTicks - ticksRemaining;
        if (ticksActive % interval == 0) {
            resolveBlueChannelInterval(state, attacker, ticksActive, interval, manaPerTick, totalTicks);
        }

        state.decrementBlueChannelTicks();
        if (state.getBlueChannelTicks() <= 0 || state.getCurrentMana() <= 0) {
            endBlueChannel(state, attacker);
        }
    }

    public static void resolveProjectile(IBattleState state, LivingEntity caster, IBattleState.PendingProjectile projectile) {
        if (projectile.attackerFigure == null || projectile.data == null || projectile.targetUUID == null || !(caster.level() instanceof ServerLevel level)) {
            return;
        }

        Entity targetEntity = level.getEntity(projectile.targetUUID);
        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        HitResult hit = caster.level().clip(new ClipContext(
                projectile.castPosition,
                target.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            if (caster instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§c" + projectile.data.name + " hit a wall and missed!"));
            }
            return;
        }

        BattleAbilityContext context = BattleAbilityContext.createResolved(
                state,
                caster,
                projectile.attackerFigure,
                projectile.slotIndex,
                projectile.data,
                projectile.actualManaCost,
                projectile.effectiveManaCost,
                projectile.isGolden
        );
        runResolvedCast(context, target, false, true, true);
    }

    private static boolean validateUse(BattleAbilityContext context) {
        if (context.state().hasEffect("reset_lock")) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§eWaiting for reset..."));
            }
            return false;
        }

        int lockedSlot = context.state().getLockedSlot();
        if (lockedSlot != -1 && lockedSlot != context.slotIndex()) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cAction locked to slot " + (lockedSlot + 1)));
            }
            return false;
        }

        if (context.state().hasEffect("stun")
                || context.state().hasEffect("arena_launch_lock")
                || context.state().isCharging()
                || context.state().isBlueChanneling()) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cCannot attack right now!"));
            }
            return false;
        }

        if (context.state().hasEffect("waffle")) {
            int blockedSlot = context.state().getEffectMagnitude("waffle");
            if (blockedSlot == context.slotIndex()) {
                if (context.attacker() instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("Â§eSlot " + (context.slotIndex() + 1) + " is Waffled!"));
                }
                return false;
            }
        }

        if (context.state().getCurrentMana() < context.actualManaCost()) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§cNot enough Mana! Need " + context.actualManaCost()));
            }
            return false;
        }

        return true;
    }

    private static void attemptImmediateCast(BattleAbilityContext context, @Nullable LivingEntity target,
                                             boolean consumeManaNow, boolean awardBatteryOnSuccess) {
        if (blocksFlightRecast(context)) {
            return;
        }

        cancelFlightBeforeImmediateCast(context);
        if (target != null && target != context.attacker() && !BattleTargeting.isPairedOpponent(context.state(), target)) {
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Ã‚Â§cNo valid opponent target!"));
            }
            return;
        }

        if (!TraitRegistry.triggerExecutionHooks(
                context.state(),
                context.attacker(),
                context.figure(),
                context.slotIndex(),
                context.data(),
                target,
                context.isGolden()
        )) {
            return;
        }

        runResolvedCast(context, target, consumeManaNow, awardBatteryOnSuccess, false);
    }

    private static boolean blocksFlightRecast(BattleAbilityContext context) {
        if (!context.state().hasEffect("flight") || context.data().effectsOnSelf == null) {
            return false;
        }

        boolean recastsFlight = context.data().effectsOnSelf.stream().anyMatch(effect -> "flight".equals(effect.id));
        if (!recastsFlight) {
            return false;
        }

        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§cYou are already flying!"));
        }
        return true;
    }

    private static void cancelFlightBeforeImmediateCast(BattleAbilityContext context) {
        if (context.isSelfTargeted() || !context.state().hasEffect("flight")) {
            return;
        }

        context.state().removeEffect("flight");
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§eFlight cancelled by attacking!"));
        }
    }

    private static void runResolvedCast(BattleAbilityContext context, @Nullable LivingEntity target, boolean consumeManaNow,
                                        boolean awardBatteryOnSuccess, boolean skipSelfEffects) {
        DamageResult result = BattleDamageResolver.buildDamageResult(context);
        sendRawDamageDebug(context, result);
        spawnCastParticles(context);

        if (!context.isSelfTargeted()) {
            if (context.state().hasEffect("flight")) {
                context.state().removeEffect("flight");
                if (context.attacker() instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("Â§eFlight cancelled by attacking!"));
                }
            }

            context.state().triggerOnAttack(context.figure());
            int totalDamageDealt = executeHitStage(context, target, result);
            if (totalDamageDealt > 0) {
                if (awardBatteryOnSuccess) {
                    awardBatteryFromManaSpent(context.state(), context.actualManaCost());
                }
                rollTofu(context);
                TraitRegistry.triggerHitHooks(
                        context.state(),
                        context.attacker(),
                        context.figure(),
                        context.data(),
                        context.effectiveManaCost(),
                        target,
                        totalDamageDealt,
                        context.isGolden()
                );
            }
        } else if (!skipSelfEffects) {
            if (awardBatteryOnSuccess) {
                awardBatteryFromManaSpent(context.state(), context.actualManaCost());
            }
        }

        if (!skipSelfEffects) {
            applySelfEffects(context);
            if (consumeManaNow) {
                context.state().consumeMana(context.actualManaCost());
            }
            announceCast(context);
        }

        resetAttackStrength(context.attacker());
    }

    private static int executeHitStage(BattleAbilityContext context, @Nullable LivingEntity target, DamageResult result) {
        if (target == null) {
            return 0;
        }

        int totalDamageDealt = 0;
        DamageResult[] splitHits = bruhof.teenycraft.battle.damage.DamagePipeline.splitIntoHitResults(result);
        int accessoryReactionId = AbilityExecutor.nextAccessoryReactionId();

        for (DamageResult singleHit : splitHits) {
            int damagePart = singleHit.baseDamagePerHit;

            if (result.isGroupDamage) {
                totalDamageDealt += target.getCapability(BattleStateProvider.BATTLE_STATE).map(targetState -> {
                    List<BattleFigure> alive = new ArrayList<>();
                    for (BattleFigure battleFigure : targetState.getTeam()) {
                        if (battleFigure.getCurrentHp() > 0) {
                            alive.add(battleFigure);
                        }
                    }

                    int damageSum = 0;
                    if (!alive.isEmpty()) {
                        int[] groupSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(damagePart, alive.size());
                        for (int i = 0; i < alive.size(); i++) {
                            DamageResult enemyHit = new DamageResult(groupSplits[i], 1, false, result.canCrit);
                            enemyHit.classBonusEligible = singleHit.classBonusEligible;
                            enemyHit.undodgeable = singleHit.undodgeable;
                            damageSum += BattleDamageResolver.applyDamageToFigure(
                                    context.state(),
                                    context.attacker(),
                                    target,
                                    targetState,
                                    alive.get(i),
                                    enemyHit,
                                    context.data(),
                                    context.effectiveManaCost(),
                                    context.isGolden(),
                                    false,
                                    false,
                                    accessoryReactionId,
                                    true
                            );
                        }
                    }
                    return damageSum;
                }).orElse(0);
                continue;
            }

            totalDamageDealt += BattleDamageResolver.applyDamage(context, target, singleHit, false, accessoryReactionId, true);
        }

        return totalDamageDealt;
    }

    private static void castSelfOnlyTraits(BattleAbilityContext context) {
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§d[Effect] Casting Buffs..."));
        }

        for (AbilityLoader.TraitData trait : context.data().traits) {
            EffectApplierRegistry.getValidated(trait.id).apply(
                    context.state(),
                    context.attacker(),
                    context.figure(),
                    context.data(),
                    context.effectiveManaCost(),
                    trait.params,
                    context.attacker()
            );
        }
    }

    private static void applySelfEffects(BattleAbilityContext context) {
        for (AbilityLoader.EffectData effect : context.data().effectsOnSelf) {
            EffectApplierRegistry.getValidated(effect.id).apply(
                    context.state(),
                    context.attacker(),
                    context.figure(),
                    context.data(),
                    context.effectiveManaCost(),
                    effect.params,
                    context.attacker()
            );
        }

        if (!context.isGolden()) {
            return;
        }

        if (context.isSelfTargeted() && context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§6[Golden] Bonus Activated!"));
        }

        for (AbilityLoader.GoldenBonusData goldenBonus : context.data().getGoldenBonuses(AbilityLoader.GoldenBonusScope.SELF)) {
            EffectApplierRegistry.getValidated(goldenBonus.targetId()).apply(
                    context.state(),
                    context.attacker(),
                    context.figure(),
                    context.data(),
                    context.effectiveManaCost(),
                    goldenBonus.params(),
                    context.attacker()
            );
        }
    }

    private static void announceCast(BattleAbilityContext context) {
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§aUsed " + context.data().name + "!"));
        }
    }

    private static void sendRawDamageDebug(BattleAbilityContext context, DamageResult result) {
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§7[Debug] Raw Damage: " + result.baseDamagePerHit));
        }
    }

    private static void spawnCastParticles(BattleAbilityContext context) {
        if (context.data().particleId == null || context.data().particleId.isEmpty()) {
            return;
        }

        ResourceLocation particleLocation = new ResourceLocation(context.data().particleId);
        if (!ForgeRegistries.PARTICLE_TYPES.containsKey(particleLocation)) {
            return;
        }

        ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleLocation);
        if (!(particleType instanceof ParticleOptions particleOptions) || !(context.attacker().level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(
                particleOptions,
                context.attacker().getX(),
                context.attacker().getEyeY(),
                context.attacker().getZ(),
                context.data().particleCount,
                0.5,
                0.5,
                0.5,
                0.1
        );
    }

    private static boolean tryDetonateMineAgainstPairedOpponent(BattleAbilityContext context) {
        if (!context.hasOpponentEffect("remote_mine")) {
            return false;
        }

        LivingEntity target = BattleTargeting.getPairedOpponent(context.attacker(), context.state(), 20.0);
        if (target == null) {
            return false;
        }

        IBattleState targetState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        BattleTargeting.ArmedMine armedMine = BattleTargeting.findArmedMine(targetState, context.slotIndex(), context.attacker().getUUID());
        if (armedMine == null) {
            return false;
        }

        detonateMine(context, target, targetState, armedMine);
        return true;
    }

    private static boolean tryDetonateMineOnResolvedTarget(BattleAbilityContext context, @Nullable LivingEntity target) {
        if (!context.hasOpponentEffect("remote_mine") || target == null) {
            return false;
        }

        IBattleState targetState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        BattleTargeting.ArmedMine armedMine = BattleTargeting.findArmedMine(targetState, context.slotIndex(), context.attacker().getUUID());
        if (armedMine == null) {
            return false;
        }

        detonateMine(context, target, targetState, armedMine);
        return true;
    }

    private static void detonateMine(BattleAbilityContext context, LivingEntity target, @Nullable IBattleState targetState,
                                     BattleTargeting.ArmedMine armedMine) {
        context.state().consumeMana(context.actualManaCost());
        if (detonateMineDamage(context, target, targetState, armedMine.figure(), armedMine.instance()) > 0) {
            awardBatteryFromManaSpent(context.state(), context.actualManaCost());
        }
    }

    private static int detonateMineDamage(BattleAbilityContext context, LivingEntity target, @Nullable IBattleState victimState,
                                          BattleFigure mineOwnerFigure, bruhof.teenycraft.battle.effect.EffectInstance mine) {
        float maxSnapshot = mine.power;
        int stages = mine.magnitude;

        float initial = maxSnapshot * TeenyBalance.REMOTE_MINE_START_PCT;
        float pool = maxSnapshot - initial;
        float weight = pool / TeenyBalance.REMOTE_MINE_STAGES;
        int detonationDamage = Math.round(initial + (stages * weight));

        DamageResult result = new DamageResult(detonationDamage, 1, false, true);
        result.classBonusEligible = true;
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§bÂ§lDETONATING! Â§fBoom for " + detonationDamage + " damage."));
        }

        int damageDealt = BattleDamageResolver.applyDamageToFigure(
                context.state(),
                context.attacker(),
                target,
                victimState,
                mineOwnerFigure,
                result,
                null,
                0,
                false,
                false,
                false,
                AbilityExecutor.nextAccessoryReactionId(),
                true
        );
        if (mineOwnerFigure != null) {
            mineOwnerFigure.getActiveEffects().remove("remote_mine_" + context.slotIndex());
        }
        return damageDealt;
    }

    private static void scheduleProjectile(BattleAbilityContext context, @Nullable LivingEntity target) {
        if (target == null) {
            failNoTarget(context);
            return;
        }

        context.state().consumeMana(context.actualManaCost());
        double distance = context.attacker().getEyePosition().distanceTo(target.getEyePosition());
        int delayTicks = (int) (distance * TeenyBalance.getRaycastDelay(context.data().raycastDelayTier));
        if (delayTicks < 1) {
            delayTicks = 1;
        }

        Vec3 castPosition = context.attacker().getEyePosition();
        context.state().addProjectile(new IBattleState.PendingProjectile(
                context.data(),
                context.slotIndex(),
                context.isGolden(),
                context.actualManaCost(),
                context.effectiveManaCost(),
                target.getUUID(),
                castPosition,
                delayTicks,
                context.figure()
        ));

        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§eProjectile fired! (" + (delayTicks / 20.0f) + "s)"));
        }

        applySelfEffects(context);
        resetAttackStrength(context.attacker());
        announceCast(context);
    }

    private static void failNoTarget(BattleAbilityContext context) {
        context.state().consumeMana(context.actualManaCost());
        if (context.attacker() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§cNo target found!"));
        }
    }

    @Nullable
    private static LivingEntity resolveChargeTarget(IBattleState state, LivingEntity attacker, BattleFigure figure, int slotIndex,
                                                    AbilityLoader.AbilityData data, @Nullable UUID targetUUID, boolean isGolden) {
        if (TeenyBalance.CHARGE_LOCK_TARGET_ON_START && targetUUID != null && attacker.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(targetUUID);
            if (entity instanceof LivingEntity target) {
                return target;
            }
        }

        BattleAbilityContext context = BattleAbilityContext.createResolved(
                state,
                attacker,
                figure,
                slotIndex,
                data,
                BattleAbilityContext.resolveActualManaCost(figure, slotIndex),
                BattleAbilityContext.resolveEffectiveManaCost(figure, slotIndex),
                isGolden
        );

        if (context.isRanged()) {
            return BattleTargeting.getConeTarget(context);
        }
        return attacker;
    }

    private static boolean shouldEndBlueChannel(IBattleState state) {
        return state.getBlueChannelTicks() <= 0
                || state.getCurrentMana() <= 0
                || state.hasEffect("stun")
                || state.hasEffect("arena_launch_lock")
                || state.hasEffect("reset_lock");
    }

    private static void endBlueChannel(IBattleState state, LivingEntity attacker) {
        state.cancelBlueChannel();
        if (attacker instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("Â§bÂ§lCHANNEL ENDED."));
        }
    }

    private static void resolveBlueChannelInterval(IBattleState state, LivingEntity attacker, int ticksActive, int interval,
                                                   float manaPerTick, int totalTicks) {
        AbilityLoader.AbilityData data = state.getBlueChannelAbility();
        int slotIndex = state.getBlueChannelSlot();
        boolean isGolden = state.isBlueChannelGolden();
        float totalDamage = state.getBlueChannelTotalDamage();
        float totalHeal = state.getBlueChannelTotalHeal();
        BattleFigure figure = state.getActiveFigure();

        if (figure == null || data == null) {
            return;
        }

        int intervalCount = ((totalTicks - 1) / interval) + 1;
        if (intervalCount <= 0) {
            intervalCount = 1;
        }

        int currentIntervalIndex = ticksActive / interval;
        int totalCalculatedDamage = Math.round(totalDamage * TeenyBalance.BLUE_DAMAGE_MULT);
        int[] damageSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(totalCalculatedDamage, intervalCount);
        int damagePerInterval = currentIntervalIndex < damageSplits.length ? damageSplits[currentIntervalIndex] : 0;

        int totalCalculatedHeal = Math.round(totalHeal * TeenyBalance.BLUE_DAMAGE_MULT);
        int[] healSplits = bruhof.teenycraft.battle.damage.DistributionHelper.split(totalCalculatedHeal, intervalCount);
        int healPerInterval = currentIntervalIndex < healSplits.length ? healSplits[currentIntervalIndex] : 0;

        BattleAbilityContext context = BattleAbilityContext.createResolved(
                state,
                attacker,
                figure,
                slotIndex,
                data,
                0,
                isGolden
        );
        LivingEntity target = resolveBlueChannelTarget(context);
        if (target == null) {
            return;
        }

        boolean awardedBatteryThisInterval = false;
        if (damagePerInterval > 0 && target != attacker) {
            IBattleState targetState = target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
            BattleFigure targetFigure = targetState != null ? targetState.getActiveFigure() : null;
            DamageResult intervalDamage = new DamageResult(damagePerInterval, 1, false, true);
            intervalDamage.classBonusEligible = true;
            int damageDealt = BattleDamageResolver.applyDamageToFigure(
                    state,
                    attacker,
                    target,
                    targetState,
                    targetFigure,
                    intervalDamage,
                    data,
                    0,
                    isGolden,
                    false,
                    false,
                    AbilityExecutor.nextAccessoryReactionId(),
                    true
            );
            if (damageDealt > 0) {
                awardBatteryFromManaSpent(state, manaPerTick * interval);
                awardedBatteryThisInterval = true;
            }
        }

        if (healPerInterval > 0) {
            state.applyEffect("heal", 0, healPerInterval);
            if (!awardedBatteryThisInterval) {
                awardBatteryFromManaSpent(state, manaPerTick * interval);
            }
        }
    }

    private static LivingEntity resolveBlueChannelTarget(BattleAbilityContext context) {
        if (context.isRanged()) {
            return BattleTargeting.getConeTarget(context);
        }
        if (context.isMelee()) {
            return BattleTargeting.getPairedOpponent(context.attacker(), context.state(), 20.0);
        }
        return context.attacker();
    }

    private static void awardBatteryFromManaSpent(IBattleState state, float manaSpent) {
        if (manaSpent <= 0) {
            return;
        }
        state.addBatteryCharge(manaSpent * TeenyBalance.ABILITY_BATTERY_CHARGE_MULT);
    }

    private static void rollTofu(BattleAbilityContext context) {
        if (context.state().getCurrentTofuMana() > 0 || context.data().damageTier <= 0) {
            return;
        }

        float chanceMultiplier = 1.0f;
        float powerMultiplier = 1.0f;

        if (context.data().traits != null) {
            for (AbilityLoader.TraitData trait : context.data().traits) {
                if (!"tofu_chance".equals(trait.id)) {
                    continue;
                }
                chanceMultiplier *= trait.params.isEmpty() ? 1.0f : trait.params.get(0);
                if (trait.params.size() > 1) {
                    powerMultiplier *= trait.params.get(1);
                }
            }
        }

        if (context.isGolden()) {
            for (AbilityLoader.GoldenBonusData goldenBonus : context.data().getGoldenBonuses(AbilityLoader.GoldenBonusScope.TRAIT)) {
                if (!"tofu_chance".equals(goldenBonus.targetId())) {
                    continue;
                }

                if (!goldenBonus.params().isEmpty()) {
                    chanceMultiplier *= goldenBonus.params().get(0);
                }
                if (goldenBonus.params().size() > 1) {
                    powerMultiplier *= goldenBonus.params().get(1);
                }
            }
        }

        float chance = (context.actualManaCost() * TeenyBalance.TOFU_CHANCE_HIT_PERMANA) * chanceMultiplier;
        if (Math.random() * 100.0 < chance) {
            context.state().spawnTofu(TeenyBalance.TOFU_BASE_MANA * powerMultiplier);
            if (context.attacker() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("Â§6Â§lTOFU SPAWNED!"));
                context.state().refreshPlayerInventory(sp);
            }
        }
    }

    private static void resetAttackStrength(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            player.resetAttackStrengthTicker();
        }
    }
}
