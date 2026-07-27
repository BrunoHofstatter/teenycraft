package bruhof.teenycraft.accessory;

import java.util.Map;
import java.util.Set;

public record AccessoryProgressSnapshot(
        String accessoryId,
        int tier,
        Map<AccessoryContribution.Type, Long> totals,
        Map<AccessoryContribution.Type, Set<Integer>> targets,
        Map<AccessoryContribution.Type, Set<String>> details,
        Map<AccessoryContribution.Type, Map<Integer, Long>> targetTotals,
        Map<AccessoryContribution.Type, Map<Integer, Set<String>>> targetDetails
) {
    public long total(AccessoryContribution.Type type) {
        return totals.getOrDefault(type, 0L);
    }

    public int distinctTargets(AccessoryContribution.Type type) {
        return targets.getOrDefault(type, Set.of()).size();
    }

    public boolean hasDetail(AccessoryContribution.Type type, String detail) {
        return details.getOrDefault(type, Set.of()).contains(detail);
    }

    public long totalForTarget(AccessoryContribution.Type type, int targetFigureIndex) {
        return targetTotals.getOrDefault(type, Map.of()).getOrDefault(targetFigureIndex, 0L);
    }

    public int distinctDetailsForTarget(AccessoryContribution.Type type, int targetFigureIndex) {
        return targetDetails.getOrDefault(type, Map.of()).getOrDefault(targetFigureIndex, Set.of()).size();
    }

    public long targetsMeetingTotal(AccessoryContribution.Type type, long required) {
        return targetTotals.getOrDefault(type, Map.of()).values().stream().filter(value -> value >= required).count();
    }

    public long targetsMeetingDistinctDetails(AccessoryContribution.Type type, int required) {
        return targetDetails.getOrDefault(type, Map.of()).values().stream().filter(values -> values.size() >= required).count();
    }

    public long targetsMeetingTotalAndPresent(AccessoryContribution.Type totalType, long required,
                                              AccessoryContribution.Type presenceType) {
        Set<Integer> present = targets.getOrDefault(presenceType, Set.of());
        return targetTotals.getOrDefault(totalType, Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() >= required && present.contains(entry.getKey()))
                .count();
    }
}
