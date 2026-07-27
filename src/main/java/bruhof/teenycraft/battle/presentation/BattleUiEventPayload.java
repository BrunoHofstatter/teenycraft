package bruhof.teenycraft.battle.presentation;

public record BattleUiEventPayload(
        BattleUiEventPayload.Type type,
        int entityId,
        int amount,
        int amount2,
        String value,
        boolean flag
) {
    public BattleUiEventPayload {
        type = type != null ? type : Type.ABILITY;
        value = value != null ? value : "";
    }

    public enum Type {
        DAMAGE,
        DAMAGE_GHOST,
        HEAL,
        CRIT,
        CLASS_BONUS,
        ABILITY,
        MANA,
        BATTERY,
        TOFU_GAINED,
        TOFU_RESULT,
        PICKUP
    }
}
