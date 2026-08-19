package bruhof.teenycraft.networking;

import bruhof.teenycraft.battle.presentation.BattleHudEffectSnapshot;
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
        buf.writeInt(participant.entityId());
        buf.writeUtf(participant.name());
        buf.writeUtf(participant.activeFigureId());
        buf.writeUtf(participant.activeSkinId());
        buf.writeUtf(participant.activeFigureModelType());
        buf.writeInt(participant.activeFigureIndex());
        buf.writeInt(participant.currentHp());
        buf.writeInt(participant.maxHp());
        buf.writeFloat(participant.currentMana());
        buf.writeFloat(participant.currentTofuMana());
        buf.writeUtf(participant.tofuPreviewEffectId());
        buf.writeBoolean(participant.tofuPreviewSelfTarget());
        buf.writeFloat(participant.batteryCharge());
        buf.writeFloat(participant.batterySpawnPct());
        buf.writeBoolean(participant.accessoryActive());
        buf.writeUtf(participant.equippedAccessoryId());
        buf.writeInt(participant.basePower());
        buf.writeInt(participant.powerUp());
        buf.writeInt(participant.powerDown());
        buf.writeVarIntArray(participant.cooldowns());
        buf.writeVarIntArray(participant.slotProgress());
        for (boolean hasMine : participant.hasActiveMine()) {
            buf.writeBoolean(hasMine);
        }
        buf.writeInt(participant.waffleBlockedSlot());
        buf.writeInt(participant.waffleTicksRemaining());
        buf.writeInt(participant.chargeSlot());
        buf.writeInt(participant.chargeTicksRemaining());
        buf.writeInt(participant.chargeTotalTicks());
        buf.writeInt(participant.blueChannelSlot());
        buf.writeInt(participant.blueChannelTicksRemaining());
        buf.writeInt(participant.blueChannelTotalTicks());
        buf.writeCollection(participant.abilityIds(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.abilityTiers(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.abilityGolden(), FriendlyByteBuf::writeBoolean);
        buf.writeCollection(participant.effects(), PacketSyncBattleData::writeEffect);
        buf.writeCollection(participant.benchInfo(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(participant.benchIndices(), FriendlyByteBuf::writeInt);
        buf.writeCollection(participant.benchFigureIds(), FriendlyByteBuf::writeUtf);
    }

    private static BattleHudParticipantSnapshot readParticipant(FriendlyByteBuf buf) {
        boolean[] hasActiveMine = new boolean[3];
        int entityId = buf.readInt();
        String name = buf.readUtf();
        String activeFigureId = buf.readUtf();
        String activeSkinId = buf.readUtf();
        String activeFigureModelType = buf.readUtf();
        int activeFigureIndex = buf.readInt();
        int currentHp = buf.readInt();
        int maxHp = buf.readInt();
        float currentMana = buf.readFloat();
        float currentTofuMana = buf.readFloat();
        String tofuPreviewEffectId = buf.readUtf();
        boolean tofuPreviewSelfTarget = buf.readBoolean();
        float batteryCharge = buf.readFloat();
        float batterySpawnPct = buf.readFloat();
        boolean accessoryActive = buf.readBoolean();
        String equippedAccessoryId = buf.readUtf();
        int basePower = buf.readInt();
        int powerUp = buf.readInt();
        int powerDown = buf.readInt();
        int[] cooldowns = buf.readVarIntArray();
        int[] slotProgress = buf.readVarIntArray();
        for (int i = 0; i < hasActiveMine.length; i++) {
            hasActiveMine[i] = buf.readBoolean();
        }
        int waffleBlockedSlot = buf.readInt();
        int waffleTicksRemaining = buf.readInt();
        int chargeSlot = buf.readInt();
        int chargeTicksRemaining = buf.readInt();
        int chargeTotalTicks = buf.readInt();
        int blueChannelSlot = buf.readInt();
        int blueChannelTicksRemaining = buf.readInt();
        int blueChannelTotalTicks = buf.readInt();

        List<String> abilityIds = buf.readList(FriendlyByteBuf::readUtf);
        List<String> abilityTiers = buf.readList(FriendlyByteBuf::readUtf);
        List<Boolean> abilityGolden = buf.readList(FriendlyByteBuf::readBoolean);
        List<BattleHudEffectSnapshot> effects = buf.readList(PacketSyncBattleData::readEffect);
        List<String> benchInfo = buf.readList(FriendlyByteBuf::readUtf);
        List<Integer> benchIndices = buf.readList(FriendlyByteBuf::readInt);
        List<String> benchFigureIds = buf.readList(FriendlyByteBuf::readUtf);

        return new BattleHudParticipantSnapshot(
                entityId,
                name,
                activeFigureId,
                activeSkinId,
                activeFigureModelType,
                activeFigureIndex,
                currentHp,
                maxHp,
                currentMana,
                currentTofuMana,
                tofuPreviewEffectId,
                tofuPreviewSelfTarget,
                batteryCharge,
                batterySpawnPct,
                accessoryActive,
                equippedAccessoryId,
                basePower,
                powerUp,
                powerDown,
                cooldowns,
                slotProgress,
                hasActiveMine,
                waffleBlockedSlot,
                waffleTicksRemaining,
                chargeSlot,
                chargeTicksRemaining,
                chargeTotalTicks,
                blueChannelSlot,
                blueChannelTicksRemaining,
                blueChannelTotalTicks,
                abilityIds,
                abilityTiers,
                abilityGolden,
                effects,
                benchInfo,
                benchIndices,
                benchFigureIds
        );
    }

    private static void writeEffect(FriendlyByteBuf buf, BattleHudEffectSnapshot effect) {
        buf.writeUtf(effect.id());
        buf.writeInt(effect.durationTicks());
        buf.writeInt(effect.magnitude());
        buf.writeBoolean(effect.infinite());
    }

    private static BattleHudEffectSnapshot readEffect(FriendlyByteBuf buf) {
        return new BattleHudEffectSnapshot(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }
}
