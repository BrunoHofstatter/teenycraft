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

        private StatModifier(ChipStatType statType, int[] flatByRank, float[] basePctByRank) {
            this.statType = statType;
            this.flatByRank = flatByRank;
            this.basePctByRank = basePctByRank;
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

        private StatModifier scaled(float scale) {
            return new StatModifier(statType, scaleIntArray(flatByRank, scale), scaleFloatArray(basePctByRank, scale));
        }
    }

    public static class Action {
        public enum Type {
            APPLY_EFFECT_SELF,
            ADD_MANA_SELF,
            ADD_BATTERY_SELF,
            HEAL_SELF_MAX_HP_PCT,
            DEAL_DAMAGE_OPPONENT,
            SUMMON_PET_SELF
        }

        private final Type type;
        private final String effectId;
        private final int[] amountByRank;
        private final int[] durationByRank;
        private final float[] percentByRank;
        private final boolean groupDamage;

        private Action(Type type, String effectId, int[] amountByRank, int[] durationByRank, float[] percentByRank, boolean groupDamage) {
            this.type = type;
            this.effectId = effectId;
            this.amountByRank = amountByRank;
            this.durationByRank = durationByRank;
            this.percentByRank = percentByRank;
            this.groupDamage = groupDamage;
        }

        public static Action applyEffectSelf(String effectId, int[] durationByRank, int[] amountByRank) {
            return new Action(Type.APPLY_EFFECT_SELF, effectId, amountByRank, durationByRank, null, false);
        }

        public static Action addManaSelf(int[] amountByRank) {
            return new Action(Type.ADD_MANA_SELF, null, amountByRank, null, null, false);
        }

        public static Action addBatterySelf(int[] amountByRank) {
            return new Action(Type.ADD_BATTERY_SELF, null, amountByRank, null, null, false);
        }

        public static Action healSelfMaxHpPct(float[] percentByRank) {
            return new Action(Type.HEAL_SELF_MAX_HP_PCT, null, null, null, percentByRank, false);
        }

        public static Action dealDamageOpponent(int[] amountByRank, boolean groupDamage) {
            return new Action(Type.DEAL_DAMAGE_OPPONENT, null, amountByRank, null, null, groupDamage);
        }

        public static Action summonPetSelf(int[] durationByRank, int[] amountByRank) {
            return new Action(Type.SUMMON_PET_SELF, null, amountByRank, durationByRank, null, false);
        }

        public Type getType() {
            return type;
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

        public float getPercent(int rank) {
            return getRankValue(percentByRank, rank);
        }

        public boolean isGroupDamage() {
            return groupDamage;
        }

        private Action scaled(float scale) {
            return new Action(type, effectId, scaleIntArray(amountByRank, scale), scaleIntArray(durationByRank, scale),
                    scaleFloatArray(percentByRank, scale), groupDamage);
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
        private final List<InheritedBehavior> inheritedBehaviors = new ArrayList<>();
        private float[] extraInstantCastChanceByRank = new float[0];
        private int[] fusionCostByRank = new int[0];

        private Builder(String id, int maxRank) {
            this.id = id;
            this.maxRank = Math.max(1, maxRank);
        }

        public Builder addFlatStat(ChipStatType statType, int... valuesByRank) {
            this.statModifiers.add(new StatModifier(statType, valuesByRank, null));
            return this;
        }

        public Builder addBasePctStat(ChipStatType statType, float... valuesByRank) {
            this.statModifiers.add(new StatModifier(statType, null, valuesByRank));
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

        public Builder extraInstantCastChance(float... valuesByRank) {
            this.extraInstantCastChanceByRank = valuesByRank;
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
            float[] mergedInstantCastChance = copyFloatArray(this.extraInstantCastChanceByRank);

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

                mergedInstantCastChance = mergeScaledFloatArrays(mergedInstantCastChance, source.extraInstantCastChanceByRank,
                        inheritedBehavior.scale());
            }

            return new ChipSpec(id, maxRank, finalStatModifiers, finalFirstAppearance, finalOnFaint, finalOnKill, finalOnCritHit,
                    mergedInstantCastChance, fusionCostByRank);
        }
    }

    private final String id;
    private final int maxRank;
    private final List<StatModifier> statModifiers;
    private final List<Action> onFirstAppearanceActions;
    private final List<Action> onFaintActions;
    private final List<Action> onKillActions;
    private final List<Action> onCritHitActions;
    private final float[] extraInstantCastChanceByRank;
    private final int[] fusionCostByRank;

    private ChipSpec(String id, int maxRank, List<StatModifier> statModifiers, List<Action> onFirstAppearanceActions,
                     List<Action> onFaintActions, List<Action> onKillActions, List<Action> onCritHitActions,
                     float[] extraInstantCastChanceByRank, int[] fusionCostByRank) {
        this.id = id;
        this.maxRank = maxRank;
        this.statModifiers = Collections.unmodifiableList(new ArrayList<>(statModifiers));
        this.onFirstAppearanceActions = Collections.unmodifiableList(new ArrayList<>(onFirstAppearanceActions));
        this.onFaintActions = Collections.unmodifiableList(new ArrayList<>(onFaintActions));
        this.onKillActions = Collections.unmodifiableList(new ArrayList<>(onKillActions));
        this.onCritHitActions = Collections.unmodifiableList(new ArrayList<>(onCritHitActions));
        this.extraInstantCastChanceByRank = extraInstantCastChanceByRank != null ? extraInstantCastChanceByRank : new float[0];
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

    public float getExtraInstantCastChance(int rank) {
        return getRankValue(extraInstantCastChanceByRank, rank);
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
}
