package bruhof.teenycraft.screen;

public enum TitanManagerSortMode {
    NAME("Name"),
    LEVEL_DESC("Lvl-"),
    LEVEL_ASC("Lvl+"),
    NEWEST("New"),
    RANK_DESC("Rnk-"),
    RANK_ASC("Rnk+");

    private final String shortLabel;

    TitanManagerSortMode(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public static TitanManagerSortMode getDefault(TitanManagerTab tab) {
        return NAME;
    }

    public boolean isSupportedBy(TitanManagerTab tab) {
        return switch (tab) {
            case FIGURES -> this == NAME || this == LEVEL_DESC || this == LEVEL_ASC || this == NEWEST;
            case CHIPS -> this == NAME || this == RANK_DESC || this == RANK_ASC;
            case ACCESSORIES -> this == NAME;
        };
    }
}
