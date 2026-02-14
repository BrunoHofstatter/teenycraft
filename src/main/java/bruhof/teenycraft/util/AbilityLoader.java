package bruhof.teenycraft.util;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityLoader extends SimplePreparableReloadListener<Map<String, AbilityLoader.AbilityData>> {
    private static final Gson GSON = new Gson();
    private static final Map<String, AbilityData> CACHE = new HashMap<>();

    public static class AbilityData {
        public String id;
        public String name;
        public String hitType;
        public int raycastDelayTier;
        public int damageTier;
        public List<EffectData> effectsOnOpponent = new ArrayList<>();
        public List<EffectData> effectsOnSelf = new ArrayList<>();
        public List<TraitData> traits = new ArrayList<>();
        public List<String> goldenBonus = new ArrayList<>();
    }

    public static class EffectData {
        public String id;
        public List<Float> params = new ArrayList<>();
    }

    public static class TraitData {
        public String id;
        public List<Float> params = new ArrayList<>();
    }

    @Override
    protected Map<String, AbilityData> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String, AbilityData> abilities = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("abilities",
                rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                AbilityData data = new AbilityData();
                data.id = json.get("id").getAsString();
                data.name = json.has("name") ? json.get("name").getAsString() : data.id;
                data.hitType = json.get("hit_type").getAsString();
                data.raycastDelayTier = json.has("raycast_delay_tier") ? json.get("raycast_delay_tier").getAsInt() : 0;
                data.damageTier = json.get("damage_tier").getAsInt();

                parseEffects(json.getAsJsonArray("effects_on_opponent"), data.effectsOnOpponent);
                parseEffects(json.getAsJsonArray("effects_on_self"), data.effectsOnSelf);
                parseTraits(json.getAsJsonArray("traits"), data.traits);

                if (json.has("golden_bonus")) {
                    json.getAsJsonArray("golden_bonus").forEach(e -> data.goldenBonus.add(e.getAsString()));
                }

                abilities.put(data.id, data);
            } catch (Exception e) {
                System.err.println("Failed to load ability: " + entry.getKey());
                e.printStackTrace();
            }
        }
        return abilities;
    }

    private void parseEffects(JsonArray array, List<EffectData> list) {
        if (array == null) return;
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            EffectData effect = new EffectData();
            effect.id = obj.get("id").getAsString();
            if (obj.has("params")) {
                obj.getAsJsonArray("params").forEach(p -> effect.params.add(p.getAsFloat()));
            }
            list.add(effect);
        }
    }

    private void parseTraits(JsonArray array, List<TraitData> list) {
        if (array == null) return;
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            TraitData trait = new TraitData();
            trait.id = obj.get("id").getAsString();
            if (obj.has("params")) {
                obj.getAsJsonArray("params").forEach(p -> trait.params.add(p.getAsFloat()));
            }
            list.add(trait);
        }
    }

    @Override
    protected void apply(Map<String, AbilityData> result, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        CACHE.clear();
        CACHE.putAll(result);
    }

    public static AbilityData getAbility(String id) {
        return CACHE.get(id);
    }
}
