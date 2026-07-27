package bruhof.teenycraft.group;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.util.FigureGroupLoader;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FigureGroupDefinitionTest {
    @Test
    void parsesMembershipAndMultipleEffects() {
        FigureGroupDefinition group = FigureGroupLoader.parseDefinition(JsonParser.parseString("""
                {
                  "id":"titans",
                  "name":"Titans",
                  "priority":2,
                  "figures":["robin","raven"],
                  "combo_effects":["stat_health","stat_luck"]
                }
                """).getAsJsonObject());

        assertEquals("titans", group.id());
        assertEquals(2, group.priority());
        assertTrue(group.figureIds().containsAll(java.util.Set.of("robin", "raven")));
        assertEquals(java.util.List.of("stat_health", "stat_luck"), group.comboEffectIds());
    }

    @Test
    void combinesReusableEffectBonuses() {
        FigureGroupDefinition group = new FigureGroupDefinition(
                "titans", "Titans", 1, java.util.Set.of("robin", "raven"),
                java.util.List.of("stat_health", "stat_power")
        );

        GroupComboStatBonus bonus = GroupComboExecutor.resolveStatBonus(group);

        assertEquals(TeenyBalance.GROUP_COMBO_HEALTH_BONUS, bonus.health());
        assertEquals(TeenyBalance.GROUP_COMBO_POWER_BONUS, bonus.power());
        assertEquals(0, bonus.dodge());
        assertEquals(0, bonus.luck());
    }

    @Test
    void choosesPriorityThenAlphabeticalUnlessPlayerOverrides() {
        FigureGroupDefinition zeta = group("zeta", 1);
        FigureGroupDefinition alpha = group("alpha", 1);
        FigureGroupDefinition priority = group("priority", 2);
        java.util.List<FigureGroupDefinition> eligible = java.util.List.of(zeta, alpha, priority);

        assertEquals("priority", FigureGroupResolver.resolve(eligible, "").orElseThrow().id());
        assertEquals("alpha", FigureGroupResolver.resolve(java.util.List.of(zeta, alpha), "").orElseThrow().id());
        assertEquals("zeta", FigureGroupResolver.resolve(eligible, "zeta").orElseThrow().id());
        assertEquals("priority", FigureGroupResolver.resolve(eligible, "missing").orElseThrow().id());
    }

    private static FigureGroupDefinition group(String id, int priority) {
        return new FigureGroupDefinition(id, id, priority, java.util.Set.of("a", "b"), java.util.List.of("none"));
    }
}
