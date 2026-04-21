package bruhof.teenycraft.world.arena;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.Set;

public class ArenaLoader extends SimplePreparableReloadListener<Map<ResourceLocation, ArenaDefinition>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, ArenaDefinition> ARENAS = new HashMap<>();

    @Override
    protected Map<ResourceLocation, ArenaDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ArenaDefinition> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "arenas",
                rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                ArenaDefinition definition = parseArena(json, entry.getKey());
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

    private ArenaDefinition parseArena(JsonObject json, ResourceLocation sourceId) {
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "id"));
        ResourceLocation templateId = ResourceLocation.parse(GsonHelper.getAsString(json, "template"));
        Vec3 playerSpawn = readVec3(json, "player_spawn");
        Vec3 opponentSpawn = readVec3(json, "opponent_spawn");
        float playerYaw = GsonHelper.getAsFloat(json, "player_yaw", 0.0f);
        float playerPitch = GsonHelper.getAsFloat(json, "player_pitch", 0.0f);
        float opponentYaw = GsonHelper.getAsFloat(json, "opponent_yaw", 180.0f);
        float opponentPitch = GsonHelper.getAsFloat(json, "opponent_pitch", 0.0f);
        BlockPos clearPadding = readBlockPos(json, "clear_padding", BlockPos.ZERO);
        List<String> tags = readStringList(json, "tags");

        return new ArenaDefinition(
                id,
                templateId,
                playerSpawn,
                playerYaw,
                playerPitch,
                opponentSpawn,
                opponentYaw,
                opponentPitch,
                clearPadding,
                tags
        );
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

    private List<String> readStringList(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        GsonHelper.getAsJsonArray(json, key).forEach(element -> result.add(element.getAsString()));
        return result;
    }
}
