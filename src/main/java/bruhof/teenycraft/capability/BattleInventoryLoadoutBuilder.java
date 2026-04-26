package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class BattleInventoryLoadoutBuilder {
    private static final int ABILITY_SLOT_COUNT = 3;
    private static final int TOFU_SLOT = 4;
    private static final int BENCH_START_SLOT = 6;
    private static final int BENCH_END_SLOT = 8;
    private static final Item[] ABILITY_ITEMS = {ModItems.ABILITY_1.get(), ModItems.ABILITY_2.get(), ModItems.ABILITY_3.get()};

    private BattleInventoryLoadoutBuilder() {
    }

    static void rebuild(BattleState state, Player player) {
        player.getInventory().clearContent();

        BattleFigure activeFigure = state.getActiveFigure();
        if (activeFigure == null) {
            return;
        }

        populateAbilitySlots(player, activeFigure.getOriginalStack());
        populateBenchSlots(state, player);
        populateTofuSlot(state, player);
        populateAccessorySlot(state, player);

        if (!(player instanceof ServerPlayer serverPlayer) || ModMessages.canSendToPlayer(serverPlayer)) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static void populateAbilitySlots(Player player, ItemStack figureStack) {
        ArrayList<String> abilityOrder = ItemFigure.getAbilityOrder(figureStack);
        for (int i = 0; i < ABILITY_SLOT_COUNT; i++) {
            if (i >= abilityOrder.size()) {
                continue;
            }

            String abilityId = abilityOrder.get(i);
            var data = bruhof.teenycraft.util.AbilityLoader.getAbility(abilityId);
            if (data == null) {
                continue;
            }

            ItemStack stack = new ItemStack(ABILITY_ITEMS[i]);
            int damage = ItemFigure.calculateAbilityDamage(figureStack, i);
            boolean golden = ItemFigure.isAbilityGolden(figureStack, abilityId);
            bruhof.teenycraft.item.custom.battle.ItemAbility.initializeAbility(stack, abilityId, data.name, damage, golden);
            player.getInventory().setItem(i, stack);
        }
    }

    private static void populateBenchSlots(BattleState state, Player player) {
        List<BattleFigure> team = state.getTeam();
        int benchSlot = BENCH_START_SLOT;
        for (int i = 0; i < team.size() && benchSlot <= BENCH_END_SLOT; i++) {
            if (i == state.getActiveFigureIndex()) {
                continue;
            }

            ItemStack icon = team.get(i).getOriginalStack().copy();
            String figureId = ItemFigure.getFigureID(icon);
            icon.setTag(new CompoundTag());
            icon.getOrCreateTag().putString("FigureID", figureId);
            icon.getOrCreateTag().putInt(BattleState.TAG_BATTLE_FIGURE_INDEX, i);
            player.getInventory().setItem(benchSlot, icon);
            benchSlot++;
        }
    }

    private static void populateTofuSlot(BattleState state, Player player) {
        if (state.getCurrentTofuMana() > 0) {
            player.getInventory().setItem(TOFU_SLOT, new ItemStack(ModItems.TOFU.get()));
        }
    }

    private static void populateAccessorySlot(BattleState state, Player player) {
        ItemStack accessoryStack = state.getEquippedAccessoryStackForBattleInventory();
        if (accessoryStack.isEmpty() || !(accessoryStack.getItem() instanceof ItemAccessory)) {
            return;
        }

        ItemStack battleAccessory = accessoryStack.copy();
        battleAccessory.getOrCreateTag().putBoolean(ItemAccessory.TAG_BATTLE_ACTIVE, state.isAccessoryActive());
        player.getInventory().setItem(BattleState.BATTLE_ACCESSORY_SLOT, battleAccessory);
    }
}
