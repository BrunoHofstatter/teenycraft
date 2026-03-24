package bruhof.teenycraft.client;

import java.util.List;
import java.util.ArrayList;

public class ClientBattleData {
    private static boolean isBattling;
    private static String activeFigureName;
    private static String activeFigureId;
    private static String activeFigureModelType;
    private static int activeFigureIndex;
    private static int currentHp;
    private static int maxHp;
    private static float currentMana;
    private static float batteryCharge;
    private static float batterySpawnPct;
    private static int basePower;
    private static int powerUp;
    private static int powerDown;
    private static int[] cooldowns = new int[3];
    private static int[] slotProgress = new int[3];
    private static boolean[] hasActiveMine = new boolean[3];
    private static List<String> abilityIds = new ArrayList<>();
    private static List<String> abilityTiers = new ArrayList<>();
    private static List<Boolean> abilityGolden = new ArrayList<>();
    private static List<String> effects = new ArrayList<>();
    private static List<String> benchInfo = new ArrayList<>();
    private static List<Integer> benchIndices = new ArrayList<>();
    private static List<String> benchFigureIds = new ArrayList<>();
    
    private static String enemyName;
    private static String enemyActiveId;
    private static String enemyActiveModelType;
    private static int enemyActiveIndex;
    private static int enemyHp;
    private static int enemyMaxHp;
    private static float enemyMana;
    private static float enemyBatteryCharge;
    private static float enemyBatterySpawnPct;
    private static int enemyBasePower;
    private static int enemyPowerUp;
    private static int enemyPowerDown;
    private static int[] enemyCooldowns = new int[3];
    private static int[] enemySlotProgress = new int[3];
    private static boolean[] enemyHasActiveMine = new boolean[3];
    private static List<String> enemyAbilityIds = new ArrayList<>();
    private static List<String> enemyAbilityTiers = new ArrayList<>();
    private static List<Boolean> enemyAbilityGolden = new ArrayList<>();
    private static List<String> enemyEffects = new ArrayList<>();
    private static List<String> enemyBenchInfo = new ArrayList<>();
    private static List<Integer> enemyBenchIndices = new ArrayList<>();
    private static List<String> enemyBenchFigureIds = new ArrayList<>();

    public static void set(boolean battling, String name, String id, String model, int index, int hp, int mHp, float mana, float batCharge, float batPct,
                           int power, int pUp, int pDown, int[] cds, int[] progress, boolean[] mines, List<String> abIds, List<String> abTiers, List<Boolean> abGold,
                           List<String> eff, List<String> bench, List<Integer> indices, List<String> bIds,
                           String eName, String eId, String eModel, int eIndex, int eHp, int eMaxHp, float eMana, float eBatCharge, float eBatPct,
                           int ePower, int ePUp, int ePDown, int[] eCds, int[] eProgress, boolean[] eMines, List<String> eAbIds, List<String> eAbTiers, List<Boolean> eAbGold,
                           List<String> eEff, List<String> eBench, List<Integer> eIndices, List<String> ebIds) {
        isBattling = battling;
        activeFigureName = name;
        activeFigureId = id;
        activeFigureModelType = model;
        activeFigureIndex = index;
        currentHp = hp;
        maxHp = mHp;
        currentMana = mana;
        batteryCharge = batCharge;
        batterySpawnPct = batPct;
        basePower = power;
        powerUp = pUp;
        powerDown = pDown;
        cooldowns = (cds != null && cds.length >= 3) ? cds : new int[3];
        slotProgress = (progress != null && progress.length >= 3) ? progress : new int[3];
        hasActiveMine = (mines != null && mines.length >= 3) ? mines : new boolean[3];
        abilityIds = abIds != null ? abIds : new ArrayList<>();
        abilityTiers = abTiers != null ? abTiers : new ArrayList<>();
        abilityGolden = abGold != null ? abGold : new ArrayList<>();
        effects = eff != null ? eff : new ArrayList<>();
        benchInfo = bench != null ? bench : new ArrayList<>();
        benchIndices = indices != null ? indices : new ArrayList<>();
        benchFigureIds = bIds != null ? bIds : new ArrayList<>();

        enemyName = eName;
        enemyActiveId = eId;
        enemyActiveModelType = eModel;
        enemyActiveIndex = eIndex;
        enemyHp = eHp;
        enemyMaxHp = eMaxHp;
        enemyMana = eMana;
        enemyBatteryCharge = eBatCharge;
        enemyBatterySpawnPct = eBatPct;
        enemyBasePower = ePower;
        enemyPowerUp = ePUp;
        enemyPowerDown = ePDown;
        enemyCooldowns = (eCds != null && eCds.length >= 3) ? eCds : new int[3];
        enemySlotProgress = (eProgress != null && eProgress.length >= 3) ? eProgress : new int[3];
        enemyHasActiveMine = (eMines != null && eMines.length >= 3) ? eMines : new boolean[3];
        enemyAbilityIds = eAbIds != null ? eAbIds : new ArrayList<>();
        enemyAbilityTiers = eAbTiers != null ? eAbTiers : new ArrayList<>();
        enemyAbilityGolden = eAbGold != null ? eAbGold : new ArrayList<>();
        enemyEffects = eEff != null ? eEff : new ArrayList<>();
        enemyBenchInfo = eBench != null ? eBench : new ArrayList<>();
        enemyBenchIndices = eIndices != null ? eIndices : new ArrayList<>();
        enemyBenchFigureIds = ebIds != null ? ebIds : new ArrayList<>();
    }

