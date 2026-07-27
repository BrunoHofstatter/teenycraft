package bruhof.teenycraft.networking;

import bruhof.teenycraft.screen.SilkieStationMenu;
import bruhof.teenycraft.screen.SilkieStationSortMode;
import bruhof.teenycraft.screen.SilkieStationSourceType;
import bruhof.teenycraft.screen.SilkieStationTargetRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSilkieStationAction {
    public enum ActionType {
        SET_TARGET,
        CLEAR_TARGET,
        TOGGLE_SACRIFICE,
        SET_SORT,
        SET_SEARCH,
        SET_BROWSER_PAGE,
        SET_FAVORITE_PAGE,
        FEED
    }

    private final int containerId;
    private final ActionType actionType;
    private final int intValue;
    private final String stringValue;
    private final SilkieStationTargetRef targetRef;

    private PacketSilkieStationAction(int containerId, ActionType actionType, int intValue,
                                      String stringValue, SilkieStationTargetRef targetRef) {
        this.containerId = containerId;
        this.actionType = actionType;
        this.intValue = intValue;
        this.stringValue = stringValue == null ? "" : stringValue;
        this.targetRef = targetRef;
    }

    public static PacketSilkieStationAction setTarget(int containerId, SilkieStationTargetRef ref) {
        return new PacketSilkieStationAction(containerId, ActionType.SET_TARGET, 0, "", ref);
    }

    public static PacketSilkieStationAction clearTarget(int containerId) {
        return simple(containerId, ActionType.CLEAR_TARGET, 0, "");
    }

    public static PacketSilkieStationAction toggleSacrifice(int containerId, int storageSlot) {
        return simple(containerId, ActionType.TOGGLE_SACRIFICE, storageSlot, "");
    }

    public static PacketSilkieStationAction setSort(int containerId, SilkieStationSortMode sortMode) {
        return simple(containerId, ActionType.SET_SORT, sortMode.ordinal(), "");
    }

    public static PacketSilkieStationAction setSearch(int containerId, String query) {
        return simple(containerId, ActionType.SET_SEARCH, 0, query);
    }

    public static PacketSilkieStationAction setBrowserPage(int containerId, int page) {
        return simple(containerId, ActionType.SET_BROWSER_PAGE, page, "");
    }

    public static PacketSilkieStationAction setFavoritePage(int containerId, int page) {
        return simple(containerId, ActionType.SET_FAVORITE_PAGE, page, "");
    }

    public static PacketSilkieStationAction feed(int containerId) {
        return simple(containerId, ActionType.FEED, 0, "");
    }

    private static PacketSilkieStationAction simple(int containerId, ActionType type, int value, String text) {
        return new PacketSilkieStationAction(containerId, type, value, text, null);
    }

    public PacketSilkieStationAction(FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        actionType = buf.readEnum(ActionType.class);
        intValue = buf.readVarInt();
        stringValue = buf.readUtf(32);
        targetRef = buf.readBoolean()
                ? new SilkieStationTargetRef(buf.readEnum(SilkieStationSourceType.class), buf.readVarInt())
                : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeEnum(actionType);
        buf.writeVarInt(intValue);
        buf.writeUtf(stringValue, 32);
        buf.writeBoolean(targetRef != null);
        if (targetRef != null) {
            buf.writeEnum(targetRef.sourceType());
            buf.writeVarInt(targetRef.slot());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof SilkieStationMenu menu)
                    || menu.containerId != containerId) {
                return;
            }
            switch (actionType) {
                case SET_TARGET -> menu.selectTarget(targetRef);
                case CLEAR_TARGET -> menu.clearTarget();
                case TOGGLE_SACRIFICE -> menu.toggleSacrifice(intValue);
                case SET_SORT -> {
                    if (intValue >= 0 && intValue < SilkieStationSortMode.values().length) {
                        menu.setSortMode(SilkieStationSortMode.values()[intValue]);
                    }
                }
                case SET_SEARCH -> menu.setSearchQuery(stringValue);
                case SET_BROWSER_PAGE -> menu.setBrowserPage(intValue);
                case SET_FAVORITE_PAGE -> menu.setFavoritePage(intValue);
                case FEED -> menu.feed();
            }
            menu.broadcastChanges();
        });
        return true;
    }
}
