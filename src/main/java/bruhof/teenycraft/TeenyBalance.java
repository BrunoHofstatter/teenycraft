package bruhof.teenycraft;

public class TeenyBalance {

    private static final int[][] MANA_COSTS_BY_SLOT = {
            {},
            {6, 8, 10, 12, 14},
            {25, 28, 31, 34, 37},
            {80, 84, 88, 92, 96}
    };

    private static final float[] EFFECTIVE_MANA_MULTIPLIERS_BY_SLOT = {
            0.0f, 1.0f, 1.0f, 1.15f
    };

    private static final float[] DAMAGE_MULTIPLIERS_BY_TIER = {
            0.0f, 0.82f, 0.85f, 0.88f, 0.91f, 0.94f, 0.97f,
            1.00f, 1.03f, 1.06f, 1.09f, 1.12f, 1.15f, 1.18f, 1.21f, 1.24f, 1.27f, 1.3f, 1.33f, 1.36f
    };

    private static final float[] RAYCAST_DELAYS_BY_TIER = {
            0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f,
            2.0f, 2.5f, 3.0f, 3.5f, 4.0f
    };

    private static final int[] RANGE_VALUES_BY_TIER = {
            0, 11, 13, 15, 17, 19, 21, 23, 26
    };

    private static final int[] SHUFFLE_BAG_CHECKPOINTS = {
            0, 5, 13, 25, 40, 60, 80
    };

    private static final int[] SHUFFLE_BAG_SIZES = {
            10, 9, 8, 7, 6, 5, 4
    };

    // ==========================================
    // SECTION 1: FIGURE PROGRESSION
    // ==========================================
    public static final int MAX_LEVEL = 20;
    public static final int[] FIGURE_XP_REQUIRED_BY_LEVEL = {
            100, 200, 300, 400, 500,
            600, 700, 800, 900, 1000,
            1100, 1200, 1300, 1400, 1500,
            1600, 1700, 1800, 1900
    };
    public static final int UPGRADE_GAIN_HP = 20;
    public static final int UPGRADE_GAIN_POWER = 5;
    public static final int UPGRADE_GAIN_DODGE = 8;
    public static final int UPGRADE_GAIN_LUCK = 8;
    public static final int FIGURE_REORDER_MIN_LEVEL = 7;
    public static final int FIGURE_REORDER_COST = 250;

    // Golden ability acquisition
    private static final int[] GOLDEN_REQUIRED_POINTS_BY_AUTHORED_SLOT = {20, 24, 28};
    private static final int[] GOLDEN_EXACT_COMBO_BONUS_BY_COUNT = {0, 0, 2, 5, 7, 10};
    private static final int[] GOLDEN_CLASS_COMBO_BONUS_BY_COUNT = {0, 0, 0, 1, 2, 3};
    private static final int[] GOLDEN_GROUP_COMBO_BONUS_BY_DISTINCT_COUNT = {0, 0, 0, 3, 3, 5};
    public static final int GOLDEN_MAX_SACRIFICES = 5;
    public static final int GOLDEN_BASE_EXACT_POINTS = 5;
    public static final int GOLDEN_BASE_GROUP_POINTS = 3;
    public static final int GOLDEN_BASE_CLASS_POINTS = 2;
    public static final int GOLDEN_BASE_UNRELATED_POINTS = 1;
    public static final int GOLDEN_CLASS_COMBO_MAX_COPIES_PER_FIGURE_ID = 2;
    public static final int GOLDEN_MIN_COMPLETE_GROUP_SIZE = 2;
    public static final int GOLDEN_COMPLETE_GROUP_BONUS_PER_MEMBER = 1;

    public static int getGoldenRequiredPoints(int authoredAbilityIndex) {
        if (authoredAbilityIndex < 0 || authoredAbilityIndex >= GOLDEN_REQUIRED_POINTS_BY_AUTHORED_SLOT.length) {
            return 0;
        }
        return GOLDEN_REQUIRED_POINTS_BY_AUTHORED_SLOT[authoredAbilityIndex];
    }

    public static int getGoldenExactComboBonus(int exactCount) {
        int index = Math.max(0, Math.min(exactCount, GOLDEN_EXACT_COMBO_BONUS_BY_COUNT.length - 1));
        return GOLDEN_EXACT_COMBO_BONUS_BY_COUNT[index];
    }

    public static int getGoldenClassComboBonus(int qualifyingCount) {
        int index = Math.max(0, Math.min(qualifyingCount, GOLDEN_CLASS_COMBO_BONUS_BY_COUNT.length - 1));
        return GOLDEN_CLASS_COMBO_BONUS_BY_COUNT[index];
    }

    public static int getGoldenGroupComboBonus(int distinctCount) {
        int index = Math.max(0, Math.min(distinctCount, GOLDEN_GROUP_COMBO_BONUS_BY_DISTINCT_COUNT.length - 1));
        return GOLDEN_GROUP_COMBO_BONUS_BY_DISTINCT_COUNT[index];
    }

    // Figure group combos use their own balance hooks even when the initial
    // values intentionally match one normal figure upgrade step.
    public static final int GROUP_COMBO_HEALTH_BONUS = UPGRADE_GAIN_HP;
    public static final int GROUP_COMBO_POWER_BONUS = UPGRADE_GAIN_POWER;
    public static final int GROUP_COMBO_DODGE_BONUS = UPGRADE_GAIN_DODGE;
    public static final int GROUP_COMBO_LUCK_BONUS = UPGRADE_GAIN_LUCK;

