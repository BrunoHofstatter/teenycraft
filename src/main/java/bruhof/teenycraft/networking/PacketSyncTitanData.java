package bruhof.teenycraft.networking;

import bruhof.teenycraft.capability.TitanManagerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncTitanData {
    private final CompoundTag inventory;

    public PacketSyncTitanData(CompoundTag inventory) {
        this.inventory = inventory;
    }

    public PacketSyncTitanData(FriendlyByteBuf buf) {
        this.inventory = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.inventory);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client side logic
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                    handler.loadNBTData(this.inventory);
                });
            }
        });
        return true;
    }
}
