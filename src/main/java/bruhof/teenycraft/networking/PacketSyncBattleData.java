package bruhof.teenycraft.networking;

import bruhof.teenycraft.client.ClientBattleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;

public class PacketSyncBattleData {
    private final boolean isBattling;
    private final String activeFigureName;
    private final int activeFigureIndex;
    private final int currentHp;
    private final int maxHp;
    private final float currentMana;
    private final List<String> effects;
    private final List<String> benchInfo;
    private final List<Integer> benchIndices;
    
    // Enemy Data
    private final String enemyName;
    private final int enemyHp;
    private final int enemyMaxHp;

    public PacketSyncBattleData(boolean isBattling, String activeFigureName, int activeFigureIndex, int currentHp, int maxHp, float currentMana, List<String> effects, List<String> benchInfo, List<Integer> benchIndices, String enemyName, int enemyHp, int enemyMaxHp) {
        this.isBattling = isBattling;
        this.activeFigureName = activeFigureName;
        this.activeFigureIndex = activeFigureIndex;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.currentMana = currentMana;
        this.effects = effects;
        this.benchInfo = benchInfo;
        this.benchIndices = benchIndices;
        this.enemyName = enemyName;
        this.enemyHp = enemyHp;
        this.enemyMaxHp = enemyMaxHp;
    }

    public PacketSyncBattleData(FriendlyByteBuf buf) {
        this.isBattling = buf.readBoolean();
        this.activeFigureName = buf.readUtf();
        this.activeFigureIndex = buf.readInt();
        this.currentHp = buf.readInt();
        this.maxHp = buf.readInt();
        this.currentMana = buf.readFloat();
        
        this.effects = buf.readList(FriendlyByteBuf::readUtf);
        this.benchInfo = buf.readList(FriendlyByteBuf::readUtf);
        this.benchIndices = buf.readList(FriendlyByteBuf::readInt);
        
        this.enemyName = buf.readUtf();
        this.enemyHp = buf.readInt();
        this.enemyMaxHp = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isBattling);
        buf.writeUtf(activeFigureName);
        buf.writeInt(activeFigureIndex);
        buf.writeInt(currentHp);
        buf.writeInt(maxHp);
        buf.writeFloat(currentMana);
        
        buf.writeCollection(effects, FriendlyByteBuf::writeUtf);
        buf.writeCollection(benchInfo, FriendlyByteBuf::writeUtf);
        buf.writeCollection(benchIndices, FriendlyByteBuf::writeInt);
        
        buf.writeUtf(enemyName);
        buf.writeInt(enemyHp);
        buf.writeInt(enemyMaxHp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientBattleData.set(isBattling, activeFigureName, activeFigureIndex, currentHp, maxHp, currentMana, effects, benchInfo, benchIndices, enemyName, enemyHp, enemyMaxHp);
        });
        return true;
    }
}
