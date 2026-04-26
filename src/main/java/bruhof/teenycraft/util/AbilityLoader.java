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
        public String description = "";
        public String goldenDescription = "";
        public String hitType;
        public int raycastDelayTier;
        public int damageTier;
        public int rangeTier = 4; // Default
        public int textureIndex = 0; // NEW: Stable index for property overrides
        public String particleId;
        public int particleCount = 10;
        public List<EffectData> effectsOnOpponent = new ArrayList<>();
        public List<EffectData> effectsOnSelf = new ArrayList<>();
        public List<TraitData> traits = new ArrayList<>();
        public List<String> goldenBonus = new ArrayList<>();
        public List<GoldenBonusData> parsedGoldenBonus = new ArrayList<>();

        public List<GoldenBonusData> getGoldenBonuses(GoldenBonusScope scope) {
            return parsedGoldenBonus.stream()
                    .filter(bonus -> bonus.scope() == scope)
                    .toList();
        }

        public boolean hasGoldenBonus(GoldenBonusScope scope, String targetId) {
            return findGoldenBonus(scope, targetId) != null;
        }

        public GoldenBonusData findGoldenBonus(GoldenBonusScope scope, String targetId) {
            GoldenBonusData match = null;
            for (GoldenBonusData bonus : parsedGoldenBonus) {
                if (bonus.matches(scope, targetId)) {
                    match = bonus;
                }
            }
            return match;
        }
    }

    public static class EffectData {
        public String id;
        public List<Float> params = new ArrayList<>();
    }

    public static class TraitData {
        public String id;
        public List<Float> params = new ArrayList<>();
    }

    public enum GoldenBonusScope {
        SELF("self"),
        OPPONENT("opponent"),
        TRAIT("trait");

        private final String serializedName;

        GoldenBonusScope(String serializedName) {
            this.serializedName = serializedName;
        }

        public static GoldenBonusScope fromSerializedName(String value) {
            for (GoldenBonusScope scope : values()) {
                if (scope.serializedName.equalsIgnoreCase(value)) {
                    return scope;
                }
            }
            return null;
        }
    }

    public record GoldenBonusData(String rawValue, GoldenBonusScope scope, String targetId, List<Float> params) {
        public GoldenBonusData {
            params = List.copyOf(params);
        }

        public boolean matches(GoldenBonusScope requestedScope, String requestedTargetId) {
            return scope == requestedScope && targetId.equals(requestedTargetId);
        }
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
                data.description = json.has("description") ? json.get("description").getAsString() : "";
                data.goldenDescription = json.has("golden_description") ? json.get("golden_description").getAsString() : "";
                data.hitType = json.get("hit_type").getAsString();
                data.raycastDelayTier = json.has("raycast_delay_tier") ? json.get("raycast_delay_tier").getAsInt() : 0;
                data.damageTier = json.get("damage_tier").getAsInt();
                data.rangeTier = json.has("range_tier") ? json.get("range_tier").getAsInt() : 4;
                data.textureIndex = json.has("texture_index") ? json.get("texture_index").getAsInt() : 0;
                data.particleId = json.has("particle") ? json.get("particle").getAsString() : null;
                data.particleCount = json.has("particle_count") ? json.get("particle_count").getAsInt() : 10;

                parseEffects(json.getAsJsonArray("effects_on_opponent"), data.effectsOnOpponent);
                parseEffects(json.getAsJsonArray("effects_on_self"), data.effectsOnSelf);
                parseTraits(json.getAsJsonArray("traits"), data.traits);

                if (json.has("golden_bonus")) {
                    json.getAsJsonArray("golden_bonus").forEach(e -> {
                        String rawBonus = e.getAsString();
                        data.goldenBonus.add(rawBonus);
                        data.parsedGoldenBonus.add(parseGoldenBonus(rawBonus));
                    });
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

    public static java.util.Set<String> getAbilityIds() {
        return CACHE.keySet();
    }

    public static int getTextureIndex(String id) {
        AbilityData data = CACHE.get(id);
        return data != null ? data.textureIndex : 0;
    }

    public static GoldenBonusData parseGoldenBonus(String rawBonus) {
        String[] parts = rawBonus.split(":", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("has invalid format '" + rawBonus + "'");
        }

        GoldenBonusScope scope = GoldenBonusScope.fromSerializedName(parts[0].trim());
        if (scope == null) {
            throw new IllegalArgumentException("uses unknown scope '" + parts[0].trim() + "'");
        }

        String targetId = parts[1].trim();
        if (targetId.isEmpty()) {
            throw new IllegalArgumentException("has invalid format '" + rawBonus + "'");
        }

        String paramsText = parts.length == 3 ? parts[2].trim() : "";
        boolean paramsRequired = scope != GoldenBonusScope.TRAIT;
        List<Float> params = parseGoldenBonusParams(rawBonus, paramsText, paramsRequired);
        return new GoldenBonusData(rawBonus, scope, targetId, params);
    }

    private static List<Float> parseGoldenBonusParams(String rawBonus, String paramsText, boolean required) {
        if (paramsText.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("is missing numeric params");
            }
            return List.of();
        }

        List<Float> params = new ArrayList<>();
        String[] values = paramsText.split(",");
        for (String value : values) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("contains an empty numeric param");
            }

            try {
                params.add(Float.parseFloat(trimmed));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("contains a non-numeric param '" + trimmed + "' in '" + rawBonus + "'");
            }
        }
        return params;
    }
}
