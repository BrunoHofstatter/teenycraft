package bruhof.teenycraft.networking;

import bruhof.teenycraft.screen.TitanManagerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSearchTitanManager {
    private final String query;

    public PacketSearchTitanManager(String query) {
        this.query = query;
    }

    public PacketSearchTitanManager(FriendlyByteBuf buf) {
        this.query = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.query);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof TitanManagerMenu menu) {
                menu.updateSearch(this.query);
            }
        });
        return true;
    }
}
