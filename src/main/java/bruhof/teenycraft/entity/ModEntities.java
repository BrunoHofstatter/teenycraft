package bruhof.teenycraft.entity;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TeenyCraft.MOD_ID);

    public static final RegistryObject<EntityType<EntityTeenyDummy>> TEENY_DUMMY =
            ENTITY_TYPES.register("teeny_dummy",
                    () -> EntityType.Builder.of(EntityTeenyDummy::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f) // Standard player size
                            .build("teeny_dummy"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
