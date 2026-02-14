package bruhof.teenycraft.networking;

import bruhof.teenycraft.screen.TitanManagerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncSearchResults {
    private final List<Integer> matches;

    public PacketSyncSearchResults(List<Integer> matches) {
        this.matches = matches;
    }

    public PacketSyncSearchResults(FriendlyByteBuf buf) {
        this.matches = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.matches.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.matches.size());
        for (int index : this.matches) {
            buf.writeInt(index);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client Side Execution
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof TitanManagerMenu menu) {
                menu.setSearchResults(this.matches);
            }
        });
        return true;
    }
}
