package bruhof.teenycraft.world.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ArenaBattleSession(
        ResourceLocation arenaId,
        int slotIndex,
        BlockPos slotOrigin,
        UUID playerId,
        UUID opponentId,
        ResourceKey<Level> returnDimension,
        Vec3 returnPosition,
        float returnYaw,
        float returnPitch
) {
}
