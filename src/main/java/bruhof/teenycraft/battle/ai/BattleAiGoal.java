package bruhof.teenycraft.battle.ai;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.FigureClassType;
import bruhof.teenycraft.battle.executor.BattleAbilityContext;
import bruhof.teenycraft.battle.executor.BattleTargeting;
import bruhof.teenycraft.capability.BattleState;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BattleAiGoal extends Goal {
    private static final double MELEE_REACH = 2.6d;
    private static final double RANGE_BAND_PADDING = 1.0d;
    private static final Set<String> CLEANSEABLE_SELF_EFFECTS = Set.of(
            "poison",
            "shock",
            "curse",
            "defense_down",
            "power_down",
            "waffle",
            "kiss",
            "root",
            "stun",
            "freeze_movement"
    );
    private static final Set<String> SELF_HEAL_EFFECTS = Set.of("heal", "group_heal", "health_radio");
    private static final Set<String> SELF_CLEANSE_EFFECTS = Set.of("cleanse");
    private static final Set<String> SELF_BUFF_EFFECTS = Set.of(
            "power_up",
            "defense_up",
            "luck_up",
            "dance",
            "cuteness",
            "shield",
            "dodge_smoke",
            "power_radio",
            "pet_slot_1",
            "pet_slot_2",
            "flight",
            "reflect"
    );
    private static final Set<String> OPPONENT_CONTROL_EFFECTS = Set.of(
            "stun",
            "root",
            "disable",
            "freeze",
            "shock",
            "waffle",
            "kiss"
    );
    private static final Set<String> OPPONENT_DEBUFF_EFFECTS = Set.of(
            "defense_down",
            "power_down",
            "poison",
            "curse",
            "bar_deplete",
            "remote_mine"
    );

    private final EntityTeenyDummy dummy;
    private Intent intent = Intent.STALL;
    private int plannedSlot = -1;
    private int reevaluateTicks = 0;
    private int actionCooldownTicks = 0;
    private int strafeTicks = 0;
    private float strafeDirection = 1.0f;
    private int lastUsedSlot = -1;
    private int lastUsedSlotStreak = 0;
    private int swapReconsiderationTicks = 0;
    private int recentDamageTicks = 0;
    private int reflectReuseTicks = 0;
    private float lastObservedHp = -1.0f;

    public BattleAiGoal(EntityTeenyDummy dummy) {
        this.dummy = dummy;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BattleState state = getState();
        return state != null && state.isBattling();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        intent = Intent.STALL;
        plannedSlot = -1;
        reevaluateTicks = 0;
        actionCooldownTicks = 0;
        strafeTicks = 0;
        strafeDirection = 1.0f;
        lastUsedSlot = -1;
        lastUsedSlotStreak = 0;
        swapReconsiderationTicks = 0;
        recentDamageTicks = 0;
        reflectReuseTicks = 0;
        lastObservedHp = dummy.getHealth();
    }

    @Override
    public void stop() {
        intent = Intent.STALL;
        plannedSlot = -1;
        dummy.getNavigation().stop();
    }

    @Override
    public void tick() {
        BattleState state = getState();
        if (state == null || !state.isBattling()) {
            stop();
            return;
        }

        LivingEntity opponent = BattleTargeting.getPairedOpponent(dummy, state, 64.0d);
        IBattleState opponentState = state.getOpponentBattleState();
        BattleFigure active = state.getActiveFigure();
        BattleFigure enemyFigure = opponentState != null ? opponentState.getActiveFigure() : null;
        if (opponent == null || opponentState == null || active == null || enemyFigure == null) {
            stop();
            return;
        }

        if (reevaluateTicks > 0) {
            reevaluateTicks--;
        }
        if (actionCooldownTicks > 0) {
            actionCooldownTicks--;
        }
        if (strafeTicks > 0) {
            strafeTicks--;
        }
        if (swapReconsiderationTicks > 0) {
            swapReconsiderationTicks--;
        }
        if (recentDamageTicks > 0) {
            recentDamageTicks--;
        }
        if (reflectReuseTicks > 0) {
            reflectReuseTicks--;
        }
        if (lastObservedHp >= 0.0f && dummy.getHealth() < lastObservedHp) {
            recentDamageTicks = 30;
        }
        lastObservedHp = dummy.getHealth();

        BattleAiProfile profile = dummy.getAiProfile();
        if (reevaluateTicks <= 0 || shouldReevaluate(state, opponent, active, profile)) {
            evaluateNextStep(state, opponentState, opponent, active, enemyFigure, profile);
            reevaluateTicks = profile.reactionIntervalTicks();
        }

        if (hasMovementLock(state)) {
            dummy.getNavigation().stop();
            return;
        }

        faceTarget(opponent);
        handleMovement(state, opponent, profile);
        tryUsePlannedAction(state, active, opponent, profile);
    }

    private void evaluateNextStep(BattleState state,
                                  IBattleState opponentState,
                                  LivingEntity opponent,
                                  BattleFigure active,
                                  BattleFigure enemyFigure,
                                  BattleAiProfile profile) {
        if (trySwap(state, opponentState, active, enemyFigure, profile)) {
            return;
        }

        ScoredAction action = chooseAction(state, opponentState, active, enemyFigure, opponent, profile);
        if (action == null) {
            intent = Intent.STALL;
            plannedSlot = -1;
            return;
        }

        plannedSlot = action.slot();
        intent = switch (action.role()) {
            case MELEE_DAMAGE -> Intent.PRESSURE_MELEE;
            case RANGED_DAMAGE, CONTROL, DEBUFF -> Intent.PRESSURE_RANGED;
            case HEAL, CLEANSE, BUFF, UTILITY -> action.prefersDistance() ? Intent.SELF_MAINTENANCE : Intent.STALL;
        };
    }

    private boolean trySwap(BattleState state,
                            IBattleState opponentState,
                            BattleFigure active,
                            BattleFigure enemyFigure,
                            BattleAiProfile profile) {
        if (!profile.considerSwap()
                || actionCooldownTicks > 0
                || swapReconsiderationTicks > 0
                || state.getSwapCooldown() > 0
                || state.hasEffect("stun")
                || state.hasEffect("root")
                || state.hasEffect("arena_launch_lock")) {
            return false;
        }

        double currentScore = evaluateFigureFit(state, active, enemyFigure, state.getActiveFigureIndex(), profile);
        int bestIndex = -1;
        double bestScore = currentScore;
        for (int i = 0; i < state.getTeam().size(); i++) {
            if (i == state.getActiveFigureIndex()) {
                continue;
            }

            BattleFigure candidate = state.getTeam().get(i);
            if (!isSwapCandidateAvailable(candidate, i)) {
                continue;
            }

            double score = evaluateFigureFit(state, candidate, enemyFigure, i, profile);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        if (bestIndex < 0) {
            swapReconsiderationTicks = profile.swapReconsiderationTicks();
            return false;
        }

        double improvement = bestScore - currentScore;
        double threshold = profile.enablesAdvancedSwapLogic() ? 2.0d : 3.2d;
        threshold -= profile.swapBias() * 0.8d;

        float currentHpPct = getHpPct(active);
        if (currentHpPct <= (0.28f + ((1.0f - profile.riskTolerance()) * 0.18f))) {
            threshold -= 0.8d;
        }

        if (improvement < threshold) {
            swapReconsiderationTicks = profile.swapReconsiderationTicks();
            return false;
        }

        state.swapFigure(bestIndex, null);
        plannedSlot = -1;
        intent = Intent.HARD_SWAP;
        actionCooldownTicks = profile.actionCommitTicks();
        reevaluateTicks = profile.reactionIntervalTicks();
        swapReconsiderationTicks = profile.swapReconsiderationTicks();
        return true;
    }

    private double evaluateFigureFit(BattleState state,
                                     BattleFigure figure,
                                     BattleFigure enemyFigure,
                                     int figureIndex,
                                     BattleAiProfile profile) {
        double score = 0.0d;
        float hpPct = getHpPct(figure);
        score += hpPct * 4.0d;

        score += scoreSwapClassMatchup(figure.getFigureClass(), enemyFigure.getFigureClass(), profile);

        if (figureIndex == state.getActiveFigureIndex()) {
            score += 0.5d;
        }

        if (!profile.enablesAdvancedSwapLogic() && hpPct > 0.45f) {
            score += 0.4d;
        }

        score += countUsableDamageTools(figure) * 0.9d;
        return score;
    }

    private int countUsableDamageTools(BattleFigure figure) {
        int count = 0;
        for (int slot = 0; slot < 3; slot++) {
            AbilityLoader.AbilityData data = BattleAbilityContext.getAbilityData(figure, slot);
            if (data != null && data.damageTier > 0) {
                count++;
            }
        }
        return count;
    }

    private boolean isSwapCandidateAvailable(BattleFigure candidate, int index) {
        return candidate.getCurrentHp() > 0
                && !candidate.getActiveEffects().containsKey("disable_" + index);
    }

    private ScoredAction chooseAction(BattleState state,
                                      IBattleState opponentState,
                                      BattleFigure active,
                                      BattleFigure enemyFigure,
                                      LivingEntity opponent,
                                      BattleAiProfile profile) {
        if (state.hasEffect("stun")
                || state.hasEffect("reset_lock")
                || state.hasEffect("arena_launch_lock")
                || state.isCharging()
                || state.isBlueChanneling()) {
            return null;
        }

        List<ScoredAction> actions = new ArrayList<>();
        for (int slot = 0; slot < 3; slot++) {
            BattleAbilityContext context = BattleAbilityContext.create(state, dummy, active, slot);
            if (context == null || !isActionUsable(state, active, context)) {
                continue;
            }

            AbilityRole role = classifyRole(context);
            double score = scoreAction(state, opponentState, active, enemyFigure, opponent, context, role, profile);
            if (slot == lastUsedSlot) {
                score -= TeenyBalance.AI_REPEAT_SLOT_SOFT_PENALTY * Math.max(1, lastUsedSlotStreak);
            }
            if (score > 0.0d) {
                actions.add(new ScoredAction(slot, role, score, prefersDistance(role)));
            }
        }

        if (actions.isEmpty()) {
            return null;
        }

        ScoredAction meleeOverride = selectMeleeOverride(actions, state, active, opponent, profile);
        if (meleeOverride != null) {
            return meleeOverride;
        }

        ScoredAction nearReadyMelee = selectNearReadyMeleeOverride(state, active, opponent, profile);
        if (nearReadyMelee != null) {
            return nearReadyMelee;
        }

        double max = actions.stream().mapToDouble(ScoredAction::score).max().orElse(0.0d);
        List<ScoredAction> shortlist = new ArrayList<>();
        double minScore = max - profile.choiceWindow();
        for (ScoredAction action : actions) {
            if (action.score() >= minScore) {
                shortlist.add(action);
            }
        }

        if (lastUsedSlot >= 0 && lastUsedSlotStreak >= TeenyBalance.AI_MAX_SAME_SLOT_STREAK) {
            List<ScoredAction> alternatives = new ArrayList<>();
            for (ScoredAction action : actions) {
                if (action.slot() != lastUsedSlot) {
                    alternatives.add(action);
                }
            }
            if (!alternatives.isEmpty()) {
                shortlist = alternatives;
            }
        }

        if (shortlist.isEmpty()) {
            return null;
        }

        double weightTotal = 0.0d;
        for (ScoredAction action : shortlist) {
            weightTotal += Math.max(0.1d, action.score() - minScore + 0.1d);
        }

        double roll = dummy.getRandom().nextDouble() * weightTotal;
        for (ScoredAction action : shortlist) {
            roll -= Math.max(0.1d, action.score() - minScore + 0.1d);
            if (roll <= 0.0d) {
                return action;
            }
        }
        return shortlist.get(shortlist.size() - 1);
    }

    private boolean isActionUsable(BattleState state, BattleFigure active, BattleAbilityContext context) {
        if (active.getCooldown(context.slotIndex()) > 0) {
            return false;
        }

        int lockedSlot = state.getLockedSlot();
        if (lockedSlot != -1 && lockedSlot != context.slotIndex()) {
            return false;
        }

        if (state.hasEffect("waffle") && state.getEffectMagnitude("waffle") == context.slotIndex()) {
            return false;
        }

        return state.getCurrentMana() >= context.actualManaCost();
    }

    private AbilityRole classifyRole(BattleAbilityContext context) {
        if (context.isMelee()) {
            return AbilityRole.MELEE_DAMAGE;
        }
        if (context.isRanged() && context.data().damageTier > 0) {
            if (containsAny(context.data().effectsOnOpponent, OPPONENT_CONTROL_EFFECTS)) {
                return AbilityRole.CONTROL;
            }
            if (containsAny(context.data().effectsOnOpponent, OPPONENT_DEBUFF_EFFECTS)) {
                return AbilityRole.DEBUFF;
            }
            return AbilityRole.RANGED_DAMAGE;
        }
        if (containsAny(context.data().effectsOnSelf, SELF_HEAL_EFFECTS)) {
            return AbilityRole.HEAL;
        }
        if (containsAny(context.data().effectsOnSelf, SELF_CLEANSE_EFFECTS)) {
            return AbilityRole.CLEANSE;
        }
        if (containsAny(context.data().effectsOnSelf, SELF_BUFF_EFFECTS)) {
            return AbilityRole.BUFF;
        }
        if (containsAny(context.data().effectsOnOpponent, OPPONENT_CONTROL_EFFECTS)) {
            return AbilityRole.CONTROL;
        }
        if (containsAny(context.data().effectsOnOpponent, OPPONENT_DEBUFF_EFFECTS)) {
            return AbilityRole.DEBUFF;
        }
        return AbilityRole.UTILITY;
    }

    private boolean containsAny(List<AbilityLoader.EffectData> effects, Set<String> ids) {
        if (effects == null) {
            return false;
        }
        for (AbilityLoader.EffectData effect : effects) {
            if (ids.contains(effect.id)) {
                return true;
            }
        }
        return false;
    }

    private double scoreAction(BattleState state,
                               IBattleState opponentState,
                               BattleFigure active,
                               BattleFigure enemyFigure,
                               LivingEntity opponent,
                               BattleAbilityContext context,
                               AbilityRole role,
                               BattleAiProfile profile) {
        double distance = dummy.distanceTo(opponent);
        float selfHpPct = getHpPct(active);
        float enemyHpPct = getHpPct(enemyFigure);
        boolean pressured = isPressured(opponent);
        boolean highMana = isHighMana(state);
        boolean fullMana = state.getCurrentMana() >= (TeenyBalance.BATTLE_MANA_MAX * 0.97f);
        PureEffectFamily pureEffectFamily = resolvePureEffectFamily(context);
        double score;

        if (pureEffectFamily != null) {
            score = scorePureEffectAbility(
                    state,
                    opponentState,
                    active,
                    enemyFigure,
                    opponent,
                    context,
                    pureEffectFamily,
                    profile,
                    distance,
                    selfHpPct,
                    pressured,
                    highMana,
                    fullMana
            );
        } else {
            score = switch (role) {
                case MELEE_DAMAGE -> 4.2d + (profile.aggression() * 2.5d);
                case RANGED_DAMAGE -> 4.0d + (profile.aggression() * 2.0d);
                case HEAL -> scoreHeal(state, active, profile);
                case CLEANSE -> scoreCleanse(state, profile);
                case BUFF -> scoreBuff(state, selfHpPct, profile);
                case CONTROL -> 3.3d + ((1.0f - enemyHpPct) * 1.5d);
                case DEBUFF -> 3.0d + ((1.0f - enemyHpPct) * 1.2d);
                case UTILITY -> 1.4d;
            };

            if (role == AbilityRole.MELEE_DAMAGE) {
                score += scoreClassMatchup(active.getFigureClass(), enemyFigure.getFigureClass(), 1.2d);
                if (distance <= MELEE_REACH + 0.8d) {
                    score += 2.0d;
                } else if (distance >= 6.0d) {
                    score -= 1.0d;
                }
            } else if (role == AbilityRole.RANGED_DAMAGE || role == AbilityRole.CONTROL || role == AbilityRole.DEBUFF) {
                double maxRange = TeenyBalance.getRangeValue(context.data().rangeTier);
                double preferredRange = adjustPreferredRange(maxRange, context, profile);
                double distanceOffset = Math.abs(distance - preferredRange);
                score += Math.max(0.0d, 2.4d - distanceOffset);

                if (context.hasOpponentEffect("remote_mine") && state.hasActiveMine(context.slotIndex(), opponent.getUUID())) {
                    score += 2.0d;
                }

                if (profile.enablesCounterAwareness() && role != AbilityRole.RANGED_DAMAGE) {
                    if (opponentState.hasEffect("cleanse_immunity")) {
                        score -= 1.8d;
                    }
                    if (hasEnemyCleanse(opponentState)) {
                        score -= 0.8d;
                    }
                }
            }

            if (pressured) {
                score += switch (role) {
                    case MELEE_DAMAGE, RANGED_DAMAGE -> 2.4d;
                    case CONTROL -> 0.4d;
                    case HEAL -> selfHpPct <= 0.35f ? 0.8d : -2.2d;
                    case CLEANSE -> -1.8d;
                    case BUFF -> -3.0d;
                    case DEBUFF -> -1.8d;
                    case UTILITY -> -1.6d;
                };
            }

            if (highMana) {
                score += switch (role) {
                    case MELEE_DAMAGE, RANGED_DAMAGE -> 1.8d;
                    case CONTROL -> 0.7d;
                    case DEBUFF -> 0.4d;
                    case HEAL -> selfHpPct <= 0.50f ? 0.2d : -1.4d;
                    case CLEANSE -> -1.0d;
                    case BUFF -> -1.8d;
                    case UTILITY -> -1.1d;
                };
            }

            if (context.isSelfTargeted() && distance <= 3.0d && role != AbilityRole.HEAL && role != AbilityRole.CLEANSE) {
                score -= 0.6d;
            }

            if (context.actualManaCost() > (state.getCurrentMana() * (0.55f + (profile.manaDiscipline() * 0.35f)))) {
                score -= 0.7d;
            }

            if (role == AbilityRole.BUFF) {
                score += scoreSelfEffectNovelty(state, context);
            }
            if (role == AbilityRole.DEBUFF || role == AbilityRole.CONTROL) {
                score += scoreOpponentEffectNovelty(opponentState, context);
            }
        }

        if (score <= 0.0d) {
            return score;
        }

        score += scoreEffectiveManaValue(context, role, highMana, fullMana);

        if (context.hasTrait("charge_up")) {
            score -= (1.0f - profile.riskTolerance()) * 1.0d;
            if (selfHpPct <= 0.35f) {
                score -= 1.2d;
            }
        }
        if (context.hasTrait("blue") && state.getCurrentMana() >= (context.actualManaCost() * 1.5f)) {
            score += 0.8d;
        }

        return score;
    }

    private double scoreHeal(BattleState state, BattleFigure active, BattleAiProfile profile) {
        float hpPct = getHpPct(active);
        float missingPct = 1.0f - hpPct;
        if (missingPct < 0.10f || healingBlocked(state)) {
            return 0.0d;
        }
        double score = 1.2d + (missingPct * missingPct * 12.0d);
        if (hpPct <= Math.max(TeenyBalance.AI_SELF_HEAL_CRITICAL_HP_PCT,
                0.30f + ((1.0f - profile.riskTolerance()) * 0.15f))) {
            score += 1.8d;
        }
        return score;
    }

    private double scoreCleanse(BattleState state, BattleAiProfile profile) {
        if (state.hasEffect("kiss")) {
            return 0.0d;
        }
        int debuffCount = countSelfCleanseableDebuffs(state);
        if (debuffCount <= 0) {
            return 0.0d;
        }
        return (debuffCount * 2.5d) + ((1.0f - profile.riskTolerance()) * 0.5d);
    }

    private double scoreBuff(BattleState state, float selfHpPct, BattleAiProfile profile) {
        double score = 1.4d + ((1.0f - profile.aggression()) * 0.6d);
        if (state.hasEffect("kiss")) {
            return 0.0d;
        }
        if (selfHpPct < 0.30f) {
            score -= 0.8d;
        }
        return score;
    }

    private double scorePureEffectAbility(BattleState state,
                                          IBattleState opponentState,
                                          BattleFigure active,
                                          BattleFigure enemyFigure,
                                          LivingEntity opponent,
                                          BattleAbilityContext context,
                                          PureEffectFamily family,
                                          BattleAiProfile profile,
                                          double distance,
                                          float selfHpPct,
                                          boolean pressured,
                                          boolean highMana,
                                          boolean fullMana) {
        boolean far = isFar(distance, enemyFigure);
        boolean threatBand = !pressured && isThreatBand(distance, enemyFigure);

        return switch (family) {
            case HEAL -> scorePureHeal(state, selfHpPct, pressured);
            case GROUP_HEAL -> scorePureGroupHeal(state, active, selfHpPct, pressured);
            case POWER_UP -> scorePowerUp(state, pressured, far, highMana, fullMana);
            case DANCE -> scoreDance(state, pressured, far, highMana, fullMana);
            case CLEANSE -> scorePureCleanse(state);
            case BATTERY_DRAIN -> scoreBatteryDrain(state, context, selfHpPct);
            case CUTENESS -> scoreThreatBandDefense(state, "cuteness", threatBand);
            case SHIELD -> scoreThreatBandDefense(state, "shield", threatBand);
            case FLIGHT -> scoreFlight(state, pressured);
            case POISON -> scoreNoPressureOpponentEffect(opponentState, pressured, far, "poison");
            case POWER_DOWN -> scoreNoPressureOpponentEffect(opponentState, pressured, far, "power_down");
            case CURSE -> scoreNoPressureOpponentEffect(opponentState, pressured, far, "curse");
            case WAFFLE -> scoreNoPressureOpponentEffect(opponentState, pressured, far, "waffle");
            case REMOTE_MINE -> scoreRemoteMine(state, opponentState, opponent, context, pressured, far, highMana);
            case HEALTH_RADIO -> scoreHealthRadio(state, selfHpPct, pressured);
            case POWER_RADIO -> scoreNoPressureSelfSetup(state, pressured, far, highMana, "power_radio", 2.4d);
            case DEFENSE_DOWN -> scoreNoPressureOpponentEffect(opponentState, pressured, far, "defense_down");
            case DEFENSE_UP -> scoreThreatBandDefense(state, "defense_up", threatBand);
            case FREEZE -> scoreFreeze(opponentState, context);
            case DODGE_SMOKE -> scoreThreatBandDefense(state, "dodge_smoke", threatBand);
            case LUCK_UP -> scoreNoPressureSelfSetup(state, pressured, far, highMana, "luck_up", 2.0d);
            case PETS -> scorePets(state, pressured, far);
            case REFLECT -> scoreReflect(state, pressured);
        };
    }

    private double scorePureHeal(BattleState state, float selfHpPct, boolean pressured) {
        if (healingBlocked(state)) {
            return 0.0d;
        }

        float missingPct = 1.0f - selfHpPct;
        if (missingPct < 0.10f) {
            return 0.0d;
        }

        double score = 1.2d + (missingPct * missingPct * 12.0d);
        if (pressured) {
            if (selfHpPct > TeenyBalance.AI_SELF_HEAL_CRITICAL_HP_PCT) {
                score *= 0.15d;
            } else {
                score += 2.0d;
            }
        }
        return score;
    }

    private double scorePureGroupHeal(BattleState state, BattleFigure active, float selfHpPct, boolean pressured) {
        if (healingBlocked(state)) {
            return 0.0d;
        }

        double totalMissingPct = 0.0d;
        int injuredBenchCount = 0;
        boolean lowBenchAlly = false;
        for (BattleFigure figure : state.getTeam()) {
            if (figure == null || figure.getCurrentHp() <= 0) {
                continue;
            }

            float hpPct = getHpPct(figure);
            float missingPct = 1.0f - hpPct;
            totalMissingPct += missingPct;

            if (figure != active && missingPct >= 0.08f) {
                injuredBenchCount++;
            }
            if (figure != active && hpPct <= TeenyBalance.AI_GROUP_HEAL_ALLY_LOW_HP_PCT) {
                lowBenchAlly = true;
            }
        }

        if (totalMissingPct < 0.16f) {
            return 0.0d;
        }
        if (injuredBenchCount == 0 && selfHpPct > TeenyBalance.AI_GROUP_HEAL_SELF_ONLY_HP_PCT) {
            return 0.0d;
        }

        double score = totalMissingPct * 4.8d;
        if (injuredBenchCount > 0) {
            score += 0.7d + (injuredBenchCount * 0.4d);
        }
        if (lowBenchAlly) {
            score += 0.9d;
        }
        if (selfHpPct <= TeenyBalance.AI_SELF_HEAL_CRITICAL_HP_PCT) {
            score += 1.5d;
        }

        if (pressured) {
            if (selfHpPct > TeenyBalance.AI_SELF_HEAL_CRITICAL_HP_PCT) {
                return 0.0d;
            }
            score = Math.max(score, 4.5d + ((1.0f - selfHpPct) * 6.0d));
        }

        return score;
    }

    private double scorePowerUp(BattleState state, boolean pressured, boolean far, boolean highMana, boolean fullMana) {
        if (positiveSelfEffectsBlocked(state) || pressured) {
            return 0.0d;
        }

        double score = 1.0d;
        if (far) {
            score += 1.4d;
        }
        if (highMana) {
            score += 1.6d;
        }
        if (fullMana) {
            score += 1.4d;
        }
        return score;
    }

    private double scoreDance(BattleState state, boolean pressured, boolean far, boolean highMana, boolean fullMana) {
        if (positiveSelfEffectsBlocked(state) || pressured || state.hasEffect("dance")) {
            return 0.0d;
        }

        double score = 0.8d;
        if (far) {
            score += 1.0d;
        }
        if (highMana) {
            score += 1.1d;
        }
        if (fullMana) {
            score += 0.8d;
        }
        return score;
    }

    private double scorePureCleanse(BattleState state) {
        if (state.hasEffect("kiss")) {
            return 0.0d;
        }

        int debuffCount = countSelfCleanseableDebuffs(state);
        if (debuffCount <= 0) {
            return 0.0d;
        }
        return debuffCount * 2.5d;
    }

    private double scoreBatteryDrain(BattleState state, BattleAbilityContext context, float selfHpPct) {
        if (context.slotIndex() == 2 || selfHpPct <= TeenyBalance.AI_BATTERY_DRAIN_MIN_HP_PCT) {
            return 0.0d;
        }

        float manaPct = getManaPct(state);
        if (manaPct >= 0.95f) {
            return 0.0d;
        }

        double safeHpWindow = Math.max(0.0d,
                (selfHpPct - TeenyBalance.AI_BATTERY_DRAIN_MIN_HP_PCT) / (1.0d - TeenyBalance.AI_BATTERY_DRAIN_MIN_HP_PCT));
        double score = 2.2d + (safeHpWindow * 1.8d);
        if (manaPct <= 0.55f) {
            score += 1.2d;
        } else if (manaPct <= 0.75f) {
            score += 0.4d;
        }
        return score;
    }

    private double scoreThreatBandDefense(BattleState state, String effectId, boolean threatBand) {
        if (positiveSelfEffectsBlocked(state) || state.hasEffect(effectId) || !threatBand) {
            return 0.0d;
        }
        return 3.4d;
    }

    private double scoreFlight(BattleState state, boolean pressured) {
        if (positiveSelfEffectsBlocked(state) || state.hasEffect("flight") || !pressured) {
            return 0.0d;
        }
        return 4.3d;
    }

    private double scoreNoPressureOpponentEffect(IBattleState opponentState, boolean pressured, boolean far, String effectId) {
        if (pressured || opponentState.hasEffect(effectId)) {
            return 0.0d;
        }

        double score = 2.6d;
        if (far) {
            score += 0.6d;
        }
        return score;
    }

    private double scoreRemoteMine(BattleState state,
                                   IBattleState opponentState,
                                   LivingEntity opponent,
                                   BattleAbilityContext context,
                                   boolean pressured,
                                   boolean far,
                                   boolean highMana) {
        if (state.hasActiveMine(context.slotIndex(), opponent.getUUID())) {
            float chargePct = getRemoteMineChargePct(opponentState, context.slotIndex());
            if (chargePct < TeenyBalance.AI_REMOTE_MINE_DETONATE_MIN_CHARGE_PCT) {
                return 0.0d;
            }
            return 6.0d + (chargePct * 4.0d);
        }

        if (pressured) {
            return 0.0d;
        }

        double score = 3.0d;
        if (far) {
            score += 0.8d;
        }
        if (highMana) {
            score += 0.4d;
        }
        return score;
    }

    private double scoreHealthRadio(BattleState state, float selfHpPct, boolean pressured) {
        if (healingBlocked(state) || pressured || state.hasEffect("health_radio")) {
            return 0.0d;
        }

        float missingPct = 1.0f - selfHpPct;
        if (missingPct < TeenyBalance.AI_HEALTH_RADIO_MIN_MISSING_HP_PCT) {
            return 0.0d;
        }
        return 1.8d + (missingPct * 5.0d);
    }

    private double scoreNoPressureSelfSetup(BattleState state,
                                            boolean pressured,
                                            boolean far,
                                            boolean highMana,
                                            String effectId,
                                            double baseScore) {
        if (positiveSelfEffectsBlocked(state) || pressured || state.hasEffect(effectId)) {
            return 0.0d;
        }

        double score = baseScore;
        if (far) {
            score += 0.6d;
        }
        if (highMana) {
            score += 0.5d;
        }
        return score;
    }

    private double scoreFreeze(IBattleState opponentState, BattleAbilityContext context) {
        if (context.slotIndex() == 2 || opponentState.hasEffect("freeze") || opponentState.hasEffect("freeze_movement")) {
            return 0.0d;
        }

        double score = 2.8d;
        if (getManaPct(opponentState) >= TeenyBalance.AI_OPPONENT_HIGH_MANA_PCT) {
            score += 2.0d;
        }
        return score;
    }

    private double scorePets(BattleState state, boolean pressured, boolean far) {
        if (positiveSelfEffectsBlocked(state) || pressured) {
            return 0.0d;
        }

        boolean hasPet1 = state.hasEffect("pet_slot_1");
        boolean hasPet2 = state.hasEffect("pet_slot_2");
        if (hasPet1 && hasPet2) {
            return 0.0d;
        }

        double score = (hasPet1 || hasPet2) ? 2.0d : 3.0d;
        if (far) {
            score += 0.6d;
        }
        return score;
    }

    private double scoreReflect(BattleState state, boolean pressured) {
        if (!pressured || reflectReuseTicks > 0 || state.hasEffect("reflect")) {
            return 0.0d;
        }
        return 4.6d;
    }

    private boolean healingBlocked(BattleState state) {
        return state.hasEffect("kiss");
    }

    private boolean positiveSelfEffectsBlocked(BattleState state) {
        return state.hasEffect("kiss");
    }

    private int countSelfCleanseableDebuffs(BattleState state) {
        int count = 0;
        for (String effectId : CLEANSEABLE_SELF_EFFECTS) {
            if (state.hasEffect(effectId)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSelfEffect(BattleAbilityContext context, String effectId) {
        if (context.data().effectsOnSelf == null) {
            return false;
        }
        for (AbilityLoader.EffectData effect : context.data().effectsOnSelf) {
            if (effectId.equals(effect.id)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOpponentEffectId(BattleAbilityContext context, String effectId) {
        if (context.data().effectsOnOpponent == null) {
            return false;
        }
        for (AbilityLoader.EffectData effect : context.data().effectsOnOpponent) {
            if (effectId.equals(effect.id)) {
                return true;
            }
        }
        return false;
    }

    private PureEffectFamily resolvePureEffectFamily(BattleAbilityContext context) {
        if (context.isSelfTargeted()) {
            if (hasSelfEffect(context, "group_heal")) {
                return PureEffectFamily.GROUP_HEAL;
            }
            if (hasSelfEffect(context, "heal")) {
                return PureEffectFamily.HEAL;
            }
            if (hasSelfEffect(context, "health_radio")) {
                return PureEffectFamily.HEALTH_RADIO;
            }
            if (hasSelfEffect(context, "cleanse")) {
                return PureEffectFamily.CLEANSE;
            }
            if (hasSelfEffect(context, "power_up")) {
                return PureEffectFamily.POWER_UP;
            }
            if (hasSelfEffect(context, "dance")) {
                return PureEffectFamily.DANCE;
            }
            if (hasSelfEffect(context, "cuteness")) {
                return PureEffectFamily.CUTENESS;
            }
            if (hasSelfEffect(context, "shield")) {
                return PureEffectFamily.SHIELD;
            }
            if (hasSelfEffect(context, "flight")) {
                return PureEffectFamily.FLIGHT;
            }
            if (hasSelfEffect(context, "power_radio")) {
                return PureEffectFamily.POWER_RADIO;
            }
            if (hasSelfEffect(context, "defense_up")) {
                return PureEffectFamily.DEFENSE_UP;
            }
            if (hasSelfEffect(context, "dodge_smoke")) {
                return PureEffectFamily.DODGE_SMOKE;
            }
            if (hasSelfEffect(context, "luck_up")) {
                return PureEffectFamily.LUCK_UP;
            }
            if (hasSelfEffect(context, "pets")) {
                return PureEffectFamily.PETS;
            }
            if (hasSelfEffect(context, "reflect")) {
                return PureEffectFamily.REFLECT;
            }
            if (hasSelfEffect(context, "self_shock") && hasSelfEffect(context, "bar_fill")) {
                return PureEffectFamily.BATTERY_DRAIN;
            }
            return null;
        }

        if (hasOpponentEffectId(context, "remote_mine")) {
            return PureEffectFamily.REMOTE_MINE;
        }
        if (hasOpponentEffectId(context, "poison")) {
            return PureEffectFamily.POISON;
        }
        if (context.data().damageTier != 0) {
            return null;
        }
        if (hasOpponentEffectId(context, "power_down")) {
            return PureEffectFamily.POWER_DOWN;
        }
        if (hasOpponentEffectId(context, "curse")) {
            return PureEffectFamily.CURSE;
        }
        if (hasOpponentEffectId(context, "waffle")) {
            return PureEffectFamily.WAFFLE;
        }
        if (hasOpponentEffectId(context, "defense_down")) {
            return PureEffectFamily.DEFENSE_DOWN;
        }
        if (hasOpponentEffectId(context, "freeze")) {
            return PureEffectFamily.FREEZE;
        }
        return null;
    }

    private boolean isFar(double distance, BattleFigure enemyFigure) {
        return distance > getThreatBandMax(enemyFigure) + TeenyBalance.AI_FAR_DISTANCE_PADDING;
    }

    private boolean isThreatBand(double distance, BattleFigure enemyFigure) {
        return distance >= TeenyBalance.AI_THREAT_BAND_MIN_DISTANCE
                && distance <= getThreatBandMax(enemyFigure);
    }

    private double getThreatBandMax(BattleFigure enemyFigure) {
        return Math.max(
                TeenyBalance.AI_THREAT_BAND_MIN_MAX_DISTANCE,
                estimateThreatRange(enemyFigure) + TeenyBalance.AI_THREAT_BAND_RANGE_PADDING
        );
    }

    private double estimateThreatRange(BattleFigure figure) {
        if (figure == null) {
            return 6.0d;
        }

        double maxRange = MELEE_REACH + 0.8d;
        for (int slot = 0; slot < 3; slot++) {
            AbilityLoader.AbilityData data = BattleAbilityContext.getAbilityData(figure, slot);
            if (data == null) {
                continue;
            }
            if ("melee".equalsIgnoreCase(data.hitType)) {
                maxRange = Math.max(maxRange, MELEE_REACH + 0.8d);
            } else if ("raycasting".equalsIgnoreCase(data.hitType) || "ranged".equalsIgnoreCase(data.hitType)) {
                maxRange = Math.max(maxRange, TeenyBalance.getRangeValue(data.rangeTier));
            }
        }
        return maxRange;
    }

    private float getRemoteMineChargePct(IBattleState opponentState, int slotIndex) {
        BattleTargeting.ArmedMine armedMine = BattleTargeting.findArmedMine(opponentState, slotIndex, dummy.getUUID());
        if (armedMine == null) {
            return 0.0f;
        }
        return Math.min(1.0f, armedMine.instance().magnitude / (float) TeenyBalance.REMOTE_MINE_STAGES);
    }

    private float getManaPct(IBattleState state) {
        if (state == null || TeenyBalance.BATTLE_MANA_MAX <= 0) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, state.getCurrentMana() / (float) TeenyBalance.BATTLE_MANA_MAX));
    }

    private double scoreSelfEffectNovelty(BattleState state, BattleAbilityContext context) {
        if (context.data().effectsOnSelf == null || context.data().effectsOnSelf.isEmpty()) {
            return 0.0d;
        }

        double score = 0.0d;
        int redundantEffects = 0;
        int freshEffects = 0;
        for (AbilityLoader.EffectData effect : context.data().effectsOnSelf) {
            if (state.hasEffect(effect.id)) {
                redundantEffects++;
                score -= 2.1d;
            } else {
                freshEffects++;
                score += 0.5d;
            }
        }

        if (freshEffects == 0 && redundantEffects > 0) {
            score -= 1.8d;
        }
        return score;
    }

    private double scoreOpponentEffectNovelty(IBattleState opponentState, BattleAbilityContext context) {
        if (context.data().effectsOnOpponent == null || context.data().effectsOnOpponent.isEmpty()) {
            return 0.0d;
        }

        double score = 0.0d;
        int redundantEffects = 0;
        int freshEffects = 0;
        for (AbilityLoader.EffectData effect : context.data().effectsOnOpponent) {
            if (opponentState.hasEffect(effect.id)) {
                redundantEffects++;
                score -= 2.4d;
            } else {
                freshEffects++;
                score += 0.5d;
            }
        }

        if (freshEffects == 0 && redundantEffects > 0) {
            score -= 2.2d;
        }
        return score;
    }

    private boolean hasEnemyCleanse(IBattleState opponentState) {
        BattleFigure enemy = opponentState.getActiveFigure();
        if (enemy == null) {
            return false;
        }

        for (int slot = 0; slot < 3; slot++) {
            AbilityLoader.AbilityData data = BattleAbilityContext.getAbilityData(enemy, slot);
            if (data != null && containsAny(data.effectsOnSelf, SELF_CLEANSE_EFFECTS)) {
                return true;
            }
        }
        return false;
    }

    private double scoreClassMatchup(FigureClassType selfClass, FigureClassType enemyClass, double magnitude) {
        if (selfClass.hasAdvantageOver(enemyClass)) {
            return magnitude;
        }
        if (enemyClass.hasAdvantageOver(selfClass)) {
            return -magnitude;
        }
        return 0.0d;
    }

    private boolean shouldReevaluate(BattleState state, LivingEntity opponent, BattleFigure active, BattleAiProfile profile) {
        if (plannedSlot < 0) {
            return true;
        }
        if (state.getActiveFigure() != active) {
            return true;
        }
        BattleAbilityContext context = BattleAbilityContext.create(state, dummy, active, plannedSlot);
        if (context == null || !isActionUsable(state, active, context)) {
            return true;
        }
        AbilityRole plannedRole = classifyRole(context);
        if (isPressured(opponent)) {
            if (plannedRole == AbilityRole.BUFF
                    || plannedRole == AbilityRole.DEBUFF
                    || plannedRole == AbilityRole.CLEANSE
                    || plannedRole == AbilityRole.UTILITY) {
                return true;
            }
            if (!context.isMelee()
                    && dummy.distanceTo(opponent) <= MELEE_REACH + 0.25d
                    && hasUsableMeleeAction(state, active)) {
                return true;
            }
        }
        if (isHighMana(state) && (plannedRole == AbilityRole.BUFF || plannedRole == AbilityRole.CLEANSE || plannedRole == AbilityRole.UTILITY)) {
            return true;
        }
        if (context.isRanged() && context.hasOpponentEffect("remote_mine") && state.hasActiveMine(plannedSlot, opponent.getUUID())) {
            return false;
        }
        return false;
    }

    private void tryUsePlannedAction(BattleState state, BattleFigure active, LivingEntity opponent, BattleAiProfile profile) {
        if (plannedSlot < 0 || actionCooldownTicks > 0) {
            return;
        }

        BattleAbilityContext context = BattleAbilityContext.create(state, dummy, active, plannedSlot);
        if (context == null || !isActionUsable(state, active, context)) {
            return;
        }

        boolean used = false;
        if (context.isMelee()) {
            if (dummy.distanceTo(opponent) <= MELEE_REACH) {
                AbilityExecutor.executeAttack(dummy, active, plannedSlot, opponent);
                used = true;
            }
        } else if (context.isSelfTargeted()) {
            AbilityExecutor.executeAction(dummy, active, plannedSlot);
            used = true;
        } else if (context.hasOpponentEffect("remote_mine") && state.hasActiveMine(plannedSlot, opponent.getUUID())) {
            AbilityExecutor.executeAction(dummy, active, plannedSlot);
            used = true;
        } else if (BattleTargeting.getConeTarget(context) != null) {
            AbilityExecutor.executeAction(dummy, active, plannedSlot);
            used = true;
        }

        if (used) {
            PureEffectFamily pureEffectFamily = resolvePureEffectFamily(context);
            if (plannedSlot == lastUsedSlot) {
                lastUsedSlotStreak++;
            } else {
                lastUsedSlot = plannedSlot;
                lastUsedSlotStreak = 1;
            }
            if (pureEffectFamily == PureEffectFamily.REFLECT) {
                reflectReuseTicks = TeenyBalance.AI_REFLECT_REUSE_TICKS;
            }
            actionCooldownTicks = profile.actionCommitTicks();
            reevaluateTicks = Math.min(reevaluateTicks, Math.max(4, profile.reactionIntervalTicks() / 2));
        }
    }

    private void handleMovement(BattleState state, LivingEntity opponent, BattleAiProfile profile) {
        double distance = dummy.distanceTo(opponent);
        double moveSpeedMult = profile.moveSpeedMult();

        switch (intent) {
            case PRESSURE_MELEE -> {
                if (distance > MELEE_REACH) {
                    dummy.getNavigation().moveTo(opponent, TeenyBalance.AI_APPROACH_MOVE_SPEED * moveSpeedMult);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.35f, 0.45f);
                }
            }
            case PRESSURE_RANGED -> {
                double desiredRange = desiredRangeForPlannedAction(state, profile);
                if (distance > desiredRange + RANGE_BAND_PADDING) {
                    dummy.getNavigation().moveTo(opponent, TeenyBalance.AI_RANGE_APPROACH_MOVE_SPEED * moveSpeedMult);
                } else if (distance < desiredRange - RANGE_BAND_PADDING) {
                    moveAwayFrom(opponent, TeenyBalance.AI_RETREAT_MOVE_SPEED * moveSpeedMult);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.05f, 0.55f);
                }
            }
            case SELF_MAINTENANCE, KITE -> {
                double desiredRange = Math.max(6.0d, desiredRangeForPlannedAction(state, profile));
                if (distance < desiredRange) {
                    moveAwayFrom(opponent, TeenyBalance.AI_RETREAT_MOVE_SPEED * moveSpeedMult);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.0f, 0.5f);
                }
            }
            case HARD_SWAP, STALL -> {
                if (distance < 5.0d) {
                    moveAwayFrom(opponent, TeenyBalance.AI_RETREAT_MOVE_SPEED * moveSpeedMult);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.0f, 0.35f);
                }
            }
        }
    }

    private void moveAwayFrom(LivingEntity opponent, double moveSpeed) {
        Vec3 away = dummy.position().subtract(opponent.position());
        if (away.lengthSqr() < 1.0E-4d) {
            away = new Vec3(dummy.getRandom().nextDouble() - 0.5d, 0.0d, dummy.getRandom().nextDouble() - 0.5d);
        }
        away = new Vec3(away.x, 0.0d, away.z).normalize();
        Vec3 target = dummy.position().add(away.scale(4.0d));
        dummy.getNavigation().moveTo(target.x, dummy.getY(), target.z, moveSpeed);
    }

    private void strafe(float forward, float sideways) {
        if (strafeTicks <= 0) {
            strafeTicks = 20 + dummy.getRandom().nextInt(20);
            strafeDirection = dummy.getRandom().nextBoolean() ? 1.0f : -1.0f;
        }
        dummy.getMoveControl().strafe(forward, sideways * strafeDirection);
    }

    private double desiredRangeForPlannedAction(BattleState state, BattleAiProfile profile) {
        BattleFigure active = state.getActiveFigure();
        if (active == null || plannedSlot < 0) {
            return 6.0d;
        }

        AbilityLoader.AbilityData data = BattleAbilityContext.getAbilityData(active, plannedSlot);
        if (data == null) {
            return 6.0d;
        }

        if ("melee".equalsIgnoreCase(data.hitType)) {
            return MELEE_REACH;
        }

        double maxRange = TeenyBalance.getRangeValue(data.rangeTier);
        return adjustPreferredRange(maxRange, BattleAbilityContext.create(state, dummy, active, plannedSlot), profile);
    }

    private double adjustPreferredRange(double maxRange, BattleAbilityContext context, BattleAiProfile profile) {
        if (maxRange <= 0.0d) {
            return 6.0d;
        }

        double desired = switch (profile.preferredRange()) {
            case CLOSE -> maxRange * 0.45d;
            case MID -> maxRange * 0.62d;
            case FAR -> maxRange * 0.78d;
            case AUTO -> {
                if (context != null && classifyRole(context) == AbilityRole.CONTROL) {
                    yield maxRange * 0.70d;
                }
                yield maxRange * 0.60d;
            }
        };
        return Math.max(3.0d, desired);
    }

    private void faceTarget(LivingEntity target) {
        dummy.getLookControl().setLookAt(target, 30.0f, 30.0f);

        Vec3 delta = target.getEyePosition().subtract(dummy.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0E-4d) {
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0d);
        float pitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontal)));
        dummy.setYRot(yaw);
        dummy.setYHeadRot(yaw);
        dummy.setYBodyRot(yaw);
        dummy.setXRot(pitch);
    }

    private boolean hasMovementLock(BattleState state) {
        return state.hasEffect("arena_launch_movement_lock") || state.hasEffect("freeze_movement");
    }

    private float getHpPct(BattleFigure figure) {
        if (figure == null || figure.getMaxHp() <= 0) {
            return 0.0f;
        }
        return (float) figure.getCurrentHp() / (float) figure.getMaxHp();
    }

    private BattleState getState() {
        return dummy.getCapability(BattleStateProvider.BATTLE_STATE)
                .resolve()
                .filter(BattleState.class::isInstance)
                .map(BattleState.class::cast)
                .orElse(null);
    }

    private boolean prefersDistance(AbilityRole role) {
        return role == AbilityRole.HEAL
                || role == AbilityRole.CLEANSE
                || role == AbilityRole.BUFF
                || role == AbilityRole.UTILITY;
    }

    private double scoreSwapClassMatchup(FigureClassType selfClass, FigureClassType enemyClass, BattleAiProfile profile) {
        if (selfClass.hasAdvantageOver(enemyClass)) {
            return profile.considersClassAdvantageSwap() ? 4.5d : 0.0d;
        }
        if (enemyClass.hasAdvantageOver(selfClass)) {
            return profile.considersClassDisadvantageSwap() ? -4.0d : 0.0d;
        }
        return 0.0d;
    }

    private boolean isPressured(LivingEntity opponent) {
        return recentDamageTicks > 0 || dummy.distanceTo(opponent) <= MELEE_REACH + 1.0d;
    }

    private boolean isHighMana(BattleState state) {
        return state.getCurrentMana() >= (TeenyBalance.BATTLE_MANA_MAX * 0.85f);
    }

    private boolean hasUsableMeleeAction(BattleState state, BattleFigure active) {
        for (int slot = 0; slot < 3; slot++) {
            BattleAbilityContext context = BattleAbilityContext.create(state, dummy, active, slot);
            if (context != null && context.isMelee() && isActionUsable(state, active, context)) {
                return true;
            }
        }
        return false;
    }

    private double scoreEffectiveManaValue(BattleAbilityContext context, AbilityRole role, boolean highMana, boolean fullMana) {
        if (context.actualManaCost() <= 0 || context.effectiveManaCost() <= context.actualManaCost()) {
            return 0.0d;
        }

        double effectiveBonusRatio = (double) (context.effectiveManaCost() - context.actualManaCost()) / (double) context.actualManaCost();
        double score = effectiveBonusRatio * TeenyBalance.AI_EFFECTIVE_MANA_VALUE_WEIGHT;

        if (highMana) {
            score += effectiveBonusRatio * TeenyBalance.AI_HIGH_MANA_EFFECTIVE_VALUE_WEIGHT;
        }

        if (context.slotIndex() == 2) {
            if (highMana) {
                score += TeenyBalance.AI_HIGH_MANA_SLOT3_PRIORITY_BONUS;
            }
            if (fullMana) {
                score += TeenyBalance.AI_FULL_MANA_SLOT3_PRIORITY_BONUS;
            }
        }

        return switch (role) {
            case MELEE_DAMAGE, RANGED_DAMAGE, CONTROL, DEBUFF -> score;
            case BUFF -> score * 0.45d;
            case HEAL, CLEANSE, UTILITY -> 0.0d;
        };
    }

    private boolean needsEmergencyMaintenance(BattleState state, BattleFigure active, BattleAiProfile profile) {
        return getHpPct(active) <= TeenyBalance.AI_SELF_HEAL_CRITICAL_HP_PCT
                || (!state.hasEffect("kiss") && countSelfCleanseableDebuffs(state) >= 2);
    }

    private ScoredAction selectMeleeOverride(List<ScoredAction> actions,
                                             BattleState state,
                                             BattleFigure active,
                                             LivingEntity opponent,
                                             BattleAiProfile profile) {
        if (dummy.distanceTo(opponent) > MELEE_REACH + 0.25d || needsEmergencyMaintenance(state, active, profile)) {
            return null;
        }

        ScoredAction best = null;
        for (ScoredAction action : actions) {
            if (action.role() != AbilityRole.MELEE_DAMAGE) {
                continue;
            }
            if (best == null || action.score() > best.score()) {
                best = action;
            }
        }
        return best;
    }

    private ScoredAction selectNearReadyMeleeOverride(BattleState state,
                                                      BattleFigure active,
                                                      LivingEntity opponent,
                                                      BattleAiProfile profile) {
        if (dummy.distanceTo(opponent) > MELEE_REACH + 0.25d || needsEmergencyMaintenance(state, active, profile)) {
            return null;
        }

        ScoredAction best = null;
        for (int slot = 0; slot < 3; slot++) {
            BattleAbilityContext context = BattleAbilityContext.create(state, dummy, active, slot);
            if (context == null || !context.isMelee()) {
                continue;
            }
            if (isActionUsable(state, active, context)) {
                continue;
            }
            if (state.getCurrentMana() <= 0 || context.actualManaCost() <= 0) {
                continue;
            }

            float readiness = state.getCurrentMana() / (float) context.actualManaCost();
            if (readiness < TeenyBalance.AI_NEAR_READY_MELEE_MANA_PCT) {
                continue;
            }

            double score = 7.0d + readiness + scoreEffectiveManaValue(context, AbilityRole.MELEE_DAMAGE, isHighMana(state), false);
            ScoredAction candidate = new ScoredAction(slot, AbilityRole.MELEE_DAMAGE, score, false);
            if (best == null || candidate.score() > best.score()) {
                best = candidate;
            }
        }
        return best;
    }

    private enum Intent {
        HARD_SWAP,
        PRESSURE_MELEE,
        PRESSURE_RANGED,
        KITE,
        SELF_MAINTENANCE,
        STALL
    }

    private enum AbilityRole {
        MELEE_DAMAGE,
        RANGED_DAMAGE,
        HEAL,
        CLEANSE,
        BUFF,
        CONTROL,
        DEBUFF,
        UTILITY
    }

    private enum PureEffectFamily {
        HEAL,
        GROUP_HEAL,
        POWER_UP,
        DANCE,
        CLEANSE,
        BATTERY_DRAIN,
        CUTENESS,
        SHIELD,
        FLIGHT,
        POISON,
        POWER_DOWN,
        CURSE,
        WAFFLE,
        REMOTE_MINE,
        HEALTH_RADIO,
        POWER_RADIO,
        DEFENSE_DOWN,
        DEFENSE_UP,
        FREEZE,
        DODGE_SMOKE,
        LUCK_UP,
        PETS,
        REFLECT
    }

    private record ScoredAction(int slot, AbilityRole role, double score, boolean prefersDistance) {
    }
}
