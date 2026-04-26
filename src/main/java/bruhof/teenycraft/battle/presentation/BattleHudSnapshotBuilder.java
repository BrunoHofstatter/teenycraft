package bruhof.teenycraft.battle.presentation;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class BattleHudSnapshotBuilder {
    private BattleHudSnapshotBuilder() {
    }

    @Nullable
    public static BattleHudSnapshot buildForViewer(IBattleState state, ServerPlayer viewer) {
        BattleFigure activeFigure = state.getActiveFigure();
        if (activeFigure == null) {
            return null;
        }

        LivingEntity opponentEntity = state.getOpponentEntity();
        IBattleState opponentState = state.getOpponentBattleState();

        BattleHudParticipantSnapshot playerSnapshot = buildParticipant(
                state,
                activeFigure,
                buildMineFlags(state, opponentEntity != null ? opponentEntity.getUUID() : null),
                activeFigure.getNickname()
        );

        BattleHudParticipantSnapshot enemySnapshot = buildOpponentParticipant(
                opponentState,
                buildMineFlags(opponentState, viewer.getUUID())
        );

        return new BattleHudSnapshot(true, playerSnapshot, enemySnapshot);
    }

    private static BattleHudParticipantSnapshot buildOpponentParticipant(@Nullable IBattleState state, boolean[] hasActiveMine) {
        BattleFigure activeFigure = state != null ? state.getActiveFigure() : null;
        if (state == null || activeFigure == null) {
            return new BattleHudParticipantSnapshot(
                    "None",
                    "none",
                    "default",
                    0,
                    0,
                    100,
                    0.0f,
                    0.0f,
                    -1.0f,
                    0,
                    0,
                    0,
                    new int[3],
                    new int[3],
                    hasActiveMine,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        return buildParticipant(state, activeFigure, hasActiveMine, activeFigure.getNickname());
    }

    private static BattleHudParticipantSnapshot buildParticipant(IBattleState state, BattleFigure activeFigure, boolean[] hasActiveMine, String name) {
        String activeFigureId = state.getActiveFigureId();
        return new BattleHudParticipantSnapshot(
                name,
                activeFigureId,
                FigureLoader.getModelType(activeFigureId),
                state.getActiveFigureIndex(),
                activeFigure.getCurrentHp(),
                activeFigure.getMaxHp(),
                state.getCurrentMana(),
                state.getBatteryCharge(),
                state.getBatterySpawnPct(),
                state.getBasePower(),
                state.getEffectMagnitude("power_up"),
                state.getEffectMagnitude("power_down"),
                state.getCooldowns(),
                new int[]{state.getSlotProgress(0), state.getSlotProgress(1), state.getSlotProgress(2)},
                hasActiveMine,
                state.getAbilityIds(),
                state.getAbilityTiers(),
                state.getAbilityGoldenStatus(),
                state.getEffectList(),
                state.getBenchInfoList(),
                state.getBenchIndicesList(),
                state.getBenchFigureIds()
        );
    }

    private static boolean[] buildMineFlags(@Nullable IBattleState state, @Nullable UUID targetUuid) {
        boolean[] flags = new boolean[3];
        if (state == null || targetUuid == null) {
            return flags;
        }

        for (int i = 0; i < flags.length; i++) {
            flags[i] = state.hasActiveMine(i, targetUuid);
        }
        return flags;
    }
}
