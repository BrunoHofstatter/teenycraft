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

        net.messageBuilder(PacketSyncTeenyCoins.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTeenyCoins::new)
                .encoder(PacketSyncTeenyCoins::toBytes)
                .consumerMainThread(PacketSyncTeenyCoins::handle)
                .add();

        net.messageBuilder(PacketTitanManagerAction.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketTitanManagerAction::new)
                .encoder(PacketTitanManagerAction::toBytes)
                .consumerMainThread(PacketTitanManagerAction::handle)
                .add();

        net.messageBuilder(PacketSyncTitanManagerView.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncTitanManagerView::new)
                .encoder(PacketSyncTitanManagerView::toBytes)
                .consumerMainThread(PacketSyncTitanManagerView::handle)
                .add();

        net.messageBuilder(PacketSyncBattleData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncBattleData::new)
                .encoder(PacketSyncBattleData::toBytes)
                .consumerMainThread(PacketSyncBattleData::handle)
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
