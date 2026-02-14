package bruhof.teenycraft.networking;

import bruhof.teenycraft.screen.TitanManagerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeBox {
    private final int boxIndex;

    public PacketChangeBox(int boxIndex) {
        this.boxIndex = boxIndex;
    }

    public PacketChangeBox(FriendlyByteBuf buf) {
        this.boxIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.boxIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof TitanManagerMenu menu) {
                menu.setBox(this.boxIndex);
            }
        });
        return true;
    }
}