    // ==========================================
    // SECTION 2: BATTLE CORE
    // ==========================================
    public static final int BATTLE_MANA_MAX = 100;
    public static final int BATTLE_MANA_REGEN_PER_SEC = 5;
    public static final int SWAP_COOLDOWN = 5;
    public static final double ARENA_PICKUP_COLLECTION_RADIUS = 1.0d;
    public static final double AI_DUMMY_BASE_MOVE_SPEED = 0.15d;
    public static final double AI_APPROACH_MOVE_SPEED = 1.2d;
    public static final double AI_RANGE_APPROACH_MOVE_SPEED = 1.0d;
    public static final double AI_RETREAT_MOVE_SPEED = 1.25d;
    public static final double AI_REPEAT_SLOT_SOFT_PENALTY = 1.35d;
    public static final int AI_MAX_SAME_SLOT_STREAK = 3;
    public static final float AI_NEAR_READY_MELEE_MANA_PCT = 0.85f;
    public static final float AI_EFFECTIVE_MANA_VALUE_WEIGHT = 5.5f;
    public static final float AI_HIGH_MANA_EFFECTIVE_VALUE_WEIGHT = 8.0f;
    public static final float AI_HIGH_MANA_SLOT3_PRIORITY_BONUS = 1.4f;
    public static final float AI_FULL_MANA_SLOT3_PRIORITY_BONUS = 1.0f;
    public static final float AI_SELF_HEAL_CRITICAL_HP_PCT = 0.25f;
    public static final float AI_GROUP_HEAL_SELF_ONLY_HP_PCT = 0.35f;
    public static final float AI_GROUP_HEAL_ALLY_LOW_HP_PCT = 0.75f;
    public static final float AI_HEALTH_RADIO_MIN_MISSING_HP_PCT = 0.05f;
    public static final float AI_BATTERY_DRAIN_MIN_HP_PCT = 0.65f;
    public static final float AI_REMOTE_MINE_DETONATE_MIN_CHARGE_PCT = 0.70f;
    public static final float AI_OPPONENT_HIGH_MANA_PCT = 0.70f;
    public static final int AI_REFLECT_REUSE_TICKS = 60;
    public static final double AI_THREAT_BAND_MIN_DISTANCE = 3.4d;
    public static final double AI_THREAT_BAND_MIN_MAX_DISTANCE = 7.5d;
    public static final double AI_THREAT_BAND_RANGE_PADDING = 2.0d;
    public static final double AI_FAR_DISTANCE_PADDING = 1.0d;

    private static final double[] ARENA_SPEED_LEVEL_MULTIPLIERS = {
            0.10d, 0.20d, 0.30d, 0.40d, 0.50d,
            0.60d, 0.70d, 0.80d, 0.90d, 1.00d
    };

    public static final float RANGED_START_WIDTH = 1.5f;
    public static final float RANGED_CONE_ANGLE = 15.0f;

    // Battery
    public static final float BATTERY_MAX_CHARGE = 100.0f;
    public static final float BATTERY_PASSIVE_CHARGE_RATE = 0.0125f;
    public static final float ABILITY_BATTERY_CHARGE_MULT = 0.125f;
    public static final float BATTERY_COLLECT_CHARGE = 5.0f;
    public static final int BATTERY_SPAWN_MIN_TICKS = 200;
    public static final int BATTERY_SPAWN_MAX_TICKS = 250;
    public static final float BATTERY_SPAWN_MIN_PCT = 0.70f;
    public static final float BATTERY_SPAWN_MAX_PCT = 0.90f;

