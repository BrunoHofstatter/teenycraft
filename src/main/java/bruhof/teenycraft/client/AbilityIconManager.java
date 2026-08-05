package bruhof.teenycraft.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class AbilityIconManager {
    private static final String ICON_TEXTURE_PREFIX = "textures/item/ability_";
    private static final String ICON_TEXTURE_SUFFIX = ".png";
    private static final String ICON_VARIANT_MARKER = "__variant_";
    private static volatile Set<String> availableIconModels = Set.of();
    private static volatile Map<String, Integer> iconVariantIndexes = Map.of();

    public static void refreshAvailableIconModels(ResourceManager resourceManager) {
        Set<String> discovered = new HashSet<>();
        Map<String, Set<String>> variantsByAbility = new TreeMap<>();
        resourceManager.listResources("textures/item", location ->
                        location.getNamespace().equals("teenycraft")
                                && location.getPath().startsWith(ICON_TEXTURE_PREFIX)
                                && location.getPath().endsWith(ICON_TEXTURE_SUFFIX)
                                && !location.getPath().endsWith("_golden.png"))
                .keySet()
                .forEach(location -> {
                    String path = location.getPath();
                    String modelId = path.substring(
                            ICON_TEXTURE_PREFIX.length(),
                            path.length() - ICON_TEXTURE_SUFFIX.length()
                    );
                    discovered.add(modelId);

                    int markerIndex = modelId.indexOf(ICON_VARIANT_MARKER);
                    if (markerIndex > 0 && markerIndex + ICON_VARIANT_MARKER.length() < modelId.length()) {
                        String abilityId = modelId.substring(0, markerIndex);
                        String variant = modelId.substring(markerIndex + ICON_VARIANT_MARKER.length());
                        variantsByAbility.computeIfAbsent(abilityId, ignored -> new TreeSet<>()).add(variant);
                    }
                });

        Map<String, Integer> variantIndexes = new HashMap<>();
        variantsByAbility.forEach((abilityId, variants) -> {
            int index = 1;
            for (String variant : variants) {
                variantIndexes.put(getVariantModelId(abilityId, variant), index++);
            }
        });
        availableIconModels = Set.copyOf(discovered);
        iconVariantIndexes = Map.copyOf(variantIndexes);
    }

    public static boolean hasIconModel(String abilityModelId) {
        return availableIconModels.contains(abilityModelId);
    }

    public static int getIconVariantIndex(String abilityId, String variant) {
        if (abilityId == null || abilityId.isBlank() || variant == null || variant.isBlank()) {
            return 0;
        }
        return iconVariantIndexes.getOrDefault(getVariantModelId(abilityId, variant), 0);
    }

    private static String getVariantModelId(String abilityId, String variant) {
        return abilityId + ICON_VARIANT_MARKER + variant;
    }

    // Vanilla-item placeholders from the Teeny Craft - Abilities sheet's icon_name column.
    // AbilityModelWrapper gives generated custom models priority over this map, so an
    // entry can remain here as a safe fallback after a custom texture is introduced.
    public static final Map<String, Item> FALLBACKS = Map.ofEntries(
            Map.entry("amazonian_beatdown", Items.NETHERITE_BOOTS),
            Map.entry("around_the_world", Items.COMPASS),
            Map.entry("arrow_storm", Items.BOW),
            Map.entry("axe_to_grind", Items.STONE_AXE),
            Map.entry("bang", Items.TNT),
            Map.entry("bat_mine", Items.TRIPWIRE_HOOK),
            Map.entry("batarang_storm", Items.FLINT),
            Map.entry("beatbox_heal", Items.JUKEBOX),
            Map.entry("black_hole", Items.ENDER_PEARL),
            Map.entry("booster_beatdown", Items.GOLDEN_SWORD),
            Map.entry("boulder_toss", Items.STONE),
            Map.entry("break_the_bat", Items.BAMBOO),
            Map.entry("break_you", Items.ANVIL),
            Map.entry("brother_shake", Items.HONEYCOMB),
            Map.entry("bubble_shield", Items.BLUE_STAINED_GLASS),
            Map.entry("bulletproof", Items.IRON_CHESTPLATE),
            Map.entry("burp_shield", Items.HEART_OF_THE_SEA),
            Map.entry("burp_surprise", Items.EXPLORER_POTTERY_SHERD),
            Map.entry("butterfly", Items.BLUE_ORCHID),
            Map.entry("camera_flash", Items.SPYGLASS),
            Map.entry("cash_blaster", Items.GOLD_NUGGET),
            Map.entry("cat_scratch", Items.LEATHER),
            Map.entry("charged_arrow", Items.ARROW),
            Map.entry("chattering_teeth", Items.BONE),
            Map.entry("construct_beam", Items.LIME_DYE),
            Map.entry("construct_blaster", Items.DISPENSER),
            Map.entry("construct_chains", Items.CHAIN),
            Map.entry("counterattack", Items.TURTLE_HELMET),
            Map.entry("curse", Items.POPPED_CHORUS_FRUIT),
            Map.entry("curse_jinx", Items.POPPED_CHORUS_FRUIT),
            Map.entry("cuteness", Items.PINK_DYE),
            Map.entry("dance", Items.MUSIC_DISC_MALL),
            Map.entry("darwin", Items.BOOK),
            Map.entry("deadly_kiss", Items.FERMENTED_SPIDER_EYE),
            Map.entry("deathstrike", Items.IRON_SWORD),
            Map.entry("dem_legs", Items.LEATHER_BOOTS),
            Map.entry("double_slam", Items.IRON_SHOVEL),
            Map.entry("energy_cannon", Items.FIRE_CHARGE),
            Map.entry("evil_laugh", Items.JACK_O_LANTERN),
            Map.entry("fear", Items.WITHER_SKELETON_SKULL),
            Map.entry("flight", Items.ELYTRA),
            Map.entry("freeze_breath", Items.SNOWBALL),
            Map.entry("good_luck", Items.RABBIT_FOOT),
            Map.entry("grappling_hook", Items.FISHING_ROD),
            Map.entry("harleys_mallet", Items.GOLDEN_AXE),
            Map.entry("heat_vision", Items.BLAZE_POWDER),
            Map.entry("hooded_barrage", Items.BLACKSTONE),
            Map.entry("hooded_void", Items.CRYING_OBSIDIAN),
            Map.entry("jiu_jitsu", Items.WARPED_BUTTON),
            Map.entry("jokers_mallet", Items.NETHERITE_AXE),
            Map.entry("journalism", Items.WRITABLE_BOOK),
            Map.entry("katana_slam", Items.DIAMOND_SWORD),
            Map.entry("killer_claws", Items.SHEARS),
            Map.entry("kryptonite_beam", Items.EMERALD),
            Map.entry("laser_eyes", Items.ENDER_EYE),
            Map.entry("laser_eyes_drlight", Items.PRISMARINE_CRYSTALS),
            Map.entry("laser_eyes_manta", Items.ENDER_EYE),
            Map.entry("laser_sneeze", Items.ENDER_EYE),
            Map.entry("light_shield", Items.YELLOW_STAINED_GLASS),
            Map.entry("lightning_bubble", Items.SEA_LANTERN),
            Map.entry("lightning_fists", Items.LIGHTNING_ROD),
            Map.entry("lightning_mace", Items.IRON_AXE),
            Map.entry("lightning_shot", Items.COPPER_INGOT),
            Map.entry("lilpenguin_pal", Items.ICE),
            Map.entry("luthorbot", Items.IRON_INGOT),
            Map.entry("macho_smooch", Items.HONEY_BOTTLE),
            Map.entry("mighty_punch", Items.OAK_BUTTON),
            Map.entry("mind_control", Items.GHAST_TEAR),
            Map.entry("missile_barrage", Items.FIREWORK_ROCKET),
            Map.entry("natures_wrath", Items.OAK_SAPLING),
            Map.entry("nuh_uh", Items.MILK_BUCKET),
            Map.entry("pattycake", Items.CAKE),
            Map.entry("plasma_shot_bee", Items.MAGMA_CREAM),
            Map.entry("plasma_shot_jinx", Items.FIREWORK_STAR),
            Map.entry("poison_arrow", Items.TIPPED_ARROW),
            Map.entry("poison_pellet", Items.SPIDER_EYE),
            Map.entry("ponder", Items.PAPER),
            Map.entry("presents", Items.CHEST),
            Map.entry("presidential_slam", Items.WOODEN_SWORD),
            Map.entry("puddin_pucker", Items.FERMENTED_SPIDER_EYE),
            Map.entry("punchies", Items.BRICK),
            Map.entry("quick_punch_raw", Items.OAK_BUTTON),
            Map.entry("quick_punch_stun", Items.OAK_BUTTON),
            Map.entry("quick_shot", Items.SPECTRAL_ARROW),
            Map.entry("raspberry", Items.SWEET_BERRIES),
            Map.entry("riddle_me_this", Items.REDSTONE_BLOCK),
            Map.entry("robo_buddy", Items.LEVER),
            Map.entry("rock_shot", Items.COBBLESTONE),
            Map.entry("root", Items.VINE),
            Map.entry("scarab_swords", Items.NETHERITE_SWORD),
            Map.entry("science", Items.POTION),
            Map.entry("shazam", Items.NETHER_STAR),
            Map.entry("shockwave_stomp", Items.IRON_BOOTS),
            Map.entry("shuriken", Items.IRON_NUGGET),
            Map.entry("shuriken_storm", Items.NETHERITE_SCRAP),
            Map.entry("skeets", Items.CLOCK),
            Map.entry("slime_ball", Items.SLIME_BALL),
            Map.entry("smoke_bomb", Items.GUNPOWDER),
            Map.entry("sonic_cannon", Items.SCULK_SENSOR),
            Map.entry("soopah_laser", Items.BEACON),
            Map.entry("soul_punch", Items.POLISHED_BLACKSTONE_BUTTON),
            Map.entry("starfish_chuck", Items.NAUTILUS_SHELL),
            Map.entry("stinky_fish", Items.COD),
            Map.entry("super_dash", Items.SUGAR),
            Map.entry("super_sockem", Items.CRIMSON_BUTTON),
            Map.entry("tea_chi", Items.GLOWSTONE_DUST),
            Map.entry("tea_time", Items.GLASS_BOTTLE),
            Map.entry("tea_toss", Items.FLOWER_POT),
            Map.entry("team_laser_eyes", Items.BLAZE_ROD),
            Map.entry("telekinesis", Items.AMETHYST_CLUSTER),
            Map.entry("teleport_strike", Items.ENDER_CHEST),
            Map.entry("the_heal", Items.GOLDEN_APPLE),
            Map.entry("trash_can_toss", Items.CAULDRON),
            Map.entry("trident_throw", Items.TRIDENT),
            Map.entry("twin_dash", Items.DRIED_KELP),
            Map.entry("ultimate_batarang", Items.PHANTOM_MEMBRANE),
            Map.entry("umbrella_shot", Items.CROSSBOW),
            Map.entry("venom", Items.NETHER_WART),
            Map.entry("waffles", Items.COOKIE),
            Map.entry("waffles_chargeup", Items.COOKIE),
            Map.entry("whale_drop", Items.PUFFERFISH),
            Map.entry("wing_ding", Items.PHANTOM_MEMBRANE)
    );
}
