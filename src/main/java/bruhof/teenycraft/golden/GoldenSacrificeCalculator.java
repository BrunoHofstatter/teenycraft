package bruhof.teenycraft.golden;

import bruhof.teenycraft.TeenyBalance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GoldenSacrificeCalculator {
    private GoldenSacrificeCalculator() {
    }

    public enum Affinity {
        EXACT,
        GROUP,
        CLASS,
        UNRELATED
    }

    public interface GroupAccess {
        Set<String> groupsForFigure(String figureId);

        Set<String> membersForGroup(String groupId);

        String nameForGroup(String groupId);
    }

    public record FigureData(String id, String figureClass) {
        public FigureData {
            id = id == null ? "" : id;
            figureClass = figureClass == null ? "" : figureClass;
        }
    }

    public record DonorBreakdown(
            int donorIndex,
            FigureData donor,
            Affinity affinity,
            int basePoints,
            List<String> sharedGroupIds
    ) {
        public DonorBreakdown {
            sharedGroupIds = List.copyOf(sharedGroupIds);
        }
    }

    public record GroupBreakdown(
            String groupId,
            String groupName,
            int distinctSacrificeCount,
            int memberCount,
            boolean complete,
            int normalBonus,
            int completionBonus,
            int awardedBonus
    ) {
    }

    public record Result(
            List<DonorBreakdown> donors,
            int basePoints,
            int exactCount,
            int exactBonus,
            int classComboCount,
            int classBonus,
            List<GroupBreakdown> groupBreakdowns,
            GroupBreakdown awardedGroup,
            int totalPoints
    ) {
        public Result {
            donors = List.copyOf(donors);
            groupBreakdowns = List.copyOf(groupBreakdowns);
        }
    }

    public static Result calculate(FigureData target, List<FigureData> sacrifices, GroupAccess groups) {
        if (target == null || target.id().isBlank()) {
            return empty();
        }
        if (sacrifices == null || sacrifices.isEmpty()) {
            return empty();
        }
        if (sacrifices.size() > TeenyBalance.GOLDEN_MAX_SACRIFICES) {
            throw new IllegalArgumentException("A golden feeding accepts at most "
                    + TeenyBalance.GOLDEN_MAX_SACRIFICES + " sacrifices");
        }

        GroupAccess safeGroups = groups == null ? EmptyGroupAccess.INSTANCE : groups;
        Set<String> targetGroups = Set.copyOf(safeGroups.groupsForFigure(target.id()));
        List<DonorBreakdown> donorBreakdowns = new ArrayList<>(sacrifices.size());
        int basePoints = 0;
        int exactCount = 0;
        Map<String, Integer> classCountsByFigureId = new HashMap<>();

        for (int index = 0; index < sacrifices.size(); index++) {
            FigureData donor = sacrifices.get(index);
            List<String> sharedGroups = sharedGroups(targetGroups, safeGroups.groupsForFigure(donor.id()));
            Affinity affinity = classify(target, donor, sharedGroups);
            int points = basePoints(affinity);
            donorBreakdowns.add(new DonorBreakdown(index, donor, affinity, points, sharedGroups));
            basePoints += points;

            if (affinity == Affinity.EXACT) {
                exactCount++;
            } else if (affinity == Affinity.CLASS) {
                classCountsByFigureId.merge(donor.id(), 1, Integer::sum);
            }
        }

        int classComboCount = classCountsByFigureId.values().stream()
                .mapToInt(count -> Math.min(count, TeenyBalance.GOLDEN_CLASS_COMBO_MAX_COPIES_PER_FIGURE_ID))
                .sum();
        int exactBonus = TeenyBalance.getGoldenExactComboBonus(exactCount);
        int classBonus = TeenyBalance.getGoldenClassComboBonus(classComboCount);
        List<GroupBreakdown> groupBreakdowns = buildGroupBreakdowns(
                target, targetGroups, donorBreakdowns, safeGroups);
        GroupBreakdown awardedGroup = groupBreakdowns.stream()
                .filter(group -> group.awardedBonus() > 0)
                .max(Comparator.comparingInt(GroupBreakdown::awardedBonus)
                        .thenComparingInt(GroupBreakdown::distinctSacrificeCount)
                        .thenComparing(GroupBreakdown::groupId, Comparator.reverseOrder()))
                .orElse(null);
        int groupBonus = awardedGroup == null ? 0 : awardedGroup.awardedBonus();
        int totalPoints = basePoints + exactBonus + classBonus + groupBonus;

        return new Result(donorBreakdowns, basePoints, exactCount, exactBonus,
                classComboCount, classBonus, groupBreakdowns, awardedGroup, totalPoints);
    }

    private static Result empty() {
        return new Result(List.of(), 0, 0, 0, 0, 0, List.of(), null, 0);
    }

    private static Affinity classify(FigureData target, FigureData donor, List<String> sharedGroups) {
        if (target.id().equals(donor.id())) {
            return Affinity.EXACT;
        }
        if (!sharedGroups.isEmpty()) {
            return Affinity.GROUP;
        }
        if (isRealClass(target.figureClass()) && target.figureClass().equalsIgnoreCase(donor.figureClass())) {
            return Affinity.CLASS;
        }
        return Affinity.UNRELATED;
    }

    private static boolean isRealClass(String figureClass) {
        return figureClass != null && !figureClass.isBlank() && !figureClass.equalsIgnoreCase("none");
    }

    private static int basePoints(Affinity affinity) {
        return switch (affinity) {
            case EXACT -> TeenyBalance.GOLDEN_BASE_EXACT_POINTS;
            case GROUP -> TeenyBalance.GOLDEN_BASE_GROUP_POINTS;
            case CLASS -> TeenyBalance.GOLDEN_BASE_CLASS_POINTS;
            case UNRELATED -> TeenyBalance.GOLDEN_BASE_UNRELATED_POINTS;
        };
    }

    private static List<String> sharedGroups(Set<String> targetGroups, Set<String> donorGroups) {
        if (targetGroups.isEmpty() || donorGroups == null || donorGroups.isEmpty()) {
            return List.of();
        }
        return targetGroups.stream().filter(donorGroups::contains).sorted().toList();
    }

    private static List<GroupBreakdown> buildGroupBreakdowns(FigureData target,
                                                               Set<String> targetGroups,
                                                               List<DonorBreakdown> donors,
                                                               GroupAccess groups) {
        Map<String, Set<String>> distinctIdsByGroup = new LinkedHashMap<>();
        for (String groupId : targetGroups.stream().sorted().toList()) {
            distinctIdsByGroup.put(groupId, new HashSet<>());
        }
        for (DonorBreakdown donor : donors) {
            if (donor.affinity() != Affinity.GROUP) {
                continue;
            }
            for (String groupId : donor.sharedGroupIds()) {
                distinctIdsByGroup.computeIfAbsent(groupId, ignored -> new HashSet<>())
                        .add(donor.donor().id());
            }
        }

        List<GroupBreakdown> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : distinctIdsByGroup.entrySet()) {
            String groupId = entry.getKey();
            Set<String> distinctDonors = entry.getValue();
            if (distinctDonors.isEmpty()) {
                continue;
            }
            Set<String> members = groups.membersForGroup(groupId);
            Set<String> represented = new HashSet<>(distinctDonors);
            represented.add(target.id());
            boolean complete = members != null
                    && members.size() >= TeenyBalance.GOLDEN_MIN_COMPLETE_GROUP_SIZE
                    && represented.containsAll(members);
            int distinctCount = distinctDonors.size();
            int normalBonus = TeenyBalance.getGoldenGroupComboBonus(distinctCount);
            int completionBonus = complete
                    ? distinctCount * TeenyBalance.GOLDEN_COMPLETE_GROUP_BONUS_PER_MEMBER
                    : 0;
            int awardedBonus = Math.max(normalBonus, completionBonus);
            result.add(new GroupBreakdown(groupId, groups.nameForGroup(groupId), distinctCount,
                    members == null ? 0 : members.size(), complete, normalBonus, completionBonus, awardedBonus));
        }
        result.sort(Comparator.comparing(GroupBreakdown::groupId));
        return result;
    }

    private enum EmptyGroupAccess implements GroupAccess {
        INSTANCE;

        @Override
        public Set<String> groupsForFigure(String figureId) {
            return Set.of();
        }

        @Override
        public Set<String> membersForGroup(String groupId) {
            return Set.of();
        }

        @Override
        public String nameForGroup(String groupId) {
            return groupId;
        }
    }
}
