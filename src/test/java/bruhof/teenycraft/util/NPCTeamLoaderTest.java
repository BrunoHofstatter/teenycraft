package bruhof.teenycraft.util;

import bruhof.teenycraft.battle.ai.BattleAiProfile;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCTeamLoaderTest {
    @Test
    void missingAiBlockKeepsBattleAiEnabled() {
        BattleAiProfile profile = NPCTeamLoader.parseAiProfile(new JsonObject());

        assertTrue(profile.enabled());
    }

    @Test
    void missingEnabledFieldKeepsBattleAiEnabled() {
        JsonObject teamJson = new JsonObject();
        JsonObject aiJson = new JsonObject();
        aiJson.addProperty("difficulty", 1);
        teamJson.add("ai", aiJson);

        BattleAiProfile profile = NPCTeamLoader.parseAiProfile(teamJson);

        assertTrue(profile.enabled());
    }

    @Test
    void enabledFalseDisablesBattleAi() {
        JsonObject teamJson = new JsonObject();
        JsonObject aiJson = new JsonObject();
        aiJson.addProperty("enabled", false);
        teamJson.add("ai", aiJson);

        BattleAiProfile profile = NPCTeamLoader.parseAiProfile(teamJson);

        assertFalse(profile.enabled());
    }
}
