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
    private final String activeFigureId;
    private final String activeFigureModelType;
    private final int activeFigureIndex;
    private final int currentHp;
    private final int maxHp;
    private final float currentMana;
    private final float batteryCharge;
    private final float batterySpawnPct;
    private final int basePower;
    private final int powerUp;
    private final int powerDown;
    private final int[] cooldowns;
    private final int[] slotProgress;
    private final boolean[] hasActiveMine;
    private final List<String> abilityIds;
    private final List<String> abilityTiers;
    private final List<Boolean> abilityGolden;
    private final List<String> effects;
    private final List<String> benchInfo;
    private final List<Integer> benchIndices;
    private final List<String> benchFigureIds;
    
    // Enemy Data
    private final String enemyName;
    private final String enemyActiveId;
    private final String enemyActiveModelType;
    private final int enemyActiveIndex;
    private final int enemyHp;
    private final int enemyMaxHp;
    private final float enemyMana;
    private final float enemyBatteryCharge;
    private final float enemyBatterySpawnPct;
    private final int enemyBasePower;
    private final int enemyPowerUp;
    private final int enemyPowerDown;
    private final int[] enemyCooldowns;
    private final int[] enemySlotProgress;
    private final boolean[] enemyHasActiveMine;
    private final List<String> enemyAbilityIds;
    private final List<String> enemyAbilityTiers;
    private final List<Boolean> enemyAbilityGolden;
    private final List<String> enemyEffects;
    private final List<String> enemyBenchInfo;
    private final List<Integer> enemyBenchIndices;
    private final List<String> enemyBenchFigureIds;

    public PacketSyncBattleData(boolean isBattling, String activeFigureName, String activeFigureId, String activeFigureModelType, int activeFigureIndex, int currentHp, int maxHp, float currentMana, float batteryCharge, float batterySpawnPct,
                                int basePower, int powerUp, int powerDown, int[] cooldowns, int[] slotProgress, boolean[] hasActiveMine, List<String> abilityIds, List<String> abilityTiers, List<Boolean> abilityGolden,
                                List<String> effects, List<String> benchInfo, List<Integer> benchIndices, List<String> benchFigureIds,
                                String enemyName, String enemyActiveId, String enemyActiveModelType, int enemyActiveIndex, int enemyHp, int enemyMaxHp, float enemyMana, float enemyBatteryCharge, float enemyBatterySpawnPct,
                                int enemyBasePower, int enemyPowerUp, int enemyPowerDown, int[] enemyCooldowns, int[] enemySlotProgress, boolean[] enemyHasActiveMine, List<String> enemyAbilityIds, List<String> enemyAbilityTiers, List<Boolean> enemyAbilityGolden,
                                List<String> enemyEffects, List<String> enemyBenchInfo, List<Integer> enemyBenchIndices, List<String> enemyBenchFigureIds) {
        this.isBattling = isBattling;
        this.activeFigureName = activeFigureName;
        this.activeFigureId = activeFigureId;
        this.activeFigureModelType = activeFigureModelType;
        this.activeFigureIndex = activeFigureIndex;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.currentMana = currentMana;
        this.batteryCharge = batteryCharge;
        this.batterySpawnPct = batterySpawnPct;
        this.basePower = basePower;
        this.powerUp = powerUp;
        this.powerDown = powerDown;
        this.cooldowns = cooldowns;
        this.slotProgress = slotProgress;
        this.hasActiveMine = hasActiveMine;
        this.abilityIds = abilityIds;
        this.abilityTiers = abilityTiers;
        this.abilityGolden = abilityGolden;
        this.effects = effects;
        this.benchInfo = benchInfo;
        this.benchIndices = benchIndices;
        this.benchFigureIds = benchFigureIds;
        this.enemyName = enemyName;
        this.enemyActiveId = enemyActiveId;
        this.enemyActiveModelType = enemyActiveModelType;
        this.enemyActiveIndex = enemyActiveIndex;
        this.enemyHp = enemyHp;
        this.enemyMaxHp = enemyMaxHp;
        this.enemyMana = enemyMana;
        this.enemyBatteryCharge = enemyBatteryCharge;
        this.enemyBatterySpawnPct = enemyBatterySpawnPct;
        this.enemyBasePower = enemyBasePower;
        this.enemyPowerUp = enemyPowerUp;
        this.enemyPowerDown = enemyPowerDown;
        this.enemyCooldowns = enemyCooldowns;
        this.enemySlotProgress = enemySlotProgress;
        this.enemyHasActiveMine = enemyHasActiveMine;
        this.enemyAbilityIds = enemyAbilityIds;
        this.enemyAbilityTiers = enemyAbilityTiers;
        this.enemyAbilityGolden = enemyAbilityGolden;
        this.enemyEffects = enemyEffects;
        this.enemyBenchInfo = enemyBenchInfo;
        this.enemyBenchIndices = enemyBenchIndices;
        this.enemyBenchFigureIds = enemyBenchFigureIds;
    }

    public PacketSyncBattleData(FriendlyByteBuf buf) {
        this.isBattling = buf.readBoolean();
        this.activeFigureName = buf.readUtf();
        this.activeFigureId = buf.readUtf();
        this.activeFigureModelType = buf.readUtf();
        this.activeFigureIndex = buf.readInt();
        this.currentHp = buf.readInt();
        this.maxHp = buf.readInt();
        this.currentMana = buf.readFloat();
        this.batteryCharge = buf.readFloat();
        this.batterySpawnPct = buf.readFloat();
        this.basePower = buf.readInt();
        this.powerUp = buf.readInt();
        this.powerDown = buf.readInt();
        this.cooldowns = buf.readVarIntArray();
        this.slotProgress = buf.readVarIntArray();
        
        this.hasActiveMine = new boolean[3];
        for(int i=0; i<3; i++) hasActiveMine[i] = buf.readBoolean();

        this.abilityIds = buf.readList(FriendlyByteBuf::readUtf);
        this.abilityTiers = buf.readList(FriendlyByteBuf::readUtf);
        this.abilityGolden = buf.readList(FriendlyByteBuf::readBoolean);
        this.effects = buf.readList(FriendlyByteBuf::readUtf);
        this.benchInfo = buf.readList(FriendlyByteBuf::readUtf);
        this.benchIndices = buf.readList(FriendlyByteBuf::readInt);
        this.benchFigureIds = buf.readList(FriendlyByteBuf::readUtf);
        
        this.enemyName = buf.readUtf();
        this.enemyActiveId = buf.readUtf();
        this.enemyActiveModelType = buf.readUtf();
        this.enemyActiveIndex = buf.readInt();
        this.enemyHp = buf.readInt();
        this.enemyMaxHp = buf.readInt();
        this.enemyMana = buf.readFloat();
        this.enemyBatteryCharge = buf.readFloat();
        this.enemyBatterySpawnPct = buf.readFloat();
        this.enemyBasePower = buf.readInt();
        this.enemyPowerUp = buf.readInt();
        this.enemyPowerDown = buf.readInt();
        this.enemyCooldowns = buf.readVarIntArray();
        this.enemySlotProgress = buf.readVarIntArray();

        this.enemyHasActiveMine = new boolean[3];
        for(int i=0; i<3; i++) enemyHasActiveMine[i] = buf.readBoolean();

        this.enemyAbilityIds = buf.readList(FriendlyByteBuf::readUtf);
        this.enemyAbilityTiers = buf.readList(FriendlyByteBuf::readUtf);
        this.enemyAbilityGolden = buf.readList(FriendlyByteBuf::readBoolean);
        this.enemyEffects = buf.readList(FriendlyByteBuf::readUtf);
        this.enemyBenchInfo = buf.readList(FriendlyByteBuf::readUtf);
        this.enemyBenchIndices = buf.readList(FriendlyByteBuf::readInt);
        this.enemyBenchFigureIds = buf.readList(FriendlyByteBuf::readUtf);
    }

    public static PacketSyncBattleData off() {
        return new PacketSyncBattleData(false, "", "none", "default", 0, 0, 0, 0, 0, -1.0f,
            0, 0, 0, new int[3], new int[3], new boolean[3], new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            "", "none", "default", 0, 0, 0, 0, 0, -1.0f,
            0, 0, 0, new int[3], new int[3], new boolean[3], new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isBattling);
        buf.writeUtf(activeFigureName);
        buf.writeUtf(activeFigureId);
        buf.writeUtf(activeFigureModelType);
        buf.writeInt(activeFigureIndex);
        buf.writeInt(currentHp);
        buf.writeInt(maxHp);
        buf.writeFloat(currentMana);
        buf.writeFloat(batteryCharge);
        buf.writeFloat(batterySpawnPct);
        buf.writeInt(basePower);
        buf.writeInt(powerUp);
        buf.writeInt(powerDown);
        buf.writeVarIntArray(cooldowns);
        buf.writeVarIntArray(slotProgress);
        
        for(int i=0; i<3; i++) buf.writeBoolean(hasActiveMine[i]);

        buf.writeCollection(abilityIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(abilityTiers, FriendlyByteBuf::writeUtf);
        buf.writeCollection(abilityGolden, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(effects, FriendlyByteBuf::writeUtf);
        buf.writeCollection(benchInfo, FriendlyByteBuf::writeUtf);
        buf.writeCollection(benchIndices, FriendlyByteBuf::writeInt);
        buf.writeCollection(benchFigureIds, FriendlyByteBuf::writeUtf);
        
        buf.writeUtf(enemyName);
        buf.writeUtf(enemyActiveId);
        buf.writeUtf(enemyActiveModelType);
        buf.writeInt(enemyActiveIndex);
        buf.writeInt(enemyHp);
        buf.writeInt(enemyMaxHp);
        buf.writeFloat(enemyMana);
        buf.writeFloat(enemyBatteryCharge);
        buf.writeFloat(enemyBatterySpawnPct);
        buf.writeInt(enemyBasePower);
        buf.writeInt(enemyPowerUp);
        buf.writeInt(enemyPowerDown);
        buf.writeVarIntArray(enemyCooldowns);
        buf.writeVarIntArray(enemySlotProgress);

        for(int i=0; i<3; i++) buf.writeBoolean(enemyHasActiveMine[i]);

        buf.writeCollection(enemyAbilityIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(enemyAbilityTiers, FriendlyByteBuf::writeUtf);
        buf.writeCollection(enemyAbilityGolden, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(enemyEffects, FriendlyByteBuf::writeUtf);
        buf.writeCollection(enemyBenchInfo, FriendlyByteBuf::writeUtf);
        buf.writeCollection(enemyBenchIndices, FriendlyByteBuf::writeInt);
        buf.writeCollection(enemyBenchFigureIds, FriendlyByteBuf::writeUtf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientBattleData.set(isBattling, activeFigureName, activeFigureId, activeFigureModelType, activeFigureIndex, currentHp, maxHp, currentMana, batteryCharge, batterySpawnPct,
                                basePower, powerUp, powerDown, cooldowns, slotProgress, hasActiveMine, abilityIds, abilityTiers, abilityGolden,
                                effects, benchInfo, benchIndices, benchFigureIds,
                                enemyName, enemyActiveId, enemyActiveModelType, enemyActiveIndex, enemyHp, enemyMaxHp, enemyMana, enemyBatteryCharge, enemyBatterySpawnPct,
                                enemyBasePower, enemyPowerUp, enemyPowerDown, enemyCooldowns, enemySlotProgress, enemyHasActiveMine, enemyAbilityIds, enemyAbilityTiers, enemyAbilityGolden,
                                enemyEffects, enemyBenchInfo, enemyBenchIndices, enemyBenchFigureIds);
        });
        return true;
    }
}
