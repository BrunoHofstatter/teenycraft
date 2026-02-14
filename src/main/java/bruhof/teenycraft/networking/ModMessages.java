package bruhof.teenycraft.networking;

import bruhof.teenycraft.TeenyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(TeenyCraft.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(PacketSyncTitanData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTitanData::new)
                .encoder(PacketSyncTitanData::toBytes)
                .consumerMainThread(PacketSyncTitanData::handle)
                .add();

        net.messageBuilder(PacketChangeBox.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketChangeBox::new)
                .encoder(PacketChangeBox::toBytes)
                .consumerMainThread(PacketChangeBox::handle)
                .add();

        net.messageBuilder(PacketSearchTitanManager.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSearchTitanManager::new)
                .encoder(PacketSearchTitanManager::toBytes)
                .consumerMainThread(PacketSearchTitanManager::handle)
                .add();

        net.messageBuilder(PacketSyncSearchResults.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncSearchResults::new)
                .encoder(PacketSyncSearchResults::toBytes)
                .consumerMainThread(PacketSyncSearchResults::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
