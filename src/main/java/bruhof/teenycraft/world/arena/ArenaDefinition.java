package bruhof.teenycraft.world.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ArenaDefinition(
        ResourceLocation id,
        ResourceLocation templateId,
        Vec3 playerSpawn,
        float playerYaw,
        float playerPitch,
        Vec3 opponentSpawn,
        float opponentYaw,
        float opponentPitch,
        BlockPos clearPadding,
        List<String> tags
) {
    public ArenaDefinition {
        clearPadding = clearPadding == null ? BlockPos.ZERO : clearPadding;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public Vec3 playerSpawnAt(BlockPos origin) {
        return origin.getCenter().add(playerSpawn.x - 0.5, playerSpawn.y, playerSpawn.z - 0.5);
    }

    public Vec3 opponentSpawnAt(BlockPos origin) {
        return origin.getCenter().add(opponentSpawn.x - 0.5, opponentSpawn.y, opponentSpawn.z - 0.5);
    }
}
