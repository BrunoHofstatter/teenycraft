package bruhof.teenycraft.item.custom;

import bruhof.teenycraft.screen.TitanManagerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class ItemTitanPad extends Item {
    public ItemTitanPad(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        if (!pLevel.isClientSide()) {
            NetworkHooks.openScreen((ServerPlayer) pPlayer, new SimpleMenuProvider(
                (id, inv, player) -> new TitanManagerMenu(id, inv, inv.player.getCapability(bruhof.teenycraft.capability.TitanManagerProvider.TITAN_MANAGER).orElseThrow(IllegalStateException::new)),
                Component.translatable("container.teenycraft.titan_manager")
            ));
        }
        return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pHand), pLevel.isClientSide());
    }
}
