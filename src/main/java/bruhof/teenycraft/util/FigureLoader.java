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
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FigureLoader extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    private static final Gson GSON = new Gson();
    private static final Map<String, JsonObject> CACHE = new HashMap<>();

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<String, JsonObject> figures = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("figures", 
            rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String id = json.get("id").getAsString();
                figures.put(id, json);
            } catch (Exception e) {
                System.err.println("Failed to load figure: " + entry.getKey());
            }
        }
        return figures;
    }

    @Override
    protected void apply(Map<String, JsonObject> result, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        CACHE.clear();
        CACHE.putAll(result);
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

        // Parse groups
        List<String> groups = new ArrayList<>();
        json.getAsJsonArray("groups").forEach(e -> groups.add(e.getAsString()));

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
            groups,
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
