package bruhof.teenycraft;

public class TeenyBalance {

    private static final int[][] MANA_COSTS_BY_SLOT = {
            {},
            {6, 8, 10, 12, 14},
            {25, 28, 31, 34, 37},
            {80, 84, 88, 92, 96}
    };

    private static final float[] DAMAGE_MULTIPLIERS_BY_TIER = {
            0.0f, 0.82f, 0.85f, 0.88f, 0.91f, 0.94f, 0.97f,
            1.00f, 1.03f, 1.06f, 1.09f, 1.12f, 1.15f, 1.18f
    };

    private static final float[] RAYCAST_DELAYS_BY_TIER = {
            0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f,
            2.0f, 2.5f, 3.0f, 3.5f, 4.0f
    };

    private static final int[] RANGE_VALUES_BY_TIER = {
            0, 5, 7, 9, 11, 13, 15, 17, 19
    };

    private static final int[] SHUFFLE_BAG_CHECKPOINTS = {
            0, 20, 35, 55, 80
    };

    private static final int[] SHUFFLE_BAG_SIZES = {
            10, 9, 8, 7, 6
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

    // ==========================================
    // SECTION 2: BATTLE CORE
    // ==========================================
    public static final int BATTLE_MANA_MAX = 100;
    public static final int BATTLE_MANA_REGEN_PER_SEC = 10;
    public static final int SWAP_COOLDOWN = 5;

    public static final float RANGED_START_WIDTH = 1.0f;
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

    public static final float ACCESSORY_TITANS_COIN_MAX_HP_BONUS_PCT = 0.25f;

    public static final int ACCESSORY_MOTHER_BOX_INTERVAL_TICKS = 40;
    public static final int ACCESSORY_MOTHER_BOX_DAMAGE = 8;

    public static final int ACCESSORY_BAT_SIGNAL_WAKE_DELAY_TICKS = 60;
    public static final int ACCESSORY_BAT_SIGNAL_DAMAGE = 100;
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

    public static final int[] CHIP_TOUGH_GUY_POWER_BY_RANK = {5, 10, 15};
    public static final int[] CHIP_TOUGH_GUY_HP_BY_RANK = {-20, -35, -50};
    public static final int[] CHIP_TOUGH_GUY_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SMOKESCREEN_DODGE_BY_RANK = {8, 16, 24};
    public static final int[] CHIP_SMOKESCREEN_FUSION_COST_BY_RANK = {100, 300};

    public static final float CHIP_SPECIAL_TOUGH_SMOKESCREEN_TOUGH_GUY_SCALE = 0.6f;
    public static final float CHIP_SPECIAL_TOUGH_SMOKESCREEN_SMOKESCREEN_SCALE = 0.6f;
    public static final int[] CHIP_TOUGH_SMOKESCREEN_FUSION_COST_BY_RANK = {100, 300};
    public static final int[] CHIP_SPECIAL_TOUGH_SMOKESCREEN_COST_BY_RANK = {50, 100, 300};

    public static final float[] CHIP_LUCKY_HEARTS_HEAL_PCT_BY_RANK = {0.06f, 0.09f, 0.12f};
    public static final int[] CHIP_LUCKY_HEARTS_FUSION_COST_BY_RANK = {200, 400};
    public static final float[] CHIP_INSTA_CAST_CHANCE_BY_RANK = {0.12f, 0.18f, 0.24f};
    public static final int[] CHIP_INSTA_CAST_CHANCE_FUSION_COST_BY_RANK = {300, 600};

    public static final int[] CHIP_DANCE_DURATION_BY_RANK = {100, 130, 160};
    public static final int[] CHIP_DANCE_ENTRY_FUSION_COST_BY_RANK = {300, 600};
    public static final int[] CHIP_MANA_BOOST_MANA_BY_RANK = {30, 40, 50};
    public static final int[] CHIP_MANA_BOOST_FUSION_COST_BY_RANK = {400, 800};

    public static final int[] CHIP_DEATH_ENERGY_BATTERY_BY_RANK = {30, 50, 70};
    public static final int[] CHIP_DEATH_ENERGY_FUSION_COST_BY_RANK = {1000, 2000};
    public static final int[] CHIP_SELF_EXPLOSION_DAMAGE_BY_RANK = {20, 30, 40};
    public static final int[] CHIP_SELF_EXPLOSION_FUSION_COST_BY_RANK = {2000, 4000};

    public static final int[] CHIP_NECROMANCER_DURATION_BY_RANK = {120, 160, 200};
    public static final int[] CHIP_NECROMANCER_DAMAGE_BY_RANK = {5, 7, 9};
    public static final int[] CHIP_NECROMANCER_FUSION_COST_BY_RANK = {800, 1600};
    public static final float[] CHIP_VAMPIRE_HEAL_PCT_BY_RANK = {0.20f, 0.30f, 0.40f};
    public static final int[] CHIP_VAMPIRE_FUSION_COST_BY_RANK = {1500, 3000};

    // ==========================================
    // SECTION 3: DAMAGE AND CORE COMBAT RULES
    // ==========================================
    public static final float BASE_DAMAGE_PERMANA = 0.015f;
    public static final float MELEE_ATTACK_SPEEED = -2.0F;
    public static final float SPEED_PER_DODGE = 0.005f;
    public static final float SURPRISE_DAMAGE_VARIANCE = 0.1f;
    public static final float LUCK_BALANCE_MULTIPLIER = 1.0f;
    public static final float BASE_LUCK_MULTIPLIER = 1.1f;

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
    public static final float SHOCK_BASE_AMOUNT = 2.0f;
    public static final float SHOCK_AMOUNT_PERMANA = 0.15f;
    public static final float SHOCK_BASE_INTERVAL = 60.0f;
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
    public static final float BAR_DEPLETE_PERMANA = 0.25f;
    public static final float BAR_DEPLETE_PERLUCK = 0.005f;

    public static final float DODGE_SMOKE_DURATION_PERMANA = 0.4f;
    public static final float DODGE_SMOKE_DURATION_PERLUCK = 0.005f;
    public static final float DODGE_SMOKE_BAGSIZE_PERMANA = 0.05f;
    public static final float DODGE_SMOKE_MULT_PERMANA = 0.01f;
    public static final int DODGE_SMOKE_USES = 5;

    public static final float SHIELD_DURATION_PERMANA = 0.25f;
    public static final float SHIELD_DURATION_PERLUCK = 0.004f;

    // Status effect durations
    public static final float STUN_DURATION_PERMANA = 0.05f;
    public static final float STUN_DURATION_PERLUCK = 0.003f;

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

    public static final float REFLECT_DURATION_PERMANA = 0.05f;
    public static final float REFLECT_DURATION_PERLUCK = 0.0005f;
    public static final float REFLECT_DEFENSE_PERMANA = 0.1f;
    public static final float REFLECT_DAMAGE_PERMANA = 0.05f;

    // Pets / remote mine
    public static final int PET_FIRE_COOLDOWN = 60;
    public static final float PET_DAMAGE_PERMANA = 0.5f;
    public static final float PET_DURATION_PERMANA = 10.0f;
    public static final float PET_DURATION_PERLUCK = 0.005f;

    public static final float REMOTE_MINE_START_PCT = 0.3f;
    public static final int REMOTE_MINE_STAGES = 10;
    public static final int REMOTE_MINE_STAGE_INTERVAL = 60;
    public static final float REMOTE_MINE_DAMAGE_MULT = 2.5f;

    // ==========================================
    // SECTION 5: TRAITS AND BATTLE SPECIALS
    // ==========================================
    public static final float ACTIVATE_DAMAGE_MULT = 1.1f;
    public static final float CHARGE_UP_MULT_PER_SEC = 1.5f;
    public static final int DEATH_SWAP_RESET_TICKS = 60;

    // ==========================================
    // SECTION 6: PRESETS
    // ==========================================
    public static final int BOSS_ROBIN_BASE_HP = 1000;
    public static final int BOSS_ROBIN_BASE_POWER = 10;
    public static final int BOSS_ROBIN_BASE_DODGE = 10;
    public static final int BOSS_ROBIN_BASE_LUCK = 10;
    public static final int BOSS_ROBIN_LEVEL = 10;
    public static final String BOSS_ROBIN_UPGRADES = "HHHHHHHHH";

    // ==========================================
    // SECTION 7: HELPERS
    // ==========================================
    public static int getManaCost(int slot, String tier) {
        if (slot < 1 || slot >= MANA_COSTS_BY_SLOT.length) {
            return 0;
        }
        return MANA_COSTS_BY_SLOT[slot][getTierLetterIndex(tier)];
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