    // Accessories
    public static final float ACCESSORY_ACTIVATION_MIN_CHARGE = 50.0f;
    public static final float ACCESSORY_DRAIN_PER_TICK = 0.2f;
    public static final int[] ACCESSORY_TIER_UPGRADE_COSTS = {250, 500, 1000, 2000};
    public static final int ACCESSORY_TIER_2_ACTIVATIONS_REQUIRED = 3;
    public static final int ACCESSORY_TIER_4_QUALIFYING_BATTLES_REQUIRED = 50;
    public static final int ACCESSORY_TIER_5_QUALIFYING_RUNS_REQUIRED = 5;
    public static final int ACCESSORY_TITANS_COIN_TIER_3_HP_GRANTED = 200;
    public static final int ACCESSORY_TITANS_COIN_TIER_4_HP_PER_BATTLE = 100;
    public static final int ACCESSORY_TITANS_COIN_TIER_5_FIGURES_AT_ONE_HP = 3;
    public static final int ACCESSORY_MOTHER_BOX_TIER_3_DAMAGE_HITS = 50;
    public static final int ACCESSORY_MOTHER_BOX_TIER_4_HITS_PER_BATTLE = 20;
    public static final int ACCESSORY_BAT_SIGNAL_TIER_3_FIGURE_HITS = 15;
    public static final int ACCESSORY_BAT_SIGNAL_TIER_5_DEFEATS_PER_ACTIVATION = 3;
    public static final int ACCESSORY_RED_LANTERN_TIER_3_POWER_UPS = 50;
    public static final int ACCESSORY_RED_LANTERN_TIER_4_BONUS_DAMAGE_PER_BATTLE = 50;
    public static final int ACCESSORY_RED_LANTERN_TIER_5_FULL_FIGURE_DAMAGE = 130;
    public static final int ACCESSORY_RED_LANTERN_TIER_5_FIGURES_PER_BATTLE = 2;
    public static final int ACCESSORY_GREEN_LANTERN_TIER_3_MANA_GRANTS = 50;
    public static final int ACCESSORY_GREEN_LANTERN_TIER_4_START_MANA_MAX = 5;
    public static final int ACCESSORY_VIOLET_LANTERN_TIER_3_HEALS = 30;
    public static final int ACCESSORY_VIOLET_LANTERN_TIER_4_HEAL_PER_FIGURE = 40;
    public static final int ACCESSORY_VIOLET_LANTERN_TIER_5_START_HP_MAX = 5;
    public static final int ACCESSORY_VIOLET_LANTERN_TIER_5_TARGET_HP = 130;
    public static final int ACCESSORY_RAVENS_SPELLBOOK_TIER_3_CURSES = 10;
    public static final int ACCESSORY_RAVENS_SPELLBOOK_TIER_4_TARGETS_PER_BATTLE = 2;
    public static final int ACCESSORY_WAFFLE_SHOOTER_TIER_3_WAFFLES = 50;
    public static final int ACCESSORY_WAFFLE_SHOOTER_ABILITY_SLOTS = 3;
    public static final int ACCESSORY_WAFFLE_SHOOTER_TIER_5_FIGURES_PER_BATTLE = 3;
    public static final int ACCESSORY_LIL_PENGUIN_TIER_3_FREEZES = 30;
    public static final int ACCESSORY_LIL_PENGUIN_TIER_4_MANA_PER_BATTLE = 200;
    public static final int ACCESSORY_KRYPTONITE_TIER_3_APPLICATIONS = 10;
    public static final int ACCESSORY_KRYPTONITE_TIER_4_BONUS_DAMAGE_PER_BATTLE = 50;
    public static final int ACCESSORY_BIRDARANG_TIER_3_HITS = 20;
    public static final int ACCESSORY_BIRDARANG_TIER_4_HITS_ON_ONE_FIGURE = 5;
    public static final int ACCESSORY_BIRDARANG_TIER_5_DEFEATS_PER_BATTLE = 3;
    public static final int ACCESSORY_UNDERPANTS_TIER_3_BLOCK_EVENTS = 10;
    public static final int ACCESSORY_UNDERPANTS_TIER_4_BLOCKED_DAMAGE_PER_BATTLE = 50;
    public static final int ACCESSORY_UNDERPANTS_TIER_5_FATAL_BLOCKS_PER_ACTIVATION = 2;
    public static final int ACCESSORY_KRYPTO_TIER_3_BUFFS = 50;
    public static final int ACCESSORY_KRYPTO_TIER_4_HEAL_PER_BATTLE = 20;
    public static final int ACCESSORY_KRYPTO_TIER_4_BONUS_DAMAGE_PER_BATTLE = 20;
    public static final float ACCESSORY_TIER_2_STANDARD_STRENGTH_MULT = 1.5f;
    public static final float ACCESSORY_TIER_4_STANDARD_STRENGTH_MULT = 2.5f;
    public static final float ACCESSORY_TIER_3_BATTERY_DRAIN_MULT = 0.7f;
    public static final float ACCESSORY_TIER_4_INTERVAL_MULT = 0.75f;
    public static final float ACCESSORY_RAVENS_SPELLBOOK_TIER_2_STRENGTH_MULT = 1.3f;
    public static final float ACCESSORY_RAVENS_SPELLBOOK_TIER_4_STRENGTH_MULT = 1.8f;
    public static final float ACCESSORY_WAFFLE_SHOOTER_TIER_2_DURATION_MULT = 1.5f;
    public static final float ACCESSORY_WAFFLE_SHOOTER_TIER_4_DURATION_MULT = 3.0f;
    public static final float ACCESSORY_LIL_PENGUIN_TIER_2_FREEZE_MULT = 1.25f;
    public static final float ACCESSORY_KRYPTONITE_TIER_2_STRENGTH_MULT = 1.25f;
    public static final float ACCESSORY_KRYPTONITE_TIER_4_STRENGTH_MULT = 1.5f;
    public static final float ACCESSORY_TITANS_COIN_TIER_5_HEAL_PCT = 0.30f;
    public static final int ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_HIT_INTERVAL = 5;
    public static final float ACCESSORY_MOTHER_BOX_TIER_5_CRITICAL_DAMAGE_MULT = 4.0f;
    public static final float ACCESSORY_BAT_SIGNAL_TIER_5_DAMAGE_MULT = 1.30f;
    public static final int ACCESSORY_GREEN_LANTERN_TIER_5_ACTIVATION_MANA = 100;
    public static final float ACCESSORY_RAVENS_SPELLBOOK_TIER_5_SPEED_MULT = 0.80f;
    public static final int ACCESSORY_KRYPTO_TIER_5_EFFECT_COUNT = 2;
    public static final float ACCESSORY_RED_LANTERN_TIER_5_POWER_UP_RETENTION = 0.50f;
    public static final float ACCESSORY_VIOLET_LANTERN_TIER_5_OVERHEAL_PCT = 0.30f;
    public static final int ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_INTERVAL = 5;
    public static final float ACCESSORY_WAFFLE_SHOOTER_TIER_5_SECONDARY_DURATION_MULT = 0.50f;
    public static final float ACCESSORY_KRYPTONITE_TIER_5_PENALTY_MULT = 0.90f;
    public static final int ACCESSORY_BIRDARANG_TIER_5_PASSIVE_DAMAGE = 2;

    public static final float ACCESSORY_TITANS_COIN_MAX_HP_BONUS_PCT = 0.25f;

    public static final int ACCESSORY_MOTHER_BOX_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_MOTHER_BOX_DAMAGE = 8;

    public static final int ACCESSORY_BAT_SIGNAL_WAKE_DELAY_TICKS = 120;
    public static final int ACCESSORY_BAT_SIGNAL_DAMAGE = 60;
    public static final int ACCESSORY_BAT_SIGNAL_HIT_COUNT = 5;

