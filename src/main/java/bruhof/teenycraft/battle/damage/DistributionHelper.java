package bruhof.teenycraft.battle.damage;

public class DistributionHelper {

    /**
     * Splits a total integer value into 'parts' number of integers.
     * The remainder is distributed one by one to the first few parts.
     * Example: split(10, 3) -> [4, 3, 3]
     */
    public static int[] split(int total, int parts) {
        if (parts <= 0) return new int[0];
        if (parts == 1) return new int[]{total};
        
        int base = total / parts;
        int remainder = total % parts;
        
        int[] result = new int[parts];
        for (int i = 0; i < parts; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }
}
