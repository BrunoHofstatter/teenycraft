package bruhof.teenycraft.accessory;

import bruhof.teenycraft.battle.effect.EffectInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryBattleProgressTrackerTest {
    @Test
    void activationSnapshotResetsWhileBattleSnapshotKeepsAccumulating() {
        AccessoryBattleProgressTracker tracker = new AccessoryBattleProgressTracker();
        tracker.beginBattle();
        tracker.beginActivation("mother_box", 3);
        tracker.record(new AccessoryContribution("mother_box", AccessoryContribution.Type.DAMAGE_DEALT,
                12, 0, ""));
        tracker.record(new AccessoryContribution("mother_box", AccessoryContribution.Type.DAMAGE_HIT,
                1, 0, ""));

        AccessoryProgressSnapshot firstActivation = tracker.endActivation();
        assertEquals(12, firstActivation.total(AccessoryContribution.Type.DAMAGE_DEALT));
        assertEquals(1, firstActivation.distinctTargets(AccessoryContribution.Type.DAMAGE_HIT));

        tracker.beginActivation("mother_box", 3);
        tracker.record(new AccessoryContribution("mother_box", AccessoryContribution.Type.DAMAGE_DEALT,
                8, 1, ""));
        AccessoryProgressSnapshot secondActivation = tracker.endActivation();
        assertEquals(8, secondActivation.total(AccessoryContribution.Type.DAMAGE_DEALT));

        AccessoryProgressSnapshot battle = tracker.battleSnapshot();
        assertEquals(20, battle.total(AccessoryContribution.Type.DAMAGE_DEALT));
        assertEquals(3, battle.tier());
    }

    @Test
    void snapshotTracksDistinctTargetsAndSemanticDetails() {
        AccessoryBattleProgressTracker tracker = new AccessoryBattleProgressTracker();
        tracker.beginBattle();
        tracker.beginActivation("krypto_the_superdog", 2);
        tracker.record(new AccessoryContribution("krypto_the_superdog",
                AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED, 1, 0, "heal"));
        tracker.record(new AccessoryContribution("krypto_the_superdog",
                AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED, 1, 0, "power_up"));

        AccessoryProgressSnapshot snapshot = tracker.endActivation();
        assertEquals(2, snapshot.total(AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED));
        assertEquals(1, snapshot.distinctTargets(AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED));
        assertTrue(snapshot.hasDetail(AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED, "heal"));
        assertTrue(snapshot.hasDetail(AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED, "power_up"));
        assertFalse(snapshot.hasDetail(AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED, "tofu"));
    }

    @Test
    void snapshotTracksTotalsAndDetailsPerFigure() {
        AccessoryBattleProgressTracker tracker = new AccessoryBattleProgressTracker();
        tracker.beginBattle();
        tracker.beginActivation("cyborgs_waffle_shooter", 4);
        tracker.record(new AccessoryContribution("cyborgs_waffle_shooter",
                AccessoryContribution.Type.WAFFLE_APPLIED, 1, 0, "slot:0"));
        tracker.record(new AccessoryContribution("cyborgs_waffle_shooter",
                AccessoryContribution.Type.WAFFLE_APPLIED, 1, 0, "slot:1"));
        tracker.record(new AccessoryContribution("cyborgs_waffle_shooter",
                AccessoryContribution.Type.WAFFLE_APPLIED, 1, 0, "slot:2"));
        tracker.record(new AccessoryContribution("cyborgs_waffle_shooter",
                AccessoryContribution.Type.HEALING_DONE, 25, 1, "heal"));
        tracker.record(new AccessoryContribution("cyborgs_waffle_shooter",
                AccessoryContribution.Type.HEALING_DONE, 20, 1, "heal"));

        AccessoryProgressSnapshot snapshot = tracker.battleSnapshot();
        assertEquals(3, snapshot.distinctDetailsForTarget(AccessoryContribution.Type.WAFFLE_APPLIED, 0));
        assertEquals(45, snapshot.totalForTarget(AccessoryContribution.Type.HEALING_DONE, 1));
        assertEquals(1, snapshot.targetsMeetingDistinctDetails(AccessoryContribution.Type.WAFFLE_APPLIED, 3));
        assertEquals(1, snapshot.targetsMeetingTotal(AccessoryContribution.Type.HEALING_DONE, 40));
    }

    @Test
    void universalTierTwoDefinitionCountsActivations() {
        AccessoryMilestoneDefinition definition = AccessoryMilestoneRegistry.get("mother_box_tier_2");
        AccessoryContribution activation = AccessoryContribution.count(
                "mother_box", AccessoryContribution.Type.ACTIVATION);

        assertEquals(AccessoryMilestoneDefinition.Trigger.CONTRIBUTION, definition.trigger());
        assertEquals(3, definition.target());
        assertEquals(1, definition.evaluator().applyAsLong(
                new AccessoryMilestoneDefinition.Evaluation(activation, null, null, false)));
    }

    @Test
    void qualifyingBattleDefinitionAwardsAtMostOneCompletion() {
        AccessoryMilestoneDefinition definition = AccessoryMilestoneDefinition.qualifyingBattles(
                "mother_box", 4, 50, false,
                snapshot -> snapshot.total(AccessoryContribution.Type.DAMAGE_HIT) >= 20);
        AccessoryBattleProgressTracker tracker = new AccessoryBattleProgressTracker();
        tracker.beginBattle();
        tracker.beginActivation("mother_box", 3);
        tracker.record(new AccessoryContribution("mother_box", AccessoryContribution.Type.DAMAGE_HIT,
                25, 0, ""));

        long increment = definition.evaluator().applyAsLong(
                new AccessoryMilestoneDefinition.Evaluation(null, null, tracker.battleSnapshot(), false));
        assertEquals(1, increment);
    }

    @Test
    void stackedEffectRetainsAccessoryOwnedMagnitude() {
        EffectInstance effect = new EffectInstance(-1, 6, 0, null,
                EffectInstance.NO_CASTER_FIGURE_INDEX, "red_lantern_battery");
        effect.addAccessoryMagnitude("red_lantern_battery", 9);

        assertEquals(15, effect.getAccessoryMagnitude("red_lantern_battery"));
        effect.clearAccessoryMagnitudes();
        assertEquals(0, effect.getAccessoryMagnitude("red_lantern_battery"));
    }

    @Test
    void registeredMilestonesMatchLiveSheetCoverage() {
        String[] tierThreeAndFour = {
                "titans_coin", "mother_box", "bat_signal", "red_lantern_battery",
                "green_lantern_battery", "violet_lantern_battery", "ravens_spellbook",
                "cyborgs_waffle_shooter", "lil_penguin", "kryptonite", "birdarang",
                "supermans_underpants", "krypto_the_superdog"
        };
        for (String accessoryId : tierThreeAndFour) {
            assertTrue(AccessoryMilestoneRegistry.get(accessoryId + "_tier_3") != null, accessoryId + " tier 3");
            assertTrue(AccessoryMilestoneRegistry.get(accessoryId + "_tier_4") != null, accessoryId + " tier 4");
        }

        String[] tierFive = {
                "titans_coin", "mother_box", "bat_signal", "red_lantern_battery",
                "violet_lantern_battery", "cyborgs_waffle_shooter", "birdarang",
                "supermans_underpants"
        };
        for (String accessoryId : tierFive) {
            assertTrue(AccessoryMilestoneRegistry.get(accessoryId + "_tier_5") != null, accessoryId + " tier 5");
        }

        assertTrue(AccessoryMilestoneRegistry.get("justice_league_coin_tier_3") == null);
        assertTrue(AccessoryMilestoneRegistry.get("green_lantern_battery_tier_5") == null);
        assertTrue(AccessoryMilestoneRegistry.get("ravens_spellbook_tier_5") == null);
        assertTrue(AccessoryMilestoneRegistry.get("lil_penguin_tier_5") == null);
        assertTrue(AccessoryMilestoneRegistry.get("kryptonite_tier_5") == null);
        assertTrue(AccessoryMilestoneRegistry.get("krypto_the_superdog_tier_5") == null);
    }

    @Test
    void eventCountMilestoneCountsApplicationsInsteadOfMagnitude() {
        AccessoryMilestoneDefinition definition = AccessoryMilestoneRegistry.get("violet_lantern_battery_tier_3");
        AccessoryContribution heal = new AccessoryContribution("violet_lantern_battery",
                AccessoryContribution.Type.HEALING_DONE, 12, 0, "heal");

        assertEquals(1, definition.evaluator().applyAsLong(
                new AccessoryMilestoneDefinition.Evaluation(heal, null, null, false)));
    }
}
