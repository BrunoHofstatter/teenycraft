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

        // Add this line:
        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }
}