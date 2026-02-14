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
    public static final int BATTLE_MANA_REGEN_PER_SEC = 4; // Example

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

    public static final float BASE_DAMAGE_PERMANA = 0.02f;
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

    public static final float MELEE_ATTACK_SPEEED = -2.0F;






    // ==========================================
    // SECTION 6: EFFECT SCALING (PER 1 MANA)
    // ==========================================

    public static final float STUN_DURATION_PERMANA = 0.05f;
    public static final float FREEZE_PERCENTAGE_PERMANA = 1.4f;
    public static final float TOFU_CHANCE_HIT_PERMANA = 1.2F;


}