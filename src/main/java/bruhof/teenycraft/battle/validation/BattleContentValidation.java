package bruhof.teenycraft.battle.validation;

import bruhof.teenycraft.battle.ai.BattleAiProfile;
import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.battle.trait.TraitRegistry;
import bruhof.teenycraft.group.GroupComboEffectRegistry;
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
        return validate(abilityFiles, figureFiles, npcTeamFiles, Map.of(), Map.of());
    }

    public static ValidationReport validate(Map<String, JsonObject> abilityFiles,
                                            Map<String, JsonObject> figureFiles,
                                            Map<String, JsonObject> npcTeamFiles,
                                            Map<String, JsonObject> figureGroupFiles) {
        return validate(abilityFiles, figureFiles, npcTeamFiles, figureGroupFiles, Map.of());
    }

    public static ValidationReport validate(Map<String, JsonObject> abilityFiles,
                                            Map<String, JsonObject> figureFiles,
                                            Map<String, JsonObject> npcTeamFiles,
                                            Map<String, JsonObject> figureGroupFiles,
                                            Map<String, JsonObject> figureFormFiles) {
        List<Issue> errors = new ArrayList<>();
        List<Issue> warnings = new ArrayList<>();

        Set<String> supportedEffects = new LinkedHashSet<>(EffectApplierRegistry.getSupportedAbilityEffectIds());
        Set<String> supportedTraits = TraitRegistry.getSupportedAbilityTraitIds();

        Set<String> abilityIds = collectIds(abilityFiles, "ability", errors);
        Set<String> figureIds = collectIds(figureFiles, "figure", errors);
        collectIds(figureFormFiles, "figure form", errors);

        for (Map.Entry<String, JsonObject> entry : abilityFiles.entrySet()) {
            validateAbility(entry.getKey(), entry.getValue(), supportedEffects, supportedTraits, errors, warnings);
        }

        for (Map.Entry<String, JsonObject> entry : figureFiles.entrySet()) {
            validateFigure(entry.getKey(), entry.getValue(), abilityIds, errors);
        }

        for (Map.Entry<String, JsonObject> entry : npcTeamFiles.entrySet()) {
            validateNpcTeam(entry.getKey(), entry.getValue(), figureIds, abilityIds, errors);
        }

        for (Map.Entry<String, JsonObject> entry : figureGroupFiles.entrySet()) {
            validateFigureGroup(entry.getKey(), entry.getValue(), figureIds,
                    GroupComboEffectRegistry.getSupportedIds(), errors);
        }

        validateFigureForms(figureFormFiles, abilityFiles, figureFiles, abilityIds, errors);

        return new ValidationReport(List.copyOf(errors), List.copyOf(warnings));
    }

    private static void validateFigureForms(Map<String, JsonObject> formFiles,
                                            Map<String, JsonObject> abilityFiles,
                                            Map<String, JsonObject> figureFiles,
                                            Set<String> abilityIds,
                                            List<Issue> errors) {
        Map<String, JsonObject> abilitiesById = new java.util.HashMap<>();
        abilityFiles.values().forEach(ability -> {
            if (ability.has("id") && ability.get("id").isJsonPrimitive()) {
                abilitiesById.put(ability.get("id").getAsString(), ability);
            }
        });

        Set<String> transitionAbilities = new LinkedHashSet<>();
        Set<String> enterAbilities = new LinkedHashSet<>();
        Map<String, JsonObject> formsByEnterAbility = new java.util.HashMap<>();

        for (Map.Entry<String, JsonObject> entry : formFiles.entrySet()) {
            String path = entry.getKey();
            JsonObject form = entry.getValue();
            String id = getRequiredString(form, "id", path, errors);
            String skin = getRequiredString(form, "skin", path, errors);
            String enterAbility = getRequiredString(form, "enter_ability", path, errors);
            String exitAbility = getRequiredString(form, "exit_ability", path, errors);

            if (id != null && !id.equals(resourceFileId(path))) {
                errors.add(new Issue(path, "id '" + id + "' must match resource filename '" + resourceFileId(path) + "'"));
            }
            if (skin != null && skin.isBlank()) {
                errors.add(new Issue(path, "skin must not be blank"));
            }
            if (enterAbility != null) {
                if (!enterAbilities.add(enterAbility)) {
                    errors.add(new Issue(path, "enter_ability '" + enterAbility + "' is already owned by another form"));
                }
                formsByEnterAbility.put(enterAbility, form);
                transitionAbilities.add(enterAbility);
                validateTransitionAbility(path, "enter_ability", enterAbility, abilitiesById, errors);
            }
            if (exitAbility != null) {
                transitionAbilities.add(exitAbility);
                validateTransitionAbility(path, "exit_ability", exitAbility, abilitiesById, errors);
            }

            JsonObject counterparts = form.getAsJsonObject("ability_counterparts");
            JsonObject tiers = form.getAsJsonObject("ability_cost_tiers");
            if (counterparts == null || counterparts.size() == 0) {
                errors.add(new Issue(path, "ability_counterparts must be a non-empty object"));
                continue;
            }
            if (tiers == null || tiers.size() == 0) {
                errors.add(new Issue(path, "ability_cost_tiers must be a non-empty object"));
                continue;
            }

            for (Map.Entry<String, JsonElement> counterpart : counterparts.entrySet()) {
                String sourceAbility = counterpart.getKey();
                if (!abilityIds.contains(sourceAbility)) {
                    errors.add(new Issue(path, "ability_counterparts references unknown source ability '" + sourceAbility + "'"));
                }
                if (!counterpart.getValue().isJsonPrimitive()) {
                    errors.add(new Issue(path, "ability_counterparts['" + sourceAbility + "'] must be a string"));
                    continue;
                }
                String effectiveAbility = counterpart.getValue().getAsString();
                if (!abilityIds.contains(effectiveAbility)) {
                    errors.add(new Issue(path, "ability_counterparts['" + sourceAbility + "'] references unknown ability '" + effectiveAbility + "'"));
                }
                if (!tiers.has(effectiveAbility)) {
                    errors.add(new Issue(path, "effective ability '" + effectiveAbility + "' has no ability_cost_tiers entry"));
                }
            }

            if (enterAbility != null && exitAbility != null
                    && (!counterparts.has(enterAbility) || !exitAbility.equals(counterparts.get(enterAbility).getAsString()))) {
                errors.add(new Issue(path, "enter_ability must map to exit_ability in ability_counterparts"));
            }

            for (Map.Entry<String, JsonElement> tier : tiers.entrySet()) {
                if (!abilityIds.contains(tier.getKey())) {
                    errors.add(new Issue(path, "ability_cost_tiers references unknown ability '" + tier.getKey() + "'"));
                }
                if (!tier.getValue().isJsonPrimitive() || !tier.getValue().getAsJsonPrimitive().isString()
                        || !tier.getValue().getAsString().matches("[a-eA-E]")) {
                    errors.add(new Issue(path, "ability_cost_tiers['" + tier.getKey() + "'] must be one of a, b, c, d, or e"));
                }
            }

            if (form.has("model_type")) {
                String modelType = form.get("model_type").getAsString();
                if (!"default".equals(modelType) && !"slim".equals(modelType)) {
                    errors.add(new Issue(path, "model_type must be 'default' or 'slim'"));
                }
            }
        }

        for (Map.Entry<String, JsonObject> abilityEntry : abilitiesById.entrySet()) {
            if (hasSelfEffect(abilityEntry.getValue(), "transform") && !transitionAbilities.contains(abilityEntry.getKey())) {
                errors.add(new Issue(abilityEntry.getKey(), "transform ability is not declared as a figure form enter_ability or exit_ability"));
            }
        }

        for (Map.Entry<String, JsonObject> figureEntry : figureFiles.entrySet()) {
            JsonArray abilities = figureEntry.getValue().getAsJsonArray("abilities");
            if (abilities == null) continue;
            for (JsonElement ability : abilities) {
                JsonObject form = formsByEnterAbility.get(ability.getAsString());
                if (form == null) continue;
                JsonObject counterparts = form.getAsJsonObject("ability_counterparts");
                for (JsonElement sourceAbility : abilities) {
                    if (!counterparts.has(sourceAbility.getAsString())) {
                        errors.add(new Issue(figureEntry.getKey(), "transforming loadout ability '"
                                + sourceAbility.getAsString() + "' has no counterpart in form '" + form.get("id").getAsString() + "'"));
                    }
                }
            }
        }
    }

    private static void validateTransitionAbility(String path,
                                                  String field,
                                                  String abilityId,
                                                  Map<String, JsonObject> abilitiesById,
                                                  List<Issue> errors) {
        JsonObject ability = abilitiesById.get(abilityId);
        if (ability == null) {
            errors.add(new Issue(path, field + " references unknown ability id '" + abilityId + "'"));
            return;
        }

        JsonObject transformEffect = findSelfEffect(ability, "transform");
        if (transformEffect == null) {
            errors.add(new Issue(path, field + " ability '" + abilityId + "' must apply the transform self effect"));
        } else if (transformEffect.has("params")
                && (!transformEffect.get("params").isJsonArray()
                || transformEffect.getAsJsonArray("params").size() != 0)) {
            errors.add(new Issue(path, field + " ability '" + abilityId + "' must use a parameterless transform effect"));
        }
    }

    private static boolean hasSelfEffect(JsonObject ability, String effectId) {
        return findSelfEffect(ability, effectId) != null;
    }

    private static JsonObject findSelfEffect(JsonObject ability, String effectId) {
        JsonArray effects = ability.getAsJsonArray("effects_on_self");
        if (effects == null) return null;
        for (JsonElement element : effects) {
            if (element.isJsonObject() && element.getAsJsonObject().has("id")
                    && effectId.equals(element.getAsJsonObject().get("id").getAsString())) {
                return element.getAsJsonObject();
            }
        }
        return null;
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
        if (json.has("groups")) {
            errors.add(new Issue(path, "legacy field 'groups' is not allowed; figure groups belong in figure_groups JSON"));
        }
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

    private static void validateFigureGroup(String path,
                                            JsonObject json,
                                            Set<String> figureIds,
                                            Set<String> supportedComboEffects,
                                            List<Issue> errors) {
        String id = getRequiredString(json, "id", path, errors);
        String name = getRequiredString(json, "name", path, errors);
        if (name != null && name.isBlank()) {
            errors.add(new Issue(path, "name must not be blank"));
        }
        if (id != null) {
            String expectedId = resourceFileId(path);
            if (!expectedId.isBlank() && !expectedId.equals(id)) {
                errors.add(new Issue(path, "id '" + id + "' must match resource filename '" + expectedId + "'"));
            }
        }
        if (!json.has("priority") || !json.get("priority").isJsonPrimitive()
                || !json.get("priority").getAsJsonPrimitive().isNumber()) {
            errors.add(new Issue(path, "missing required integer field 'priority'"));
        }

        JsonArray figures = json.getAsJsonArray("figures");
        if (figures == null || figures.size() < 2) {
            errors.add(new Issue(path, "figures must contain at least two figure ids"));
        } else {
            Set<String> seenFigures = new LinkedHashSet<>();
            for (int i = 0; i < figures.size(); i++) {
                JsonElement element = figures.get(i);
                if (!element.isJsonPrimitive()) {
                    errors.add(new Issue(path, "figures[" + i + "] must be a string"));
                    continue;
                }
                String figureId = element.getAsString();
                if (!seenFigures.add(figureId)) {
                    errors.add(new Issue(path, "figures contains duplicate figure id '" + figureId + "'"));
                }
                if (!figureIds.contains(figureId)) {
                    errors.add(new Issue(path, "figures[" + i + "] references unknown figure id '" + figureId + "'"));
                }
            }
        }

        JsonArray effects = json.getAsJsonArray("combo_effects");
        if (effects == null || effects.isEmpty()) {
            errors.add(new Issue(path, "combo_effects must contain at least one registered effect id"));
        } else {
            for (int i = 0; i < effects.size(); i++) {
                JsonElement element = effects.get(i);
                if (!element.isJsonPrimitive()) {
                    errors.add(new Issue(path, "combo_effects[" + i + "] must be a string"));
                    continue;
                }
                String effectId = element.getAsString();
                if (!supportedComboEffects.contains(effectId)) {
                    errors.add(new Issue(path, "combo_effects[" + i + "] references unknown combo effect id '" + effectId + "'"));
                }
            }
        }
    }

    private static String resourceFileId(String path) {
        int slash = path.lastIndexOf('/');
        int colon = path.lastIndexOf(':');
        int start = Math.max(slash, colon) + 1;
        int end = path.endsWith(".json") ? path.length() - 5 : path.length();
        return start >= 0 && start < end ? path.substring(start, end) : "";
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
