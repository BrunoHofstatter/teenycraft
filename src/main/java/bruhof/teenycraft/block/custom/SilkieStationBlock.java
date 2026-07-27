package bruhof.teenycraft.block.custom;

import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.screen.SilkieStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class SilkieStationBlock extends Block {
    public SilkieStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean battling = player.getCapability(BattleStateProvider.BATTLE_STATE)
                .map(battle -> battle.isBattling()).orElse(false);
        if (battling) {
            player.displayClientMessage(Component.literal("Silkie Station cannot be used during battle."), true);
            return InteractionResult.CONSUME;
        }

        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(manager ->
                NetworkHooks.openScreen(serverPlayer,
                        new SimpleMenuProvider(
                                (containerId, inventory, menuPlayer) ->
                                        new SilkieStationMenu(containerId, inventory, pos, manager),
                                Component.translatable("container.teenycraft.silkie_station")),
                        buffer -> buffer.writeBlockPos(pos)));
        return InteractionResult.CONSUME;
    }
}
