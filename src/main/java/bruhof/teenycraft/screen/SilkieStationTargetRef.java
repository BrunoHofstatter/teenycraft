package bruhof.teenycraft.screen;

public record SilkieStationTargetRef(SilkieStationSourceType sourceType, int slot) {
    public SilkieStationTargetRef {
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType cannot be null");
        }
    }
}
