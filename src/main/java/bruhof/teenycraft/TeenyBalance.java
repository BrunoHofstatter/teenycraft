package bruhof.teenycraft;

public class TeenyBalance {

    // ==========================================
    // SECTION 1: FIGURE ATTRIBUTES
    // ==========================================
    public static final int MAX_LEVEL = 20;
    public static final int UPGRADE_GAIN_HP = 20;
    public static final int UPGRADE_GAIN_POWER = 5;
    public static final int UPGRADE_GAIN_DODGE = 8;
    public static final int UPGRADE_GAIN_LUCK = 8;
    



    // ==========================================
    // SECTION 2: BATTLE MECHANICS
    // ==========================================
    public static final int BATTLE_MANA_MAX = 100;
    public static final int BATTLE_MANA_REGEN_PER_SEC = 10; // Example
    public static final int SWAP_COOLDOWN = 5;
    
    public static final float RANGED_START_WIDTH = 1.0f;
    public static final float RANGED_CONE_ANGLE = 15.0f; // Total angle in degrees

    // ==========================================
    // SECTION 3: ABILITY COST TIERS
    // ==========================================
    public static final int COST_1A = 6;
    public static final int COST_1B = 8;
    public static final int COST_1C = 10;
    public static final int COST_1D = 12;
    public static final int COST_1E = 14;

    public static final int COST_2A = 25;
    public static final int COST_2B = 28;
    public static final int COST_2C = 31;
    public static final int COST_2D = 34;
    public static final int COST_2E = 37;

    public static final int COST_3A = 80;
    public static final int COST_3B = 84;
    public static final int COST_3C = 88;
    public static final int COST_3D = 92;
    public static final int COST_3E = 96;

    public static int getManaCost(int slot, String tier) {
        String t = tier.toLowerCase();
        return switch (slot) {
            case 1 -> switch (t) {
                case "a" -> COST_1A;
                case "b" -> COST_1B;
                case "c" -> COST_1C;
                case "d" -> COST_1D;
                case "e" -> COST_1E;
                default -> COST_1A;
            };
            case 2 -> switch (t) {
                case "a" -> COST_2A;
                case "b" -> COST_2B;
                case "c" -> COST_2C;
                case "d" -> COST_2D;
                case "e" -> COST_2E;
                default -> COST_2A;
            };
            case 3 -> switch (t) {
                case "a" -> COST_3A;
                case "b" -> COST_3B;
                case "c" -> COST_3C;
                case "d" -> COST_3D;
                case "e" -> COST_3E;
                default -> COST_3A;
            };
            default -> 0;
        };
    }

    // ==========================================
    // SECTION 4: DAMAGE
    // ==========================================

    public static final float BASE_DAMAGE_PERMANA = 0.015f;
    public static final float DAMAGE_MULTIPLIER_0 = 0.0f;
    public static final float DAMAGE_MULTIPLIER_1 = 0.82f;
    public static final float DAMAGE_MULTIPLIER_2 = 0.85f;
    public static final float DAMAGE_MULTIPLIER_3 = 0.88f;
    public static final float DAMAGE_MULTIPLIER_4 = 0.91f;
    public static final float DAMAGE_MULTIPLIER_5 = 0.94f;
    public static final float DAMAGE_MULTIPLIER_6 = 0.97f;
    public static final float DAMAGE_MULTIPLIER_7 = 1.00f;
    public static final float DAMAGE_MULTIPLIER_8 = 1.03f;
    public static final float DAMAGE_MULTIPLIER_9 = 1.06f;
    public static final float DAMAGE_MULTIPLIER_10 = 1.09f;
    public static final float DAMAGE_MULTIPLIER_11 = 1.12f;
    public static final float DAMAGE_MULTIPLIER_12 = 1.15f;
    public static final float DAMAGE_MULTIPLIER_13 = 1.18f;

    public static float getDamageMultiplier(int tier) {
        return switch (tier) {
            case 0 -> DAMAGE_MULTIPLIER_0;
            case 1 -> DAMAGE_MULTIPLIER_1;
            case 2 -> DAMAGE_MULTIPLIER_2;
            case 3 -> DAMAGE_MULTIPLIER_3;
            case 4 -> DAMAGE_MULTIPLIER_4;
            case 5 -> DAMAGE_MULTIPLIER_5;
            case 6 -> DAMAGE_MULTIPLIER_6;
            case 7 -> DAMAGE_MULTIPLIER_7;
            case 8 -> DAMAGE_MULTIPLIER_8;
            case 9 -> DAMAGE_MULTIPLIER_9;
            case 10 -> DAMAGE_MULTIPLIER_10;
            case 11 -> DAMAGE_MULTIPLIER_11;
            case 12 -> DAMAGE_MULTIPLIER_12;
            case 13 -> DAMAGE_MULTIPLIER_13;
            default -> 1.0f;
        };
    }

