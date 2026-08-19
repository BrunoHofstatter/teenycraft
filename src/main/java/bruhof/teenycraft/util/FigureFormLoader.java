package bruhof.teenycraft.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Loads battle-only figure forms from data/teenycraft/figure_forms. */
public class FigureFormLoader extends SimplePreparableReloadListener<Map<String, FigureFormLoader.FigureFormData>> {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, FigureFormData> CACHE = new HashMap<>();

    public record FigureFormData(
            String id,
            String skin,
            String enterAbility,
            String exitAbility,
            @Nullable String modelType,
            Map<String, String> abilityCounterparts,
            Map<String, String> abilityCostTiers
    ) {
        public FigureFormData {
            abilityCounterparts = Map.copyOf(abilityCounterparts);
            abilityCostTiers = Map.copyOf(abilityCostTiers);
        }

        @Nullable
        public String resolveAbility(String sourceAbilityId) {
            return abilityCounterparts.get(sourceAbilityId);
        }

        @Nullable
        public String resolveCostTier(String effectiveAbilityId) {
            return abilityCostTiers.get(effectiveAbilityId);
        }
    }

    @Override
    protected Map<String, FigureFormData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, FigureFormData> forms = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "figure_forms",
                location -> location.getNamespace().equals("teenycraft") && location.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (PushbackReader reader = openUtf8Json(entry.getValue())) {
                FigureFormData form = parse(GSON.fromJson(reader, JsonObject.class));
                forms.put(form.id(), form);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load figure form " + entry.getKey(), exception);
            }
        }
        return forms;
    }

    private static PushbackReader openUtf8Json(Resource resource) throws java.io.IOException {
        PushbackReader reader = new PushbackReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8), 1);
        int first = reader.read();
        if (first != -1 && first != '\uFEFF') {
            reader.unread(first);
        }
        return reader;
    }

    @Override
    protected void apply(Map<String, FigureFormData> result, ResourceManager resourceManager, ProfilerFiller profiler) {
        CACHE.clear();
        CACHE.putAll(result);
        LOGGER.info("Loaded {} battle figure forms", CACHE.size());
    }

    public static FigureFormData parse(JsonObject json) {
        String id = json.get("id").getAsString();
        String skin = json.has("skin") ? json.get("skin").getAsString() : id;
        String enterAbility = json.get("enter_ability").getAsString();
        String exitAbility = json.get("exit_ability").getAsString();
        String modelType = json.has("model_type") ? json.get("model_type").getAsString() : null;
        Map<String, String> counterparts = readStringMap(json.getAsJsonObject("ability_counterparts"));
        Map<String, String> tiers = readStringMap(json.getAsJsonObject("ability_cost_tiers"));
        return new FigureFormData(id, skin, enterAbility, exitAbility, modelType, counterparts, tiers);
    }

    private static Map<String, String> readStringMap(JsonObject json) {
        Map<String, String> result = new HashMap<>();
        if (json != null) {
            json.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        }
        return result;
    }

    @Nullable
    public static FigureFormData get(String id) {
        return id == null ? null : CACHE.get(id);
    }

    @Nullable
    public static FigureFormData findByEnterAbility(String abilityId) {
        if (abilityId == null) {
            return null;
        }
        for (FigureFormData form : CACHE.values()) {
            if (abilityId.equals(form.enterAbility())) {
                return form;
            }
        }
        return null;
    }
}
