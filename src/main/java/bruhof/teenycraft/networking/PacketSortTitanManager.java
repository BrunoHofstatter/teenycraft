package bruhof.teenycraft.networking;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.screen.TitanManagerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class PacketSortTitanManager {
    public enum SortType {
        ALPHABETICAL,
        LEVEL_DESC,
        LEVEL_ASC
    }

    private final SortType sortType;

    public PacketSortTitanManager(SortType sortType) {
        this.sortType = sortType;
    }

    public PacketSortTitanManager(FriendlyByteBuf buf) {
        this.sortType = SortType.values()[buf.readInt()];
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.sortType.ordinal());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(titanManager -> {
                ItemStackHandler inv = (ItemStackHandler) titanManager.getInventory();
                
                // 1. Extract all items from Storage (Indices 4 to 975)
                List<ItemStack> figures = new ArrayList<>();
                for (int i = 0; i < TitanManager.TOTAL_STORAGE_SLOTS; i++) {
                    int slotIndex = TitanManager.STORAGE_START + i;
                    ItemStack stack = inv.getStackInSlot(slotIndex);
                    if (!stack.isEmpty()) {
                        figures.add(stack.copy());
                        inv.setStackInSlot(slotIndex, ItemStack.EMPTY); // Remove from slot
                    }
                }

                // 2. Sort
                figures.sort(getComparator(sortType));

                // 3. Re-insert
                for (int i = 0; i < figures.size(); i++) {
                    int slotIndex = TitanManager.STORAGE_START + i;
                    inv.setStackInSlot(slotIndex, figures.get(i));
                }
                
                // 4. Update Client (Using the sync packet we already have)
                // Or simply notify the menu to broadcast changes if the player has it open
                if (player.containerMenu instanceof TitanManagerMenu menu) {
                    menu.broadcastChanges();
                    // We might need a full sync packet if broadcastChanges handles only visible slots
                    // But for sorting, the visible slots WILL change, so this is usually enough.
                    // To be safe, we force a sync.
                    net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                    titanManager.saveNBTData(nbt);
                    ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
                }
            });
        });
        return true;
    }

    private Comparator<ItemStack> getComparator(SortType type) {
        return (s1, s2) -> {
            switch (type) {
                case ALPHABETICAL:
                    return s1.getHoverName().getString().compareToIgnoreCase(s2.getHoverName().getString());
                case LEVEL_DESC:
                    // High -> Low
                    return Integer.compare(ItemFigure.getLevel(s2), ItemFigure.getLevel(s1));
                case LEVEL_ASC:
                    // Low -> High
                    return Integer.compare(ItemFigure.getLevel(s1), ItemFigure.getLevel(s2));
                default:
                    return 0;
            }
        };
    }
}