    public static final float RAYCAST_DELAY_0 = 0.1f;
    public static final float RAYCAST_DELAY_1 = 0.2f;
    public static final float RAYCAST_DELAY_2 = 0.3f;
    public static final float RAYCAST_DELAY_3 = 0.4f;
    public static final float RAYCAST_DELAY_4 = 0.5f;
    public static final float RAYCAST_DELAY_5 = 0.6f;
    public static final float RAYCAST_DELAY_6 = 0.7f;
    public static final float RAYCAST_DELAY_7 = 0.8f;
    public static final float RAYCAST_DELAY_8 = 0.9f;
    public static final float RAYCAST_DELAY_9 = 1.0f;
    public static final float RAYCAST_DELAY_10 = 1.1f;
    public static final float RAYCAST_DELAY_11 = 1.2f;

    public static float getRaycastDelay(int tier) {
        return switch (tier) {
            case 0 -> RAYCAST_DELAY_0;
            case 1 -> RAYCAST_DELAY_1;
            case 2 -> RAYCAST_DELAY_2;
            case 3 -> RAYCAST_DELAY_3;
            case 4 -> RAYCAST_DELAY_4;
            case 5 -> RAYCAST_DELAY_5;
            case 6 -> RAYCAST_DELAY_6;
            case 7 -> RAYCAST_DELAY_7;
            case 8 -> RAYCAST_DELAY_8;
            case 9 -> RAYCAST_DELAY_9;
            case 10 -> RAYCAST_DELAY_10;
            case 11 -> RAYCAST_DELAY_11;
            default -> 0.5f;
        };
    }

    public static final int RANGE_VALUE_1 = 5;
    public static final int RANGE_VALUE_2 = 7;
    public static final int RANGE_VALUE_3 = 9;
    public static final int RANGE_VALUE_4 = 11;
    public static final int RANGE_VALUE_5 = 13;
    public static final int RANGE_VALUE_6 = 15;
    public static final int RANGE_VALUE_7 = 17;
    public static final int RANGE_VALUE_8 = 19;

    public static int getRangeValue(int tier) {
        return switch (tier) {
            case 1 -> RANGE_VALUE_1;
            case 2 -> RANGE_VALUE_2;
            case 3 -> RANGE_VALUE_3;
            case 4 -> RANGE_VALUE_4;
            case 5 -> RANGE_VALUE_5;
            case 6 -> RANGE_VALUE_6;
            case 7 -> RANGE_VALUE_7;
            case 8 -> RANGE_VALUE_8;
            default -> 11;
        };
    }

    public static final float MELEE_ATTACK_SPEEED = -2.0F;

    public static final float SPEED_PER_DODGE = 0.005f;

    // ==========================================
    // SECTION 5: SHUFFLE BAG (DODGE & LUCK)
    // ==========================================
    
    // Checkpoints (Stat Values)
    public static final int CHECKPOINT_0 = 0;
    public static final int CHECKPOINT_1 = 20;
    public static final int CHECKPOINT_2 = 35;
    public static final int CHECKPOINT_3 = 55;
    public static final int CHECKPOINT_4 = 80;

    // Bag Sizes for each checkpoint
    public static final int BAG_SIZE_0 = 10;
    public static final int BAG_SIZE_1 = 9;
    public static final int BAG_SIZE_2 = 8;
    public static final int BAG_SIZE_3 = 7;
    public static final int BAG_SIZE_4 = 6;
    
    public static final float LUCK_BALANCE_MULTIPLIER = 1.0f;
    public static final float BASE_LUCK_MULTIPLIER = 1.1f;
    
    public static int getBagSize(int statValue) {
        if (statValue >= CHECKPOINT_4) return BAG_SIZE_4;
        if (statValue >= CHECKPOINT_3) return BAG_SIZE_3;
        if (statValue >= CHECKPOINT_2) return BAG_SIZE_2;
        if (statValue >= CHECKPOINT_1) return BAG_SIZE_1;
        return BAG_SIZE_0;
    }
    

    
    // ==========================================
    // SECTION 6: EFFECT SCALING
    // ==========================================

    public static final float BASE_MANA_FILL_PERMANA = 2.0f;
    public static final float BASE_SELF_SHOCK_PERMANA = 0.01f;
    public static final float BASE_HEAL_PERMANA = 1.0f;
    public static final float BASE_POWER_UP_PERMANA = 1.0f;
    public static final float BASE_POWER_DOWN_PERMANA = 0.8f;

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

    public static final float SHOCK_BASE_AMOUNT = 2.0f;
    public static final float SHOCK_AMOUNT_PERMANA = 0.15f;
    public static final float SHOCK_BASE_INTERVAL = 60.0f; // 3s
    public static final float SHOCK_INTERVAL_PERMANA = 200.0f; // Multiplied by (1/Mana)
    public static final float SHOCK_INTERVAL_PERLUCK = 0.005f;
    public static final float SHOCK_DURATION_PERMANA = 0.4f; // Duration of the stun itself
    public static final float SHOCK_DURATION_PERLUCK = 0.006f;

