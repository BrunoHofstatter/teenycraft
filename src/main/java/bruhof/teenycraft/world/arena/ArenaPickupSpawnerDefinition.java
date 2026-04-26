package bruhof.teenycraft.world.arena;

import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ArenaPickupSpawnerDefinition(
        String id,
        List<Vec3> spots,
        ArenaSelectionMode spotMode,
        List<ArenaPickupVariantDefinition> variants,
        ArenaSelectionMode variantMode,
        int firstSpawnDelayTicks,
        int cooldownTicks
) {
    public ArenaPickupSpawnerDefinition {
        spots = spots == null ? List.of() : List.copyOf(spots);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
