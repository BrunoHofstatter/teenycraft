package bruhof.teenycraft.item.custom;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.capability.BattleStateProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemAccessory extends Item {
    public static final String TAG_BATTLE_ACTIVE = "BattleAccessoryActive";

    private final String accessoryId;

    public ItemAccessory(Properties properties, String accessoryId) {
        super(properties.stacksTo(1));
        this.accessoryId = accessoryId;
    }

    public String getAccessoryId() {
        return accessoryId;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_BATTLE_ACTIVE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
                if (!state.isBattling()) {
                    return;
                }
                if (state.isAccessoryActive()) {
                    serverPlayer.sendSystemMessage(Component.literal("§eAccessory already active."));
                    return;
                }
                if (state.getBatteryCharge() < TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE) {
                    serverPlayer.sendSystemMessage(Component.literal("§cAccessory needs at least " + Math.round(TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE) + " battery."));
                    return;
                }
                if (!state.tryActivateAccessory()) {
                    serverPlayer.sendSystemMessage(Component.literal("§cNo accessory equipped."));
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