    public static final int ACCESSORY_RED_LANTERN_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_RED_LANTERN_POWER_UP = 6;

    public static final int ACCESSORY_GREEN_LANTERN_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_GREEN_LANTERN_MANA = 10;

    public static final int ACCESSORY_VIOLET_LANTERN_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_VIOLET_LANTERN_HEAL = 8;

    public static final int ACCESSORY_RAVENS_SPELLBOOK_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_RAVENS_SPELLBOOK_CURSE_DURATION_TICKS = 40;

    public static final int ACCESSORY_CYBORG_WAFFLE_SHOOTER_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_CYBORG_WAFFLE_SHOOTER_DURATION_TICKS = 40;

    public static final int ACCESSORY_LIL_PENGUIN_INTERVAL_TICKS = 140;
    public static final int ACCESSORY_LIL_PENGUIN_FREEZE_DURATION_TICKS = 20;
    public static final int ACCESSORY_LIL_PENGUIN_FREEZE_BURN_PCT = 50;

    public static final int ACCESSORY_KRYPTONITE_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_KRYPTONITE_DEFENSE_DOWN_DURATION_TICKS = 40;
    public static final int ACCESSORY_KRYPTONITE_DEFENSE_DOWN_MAGNITUDE = 15;

    public static final int ACCESSORY_JUSTICE_LEAGUE_COIN_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_JUSTICE_LEAGUE_COIN_LUCK_UP_DURATION_TICKS = 40;
    public static final int ACCESSORY_JUSTICE_LEAGUE_COIN_LUCK_UP_MAGNITUDE = 35;

    public static final int ACCESSORY_BIRDARANG_DAMAGE = 10;

    public static final float ACCESSORY_SUPERMANS_UNDERPANTS_BATTERY_DRAIN_MULT = 0.3f;

    public static final int ACCESSORY_KRYPTO_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_KRYPTO_POWER_UP_DURATION_TICKS = 40;
    public static final int ACCESSORY_KRYPTO_POWER_UP = 6;
    public static final int ACCESSORY_KRYPTO_HEAL = 8;
    public static final float ACCESSORY_KRYPTO_TOFU_POWER = 30.0f;

    // Chips

    public static final int[] CHIP_ONE_MAGNITUDE_BY_RANK = {1, 1, 1};
    public static final int[] CHIP_INFINITE_DURATION_BY_RANK = {-1, -1, -1};

    public static final int[] CHIP_TOUGH_GUY_POWER_BY_RANK = {5, 10, 15};
    public static final int[] CHIP_TOUGH_GUY_HP_BY_RANK = {-20, -35, -50};
    public static final int[] CHIP_TOUGH_GUY_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SMOKESCREEN_DODGE_BY_RANK = {8, 16, 24};
    public static final int[] CHIP_SMOKESCREEN_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_POWER_POWER_BY_RANK = {5, 10, 15};
    public static final int[] CHIP_POWER_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_HEALTH_HP_BY_RANK = {20, 35, 50};
    public static final int[] CHIP_HEALTH_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_LUCK_LUCK_BY_RANK = {8, 16, 24};
    public static final int[] CHIP_LUCK_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_NINJA_SKILLS_DODGE_BY_RANK = {8, 16, 24};
    public static final int[] CHIP_NINJA_SKILLS_HP_BY_RANK = {-20, -35, -50};
    public static final int[] CHIP_NINJA_SKILLS_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_LOADED_DICE_LUCK_BY_RANK = {8, 16, 24};
    public static final int[] CHIP_LOADED_DICE_POWER_BY_RANK = {-4, -8, -12};
    public static final int[] CHIP_LOADED_DICE_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_TOUGH_STUFF_HP_BY_RANK = {20, 35, 50};
    public static final int[] CHIP_TOUGH_STUFF_DODGE_BY_RANK = {0, 0, 0};
    public static final int[] CHIP_TOUGH_STUFF_FUSION_COST_BY_RANK = {100, 300};
    public static final float CHIP_STANDARD_HYBRID_PRIMARY_SCALE = 0.6f;
    public static final float CHIP_STANDARD_HYBRID_SECONDARY_SCALE = 0.6f;
    public static final int[] CHIP_STANDARD_HYBRID_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_STANDARD_HYBRID_SPECIAL_COST_BY_RANK = {50, 100, 300};

    public static final float CHIP_SPECIAL_TOUGH_SMOKESCREEN_TOUGH_GUY_SCALE = 0.6f;
    public static final float CHIP_SPECIAL_TOUGH_SMOKESCREEN_SMOKESCREEN_SCALE = 0.6f;
    public static final int[] CHIP_TOUGH_SMOKESCREEN_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SPECIAL_TOUGH_SMOKESCREEN_COST_BY_RANK = {50, 100, 300};
    public static final float CHIP_SPECIAL_LUCKY_HEALTHY_DUCK_LUCK_SCALE = 0.6f;
    public static final float CHIP_SPECIAL_LUCKY_HEALTHY_DUCK_HEALTHY_DUCK_SCALE = 0.6f;
    public static final int[] CHIP_LUCKY_HEALTHY_DUCK_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SPECIAL_LUCKY_HEALTHY_DUCK_COST_BY_RANK = {50, 100, 300};
    public static final float CHIP_SPECIAL_POWERED_BEASTLY_ENTRY_POWER_SCALE = 0.6f;
    public static final float CHIP_SPECIAL_POWERED_BEASTLY_ENTRY_BEASTLY_ENTRY_SCALE = 0.6f;
    public static final int[] CHIP_POWERED_BEASTLY_ENTRY_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SPECIAL_POWERED_BEASTLY_ENTRY_COST_BY_RANK = {50, 100, 300};

