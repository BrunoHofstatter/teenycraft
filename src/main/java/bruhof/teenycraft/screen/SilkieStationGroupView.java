package bruhof.teenycraft.screen;

public record SilkieStationGroupView(
        String groupId,
        String groupName,
        int representedSacrifices,
        int memberCount,
        boolean complete,
        int bonus,
        boolean awarded
) {
}
