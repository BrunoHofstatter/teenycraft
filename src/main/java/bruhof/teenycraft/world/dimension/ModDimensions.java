package bruhof.teenycraft.world.dimension;

import bruhof.teenycraft.TeenyCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> TEENYVERSE_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(TeenyCraft.MOD_ID, "teenyverse"));

    public static final ResourceKey<DimensionType> TEENYVERSE_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            new ResourceLocation(TeenyCraft.MOD_ID, "teenyverse"));

    public static void register() {
        System.out.println("Registering ModDimensions for " + TeenyCraft.MOD_ID);
    }
}
