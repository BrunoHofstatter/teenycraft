package bruhof.teenycraft.accessory;

import bruhof.teenycraft.TeenyBalance;

import java.util.HashMap;
import java.util.Map;

public final class AccessoryMilestoneRegistry {
    private static final Map<String, AccessoryMilestoneDefinition> DEFINITIONS = new HashMap<>();

    static {
        for (String accessoryId : AccessoryRegistry.getIds()) {
            register(AccessoryMilestoneDefinition.lifetimeContribution(
                    accessoryId,
                    2,
                    AccessoryContribution.Type.ACTIVATION,
                    TeenyBalance.ACCESSORY_TIER_2_ACTIVATIONS_REQUIRED,
                    "Activate this accessory " + TeenyBalance.ACCESSORY_TIER_2_ACTIVATIONS_REQUIRED + " times.",
                    contribution -> contribution.amount()
            ));
        }
        registerTitansCoin();
        registerMotherBox();
        registerBatSignal();
        registerRedLanternBattery();
        registerGreenLanternBattery();
        registerVioletLanternBattery();
        registerRavensSpellbook();
        registerWaffleShooter();
        registerLilPenguin();
        registerKryptonite();
        registerBirdarang();
        registerSupermansUnderpants();
        registerKrypto();
    }

    private AccessoryMilestoneRegistry() {
    }

    public static AccessoryMilestoneDefinition get(String milestoneId) {
        return DEFINITIONS.get(milestoneId);
    }

