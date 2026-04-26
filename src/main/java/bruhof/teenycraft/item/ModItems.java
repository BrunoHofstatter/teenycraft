package bruhof.teenycraft.item;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.block.ModBlocks;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemChip;
import net.minecraft.world.item.BlockItem;
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

    public static final RegistryObject<Item> BATMAN = ITEMS.register("figure_batman",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> SUPERMAN = ITEMS.register("figure_superman",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> ARGYLE_TRIGON = ITEMS.register("figure_argyle_trigon",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> SEE_MORE = ITEMS.register("figure_see_more",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> HARLEY_QUINN = ITEMS.register("figure_harley_quinn",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> JOKER = ITEMS.register("figure_joker",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> BILLY_NUMEROUS = ITEMS.register("figure_billy_numerous",
            () -> new ItemFigure(new Item.Properties()));

    public static final RegistryObject<Item> TITAN_PAD = ITEMS.register("titan_pad",
            () -> new bruhof.teenycraft.item.custom.ItemTitanPad(new Item.Properties()));

    public static final RegistryObject<Item> CHIP_FUSER = ITEMS.register("chip_fuser",
            () -> new BlockItem(ModBlocks.CHIP_FUSER.get(), new Item.Properties()));

    public static final RegistryObject<Item> TOFU = ITEMS.register("tofu",
            () -> new bruhof.teenycraft.item.custom.battle.ItemTofu(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ACCESSORY_TITANS_COIN = ITEMS.register("accessory_titans_coin",
            () -> new ItemAccessory(new Item.Properties(), "titans_coin"));
    public static final RegistryObject<Item> ACCESSORY_MOTHER_BOX = ITEMS.register("accessory_mother_box",
            () -> new ItemAccessory(new Item.Properties(), "mother_box"));
    public static final RegistryObject<Item> ACCESSORY_BAT_SIGNAL = ITEMS.register("accessory_bat_signal",
            () -> new ItemAccessory(new Item.Properties(), "bat_signal"));
    public static final RegistryObject<Item> ACCESSORY_RED_LANTERN_BATTERY = ITEMS.register("accessory_red_lantern_battery",
            () -> new ItemAccessory(new Item.Properties(), "red_lantern_battery"));
    public static final RegistryObject<Item> ACCESSORY_GREEN_LANTERN_BATTERY = ITEMS.register("accessory_green_lantern_battery",
            () -> new ItemAccessory(new Item.Properties(), "green_lantern_battery"));
    public static final RegistryObject<Item> ACCESSORY_VIOLET_LANTERN_BATTERY = ITEMS.register("accessory_violet_lantern_battery",
            () -> new ItemAccessory(new Item.Properties(), "violet_lantern_battery"));
    public static final RegistryObject<Item> ACCESSORY_RAVENS_SPELLBOOK = ITEMS.register("accessory_ravens_spellbook",
            () -> new ItemAccessory(new Item.Properties(), "ravens_spellbook"));
    public static final RegistryObject<Item> ACCESSORY_CYBORGS_WAFFLE_SHOOTER = ITEMS.register("accessory_cyborgs_waffle_shooter",
            () -> new ItemAccessory(new Item.Properties(), "cyborgs_waffle_shooter"));
    public static final RegistryObject<Item> ACCESSORY_LIL_PENGUIN = ITEMS.register("accessory_lil_penguin",
            () -> new ItemAccessory(new Item.Properties(), "lil_penguin"));
    public static final RegistryObject<Item> ACCESSORY_KRYPTONITE = ITEMS.register("accessory_kryptonite",
            () -> new ItemAccessory(new Item.Properties(), "kryptonite"));
    public static final RegistryObject<Item> ACCESSORY_JUSTICE_LEAGUE_COIN = ITEMS.register("accessory_justice_league_coin",
            () -> new ItemAccessory(new Item.Properties(), "justice_league_coin"));
    public static final RegistryObject<Item> ACCESSORY_BIRDARANG = ITEMS.register("accessory_birdarang",
            () -> new ItemAccessory(new Item.Properties(), "birdarang"));
    public static final RegistryObject<Item> ACCESSORY_SUPERMANS_UNDERPANTS = ITEMS.register("accessory_supermans_underpants",
            () -> new ItemAccessory(new Item.Properties(), "supermans_underpants"));
    public static final RegistryObject<Item> ACCESSORY_KRYPTO_THE_SUPERDOG = ITEMS.register("accessory_krypto_the_superdog",
            () -> new ItemAccessory(new Item.Properties(), "krypto_the_superdog"));

    public static final RegistryObject<Item> CHIP_TOUGH_GUY = ITEMS.register("chip_tough_guy",
            () -> new ItemChip(new Item.Properties(), "tough_guy"));
    public static final RegistryObject<Item> CHIP_SMOKESCREEN = ITEMS.register("chip_smokescreen",
            () -> new ItemChip(new Item.Properties(), "smokescreen"));
    public static final RegistryObject<Item> CHIP_TOUGH_SMOKESCREEN = ITEMS.register("chip_tough_smokescreen",
            () -> new ItemChip(new Item.Properties(), "tough_smokescreen"));
    public static final RegistryObject<Item> CHIP_LUCKY_HEARTS = ITEMS.register("chip_lucky_hearts",
            () -> new ItemChip(new Item.Properties(), "lucky_hearts"));
    public static final RegistryObject<Item> CHIP_INSTA_CAST_CHANCE = ITEMS.register("chip_insta_cast_chance",
            () -> new ItemChip(new Item.Properties(), "insta_cast_chance"));
    public static final RegistryObject<Item> CHIP_DANCE_ENTRY = ITEMS.register("chip_dance_entry",
            () -> new ItemChip(new Item.Properties(), "dance_entry"));
    public static final RegistryObject<Item> CHIP_MANA_BOOST = ITEMS.register("chip_mana_boost",
            () -> new ItemChip(new Item.Properties(), "mana_boost"));
    public static final RegistryObject<Item> CHIP_DEATH_ENERGY = ITEMS.register("chip_death_energy",
            () -> new ItemChip(new Item.Properties(), "death_energy"));
    public static final RegistryObject<Item> CHIP_SELF_EXPLOSION = ITEMS.register("chip_self_explosion",
            () -> new ItemChip(new Item.Properties(), "self_explosion"));
    public static final RegistryObject<Item> CHIP_NECROMANCER = ITEMS.register("chip_necromancer",
            () -> new ItemChip(new Item.Properties(), "necromancer"));
    public static final RegistryObject<Item> CHIP_VAMPIRE = ITEMS.register("chip_vampire",
            () -> new ItemChip(new Item.Properties(), "vampire"));

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
