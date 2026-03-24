package bruhof.teenycraft.client;

import java.util.Map;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;

public class AbilityIconManager {
    // A clean map where you can link any Ability ID to a Vanilla Item Fallback!
    // Just add lines here. No need to touch JSON files anymore.
    public static final Map<String, Item> FALLBACKS = Map.ofEntries(
            Map.entry("raspberry", Items.SWEET_BERRIES),
            Map.entry("punchies", Items.RABBIT_FOOT),
            Map.entry("quick_punch", Items.OAK_BUTTON),
            Map.entry("flight", Items.ELYTRA),
            Map.entry("super_sockem", Items.CRIMSON_BUTTON),
            Map.entry("heat_vision", Items.ENDER_EYE),
            Map.entry("missile_barrage", Items.FIREWORK_ROCKET),
            Map.entry("harleys_mallet", Items.GOLDEN_AXE),
            Map.entry("puddin_pucker", Items.FERMENTED_SPIDER_EYE),
            Map.entry("bang", Items.TNT),
            Map.entry("laser_eyes", Items.ENDER_EYE),
            Map.entry("the_heal", Items.TORCHFLOWER),
            Map.entry("dance", Items.MUSIC_DISC_MALL),
            Map.entry("nuh_uh", Items.FEATHER),
            Map.entry("laser_eyes2", Items.ENDER_EYE),
            Map.entry("burp_shield", Items.HEART_OF_THE_SEA),
            Map.entry("burp_surprise", Items.EXPLORER_POTTERY_SHERD),
            Map.entry("cuteness", Items.PINK_DYE),
            Map.entry("curse", Items.POPPED_CHORUS_FRUIT),
            Map.entry("soul_punch", Items.POLISHED_BLACKSTONE_BUTTON),
            Map.entry("black_hole", Items.MUSIC_DISC_11),
            Map.entry("chattering_teeth", Items.GOLD_NUGGET),
            Map.entry("evil_laugh", Items.TORCHFLOWER),
            Map.entry("jokers_mallet", Items.GOLDEN_AXE),
            Map.entry("mighty_punch", Items.OAK_BUTTON),
            Map.entry("whale_drop", Items.PUFFERFISH),
            Map.entry("bat_mine", Items.TRIPWIRE_HOOK),
            Map.entry("grappling_hook", Items.IRON_HOE),
            Map.entry("jiu_jitsu", Items.WARPED_BUTTON)
    );
}
