package bruhof.teenycraft.util;

import bruhof.teenycraft.battle.ai.BattleAiProfile;
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

public class NPCTeamLoader extends SimplePreparableReloadListener<Map<String, NPCTeamLoader.NPCTeamDefinition>> {
    private static final Gson GSON = new Gson();
    private static final Map<String, NPCTeamDefinition> TEAMS = new HashMap<>();

    public static class NPCTeamDefinition {
        public final String id;
        public final List<NPCFigureBuilder.NPCFigureData> figures;
        public final BattleAiProfile aiProfile;

        public NPCTeamDefinition(String id, List<NPCFigureBuilder.NPCFigureData> figures, BattleAiProfile aiProfile) {
            this.id = id;
            this.figures = figures;
            this.aiProfile = aiProfile;
        }
    }

    @Override
    protected Map<String, NPCTeamDefinition> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, NPCTeamDefinition> result = new HashMap<>();
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

                    if (figJson.has("chip_id")) {
                        data.chipId = figJson.get("chip_id").getAsString();
                    }
                    if (figJson.has("chip_rank")) {
                        data.chipRank = figJson.get("chip_rank").getAsInt();
                    }
                    
                    figures.add(data);
                }

                BattleAiProfile aiProfile = BattleAiProfile.DEFAULT;
                if (json.has("ai") && json.get("ai").isJsonObject()) {
                    JsonObject aiJson = json.getAsJsonObject("ai");
                    aiProfile = new BattleAiProfile(
                            aiJson.has("difficulty") ? aiJson.get("difficulty").getAsInt() : BattleAiProfile.DEFAULT.difficulty(),
                            aiJson.has("aggression") ? aiJson.get("aggression").getAsFloat() : BattleAiProfile.DEFAULT.aggression(),
                            aiJson.has("swap_bias") ? aiJson.get("swap_bias").getAsFloat() : BattleAiProfile.DEFAULT.swapBias(),
                            BattleAiProfile.PreferredRange.fromSerialized(aiJson.has("preferred_range") ? aiJson.get("preferred_range").getAsString() : null),
                            aiJson.has("mana_discipline") ? aiJson.get("mana_discipline").getAsFloat() : BattleAiProfile.DEFAULT.manaDiscipline(),
                            aiJson.has("risk_tolerance") ? aiJson.get("risk_tolerance").getAsFloat() : BattleAiProfile.DEFAULT.riskTolerance()
                    );
                }

                result.put(id, new NPCTeamDefinition(id, figures, aiProfile));
                System.out.println("Loaded NPC Team: " + id + " from " + entry.getKey());
            } catch (Exception e) {
                System.err.println("Failed to load NPC Team: " + entry.getKey());
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, NPCTeamDefinition> result, ResourceManager resourceManager, ProfilerFiller profiler) {
        TEAMS.clear();
        TEAMS.putAll(result);
    }

    public static List<NPCFigureBuilder.NPCFigureData> getTeam(String id) {
        NPCTeamDefinition team = TEAMS.get(id);
        return team != null ? team.figures : null;
    }

    public static NPCTeamDefinition getTeamDefinition(String id) {
        return TEAMS.get(id);
    }

    public static java.util.Set<String> getLoadedTeamIds() {
        return TEAMS.keySet();
    }
}
