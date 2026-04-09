package bruhof.teenycraft.item;

import bruhof.teenycraft.TeenyCraft;
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
                        pOutput.accept(ModItems.ROBIN.get());
                        pOutput.accept(ModItems.CYBORG.get());
                        pOutput.accept(ModItems.RAVEN.get());
                        pOutput.accept(ModItems.STARFIRE.get());
                        pOutput.accept(ModItems.BEAST_BOY.get());
                        pOutput.accept(ModItems.SILKIE.get());

                        pOutput.accept(ModItems.BATMAN.get());
                        pOutput.accept(ModItems.SUPERMAN.get());
                        pOutput.accept(ModItems.SEE_MORE.get());
                        pOutput.accept(ModItems.HARLEY_QUINN.get());
                        pOutput.accept(ModItems.JOKER.get());
                        pOutput.accept(ModItems.BILLY_NUMEROUS.get());

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

                        pOutput.accept(ModItems.TOFU.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