    public static final float POISON_BASE_AMOUNT = 3.0f;
    public static final float POISON_AMOUNT_PERMANA = 0.2f;
    public static final float POISON_BASE_INTERVAL = 40.0f; // 2s
    public static final float POISON_INTERVAL_PERMANA = 150.0f;
    public static final float POISON_INTERVAL_PERLUCK = 0.005f;

    public static final float RADIO_BASE_AMOUNT = 3.0f;
    public static final float RADIO_AMOUNT_PERMANA = 0.2f;
    public static final float RADIO_BASE_INTERVAL = 40.0f; // 2s
    public static final float RADIO_INTERVAL_PERMANA = 150.0f;
    public static final float RADIO_INTERVAL_PERLUCK = 0.005f;

    public static final int BASE_CHARGE_DELAY = 20; // 1 second base
    public static final boolean CHARGE_CANCEL_ON_STUN = true;
    public static final boolean CHARGE_LOCK_TARGET_ON_START = false;

    public static final float BAR_DEPLETE_PERMANA = 0.25f;
    public static final float BAR_DEPLETE_PERLUCK = 0.005f;

    public static final float DODGE_SMOKE_DURATION_PERMANA = 0.4f;
    public static final float DODGE_SMOKE_DURATION_PERLUCK = 0.005f;
    public static final float DODGE_SMOKE_BAGSIZE_PERMANA = 0.05f; // 20 mana = 1 reduction
    public static final float DODGE_SMOKE_MULT_PERMANA = 0.01f; // Enhanced mitigation mult
    public static final int DODGE_SMOKE_USES = 5;

    public static final float SHIELD_DURATION_PERMANA = 0.25f;
    public static final float SHIELD_DURATION_PERLUCK = 0.004f;

    public static final float STUN_DURATION_PERMANA = 0.05f;
    public static final float STUN_DURATION_PERLUCK = 0.003f;
    
    public static final float DANCE_DURATION_PERMANA = 0.3f;
    public static final float DANCE_DURATION_PERLUCK = 0.005f;
    public static final float DANCE_MANA_REGEN_MULTIPLIER = 2.0f;

    public static final float FREEZE_PERCENTAGE_PERMANA = 1.8f;
    public static final float FREEZE_PERCENTAGE_PERLUCK = 0.005f;

    public static final float FREEZE_PERCENTAGE_MAX = 100.0f; // Max % of mana that can be burned

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



    public static final float TOFU_CHANCE_HIT_PERMANA = 1.0F;
    public static final float TOFU_BASE_MANA = 30.0f;
    public static final float TOFU_ABILITY_PERMANA = 0.05f;

    // Tofu Effect Multipliers (Applied to Virtual Mana)
    public static final float TOFU_HEAL_MULT = 0.5f;
    public static final float TOFU_POWER_UP_MULT = 0.5f;
    public static final float TOFU_BAR_FILL_MULT = 0.7f;
    public static final float TOFU_DANCE_MULT = 1.0f;
    public static final float TOFU_CLEANSE_MULT = 1.0f;
    
    public static final float TOFU_STUN_MULT = 1.0f;
    public static final float TOFU_FREEZE_MULT = 1.0f;
    public static final float TOFU_WAFFLE_MULT = 1.0f;

    // Effects - Luck Up
    public static final float LUCK_UP_PERCENT_PERMANA = 1.0f;
    public static final float LUCK_UP_BASE_PERCENT = 10.0f;
    public static final float LUCK_UP_DURATION_PERMANA = 0.7f;
    public static final int LUCK_UP_BASE_DURATION = 20;

    // Effects - Cuteness
    public static final float CUTENESS_DURATION_PERMANA = 0.7f;
    public static final float CUTENESS_DURATION_PERLUCK = 0.005f;
    public static final float CUTENESS_PERCENT_PERMANA = 2.0f;
    public static final float CUTENESS_PERCENT_PERLUCK = 0.005f;

    // Effects - Reflect
    public static final float REFLECT_DURATION_PERMANA = 0.05f;
    public static final float REFLECT_DURATION_PERLUCK = 0.0005f;
    public static final float REFLECT_DEFENSE_PERMANA = 0.008f; // 10% reduction per mana?
    public static final float REFLECT_DAMAGE_PERMANA = 0.2f; // 5% power-scaled reflection per mana

    // Death Swap
    public static final int DEATH_SWAP_RESET_TICKS = 60; // 3 seconds

    // ==========================================
    // SECTION 7: BOSS CONFIGURATION
    // ==========================================
    public static final int BOSS_ROBIN_BASE_HP = 1000;
    public static final int BOSS_ROBIN_BASE_POWER = 10;
    public static final int BOSS_ROBIN_BASE_DODGE = 10;
    public static final int BOSS_ROBIN_BASE_LUCK = 10;
    public static final int BOSS_ROBIN_LEVEL = 10;
    public static final String BOSS_ROBIN_UPGRADES = "HHHHHHHHH"; // 9 HP Upgrades

}