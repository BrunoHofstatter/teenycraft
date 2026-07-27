package bruhof.teenycraft.networking;

import bruhof.teenycraft.capability.TitanManagerStorageSection;
import bruhof.teenycraft.capability.TitanManagerStorageSlot;
import bruhof.teenycraft.screen.TitanManagerMenu;
import bruhof.teenycraft.screen.GroupComboEffectOption;
import bruhof.teenycraft.screen.GroupComboOption;
import bruhof.teenycraft.screen.TitanManagerSortMode;
import bruhof.teenycraft.screen.TitanManagerTab;
import bruhof.teenycraft.screen.TitanManagerViewState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncTitanManagerView {
    private final int containerId;
    private final TitanManagerTab activeTab;
    private final TitanManagerSortMode sortMode;
    private final boolean favoritesOnly;
    private final String searchQuery;
    private final String figureClassFilter;
    private final int pageIndex;
    private final int totalResults;
    private final int pageCount;
    private final List<TitanManagerStorageSlot> visibleSlots;
    private final List<GroupComboOption> comboOptions;
    private final String effectiveComboGroupId;
    private final boolean comboAutomatic;
    private final int leadTeamSlot;

    public PacketSyncTitanManagerView(int containerId,
                                      TitanManagerTab activeTab,
                                      TitanManagerSortMode sortMode,
                                      boolean favoritesOnly,
                                      String searchQuery,
                                      String figureClassFilter,
                                      int pageIndex,
                                      int totalResults,
                                      int pageCount,
                                      List<TitanManagerStorageSlot> visibleSlots,
                                      List<GroupComboOption> comboOptions,
                                      String effectiveComboGroupId,
                                      boolean comboAutomatic,
                                      int leadTeamSlot) {
        this.containerId = containerId;
        this.activeTab = activeTab;
        this.sortMode = sortMode;
        this.favoritesOnly = favoritesOnly;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        this.figureClassFilter = figureClassFilter == null ? "" : figureClassFilter;
        this.pageIndex = pageIndex;
        this.totalResults = totalResults;
        this.pageCount = pageCount;
        this.visibleSlots = visibleSlots;
        this.comboOptions = comboOptions;
        this.effectiveComboGroupId = effectiveComboGroupId;
        this.comboAutomatic = comboAutomatic;
        this.leadTeamSlot = leadTeamSlot;
    }

    public static PacketSyncTitanManagerView fromMenu(int containerId, TitanManagerViewState viewState, TitanManagerMenu menu) {
        return new PacketSyncTitanManagerView(
                containerId,
                viewState.getActiveTab(),
                viewState.getSortMode(),
                viewState.isFavoritesOnly(),
                viewState.getSearchQuery(),
                viewState.getFigureClassFilter(),
                viewState.getPageIndex(),
                viewState.getTotalResults(),
                viewState.getPageCount(),
                new ArrayList<>(viewState.getVisibleSlots()),
                new ArrayList<>(menu.getComboOptions()),
                menu.getEffectiveComboGroupId(),
                menu.isComboAutomatic(),
                menu.getLeadTeamSlot()
        );
    }

    public PacketSyncTitanManagerView(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.activeTab = buf.readEnum(TitanManagerTab.class);
        this.sortMode = buf.readEnum(TitanManagerSortMode.class);
        this.favoritesOnly = buf.readBoolean();
        this.searchQuery = buf.readUtf();
        this.figureClassFilter = buf.readUtf();
        this.pageIndex = buf.readVarInt();
        this.totalResults = buf.readVarInt();
        this.pageCount = buf.readVarInt();
        int size = buf.readVarInt();
        this.visibleSlots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TitanManagerStorageSection section = buf.readEnum(TitanManagerStorageSection.class);
            int slot = buf.readVarInt();
            visibleSlots.add(new TitanManagerStorageSlot(section, slot));
        }
        int comboSize = buf.readVarInt();
        this.comboOptions = new ArrayList<>(comboSize);
        for (int i = 0; i < comboSize; i++) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            int priority = buf.readVarInt();
            int effectCount = buf.readVarInt();
            List<GroupComboEffectOption> effects = new ArrayList<>(effectCount);
            for (int j = 0; j < effectCount; j++) {
                effects.add(new GroupComboEffectOption(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf()));
            }
            comboOptions.add(new GroupComboOption(id, name, priority, effects));
        }
        this.effectiveComboGroupId = buf.readUtf();
        this.comboAutomatic = buf.readBoolean();
        this.leadTeamSlot = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeEnum(activeTab);
        buf.writeEnum(sortMode);
        buf.writeBoolean(favoritesOnly);
        buf.writeUtf(searchQuery);
        buf.writeUtf(figureClassFilter);
        buf.writeVarInt(pageIndex);
        buf.writeVarInt(totalResults);
        buf.writeVarInt(pageCount);
        buf.writeVarInt(visibleSlots.size());
        for (TitanManagerStorageSlot slot : visibleSlots) {
            buf.writeEnum(slot.section());
            buf.writeVarInt(slot.slot());
        }
        buf.writeVarInt(comboOptions.size());
        for (GroupComboOption option : comboOptions) {
            buf.writeUtf(option.id());
            buf.writeUtf(option.name());
            buf.writeVarInt(option.priority());
            buf.writeVarInt(option.effects().size());
            for (GroupComboEffectOption effect : option.effects()) {
                buf.writeUtf(effect.id());
                buf.writeUtf(effect.label());
                buf.writeUtf(effect.description());
                buf.writeUtf(effect.iconId());
            }
        }
        buf.writeUtf(effectiveComboGroupId);
        buf.writeBoolean(comboAutomatic);
        buf.writeVarInt(leadTeamSlot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof TitanManagerMenu menu)) {
                return;
            }
            if (menu.containerId != containerId) {
                return;
            }

            menu.applySyncedViewState(activeTab, sortMode, favoritesOnly, searchQuery, figureClassFilter,
                    pageIndex, totalResults, pageCount, visibleSlots);
            menu.applySyncedComboState(comboOptions, effectiveComboGroupId, comboAutomatic, leadTeamSlot);
        });
        return true;
    }
}
