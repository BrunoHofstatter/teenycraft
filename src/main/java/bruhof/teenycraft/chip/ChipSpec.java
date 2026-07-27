package bruhof.teenycraft.chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChipSpec {

    public static class StatModifier {
        private final ChipStatType statType;
        private final int[] flatByRank;
        private final float[] basePctByRank;
        private final int[] exactByRank;

        private StatModifier(ChipStatType statType, int[] flatByRank, float[] basePctByRank, int[] exactByRank) {
            this.statType = statType;
            this.flatByRank = flatByRank;
            this.basePctByRank = basePctByRank;
            this.exactByRank = exactByRank;
        }

        public ChipStatType getStatType() {
            return statType;
        }

        public int getFlat(int rank) {
            return getRankValue(flatByRank, rank);
        }

        public float getBasePct(int rank) {
            return getRankValue(basePctByRank, rank);
        }

        public boolean hasExactValue() {
            return exactByRank != null && exactByRank.length > 0;
        }

        public int getExactValue(int rank) {
            return getRankValue(exactByRank, rank);
        }

        private StatModifier scaled(float scale) {
            return new StatModifier(
                    statType,
                    scaleIntArray(flatByRank, scale),
                    scaleFloatArray(basePctByRank, scale),
                    scaleIntArray(exactByRank, scale)
            );
        }
    }

    public static class EffectSpec {
        private final String effectId;
        private final int[] amountByRank;
        private final int[] durationByRank;
        private final float[] powerByRank;

        private EffectSpec(String effectId, int[] amountByRank, int[] durationByRank, float[] powerByRank) {
            this.effectId = effectId;
            this.amountByRank = amountByRank;
            this.durationByRank = durationByRank;
            this.powerByRank = powerByRank;
        }

        public static EffectSpec of(String effectId, int[] durationByRank, int[] amountByRank) {
            return new EffectSpec(effectId, amountByRank, durationByRank, null);
        }

        public static EffectSpec of(String effectId, int[] durationByRank, int[] amountByRank, float[] powerByRank) {
            return new EffectSpec(effectId, amountByRank, durationByRank, powerByRank);
        }

        public String getEffectId() {
            return effectId;
        }

        public int getAmount(int rank) {
            return getRankValue(amountByRank, rank);
        }

        public int getDuration(int rank) {
            return getRankValue(durationByRank, rank);
        }

        public float getPower(int rank) {
            return getRankValue(powerByRank, rank);
        }

        private EffectSpec scaled(float scale) {
            return new EffectSpec(effectId, scaleIntArray(amountByRank, scale), scaleIntArray(durationByRank, scale),
                    scaleFloatArray(powerByRank, scale));
        }
    }

    public static class Action {
        public enum Type {
            APPLY_EFFECT_SELF,
            APPLY_EFFECT_OPPONENT,
            APPLY_RANDOM_EFFECT_OPPONENT,
            ADD_MANA_SELF,
            ADD_BATTERY_SELF,
            HEAL_SELF_FLAT,
            HEAL_SELF_MAX_HP_PCT,
            STEAL_MANA_OPPONENT,
            DEAL_DAMAGE_OPPONENT,
            SUMMON_PET_SELF
        }

        private final Type type;
        private final int[] amountByRank;
        private final int[] durationByRank;
        private final float[] percentByRank;
        private final boolean groupDamage;
        private final EffectSpec effectSpec;
        private final List<EffectSpec> randomEffectPool;

        private Action(Type type, int[] amountByRank, int[] durationByRank, float[] percentByRank, boolean groupDamage,
                       EffectSpec effectSpec, List<EffectSpec> randomEffectPool) {
            this.type = type;
            this.amountByRank = amountByRank;
            this.durationByRank = durationByRank;
            this.percentByRank = percentByRank;
            this.groupDamage = groupDamage;
            this.effectSpec = effectSpec;
            this.randomEffectPool = randomEffectPool != null
                    ? Collections.unmodifiableList(new ArrayList<>(randomEffectPool))
                    : List.of();
        }

        public static Action applyEffectSelf(String effectId, int[] durationByRank, int[] amountByRank) {
            return new Action(Type.APPLY_EFFECT_SELF, null, null, null, false,
                    EffectSpec.of(effectId, durationByRank, amountByRank), null);
        }

        public static Action applyEffectSelf(String effectId, int[] durationByRank, int[] amountByRank, float[] powerByRank) {
            return new Action(Type.APPLY_EFFECT_SELF, null, null, null, false,
                    EffectSpec.of(effectId, durationByRank, amountByRank, powerByRank), null);
        }

        public static Action applyEffectOpponent(String effectId, int[] durationByRank, int[] amountByRank) {
            return new Action(Type.APPLY_EFFECT_OPPONENT, null, null, null, false,
                    EffectSpec.of(effectId, durationByRank, amountByRank), null);
        }

        public static Action applyEffectOpponent(String effectId, int[] durationByRank, int[] amountByRank, float[] powerByRank) {
            return new Action(Type.APPLY_EFFECT_OPPONENT, null, null, null, false,
                    EffectSpec.of(effectId, durationByRank, amountByRank, powerByRank), null);
        }

        public static Action applyRandomEffectOpponent(EffectSpec... effects) {
            return new Action(Type.APPLY_RANDOM_EFFECT_OPPONENT, null, null, null, false, null, Arrays.asList(effects));
        }

        public static Action addManaSelf(int[] amountByRank) {
            return new Action(Type.ADD_MANA_SELF, amountByRank, null, null, false, null, null);
        }

        public static Action addBatterySelf(int[] amountByRank) {
            return new Action(Type.ADD_BATTERY_SELF, amountByRank, null, null, false, null, null);
        }

        public static Action healSelfFlat(int[] amountByRank) {
            return new Action(Type.HEAL_SELF_FLAT, amountByRank, null, null, false, null, null);
        }

        public static Action healSelfMaxHpPct(float[] percentByRank) {
            return new Action(Type.HEAL_SELF_MAX_HP_PCT, null, null, percentByRank, false, null, null);
        }

        public static Action stealManaOpponent(int[] amountByRank) {
            return new Action(Type.STEAL_MANA_OPPONENT, amountByRank, null, null, false, null, null);
        }

        public static Action dealDamageOpponent(int[] amountByRank, boolean groupDamage) {
            return new Action(Type.DEAL_DAMAGE_OPPONENT, amountByRank, null, null, groupDamage, null, null);
        }

        public static Action summonPetSelf(int[] durationByRank, int[] amountByRank) {
            return new Action(Type.SUMMON_PET_SELF, amountByRank, durationByRank, null, false, null, null);
        }

        public Type getType() {
            return type;
        }

        public int getAmount(int rank) {
            return getRankValue(amountByRank, rank);
        }

        public int getDuration(int rank) {
            return getRankValue(durationByRank, rank);
        }

        public float getPercent(int rank) {
            return getRankValue(percentByRank, rank);
        }

        public boolean isGroupDamage() {
            return groupDamage;
        }

        public EffectSpec getEffectSpec() {
            return effectSpec;
        }

        public List<EffectSpec> getRandomEffectPool() {
            return randomEffectPool;
        }

        private Action scaled(float scale) {
            List<EffectSpec> scaledPool = randomEffectPool.stream()
                    .map(effect -> effect.scaled(scale))
                    .toList();
            return new Action(
                    type,
                    scaleIntArray(amountByRank, scale),
                    scaleIntArray(durationByRank, scale),
                    scaleFloatArray(percentByRank, scale),
                    groupDamage,
                    effectSpec != null ? effectSpec.scaled(scale) : null,
                    scaledPool
            );
        }
    }

    public static class Builder {
        private record InheritedBehavior(String chipId, float scale) { }

        private final String id;
        private final int maxRank;
        private final List<StatModifier> statModifiers = new ArrayList<>();
        private final List<Action> onFirstAppearanceActions = new ArrayList<>();
        private final List<Action> onFaintActions = new ArrayList<>();
        private final List<Action> onKillActions = new ArrayList<>();
        private final List<Action> onCritHitActions = new ArrayList<>();
        private final List<Action> onDodgeActions = new ArrayList<>();
        private final List<Action> onDamagedActions = new ArrayList<>();
        private final List<InheritedBehavior> inheritedBehaviors = new ArrayList<>();
        private float[] extraInstantCastChanceByRank = new float[0];
        private float[] chargeSpeedBonusByRank = new float[0];
        private float[] projectileSpeedBonusByRank = new float[0];
        private float[] passiveBatteryChargeBonusByRank = new float[0];
        private float[] tofuChanceBonusByRank = new float[0];
        private float[] abilityHealBonusByRank = new float[0];
        private int[] activeRegenIntervalByRank = new int[0];
        private int[] activeRegenHealByRank = new int[0];
        private float[] directStunDurationReductionByRank = new float[0];
        private float[] finisherHpThresholdByRank = new float[0];
        private float[] finisherDamageBonusByRank = new float[0];
        private float[] teamMedicBenchHealPctByRank = new float[0];
        private int[] secondChanceSurviveHpByRank = new int[0];
        private int[] secondChanceDebuffDurationByRank = new int[0];
        private int[] secondChanceSlowMagnitudeByRank = new int[0];
        private int[] fusionCostByRank = new int[0];

        private Builder(String id, int maxRank) {
            this.id = id;
            this.maxRank = Math.max(1, maxRank);
        }

        public Builder addFlatStat(ChipStatType statType, int... valuesByRank) {
            this.statModifiers.add(new StatModifier(statType, valuesByRank, null, null));
            return this;
        }

        public Builder addBasePctStat(ChipStatType statType, float... valuesByRank) {
            this.statModifiers.add(new StatModifier(statType, null, valuesByRank, null));
            return this;
        }

        public Builder setExactStat(ChipStatType statType, int... valuesByRank) {
            this.statModifiers.add(new StatModifier(statType, null, null, valuesByRank));
            return this;
        }

        public Builder inherit(String chipId, float scale) {
            this.inheritedBehaviors.add(new InheritedBehavior(chipId, scale));
            return this;
        }

        public Builder onFirstAppearance(Action action) {
            this.onFirstAppearanceActions.add(action);
            return this;
        }

        public Builder onFaint(Action action) {
            this.onFaintActions.add(action);
            return this;
        }

        public Builder onKill(Action action) {
            this.onKillActions.add(action);
            return this;
        }

        public Builder onCritHit(Action action) {
            this.onCritHitActions.add(action);
            return this;
        }

        public Builder onDodge(Action action) {
            this.onDodgeActions.add(action);
            return this;
        }

        public Builder onDamaged(Action action) {
            this.onDamagedActions.add(action);
            return this;
        }

        public Builder extraInstantCastChance(float... valuesByRank) {
            this.extraInstantCastChanceByRank = valuesByRank;
            return this;
        }

        public Builder chargeSpeedBonus(float... valuesByRank) {
            this.chargeSpeedBonusByRank = valuesByRank;
            return this;
        }

        public Builder projectileSpeedBonus(float... valuesByRank) {
            this.projectileSpeedBonusByRank = valuesByRank;
            return this;
        }

        public Builder passiveBatteryChargeBonus(float... valuesByRank) {
            this.passiveBatteryChargeBonusByRank = valuesByRank;
            return this;
        }

        public Builder tofuChanceBonus(float... valuesByRank) {
            this.tofuChanceBonusByRank = valuesByRank;
            return this;
        }

        public Builder abilityHealBonus(float... valuesByRank) {
            this.abilityHealBonusByRank = valuesByRank;
            return this;
        }

        public Builder activeRegenInterval(int... valuesByRank) {
            this.activeRegenIntervalByRank = valuesByRank;
            return this;
        }

        public Builder activeRegenHeal(int... valuesByRank) {
            this.activeRegenHealByRank = valuesByRank;
            return this;
        }

        public Builder directStunDurationReduction(float... valuesByRank) {
            this.directStunDurationReductionByRank = valuesByRank;
            return this;
        }

        public Builder finisherHpThreshold(float... valuesByRank) {
            this.finisherHpThresholdByRank = valuesByRank;
            return this;
        }

        public Builder finisherDamageBonus(float... valuesByRank) {
            this.finisherDamageBonusByRank = valuesByRank;
            return this;
        }

        public Builder teamMedicBenchHealPct(float... valuesByRank) {
            this.teamMedicBenchHealPctByRank = valuesByRank;
            return this;
        }

        public Builder secondChanceSurviveHp(int... valuesByRank) {
            this.secondChanceSurviveHpByRank = valuesByRank;
            return this;
        }

        public Builder secondChanceDebuffDuration(int... valuesByRank) {
            this.secondChanceDebuffDurationByRank = valuesByRank;
            return this;
        }

        public Builder secondChanceSlowMagnitude(int... valuesByRank) {
            this.secondChanceSlowMagnitudeByRank = valuesByRank;
            return this;
        }

        public Builder fusionCosts(int... valuesByRank) {
            this.fusionCostByRank = valuesByRank;
            return this;
        }

        public ChipSpec build() {
            List<StatModifier> finalStatModifiers = new ArrayList<>(this.statModifiers);
            List<Action> finalFirstAppearance = new ArrayList<>(this.onFirstAppearanceActions);
            List<Action> finalOnFaint = new ArrayList<>(this.onFaintActions);
            List<Action> finalOnKill = new ArrayList<>(this.onKillActions);
            List<Action> finalOnCritHit = new ArrayList<>(this.onCritHitActions);
            List<Action> finalOnDodge = new ArrayList<>(this.onDodgeActions);
            List<Action> finalOnDamaged = new ArrayList<>(this.onDamagedActions);
            float[] mergedInstantCastChance = copyFloatArray(this.extraInstantCastChanceByRank);
            float[] mergedChargeSpeedBonus = copyFloatArray(this.chargeSpeedBonusByRank);
            float[] mergedProjectileSpeedBonus = copyFloatArray(this.projectileSpeedBonusByRank);
            float[] mergedPassiveBatteryChargeBonus = copyFloatArray(this.passiveBatteryChargeBonusByRank);
            float[] mergedTofuChanceBonus = copyFloatArray(this.tofuChanceBonusByRank);
            float[] mergedAbilityHealBonus = copyFloatArray(this.abilityHealBonusByRank);
            int[] mergedActiveRegenInterval = copyIntArray(this.activeRegenIntervalByRank);
            int[] mergedActiveRegenHeal = copyIntArray(this.activeRegenHealByRank);
            float[] mergedDirectStunDurationReduction = copyFloatArray(this.directStunDurationReductionByRank);
            float[] mergedFinisherHpThreshold = copyFloatArray(this.finisherHpThresholdByRank);
            float[] mergedFinisherDamageBonus = copyFloatArray(this.finisherDamageBonusByRank);
            float[] mergedTeamMedicBenchHealPct = copyFloatArray(this.teamMedicBenchHealPctByRank);
            int[] mergedSecondChanceSurviveHp = copyIntArray(this.secondChanceSurviveHpByRank);
            int[] mergedSecondChanceDebuffDuration = copyIntArray(this.secondChanceDebuffDurationByRank);
            int[] mergedSecondChanceSlowMagnitude = copyIntArray(this.secondChanceSlowMagnitudeByRank);

            for (InheritedBehavior inheritedBehavior : inheritedBehaviors) {
                ChipSpec source = ChipRegistry.get(inheritedBehavior.chipId());
                if (source == null) {
                    throw new IllegalStateException("Cannot inherit chip behavior from unknown chip: " + inheritedBehavior.chipId());
                }

                for (StatModifier modifier : source.statModifiers) {
                    finalStatModifiers.add(modifier.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onFirstAppearanceActions) {
                    finalFirstAppearance.add(action.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onFaintActions) {
                    finalOnFaint.add(action.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onKillActions) {
                    finalOnKill.add(action.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onCritHitActions) {
                    finalOnCritHit.add(action.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onDodgeActions) {
                    finalOnDodge.add(action.scaled(inheritedBehavior.scale()));
                }
                for (Action action : source.onDamagedActions) {
                    finalOnDamaged.add(action.scaled(inheritedBehavior.scale()));
                }

                mergedInstantCastChance = mergeScaledFloatArrays(mergedInstantCastChance, source.extraInstantCastChanceByRank,
                        inheritedBehavior.scale());
                mergedChargeSpeedBonus = mergeScaledFloatArrays(mergedChargeSpeedBonus, source.chargeSpeedBonusByRank,
                        inheritedBehavior.scale());
                mergedProjectileSpeedBonus = mergeScaledFloatArrays(mergedProjectileSpeedBonus, source.projectileSpeedBonusByRank,
                        inheritedBehavior.scale());
                mergedPassiveBatteryChargeBonus = mergeScaledFloatArrays(mergedPassiveBatteryChargeBonus,
                        source.passiveBatteryChargeBonusByRank, inheritedBehavior.scale());
                mergedTofuChanceBonus = mergeScaledFloatArrays(mergedTofuChanceBonus, source.tofuChanceBonusByRank,
                        inheritedBehavior.scale());
                mergedAbilityHealBonus = mergeScaledFloatArrays(mergedAbilityHealBonus, source.abilityHealBonusByRank,
                        inheritedBehavior.scale());
                mergedActiveRegenInterval = mergeScaledIntArrays(mergedActiveRegenInterval, source.activeRegenIntervalByRank,
                        inheritedBehavior.scale());
                mergedActiveRegenHeal = mergeScaledIntArrays(mergedActiveRegenHeal, source.activeRegenHealByRank,
                        inheritedBehavior.scale());
                mergedDirectStunDurationReduction = mergeScaledFloatArrays(mergedDirectStunDurationReduction,
                        source.directStunDurationReductionByRank, inheritedBehavior.scale());
                mergedFinisherHpThreshold = mergeScaledFloatArrays(mergedFinisherHpThreshold, source.finisherHpThresholdByRank,
                        inheritedBehavior.scale());
                mergedFinisherDamageBonus = mergeScaledFloatArrays(mergedFinisherDamageBonus, source.finisherDamageBonusByRank,
                        inheritedBehavior.scale());
                mergedTeamMedicBenchHealPct = mergeScaledFloatArrays(mergedTeamMedicBenchHealPct,
                        source.teamMedicBenchHealPctByRank, inheritedBehavior.scale());
                mergedSecondChanceSurviveHp = mergeScaledIntArrays(mergedSecondChanceSurviveHp,
                        source.secondChanceSurviveHpByRank, inheritedBehavior.scale());
                mergedSecondChanceDebuffDuration = mergeScaledIntArrays(mergedSecondChanceDebuffDuration,
                        source.secondChanceDebuffDurationByRank, inheritedBehavior.scale());
                mergedSecondChanceSlowMagnitude = mergeScaledIntArrays(mergedSecondChanceSlowMagnitude,
                        source.secondChanceSlowMagnitudeByRank, inheritedBehavior.scale());
            }

            return new ChipSpec(id, maxRank, finalStatModifiers, finalFirstAppearance, finalOnFaint, finalOnKill, finalOnCritHit,
                    finalOnDodge, finalOnDamaged, mergedInstantCastChance, mergedChargeSpeedBonus, mergedProjectileSpeedBonus,
                    mergedPassiveBatteryChargeBonus, mergedTofuChanceBonus, mergedAbilityHealBonus, mergedActiveRegenInterval,
                    mergedActiveRegenHeal, mergedDirectStunDurationReduction, mergedFinisherHpThreshold,
                    mergedFinisherDamageBonus, mergedTeamMedicBenchHealPct, mergedSecondChanceSurviveHp,
                    mergedSecondChanceDebuffDuration, mergedSecondChanceSlowMagnitude, fusionCostByRank);
        }
    }

    private final String id;
    private final int maxRank;
    private final List<StatModifier> statModifiers;
    private final List<Action> onFirstAppearanceActions;
    private final List<Action> onFaintActions;
    private final List<Action> onKillActions;
    private final List<Action> onCritHitActions;
    private final List<Action> onDodgeActions;
    private final List<Action> onDamagedActions;
    private final float[] extraInstantCastChanceByRank;
    private final float[] chargeSpeedBonusByRank;
    private final float[] projectileSpeedBonusByRank;
    private final float[] passiveBatteryChargeBonusByRank;
    private final float[] tofuChanceBonusByRank;
    private final float[] abilityHealBonusByRank;
    private final int[] activeRegenIntervalByRank;
    private final int[] activeRegenHealByRank;
    private final float[] directStunDurationReductionByRank;
    private final float[] finisherHpThresholdByRank;
    private final float[] finisherDamageBonusByRank;
    private final float[] teamMedicBenchHealPctByRank;
    private final int[] secondChanceSurviveHpByRank;
    private final int[] secondChanceDebuffDurationByRank;
    private final int[] secondChanceSlowMagnitudeByRank;
    private final int[] fusionCostByRank;

    private ChipSpec(String id, int maxRank, List<StatModifier> statModifiers, List<Action> onFirstAppearanceActions,
                     List<Action> onFaintActions, List<Action> onKillActions, List<Action> onCritHitActions,
                     List<Action> onDodgeActions, List<Action> onDamagedActions, float[] extraInstantCastChanceByRank,
                     float[] chargeSpeedBonusByRank, float[] projectileSpeedBonusByRank, float[] passiveBatteryChargeBonusByRank,
                     float[] tofuChanceBonusByRank, float[] abilityHealBonusByRank, int[] activeRegenIntervalByRank,
                     int[] activeRegenHealByRank, float[] directStunDurationReductionByRank, float[] finisherHpThresholdByRank,
                     float[] finisherDamageBonusByRank, float[] teamMedicBenchHealPctByRank, int[] secondChanceSurviveHpByRank,
                     int[] secondChanceDebuffDurationByRank, int[] secondChanceSlowMagnitudeByRank, int[] fusionCostByRank) {
        this.id = id;
        this.maxRank = maxRank;
        this.statModifiers = Collections.unmodifiableList(new ArrayList<>(statModifiers));
        this.onFirstAppearanceActions = Collections.unmodifiableList(new ArrayList<>(onFirstAppearanceActions));
        this.onFaintActions = Collections.unmodifiableList(new ArrayList<>(onFaintActions));
        this.onKillActions = Collections.unmodifiableList(new ArrayList<>(onKillActions));
        this.onCritHitActions = Collections.unmodifiableList(new ArrayList<>(onCritHitActions));
        this.onDodgeActions = Collections.unmodifiableList(new ArrayList<>(onDodgeActions));
        this.onDamagedActions = Collections.unmodifiableList(new ArrayList<>(onDamagedActions));
        this.extraInstantCastChanceByRank = extraInstantCastChanceByRank != null ? extraInstantCastChanceByRank : new float[0];
        this.chargeSpeedBonusByRank = chargeSpeedBonusByRank != null ? chargeSpeedBonusByRank : new float[0];
        this.projectileSpeedBonusByRank = projectileSpeedBonusByRank != null ? projectileSpeedBonusByRank : new float[0];
        this.passiveBatteryChargeBonusByRank = passiveBatteryChargeBonusByRank != null ? passiveBatteryChargeBonusByRank : new float[0];
        this.tofuChanceBonusByRank = tofuChanceBonusByRank != null ? tofuChanceBonusByRank : new float[0];
        this.abilityHealBonusByRank = abilityHealBonusByRank != null ? abilityHealBonusByRank : new float[0];
        this.activeRegenIntervalByRank = activeRegenIntervalByRank != null ? activeRegenIntervalByRank : new int[0];
        this.activeRegenHealByRank = activeRegenHealByRank != null ? activeRegenHealByRank : new int[0];
        this.directStunDurationReductionByRank = directStunDurationReductionByRank != null
                ? directStunDurationReductionByRank : new float[0];
        this.finisherHpThresholdByRank = finisherHpThresholdByRank != null ? finisherHpThresholdByRank : new float[0];
        this.finisherDamageBonusByRank = finisherDamageBonusByRank != null ? finisherDamageBonusByRank : new float[0];
        this.teamMedicBenchHealPctByRank = teamMedicBenchHealPctByRank != null ? teamMedicBenchHealPctByRank : new float[0];
        this.secondChanceSurviveHpByRank = secondChanceSurviveHpByRank != null ? secondChanceSurviveHpByRank : new int[0];
        this.secondChanceDebuffDurationByRank = secondChanceDebuffDurationByRank != null
                ? secondChanceDebuffDurationByRank : new int[0];
        this.secondChanceSlowMagnitudeByRank = secondChanceSlowMagnitudeByRank != null
                ? secondChanceSlowMagnitudeByRank : new int[0];
        this.fusionCostByRank = fusionCostByRank != null ? fusionCostByRank : new int[0];
    }

    public static Builder builder(String id, int maxRank) {
        return new Builder(id, maxRank);
    }

    public String getId() {
        return id;
    }

    public int getMaxRank() {
        return maxRank;
    }

    public int clampRank(int requestedRank) {
        return Math.max(1, Math.min(requestedRank, maxRank));
    }

    public List<StatModifier> getStatModifiers() {
        return statModifiers;
    }

    public List<Action> getOnFirstAppearanceActions() {
        return onFirstAppearanceActions;
    }

    public List<Action> getOnFaintActions() {
        return onFaintActions;
    }

    public List<Action> getOnKillActions() {
        return onKillActions;
    }

    public List<Action> getOnCritHitActions() {
        return onCritHitActions;
    }

    public List<Action> getOnDodgeActions() {
        return onDodgeActions;
    }

    public List<Action> getOnDamagedActions() {
        return onDamagedActions;
    }

    public float getExtraInstantCastChance(int rank) {
        return getRankValue(extraInstantCastChanceByRank, rank);
    }

    public float getChargeSpeedBonus(int rank) {
        return getRankValue(chargeSpeedBonusByRank, rank);
    }

    public float getProjectileSpeedBonus(int rank) {
        return getRankValue(projectileSpeedBonusByRank, rank);
    }

    public float getPassiveBatteryChargeBonus(int rank) {
        return getRankValue(passiveBatteryChargeBonusByRank, rank);
    }

    public float getTofuChanceMultiplier(int rank) {
        return 1.0f + Math.max(0.0f, getRankValue(tofuChanceBonusByRank, rank));
    }

    public float getAbilityHealMultiplier(int rank) {
        return 1.0f + Math.max(0.0f, getRankValue(abilityHealBonusByRank, rank));
    }

    public int getActiveRegenInterval(int rank) {
        return getRankValue(activeRegenIntervalByRank, rank);
    }

    public int getActiveRegenHeal(int rank) {
        return getRankValue(activeRegenHealByRank, rank);
    }

    public float getDirectStunDurationMultiplier(int rank) {
        return Math.max(0.05f, 1.0f - Math.max(0.0f, getRankValue(directStunDurationReductionByRank, rank)));
    }

    public float getFinisherHpThreshold(int rank) {
        return Math.max(0.0f, getRankValue(finisherHpThresholdByRank, rank));
    }

    public float getFinisherDamageMultiplier(int rank) {
        return 1.0f + Math.max(0.0f, getRankValue(finisherDamageBonusByRank, rank));
    }

    public float getTeamMedicBenchHealPct(int rank) {
        return Math.max(0.0f, getRankValue(teamMedicBenchHealPctByRank, rank));
    }

    public int getSecondChanceSurviveHp(int rank) {
        return Math.max(0, getRankValue(secondChanceSurviveHpByRank, rank));
    }

    public int getSecondChanceDebuffDuration(int rank) {
        return Math.max(0, getRankValue(secondChanceDebuffDurationByRank, rank));
    }

    public int getSecondChanceSlowMagnitude(int rank) {
        return Math.max(0, getRankValue(secondChanceSlowMagnitudeByRank, rank));
    }

    public int getFusionCostForCurrentRank(int currentRank) {
        return getRankValue(fusionCostByRank, currentRank);
    }

    private static int getRankValue(int[] valuesByRank, int rank) {
        if (valuesByRank == null || valuesByRank.length == 0) {
            return 0;
        }
        int index = Math.max(0, Math.min(valuesByRank.length - 1, rank - 1));
        return valuesByRank[index];
    }

    private static float getRankValue(float[] valuesByRank, int rank) {
        if (valuesByRank == null || valuesByRank.length == 0) {
            return 0.0f;
        }
        int index = Math.max(0, Math.min(valuesByRank.length - 1, rank - 1));
        return valuesByRank[index];
    }

    private static int[] scaleIntArray(int[] source, float scale) {
        if (source == null) {
            return null;
        }
        int[] result = Arrays.copyOf(source, source.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = Math.round(result[i] * scale);
        }
        return result;
    }

    private static float[] scaleFloatArray(float[] source, float scale) {
        if (source == null) {
            return null;
        }
        float[] result = Arrays.copyOf(source, source.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i] * scale;
        }
        return result;
    }

    private static float[] copyFloatArray(float[] source) {
        return source == null ? new float[0] : Arrays.copyOf(source, source.length);
    }

    private static int[] copyIntArray(int[] source) {
        return source == null ? new int[0] : Arrays.copyOf(source, source.length);
    }

    private static float[] mergeScaledFloatArrays(float[] base, float[] extra, float scale) {
        if (extra == null || extra.length == 0) {
            return base;
        }

        int size = Math.max(base.length, extra.length);
        float[] result = Arrays.copyOf(base, size);
        for (int i = 0; i < extra.length; i++) {
            result[i] += extra[i] * scale;
        }
        return result;
    }

    private static int[] mergeScaledIntArrays(int[] base, int[] extra, float scale) {
        if (extra == null || extra.length == 0) {
            return base;
        }

        int size = Math.max(base.length, extra.length);
        int[] result = Arrays.copyOf(base, size);
        for (int i = 0; i < extra.length; i++) {
            result[i] += Math.round(extra[i] * scale);
        }
        return result;
    }
}