    private static void register(AccessoryMilestoneDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    private static void registerTitansCoin() {
        register(amount("titans_coin", 3, AccessoryContribution.Type.MAX_HP_GRANTED,
                TeenyBalance.ACCESSORY_TITANS_COIN_TIER_3_HP_GRANTED,
                "Increase figure maximum HP by " + TeenyBalance.ACCESSORY_TITANS_COIN_TIER_3_HP_GRANTED + " total HP."));
        register(battles("titans_coin", 4,
                "Increase figure maximum HP by at least " + TeenyBalance.ACCESSORY_TITANS_COIN_TIER_4_HP_PER_BATTLE
                        + " in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.MAX_HP_GRANTED)
                        >= TeenyBalance.ACCESSORY_TITANS_COIN_TIER_4_HP_PER_BATTLE));
        register(count("titans_coin", 5, AccessoryContribution.Type.TITANS_COIN_ONE_HP_SURVIVAL,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED,
                "Have all 3 figures survive at 1 HP when Titan's Coin ends, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times."));
    }

    private static void registerMotherBox() {
        register(count("mother_box", 3, AccessoryContribution.Type.DAMAGE_HIT,
                TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_3_DAMAGE_HITS,
                "Damage an opponent with Mother Box " + TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_3_DAMAGE_HITS + " times."));
        register(battles("mother_box", 4,
                "Damage opponents with Mother Box at least " + TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_4_HITS_PER_BATTLE
                        + " times in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.DAMAGE_HIT)
                        >= TeenyBalance.ACCESSORY_MOTHER_BOX_TIER_4_HITS_PER_BATTLE));
        register(count("mother_box", 5, AccessoryContribution.Type.MOTHER_BOX_FULL_DEFEAT, 1,
                "Defeat a full-health figure using only Mother Box damage."));
    }

    private static void registerBatSignal() {
        register(count("bat_signal", 3, AccessoryContribution.Type.DAMAGE_HIT,
                TeenyBalance.ACCESSORY_BAT_SIGNAL_TIER_3_FIGURE_HITS,
                "Hit enemy figures with Bat Signal " + TeenyBalance.ACCESSORY_BAT_SIGNAL_TIER_3_FIGURE_HITS + " times."));
        register(battles("bat_signal", 4,
                "Defeat at least 1 figure with Bat Signal in "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " battles.",
                snapshot -> snapshot.total(AccessoryContribution.Type.FIGURE_DEFEATED) >= 1));
        register(AccessoryMilestoneDefinition.activationChallenge("bat_signal", 5,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED,
                "Defeat all 3 enemy figures with one Bat Signal barrage, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times.",
                snapshot -> snapshot.distinctTargets(AccessoryContribution.Type.FIGURE_DEFEATED)
                        >= TeenyBalance.ACCESSORY_BAT_SIGNAL_TIER_5_DEFEATS_PER_ACTIVATION));
    }

    private static void registerRedLanternBattery() {
        register(count("red_lantern_battery", 3, AccessoryContribution.Type.POWER_UP_GRANTED,
                TeenyBalance.ACCESSORY_RED_LANTERN_TIER_3_POWER_UPS,
                "Gain Power Up from Red Lantern Battery " + TeenyBalance.ACCESSORY_RED_LANTERN_TIER_3_POWER_UPS + " times."));
        register(battles("red_lantern_battery", 4,
                "Deal at least " + TeenyBalance.ACCESSORY_RED_LANTERN_TIER_4_BONUS_DAMAGE_PER_BATTLE
                        + " accessory-owned Power Up bonus damage in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.POWER_UP_BONUS_DAMAGE)
                        >= TeenyBalance.ACCESSORY_RED_LANTERN_TIER_4_BONUS_DAMAGE_PER_BATTLE));
        register(AccessoryMilestoneDefinition.qualifyingBattles("red_lantern_battery", 5,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED, false,
                "Contribute at least " + TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_FULL_FIGURE_DAMAGE
                        + " Power Up bonus damage to 2 defeated figures in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times.",
                snapshot -> snapshot.targetsMeetingTotalAndPresent(AccessoryContribution.Type.POWER_UP_BONUS_DAMAGE,
                        TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_FULL_FIGURE_DAMAGE,
                        AccessoryContribution.Type.FIGURE_DEFEATED)
                        >= TeenyBalance.ACCESSORY_RED_LANTERN_TIER_5_FIGURES_PER_BATTLE));
    }

    private static void registerGreenLanternBattery() {
        register(count("green_lantern_battery", 3, AccessoryContribution.Type.MANA_GRANTED,
                TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_3_MANA_GRANTS,
                "Gain mana from Green Lantern Battery " + TeenyBalance.ACCESSORY_GREEN_LANTERN_TIER_3_MANA_GRANTS + " times."));
        register(battles("green_lantern_battery", 4,
                "Go from 5 or less mana to 100 without swapping while the accessory is active, in "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " battles.",
                snapshot -> snapshot.total(AccessoryContribution.Type.MANA_FILLED_FROM_LOW) >= 1));
    }

    private static void registerVioletLanternBattery() {
        register(count("violet_lantern_battery", 3, AccessoryContribution.Type.HEALING_DONE,
                TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_3_HEALS,
                "Restore missing HP with Violet Lantern Battery " + TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_3_HEALS + " times."));
        register(battles("violet_lantern_battery", 4,
                "Heal one figure by at least " + TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_4_HEAL_PER_FIGURE
                        + " HP in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.targetsMeetingTotal(AccessoryContribution.Type.HEALING_DONE,
                        TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_4_HEAL_PER_FIGURE) >= 1));
        register(count("violet_lantern_battery", 5, AccessoryContribution.Type.HEALED_FROM_LOW_TO_TARGET,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED,
                "Heal one figure from 1-5 HP to " + TeenyBalance.ACCESSORY_VIOLET_LANTERN_TIER_5_TARGET_HP
                        + " using only this accessory and without swapping, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times."));
    }

    private static void registerRavensSpellbook() {
        register(filteredCount("ravens_spellbook", 3, AccessoryContribution.Type.EFFECT_APPLIED, "curse",
                TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_3_CURSES,
                "Apply Curse with Raven's Spellbook " + TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_3_CURSES + " times."));
        register(battles("ravens_spellbook", 4,
                "Curse at least 2 different enemy figures in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.distinctTargets(AccessoryContribution.Type.EFFECT_APPLIED)
                        >= TeenyBalance.ACCESSORY_RAVENS_SPELLBOOK_TIER_4_TARGETS_PER_BATTLE));
    }

