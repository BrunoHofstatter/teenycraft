package bruhof.teenycraft.networking;

import bruhof.teenycraft.battle.presentation.BattleUiEventPayload;
import bruhof.teenycraft.client.BattleUiFeedbackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBattleUiEvent {
    private final BattleUiEventPayload payload;

    public PacketBattleUiEvent(BattleUiEventPayload payload) {
        this.payload = payload;
    }

    public PacketBattleUiEvent(FriendlyByteBuf buf) {
        this.payload = new BattleUiEventPayload(
                buf.readEnum(BattleUiEventPayload.Type.class),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(payload.type());
        buf.writeInt(payload.entityId());
        buf.writeInt(payload.amount());
        buf.writeInt(payload.amount2());
        buf.writeUtf(payload.value());
        buf.writeBoolean(payload.flag());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> BattleUiFeedbackManager.push(payload));
        context.setPacketHandled(true);
        return true;
    }
}
