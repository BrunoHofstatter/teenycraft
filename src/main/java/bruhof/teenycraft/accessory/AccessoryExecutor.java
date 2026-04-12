package bruhof.teenycraft.accessory;

import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.damage.DistributionHelper;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class AccessoryExecutor {
    private static final Map<IBattleState, Integer> LAST_BIRDARANG_REACTION = Collections.synchronizedMap(new WeakHashMap<>());

    public static void onActivated(IBattleState ownerState, ServerPlayer owner, AccessorySpec spec, Component accessoryName) {
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("§6Accessory Activated: ").append(accessoryName));
        }
        if (spec.getType() == AccessorySpec.Type.TITANS_COIN) {
            syncTitansCoin(ownerState, spec);
        }
    }

    public static void onTick(IBattleState ownerState, ServerPlayer owner, AccessorySpec spec, int activeTicks) {
        if (owner == null || spec == null) return;

        if ("bat_signal".equals(spec.getId())) {
            if (activeTicks == spec.getIntervalTicks()) {
                applyPeriodicDamage(ownerState, owner, spec);
            }
            return;
        }

        if ("krypto_the_superdog".equals(spec.getId())) {
            if (shouldPulse(spec, activeTicks)) {
                applyKryptoEffect(ownerState, owner);
            }
            return;
        }

        switch (spec.getType()) {
            case TITANS_COIN -> syncTitansCoin(ownerState, spec);
            case PERIODIC_EFFECT -> {
                if (shouldPulse(spec, activeTicks)) {
                    applyPeriodicEffect(ownerState, owner, spec);
                }
            }
            case PERIODIC_DAMAGE -> {
                if (shouldPulse(spec, activeTicks)) {
                    applyPeriodicDamage(ownerState, owner, spec);
                }
            }
        }
    }

    public static void onActiveFigureChanged(IBattleState ownerState, AccessorySpec spec) {
        if (ownerState == null || spec == null) return;

        if (spec.getType() == AccessorySpec.Type.TITANS_COIN) {
            syncTitansCoin(ownerState, spec);
        }
    }

    public static void onDeactivated(IBattleState ownerState, ServerPlayer owner, AccessorySpec spec, Component accessoryName) {
        if (spec != null && spec.getType() == AccessorySpec.Type.TITANS_COIN) {
            clearTitansCoin(ownerState);
        }
        if (owner != null && accessoryName != null) {
            owner.sendSystemMessage(Component.literal("§7Accessory Deactivated: ").append(accessoryName));
        }
    }

    public static int onIncomingDamage(IBattleState victimState, LivingEntity victimEntity, BattleFigure victimFigure, LivingEntity attackerEntity,
                                       int incomingDamage, int accessoryReactionId, boolean canTriggerBirdarang) {
        if (victimState == null || victimEntity == null || victimFigure == null || incomingDamage <= 0 || !victimState.isAccessoryActive()) {
            return incomingDamage;
        }

        String accessoryId = victimState.getActiveAccessoryId();
        if (accessoryId == null) {
            return incomingDamage;
        }

        int adjustedDamage = incomingDamage;

        if ("supermans_underpants".equals(accessoryId)) {
            victimState.addBatteryCharge(-(incomingDamage * bruhof.teenycraft.TeenyBalance.ACCESSORY_SUPERMANS_UNDERPANTS_BATTERY_DRAIN_MULT));
            if (victimState.getBatteryCharge() <= 0) {
                victimState.forceDeactivateAccessory();
            }
            adjustedDamage = 0;
        }

        if (canTriggerBirdarang && "birdarang".equals(accessoryId) && victimFigure == victimState.getActiveFigure() && shouldTriggerBirdarang(victimState, accessoryReactionId)) {
            triggerBirdarangRetaliation(victimState, victimEntity, attackerEntity);
        }

        return adjustedDamage;
    }

    private static boolean shouldPulse(AccessorySpec spec, int activeTicks) {
        return spec.getIntervalTicks() > 0 && activeTicks > 0 && activeTicks % spec.getIntervalTicks() == 0;
    }

    private static void applyPeriodicEffect(IBattleState ownerState, ServerPlayer owner, AccessorySpec spec) {
        if (spec.getTarget() == AccessorySpec.Target.SELF) {
            ownerState.applyEffect(spec.getEffectId(), spec.getEffectDurationTicks(), spec.getEffectMagnitude());
            return;
        }

        LivingEntity opponent = findNearestOpponent(owner);
        if (opponent == null) return;

        IBattleState opponentState = opponent.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        if (opponentState == null || !opponentState.isBattling()) return;
        if ("waffle".equals(spec.getEffectId())) {
            applyWaffleEffect(opponentState, spec);
            return;
        }
        opponentState.applyEffect(spec.getEffectId(), spec.getEffectDurationTicks(), spec.getEffectMagnitude());
    }

    private static void applyPeriodicDamage(IBattleState ownerState, ServerPlayer owner, AccessorySpec spec) {
        LivingEntity opponent = findNearestOpponent(owner);
        if (opponent == null) return;

        IBattleState opponentState = opponent.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        if (opponentState == null || !opponentState.isBattling()) return;

        int[] hitSplits = DistributionHelper.split(spec.getDamage(), Math.max(1, spec.getHitCount()));
        for (int hitDamage : hitSplits) {
            if (spec.isGroupDamage()) {
                List<BattleFigure> alive = new ArrayList<>();
                for (BattleFigure figure : opponentState.getTeam()) {
                    if (figure.getCurrentHp() > 0) {
                        alive.add(figure);
                    }
                }
                if (alive.isEmpty()) return;

                int[] groupSplits = DistributionHelper.split(hitDamage, alive.size());
                int reactionId = AbilityExecutor.nextAccessoryReactionId();
                for (int i = 0; i < alive.size(); i++) {
                    DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(groupSplits[i], 1, false, false);
                    AbilityExecutor.applyDamageToFigure(ownerState, owner, opponent, opponentState, alive.get(i), result, null, 0, false, false, false, reactionId, true);
                }
            } else {
                BattleFigure victim = opponentState.getActiveFigure();
                if (victim == null) return;
                DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(hitDamage, 1, false, false);
                AbilityExecutor.applyDamageToFigure(ownerState, owner, opponent, opponentState, victim, result, null, 0, false, false, false, AbilityExecutor.nextAccessoryReactionId(), true);
            }
        }
    }

    private static void syncTitansCoin(IBattleState ownerState, AccessorySpec spec) {
        List<BattleFigure> team = ownerState.getTeam();
        for (BattleFigure figure : team) {
            int bonus = Math.round(figure.getBaseMaxHp() * spec.getMaxHpBonusPct());
            figure.setAccessoryMaxHpBonus(bonus);
        }
    }

    private static void applyWaffleEffect(IBattleState targetState, AccessorySpec spec) {
        targetState.removeEffect("waffle");
        int blockedSlot = (int) (Math.random() * 3);
        targetState.applyEffect("waffle", spec.getEffectDurationTicks(), blockedSlot);
    }

    private static void applyKryptoEffect(IBattleState ownerState, ServerPlayer owner) {
        int roll = (int) (Math.random() * 3);
        switch (roll) {
            case 0 -> ownerState.applyEffect("power_up", -1, bruhof.teenycraft.TeenyBalance.ACCESSORY_KRYPTO_POWER_UP);
            case 1 -> ownerState.applyEffect("heal", 0, bruhof.teenycraft.TeenyBalance.ACCESSORY_KRYPTO_HEAL);
            default -> {
                ownerState.spawnTofu(bruhof.teenycraft.TeenyBalance.ACCESSORY_KRYPTO_TOFU_POWER);
                ownerState.refreshPlayerInventory(owner);
            }
        }
    }

    private static boolean shouldTriggerBirdarang(IBattleState victimState, int accessoryReactionId) {
        Integer lastReaction = LAST_BIRDARANG_REACTION.get(victimState);
        if (lastReaction != null && lastReaction == accessoryReactionId) {
            return false;
        }
        LAST_BIRDARANG_REACTION.put(victimState, accessoryReactionId);
        return true;
    }

    private static void triggerBirdarangRetaliation(IBattleState victimState, LivingEntity victimEntity, LivingEntity attackerEntity) {
        if (attackerEntity == null) return;

        IBattleState attackerState = attackerEntity.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        if (attackerState == null || !attackerState.isBattling()) return;

        BattleFigure attackerFigure = attackerState.getActiveFigure();
        if (attackerFigure == null || attackerFigure.getCurrentHp() <= 0) return;

        DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(bruhof.teenycraft.TeenyBalance.ACCESSORY_BIRDARANG_DAMAGE, 1, false, false);
        AbilityExecutor.applyDamageToFigure(victimState, victimEntity, attackerEntity, attackerState, attackerFigure, result, null, 0, false, false, false,
                AbilityExecutor.nextAccessoryReactionId(), false);
    }

    private static void clearTitansCoin(IBattleState ownerState) {
        for (BattleFigure figure : ownerState.getTeam()) {
            figure.setAccessoryMaxHpBonus(0);
        }
    }

    private static LivingEntity findNearestOpponent(LivingEntity owner) {
        return owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(64),
                entity -> entity != owner && entity.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
                .stream()
                .filter(entity -> entity.getCapability(BattleStateProvider.BATTLE_STATE).map(IBattleState::isBattling).orElse(false))
                .findFirst()
                .orElse(null);
    }
}
