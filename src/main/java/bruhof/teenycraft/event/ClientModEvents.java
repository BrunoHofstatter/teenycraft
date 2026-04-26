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
import bruhof.teenycraft.client.model.AbilityModelWrapper;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import bruhof.teenycraft.client.ClientBattleData;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;

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

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        String[] abilities = {"ability_1", "ability_2", "ability_3"};
        for (String ability : abilities) {
            ModelResourceLocation mrl = new ModelResourceLocation(TeenyCraft.MOD_ID, ability, "inventory");
            if (event.getModels().containsKey(mrl)) {
                event.getModels().put(mrl, new AbilityModelWrapper(event.getModels().get(mrl)));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
             if (ClientBattleData.isBattling()) {
                 if (ClientBattleData.hasEffect("freeze_movement") || ClientBattleData.hasEffect("arena_launch_movement_lock")) {
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
            return stack.hasTag() && stack.getTag().getBoolean("IsGolden") ? 1.0f : 0.0f;
        });

        ItemProperties.register(item, new ResourceLocation(TeenyCraft.MOD_ID, "is_button"), (stack, level, entity, seed) -> {
            if (entity instanceof Player player) {
                int slot = -1;
                if (item == ModItems.ABILITY_1.get()) slot = 0;
                else if (item == ModItems.ABILITY_2.get()) slot = 1;
                else if (item == ModItems.ABILITY_3.get()) slot = 2;

                if (slot != -1 && ClientBattleData.isBattling()) {
                    return ClientBattleData.hasActiveMine(slot) ? 1.0f : 0.0f;
                }
            }
            return 0.0f;
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
