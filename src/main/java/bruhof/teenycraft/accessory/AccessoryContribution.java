package bruhof.teenycraft.accessory;

public record AccessoryContribution(
        String accessoryId,
        Type type,
        long amount,
        int targetFigureIndex,
        String detail
) {
    public static final int NO_TARGET = -1;

    public AccessoryContribution {
        detail = detail != null ? detail : "";
    }

    public static AccessoryContribution count(String accessoryId, Type type) {
        return new AccessoryContribution(accessoryId, type, 1, NO_TARGET, "");
    }

    public static AccessoryContribution amount(String accessoryId, Type type, long amount) {
        return new AccessoryContribution(accessoryId, type, amount, NO_TARGET, "");
    }

    public enum Type {
        ACTIVATION,
        PULSE,
        DAMAGE_HIT,
        DAMAGE_DEALT,
        FIGURE_DEFEATED,
        MAX_HP_GRANTED,
        POWER_UP_GRANTED,
        MANA_GRANTED,
        HEALING_DONE,
        EFFECT_APPLIED,
        WAFFLE_APPLIED,
        MANA_BURNED,
        RETALIATION_HIT,
        DAMAGE_BLOCKED,
        FATAL_HIT_BLOCKED,
        KRYPTO_EFFECT_GRANTED,
        TITANS_COIN_ONE_HP_SURVIVAL,
        MOTHER_BOX_FULL_DEFEAT,
        POWER_UP_BONUS_DAMAGE,
        DEFENSE_DOWN_BONUS_DAMAGE,
        MANA_FILLED_FROM_LOW,
        HEALED_FROM_LOW_TO_TARGET
    }
}
