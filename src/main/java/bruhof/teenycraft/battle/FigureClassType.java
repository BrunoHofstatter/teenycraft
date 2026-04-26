package bruhof.teenycraft.battle;

import java.util.Locale;

public enum FigureClassType {
    NONE("None"),
    CUTE("Cute"),
    DARK_ARTS("Dark Arts"),
    SUPER("Super"),
    TECH("Tech"),
    MARTIAL_ARTS("Martial Arts"),
    BEAST("Beast");

    private final String displayName;

    FigureClassType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasAdvantageOver(FigureClassType other) {
        return switch (this) {
            case CUTE -> other == DARK_ARTS;
            case DARK_ARTS -> other == SUPER;
            case SUPER -> other == TECH;
            case TECH -> other == MARTIAL_ARTS;
            case MARTIAL_ARTS -> other == BEAST;
            case BEAST -> other == CUTE;
            case NONE -> false;
        };
    }

    public static FigureClassType fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
        normalized = normalized.replaceAll("\\s+", " ");

        return switch (normalized) {
            case "cute" -> CUTE;
            case "dark arts" -> DARK_ARTS;
            case "super" -> SUPER;
            case "tech" -> TECH;
            case "martial arts" -> MARTIAL_ARTS;
            case "beast" -> BEAST;
            case "none" -> NONE;
            default -> NONE;
        };
    }
}
