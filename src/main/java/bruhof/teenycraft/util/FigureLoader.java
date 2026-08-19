package bruhof.teenycraft.util;

import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FigureLoader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    private static final Gson GSON = new Gson();
    private static final Map<String, JsonObject> CACHE = new HashMap<>();
    private static final Map<String, String> MODEL_CACHE = new HashMap<>();

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String, JsonObject> figures = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("figures", 
            rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (PushbackReader reader = openUtf8Json(entry.getValue())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String id = json.get("id").getAsString();
                figures.put(id, json);
                
                if (json.has("model_type")) {
                    MODEL_CACHE.put(id, json.get("model_type").getAsString());
                } else {
                    MODEL_CACHE.put(id, "default");
                }
            } catch (Exception e) {
                System.err.println("Failed to load figure: " + entry.getKey());
            }
        }
        return figures;
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
    protected void apply(Map<String, JsonObject> result, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        CACHE.clear();
        // Note: MODEL_CACHE is filled in prepare, and we don't clear it here 
        // because prepare runs before apply and result only contains JsonObjects.
        // If we want to be safe, we'd rebuild MODEL_CACHE here from result.
        MODEL_CACHE.clear();
        for (Map.Entry<String, JsonObject> entry : result.entrySet()) {
            if (entry.getValue().has("model_type")) {
                MODEL_CACHE.put(entry.getKey(), entry.getValue().get("model_type").getAsString());
            } else {
                MODEL_CACHE.put(entry.getKey(), "default");
            }
        }
        CACHE.putAll(result);
    }

    public static String getModelType(String id) {
        return MODEL_CACHE.getOrDefault(id, "default");
    }

    public static String getAbilityIconVariant(String figureId, String abilityId) {
        JsonObject figure = CACHE.get(figureId);
        if (figure == null || abilityId == null || !figure.has("ability_icon_variants")
                || !figure.get("ability_icon_variants").isJsonObject()) {
            return "";
        }

        JsonObject variants = figure.getAsJsonObject("ability_icon_variants");
        if (!variants.has(abilityId) || !variants.get(abilityId).isJsonPrimitive()
                || !variants.get(abilityId).getAsJsonPrimitive().isString()) {
            return "";
        }
        return variants.get(abilityId).getAsString();
    }

    public static String getAbilityIconVariant(ItemStack figureStack, String abilityId) {
        return getAbilityIconVariant(ItemFigure.getFigureID(figureStack), abilityId);
    }

    public static ItemStack getFigureStack(String id) {
        if (!CACHE.containsKey(id)) return ItemStack.EMPTY;
        
        JsonObject json = CACHE.get(id);
        
        // Find the matching item in registry (e.g., figure_robin)
        ResourceLocation itemRl = new ResourceLocation("teenycraft", "figure_" + id);
        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(itemRl);
        if (item == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item);
        
        // Parse attributes
        JsonObject attrs = json.getAsJsonObject("attributes");
        float hp = attrs.get("hp_scale").getAsFloat();
        float power = attrs.get("power_scale").getAsFloat();
        float dodge = attrs.get("dodge_scale").getAsFloat();
        float luck = attrs.get("luck_scale").getAsFloat();

        // Parse abilities
        List<String> abilities = new ArrayList<>();
        json.getAsJsonArray("abilities").forEach(e -> abilities.add(e.getAsString()));

        // Parse cost tiers
        List<String> costTiers = new ArrayList<>();
        if (json.has("ability_cost_tiers")) {
            json.getAsJsonArray("ability_cost_tiers").forEach(e -> costTiers.add(e.getAsString()));
        }

        return ItemFigure.create(
            stack,
            id,
            json.get("name").getAsString(),
            json.get("description").getAsString(),
            json.get("class").getAsString(),
            json.get("price").getAsInt(),
            hp, power, dodge, luck,
            abilities,
            costTiers
        );
    }

    public static java.util.Set<String> getLoadedFigureIds() {
        return CACHE.keySet();
    }
}
