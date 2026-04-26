package bruhof.teenycraft.command;

import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

final class BattleDebugContextResolver {
    private BattleDebugContextResolver() {
    }

    static Resolution resolve(ServerPlayer player, String casterType) {
        LivingEntity pairedOpponent = resolvePairedOpponent(player);
        if (pairedOpponent != null) {
            if ("opponent".equalsIgnoreCase(casterType)) {
                return new Resolution(pairedOpponent, player);
            }
            return new Resolution(player, pairedOpponent);
        }

        LivingEntity nearbyBattleParticipant = findNearbyBattleParticipant(player);
        if ("opponent".equalsIgnoreCase(casterType)) {
            return new Resolution(nearbyBattleParticipant, player);
        }
        return new Resolution(player, nearbyBattleParticipant);
    }

    @Nullable
    private static LivingEntity resolvePairedOpponent(ServerPlayer player) {
        return player.getCapability(BattleStateProvider.BATTLE_STATE)
                .resolve()
                .filter(IBattleState::isBattling)
                .map(IBattleState::getOpponentEntity)
                .orElse(null);
    }

    @Nullable
    private static LivingEntity findNearbyBattleParticipant(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(15),
                        entity -> entity != player && entity.getCapability(BattleStateProvider.BATTLE_STATE).isPresent()
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    record Resolution(@Nullable LivingEntity caster, @Nullable LivingEntity enemy) {
    }
}