    public static final float[] CHIP_LUCKY_HEARTS_HEAL_PCT_BY_RANK = {0.06f, 0.09f, 0.12f};
    public static final int[] CHIP_LUCKY_HEARTS_FUSION_COST_BY_RANK = {200, 400};
    public static final float[] CHIP_INSTA_CAST_CHANCE_BY_RANK = {0.12f, 0.18f, 0.24f};
    public static final int[] CHIP_INSTA_CAST_CHANCE_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_CLEAN_ENTRY_DURATION_BY_RANK = {60, 90, 120};
    public static final int[] CHIP_CLEAN_ENTRY_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_BEASTLY_ENTRY_POWER_UP_BY_RANK = {5, 8, 12};
    public static final int[] CHIP_BEASTLY_ENTRY_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_CURSE_ENTRY_DURATION_BY_RANK = {80, 110, 140};
    public static final int[] CHIP_CURSE_ENTRY_FUSION_COST_BY_RANK = {300, 600};

    public static final int[] CHIP_DANCE_DURATION_BY_RANK = {100, 130, 160};
    public static final int[] CHIP_DANCE_ENTRY_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_MANA_BOOST_MANA_BY_RANK = {30, 40, 50};
    public static final int[] CHIP_MANA_BOOST_FUSION_COST_BY_RANK = {400, 800};
    public static final int[] CHIP_MOMENTUM_MANA_BY_RANK = {20, 30, 40};
    public static final int[] CHIP_MOMENTUM_FUSION_COST_BY_RANK = {400, 800};
    public static final int[] CHIP_VICTORY_DANCE_DURATION_BY_RANK = {100, 130, 160};
    public static final int[] CHIP_VICTORY_DANCE_FUSION_COST_BY_RANK = {400, 800};
    public static final int[] CHIP_MANA_STEAL_DUCK_MANA_BY_RANK = {8, 12, 16};
    public static final int[] CHIP_MANA_STEAL_DUCK_FUSION_COST_BY_RANK = {400, 800};

    public static final int[] CHIP_DEATH_ENERGY_BATTERY_BY_RANK = {30, 50, 70};
    public static final int[] CHIP_DEATH_ENERGY_FUSION_COST_BY_RANK = {1000, 2000};
    public static final int[] CHIP_SELF_EXPLOSION_DAMAGE_BY_RANK = {20, 30, 40};
    public static final int[] CHIP_SELF_EXPLOSION_FUSION_COST_BY_RANK = {2000, 4000};
    public static final int[] CHIP_HEALTHY_DODGE_HEAL_BY_RANK = {6, 9, 12};
    public static final int[] CHIP_HEALTHY_DODGE_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_SECOND_CHANCE_SURVIVE_HP_BY_RANK = {1, 1, 1};
    public static final int[] CHIP_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK = {60, 80, 100};
    public static final int[] CHIP_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK = {15, 25, 35};
    public static final int[] CHIP_SECOND_CHANCE_FUSION_COST_BY_RANK = {700, 1100};
    public static final int[] CHIP_HEALTHY_SECOND_CHANCE_SURVIVE_HP_BY_RANK = {8, 14, 20};
    public static final int[] CHIP_HEALTHY_SECOND_CHANCE_DEBUFF_DURATION_BY_RANK = {80, 100, 120};
    public static final int[] CHIP_HEALTHY_SECOND_CHANCE_SLOW_MAGNITUDE_BY_RANK = {25, 35, 45};
    public static final int[] CHIP_HEALTHY_SECOND_CHANCE_FUSION_COST_BY_RANK = {900, 1400};
    public static final int[] CHIP_SPECIAL_HEALTHY_SECOND_CHANCE_COST_BY_RANK = {800, 1200, 1700};
    public static final int[] CHIP_SPEEDY_DODGE_DURATION_BY_RANK = {40, 60, 80};
    public static final int[] CHIP_SPEEDY_DODGE_SPEED_PCT_BY_RANK = {20, 30, 40};
    public static final int[] CHIP_SPEEDY_DODGE_FUSION_COST_BY_RANK = {300, 600};
    public static final float[] CHIP_FAST_CAST_SPEED_BONUS_BY_RANK = {0.25f, 0.50f, 0.75f};
    public static final int[] CHIP_FAST_CAST_FUSION_COST_BY_RANK = {500, 900};
    public static final float[] CHIP_FAST_DRAW_SPEED_BONUS_BY_RANK = {0.25f, 0.50f, 0.75f};
    public static final int[] CHIP_FAST_DRAW_FUSION_COST_BY_RANK = {500, 900};
    public static final float[] CHIP_ENERGETIC_BATTERY_PASSIVE_CHARGE_BONUS_BY_RANK = {1.0f, 1.50f, 2.00f};
    public static final int[] CHIP_ENERGETIC_BATTERY_FUSION_COST_BY_RANK = {600, 1000};
    public static final int[] CHIP_LAST_LAUGH_FUSION_COST_BY_RANK = {900, 1500};
    public static final int[] CHIP_LAST_LAUGH_CURSE_DURATION_BY_RANK = {80, 110, 140};
    public static final int[] CHIP_LAST_LAUGH_SHOCK_DURATION_BY_RANK = {80, 100, 120};
    public static final int[] CHIP_LAST_LAUGH_SHOCK_INTERVAL_BY_RANK = {20, 20, 20};
    public static final float[] CHIP_LAST_LAUGH_SHOCK_POWER_BY_RANK = {20.0f, 30.0f, 40.0f};
    public static final int[] CHIP_LAST_LAUGH_POISON_DURATION_BY_RANK = {80, 100, 120};
    public static final int[] CHIP_LAST_LAUGH_POISON_INTERVAL_BY_RANK = {20, 20, 20};
    public static final float[] CHIP_LAST_LAUGH_POISON_POWER_BY_RANK = {4.0f, 6.0f, 8.0f};
    public static final int[] CHIP_LAST_LAUGH_FREEZE_DURATION_BY_RANK = {40, 60, 80};
    public static final int[] CHIP_LAST_LAUGH_FREEZE_BURN_PCT_BY_RANK = {20, 30, 40};
    public static final int[] CHIP_LAST_LAUGH_WAFFLE_DURATION_BY_RANK = {60, 80, 100};

