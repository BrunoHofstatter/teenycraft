package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessoryTierResolverTest {
    @Test
    void standardDamageAndIntervalUpgradesUseBaseValues() {
        AccessorySpec motherBox = require("mother_box");

        ResolvedAccessorySpec tier1 = AccessoryTierResolver.resolve(motherBox, 1);
        ResolvedAccessorySpec tier2 = AccessoryTierResolver.resolve(motherBox, 2);
        ResolvedAccessorySpec tier3 = AccessoryTierResolver.resolve(motherBox, 3);
        ResolvedAccessorySpec tier4 = AccessoryTierResolver.resolve(motherBox, 4);

        assertEquals(8, tier1.damage());
        assertEquals(12, tier2.damage());
        assertEquals(12, tier3.damage());
        assertEquals(30, tier4.intervalTicks());
        assertEquals(TeenyBalance.ACCESSORY_DRAIN_PER_TICK * 0.7f, tier3.batteryDrainPerTick(), 0.0001f);
    }

    @Test
    void tierFourStrengthReplacesTierTwoStrengthMultiplier() {
        ResolvedAccessorySpec titanCoin = AccessoryTierResolver.resolve(require("titans_coin"), 4);
        ResolvedAccessorySpec birdarang = AccessoryTierResolver.resolve(require("birdarang"), 4);

        assertEquals(0.625f, titanCoin.maxHpBonusPct(), 0.0001f);
        assertEquals(25, birdarang.damage());
    }

    @Test
    void specialEffectCurvesResolveExpectedValues() {
        assertEquals(40, AccessoryTierResolver.resolve(require("ravens_spellbook"), 1).effectMagnitude());
        assertEquals(52, AccessoryTierResolver.resolve(require("ravens_spellbook"), 2).effectMagnitude());
        assertEquals(72, AccessoryTierResolver.resolve(require("ravens_spellbook"), 4).effectMagnitude());
        assertEquals(120, AccessoryTierResolver.resolve(require("cyborgs_waffle_shooter"), 4).effectDurationTicks());
        assertEquals(63, AccessoryTierResolver.resolve(require("lil_penguin"), 2).effectMagnitude());
        assertEquals(105, AccessoryTierResolver.resolve(require("lil_penguin"), 4).intervalTicks());
        assertEquals(23, AccessoryTierResolver.resolve(require("kryptonite"), 4).effectMagnitude());
    }

    @Test
    void kryptoScalesAllThreePossibleEffectsAndThenItsInterval() {
        ResolvedAccessorySpec tier2 = AccessoryTierResolver.resolve(require("krypto_the_superdog"), 2);
        ResolvedAccessorySpec tier4 = AccessoryTierResolver.resolve(require("krypto_the_superdog"), 4);

        assertEquals(9, tier2.kryptoPowerUp());
        assertEquals(12, tier2.kryptoHeal());
        assertEquals(45.0f, tier2.kryptoTofuPower(), 0.0001f);
        assertEquals(30, tier4.intervalTicks());
    }

    @Test
    void masteredBatSignalAddsItsUniqueDamageIncrease() {
        ResolvedAccessorySpec tier4 = AccessoryTierResolver.resolve(require("bat_signal"), 4);
        ResolvedAccessorySpec tier5 = AccessoryTierResolver.resolve(require("bat_signal"), 5);

        assertEquals(90, tier4.damage());
        assertEquals(117, tier5.damage());
        assertEquals(tier4.intervalTicks(), tier5.intervalTicks());
    }

    @Test
    void masteredMotherBoxMakesEveryFifthPulseAQuadrupleCritical() {
        ResolvedAccessorySpec tier5 = AccessoryTierResolver.resolve(require("mother_box"), 5);

        assertEquals(1.0f, AccessoryExecutor.masteryCriticalMultiplierForHit(tier5, 4), 0.0001f);
        assertEquals(4.0f, AccessoryExecutor.masteryCriticalMultiplierForHit(tier5, 5), 0.0001f);
        assertEquals(4.0f, AccessoryExecutor.masteryCriticalMultiplierForHit(tier5, 10), 0.0001f);
    }

    @Test
    void unresolvedStrengthCurvesRemainAtBaseWhileTierThreeDrainStillApplies() {
        AccessorySpec justiceCoin = require("justice_league_coin");
        ResolvedAccessorySpec tier4 = AccessoryTierResolver.resolve(justiceCoin, 4);

        assertEquals(justiceCoin.getEffectMagnitude(), tier4.effectMagnitude());
        assertEquals(justiceCoin.getIntervalTicks(), tier4.intervalTicks());
        assertEquals(TeenyBalance.ACCESSORY_DRAIN_PER_TICK * 0.7f, tier4.batteryDrainPerTick(), 0.0001f);
    }

    @Test
    void multiHitDamagePreservesAccessoryOwnedBonusAcrossSplits() {
        bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult result =
                new bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult(30, 3, false, true);
        result.accessoryBonusDamage.put("red_lantern_battery", 9);
        result.forcedCriticalMultiplier = 4.0f;

        bruhof.teenycraft.battle.damage.DamagePipeline.DamageResult[] hits =
                bruhof.teenycraft.battle.damage.DamagePipeline.splitIntoHitResults(result);

        assertEquals(3, hits.length);
        assertEquals(9, java.util.Arrays.stream(hits)
                .mapToInt(hit -> hit.accessoryBonusDamage.getOrDefault("red_lantern_battery", 0)).sum());
        assertEquals(4.0f, hits[0].forcedCriticalMultiplier, 0.0001f);
    }

    private static AccessorySpec require(String id) {
        AccessorySpec spec = AccessoryRegistry.get(id);
        if (spec == null) {
            throw new IllegalStateException("Missing accessory spec: " + id);
        }
        return spec;
    }
}
