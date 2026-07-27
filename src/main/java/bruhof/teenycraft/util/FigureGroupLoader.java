package bruhof.teenycraft.util;

import bruhof.teenycraft.group.FigureGroupDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FigureGroupLoader extends SimplePreparableReloadListener<FigureGroupLoader.LoadedGroups> {
    private static final Gson GSON = new Gson();
    private static final Comparator<FigureGroupDefinition> DEFAULT_ORDER =
            Comparator.comparingInt(FigureGroupDefinition::priority).reversed()
                    .thenComparing(FigureGroupDefinition::id);

    private static volatile Map<String, FigureGroupDefinition> GROUPS_BY_ID = Map.of();
    private static volatile Map<String, Set<String>> GROUP_IDS_BY_FIGURE_ID = Map.of();

    @Override
    protected LoadedGroups prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, FigureGroupDefinition> groupsById = new LinkedHashMap<>();
        Map<String, Set<String>> groupIdsByFigure = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "figure_groups",
                id -> id.getNamespace().equals("teenycraft") && id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                FigureGroupDefinition definition = parseDefinition(GSON.fromJson(reader, JsonObject.class));
                if (groupsById.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalStateException("Duplicate figure group id: " + definition.id());
                }
                for (String figureId : definition.figureIds()) {
                    groupIdsByFigure.computeIfAbsent(figureId, ignored -> new LinkedHashSet<>())
                            .add(definition.id());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load figure group " + entry.getKey(), e);
            }
        }

        Map<String, Set<String>> immutableReverse = new LinkedHashMap<>();
        groupIdsByFigure.forEach((figureId, groupIds) -> immutableReverse.put(figureId, Set.copyOf(groupIds)));
        return new LoadedGroups(Map.copyOf(groupsById), Map.copyOf(immutableReverse));
    }

    @Override
    protected void apply(LoadedGroups result, ResourceManager resourceManager, ProfilerFiller profiler) {
        GROUPS_BY_ID = result.groupsById();
        GROUP_IDS_BY_FIGURE_ID = result.groupIdsByFigureId();
    }

    public static FigureGroupDefinition parseDefinition(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        int priority = json.get("priority").getAsInt();
        Set<String> figures = readStringSet(json.getAsJsonArray("figures"));
        List<String> effects = readStringList(json.getAsJsonArray("combo_effects"));
        return new FigureGroupDefinition(id, name, priority, figures, effects);
    }

    public static FigureGroupDefinition getGroup(String groupId) {
        return GROUPS_BY_ID.get(groupId);
    }

    public static Map<String, FigureGroupDefinition> getGroupsById() {
        return GROUPS_BY_ID;
    }

    public static Set<String> getGroupIdsForFigure(String figureId) {
        return GROUP_IDS_BY_FIGURE_ID.getOrDefault(figureId, Set.of());
    }

    public static List<FigureGroupDefinition> getGroupsForFigure(String figureId) {
        return getGroupIdsForFigure(figureId).stream()
                .map(GROUPS_BY_ID::get)
                .filter(java.util.Objects::nonNull)
                .sorted(DEFAULT_ORDER)
                .toList();
    }

    public static List<FigureGroupDefinition> getSharedGroups(String firstFigureId, String secondFigureId) {
        if (firstFigureId == null || secondFigureId == null || firstFigureId.isBlank() || secondFigureId.isBlank()) {
            return List.of();
        }
        Set<String> firstGroups = getGroupIdsForFigure(firstFigureId);
        Set<String> secondGroups = getGroupIdsForFigure(secondFigureId);
        if (firstGroups.isEmpty() || secondGroups.isEmpty()) {
            return List.of();
        }
        return firstGroups.stream()
                .filter(secondGroups::contains)
                .map(GROUPS_BY_ID::get)
                .filter(java.util.Objects::nonNull)
                .sorted(DEFAULT_ORDER)
                .toList();
    }

    private static Set<String> readStringSet(JsonArray array) {
        return new LinkedHashSet<>(readStringList(array));
    }

    private static List<String> readStringList(JsonArray array) {
        List<String> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (JsonElement element : array) {
            result.add(element.getAsString());
        }
        return result;
    }

    protected record LoadedGroups(Map<String, FigureGroupDefinition> groupsById,
                                  Map<String, Set<String>> groupIdsByFigureId) {
    }
}
