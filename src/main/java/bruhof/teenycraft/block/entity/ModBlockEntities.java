package bruhof.teenycraft.block.entity;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TeenyCraft.MOD_ID);

    public static final RegistryObject<BlockEntityType<ChipFuserBlockEntity>> CHIP_FUSER =
            BLOCK_ENTITIES.register("chip_fuser",
                    () -> BlockEntityType.Builder.of(ChipFuserBlockEntity::new, ModBlocks.CHIP_FUSER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
