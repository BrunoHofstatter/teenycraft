package bruhof.teenycraft.battle.validation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class BattleContentValidator extends SimplePreparableReloadListener<BattleContentValidation.ValidationReport> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    @Override
    protected BattleContentValidation.ValidationReport prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, JsonObject> abilities = loadJsonObjects(resourceManager, "abilities");
        Map<String, JsonObject> figures = loadJsonObjects(resourceManager, "figures");
        Map<String, JsonObject> npcTeams = loadJsonObjects(resourceManager, "npc_teams");
        Map<String, JsonObject> figureGroups = loadJsonObjects(resourceManager, "figure_groups");
        Map<String, JsonObject> figureForms = loadJsonObjects(resourceManager, "figure_forms");

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                abilities, figures, npcTeams, figureGroups, figureForms);
        if (report.hasErrors()) {
            StringBuilder builder = new StringBuilder("Battle content validation failed:\n");
            for (BattleContentValidation.Issue error : report.errors()) {
                builder.append(" - ").append(error).append('\n');
            }
            throw new IllegalStateException(builder.toString());
        }

        return report;
    }

    @Override
    protected void apply(BattleContentValidation.ValidationReport report, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (BattleContentValidation.Issue warning : report.warnings()) {
            LOGGER.warn("Battle content validation warning: {}", warning);
        }

        LOGGER.info("Battle content validation passed for {} issues and {} warnings", report.errors().size(), report.warnings().size());
    }

    private static Map<String, JsonObject> loadJsonObjects(ResourceManager resourceManager, String folder) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                folder,
                rl -> rl.getNamespace().equals("teenycraft") && rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (PushbackReader reader = openUtf8Json(entry.getValue())) {
                result.put(entry.getKey().toString(), GSON.fromJson(reader, JsonObject.class));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read JSON resource " + entry.getKey(), e);
            }
        }

        return result;
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
}
