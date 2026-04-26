package bruhof.teenycraft.world.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ArenaPickupVariantDefinition(
        ArenaPickupType type,
        int amount,
        int speedLevel,
        int durationTicks,
        Vec3 destination,
        double arcHeight,
        ResourceLocation wallBlock,
        List<BlockPos> wallBlocks
) {
    public ArenaPickupVariantDefinition {
        wallBlocks = wallBlocks == null ? List.of() : List.copyOf(wallBlocks);
    }
}
