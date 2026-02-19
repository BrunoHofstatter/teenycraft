package bruhof.teenycraft.client;

import java.util.List;
import java.util.ArrayList;

public class ClientBattleData {
    private static boolean isBattling;
    private static String activeFigureName;
    private static int activeFigureIndex;
    private static int currentHp;
    private static int maxHp;
    private static float currentMana;
    private static List<String> effects = new ArrayList<>();
    private static List<String> benchInfo = new ArrayList<>();
    private static List<Integer> benchIndices = new ArrayList<>();
    
    private static String enemyName;
    private static int enemyHp;
    private static int enemyMaxHp;

    public static void set(boolean battling, String name, int index, int hp, int mHp, float mana, List<String> eff, List<String> bench, List<Integer> indices, String eName, int eHp, int eMaxHp) {
        isBattling = battling;
        activeFigureName = name;
        activeFigureIndex = index;
        currentHp = hp;
        maxHp = mHp;
        currentMana = mana;
        effects = eff != null ? eff : new ArrayList<>();
        benchInfo = bench != null ? bench : new ArrayList<>();
        benchIndices = indices != null ? indices : new ArrayList<>();
        enemyName = eName;
        enemyHp = eHp;
        enemyMaxHp = eMaxHp;
    }

    public static boolean isBattling() { return isBattling; }
    public static String getActiveFigureName() { return activeFigureName; }
    public static int getActiveFigureIndex() { return activeFigureIndex; }
    public static int getCurrentHp() { return currentHp; }
    public static int getMaxHp() { return maxHp; }
    public static float getCurrentMana() { return currentMana; }
    public static List<String> getEffects() { return effects; }
    public static List<String> getBenchInfo() { return benchInfo; }
    public static List<Integer> getBenchIndices() { return benchIndices; }
    
    public static boolean hasEffect(String name) {
        for (String s : effects) {
            if (s.toLowerCase().startsWith(name.toLowerCase())) return true;
        }
        return false;
    }
    
    public static String getEnemyName() { return enemyName; }
    public static int getEnemyHp() { return enemyHp; }
    public static int getEnemyMaxHp() { return enemyMaxHp; }
}
