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

import java.util.List;

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

    public static final List<RegistryObject<ItemFigure>> ADDITIONAL_FIGURES = List.of(
            "alfred",
            "aquaman",
            "artemis",
            "bane",
            "batgirl",
            "batman_beyond",
            "bizarro",
            "black_lightning",
            "black_manta",
            "blackfire",
            "blue_beetle",
            "booster_gold",
            "bumblebee",
            "cat_beast_boy",
            "catwoman",
            "clark_kent",
            "darkseid",
            "dr_light",
            "george_washington",
            "gizmo",
            "gorilla_grodd",
            "hawkgirl",
            "jessica_cruz",
            "jinx",
            "john_stewart",
            "kid_flash",
            "killer_croc",
            "killer_moth",
            "lex_luthor",
            "mammoth",
            "martian_manhunter",
            "mas_y_menos",
            "nightwing",
            "poison_ivy",
            "red_arrow",
            "red_x",
            "robotic_brother_blood",
            "rose_wilson",
            "santa_clause",
            "shazam",
            "sinestro",
            "slade",
            "speedy",
            "sticky_joe",
            "supergirl",
            "terra",
            "the_flash",
            "the_hooded_hood",
            "the_penguin",
            "the_riddler",
            "trigon",
            "wonder_woman"
    ).stream().map(id -> ITEMS.register("figure_" + id,
            () -> new ItemFigure(new Item.Properties()))).toList();

    public static final RegistryObject<Item> TITAN_PAD = ITEMS.register("titan_pad",
            () -> new bruhof.teenycraft.item.custom.ItemTitanPad(new Item.Properties()));

    public static final RegistryObject<Item> CHIP_FUSER = ITEMS.register("chip_fuser",
            () -> new BlockItem(ModBlocks.CHIP_FUSER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SILKIE_STATION = ITEMS.register("silkie_station",
            () -> new BlockItem(ModBlocks.SILKIE_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> TOFU = ITEMS.register("tofu",
            () -> new bruhof.teenycraft.item.custom.battle.ItemTofu(new Item.Properties().stacksTo(1)));

    // Temporary inert items for comparing vanilla item display transforms in-game.
    public static final RegistryObject<Item> DISPLAY_TEST_AXE_FIRSTPERSON = ITEMS.register("display_test_axe_firstperson",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_AROUND_HYBRID = ITEMS.register("display_test_around_hybrid",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_AMAZONIAN_BEATDOWN = ITEMS.register("display_test_amazonian_beatdown",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_BIRDARANG = ITEMS.register("display_test_birdarang",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_BAT_MINE = ITEMS.register("display_test_bat_mine",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_TRIDENT_THROW = ITEMS.register("display_test_trident_throw",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_CONSTRUCT_BEAM = ITEMS.register("display_test_construct_beam",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DISPLAY_TEST_GRAPPLING_HOOK = ITEMS.register("display_test_grappling_hook",
            () -> new Item(new Item.Properties().stacksTo(1)));

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
    public static final RegistryObject<Item> CHIP_POWER = ITEMS.register("chip_power",
            () -> new ItemChip(new Item.Properties(), "power"));
    public static final RegistryObject<Item> CHIP_HEALTH = ITEMS.register("chip_health",
            () -> new ItemChip(new Item.Properties(), "health"));
    public static final RegistryObject<Item> CHIP_LUCK = ITEMS.register("chip_luck",
            () -> new ItemChip(new Item.Properties(), "luck"));
    public static final RegistryObject<Item> CHIP_NINJA_SKILLS = ITEMS.register("chip_ninja_skills",
            () -> new ItemChip(new Item.Properties(), "ninja_skills"));
    public static final RegistryObject<Item> CHIP_LOADED_DICE = ITEMS.register("chip_loaded_dice",
            () -> new ItemChip(new Item.Properties(), "loaded_dice"));
    public static final RegistryObject<Item> CHIP_TOUGH_STUFF = ITEMS.register("chip_tough_stuff",
            () -> new ItemChip(new Item.Properties(), "tough_stuff"));
    public static final RegistryObject<Item> CHIP_LUCKY_HEARTS = ITEMS.register("chip_lucky_hearts",
            () -> new ItemChip(new Item.Properties(), "lucky_hearts"));
    public static final RegistryObject<Item> CHIP_MANA_STEAL_DUCK = ITEMS.register("chip_mana_steal_duck",
            () -> new ItemChip(new Item.Properties(), "mana_steal_duck"));
    public static final RegistryObject<Item> CHIP_LUCKY_STEAL_DUCK = ITEMS.register("chip_lucky_steal_duck",
            () -> new ItemChip(new Item.Properties(), "lucky_steal_duck"));
    public static final RegistryObject<Item> CHIP_HEALTHY_DODGE = ITEMS.register("chip_healthy_dodge",
            () -> new ItemChip(new Item.Properties(), "healthy_dodge"));
    public static final RegistryObject<Item> CHIP_DODGY_HEALTHY_DODGE = ITEMS.register("chip_dodgy_healthy_dodge",
            () -> new ItemChip(new Item.Properties(), "dodgy_healthy_dodge"));
    public static final RegistryObject<Item> CHIP_SECOND_CHANCE = ITEMS.register("chip_second_chance",
            () -> new ItemChip(new Item.Properties(), "second_chance"));
    public static final RegistryObject<Item> CHIP_SPEEDY_DODGE = ITEMS.register("chip_speedy_dodge",
            () -> new ItemChip(new Item.Properties(), "speedy_dodge"));
    public static final RegistryObject<Item> CHIP_DODGY_SPEEDY_DODGE = ITEMS.register("chip_dodgy_speedy_dodge",
            () -> new ItemChip(new Item.Properties(), "dodgy_speedy_dodge"));
    public static final RegistryObject<Item> CHIP_HEALTHY_SECOND_CHANCE = ITEMS.register("chip_healthy_second_chance",
            () -> new ItemChip(new Item.Properties(), "healthy_second_chance"));
    public static final RegistryObject<Item> CHIP_POINTY_HEALTH = ITEMS.register("chip_pointy_health",
            () -> new ItemChip(new Item.Properties(), "pointy_health"));
    public static final RegistryObject<Item> CHIP_LUCKY_HEALTHY_DUCK = ITEMS.register("chip_lucky_healthy_duck",
            () -> new ItemChip(new Item.Properties(), "lucky_healthy_duck"));
    public static final RegistryObject<Item> CHIP_INSTA_CAST_CHANCE = ITEMS.register("chip_insta_cast_chance",
            () -> new ItemChip(new Item.Properties(), "insta_cast_chance"));
    public static final RegistryObject<Item> CHIP_FAST_CAST = ITEMS.register("chip_fast_cast",
            () -> new ItemChip(new Item.Properties(), "fast_cast"));
    public static final RegistryObject<Item> CHIP_FAST_DRAW = ITEMS.register("chip_fast_draw",
            () -> new ItemChip(new Item.Properties(), "fast_draw"));
    public static final RegistryObject<Item> CHIP_FAST_DRAWCAST = ITEMS.register("chip_fast_drawcast",
            () -> new ItemChip(new Item.Properties(), "fast_drawcast"));
    public static final RegistryObject<Item> CHIP_CLEAN_ENTRY = ITEMS.register("chip_clean_entry",
            () -> new ItemChip(new Item.Properties(), "clean_entry"));
    public static final RegistryObject<Item> CHIP_BEASTLY_ENTRY = ITEMS.register("chip_beastly_entry",
            () -> new ItemChip(new Item.Properties(), "beastly_entry"));
    public static final RegistryObject<Item> CHIP_CURSE_ENTRY = ITEMS.register("chip_curse_entry",
            () -> new ItemChip(new Item.Properties(), "curse_entry"));
    public static final RegistryObject<Item> CHIP_DANCE_ENTRY = ITEMS.register("chip_dance_entry",
            () -> new ItemChip(new Item.Properties(), "dance_entry"));
    public static final RegistryObject<Item> CHIP_MANA_BOOST = ITEMS.register("chip_mana_boost",
            () -> new ItemChip(new Item.Properties(), "mana_boost"));
    public static final RegistryObject<Item> CHIP_MOMENTUM = ITEMS.register("chip_momentum",
            () -> new ItemChip(new Item.Properties(), "momentum"));
    public static final RegistryObject<Item> CHIP_FINISHING_MOMENTUM = ITEMS.register("chip_finishing_momentum",
            () -> new ItemChip(new Item.Properties(), "finishing_momentum"));
    public static final RegistryObject<Item> CHIP_VICTORY_DANCE = ITEMS.register("chip_victory_dance",
            () -> new ItemChip(new Item.Properties(), "victory_dance"));
    public static final RegistryObject<Item> CHIP_POWERED_BEASTLY_ENTRY = ITEMS.register("chip_powered_beastly_entry",
            () -> new ItemChip(new Item.Properties(), "powered_beastly_entry"));
    public static final RegistryObject<Item> CHIP_POWERED_FINISHER = ITEMS.register("chip_powered_finisher",
            () -> new ItemChip(new Item.Properties(), "powered_finisher"));
    public static final RegistryObject<Item> CHIP_ENERGETIC_BATTERY = ITEMS.register("chip_energetic_battery",
            () -> new ItemChip(new Item.Properties(), "energetic_battery"));
    public static final RegistryObject<Item> CHIP_DEATH_ENERGY = ITEMS.register("chip_death_energy",
            () -> new ItemChip(new Item.Properties(), "death_energy"));
    public static final RegistryObject<Item> CHIP_SELF_EXPLOSION = ITEMS.register("chip_self_explosion",
            () -> new ItemChip(new Item.Properties(), "self_explosion"));
    public static final RegistryObject<Item> CHIP_LAST_LAUGH = ITEMS.register("chip_last_laugh",
            () -> new ItemChip(new Item.Properties(), "last_laugh"));
    public static final RegistryObject<Item> CHIP_NECROMANCER = ITEMS.register("chip_necromancer",
            () -> new ItemChip(new Item.Properties(), "necromancer"));
    public static final RegistryObject<Item> CHIP_VAMPIRE = ITEMS.register("chip_vampire",
            () -> new ItemChip(new Item.Properties(), "vampire"));
    public static final RegistryObject<Item> CHIP_REGENERATIVE_CUTENESS = ITEMS.register("chip_regenerative_cuteness",
            () -> new ItemChip(new Item.Properties(), "regenerative_cuteness"));
    public static final RegistryObject<Item> CHIP_TOFU_LOVER = ITEMS.register("chip_tofu_lover",
            () -> new ItemChip(new Item.Properties(), "tofu_lover"));
    public static final RegistryObject<Item> CHIP_LUCKY_TOFU_LOVER = ITEMS.register("chip_lucky_tofu_lover",
            () -> new ItemChip(new Item.Properties(), "lucky_tofu_lover"));
    public static final RegistryObject<Item> CHIP_HELLO_NURSE = ITEMS.register("chip_hello_nurse",
            () -> new ItemChip(new Item.Properties(), "hello_nurse"));
    public static final RegistryObject<Item> CHIP_TEAM_MEDIC = ITEMS.register("chip_team_medic",
            () -> new ItemChip(new Item.Properties(), "team_medic"));
    public static final RegistryObject<Item> CHIP_POINTY = ITEMS.register("chip_pointy",
            () -> new ItemChip(new Item.Properties(), "pointy"));
    public static final RegistryObject<Item> CHIP_STUN_RESISTER = ITEMS.register("chip_stun_resister",
            () -> new ItemChip(new Item.Properties(), "stun_resister"));
    public static final RegistryObject<Item> CHIP_FINISHER = ITEMS.register("chip_finisher",
            () -> new ItemChip(new Item.Properties(), "finisher"));

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
