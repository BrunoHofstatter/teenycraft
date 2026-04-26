package bruhof.teenycraft.world.arena;

public enum ArenaSelectionMode {
    CYCLE,
    RANDOM;

    public static ArenaSelectionMode fromSerialized(String value) {
        if (value == null) {
            return CYCLE;
        }

        return switch (value.toLowerCase()) {
            case "cycle" -> CYCLE;
            case "random" -> RANDOM;
            default -> throw new IllegalArgumentException("Unknown arena selection mode: " + value);
        };
    }
}
