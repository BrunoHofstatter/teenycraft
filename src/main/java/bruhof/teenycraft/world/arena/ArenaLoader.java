package bruhof.teenycraft.world.arena;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public class ArenaLoader extends SimplePreparableReloadListener<Map<ResourceLocation, ArenaDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, ArenaDefinition> ARENAS = new HashMap<>();
    private static final Pattern GRID_SUFFIX_PATTERN = Pattern.compile(".*_x(-?\\d+)z(-?\\d+)$");

    @Override
    protected Map<ResourceLocation, ArenaDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ArenaDefinition> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "arenas",
                rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                ArenaDefinition definition = parseArena(json);
                result.put(definition.id(), definition);
            } catch (Exception e) {
                LOGGER.error("Failed to load arena definition {}", entry.getKey(), e);
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, ArenaDefinition> result, ResourceManager resourceManager, ProfilerFiller profiler) {
        ARENAS.clear();
        ARENAS.putAll(result);
        LOGGER.info("Loaded {} arena definitions", ARENAS.size());
    }

    public static ArenaDefinition getArena(ResourceLocation id) {
        return ARENAS.get(id);
    }

    public static Set<ResourceLocation> getLoadedArenaIds() {
        return new LinkedHashSet<>(ARENAS.keySet());
    }

    public static ResourceLocation getDefaultArenaId() {
        return ARENAS.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .findFirst()
                .orElse(null);
    }

    private ArenaDefinition parseArena(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "id"));
        List<ArenaTemplateDefinition> templates = readTemplates(json);
        Vec3 playerSpawn = readVec3(json, "player_spawn");
        Vec3 opponentSpawn = readVec3(json, "opponent_spawn");
        float playerYaw = GsonHelper.getAsFloat(json, "player_yaw", 0.0f);
        float playerPitch = GsonHelper.getAsFloat(json, "player_pitch", 0.0f);
        float opponentYaw = GsonHelper.getAsFloat(json, "opponent_yaw", 180.0f);
        float opponentPitch = GsonHelper.getAsFloat(json, "opponent_pitch", 0.0f);
        BlockPos clearPadding = readBlockPos(json, "clear_padding", BlockPos.ZERO);
        List<String> tags = readStringList(json, "tags");
        List<ArenaPickupSpawnerDefinition> pickupSpawners = readPickupSpawners(json);

        return new ArenaDefinition(
                id,
                templates,
                playerSpawn,
                playerYaw,
                playerPitch,
                opponentSpawn,
                opponentYaw,
                opponentPitch,
                clearPadding,
                tags,
                pickupSpawners
        );
    }

    private List<ArenaTemplateDefinition> readTemplates(JsonObject json) {
        JsonElement templateElement = json.get("template");
        if (templateElement == null) {
            throw new IllegalArgumentException("Arena is missing template.");
        }

        if (templateElement.isJsonPrimitive()) {
            return List.of(new ArenaTemplateDefinition(
                    ResourceLocation.parse(templateElement.getAsString()),
                    0,
                    0
            ));
        }

        if (!templateElement.isJsonArray()) {
            throw new IllegalArgumentException("Arena template must be a string or array.");
        }

        List<ArenaTemplateDefinition> templates = new ArrayList<>();
        JsonArray array = templateElement.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element instanceof JsonPrimitive primitive && primitive.isString()) {
                ResourceLocation templateId = ResourceLocation.parse(primitive.getAsString());
                int[] grid = parseGridFromTemplateId(templateId, i);
                templates.add(new ArenaTemplateDefinition(templateId, grid[0], grid[1]));
                continue;
            }

            JsonObject templateJson = element.getAsJsonObject();
            ResourceLocation templateId = ResourceLocation.parse(GsonHelper.getAsString(templateJson, "id"));
            int[] grid = parseGridFromTemplateId(templateId, i);
            if (templateJson.has("grid")) {
                JsonArray gridArray = GsonHelper.getAsJsonArray(templateJson, "grid");
                if (gridArray.size() != 2) {
                    throw new IllegalArgumentException("Expected 2 integer values for template grid");
                }
                grid[0] = gridArray.get(0).getAsInt();
                grid[1] = gridArray.get(1).getAsInt();
            }
            templates.add(new ArenaTemplateDefinition(templateId, grid[0], grid[1]));
        }

        return templates;
    }

    private int[] parseGridFromTemplateId(ResourceLocation templateId, int fallbackIndex) {
        Matcher matcher = GRID_SUFFIX_PATTERN.matcher(templateId.getPath());
        if (matcher.matches()) {
            return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            };
        }
        return new int[] {fallbackIndex, 0};
    }

    private List<ArenaPickupSpawnerDefinition> readPickupSpawners(JsonObject json) {
        if (!json.has("pickups")) {
            return List.of();
        }

        List<ArenaPickupSpawnerDefinition> result = new ArrayList<>();
        JsonArray pickupArray = GsonHelper.getAsJsonArray(json, "pickups");
        for (int i = 0; i < pickupArray.size(); i++) {
            JsonObject pickupJson = pickupArray.get(i).getAsJsonObject();
            String id = GsonHelper.getAsString(pickupJson, "id", "pickup_" + i);
            List<Vec3> spots = readVec3List(pickupJson, "spots");
            ArenaSelectionMode spotMode = ArenaSelectionMode.fromSerialized(GsonHelper.getAsString(pickupJson, "spot_mode", "cycle"));
            List<ArenaPickupVariantDefinition> variants = readPickupVariants(pickupJson);
            ArenaSelectionMode variantMode = ArenaSelectionMode.fromSerialized(GsonHelper.getAsString(pickupJson, "variant_mode", "cycle"));
            int firstSpawnDelayTicks = GsonHelper.getAsInt(pickupJson, "first_spawn_delay_ticks", 0);
            int cooldownTicks = GsonHelper.getAsInt(pickupJson, "cooldown_ticks", 0);

            result.add(new ArenaPickupSpawnerDefinition(
                    id,
                    spots,
                    spotMode,
                    variants,
                    variantMode,
                    firstSpawnDelayTicks,
                    cooldownTicks
            ));
        }

        return result;
    }

    private List<ArenaPickupVariantDefinition> readPickupVariants(JsonObject pickupJson) {
        List<ArenaPickupVariantDefinition> result = new ArrayList<>();
        JsonArray variantArray = GsonHelper.getAsJsonArray(pickupJson, "variants");
        for (int i = 0; i < variantArray.size(); i++) {
            JsonObject variantJson = variantArray.get(i).getAsJsonObject();
            ArenaPickupType type = ArenaPickupType.fromSerialized(GsonHelper.getAsString(variantJson, "type"));

            switch (type) {
                case HEAL, MANA, AMP -> result.add(new ArenaPickupVariantDefinition(
                        type,
                        GsonHelper.getAsInt(variantJson, "amount"),
                        0,
                        0,
                        null,
                        0.0d,
                        null,
                        List.of()
                ));
                case SPEED -> result.add(new ArenaPickupVariantDefinition(
                        type,
                        0,
                        GsonHelper.getAsInt(variantJson, "level"),
                        GsonHelper.getAsInt(variantJson, "duration_ticks"),
                        null,
                        0.0d,
                        null,
                        List.of()
                ));
                case LAUNCH -> result.add(new ArenaPickupVariantDefinition(
                        type,
                        0,
                        0,
                        GsonHelper.getAsInt(variantJson, "duration_ticks", 0),
                        readVec3(variantJson, "destination"),
                        GsonHelper.getAsDouble(variantJson, "arc_height"),
                        null,
                        List.of()
                ));
                case WALL -> result.add(new ArenaPickupVariantDefinition(
                        type,
                        0,
                        0,
                        GsonHelper.getAsInt(variantJson, "duration_ticks"),
                        null,
                        0.0d,
                        ResourceLocation.parse(GsonHelper.getAsString(variantJson, "block")),
                        readBlockPosList(variantJson, "blocks")
                ));
            }
        }

        return result;
    }

    private Vec3 readVec3(JsonObject json, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        if (array.size() != 3) {
            throw new IllegalArgumentException("Expected 3 values for " + key);
        }

        return new Vec3(
                array.get(0).getAsDouble(),
                array.get(1).getAsDouble(),
                array.get(2).getAsDouble()
        );
    }

    private List<Vec3> readVec3List(JsonObject json, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        List<Vec3> result = new ArrayList<>();
        array.forEach(element -> {
            JsonArray entry = element.getAsJsonArray();
            if (entry.size() != 3) {
                throw new IllegalArgumentException("Expected 3 values for entries in " + key);
            }
            result.add(new Vec3(
                    entry.get(0).getAsDouble(),
                    entry.get(1).getAsDouble(),
                    entry.get(2).getAsDouble()
            ));
        });
        return result;
    }

    private BlockPos readBlockPos(JsonObject json, String key, BlockPos fallback) {
        if (!json.has(key)) {
            return fallback;
        }

        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        if (array.size() != 3) {
            throw new IllegalArgumentException("Expected 3 integer values for " + key);
        }

        return new BlockPos(
                array.get(0).getAsInt(),
                array.get(1).getAsInt(),
                array.get(2).getAsInt()
        );
    }

    private List<BlockPos> readBlockPosList(JsonObject json, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        List<BlockPos> result = new ArrayList<>();
        array.forEach(element -> {
            JsonArray entry = element.getAsJsonArray();
            if (entry.size() != 3) {
                throw new IllegalArgumentException("Expected 3 integer values for entries in " + key);
            }
            result.add(new BlockPos(
                    entry.get(0).getAsInt(),
                    entry.get(1).getAsInt(),
                    entry.get(2).getAsInt()
            ));
        });
        return result;
    }

    private List<String> readStringList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        GsonHelper.getAsJsonArray(json, key).forEach(element -> result.add(element.getAsString()));
        return result;
    }
}
