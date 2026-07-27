package bruhof.teenycraft.accessory;

public final class AccessoryProgression {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private AccessoryProgression() {
    }

    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    public static String milestoneIdForUnlock(String accessoryId, int unlockTier) {
        if (accessoryId == null || accessoryId.isBlank() || unlockTier <= MIN_TIER || unlockTier > MAX_TIER) {
            return "";
        }
        return accessoryId + "_tier_" + unlockTier;
    }

    public static String currentMilestoneId(String accessoryId, int currentTier) {
        return milestoneIdForUnlock(accessoryId, currentTier + 1);
    }
}