    public static final int[] CHIP_NECROMANCER_DURATION_BY_RANK = {120, 160, 200};
    public static final int[] CHIP_NECROMANCER_DAMAGE_BY_RANK = {5, 7, 9};
    public static final int[] CHIP_NECROMANCER_FUSION_COST_BY_RANK = {800, 1600};
    public static final float[] CHIP_VAMPIRE_HEAL_PCT_BY_RANK = {0.20f, 0.30f, 0.40f};
    public static final int[] CHIP_VAMPIRE_FUSION_COST_BY_RANK = {1500, 3000};
    public static final int[] CHIP_REGENERATIVE_CUTENESS_INTERVAL_BY_RANK = {40, 30, 20};
    public static final int[] CHIP_REGENERATIVE_CUTENESS_HEAL_BY_RANK = {3, 5, 7};
    public static final int[] CHIP_REGENERATIVE_CUTENESS_FUSION_COST_BY_RANK = {500, 900};
    public static final float[] CHIP_TOFU_LOVER_TOFU_CHANCE_BONUS_BY_RANK = {0.20f, 0.35f, 0.50f};
    public static final int[] CHIP_TOFU_LOVER_FUSION_COST_BY_RANK = {400, 700};
    public static final float[] CHIP_HELLO_NURSE_HEAL_BONUS_BY_RANK = {0.20f, 0.35f, 0.50f};
    public static final int[] CHIP_HELLO_NURSE_FUSION_COST_BY_RANK = {500, 900};
    public static final float[] CHIP_TEAM_MEDIC_BENCH_HEAL_PCT_BY_RANK = {0.25f, 0.35f, 0.45f};
    public static final int[] CHIP_TEAM_MEDIC_FUSION_COST_BY_RANK = {700, 1100};
    public static final int[] CHIP_POINTY_DAMAGE_BY_RANK = {6, 9, 12};
    public static final int[] CHIP_POINTY_FUSION_COST_BY_RANK = {500, 900};
    public static final float[] CHIP_STUN_RESISTER_DURATION_REDUCTION_BY_RANK = {0.25f, 0.40f, 0.55f};
    public static final int[] CHIP_STUN_RESISTER_FUSION_COST_BY_RANK = {400, 700};
    public static final float[] CHIP_FINISHER_HP_THRESHOLD_BY_RANK = {0.35f, 0.45f, 0.55f};
    public static final float[] CHIP_FINISHER_DAMAGE_BONUS_BY_RANK = {0.20f, 0.30f, 0.40f};
    public static final int[] CHIP_FINISHER_FUSION_COST_BY_RANK = {700, 1200};

    // ==========================================
    // SECTION 3: DAMAGE AND CORE COMBAT RULES
    // ==========================================
    public static final float BASE_DAMAGE_PERMANA = 0.015f;
    public static final float MELEE_ATTACK_SPEEED = -2.0F;
    public static final float SPEED_PER_DODGE = 0.007f;
    public static final float SURPRISE_DAMAGE_VARIANCE = 0.1f;
    public static final float LUCK_BALANCE_MULTIPLIER = 1.0f;
    public static final float BASE_LUCK_MULTIPLIER = 1.1f;
    public static final float LUCKIER_FLAT_CRIT_CHANCE = 0.30f;
    public static final float CLASS_ADVANTAGE_BONUS_MULT = 0.20f;

    // ==========================================
    // SECTION 4: EFFECT FORMULAS
    // ==========================================
    // Generic scaling
    public static final float BASE_MANA_FILL_PERMANA = 2.0f;
    public static final float BASE_SELF_SHOCK_PERMANA = 0.01f;
    public static final float BASE_HEAL_PERMANA = 1.0f;
    public static final float BASE_POWER_UP_PERMANA = 1.0f;
    public static final float BASE_POWER_DOWN_PERMANA = 0.8f;

    // Defense / control
    public static final float BASE_DEFENSE_MAG = 10.0f;
    public static final float DEFENSE_MAG_PERMANA = 0.2f;

    public static final float DEFENSE_UP_DURATION_PERMANA = 0.4f;
    public static final float DEFENSE_UP_DURATION_PERLUCK = 0.005f;

    public static final float DEFENSE_DOWN_DURATION_PERMANA = 0.4f;
    public static final float DEFENSE_DOWN_DURATION_PERLUCK = 0.005f;

    public static final float ROOT_DURATION_PERMANA = 0.3f;
    public static final float ROOT_DURATION_PERLUCK = 0.004f;

    public static final float DISABLE_DURATION_PERMANA = 0.4f;
    public static final float DISABLE_DURATION_PERLUCK = 0.005f;

