package bruhof.teenycraft.client.model;

import bruhof.teenycraft.client.AbilityIconManager;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

public class AbilityModelWrapper extends BakedModelWrapper<BakedModel> {

    private final CustomItemOverrides overrides;

    public AbilityModelWrapper(BakedModel originalModel) {
        super(originalModel);
        this.overrides = new CustomItemOverrides(originalModel.getOverrides());
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    private static class CustomItemOverrides extends ItemOverrides {
        private final ItemOverrides original;

        public CustomItemOverrides(ItemOverrides original) {
            this.original = original;
        }

        @Override
        public BakedModel resolve(BakedModel pModel, ItemStack pStack, ClientLevel pLevel, LivingEntity pEntity, int pSeed) {
            if (pStack.hasTag() && pStack.getTag().contains(ItemAbility.TAG_ID)) {
                String id = pStack.getTag().getString(ItemAbility.TAG_ID);
                boolean isMineActive = false;
                
                // Check if this slot has an active mine button
                if (pStack.getItem() instanceof ItemAbility itemAbility) {
                    int slot = itemAbility.getSlotIndex();
                    if (bruhof.teenycraft.client.ClientBattleData.isBattling() && bruhof.teenycraft.client.ClientBattleData.hasActiveMine(slot)) {
                        isMineActive = true;
                    }
                }

                // 1. Dynamic Vanilla Fallback Logic
                if (isMineActive) {
                     // Force the "Detonate Button" look if the mine is currently placed
                     return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(net.minecraft.world.item.Items.LEVER);
                } else if (AbilityIconManager.FALLBACKS.containsKey(id)) {
                    Item fallbackItem = AbilityIconManager.FALLBACKS.get(id);
                    if (fallbackItem != null) {
                        return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(fallbackItem);
                    }
                }
            }
            
            // 2. Otherwise, use standard JSON predicates (original logic)
            return original.resolve(pModel, pStack, pLevel, pEntity, pSeed);
        }
    }
}
