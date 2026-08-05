package bruhof.teenycraft.screen;

import bruhof.teenycraft.accessory.AccessoryMasteryService;
import bruhof.teenycraft.accessory.AccessoryMilestoneRegistry;
import bruhof.teenycraft.accessory.AccessoryRegistry;
import bruhof.teenycraft.capability.AccessoryMasteryProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public class AccessoryScreenMenu extends AbstractContainerMenu {
    public static final int BUTTON_PURCHASE_NEXT_TIER = 0;
    public static final int BUTTON_BACK_TO_MANAGER = 1;

    private final String accessoryId;
    private final TitanManagerReturnState returnState;

    public AccessoryScreenMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId,
                inventory,
                extraData.readUtf(),
                TitanManagerReturnState.readOptional(extraData));
    }

    public AccessoryScreenMenu(int containerId, Inventory inventory, String accessoryId) {
        this(containerId, inventory, accessoryId, null);
    }

    private AccessoryScreenMenu(int containerId,
                                Inventory inventory,
                                String accessoryId,
                                TitanManagerReturnState returnState) {
        super(ModMenuTypes.ACCESSORY_SCREEN_MENU.get(), containerId);
        this.accessoryId = accessoryId;
        this.returnState = returnState;
    }

    public String getAccessoryId() {
        return accessoryId;
    }

    public boolean canReturnToTitanManager() {
        return returnState != null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_BACK_TO_MANAGER && returnState != null && player instanceof ServerPlayer serverPlayer) {
            TitanManagerMenu.open(serverPlayer, returnState);
            return true;
        }

        if (id != BUTTON_PURCHASE_NEXT_TIER || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        boolean milestoneRegistered = serverPlayer.getCapability(AccessoryMasteryProvider.ACCESSORY_MASTERY)
                .map(mastery -> AccessoryMilestoneRegistry.get(mastery.getCurrentMilestoneId(accessoryId)) != null)
                .orElse(false);
        if (!milestoneRegistered) {
            serverPlayer.sendSystemMessage(Component.literal("This accessory milestone is not available yet."));
            return false;
        }

        AccessoryMasteryService.PurchaseResult result =
                AccessoryMasteryService.purchaseNextTier(serverPlayer, accessoryId);
        if (result != AccessoryMasteryService.PurchaseResult.SUCCESS) {
            serverPlayer.sendSystemMessage(purchaseFailure(result));
            return false;
        }

        serverPlayer.sendSystemMessage(Component.literal("Accessory upgraded."));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && AccessoryRegistry.get(accessoryId) != null;
    }

    public static void open(ServerPlayer player, String accessoryId) {
        open(player, accessoryId, null);
    }

    public static void open(ServerPlayer player,
                            String accessoryId,
                            TitanManagerReturnState returnState) {
        if (AccessoryRegistry.get(accessoryId) == null) {
            return;
        }

        AccessoryMasteryService.syncMastery(player);
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) ->
                                new AccessoryScreenMenu(containerId, inventory, accessoryId, returnState),
                        Component.translatable("item.teenycraft.accessory_" + accessoryId)
                ),
                buffer -> {
                    buffer.writeUtf(accessoryId);
                    TitanManagerReturnState.writeOptional(buffer, returnState);
                });
    }

    private static Component purchaseFailure(AccessoryMasteryService.PurchaseResult result) {
        return switch (result) {
            case UNKNOWN_ACCESSORY -> Component.literal("Unknown accessory.");
            case MAX_TIER -> Component.literal("This accessory is already at Tier 5.");
            case MILESTONE_INCOMPLETE -> Component.literal("Complete the current milestone first.");
            case NOT_ENOUGH_COINS -> Component.literal("Not enough Teeny Coins.");
            case MASTERY_UPGRADE_UNAVAILABLE -> Component.literal("This Tier 5 mastery upgrade is not implemented yet.");
            case MISSING_CAPABILITY -> Component.literal("Accessory progression is unavailable.");
            case SUCCESS -> Component.empty();
        };
    }
}
