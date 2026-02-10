package bruhof.teenycraft.item;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // The DeferredRegister is the standard way to register items in Forge
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TeenyCraft.MOD_ID);

    // 1. The Generic Base Item (Hidden or used for logic)
    public static final RegistryObject<Item> FIGURE_BASE = ITEMS.register("figure_base",
            () -> new ItemFigure(new Item.Properties()));

    // 2. Specific Figures (These are what you hold)
    public static final RegistryObject<Item> ROBIN = ITEMS.register("figure_robin",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> CYBORG = ITEMS.register("figure_cyborg",
            () -> new ItemFigure(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}