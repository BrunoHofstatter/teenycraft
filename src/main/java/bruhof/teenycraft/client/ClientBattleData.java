package bruhof.teenycraft.client;

import bruhof.teenycraft.battle.presentation.BattleHudParticipantSnapshot;
import bruhof.teenycraft.battle.presentation.BattleHudSnapshot;

import java.util.List;

public class ClientBattleData {
    private static BattleHudSnapshot snapshot = BattleHudSnapshot.off();

    public static void set(BattleHudSnapshot battleSnapshot) {
        snapshot = battleSnapshot != null ? battleSnapshot : BattleHudSnapshot.off();
    }

    public static BattleHudSnapshot getSnapshot() {
        return snapshot;
    }

    public static BattleHudParticipantSnapshot getPlayerSnapshot() {
        return snapshot.player();
    }

    public static BattleHudParticipantSnapshot getEnemySnapshot() {
        return snapshot.enemy();
    }

    public static boolean isBattling() {
        return snapshot.isBattling();
    }

    public static String getActiveFigureName() {
        return snapshot.player().name();
    }

    public static String getActiveFigureId() {
        return snapshot.player().activeFigureId();
    }

    public static String getActiveFigureModelType() {
        return snapshot.player().activeFigureModelType();
    }

    public static int getActiveFigureIndex() {
        return snapshot.player().activeFigureIndex();
    }

    public static int getCurrentHp() {
        return snapshot.player().currentHp();
    }

    public static int getMaxHp() {
        return snapshot.player().maxHp();
    }

    public static float getCurrentMana() {
        return snapshot.player().currentMana();
    }

    public static float getBatteryCharge() {
        return snapshot.player().batteryCharge();
    }

    public static float getBatterySpawnPct() {
        return snapshot.player().batterySpawnPct();
    }

    public static int getBasePower() {
        return snapshot.player().basePower();
    }

    public static int getPowerUp() {
        return snapshot.player().powerUp();
    }

    public static int getPowerDown() {
        return snapshot.player().powerDown();
    }

    public static int[] getCooldowns() {
        return snapshot.player().cooldowns();
    }

    public static int[] getSlotProgress() {
        return snapshot.player().slotProgress();
    }

    public static boolean hasActiveMine(int slot) {
        return snapshot.player().hasActiveMine(slot);
    }

    public static List<String> getAbilityIds() {
        return snapshot.player().abilityIds();
    }

    public static List<String> getAbilityTiers() {
        return snapshot.player().abilityTiers();
    }

    public static List<Boolean> getAbilityGolden() {
        return snapshot.player().abilityGolden();
    }

    public static List<String> getEffects() {
        return snapshot.player().effects();
    }

    public static List<String> getBenchInfo() {
        return snapshot.player().benchInfo();
    }

    public static List<Integer> getBenchIndices() {
        return snapshot.player().benchIndices();
    }

    public static List<String> getBenchFigureIds() {
        return snapshot.player().benchFigureIds();
    }

    public static boolean hasEffect(String name) {
        return snapshot.player().hasEffect(name);
    }

    public static String getEnemyName() {
        return snapshot.enemy().name();
    }

    public static String getEnemyActiveId() {
        return snapshot.enemy().activeFigureId();
    }

    public static String getEnemyActiveModelType() {
        return snapshot.enemy().activeFigureModelType();
    }

    public static int getEnemyActiveIndex() {
        return snapshot.enemy().activeFigureIndex();
    }

    public static int getEnemyHp() {
        return snapshot.enemy().currentHp();
    }

    public static int getEnemyMaxHp() {
        return snapshot.enemy().maxHp();
    }

    public static float getEnemyMana() {
        return snapshot.enemy().currentMana();
    }

    public static float getEnemyBatteryCharge() {
        return snapshot.enemy().batteryCharge();
    }

    public static float getEnemyBatterySpawnPct() {
        return snapshot.enemy().batterySpawnPct();
    }

    public static int getEnemyBasePower() {
        return snapshot.enemy().basePower();
    }

    public static int getEnemyPowerUp() {
        return snapshot.enemy().powerUp();
    }

    public static int getEnemyPowerDown() {
        return snapshot.enemy().powerDown();
    }

    public static int[] getEnemyCooldowns() {
        return snapshot.enemy().cooldowns();
    }

    public static int[] getEnemySlotProgress() {
        return snapshot.enemy().slotProgress();
    }

    public static boolean enemyHasActiveMine(int slot) {
        return snapshot.enemy().hasActiveMine(slot);
    }

    public static List<String> getEnemyAbilityIds() {
        return snapshot.enemy().abilityIds();
    }

    public static List<String> getEnemyAbilityTiers() {
        return snapshot.enemy().abilityTiers();
    }

    public static List<Boolean> getEnemyAbilityGolden() {
        return snapshot.enemy().abilityGolden();
    }

    public static List<String> getEnemyEffects() {
        return snapshot.enemy().effects();
    }

    public static List<String> getEnemyBenchInfo() {
        return snapshot.enemy().benchInfo();
    }

    public static List<Integer> getEnemyBenchIndices() {
        return snapshot.enemy().benchIndices();
    }

    public static List<String> getEnemyBenchFigureIds() {
        return snapshot.enemy().benchFigureIds();
    }
}