    private static void registerWaffleShooter() {
        register(count("cyborgs_waffle_shooter", 3, AccessoryContribution.Type.WAFFLE_APPLIED,
                TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_3_WAFFLES,
                "Shoot " + TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_3_WAFFLES + " waffles onto opponents."));
        register(battles("cyborgs_waffle_shooter", 4,
                "Waffle all 3 ability slots of one figure in a battle, "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.targetsMeetingDistinctDetails(AccessoryContribution.Type.WAFFLE_APPLIED,
                        TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_ABILITY_SLOTS) >= 1));
        register(AccessoryMilestoneDefinition.qualifyingBattles("cyborgs_waffle_shooter", 5,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED, false,
                "Waffle all 3 ability slots of all 3 enemy figures in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times.",
                snapshot -> snapshot.targetsMeetingDistinctDetails(AccessoryContribution.Type.WAFFLE_APPLIED,
                        TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_ABILITY_SLOTS)
                        >= TeenyBalance.ACCESSORY_WAFFLE_SHOOTER_TIER_5_FIGURES_PER_BATTLE));
    }

    private static void registerLilPenguin() {
        register(filteredCount("lil_penguin", 3, AccessoryContribution.Type.EFFECT_APPLIED, "freeze",
                TeenyBalance.ACCESSORY_LIL_PENGUIN_TIER_3_FREEZES,
                "Freeze opponents " + TeenyBalance.ACCESSORY_LIL_PENGUIN_TIER_3_FREEZES + " times."));
        register(battles("lil_penguin", 4,
                "Burn at least " + TeenyBalance.ACCESSORY_LIL_PENGUIN_TIER_4_MANA_PER_BATTLE
                        + " enemy mana in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.MANA_BURNED)
                        >= TeenyBalance.ACCESSORY_LIL_PENGUIN_TIER_4_MANA_PER_BATTLE));
    }

    private static void registerKryptonite() {
        register(filteredCount("kryptonite", 3, AccessoryContribution.Type.EFFECT_APPLIED, "defense_down",
                TeenyBalance.ACCESSORY_KRYPTONITE_TIER_3_APPLICATIONS,
                "Apply Defense Down with Kryptonite " + TeenyBalance.ACCESSORY_KRYPTONITE_TIER_3_APPLICATIONS + " times."));
        register(battles("kryptonite", 4,
                "Deal at least " + TeenyBalance.ACCESSORY_KRYPTONITE_TIER_4_BONUS_DAMAGE_PER_BATTLE
                        + " bonus damage from Kryptonite's Defense Down in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.DEFENSE_DOWN_BONUS_DAMAGE)
                        >= TeenyBalance.ACCESSORY_KRYPTONITE_TIER_4_BONUS_DAMAGE_PER_BATTLE));
    }

