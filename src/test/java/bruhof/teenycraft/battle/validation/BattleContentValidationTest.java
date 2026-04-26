package bruhof.teenycraft.battle.validation;

import bruhof.teenycraft.battle.effect.EffectApplierRegistry;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.trait.TraitRegistry;
import bruhof.teenycraft.util.AbilityLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleContentValidationTest {
    @BeforeAll
    static void initRegistries() {
        EffectRegistry.init();
        EffectApplierRegistry.init();
        TraitRegistry.init();
    }

    @Test
    void acceptsCurrentTraitAndEffectGoldenContracts() {
        JsonObject ability = json("""
                {
                  "id": "test_ability",
                  "effects_on_self": [{"id":"power_up","params":[1.0]}],
                  "effects_on_opponent": [{"id":"remote_mine","params":[1.0]}],
                  "traits": [{"id":"tofu_chance","params":[1.5,1.2]}],
                  "golden_bonus": [
                    "self:power_up:0.3",
                    "opponent:remote_mine:1.3",
                    "trait:tofu_chance:1.5,1.2",
                    "trait:instant_cast"
                  ]
                }
                """);
        JsonObject figure = json("""
                {"id":"test_figure","abilities":["test_ability"]}
                """);
        JsonObject npc = json("""
                {"id":"test_team","figures":[{"figure_id":"test_figure","golden_abilities":["test_ability"]}]}
                """);

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                Map.of("teenycraft:abilities/test_ability.json", ability),
                Map.of("teenycraft:figures/test_figure.json", figure),
                Map.of("teenycraft:npc_teams/test_team.json", npc)
        );

        assertFalse(report.hasErrors());
        assertTrue(report.warnings().isEmpty());
    }

    @Test
    void parsesGoldenBonusIntoStructuredContract() {
        AbilityLoader.GoldenBonusData bonus = AbilityLoader.parseGoldenBonus("trait:tofu_chance:1.5,1.2");

        assertEquals(AbilityLoader.GoldenBonusScope.TRAIT, bonus.scope());
        assertEquals("tofu_chance", bonus.targetId());
        assertEquals(2, bonus.params().size());
        assertEquals(1.5f, bonus.params().get(0));
        assertEquals(1.2f, bonus.params().get(1));
    }

    @Test
    void rejectsUnknownGoldenEffectId() {
        JsonObject ability = json("""
                {
                  "id": "test_ability",
                  "effects_on_self": [],
                  "effects_on_opponent": [],
                  "traits": [],
                  "golden_bonus": ["self:not_real:1.0"]
                }
                """);

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                Map.of("teenycraft:abilities/test_ability.json", ability),
                Map.of(),
                Map.of()
        );

        assertTrue(report.hasErrors());
        assertTrue(report.errors().stream().anyMatch(issue -> issue.message().contains("unknown effect id 'not_real'")));
    }

    @Test
    void rejectsStoredEffectIdWithoutExplicitApplierContract() {
        JsonObject ability = json("""
                {
                  "id": "test_ability",
                  "effects_on_self": [{"id":"freeze_movement","params":[1.0]}],
                  "effects_on_opponent": [],
                  "traits": []
                }
                """);

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                Map.of("teenycraft:abilities/test_ability.json", ability),
                Map.of(),
                Map.of()
        );

        assertTrue(report.hasErrors());
        assertTrue(report.errors().stream().anyMatch(issue -> issue.message().contains("unknown effect id 'freeze_movement'")));
    }

    @Test
    void rejectsFigureWithMissingAbilityReference() {
        JsonObject ability = json("""
                {
                  "id": "real_ability",
                  "effects_on_self": [],
                  "effects_on_opponent": [],
                  "traits": []
                }
                """);
        JsonObject figure = json("""
                {"id":"test_figure","abilities":["missing_ability"]}
                """);

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                Map.of("teenycraft:abilities/real_ability.json", ability),
                Map.of("teenycraft:figures/test_figure.json", figure),
                Map.of()
        );

        assertTrue(report.hasErrors());
        assertTrue(report.errors().stream().anyMatch(issue -> issue.message().contains("unknown ability id 'missing_ability'")));
    }

    @Test
    void rejectsUnknownGoldenScope() {
        JsonObject ability = json("""
                {
                  "id": "test_ability",
                  "effects_on_self": [],
                  "effects_on_opponent": [],
                  "traits": [],
                  "golden_bonus": ["weird:power_up:0.3"]
                }
                """);

        BattleContentValidation.ValidationReport report = BattleContentValidation.validate(
                Map.of("teenycraft:abilities/test_ability.json", ability),
                Map.of(),
                Map.of()
        );

        assertTrue(report.hasErrors());
        assertTrue(report.errors().stream().anyMatch(issue -> issue.message().contains("unknown scope 'weird'")));
    }

    private static JsonObject json(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
