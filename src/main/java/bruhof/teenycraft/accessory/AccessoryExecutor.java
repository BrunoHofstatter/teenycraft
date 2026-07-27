package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.battle.damage.DistributionHelper;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class AccessoryExecutor {
    private static final Map<IBattleState, Integer> LAST_BIRDARANG_REACTION = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<IBattleState, ActivationRuntime> ACTIVATION_RUNTIME = Collections.synchronizedMap(new WeakHashMap<>());

    public static void onActivated(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec, Component accessoryName) {
        BattleFigure active = ownerState.getActiveFigure();
        ACTIVATION_RUNTIME.put(ownerState, new ActivationRuntime(
                ownerState.getActiveFigureIndex(),
                ownerState.getCurrentMana() <= TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_4_START_MANA_MAX,
                active != null && active.getCurrentHp() <= TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_START_HP_MAX,
                active != null ? active.getCurrentHp() : 0));
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("\u00A76Accessory Activated: ").append(accessoryName));
        }
        if (spec.type() == AccessorySpec.Type.TITANS_COIN) {
            int totalBonus = syncTitansCoin(ownerState, spec);
            record(ownerState, owner, AccessoryContribution.amount(
                    spec.id(), AccessoryContribution.Type.MAX_HP_GRANTED, totalBonus));
            if (spec.tier() >= 5) {
                healTitansCoinTeam(ownerState, owner, spec.id());
            }
            return;
        }
        if (spec.tier() >= 5 && "green_lantern_battery".equals(spec.id())) {
            grantGreenLanternMasteryMana(ownerState, owner, spec);
        }
        if (triggersImmediately(spec)) {
            applyPulse(ownerState, owner, spec);
        }
    }

    public static void onTick(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec, int activeTicks) {
        if (owner == null || spec == null) return;

        updateActivationRuntime(ownerState, spec);

        if ("bat_signal".equals(spec.id())) {
            if (activeTicks == spec.intervalTicks()) {
                applyPeriodicDamage(ownerState, owner, spec);
            }
            return;
        }

        switch (spec.type()) {
            case TITANS_COIN -> syncTitansCoin(ownerState, spec);
            case PERIODIC_EFFECT, PERIODIC_DAMAGE -> {
                if (shouldPulse(spec, activeTicks)) {
                    applyPulse(ownerState, owner, spec);
                }
            }
        }
    }

    public static void onTick(IBattleState ownerState, Player owner, AccessorySpec spec, int activeTicks) {
        onTick(ownerState, owner, AccessoryTierResolver.resolve(spec, 1), activeTicks);
    }

    public static void onActiveFigureChanged(IBattleState ownerState, ResolvedAccessorySpec spec) {
        if (ownerState == null || spec == null) return;
        if (spec.type() == AccessorySpec.Type.TITANS_COIN) {
            syncTitansCoin(ownerState, spec);
        }
    }

    public static void onDeactivated(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec, Component accessoryName) {
        if (spec != null && spec.type() == AccessorySpec.Type.TITANS_COIN) {
            boolean allSurviveAtOne = ownerState.getTeam().size() >= TeenyBalance.ACCESSORY_TITANS_COIN_TIER_5_FIGURES_AT_ONE_HP
                    && ownerState.getTeam().stream().allMatch(figure -> {
                        int bonus = figure.getMaxHp() - figure.getBaseMaxHp();
                        return figure.getCurrentHp() > 0 && bonus > 0 && figure.getCurrentHp() <= bonus;
                    });
            clearTitansCoin(ownerState);
            if (allSurviveAtOne && ownerState.getTeam().stream().allMatch(figure -> figure.getCurrentHp() == 1)) {
                record(ownerState, owner, AccessoryContribution.count(
                        spec.id(), AccessoryContribution.Type.TITANS_COIN_ONE_HP_SURVIVAL));
            }
        }
        ACTIVATION_RUNTIME.remove(ownerState);
        if (owner != null && accessoryName != null) {
            owner.sendSystemMessage(Component.literal("\u00A77Accessory Deactivated: ").append(accessoryName));
        }
    }

    public static int onIncomingDamage(IBattleState victimState, LivingEntity victimEntity, BattleFigure victimFigure,
                                       LivingEntity attackerEntity, int incomingDamage, int accessoryReactionId,
                                       boolean canTriggerBirdarang) {
        if (victimState == null || victimEntity == null || victimFigure == null || incomingDamage <= 0) {
            return incomingDamage;
        }

        String activeAccessoryId = victimState.isAccessoryActive() ? victimState.getActiveAccessoryId() : null;
        AccessorySpec baseSpec = activeAccessoryId != null ? AccessoryRegistry.get(activeAccessoryId) : null;
        ResolvedAccessorySpec spec = baseSpec != null
                ? AccessoryTierResolver.resolve(baseSpec, victimState.getActiveAccessoryTier())
                : null;
        int adjustedDamage = incomingDamage;

        if ("supermans_underpants".equals(activeAccessoryId)) {
            record(victimState, victimEntity, AccessoryContribution.amount(
                    activeAccessoryId, AccessoryContribution.Type.DAMAGE_BLOCKED, incomingDamage));
            if (incomingDamage >= victimFigure.getCurrentHp()) {
                record(victimState, victimEntity, AccessoryContribution.count(
                        activeAccessoryId, AccessoryContribution.Type.FATAL_HIT_BLOCKED));
            }
            victimState.addBatteryCharge(-(incomingDamage * TeenyBalance.ACCESSORY_SUPERMANS_UNDERPANTS_BATTERY_DRAIN_MULT));
            if (victimState.getBatteryCharge() <= 0) {
                victimState.forceDeactivateAccessory();
            }
            adjustedDamage = 0;
        }

        boolean activeBirdarang = spec != null && "birdarang".equals(activeAccessoryId);
        boolean passiveMasteredBirdarang = !victimState.isAccessoryActive()
                && "birdarang".equals(victimState.getEquippedAccessoryId())
                && victimEntity instanceof Player player
                && AccessoryTierResolver.getTier(player, "birdarang") >= 5;
        if (canTriggerBirdarang && (activeBirdarang || passiveMasteredBirdarang)
                && victimFigure == victimState.getActiveFigure()
                && shouldTriggerBirdarang(victimState, accessoryReactionId)) {
            int damage = activeBirdarang ? spec.damage() : TeenyBalance.ACCESSORY_BIRDARANG_TIER_5_PASSIVE_DAMAGE;
            triggerBirdarangRetaliation(victimState, victimEntity, attackerEntity, damage);
        }

        return adjustedDamage;
    }

    public static void onAbilityDamageApplied(IBattleState attackerState, LivingEntity attacker,
                                              IBattleState victimState, BattleFigure victimFigure,
                                              DamagePipeline.DamageResult result, int actualDamage) {
        if (attackerState == null || victimState == null || victimFigure == null || result == null || actualDamage <= 0) {
            return;
        }
        int targetIndex = victimState.getTeam().indexOf(victimFigure);
        int preMitigationDamage = Math.max(1, result.getTotalDamagePerHit());
        result.accessoryBonusDamage.forEach((accessoryId, rawBonus) -> {
            int actualBonus = Math.min(actualDamage,
                    Math.max(0, Math.round(actualDamage * (rawBonus / (float) preMitigationDamage))));
            if (actualBonus > 0) {
                record(attackerState, attacker, new AccessoryContribution(accessoryId,
                        AccessoryContribution.Type.POWER_UP_BONUS_DAMAGE, actualBonus, targetIndex, ""));
            }
            if ("red_lantern_battery".equals(accessoryId) && actualDamage >= victimFigure.getCurrentHp()) {
                record(attackerState, attacker, new AccessoryContribution(accessoryId,
                        AccessoryContribution.Type.FIGURE_DEFEATED, 1, targetIndex, "power_up_attack"));
            }
        });

        int kryptoniteMagnitude = victimState.getEffectAccessoryMagnitude("defense_down", "kryptonite");
        if (kryptoniteMagnitude > 0) {
            int totalDefense = victimFigure.getEffectiveStat(bruhof.teenycraft.battle.StatType.DEFENSE_PERCENT, victimState);
            float withKryptonite = Math.max(0.0f, 1.0f - totalDefense / 100.0f);
            float withoutKryptonite = Math.max(0.0f, 1.0f - (totalDefense + kryptoniteMagnitude) / 100.0f);
            int actualBonus = withKryptonite <= 0.0f ? 0 : Math.max(0,
                    Math.round(actualDamage * ((withKryptonite - withoutKryptonite) / withKryptonite)));
            if (actualBonus > 0) {
                record(attackerState, attacker, new AccessoryContribution("kryptonite",
                        AccessoryContribution.Type.DEFENSE_DOWN_BONUS_DAMAGE, actualBonus, targetIndex, ""));
            }
        }
    }

    private static boolean triggersImmediately(ResolvedAccessorySpec spec) {
        return spec.intervalTicks() > 0 && !"bat_signal".equals(spec.id());
    }

    private static boolean shouldPulse(ResolvedAccessorySpec spec, int activeTicks) {
        return spec.intervalTicks() > 0 && activeTicks > 0 && activeTicks % spec.intervalTicks() == 0;
    }

    private static void applyPulse(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec) {
        record(ownerState, owner, AccessoryContribution.count(spec.id(), AccessoryContribution.Type.PULSE));
        if ("krypto_the_superdog".equals(spec.id())) {
            applyKryptoEffect(ownerState, owner, spec);
        } else if (spec.type() == AccessorySpec.Type.PERIODIC_EFFECT) {
            applyPeriodicEffect(ownerState, owner, spec);
        } else if (spec.type() == AccessorySpec.Type.PERIODIC_DAMAGE) {
            applyPeriodicDamage(ownerState, owner, spec);
        }
    }

    private static void applyPeriodicEffect(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec) {
        if (spec.target() == AccessorySpec.Target.SELF) {
            BattleFigure targetFigure = ownerState.getActiveFigure();
            int hpBefore = targetFigure != null ? targetFigure.getCurrentHp() : 0;
            float manaBefore = ownerState.getCurrentMana();
            int magnitudeBefore = ownerState.getEffectMagnitude(spec.effectId());
            boolean masteredVioletHeal = spec.tier() >= 5 && "violet_lantern_battery".equals(spec.id())
                    && "heal".equals(spec.effectId());
            boolean stored;
            if (masteredVioletHeal) {
                stored = false;
                if (!ownerState.hasEffect("kiss") && targetFigure != null) {
                    ownerState.applyResolvedAccessoryOverheal(targetFigure, spec.effectMagnitude(),
                            TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_OVERHEAL_PCT);
                }
            } else {
                stored = ownerState.applyAccessoryEffect(
                        spec.id(), spec.effectId(), spec.effectDurationTicks(), spec.effectMagnitude());
            }
            int actualHeal = targetFigure != null ? Math.max(0, targetFigure.getCurrentHp() - hpBefore) : 0;
            int actualMana = Math.max(0, Math.round(ownerState.getCurrentMana() - manaBefore));
            boolean applied = switch (spec.effectId()) {
                case "heal" -> actualHeal > 0;
                case "bar_fill" -> actualMana > 0;
                default -> stored;
            };
            if (!applied) {
                return;
            }
            int targetIndex = ownerState.getActiveFigureIndex();
            record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.EFFECT_APPLIED,
                    1, targetIndex, spec.effectId()));
            if ("heal".equals(spec.effectId()) && targetFigure != null) {
                record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.HEALING_DONE,
                        actualHeal, targetIndex, spec.effectId()));
                recordVioletLowHeal(ownerState, owner, spec, targetFigure, actualHeal);
            } else if ("bar_fill".equals(spec.effectId())) {
                record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.MANA_GRANTED,
                        actualMana, targetIndex, spec.effectId()));
                recordGreenLowFill(ownerState, owner, spec);
            } else if ("power_up".equals(spec.effectId())) {
                record(ownerState, owner, AccessoryContribution.amount(spec.id(), AccessoryContribution.Type.POWER_UP_GRANTED,
                        Math.max(0, ownerState.getEffectMagnitude(spec.effectId()) - magnitudeBefore)));
            }
            return;
        }

        IBattleState opponentState = ownerState.getOpponentBattleState();
        if (opponentState == null) return;
        if ("waffle".equals(spec.effectId())) {
            int blockedSlot = applyWaffleEffect(opponentState, spec);
            if (blockedSlot >= 0) {
                record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.WAFFLE_APPLIED,
                        1, opponentState.getActiveFigureIndex(), "slot:" + blockedSlot));
                applyMasteredSecondaryWaffle(ownerState, opponentState, spec, blockedSlot);
            }
            return;
        }
        float manaBefore = opponentState.getCurrentMana();
        boolean blocked = opponentState.hasEffect("cleanse_immunity");
        boolean stored = opponentState.applyAccessoryEffect(
                spec.id(), spec.effectId(), spec.effectDurationTicks(), spec.effectMagnitude());
        boolean applied = "freeze".equals(spec.effectId()) ? !blocked : stored;
        if (!applied) {
            return;
        }
        if (spec.tier() >= 5 && "ravens_spellbook".equals(spec.id())) {
            int slowPercent = Math.round((1.0f - TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_5_SPEED_MULT) * 100.0f);
            opponentState.applyAccessoryEffect(spec.id(), "ravens_slow", spec.effectDurationTicks(), slowPercent);
        }
        if (spec.tier() >= 5 && "kryptonite".equals(spec.id())) {
            int penaltyPercent = Math.round((1.0f - TeenyBalance.ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT) * 100.0f);
            opponentState.applyAccessoryEffect(spec.id(), "kryptonite_mastery",
                    spec.effectDurationTicks(), penaltyPercent);
        }
        int targetIndex = opponentState.getActiveFigureIndex();
        record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.EFFECT_APPLIED,
                1, targetIndex, spec.effectId()));
        if ("freeze".equals(spec.effectId())) {
            record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.MANA_BURNED,
                    Math.max(0, Math.round(manaBefore - opponentState.getCurrentMana())), targetIndex, "freeze"));
        }
    }

    private static void applyPeriodicDamage(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec) {
        LivingEntity opponent = ownerState.getOpponentEntity();
        if (opponent == null) return;

        IBattleState opponentState = ownerState.getOpponentBattleState();
        if (opponentState == null) return;

        float forcedCriticalMultiplier = nextMasteryCriticalMultiplier(ownerState, spec);
        int[] hitSplits = DistributionHelper.split(spec.damage(), Math.max(1, spec.hitCount()));
        for (int hitDamage : hitSplits) {
            if (spec.groupDamage()) {
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
                    configureMasteryDamage(result, spec, forcedCriticalMultiplier);
                    BattleFigure victim = alive.get(i);
                    int hpBefore = victim.getCurrentHp();
                    int actualDamage = AbilityExecutor.applyDamageToFigure(ownerState, owner, opponent, opponentState, victim, result,
                            null, 0, false, false, false, reactionId, true);
                    recordAccessoryDamage(ownerState, owner, spec.id(), opponentState, victim, hpBefore, actualDamage);
                }
            } else {
                BattleFigure victim = opponentState.getActiveFigure();
                if (victim == null) return;
                DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(hitDamage, 1, false, false);
                configureMasteryDamage(result, spec, forcedCriticalMultiplier);
                int hpBefore = victim.getCurrentHp();
                int actualDamage = AbilityExecutor.applyDamageToFigure(ownerState, owner, opponent, opponentState, victim, result,
                        null, 0, false, false, false, AbilityExecutor.nextAccessoryReactionId(), true);
                recordAccessoryDamage(ownerState, owner, spec.id(), opponentState, victim, hpBefore, actualDamage);
            }
        }
    }

    private static int syncTitansCoin(IBattleState ownerState, ResolvedAccessorySpec spec) {
        int totalBonus = 0;
        for (BattleFigure figure : ownerState.getTeam()) {
            int bonus = Math.round(figure.getBaseMaxHp() * spec.maxHpBonusPct());
            figure.setAccessoryMaxHpBonus(bonus);
            totalBonus += bonus;
        }
        return totalBonus;
    }

    private static void healTitansCoinTeam(IBattleState ownerState, Player owner, String accessoryId) {
        List<BattleFigure> team = ownerState.getTeam();
        for (int i = 0; i < team.size(); i++) {
            BattleFigure figure = team.get(i);
            if (figure.getCurrentHp() <= 0) {
                continue;
            }
            int hpBefore = figure.getCurrentHp();
            int requestedHeal = Math.round(figure.getBaseMaxHp() * TeenyBalance.ACCESSORY_TITANS_COIN_TIER_5_HEAL_PCT);
            ownerState.applyResolvedCombatFigureDelta(figure, requestedHeal);
            int actualHeal = figure.getCurrentHp() - hpBefore;
            if (actualHeal > 0) {
                record(ownerState, owner, new AccessoryContribution(accessoryId,
                        AccessoryContribution.Type.HEALING_DONE, actualHeal, i, "activation"));
            }
        }
    }

    private static void grantGreenLanternMasteryMana(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec) {
        float manaBefore = ownerState.getCurrentMana();
        ownerState.applyAccessoryEffect(spec.id(), "bar_fill", 0,
                TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_5_ACTIVATION_MANA);
        int actualMana = Math.max(0, Math.round(ownerState.getCurrentMana() - manaBefore));
        if (actualMana > 0) {
            record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.MANA_GRANTED,
                    actualMana, ownerState.getActiveFigureIndex(), "activation"));
            recordGreenLowFill(ownerState, owner, spec);
        }
    }

    private static float nextMasteryCriticalMultiplier(IBattleState ownerState, ResolvedAccessorySpec spec) {
        if (spec.tier() < 5 || !"mother_box".equals(spec.id())) {
            return 1.0f;
        }
        ActivationRuntime runtime = ACTIVATION_RUNTIME.get(ownerState);
        if (runtime == null) {
            return 1.0f;
        }
        runtime.motherBoxHitCount++;
        return masteryCriticalMultiplierForHit(spec, runtime.motherBoxHitCount);
    }

    static float masteryCriticalMultiplierForHit(ResolvedAccessorySpec spec, int hitNumber) {
        return spec.tier() >= 5 && "mother_box".equals(spec.id()) && hitNumber > 0
                && hitNumber % TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_HIT_INTERVAL == 0
                ? TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_DAMAGE_MULT
                : 1.0f;
    }

    private static void configureMasteryDamage(DamagePipeline.DamageResult result, ResolvedAccessorySpec spec,
                                                float forcedCriticalMultiplier) {
        result.forcedCriticalMultiplier = forcedCriticalMultiplier;
        if (spec.tier() >= 5 && "bat_signal".equals(spec.id())) {
            result.undodgeable = true;
        }
    }

    private static int applyWaffleEffect(IBattleState targetState, ResolvedAccessorySpec spec) {
        targetState.removeEffect("waffle");
        int blockedSlot = (int) (Math.random() * 3);
        return targetState.applyAccessoryEffect(spec.id(), "waffle", spec.effectDurationTicks(), blockedSlot)
                ? blockedSlot : -1;
    }

    private static void applyMasteredSecondaryWaffle(IBattleState ownerState, IBattleState targetState,
                                                       ResolvedAccessorySpec spec, int primarySlot) {
        if (spec.tier() < 5 || !"cyborgs_waffle_shooter".equals(spec.id())) {
            return;
        }
        ActivationRuntime runtime = ACTIVATION_RUNTIME.get(ownerState);
        if (runtime == null) {
            return;
        }
        runtime.waffleHitCount++;
        if (runtime.waffleHitCount % TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_INTERVAL != 0) {
            return;
        }

        targetState.removeEffect("waffle_secondary");
        int secondarySlot = (primarySlot + 1 + (int) (Math.random() * 2)) % 3;
        int secondaryDuration = Math.max(1, Math.round(spec.effectDurationTicks()
                * TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_DURATION_MULT));
        targetState.applyAccessoryEffect(spec.id(), "waffle_secondary", secondaryDuration, secondarySlot);
    }

    private static void applyKryptoEffect(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec) {
        int firstRoll = (int) (Math.random() * 3);
        applyKryptoOutcome(ownerState, owner, spec, firstRoll);
        if (spec.tier() >= 5 && TeenyBalance.ACCESSORY_KRYPTO_TIER_5_EFFECT_COUNT >= 2) {
            int secondRoll = (firstRoll + 1 + (int) (Math.random() * 2)) % 3;
            applyKryptoOutcome(ownerState, owner, spec, secondRoll);
        }
    }

    private static void applyKryptoOutcome(IBattleState ownerState, Player owner, ResolvedAccessorySpec spec, int roll) {
        switch (roll) {
            case 0 -> {
                int before = ownerState.getEffectMagnitude("power_up");
                if (ownerState.applyAccessoryEffect(spec.id(), "power_up", -1, spec.kryptoPowerUp())) {
                    record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED,
                            1, ownerState.getActiveFigureIndex(), "power_up"));
                    record(ownerState, owner, AccessoryContribution.amount(spec.id(), AccessoryContribution.Type.POWER_UP_GRANTED,
                            Math.max(0, ownerState.getEffectMagnitude("power_up") - before)));
                }
            }
            case 1 -> {
                BattleFigure target = ownerState.getActiveFigure();
                int before = target != null ? target.getCurrentHp() : 0;
                ownerState.applyAccessoryEffect(spec.id(), "heal", 0, spec.kryptoHeal());
                int actualHeal = target != null ? Math.max(0, target.getCurrentHp() - before) : 0;
                if (actualHeal > 0) {
                    record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED,
                            1, ownerState.getActiveFigureIndex(), "heal"));
                    record(ownerState, owner, AccessoryContribution.amount(spec.id(), AccessoryContribution.Type.HEALING_DONE,
                            actualHeal));
                }
            }
            default -> {
                ownerState.spawnTofu(spec.kryptoTofuPower());
                ownerState.refreshPlayerInventory(owner);
                record(ownerState, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED,
                        1, ownerState.getActiveFigureIndex(), "tofu"));
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

    private static void triggerBirdarangRetaliation(IBattleState victimState, LivingEntity victimEntity,
                                                     LivingEntity attackerEntity, int damage) {
        if (attackerEntity == null) return;

        IBattleState attackerState = attackerEntity.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null);
        if (attackerState == null || !attackerState.isBattling()) return;

        BattleFigure attackerFigure = attackerState.getActiveFigure();
        if (attackerFigure == null || attackerFigure.getCurrentHp() <= 0) return;

        DamagePipeline.DamageResult result = new DamagePipeline.DamageResult(damage, 1, false, false);
        int hpBefore = attackerFigure.getCurrentHp();
        int actualDamage = AbilityExecutor.applyDamageToFigure(victimState, victimEntity, attackerEntity, attackerState, attackerFigure,
                result, null, 0, false, false, false, AbilityExecutor.nextAccessoryReactionId(), false);
        if (actualDamage > 0) {
            int targetIndex = attackerState.getActiveFigureIndex();
            record(victimState, victimEntity, new AccessoryContribution("birdarang",
                    AccessoryContribution.Type.RETALIATION_HIT, 1, targetIndex, ""));
            record(victimState, victimEntity, new AccessoryContribution("birdarang",
                    AccessoryContribution.Type.DAMAGE_DEALT, actualDamage, targetIndex, ""));
            if (hpBefore > 0 && attackerFigure.getCurrentHp() <= 0) {
                record(victimState, victimEntity, new AccessoryContribution("birdarang",
                        AccessoryContribution.Type.FIGURE_DEFEATED, 1, targetIndex, ""));
            }
        }
    }

    private static void clearTitansCoin(IBattleState ownerState) {
        for (BattleFigure figure : ownerState.getTeam()) {
            figure.setAccessoryMaxHpBonus(0);
        }
    }

    private static void recordAccessoryDamage(IBattleState ownerState, Player owner, String accessoryId,
                                              IBattleState targetState, BattleFigure victim, int hpBefore,
                                              int actualDamage) {
        if (actualDamage <= 0) {
            return;
        }
        int targetIndex = targetState.getTeam().indexOf(victim);
        AccessoryProgressSnapshot before = ownerState.getAccessoryBattleProgressTracker().battleSnapshot();
        boolean firstAccessoryHit = before.totalForTarget(AccessoryContribution.Type.DAMAGE_HIT, targetIndex) == 0;
        String hitDetail = firstAccessoryHit && hpBefore == victim.getMaxHp() ? "started_full" : "";
        record(ownerState, owner, new AccessoryContribution(accessoryId,
                AccessoryContribution.Type.DAMAGE_HIT, 1, targetIndex, hitDetail));
        record(ownerState, owner, new AccessoryContribution(accessoryId,
                AccessoryContribution.Type.DAMAGE_DEALT, actualDamage, targetIndex, ""));
        if (hpBefore > 0 && victim.getCurrentHp() <= 0) {
            record(ownerState, owner, new AccessoryContribution(accessoryId,
                    AccessoryContribution.Type.FIGURE_DEFEATED, 1, targetIndex, ""));
            AccessoryProgressSnapshot after = ownerState.getAccessoryBattleProgressTracker().battleSnapshot();
            if ("mother_box".equals(accessoryId)
                    && after.targetDetails().getOrDefault(AccessoryContribution.Type.DAMAGE_HIT, Map.of())
                    .getOrDefault(targetIndex, java.util.Set.of()).contains("started_full")
                    && after.totalForTarget(AccessoryContribution.Type.DAMAGE_DEALT, targetIndex) >= victim.getMaxHp()) {
                record(ownerState, owner, new AccessoryContribution(accessoryId,
                        AccessoryContribution.Type.MOTHER_BOX_FULL_DEFEAT, 1, targetIndex, ""));
            }
        }
    }

    private static void updateActivationRuntime(IBattleState state, ResolvedAccessorySpec spec) {
        ActivationRuntime runtime = ACTIVATION_RUNTIME.get(state);
        if (runtime == null || state.getActiveFigureIndex() != runtime.figureIndex) {
            return;
        }
        if ("green_lantern_battery".equals(spec.id())
                && state.getCurrentMana() <= TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_4_START_MANA_MAX) {
            runtime.greenSawLow = true;
        }
        BattleFigure active = state.getActiveFigure();
        if ("violet_lantern_battery".equals(spec.id()) && active != null
                && active.getCurrentHp() <= TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_START_HP_MAX) {
            runtime.violetSawLow = true;
            runtime.violetStartHp = active.getCurrentHp();
            runtime.violetAccessoryHealing = 0;
        }
    }

    private static void recordGreenLowFill(IBattleState state, Player owner, ResolvedAccessorySpec spec) {
        ActivationRuntime runtime = ACTIVATION_RUNTIME.get(state);
        if (runtime != null && runtime.greenSawLow && !runtime.greenCompleted
                && state.getActiveFigureIndex() == runtime.figureIndex
                && state.getCurrentMana() >= TeenyBalance.BATTLE_MANA_MAX) {
            runtime.greenCompleted = true;
            record(state, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.MANA_FILLED_FROM_LOW,
                    1, runtime.figureIndex, ""));
        }
    }

    private static void recordVioletLowHeal(IBattleState state, Player owner, ResolvedAccessorySpec spec,
                                            BattleFigure figure, int actualHeal) {
        ActivationRuntime runtime = ACTIVATION_RUNTIME.get(state);
        if (runtime == null || !runtime.violetSawLow || runtime.violetCompleted
                || state.getActiveFigureIndex() != runtime.figureIndex) {
            return;
        }
        runtime.violetAccessoryHealing += actualHeal;
        int requiredHealing = Math.max(0, TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_TARGET_HP - runtime.violetStartHp);
        if (figure.getCurrentHp() >= TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_TARGET_HP
                && runtime.violetAccessoryHealing >= requiredHealing) {
            runtime.violetCompleted = true;
            record(state, owner, new AccessoryContribution(spec.id(), AccessoryContribution.Type.HEALED_FROM_LOW_TO_TARGET,
                    1, runtime.figureIndex, ""));
        }
    }

    private static final class ActivationRuntime {
        private final int figureIndex;
        private boolean greenSawLow;
        private boolean greenCompleted;
        private boolean violetSawLow;
        private int violetStartHp;
        private int violetAccessoryHealing;
        private boolean violetCompleted;
        private int motherBoxHitCount;
        private int waffleHitCount;

        private ActivationRuntime(int figureIndex, boolean greenSawLow, boolean violetSawLow, int violetStartHp) {
            this.figureIndex = figureIndex;
            this.greenSawLow = greenSawLow;
            this.violetSawLow = violetSawLow;
            this.violetStartHp = violetStartHp;
        }
    }

    private static void record(IBattleState state, LivingEntity owner, AccessoryContribution contribution) {
        AccessoryMilestoneService.record(state, owner instanceof Player player ? player : null, contribution);
    }
}