    private static void registerBirdarang() {
        register(count("birdarang", 3, AccessoryContribution.Type.RETALIATION_HIT,
                TeenyBalance.ACCESSORY_BIRDARANG_TIER_3_HITS,
                "Damage opponents with Birdarang " + TeenyBalance.ACCESSORY_BIRDARANG_TIER_3_HITS + " times."));
        register(battles("birdarang", 4,
                "Hit one figure with Birdarang at least " + TeenyBalance.ACCESSORY_BIRDARANG_TIER_4_HITS_ON_ONE_FIGURE
                        + " times in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.targetsMeetingTotal(AccessoryContribution.Type.RETALIATION_HIT,
                        TeenyBalance.ACCESSORY_BIRDARANG_TIER_4_HITS_ON_ONE_FIGURE) >= 1));
        register(AccessoryMilestoneDefinition.qualifyingBattles("birdarang", 5,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED, false,
                "Defeat all 3 enemy figures with Birdarang in one battle, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times.",
                snapshot -> snapshot.distinctTargets(AccessoryContribution.Type.FIGURE_DEFEATED)
                        >= TeenyBalance.ACCESSORY_BIRDARANG_TIER_5_DEFEATS_PER_BATTLE));
    }

    private static void registerSupermansUnderpants() {
        register(count("supermans_underpants", 3, AccessoryContribution.Type.DAMAGE_BLOCKED,
                TeenyBalance.ACCESSORY_UNDERPANTS_TIER_3_BLOCK_EVENTS,
                "Block damage with Superman's Underpants " + TeenyBalance.ACCESSORY_UNDERPANTS_TIER_3_BLOCK_EVENTS + " times."));
        register(battles("supermans_underpants", 4,
                "Block at least " + TeenyBalance.ACCESSORY_UNDERPANTS_TIER_4_BLOCKED_DAMAGE_PER_BATTLE
                        + " damage in one battle, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.DAMAGE_BLOCKED)
                        >= TeenyBalance.ACCESSORY_UNDERPANTS_TIER_4_BLOCKED_DAMAGE_PER_BATTLE));
        register(AccessoryMilestoneDefinition.activationChallenge("supermans_underpants", 5,
                TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED,
                "Block 2 fatal hits during one accessory activation, "
                        + TeenyBalance.ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.FATAL_HIT_BLOCKED)
                        >= TeenyBalance.ACCESSORY_UNDERPANTS_TIER_5_FATAL_BLOCKS_PER_ACTIVATION));
    }

    private static void registerKrypto() {
        register(count("krypto_the_superdog", 3, AccessoryContribution.Type.KRYPTO_EFFECT_GRANTED,
                TeenyBalance.ACCESSORY_KRYPTO_TIER_3_BUFFS,
                "Gain a useful effect from Krypto " + TeenyBalance.ACCESSORY_KRYPTO_TIER_3_BUFFS + " times."));
        register(battles("krypto_the_superdog", 4,
                "In one battle, heal at least " + TeenyBalance.ACCESSORY_KRYPTO_TIER_4_HEAL_PER_BATTLE
                        + " HP and deal at least " + TeenyBalance.ACCESSORY_KRYPTO_TIER_4_BONUS_DAMAGE_PER_BATTLE
                        + " Krypto Power Up bonus damage, " + TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED + " times.",
                snapshot -> snapshot.total(AccessoryContribution.Type.HEALING_DONE)
                        >= TeenyBalance.ACCESSORY_KRYPTO_TIER_4_HEAL_PER_BATTLE
                        && snapshot.total(AccessoryContribution.Type.POWER_UP_BONUS_DAMAGE)
                        >= TeenyBalance.ACCESSORY_KRYPTO_TIER_4_BONUS_DAMAGE_PER_BATTLE));
    }

    private static AccessoryMilestoneDefinition count(String accessoryId, int tier,
                                                       AccessoryContribution.Type type, long target,
                                                       String description) {
        return AccessoryMilestoneDefinition.lifetimeContribution(accessoryId, tier, type, target,
                description, contribution -> 1);
    }

    private static AccessoryMilestoneDefinition filteredCount(String accessoryId, int tier,
                                                               AccessoryContribution.Type type, String detail,
                                                               long target, String description) {
        return AccessoryMilestoneDefinition.lifetimeContribution(accessoryId, tier, type, target,
                description, contribution -> detail.equals(contribution.detail()) ? 1 : 0);
    }

    private static AccessoryMilestoneDefinition amount(String accessoryId, int tier,
                                                        AccessoryContribution.Type type, long target,
                                                        String description) {
        return AccessoryMilestoneDefinition.lifetimeContribution(accessoryId, tier, type, target,
                description, AccessoryContribution::amount);
    }

    private static AccessoryMilestoneDefinition battles(String accessoryId, int tier, String description,
                                                         java.util.function.Predicate<AccessoryProgressSnapshot> condition) {
        return AccessoryMilestoneDefinition.qualifyingBattles(accessoryId, tier,
                TeenyBalance.ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED, false, description, condition);
    }
}
