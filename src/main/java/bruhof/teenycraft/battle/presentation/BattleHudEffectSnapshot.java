package bruhof.teenycraft.battle.presentation;

public record BattleHudEffectSnapshot(
        String id,
        int durationTicks,
        int magnitude,
        boolean infinite
) {
    public BattleHudEffectSnapshot {
        id = id != null ? id : "";
    }
}
