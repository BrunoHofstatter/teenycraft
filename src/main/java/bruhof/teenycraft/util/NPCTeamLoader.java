package bruhof.teenycraft.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

public class NPCTeamLoader extends SimplePreparableReloadListener<Map<String, List<NPCFigureBuilder.NPCFigureData>>> {
    private static final Gson GSON = new Gson();
    private static final Map<String, List<NPCFigureBuilder.NPCFigureData>> TEAMS = new HashMap<>();

    @Override
    protected Map<String, List<NPCFigureBuilder.NPCFigureData>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, List<NPCFigureBuilder.NPCFigureData>> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("npc_teams",
                rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String id = json.get("id").getAsString();
                
                List<NPCFigureBuilder.NPCFigureData> figures = new ArrayList<>();
                JsonArray figuresArray = json.getAsJsonArray("figures");
                
                for (int i = 0; i < figuresArray.size(); i++) {
                    JsonObject figJson = figuresArray.get(i).getAsJsonObject();
                    NPCFigureBuilder.NPCFigureData data = new NPCFigureBuilder.NPCFigureData();
                    data.figureId = figJson.get("figure_id").getAsString();
                    data.level = figJson.has("level") ? figJson.get("level").getAsInt() : 1;
                    data.upgrades = figJson.has("upgrades") ? figJson.get("upgrades").getAsString() : "";
                    
                    if (figJson.has("ability_order")) {
                        figJson.getAsJsonArray("ability_order").forEach(e -> data.abilityOrder.add(e.getAsInt()));
                    }
                    
                    if (figJson.has("golden_abilities")) {
                        figJson.getAsJsonArray("golden_abilities").forEach(e -> data.goldenAbilities.add(e.getAsString()));
                    }
                    
                    figures.add(data);
                }
                result.put(id, figures);
                System.out.println("Loaded NPC Team: " + id + " from " + entry.getKey());
            } catch (Exception e) {
                System.err.println("Failed to load NPC Team: " + entry.getKey());
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, List<NPCFigureBuilder.NPCFigureData>> result, ResourceManager resourceManager, ProfilerFiller profiler) {
        TEAMS.clear();
        TEAMS.putAll(result);
    }

    public static List<NPCFigureBuilder.NPCFigureData> getTeam(String id) {
        return TEAMS.get(id);
    }

    public static java.util.Set<String> getLoadedTeamIds() {
        return TEAMS.keySet();
    }
}
