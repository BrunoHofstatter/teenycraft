package bruhof.teenycraft.battle.ai;

public record BattleAiProfile(int difficulty,
                              float aggression,
                              float swapBias,
                              PreferredRange preferredRange,
                              float manaDiscipline,
                              float riskTolerance) {
    public static final BattleAiProfile DEFAULT = new BattleAiProfile(3, 0.5f, 0.5f, PreferredRange.AUTO, 0.5f, 0.5f);

    public BattleAiProfile {
        difficulty = clampInt(difficulty, 1, 5);
        aggression = clampUnit(aggression);
        swapBias = clampUnit(swapBias);
        preferredRange = preferredRange != null ? preferredRange : PreferredRange.AUTO;
        manaDiscipline = clampUnit(manaDiscipline);
        riskTolerance = clampUnit(riskTolerance);
    }

    public int reactionIntervalTicks() {
        return switch (difficulty) {
            case 1 -> 24;
            case 2 -> 18;
            case 3 -> 14;
            case 4 -> 10;
            default -> 7;
        };
    }

    public int actionCommitTicks() {
        return switch (difficulty) {
            case 1 -> 18;
            case 2 -> 15;
            case 3 -> 12;
            case 4 -> 9;
            default -> 7;
        };
    }

    public double choiceWindow() {
        return switch (difficulty) {
            case 1 -> 2.4d;
            case 2 -> 2.0d;
            case 3 -> 1.6d;
            case 4 -> 1.2d;
            default -> 0.8d;
        };
    }

    public boolean enablesCounterAwareness() {
        return difficulty >= 4;
    }

    public boolean enablesAdvancedSwapLogic() {
        return difficulty >= 3;
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
}
