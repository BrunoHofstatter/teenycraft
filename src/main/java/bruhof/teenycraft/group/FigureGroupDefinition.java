package bruhof.teenycraft.group;

import java.util.List;
import java.util.Set;

public record FigureGroupDefinition(
        String id,
        String name,
        int priority,
        Set<String> figureIds,
        List<String> comboEffectIds
) {
    public FigureGroupDefinition {
        figureIds = Set.copyOf(figureIds);
        comboEffectIds = List.copyOf(comboEffectIds);
    }
}