    // Damage-over-time / periodic effects
    public static final float SHOCK_BASE_AMOUNT = 1.5f;
    public static final float SHOCK_AMOUNT_PERMANA = 0.13f;
    public static final float SHOCK_BASE_INTERVAL = 100.0f;
    public static final float SHOCK_INTERVAL_PERMANA = 200.0f;
    public static final float SHOCK_INTERVAL_PERLUCK = 0.005f;
    public static final float SHOCK_DURATION_PERMANA = 0.4f;
    public static final float SHOCK_DURATION_PERLUCK = 0.006f;

    public static final float POISON_BASE_AMOUNT = 3.0f;
    public static final float POISON_AMOUNT_PERMANA = 0.2f;
    public static final float POISON_BASE_INTERVAL = 40.0f;
    public static final float POISON_INTERVAL_PERMANA = 150.0f;
    public static final float POISON_INTERVAL_PERLUCK = 0.005f;

    public static final float RADIO_BASE_AMOUNT = 3.0f;
    public static final float RADIO_AMOUNT_PERMANA = 0.2f;
    public static final float RADIO_BASE_INTERVAL = 40.0f;
    public static final float RADIO_INTERVAL_PERMANA = 150.0f;
    public static final float RADIO_INTERVAL_PERLUCK = 0.005f;

    // Charge / trait interaction knobs
    public static final int BASE_CHARGE_DELAY = 20;
    public static final float INSTANT_CAST_CHANCE_PERCENTAGE = 0.5f;
    public static final float BLUE_TICKS_PER_MANA = 0.15f;
    public static final int BLUE_TICKS_FLAT = 4;
    public static final float BLUE_DAMAGE_MULT = 1.5f;
    public static final int BLUE_BASE_INTERVAL = 15;

    // Charge runtime flags
    public static final boolean CHARGE_CANCEL_ON_STUN = true;
    public static final boolean CHARGE_LOCK_TARGET_ON_START = false;

    // Resource / defensive utility
    public static final float BAR_DEPLETE_PERMANA = 0.18f;
    public static final float BAR_DEPLETE_PERLUCK = 0.005f;

    public static final float DODGE_SMOKE_DURATION_PERMANA = 1.3f;
    public static final float DODGE_SMOKE_DURATION_PERLUCK = 0.008f;
    public static final float DODGE_SMOKE_BAGSIZE_PERMANA = 0.05f;
    public static final float DODGE_SMOKE_MULT_PERMANA = 0.01f;
    public static final int DODGE_SMOKE_USES = 5;

    public static final float SHIELD_DURATION_PERMANA = 0.25f;
    public static final float SHIELD_DURATION_PERLUCK = 0.004f;

    // Status effect durations
    public static final float STUN_DURATION_PERMANA = 0.04f;
    public static final float STUN_DURATION_PERLUCK = 0.002f;

    public static final float DANCE_DURATION_PERMANA = 0.3f;
    public static final float DANCE_DURATION_PERLUCK = 0.005f;
    public static final float DANCE_MANA_REGEN_MULTIPLIER = 2.0f;

    public static final float FREEZE_PERCENTAGE_PERMANA = 1.8f;
    public static final float FREEZE_PERCENTAGE_PERLUCK = 0.005f;
    public static final float FREEZE_PERCENTAGE_MAX = 100.0f;
    public static final float FREEZE_DURATION_PERMANA = 0.03f;
    public static final float FREEZE_DURATION_PERLUCK = 0.0015f;

    public static final float CURSE_DURATION_PERMANA = 0.7f;
    public static final float CURSE_DURATION_PERLUCK = 0.005f;
    public static final float CURSE_EFFICIENCY = 0.6f;

    public static final float WAFFLE_DURATION_PERMANA = 0.5f;
    public static final float WAFFLE_DURATION_PERLUCK = 0.01f;

    public static final float CLEANSE_DURATION_PERMANA = 0.4f;
    public static final float CLEANSE_DURATION_PERLUCK = 0.008f;

    public static final float KISS_DURATION_PERMANA = 0.35f;
    public static final float KISS_DURATION_PERLUCK = 0.006f;

    public static final float FLIGHT_DURATION_PERMANA = 0.5f;
    public static final float FLIGHT_DURATION_PERLUCK = 0.005f;
    public static final double FLIGHT_VERTICAL_BOOST = 1.2;
    public static final int FLIGHT_APEX_TICK_DELAY = 14;
    public static final double FLIGHT_HORIZONTAL_DRAG = 0.1;

    // Tofu
    public static final float TOFU_CHANCE_HIT_PERMANA = 1.0F;
    public static final float TOFU_BASE_MANA = 30.0f;
    public static final float TOFU_ABILITY_PERMANA = 0.05f;

    public static final float TOFU_HEAL_MULT = 0.5f;
    public static final float TOFU_POWER_UP_MULT = 0.5f;
    public static final float TOFU_BAR_FILL_MULT = 0.7f;
    public static final float TOFU_DANCE_MULT = 1.0f;
    public static final float TOFU_CLEANSE_MULT = 1.0f;
    public static final float TOFU_STUN_MULT = 1.0f;
    public static final float TOFU_FREEZE_MULT = 1.0f;
    public static final float TOFU_WAFFLE_MULT = 1.0f;

    // Luck up / cuteness / reflect
    public static final float LUCK_UP_PERCENT_PERMANA = 1.0f;
    public static final float LUCK_UP_BASE_PERCENT = 10.0f;
    public static final float LUCK_UP_DURATION_PERMANA = 0.7f;
    public static final int LUCK_UP_BASE_DURATION = 20;

    public static final float CUTENESS_DURATION_PERMANA = 0.7f;
    public static final float CUTENESS_DURATION_PERLUCK = 0.005f;
    public static final float CUTENESS_PERCENT_PERMANA = 2.0f;
    public static final float CUTENESS_PERCENT_PERLUCK = 0.005f;

