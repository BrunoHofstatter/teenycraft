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

    public static final RegistryObject<Item> RAVEN = ITEMS.register("figure_raven",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> BEAST_BOY = ITEMS.register("figure_beast_boy",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> STARFIRE = ITEMS.register("figure_starfire",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> SILKIE = ITEMS.register("figure_silkie",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> CYBORG = ITEMS.register("figure_cyborg",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> TITAN_PAD = ITEMS.register("titan_pad",
            () -> new bruhof.teenycraft.item.custom.ItemTitanPad(new Item.Properties()));

    public static final RegistryObject<Item> TOFU = ITEMS.register("tofu",
            () -> new bruhof.teenycraft.item.custom.battle.ItemTofu(new Item.Properties().stacksTo(1)));

    // BATTLE ITEMS (Not in creative tabs, given by code)
    public static final RegistryObject<Item> ABILITY_1 = ITEMS.register("ability_1",
            () -> new bruhof.teenycraft.item.custom.battle.ItemAbility(new Item.Properties(), 0));
    public static final RegistryObject<Item> ABILITY_2 = ITEMS.register("ability_2",
            () -> new bruhof.teenycraft.item.custom.battle.ItemAbility(new Item.Properties(), 1));
    public static final RegistryObject<Item> ABILITY_3 = ITEMS.register("ability_3",
            () -> new bruhof.teenycraft.item.custom.battle.ItemAbility(new Item.Properties(), 2));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}