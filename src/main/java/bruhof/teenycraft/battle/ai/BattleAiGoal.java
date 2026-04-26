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

        BattleAiProfile profile = dummy.getAiProfile();
        if (reevaluateTicks <= 0 || shouldReevaluate(state, opponent, active)) {
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
        if (actionCooldownTicks > 0
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
            return false;
        }

        state.swapFigure(bestIndex, null);
        plannedSlot = -1;
        intent = Intent.HARD_SWAP;
        actionCooldownTicks = profile.actionCommitTicks();
        reevaluateTicks = profile.reactionIntervalTicks();
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

        if (figure.getFigureClass().hasAdvantageOver(enemyFigure.getFigureClass())) {
            score += 4.5d;
        } else if (enemyFigure.getFigureClass().hasAdvantageOver(figure.getFigureClass())) {
            score -= 4.0d;
        }

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
                score -= 0.85d;
            }
            if (score > 0.0d) {
                actions.add(new ScoredAction(slot, role, score, prefersDistance(role)));
            }
        }

        if (actions.isEmpty()) {
            return null;
        }

        double max = actions.stream().mapToDouble(ScoredAction::score).max().orElse(0.0d);
        List<ScoredAction> shortlist = new ArrayList<>();
        double minScore = max - profile.choiceWindow();
        for (ScoredAction action : actions) {
            if (action.score() >= minScore) {
                shortlist.add(action);
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

        return state.getCurrentMana() >= context.manaCost();
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
        double score = switch (role) {
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

        if (context.hasTrait("charge_up")) {
            score -= (1.0f - profile.riskTolerance()) * 1.0d;
            if (selfHpPct <= 0.35f) {
                score -= 1.2d;
            }
        }
        if (context.hasTrait("blue") && state.getCurrentMana() >= (context.manaCost() * 1.5f)) {
            score += 0.8d;
        }

        if (context.isSelfTargeted() && distance <= 3.0d && role != AbilityRole.HEAL && role != AbilityRole.CLEANSE) {
            score -= 0.6d;
        }

        if (context.manaCost() > (state.getCurrentMana() * (0.55f + (profile.manaDiscipline() * 0.35f)))) {
            score -= 0.7d;
        }

        return score;
    }

    private double scoreHeal(BattleState state, BattleFigure active, BattleAiProfile profile) {
        float hpPct = getHpPct(active);
        float missingPct = 1.0f - hpPct;
        if (missingPct < 0.18f) {
            return 0.0d;
        }
        double score = 2.0d + (missingPct * 8.0d);
        if (state.hasEffect("kiss")) {
            score = 0.0d;
        }
        if (hpPct <= (0.30f + ((1.0f - profile.riskTolerance()) * 0.15f))) {
            score += 1.8d;
        }
        return score;
    }

    private double scoreCleanse(BattleState state, BattleAiProfile profile) {
        double score = 0.0d;
        for (String effectId : CLEANSEABLE_SELF_EFFECTS) {
            if (state.hasEffect(effectId)) {
                score += switch (effectId) {
                    case "stun", "root", "freeze_movement" -> 3.5d;
                    case "poison", "shock", "kiss" -> 2.5d;
                    default -> 1.4d;
                };
            }
        }
        if (state.hasEffect("kiss")) {
            return 0.0d;
        }
        return score + ((1.0f - profile.riskTolerance()) * 0.5d);
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

    private boolean shouldReevaluate(BattleState state, LivingEntity opponent, BattleFigure active) {
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
            lastUsedSlot = plannedSlot;
            actionCooldownTicks = profile.actionCommitTicks();
            reevaluateTicks = Math.min(reevaluateTicks, Math.max(4, profile.reactionIntervalTicks() / 2));
        }
    }

    private void handleMovement(BattleState state, LivingEntity opponent, BattleAiProfile profile) {
        double distance = dummy.distanceTo(opponent);
        double moveSpeed = 1.0d;

        switch (intent) {
            case PRESSURE_MELEE -> {
                if (distance > MELEE_REACH) {
                    dummy.getNavigation().moveTo(opponent, moveSpeed);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.35f, 0.45f);
                }
            }
            case PRESSURE_RANGED -> {
                double desiredRange = desiredRangeForPlannedAction(state, profile);
                if (distance > desiredRange + RANGE_BAND_PADDING) {
                    dummy.getNavigation().moveTo(opponent, moveSpeed);
                } else if (distance < desiredRange - RANGE_BAND_PADDING) {
                    moveAwayFrom(opponent, moveSpeed);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.05f, 0.55f);
                }
            }
            case SELF_MAINTENANCE, KITE -> {
                double desiredRange = Math.max(6.0d, desiredRangeForPlannedAction(state, profile));
                if (distance < desiredRange) {
                    moveAwayFrom(opponent, moveSpeed);
                } else {
                    dummy.getNavigation().stop();
                    strafe(0.0f, 0.5f);
                }
            }
            case HARD_SWAP, STALL -> {
                if (distance < 5.0d) {
                    moveAwayFrom(opponent, moveSpeed);
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

    private record ScoredAction(int slot, AbilityRole role, double score, boolean prefersDistance) {
    }
}
