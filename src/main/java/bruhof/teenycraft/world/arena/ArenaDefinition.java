package bruhof.teenycraft.world.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ArenaDefinition(
        ResourceLocation id,
        List<ArenaTemplateDefinition> templates,
        Vec3 playerSpawn,
        float playerYaw,
        float playerPitch,
        Vec3 opponentSpawn,
        float opponentYaw,
        float opponentPitch,
        BlockPos clearPadding,
        List<String> tags,
        List<ArenaPickupSpawnerDefinition> pickupSpawners
) {
    public ArenaDefinition {
        templates = templates == null ? List.of() : List.copyOf(templates);
        clearPadding = clearPadding == null ? BlockPos.ZERO : clearPadding;
        tags = tags == null ? List.of() : List.copyOf(tags);
        pickupSpawners = pickupSpawners == null ? List.of() : List.copyOf(pickupSpawners);
    }

    public Vec3 positionAt(BlockPos origin, Vec3 localPosition) {
        return origin.getCenter().add(localPosition.x - 0.5, localPosition.y, localPosition.z - 0.5);
    }

    public BlockPos blockAt(BlockPos origin, BlockPos localPosition) {
        return origin.offset(localPosition);
    }

    public Vec3 playerSpawnAt(BlockPos origin) {
        return positionAt(origin, playerSpawn);
    }

    public Vec3 opponentSpawnAt(BlockPos origin) {
        return positionAt(origin, opponentSpawn);
    }
}
