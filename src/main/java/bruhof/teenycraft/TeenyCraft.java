package bruhof.teenycraft;

import bruhof.teenycraft.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TeenyCraft.MOD_ID)
public class TeenyCraft {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "teenycraft";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public TeenyCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        bruhof.teenycraft.item.ModCreativeTabs.register(modEventBus);
        bruhof.teenycraft.networking.ModMessages.register();
        bruhof.teenycraft.screen.ModMenuTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        
        // Register the Figure Loader
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
    }

    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
    }

    private void clientSetup(final net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(bruhof.teenycraft.screen.ModMenuTypes.TITAN_MANAGER_MENU.get(), bruhof.teenycraft.screen.TitanManagerScreen::new);
        });
    }

    private void onAddReloadListener(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new bruhof.teenycraft.util.FigureLoader());
        event.addListener(new bruhof.teenycraft.util.AbilityLoader());
    }
}