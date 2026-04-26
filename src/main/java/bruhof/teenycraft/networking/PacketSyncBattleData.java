package bruhof.teenycraft.networking;

import bruhof.teenycraft.battle.presentation.BattleHudParticipantSnapshot;
import bruhof.teenycraft.battle.presentation.BattleHudSnapshot;
import bruhof.teenycraft.client.ClientBattleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketSyncBattleData {
    private final BattleHudSnapshot snapshot;

    public PacketSyncBattleData(BattleHudSnapshot snapshot) {
        this.snapshot = snapshot != null ? snapshot : BattleHudSnapshot.off();
    }

    public PacketSyncBattleData(FriendlyByteBuf buf) {
        this.snapshot = new BattleHudSnapshot(buf.readBoolean(), readParticipant(buf), readParticipant(buf));
    }

    public static PacketSyncBattleData off() {
        return new PacketSyncBattleData(BattleHudSnapshot.off());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(snapshot.isBattling());
        writeParticipant(buf, snapshot.player());
        writeParticipant(buf, snapshot.enemy());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientBattleData.set(snapshot));
        context.setPacketHandled(true);
        return true;
    }

    private static void writeParticipant(FriendlyByteBuf buf, BattleHudParticipantSnapshot participant) {
        buf.writeUtf(participant.name());
        buf.writeUtf(participant.activeFigureId());
        buf.writeUtf(participant.activeFigureModelType());
        buf.writeInt(participant.activeFigureIndex());
        buf.writeInt(participant.currentHp());
        buf.writeInt(participant.maxHp());
        buf.writeFloat(participant.currentMana());
        buf.writeFloat(participant.batteryCharge());
        buf.writeFloat(participant.batterySpawnPct());
        buf.writeInt(participant.basePower());
        buf.writeInt(participant.powerUp());
        buf.writeInt(participant.powerDown());
        buf.writeVarIntArray(participant.cooldowns());
        buf.writeVarIntArray(participant.slotProgress());
        for (boolean hasMine : participant.hasActiveMine()) {
            buf.writeBoolean(hasMine);
        }
        buf.writeCollection(participant.abilityIds(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.abilityTiers(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.abilityGolden(), FriendlyByteBuf::writeBoolean);
        buf.writeCollection(participant.effects(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.benchInfo(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.benchIndices(), FriendlyByteBuf::writeInt);
        buf.writeCollection(participant.benchFigureIds(), FriendlyByteBuf::writeUtf);
    }

    private static BattleHudParticipantSnapshot readParticipant(FriendlyByteBuf buf) {
        boolean[] hasActiveMine = new boolean[3];
        String name = buf.readUtf();
        String activeFigureId = buf.readUtf();
        String activeFigureModelType = buf.readUtf();
        int activeFigureIndex = buf.readInt();
        int currentHp = buf.readInt();
        int maxHp = buf.readInt();
        float currentMana = buf.readFloat();
        float batteryCharge = buf.readFloat();
        float batterySpawnPct = buf.readFloat();
        int basePower = buf.readInt();
        int powerUp = buf.readInt();
        int powerDown = buf.readInt();
        int[] cooldowns = buf.readVarIntArray();
        int[] slotProgress = buf.readVarIntArray();
        for (int i = 0; i < hasActiveMine.length; i++) {
            hasActiveMine[i] = buf.readBoolean();
        }

        List<String> abilityIds = buf.readList(FriendlyByteBuf::readUtf);
        List<String> abilityTiers = buf.readList(FriendlyByteBuf::readUtf);
        List<Boolean> abilityGolden = buf.readList(FriendlyByteBuf::readBoolean);
        List<String> effects = buf.readList(FriendlyByteBuf::readUtf);
        List<String> benchInfo = buf.readList(FriendlyByteBuf::readUtf);
        List<Integer> benchIndices = buf.readList(FriendlyByteBuf::readInt);
        List<String> benchFigureIds = buf.readList(FriendlyByteBuf::readUtf);

        return new BattleHudParticipantSnapshot(
                name,
                activeFigureId,
                activeFigureModelType,
                activeFigureIndex,
                currentHp,
                maxHp,
                currentMana,
                batteryCharge,
                batterySpawnPct,
                basePower,
                powerUp,
                powerDown,
                cooldowns,
                slotProgress,
                hasActiveMine,
                abilityIds,
                abilityTiers,
                abilityGolden,
                effects,
                benchInfo,
                benchIndices,
                benchFigureIds
        );
    }
}
