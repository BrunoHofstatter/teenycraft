package bruhof.teenycraft.event;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.client.model.HandOnlyModelWrapper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Isolated visual harness for comparing golden held-item treatments.
 *
 * <p>The registered display-test items remain inert. Their standard models are
 * preserved outside first- and third-person hand rendering.</p>
 */
@Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GoldenDisplayTestClientEvents {
    private static final List<ModelPair> TEST_MODELS = List.of(
            new ModelPair("display_test_amazonian_beatdown", "display_test_gold_halo_amazonian"),
            new ModelPair("display_test_birdarang", "display_test_gold_halo_birdarang")
    );

    private GoldenDisplayTestClientEvents() {
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelPair pair : TEST_MODELS) {
            event.register(modelLocation(pair.heldModel()));
        }
    }

    @SubscribeEvent
    public static void wrapDisplayTestModels(ModelEvent.ModifyBakingResult event) {
        for (ModelPair pair : TEST_MODELS) {
            ModelResourceLocation baseLocation = modelLocation(pair.baseModel());
            BakedModel baseModel = event.getModels().get(baseLocation);
            BakedModel heldModel = event.getModels().get(modelLocation(pair.heldModel()));
            if (baseModel != null && heldModel != null) {
                event.getModels().put(baseLocation, new HandOnlyModelWrapper(baseModel, heldModel));
            }
        }
    }

    private static ModelResourceLocation modelLocation(String path) {
        return new ModelResourceLocation(TeenyCraft.MOD_ID, path, "inventory");
    }

    private record ModelPair(String baseModel, String heldModel) {
    }
}
