package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A stable, server-validated reference to a figure that can live outside the
 * vanilla hotbar. Storage references use the absolute backing slot, never a
 * sorted/filtered Titan Manager view index.
 */
public record FigureItemLocation(Source source, int slot) {
    public enum Source {
        PLAYER_INVENTORY,
        OFF_HAND,
        TITAN_TEAM,
        TITAN_STORAGE
    }

    public FigureItemLocation {
        source = source == null ? Source.PLAYER_INVENTORY : source;
    }

    public static FigureItemLocation playerInventory(int slot) {
        return new FigureItemLocation(Source.PLAYER_INVENTORY, slot);
    }

    public static FigureItemLocation offHand() {
        return new FigureItemLocation(Source.OFF_HAND, 0);
    }

    public static FigureItemLocation titanTeam(int slot) {
        return new FigureItemLocation(Source.TITAN_TEAM, slot);
    }

    public static FigureItemLocation titanStorage(int slot) {
        return new FigureItemLocation(Source.TITAN_STORAGE, slot);
    }

    public static FigureItemLocation read(FriendlyByteBuf buffer) {
        return new FigureItemLocation(buffer.readEnum(Source.class), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(source);
        buffer.writeVarInt(slot);
    }

    public ItemStack resolve(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        return switch (source) {
            case PLAYER_INVENTORY -> slot >= 0 && slot < player.getInventory().items.size()
                    ? player.getInventory().getItem(slot)
                    : ItemStack.EMPTY;
            case OFF_HAND -> player.getOffhandItem();
            case TITAN_TEAM -> player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                    .map(manager -> slot >= 0 && slot < ITitanManager.TEAM_SIZE
                            ? manager.getTeamStack(slot)
                            : ItemStack.EMPTY)
                    .orElse(ItemStack.EMPTY);
            case TITAN_STORAGE -> player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                    .map(manager -> slot >= 0 && slot < manager.getFigureStorage().getSlots()
                            ? manager.getFigureStorage().getStackInSlot(slot)
                            : ItemStack.EMPTY)
                    .orElse(ItemStack.EMPTY);
        };
    }
}
