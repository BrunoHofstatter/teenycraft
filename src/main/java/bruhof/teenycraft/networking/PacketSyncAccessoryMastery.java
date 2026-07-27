package bruhof.teenycraft.networking;

import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncAccessoryMastery {
    private final CompoundTag masteryData;

    public PacketSyncAccessoryMastery(CompoundTag masteryData) {
        this.masteryData = masteryData.copy();
    }

    public PacketSyncAccessoryMastery(FriendlyByteBuf buf) {
        CompoundTag decoded = buf.readNbt();
        this.masteryData = decoded != null ? decoded : new CompoundTag();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(masteryData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                        .ifPresent(mastery -> mastery.loadNBTData(masteryData));
            }
        });
        return true;
    }
}
