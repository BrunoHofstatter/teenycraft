package bruhof.teenycraft.battle.validation;

import bruhof.teenycraft.battle.ai.BattleAiProfile;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.battle.trait.TraitRegistry;
import bruhof.teenycraft.util.AbilityLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BattleContentValidation {
    private BattleContentValidation() {
    }

    public record Issue(String path, String message) {
        @Override
        public String toString() {
            return path + ": " + message;
        }
    }

    public record ValidationReport(List<Issue> errors, List<Issue> warnings) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public static ValidationReport validate(Map<String, JsonObject> abilityFiles,
                                            Map<String, JsonObject> figureFiles,
                                            Map<String, JsonObject> npcTeamFiles) {
        List<Issue> errors = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();

        Set<String> supportedEffects = new LinkedHashSet<>(EffectApplierRegistry.getSupportedAbilityEffectIds());
        Set<String> supportedTraits = TraitRegistry.getSupportedAbilityTraitIds();

        Set<String> abilityIds = collectIds(abilityFiles, "ability", errors);
        Set<String> figureIds = collectIds(figureFiles, "figure", errors);

        for (Map.Entry<String, JsonObject> entry : abilityFiles.entrySet()) {
            validateAbility(entry.getKey(), entry.getValue(), supportedEffects, supportedTraits, errors, warnings);
        }

        for (Map.Entry<String, JsonObject> entry : figureFiles.entrySet()) {
            validateFigure(entry.getKey(), entry.getValue(), abilityIds, errors);
        }

        for (Map.Entry<String, JsonObject> entry : npcTeamFiles.entrySet()) {
            validateNpcTeam(entry.getKey(), entry.getValue(), figureIds, abilityIds, errors);
        }

        return new ValidationReport(List.copyOf(errors), List.copyOf(warnings));
    }

    private static Set<String> collectIds(Map<String, JsonObject> files, String type, List<Issue> errors) {
        Set<String> ids = new LinkedHashSet<>();
        for (Map.Entry<String, JsonObject> entry : files.entrySet()) {
            String id = getRequiredString(entry.getValue(), "id", entry.getKey(), errors);
            if (id == null) {
                continue;
            }
            if (!ids.add(id)) {
                errors.add(new Issue(entry.getKey(), "duplicate " + type + " id '" + id + "'"));
            }
        }
        return ids;
    }

    private static void validateAbility(String path,
                                        JsonObject json,
                                        Set<String> supportedEffects,
                                        Set<String> supportedTraits,
                                        List<Issue> errors,
                                        List<Issue> warnings) {
        validateEffectArray(path, json.getAsJsonArray("effects_on_self"), "effects_on_self", supportedEffects, errors);
        validateEffectArray(path, json.getAsJsonArray("effects_on_opponent"), "effects_on_opponent", supportedEffects, errors);
        validateTraitArray(path, json.getAsJsonArray("traits"), supportedTraits, errors);

        JsonArray goldenBonus = json.getAsJsonArray("golden_bonus");
        if (goldenBonus == null) {
            return;
        }

        for (int i = 0; i < goldenBonus.size(); i++) {
            JsonElement element = goldenBonus.get(i);
            if (!element.isJsonPrimitive()) {
                errors.add(new Issue(path, "golden_bonus[" + i + "] must be a string"));
                continue;
            }

            String bonus = element.getAsString();
            AbilityLoader.GoldenBonusData parsedBonus;
            try {
                parsedBonus = AbilityLoader.parseGoldenBonus(bonus);
            } catch (IllegalArgumentException ex) {
                errors.add(new Issue(path, "golden_bonus[" + i + "] " + ex.getMessage()));
                continue;
            }

            validateGoldenBonus(path, i, parsedBonus, supportedEffects, supportedTraits, errors, warnings);
        }
    }

    private static void validateFigure(String path,
                                       JsonObject json,
                                       Set<String> abilityIds,
                                       List<Issue> errors) {
        JsonArray abilities = json.getAsJsonArray("abilities");
        if (abilities == null) {
            return;
        }

        for (int i = 0; i < abilities.size(); i++) {
            JsonElement element = abilities.get(i);
            if (!element.isJsonPrimitive()) {
                errors.add(new Issue(path, "abilities[" + i + "] must be a string"));
                continue;
            }

            String abilityId = element.getAsString();
            if (!abilityIds.contains(abilityId)) {
                errors.add(new Issue(path, "abilities[" + i + "] references unknown ability id '" + abilityId + "'"));
            }
        }
    }

    private static void validateNpcTeam(String path,
                                        JsonObject json,
                                        Set<String> figureIds,
                                        Set<String> abilityIds,
                                        List<Issue> errors) {
        JsonArray figures = json.getAsJsonArray("figures");
        if (figures == null) {
            return;
        }

        for (int i = 0; i < figures.size(); i++) {
            JsonElement element = figures.get(i);
            if (!element.isJsonObject()) {
                errors.add(new Issue(path, "figures[" + i + "] must be an object"));
                continue;
            }

            JsonObject figure = element.getAsJsonObject();
            String figureId = getRequiredString(figure, "figure_id", path, errors);
            if (figureId != null && !figureIds.contains(figureId)) {
                errors.add(new Issue(path, "figures[" + i + "] references unknown figure id '" + figureId + "'"));
            }

            JsonArray goldenAbilities = figure.getAsJsonArray("golden_abilities");
            if (goldenAbilities == null) {
                continue;
            }

            for (int j = 0; j < goldenAbilities.size(); j++) {
                JsonElement golden = goldenAbilities.get(j);
                if (!golden.isJsonPrimitive()) {
                    errors.add(new Issue(path, "figures[" + i + "].golden_abilities[" + j + "] must be a string"));
                    continue;
                }

                String abilityId = golden.getAsString();
                if (!abilityIds.contains(abilityId)) {
                    errors.add(new Issue(path, "figures[" + i + "].golden_abilities[" + j + "] references unknown ability id '" + abilityId + "'"));
                }
            }
        }

        validateNpcTeamAi(path, json.getAsJsonObject("ai"), errors);
    }

    private static void validateNpcTeamAi(String path, JsonObject ai, List<Issue> errors) {
        if (ai == null) {
            return;
        }

        if (ai.has("difficulty")) {
            int difficulty = ai.get("difficulty").getAsInt();
            if (difficulty < 1 || difficulty > 5) {
                errors.add(new Issue(path, "ai.difficulty must be between 1 and 5"));
            }
        }

        validateUnitFloat(path, ai, "aggression", errors);
        validateUnitFloat(path, ai, "swap_bias", errors);
        validateUnitFloat(path, ai, "mana_discipline", errors);
        validateUnitFloat(path, ai, "risk_tolerance", errors);
        validateRangedFloat(path, ai, "move_speed_mult", 0.5f, 2.0f, errors);
        validateMinInt(path, ai, "reaction_ticks", 1, errors);
        validateMinInt(path, ai, "action_commit_ticks", 1, errors);
        validateRangedDouble(path, ai, "choice_window", 0.0d, 10.0d, errors);
        validateMinInt(path, ai, "swap_reconsideration_ticks", 0, errors);

        if (ai.has("preferred_range")) {
            String preferredRange = ai.get("preferred_range").getAsString();
            if (BattleAiProfile.PreferredRange.fromSerialized(preferredRange) == BattleAiProfile.PreferredRange.AUTO
                    && !"auto".equalsIgnoreCase(preferredRange)) {
                errors.add(new Issue(path, "ai.preferred_range must be one of auto, close, mid, or far"));
            }
        }

        validateBoolean(path, ai, "consider_swap", errors);
        validateBoolean(path, ai, "counter_awareness", errors);
        validateBoolean(path, ai, "advanced_swap_logic", errors);
        validateBoolean(path, ai, "consider_class_disadvantage_swap", errors);
        validateBoolean(path, ai, "consider_class_advantage_swap", errors);
    }

    private static void validateUnitFloat(String path, JsonObject json, String field, List<Issue> errors) {
        if (!json.has(field)) {
            return;
        }

        float value = json.get(field).getAsFloat();
        if (value < 0.0f || value > 1.0f) {
            errors.add(new Issue(path, "ai." + field + " must be between 0.0 and 1.0"));
        }
    }

    private static void validateRangedFloat(String path, JsonObject json, String field, float min, float max, List<Issue> errors) {
        if (!json.has(field)) {
            return;
        }

        float value = json.get(field).getAsFloat();
        if (value < min || value > max) {
            errors.add(new Issue(path, "ai." + field + " must be between " + min + " and " + max));
        }
    }

    private static void validateRangedDouble(String path, JsonObject json, String field, double min, double max, List<Issue> errors) {
        if (!json.has(field)) {
            return;
        }

        double value = json.get(field).getAsDouble();
        if (value < min || value > max) {
            errors.add(new Issue(path, "ai." + field + " must be between " + min + " and " + max));
        }
    }

    private static void validateMinInt(String path, JsonObject json, String field, int min, List<Issue> errors) {
        if (!json.has(field)) {
            return;
        }

        int value = json.get(field).getAsInt();
        if (value < min) {
            errors.add(new Issue(path, "ai." + field + " must be >= " + min));
        }
    }

    private static void validateBoolean(String path, JsonObject json, String field, List<Issue> errors) {
        if (!json.has(field)) {
            return;
        }

        JsonElement element = json.get(field);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            errors.add(new Issue(path, "ai." + field + " must be a boolean"));
        }
    }

    private static void validateEffectArray(String path,
                                            JsonArray array,
                                            String field,
                                            Set<String> supportedEffects,
                                            List<Issue> errors) {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                errors.add(new Issue(path, field + "[" + i + "] must be an object"));
                continue;
            }

            JsonObject effect = element.getAsJsonObject();
            String effectId = getRequiredString(effect, "id", path, errors);
            if (effectId != null && !supportedEffects.contains(effectId)) {
                errors.add(new Issue(path, field + "[" + i + "] references unknown effect id '" + effectId + "'"));
            }
        }
    }

    private static void validateTraitArray(String path,
                                           JsonArray array,
                                           Set<String> supportedTraits,
                                           List<Issue> errors) {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                errors.add(new Issue(path, "traits[" + i + "] must be an object"));
                continue;
            }

            JsonObject trait = element.getAsJsonObject();
            String traitId = getRequiredString(trait, "id", path, errors);
            if (traitId != null && !supportedTraits.contains(traitId)) {
                errors.add(new Issue(path, "traits[" + i + "] references unknown trait id '" + traitId + "'"));
            }
        }
    }

    private static void validateGoldenBonus(String path,
                                            int index,
                                            AbilityLoader.GoldenBonusData bonus,
                                            Set<String> supportedEffects,
                                            Set<String> supportedTraits,
                                            List<Issue> errors,
                                            List<Issue> warnings) {
        switch (bonus.scope()) {
            case SELF, OPPONENT -> {
                if (!supportedEffects.contains(bonus.targetId())) {
                    errors.add(new Issue(path, "golden_bonus[" + index + "] references unknown effect id '" + bonus.targetId() + "'"));
                }
            }
            case TRAIT -> {
                if (!supportedTraits.contains(bonus.targetId())) {
                    errors.add(new Issue(path, "golden_bonus[" + index + "] references unknown trait id '" + bonus.targetId() + "'"));
                }
            }
        }
    }

    private static String getRequiredString(JsonObject json, String key, String path, List<Issue> errors) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            errors.add(new Issue(path, "missing required string field '" + key + "'"));
            return null;
        }
        return element.getAsString();
    }
}
