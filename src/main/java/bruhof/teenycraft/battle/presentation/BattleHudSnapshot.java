package bruhof.teenycraft.battle.presentation;

public record BattleHudSnapshot(
        boolean isBattling,
        BattleHudParticipantSnapshot player,
        BattleHudParticipantSnapshot enemy
) {
    public BattleHudSnapshot {
        player = player != null ? player : BattleHudParticipantSnapshot.empty();
        enemy = enemy != null ? enemy : BattleHudParticipantSnapshot.empty();
    }

    public static BattleHudSnapshot off() {
        return new BattleHudSnapshot(false, BattleHudParticipantSnapshot.empty(), BattleHudParticipantSnapshot.empty());
    }

    public BattleHudParticipantSnapshot participant(boolean isEnemy) {
        return isEnemy ? enemy : player;
    }
}
