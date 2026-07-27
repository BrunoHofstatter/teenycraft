package bruhof.teenycraft.group;

import bruhof.teenycraft.util.FigureGroupLoader;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

public final class FigureGroupResolver {
    private FigureGroupResolver() {
    }

    public static Optional<FigureGroupDefinition> resolve(String firstFigureId,
                                                          String secondFigureId,
                                                          String selectedGroupId) {
        List<FigureGroupDefinition> eligible = FigureGroupLoader.getSharedGroups(firstFigureId, secondFigureId);
        return resolve(eligible, selectedGroupId);
    }

    public static Optional<FigureGroupDefinition> resolve(List<FigureGroupDefinition> eligible,
                                                          String selectedGroupId) {
        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        if (selectedGroupId != null && !selectedGroupId.isBlank()) {
            for (FigureGroupDefinition group : eligible) {
                if (group.id().equals(selectedGroupId)) {
                    return Optional.of(group);
                }
            }
        }

        return eligible.stream()
                .sorted(Comparator.comparingInt(FigureGroupDefinition::priority).reversed()
                        .thenComparing(FigureGroupDefinition::id))
                .findFirst();
    }
}
