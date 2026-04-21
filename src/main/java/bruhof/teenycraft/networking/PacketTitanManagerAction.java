package bruhof.teenycraft.networking;

import bruhof.teenycraft.screen.TitanManagerMenu;
import bruhof.teenycraft.screen.TitanManagerSortMode;
import bruhof.teenycraft.screen.TitanManagerTab;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTitanManagerAction {
    public enum ActionType {
        SET_TAB,
        SET_SORT,
        SET_SEARCH,
        SET_PAGE,
        TOGGLE_FAVORITES_ONLY,
        CYCLE_FIGURE_CLASS,
        TOGGLE_FAVORITE
    }

    private final int containerId;
    private final ActionType actionType;
    private final int intValue;
    private final String stringValue;

    public PacketTitanManagerAction(int containerId, ActionType actionType, int intValue, String stringValue) {
        this.containerId = containerId;
        this.actionType = actionType;
        this.intValue = intValue;
        this.stringValue = stringValue == null ? "" : stringValue;
    }

    public static PacketTitanManagerAction setTab(int containerId, TitanManagerTab tab) {
        return new PacketTitanManagerAction(containerId, ActionType.SET_TAB, tab.ordinal(), "");
    }

    public static PacketTitanManagerAction setSort(int containerId, TitanManagerSortMode sortMode) {
        return new PacketTitanManagerAction(containerId, ActionType.SET_SORT, sortMode.ordinal(), "");
    }

    public static PacketTitanManagerAction setSearch(int containerId, String query) {
        return new PacketTitanManagerAction(containerId, ActionType.SET_SEARCH, 0, query);
    }

    public static PacketTitanManagerAction setPage(int containerId, int pageIndex) {
        return new PacketTitanManagerAction(containerId, ActionType.SET_PAGE, pageIndex, "");
    }

    public static PacketTitanManagerAction toggleFavoritesOnly(int containerId) {
        return new PacketTitanManagerAction(containerId, ActionType.TOGGLE_FAVORITES_ONLY, 0, "");
    }

    public static PacketTitanManagerAction cycleFigureClass(int containerId) {
        return new PacketTitanManagerAction(containerId, ActionType.CYCLE_FIGURE_CLASS, 0, "");
    }

    public static PacketTitanManagerAction toggleFavorite(int containerId, int viewSlotIndex) {
        return new PacketTitanManagerAction(containerId, ActionType.TOGGLE_FAVORITE, viewSlotIndex, "");
    }

    public PacketTitanManagerAction(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.actionType = buf.readEnum(ActionType.class);
        this.intValue = buf.readVarInt();
        this.stringValue = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeEnum(actionType);
        buf.writeVarInt(intValue);
        buf.writeUtf(stringValue);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof TitanManagerMenu menu) || menu.containerId != containerId) {
                return;
            }

            switch (actionType) {
                case SET_TAB -> {
                    if (intValue >= 0 && intValue < TitanManagerTab.values().length) {
                        menu.setActiveTab(TitanManagerTab.values()[intValue]);
                    }
                }
                case SET_SORT -> {
                    if (intValue >= 0 && intValue < TitanManagerSortMode.values().length) {
                        menu.setSortMode(TitanManagerSortMode.values()[intValue]);
                    }
                }
                case SET_SEARCH -> menu.setSearchQuery(stringValue);
                case SET_PAGE -> menu.setPageIndex(intValue);
                case TOGGLE_FAVORITES_ONLY -> menu.toggleFavoritesOnly();
                case CYCLE_FIGURE_CLASS -> menu.cycleFigureClassFilter();
                case TOGGLE_FAVORITE -> menu.toggleFavorite(intValue);
            }
            menu.broadcastChanges();
        });
        return true;
    }
}
