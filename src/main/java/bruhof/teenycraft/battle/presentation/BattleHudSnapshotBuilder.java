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
            return BattleHudParticipantSnapshot.empty();
        }

        return buildParticipant(state, activeFigure, hasActiveMine, activeFigure.getNickname());
    }

    private static BattleHudParticipantSnapshot buildParticipant(IBattleState state, BattleFigure activeFigure, boolean[] hasActiveMine, String name) {
        String activeFigureId = state.getActiveFigureId();
        return new BattleHudParticipantSnapshot(
                state.getBattleEntity() != null ? state.getBattleEntity().getId() : 0,
                name,
                activeFigureId,
                activeFigure.getEffectiveSkinId(),
                activeFigure.getEffectiveModelType(),
                state.getActiveFigureIndex(),
                activeFigure.getCurrentHp(),
                activeFigure.getMaxHp(),
                state.getCurrentMana(),
                state.getCurrentTofuMana(),
                state.getCurrentTofuPreviewEffectId(),
                state.isCurrentTofuPreviewSelfTarget(),
                state.getBatteryCharge(),
                state.getBatterySpawnPct(),
                state.isAccessoryActive(),
                state.getEquippedAccessoryId(),
                state.getBasePower(),
                state.getEffectMagnitude("power_up"),
                state.getEffectMagnitude("power_down"),
                state.getCooldowns(),
                new int[]{state.getSlotProgress(0), state.getSlotProgress(1), state.getSlotProgress(2)},
                hasActiveMine,
                state.hasEffect("waffle") ? state.getEffectMagnitude("waffle") : -1,
                effectDuration(state, "waffle"),
                state.isCharging() ? state.getPendingSlot() : -1,
                state.getChargeTicks(),
                state.getChargeTotalTicks(),
                state.isBlueChanneling() ? state.getBlueChannelSlot() : -1,
                state.getBlueChannelTicks(),
                state.getBlueChannelTotalTicks(),
                state.getAbilityIds(),
                state.getAbilityTiers(),
                state.getAbilityGoldenStatus(),
                state.getEffectSnapshots(),
                state.getBenchInfoList(),
                state.getBenchIndicesList(),
                state.getBenchFigureIds()
        );
    }

    private static int effectDuration(IBattleState state, String effectId) {
        var instance = state.getEffectInstance(effectId);
        return instance != null ? instance.duration : 0;
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
