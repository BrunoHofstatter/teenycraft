package bruhof.teenycraft.world.arena;

import net.minecraft.resources.ResourceLocation;

public record ArenaTemplateDefinition(
        ResourceLocation templateId,
        int gridX,
        int gridZ
) {
}
