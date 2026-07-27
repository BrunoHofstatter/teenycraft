package bruhof.teenycraft.screen;

public enum SilkieStationSortMode {
    RECOMMENDED("Recommended"),
    CLASS("Class"),
    LEVEL("Level");

    private final String label;

    SilkieStationSortMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
