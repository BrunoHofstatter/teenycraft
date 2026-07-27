package bruhof.teenycraft.networking;

import bruhof.teenycraft.golden.GoldenSacrificeCalculator;
import bruhof.teenycraft.screen.SilkieStationAbilityView;
import bruhof.teenycraft.screen.SilkieStationFigureView;
import bruhof.teenycraft.screen.SilkieStationGroupView;
import bruhof.teenycraft.screen.SilkieStationMenu;
import bruhof.teenycraft.screen.SilkieStationSnapshot;
import bruhof.teenycraft.screen.SilkieStationSortMode;
import bruhof.teenycraft.screen.SilkieStationSourceType;
import bruhof.teenycraft.screen.SilkieStationTargetRef;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncSilkieStation {
    private final int containerId;
    private final SilkieStationSnapshot snapshot;

    public PacketSyncSilkieStation(int containerId, SilkieStationSnapshot snapshot) {
        this.containerId = containerId;
        this.snapshot = snapshot;
    }

    public PacketSyncSilkieStation(FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        SilkieStationTargetRef targetRef = readRef(buf);
        ItemStack targetStack = buf.readItem();
        List<SilkieStationFigureView> active = readViews(buf);
        List<SilkieStationFigureView> favorites = readViews(buf);
        int favoritePage = buf.readVarInt();
        int favoritePageCount = buf.readVarInt();
        List<SilkieStationFigureView> offerings = readViews(buf);
        List<SilkieStationFigureView> candidates = readViews(buf);
        int browserPage = buf.readVarInt();
        int browserPageCount = buf.readVarInt();
        int browserTotal = buf.readVarInt();
        SilkieStationSortMode sortMode = buf.readEnum(SilkieStationSortMode.class);
        String searchQuery = buf.readUtf(32);
        List<SilkieStationAbilityView> abilities = readAbilities(buf);
        int selectedAbility = buf.readVarInt();
        int basePoints = buf.readVarInt();
        int exactCount = buf.readVarInt();
        int exactBonus = buf.readVarInt();
        int classComboCount = buf.readVarInt();
        int classBonus = buf.readVarInt();
        List<SilkieStationGroupView> groups = readGroups(buf);
        int totalPoints = buf.readVarInt();
        int appliedPoints = buf.readVarInt();
        int overflowPoints = buf.readVarInt();
        boolean invested = buf.readBoolean();
        boolean canFeed = buf.readBoolean();
        String status = buf.readUtf(256);
        snapshot = new SilkieStationSnapshot(targetRef, targetStack, active, favorites, favoritePage,
                favoritePageCount, offerings, candidates, browserPage, browserPageCount, browserTotal,
                sortMode, searchQuery, abilities, selectedAbility, basePoints, exactCount, exactBonus,
                classComboCount, classBonus, groups, totalPoints, appliedPoints, overflowPoints,
                invested, canFeed, status);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        writeRef(buf, snapshot.targetRef());
        buf.writeItem(snapshot.targetStack());
        writeViews(buf, snapshot.activeTargets());
        writeViews(buf, snapshot.favoriteTargets());
        buf.writeVarInt(snapshot.favoritePage());
        buf.writeVarInt(snapshot.favoritePageCount());
        writeViews(buf, snapshot.offerings());
        writeViews(buf, snapshot.candidates());
        buf.writeVarInt(snapshot.browserPage());
        buf.writeVarInt(snapshot.browserPageCount());
        buf.writeVarInt(snapshot.browserTotal());
        buf.writeEnum(snapshot.sortMode());
        buf.writeUtf(snapshot.searchQuery(), 32);
        writeAbilities(buf, snapshot.abilities());
        buf.writeVarInt(snapshot.selectedAbility());
        buf.writeVarInt(snapshot.basePoints());
        buf.writeVarInt(snapshot.exactCount());
        buf.writeVarInt(snapshot.exactBonus());
        buf.writeVarInt(snapshot.classComboCount());
        buf.writeVarInt(snapshot.classBonus());
        writeGroups(buf, snapshot.groups());
        buf.writeVarInt(snapshot.totalPoints());
        buf.writeVarInt(snapshot.appliedPoints());
        buf.writeVarInt(snapshot.overflowPoints());
        buf.writeBoolean(snapshot.hasInvestedSacrifice());
        buf.writeBoolean(snapshot.canFeed());
        buf.writeUtf(snapshot.statusMessage(), 256);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null
                    || !(Minecraft.getInstance().player.containerMenu instanceof SilkieStationMenu menu)
                    || menu.containerId != containerId) {
                return;
            }
            menu.applySnapshot(snapshot);
        });
        return true;
    }

    private static void writeViews(FriendlyByteBuf buf, List<SilkieStationFigureView> views) {
        buf.writeVarInt(views.size());
        for (SilkieStationFigureView view : views) {
            writeRef(buf, view.ref());
            buf.writeItem(view.stack());
            buf.writeEnum(view.affinity());
            buf.writeVarInt(view.basePoints());
            buf.writeUtf(view.reason(), 128);
            buf.writeBoolean(view.invested());
        }
    }

    private static List<SilkieStationFigureView> readViews(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SilkieStationFigureView> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            SilkieStationTargetRef ref = readRef(buf);
            ItemStack stack = buf.readItem();
            GoldenSacrificeCalculator.Affinity affinity = buf.readEnum(GoldenSacrificeCalculator.Affinity.class);
            int points = buf.readVarInt();
            String reason = buf.readUtf(128);
            boolean invested = buf.readBoolean();
            result.add(new SilkieStationFigureView(ref, stack, affinity, points, reason, invested));
        }
        return result;
    }

    private static void writeAbilities(FriendlyByteBuf buf, List<SilkieStationAbilityView> abilities) {
        buf.writeVarInt(abilities.size());
        for (SilkieStationAbilityView ability : abilities) {
            buf.writeUtf(ability.abilityId(), 128);
            buf.writeUtf(ability.name(), 128);
            buf.writeVarInt(ability.points());
            buf.writeVarInt(ability.requirement());
            buf.writeBoolean(ability.golden());
        }
    }

    private static List<SilkieStationAbilityView> readAbilities(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SilkieStationAbilityView> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(new SilkieStationAbilityView(buf.readUtf(128), buf.readUtf(128),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
        return result;
    }

    private static void writeGroups(FriendlyByteBuf buf, List<SilkieStationGroupView> groups) {
        buf.writeVarInt(groups.size());
        for (SilkieStationGroupView group : groups) {
            buf.writeUtf(group.groupId(), 128);
            buf.writeUtf(group.groupName(), 128);
            buf.writeVarInt(group.representedSacrifices());
            buf.writeVarInt(group.memberCount());
            buf.writeBoolean(group.complete());
            buf.writeVarInt(group.bonus());
            buf.writeBoolean(group.awarded());
        }
    }

    private static List<SilkieStationGroupView> readGroups(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SilkieStationGroupView> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(new SilkieStationGroupView(buf.readUtf(128), buf.readUtf(128),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean()));
        }
        return result;
    }

    private static void writeRef(FriendlyByteBuf buf, SilkieStationTargetRef ref) {
        buf.writeBoolean(ref != null);
        if (ref != null) {
            buf.writeEnum(ref.sourceType());
            buf.writeVarInt(ref.slot());
        }
    }

    private static SilkieStationTargetRef readRef(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new SilkieStationTargetRef(buf.readEnum(SilkieStationSourceType.class), buf.readVarInt());
    }
}
