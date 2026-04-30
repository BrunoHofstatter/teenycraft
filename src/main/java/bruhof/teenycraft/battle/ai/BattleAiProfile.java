package bruhof.teenycraft.battle.ai;

public record BattleAiProfile(int difficulty,
                              float aggression,
                              float swapBias,
                              PreferredRange preferredRange,
                              float manaDiscipline,
                              float riskTolerance,
                              float moveSpeedMult,
                              int reactionIntervalTicks,
                              int actionCommitTicks,
                              double choiceWindow,
                              boolean considerSwap,
                              boolean counterAwareness,
                              boolean advancedSwapLogic,
                              boolean considerClassDisadvantageSwap,
                              boolean considerClassAdvantageSwap,
                              int swapReconsiderationTicks) {
    public static final BattleAiProfile DEFAULT = forDifficulty(3);

    public BattleAiProfile {
        difficulty = clampInt(difficulty, 1, 5);
        aggression = clampUnit(aggression);
        swapBias = clampUnit(swapBias);
        preferredRange = preferredRange != null ? preferredRange : PreferredRange.AUTO;
        manaDiscipline = clampUnit(manaDiscipline);
        riskTolerance = clampUnit(riskTolerance);
        moveSpeedMult = clampFloat(moveSpeedMult, 0.5f, 2.0f);
        reactionIntervalTicks = Math.max(1, reactionIntervalTicks);
        actionCommitTicks = Math.max(1, actionCommitTicks);
        choiceWindow = clampDouble(choiceWindow, 0.0d, 10.0d);
        swapReconsiderationTicks = Math.max(0, swapReconsiderationTicks);
    }

    public static BattleAiProfile forDifficulty(int difficulty) {
        int clamped = clampInt(difficulty, 1, 5);
        return new BattleAiProfile(
                clamped,
                0.5f,
                0.5f,
                PreferredRange.AUTO,
                0.5f,
                0.5f,
                1.0f,
                switch (clamped) {
                    case 1 -> 24;
                    case 2 -> 18;
                    case 3 -> 14;
                    case 4 -> 10;
                    default -> 7;
                },
                switch (clamped) {
                    case 1 -> 18;
                    case 2 -> 15;
                    case 3 -> 12;
                    case 4 -> 9;
                    default -> 7;
                },
                switch (clamped) {
                    case 1 -> 2.4d;
                    case 2 -> 2.0d;
                    case 3 -> 1.6d;
                    case 4 -> 1.2d;
                    default -> 0.8d;
                },
                true,
                clamped >= 4,
                clamped >= 3,
                clamped >= 2,
                clamped >= 3,
                switch (clamped) {
                    case 1 -> 100;
                    case 2 -> 72;
                    case 3 -> 48;
                    case 4 -> 30;
                    default -> 18;
                }
        );
    }

    public boolean enablesCounterAwareness() {
        return counterAwareness;
    }

    public boolean enablesAdvancedSwapLogic() {
        return advancedSwapLogic;
    }

    public boolean considersClassDisadvantageSwap() {
        return considerClassDisadvantageSwap;
    }

    public boolean considersClassAdvantageSwap() {
        return considerClassAdvantageSwap;
    }

    public enum PreferredRange {
        AUTO("auto"),
        CLOSE("close"),
        MID("mid"),
        FAR("far");

        private final String serializedName;

        PreferredRange(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static PreferredRange fromSerialized(String value) {
            if (value == null || value.isBlank()) {
                return AUTO;
            }

            for (PreferredRange range : values()) {
                if (range.serializedName.equalsIgnoreCase(value.trim())) {
                    return range;
                }
            }
            return AUTO;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampUnit(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
