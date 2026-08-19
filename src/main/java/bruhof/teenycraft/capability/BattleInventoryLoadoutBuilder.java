package bruhof.teenycraft.capability;

import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.BattleAbilitySlot;
import bruhof.teenycraft.battle.damage.DamagePipeline;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemAccessory;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

        populateAbilitySlots(state, player, activeFigure);
        populateBenchSlots(state, player);
        populateTofuSlot(state, player);
        populateAccessorySlot(state, player);

        if (!(player instanceof ServerPlayer serverPlayer) || ModMessages.canSendToPlayer(serverPlayer)) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static void populateAbilitySlots(BattleState state, Player player, BattleFigure figure) {
        for (int i = 0; i < ABILITY_SLOT_COUNT; i++) {
            BattleAbilitySlot slot = BattleAbilitySlot.resolve(figure, i);
            if (slot == null) {
                continue;
            }

            ItemStack stack = new ItemStack(ABILITY_ITEMS[i]);
            int damage = DamagePipeline.calculateOutput(state, figure, slot.data(), slot.effectiveManaCost(), slot.golden()).baseDamagePerHit;
            bruhof.teenycraft.item.custom.battle.ItemAbility.initializeAbility(
                    stack,
                    slot.effectiveAbilityId(),
                    slot.data().name,
                    damage,
                    slot.golden(),
                    slot.iconVariant()
            );
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
