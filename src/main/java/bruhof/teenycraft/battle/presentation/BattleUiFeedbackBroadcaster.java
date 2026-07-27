package bruhof.teenycraft.battle.presentation;

import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketBattleUiEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class BattleUiFeedbackBroadcaster {
    private BattleUiFeedbackBroadcaster() {
    }

    public static void emitToViewers(@Nullable IBattleState state, BattleUiEventPayload payload) {
        if (state == null || payload == null) {
            return;
        }

        sendIfPlayer(state.getBattleEntity(), payload);

        LivingEntity opponent = state.getOpponentEntity();
        if (opponent != state.getBattleEntity()) {
            sendIfPlayer(opponent, payload);
        }
    }

    private static void sendIfPlayer(@Nullable LivingEntity entity, BattleUiEventPayload payload) {
        if (entity instanceof ServerPlayer player) {
            ModMessages.sendToPlayer(new PacketBattleUiEvent(payload), player);
        }
    }
}
