package bruhof.teenycraft.world.arena;

public enum ArenaPickupType {
    HEAL,
    MANA,
    AMP,
    SPEED,
    LAUNCH,
    WALL;

    public static ArenaPickupType fromSerialized(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Arena pickup type cannot be null.");
        }

        return switch (value.toLowerCase()) {
            case "heal" -> HEAL;
            case "mana" -> MANA;
            case "amp" -> AMP;
            case "speed" -> SPEED;
            case "launch" -> LAUNCH;
            case "wall" -> WALL;
            default -> throw new IllegalArgumentException("Unknown arena pickup type: " + value);
        };
    }
}
