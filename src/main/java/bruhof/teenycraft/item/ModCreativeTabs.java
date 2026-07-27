package bruhof.teenycraft.item;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.item.custom.ItemChip;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TeenyCraft.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TEENY_CRAFT_TAB = CREATIVE_MODE_TABS.register("teenycraft_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.ROBIN.get()))
                    .title(Component.translatable("creativetab.teenycraft_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        // Dynamically add all figures found in JSONs
                        for (String id : FigureLoader.getLoadedFigureIds()) {
                            ItemStack figure = FigureLoader.getFigureStack(id);
                            if (!figure.isEmpty()) {
                                pOutput.accept(figure);
                            }
                        }
                        
                        // Also add base items for reference
                        pOutput.accept(ModItems.TITAN_PAD.get());
                        pOutput.accept(ModItems.CHIP_FUSER.get());
                        pOutput.accept(ModItems.SILKIE_STATION.get());
                        pOutput.accept(ModItems.ROBIN.get());

                        pOutput.accept(ModItems.DISPLAY_TEST_AXE_FIRSTPERSON.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_AROUND_HYBRID.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_AMAZONIAN_BEATDOWN.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_BIRDARANG.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_BAT_MINE.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_TRIDENT_THROW.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_CONSTRUCT_BEAM.get());
                        pOutput.accept(ModItems.DISPLAY_TEST_GRAPPLING_HOOK.get());

                        pOutput.accept(ModItems.ACCESSORY_TITANS_COIN.get());
                        pOutput.accept(ModItems.ACCESSORY_MOTHER_BOX.get());
                        pOutput.accept(ModItems.ACCESSORY_BAT_SIGNAL.get());
                        pOutput.accept(ModItems.ACCESSORY_RED_LANTERN_BATTERY.get());
                        pOutput.accept(ModItems.ACCESSORY_GREEN_LANTERN_BATTERY.get());
                        pOutput.accept(ModItems.ACCESSORY_VIOLET_LANTERN_BATTERY.get());
                        pOutput.accept(ModItems.ACCESSORY_RAVENS_SPELLBOOK.get());
                        pOutput.accept(ModItems.ACCESSORY_CYBORGS_WAFFLE_SHOOTER.get());
                        pOutput.accept(ModItems.ACCESSORY_LIL_PENGUIN.get());
                        pOutput.accept(ModItems.ACCESSORY_KRYPTONITE.get());
                        pOutput.accept(ModItems.ACCESSORY_JUSTICE_LEAGUE_COIN.get());
                        pOutput.accept(ModItems.ACCESSORY_BIRDARANG.get());
                        pOutput.accept(ModItems.ACCESSORY_SUPERMANS_UNDERPANTS.get());
                        pOutput.accept(ModItems.ACCESSORY_KRYPTO_THE_SUPERDOG.get());

                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_TOUGH_GUY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_SMOKESCREEN.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_POWER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_HEALTH.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LUCK.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_NINJA_SKILLS.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LOADED_DICE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_TOUGH_STUFF.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LUCKY_HEARTS.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_MANA_STEAL_DUCK.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LUCKY_STEAL_DUCK.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_HEALTHY_DODGE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_DODGY_HEALTHY_DODGE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_SECOND_CHANCE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_SPEEDY_DODGE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_DODGY_SPEEDY_DODGE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_HEALTHY_SECOND_CHANCE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_POINTY_HEALTH.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LUCKY_HEALTHY_DUCK.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_INSTA_CAST_CHANCE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_FAST_CAST.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_FAST_DRAW.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_FAST_DRAWCAST.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_CLEAN_ENTRY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_BEASTLY_ENTRY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_CURSE_ENTRY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_DANCE_ENTRY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_MANA_BOOST.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_MOMENTUM.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_FINISHING_MOMENTUM.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_VICTORY_DANCE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_POWERED_BEASTLY_ENTRY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_POWERED_FINISHER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_ENERGETIC_BATTERY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_DEATH_ENERGY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_SELF_EXPLOSION.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LAST_LAUGH.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_NECROMANCER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_VAMPIRE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_REGENERATIVE_CUTENESS.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_TOFU_LOVER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_LUCKY_TOFU_LOVER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_HELLO_NURSE.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_TEAM_MEDIC.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_POINTY.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_STUN_RESISTER.get(), 1));
                        pOutput.accept(ItemChip.createStack(ModItems.CHIP_FINISHER.get(), 1));

                        pOutput.accept(ModItems.TOFU.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
