package bruhof.teenycraft.block;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.block.custom.ChipFuserBlock;
import bruhof.teenycraft.block.custom.SilkieStationBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TeenyCraft.MOD_ID);

    public static final RegistryObject<Block> CHIP_FUSER = BLOCKS.register("chip_fuser",
            () -> new ChipFuserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SILKIE_STATION = BLOCKS.register("silkie_station",
            () -> new SilkieStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