    public static final float REFLECT_DURATION_PERMANA = 0.1f;
    public static final float REFLECT_DURATION_PERLUCK = 0.005f;
    public static final float REFLECT_DEFENSE_PERMANA = 0.1f;
    public static final float REFLECT_DAMAGE_PERMANA = 0.05f;

    // Pets / remote mine
    public static final int PET_FIRE_COOLDOWN = 60;
    public static final float PET_DAMAGE_PERMANA = 0.5f;
    public static final float PET_DURATION_PERMANA = 10.0f;
    public static final float PET_DURATION_PERLUCK = 0.005f;

    public static final float REMOTE_MINE_START_PCT = 0.3f;
    public static final int REMOTE_MINE_STAGES = 10;
    public static final int REMOTE_MINE_STAGE_INTERVAL = 30;
    public static final float REMOTE_MINE_DAMAGE_MULT = 2.5f;

    // ==========================================
    // SECTION 5: TRAITS AND BATTLE SPECIALS
    // ==========================================
    public static final float ACTIVATE_DAMAGE_MULT = 1.1f;
    public static final float CHARGE_UP_MULT_PER_SEC = 1.3f;
    public static final int DEATH_SWAP_RESET_TICKS = 60;

    // ==========================================
    // SECTION 6: PRESETS
    // ==========================================
    public static final int BOSS_ROBIN_BASE_HP = 1000;
    public static final int BOSS_ROBIN_BASE_POWER = 10;
    public static final int BOSS_ROBIN_BASE_DODGE = 10;
    public static final int BOSS_ROBIN_BASE_LUCK = 10;
    public static final int BOSS_ROBIN_LEVEL = 10;
    public static final String BOSS_ROBIN_UPGRADES = "HHHHHHHHHDDDDDDDDDDDDDDDDDDHHHHHHHHHHHDDDDDDDDDDHHHHHHHHDDDDDHHHHHHHHHDDDDDD";

    // ==========================================
    // SECTION 7: HELPERS
    // ==========================================
    public static int getManaCost(int slot, String tier) {
        if (slot < 1 || slot >= MANA_COSTS_BY_SLOT.length) {
            return 0;
        }
        return MANA_COSTS_BY_SLOT[slot][getTierLetterIndex(tier)];
    }

    public static float getEffectiveManaMultiplier(int slot) {
        if (slot < 1 || slot >= EFFECTIVE_MANA_MULTIPLIERS_BY_SLOT.length) {
            return 1.0f;
        }
        return EFFECTIVE_MANA_MULTIPLIERS_BY_SLOT[slot];
    }

    public static int getEffectiveManaCost(int slot, String tier) {
        return getEffectiveManaCost(getManaCost(slot, tier), slot);
    }

    public static int getEffectiveManaCost(int actualManaCost, int slot) {
        if (actualManaCost <= 0) {
            return 0;
        }
        return Math.max(0, Math.round(actualManaCost * getEffectiveManaMultiplier(slot)));
    }

    public static float getDamageMultiplier(int tier) {
        if (tier < 0 || tier >= DAMAGE_MULTIPLIERS_BY_TIER.length) {
            return 1.0f;
        }
        return DAMAGE_MULTIPLIERS_BY_TIER[tier];
    }

    public static float getRaycastDelay(int tier) {
        if (tier < 0 || tier >= RAYCAST_DELAYS_BY_TIER.length) {
            return 0.5f;
        }
        return RAYCAST_DELAYS_BY_TIER[tier];
    }

    public static int getRangeValue(int tier) {
        if (tier <= 0 || tier >= RANGE_VALUES_BY_TIER.length) {
            return 11;
        }
        return RANGE_VALUES_BY_TIER[tier];
    }

    public static int getBagSize(int statValue) {
        for (int i = SHUFFLE_BAG_CHECKPOINTS.length - 1; i >= 0; i--) {
            if (statValue >= SHUFFLE_BAG_CHECKPOINTS[i]) {
                return SHUFFLE_BAG_SIZES[i];
            }
        }
        return SHUFFLE_BAG_SIZES[0];
    }

    public static int getFigureXpRequired(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0;
        }

        int index = Math.max(0, currentLevel - 1);
        if (index >= FIGURE_XP_REQUIRED_BY_LEVEL.length) {
            return FIGURE_XP_REQUIRED_BY_LEVEL[FIGURE_XP_REQUIRED_BY_LEVEL.length - 1];
        }

        return FIGURE_XP_REQUIRED_BY_LEVEL[index];
    }

    public static int getAccessoryTierUpgradeCost(int targetTier) {
        int index = targetTier - 2;
        if (index < 0 || index >= ACCESSORY_TIER_UPGRADE_COSTS.length) {
            return 0;
        }
        return ACCESSORY_TIER_UPGRADE_COSTS[index];
    }

    public static double getArenaSpeedMultiplier(int level) {
        if (level <= 0) {
            return 0.0d;
        }

        int index = Math.min(level, ARENA_SPEED_LEVEL_MULTIPLIERS.length) - 1;
        return ARENA_SPEED_LEVEL_MULTIPLIERS[index];
    }

    private static int getTierLetterIndex(String tier) {
        if (tier == null || tier.isEmpty()) {
            return 0;
        }
        return switch (Character.toLowerCase(tier.charAt(0))) {
            case 'b' -> 1;
            case 'c' -> 2;
            case 'd' -> 3;
            case 'e' -> 4;
            default -> 0;
        };
    }
}
