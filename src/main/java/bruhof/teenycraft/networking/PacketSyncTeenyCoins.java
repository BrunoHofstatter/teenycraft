package bruhof.teenycraft.networking;

import bruhof.teenycraft.capability.TeenyCoinsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncTeenyCoins {
    private final int coins;

    public PacketSyncTeenyCoins(int coins) {
        this.coins = coins;
    }

    public PacketSyncTeenyCoins(FriendlyByteBuf buf) {
        this.coins = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.coins);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(TeenyCoinsProvider.TEENY_COINS).ifPresent(handler -> {
                    handler.setCoins(this.coins);
                });
            }
        });
        return true;
    }
}
