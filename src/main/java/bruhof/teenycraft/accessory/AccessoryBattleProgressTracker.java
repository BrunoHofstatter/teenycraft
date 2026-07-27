package bruhof.teenycraft.accessory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccessoryBattleProgressTracker {
    private final MetricStore battle = new MetricStore();
    private final MetricStore activation = new MetricStore();
    private String battleAccessoryId = "";
    private int battleTier = AccessoryProgression.MIN_TIER;
    private String activationAccessoryId = "";
    private int activationTier = AccessoryProgression.MIN_TIER;

    public void beginBattle() {
        reset();
    }

    public void beginActivation(String accessoryId, int tier) {
        activation.clear();
        activationAccessoryId = accessoryId != null ? accessoryId : "";
        activationTier = AccessoryProgression.clampTier(tier);
        if (battleAccessoryId.isEmpty()) {
            battleAccessoryId = activationAccessoryId;
            battleTier = activationTier;
        }
    }

    public void record(AccessoryContribution contribution) {
        if (contribution == null || contribution.amount() <= 0 || contribution.accessoryId().isBlank()) {
            return;
        }
        battle.record(contribution);
        if (contribution.accessoryId().equals(activationAccessoryId)) {
            activation.record(contribution);
        }
    }

    public AccessoryProgressSnapshot endActivation() {
        AccessoryProgressSnapshot snapshot = activation.snapshot(activationAccessoryId, activationTier);
        activation.clear();
        activationAccessoryId = "";
        activationTier = AccessoryProgression.MIN_TIER;
        return snapshot;
    }

    public AccessoryProgressSnapshot battleSnapshot() {
        return battle.snapshot(battleAccessoryId, battleTier);
    }

    public void reset() {
        battle.clear();
        activation.clear();
        battleAccessoryId = "";
        battleTier = AccessoryProgression.MIN_TIER;
        activationAccessoryId = "";
        activationTier = AccessoryProgression.MIN_TIER;
    }

    private static final class MetricStore {
        private final EnumMap<AccessoryContribution.Type, Long> totals = new EnumMap<>(AccessoryContribution.Type.class);
        private final EnumMap<AccessoryContribution.Type, Set<Integer>> targets = new EnumMap<>(AccessoryContribution.Type.class);
        private final EnumMap<AccessoryContribution.Type, Set<String>> details = new EnumMap<>(AccessoryContribution.Type.class);
        private final EnumMap<AccessoryContribution.Type, Map<Integer, Long>> targetTotals = new EnumMap<>(AccessoryContribution.Type.class);
        private final EnumMap<AccessoryContribution.Type, Map<Integer, Set<String>>> targetDetails = new EnumMap<>(AccessoryContribution.Type.class);

        private void record(AccessoryContribution contribution) {
            totals.merge(contribution.type(), contribution.amount(), AccessoryBattleProgressTracker::saturatedAdd);
            if (contribution.targetFigureIndex() >= 0) {
                targets.computeIfAbsent(contribution.type(), ignored -> new HashSet<>()).add(contribution.targetFigureIndex());
                targetTotals.computeIfAbsent(contribution.type(), ignored -> new java.util.HashMap<>())
                        .merge(contribution.targetFigureIndex(), contribution.amount(), AccessoryBattleProgressTracker::saturatedAdd);
            }
            if (!contribution.detail().isEmpty()) {
                details.computeIfAbsent(contribution.type(), ignored -> new HashSet<>()).add(contribution.detail());
                if (contribution.targetFigureIndex() >= 0) {
                    targetDetails.computeIfAbsent(contribution.type(), ignored -> new java.util.HashMap<>())
                            .computeIfAbsent(contribution.targetFigureIndex(), ignored -> new HashSet<>())
                            .add(contribution.detail());
                }
            }
        }

        private AccessoryProgressSnapshot snapshot(String accessoryId, int tier) {
            EnumMap<AccessoryContribution.Type, Long> totalsCopy = new EnumMap<>(totals);
            EnumMap<AccessoryContribution.Type, Set<Integer>> targetsCopy = new EnumMap<>(AccessoryContribution.Type.class);
            targets.forEach((type, values) -> targetsCopy.put(type, Set.copyOf(values)));
            EnumMap<AccessoryContribution.Type, Set<String>> detailsCopy = new EnumMap<>(AccessoryContribution.Type.class);
            details.forEach((type, values) -> detailsCopy.put(type, Set.copyOf(values)));
            EnumMap<AccessoryContribution.Type, Map<Integer, Long>> targetTotalsCopy = new EnumMap<>(AccessoryContribution.Type.class);
            targetTotals.forEach((type, values) -> targetTotalsCopy.put(type, Map.copyOf(values)));
            EnumMap<AccessoryContribution.Type, Map<Integer, Set<String>>> targetDetailsCopy = new EnumMap<>(AccessoryContribution.Type.class);
            targetDetails.forEach((type, values) -> {
                Map<Integer, Set<String>> copy = new java.util.HashMap<>();
                values.forEach((target, targetValues) -> copy.put(target, Set.copyOf(targetValues)));
                targetDetailsCopy.put(type, Map.copyOf(copy));
            });
            return new AccessoryProgressSnapshot(accessoryId, tier, Map.copyOf(totalsCopy), Map.copyOf(targetsCopy),
                    Map.copyOf(detailsCopy), Map.copyOf(targetTotalsCopy), Map.copyOf(targetDetailsCopy));
        }

        private void clear() {
            totals.clear();
            targets.clear();
            details.clear();
            targetTotals.clear();
            targetDetails.clear();
        }
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
