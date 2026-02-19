package bruhof.teenycraft.event;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.client.BattleOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import bruhof.teenycraft.entity.ModEntities;
import bruhof.teenycraft.client.renderer.DummyRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import bruhof.teenycraft.util.AbilityLoader;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import bruhof.teenycraft.client.ClientBattleData;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

@Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerAbilityProperties(ModItems.ABILITY_1.get());
            registerAbilityProperties(ModItems.ABILITY_2.get());
            registerAbilityProperties(ModItems.ABILITY_3.get());
        });
    }

    @Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
             if (ClientBattleData.isBattling()) {
                 if (ClientBattleData.hasEffect("freeze_movement")) {
                     event.getInput().forwardImpulse = 0;
                     event.getInput().leftImpulse = 0;
                     event.getInput().jumping = false;
                     event.getInput().shiftKeyDown = false;
                 }
             }
        }
    }

    private static void registerAbilityProperties(net.minecraft.world.item.Item item) {
        ItemProperties.register(item, new ResourceLocation(TeenyCraft.MOD_ID, "ability_id"), (stack, level, entity, seed) -> {
            if (stack.hasTag()) {
                String id = stack.getTag().getString(ItemAbility.TAG_ID);
                return (float) AbilityLoader.getTextureIndex(id);
            }
            return 0.0f;
        });

        ItemProperties.register(item, new ResourceLocation(TeenyCraft.MOD_ID, "is_golden"), (stack, level, entity, seed) -> {
            return (stack.hasTag() && stack.getTag().getBoolean(ItemAbility.TAG_GOLDEN)) ? 1.0f : 0.0f;
        });
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("battle_overlay", BattleOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TEENY_DUMMY.get(), DummyRenderer::new);
    }
}