    public static boolean isBattling() { return isBattling; }
    public static String getActiveFigureName() { return activeFigureName; }
    public static String getActiveFigureId() { return activeFigureId; }
    public static String getActiveFigureModelType() { return activeFigureModelType; }
    public static int getActiveFigureIndex() { return activeFigureIndex; }
    public static int getCurrentHp() { return currentHp; }
    public static int getMaxHp() { return maxHp; }
    public static float getCurrentMana() { return currentMana; }
    public static float getBatteryCharge() { return batteryCharge; }
    public static float getBatterySpawnPct() { return batterySpawnPct; }
    public static int getBasePower() { return basePower; }
    public static int getPowerUp() { return powerUp; }
    public static int getPowerDown() { return powerDown; }
    public static int[] getCooldowns() { return cooldowns; }
    public static int[] getSlotProgress() { return slotProgress; }
    public static boolean hasActiveMine(int slot) { 
        if(slot < 0 || slot >= 3) return false;
        return hasActiveMine[slot]; 
    }
    public static List<String> getAbilityIds() { return abilityIds; }
    public static List<String> getAbilityTiers() { return abilityTiers; }
    public static List<Boolean> getAbilityGolden() { return abilityGolden; }
    public static List<String> getEffects() { return effects; }
    public static List<String> getBenchInfo() { return benchInfo; }
    public static List<Integer> getBenchIndices() { return benchIndices; }
    public static List<String> getBenchFigureIds() { return benchFigureIds; }
    
    public static boolean hasEffect(String name) {
        for (String s : effects) {
            if (s.toLowerCase().startsWith(name.toLowerCase())) return true;
        }
        return false;
    }
    
    public static String getEnemyName() { return enemyName; }
    public static String getEnemyActiveId() { return enemyActiveId; }
    public static String getEnemyActiveModelType() { return enemyActiveModelType; }
    public static int getEnemyActiveIndex() { return enemyActiveIndex; }
    public static int getEnemyHp() { return enemyHp; }
    public static int getEnemyMaxHp() { return enemyMaxHp; }
    public static float getEnemyMana() { return enemyMana; }
    public static float getEnemyBatteryCharge() { return enemyBatteryCharge; }
    public static float getEnemyBatterySpawnPct() { return enemyBatterySpawnPct; }
    public static int getEnemyBasePower() { return enemyBasePower; }
    public static int getEnemyPowerUp() { return enemyPowerUp; }
    public static int getEnemyPowerDown() { return enemyPowerDown; }
    public static int[] getEnemyCooldowns() { return enemyCooldowns; }
    public static int[] getEnemySlotProgress() { return enemySlotProgress; }
    public static boolean enemyHasActiveMine(int slot) {
        if(slot < 0 || slot >= 3) return false;
        return enemyHasActiveMine[slot];
    }
    public static List<String> getEnemyAbilityIds() { return enemyAbilityIds; }
    public static List<String> getEnemyAbilityTiers() { return enemyAbilityTiers; }
    public static List<Boolean> getEnemyAbilityGolden() { return enemyAbilityGolden; }
    public static List<String> getEnemyEffects() { return enemyEffects; }
    public static List<String> getEnemyBenchInfo() { return enemyBenchInfo; }
    public static List<Integer> getEnemyBenchIndices() { return enemyBenchIndices; }
    public static List<String> getEnemyBenchFigureIds() { return enemyBenchFigureIds; }
}
