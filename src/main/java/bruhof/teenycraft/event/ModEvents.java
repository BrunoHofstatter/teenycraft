package bruhof.teenycraft.event;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import net.minecraft.resources.ResourceLocation;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTitanData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(TitanManagerProvider.TITAN_MANAGER).isPresent()) {
                event.addCapability(new ResourceLocation(TeenyCraft.MOD_ID, "titan_manager"), new TitanManagerProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(oldStore -> {
                event.getEntity().getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(ITitanManager.class);
        }
    }
}
