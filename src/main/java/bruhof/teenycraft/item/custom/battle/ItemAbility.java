package bruhof.teenycraft.item.custom.battle;

import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.battle.BattleFigure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.UseAnim;

public class ItemAbility extends Item {
    private final int slotIndex; // 0, 1, 2

    public static final String TAG_ID = "AbilityID";
    public static final String TAG_NAME = "AbilityName";
    public static final String TAG_DAMAGE = "Damage";
    public static final String TAG_GOLDEN = "IsGolden";

    public ItemAbility(Properties pProperties, int slotIndex) {
        super(pProperties.stacksTo(1));
        this.slotIndex = slotIndex;
    }

    public static void initializeAbility(ItemStack stack, String id, String name, int damage, boolean golden) {
        var tag = stack.getOrCreateTag();
        tag.putString(TAG_ID, id);
        tag.putString(TAG_NAME, name);
        tag.putInt(TAG_DAMAGE, damage);
        tag.putBoolean(TAG_GOLDEN, golden);
    }

    @Override
    public Component getName(ItemStack pStack) {
        if (!pStack.hasTag()) return super.getName(pStack);
        
        var tag = pStack.getTag();
        String name = tag.getString(TAG_NAME);
        int damage = tag.getInt(TAG_DAMAGE);
        boolean golden = tag.getBoolean(TAG_GOLDEN);
        
        String color = golden ? "§6" : "§9";
        String displayName = color + name;
        if (damage > 0) {
            displayName += " - " + damage;
        }
        
        return Component.literal(displayName);
    }
    
    public int getSlotIndex() { return slotIndex; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            pPlayer.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
                if (battle.isBattling()) {
                    BattleFigure active = battle.getActiveFigure();
                    if (active != null) {
                        java.util.ArrayList<String> order = ItemFigure.getAbilityOrder(active.getOriginalStack());
                        if (slotIndex < order.size()) {
                            AbilityLoader.AbilityData data = AbilityLoader.getAbility(order.get(slotIndex));
                            
                            // Melee Check (Don't execute on Right-Click)
                            if (data != null && "melee".equalsIgnoreCase(data.hitType)) {
                                pPlayer.sendSystemMessage(Component.literal("§eLeft-Click to use Melee Attacks!"));
                                return;
                            }
                            
                            // Execute Logic (Ranged / Buffs)
                            AbilityExecutor.executeAction(serverPlayer, active, slotIndex);
                        }
                    }
                } else {
                    pPlayer.sendSystemMessage(Component.literal("§cYou are not in a battle!"));
                }
            });
        }
        
        // Client-side Check to prevent swing for Melee
        if (pLevel.isClientSide) {
             // We need to fetch data to know if it's melee to return PASS
             // But we don't have easy access to AbilityData here without NBT or Capability (which might be empty on client)
             // However, server side logic handles the "Don't Execute".
             // To stop swing, we need to know NOW.
             // Let's read the ID from NBT (ItemAbility has it)
             String id = pPlayer.getItemInHand(pUsedHand).getTag().getString(TAG_ID);
             AbilityLoader.AbilityData data = AbilityLoader.getAbility(id);
             if (data != null && "melee".equalsIgnoreCase(data.hitType)) {
                 return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
             }
        }
        
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pUsedHand));
    }
}
