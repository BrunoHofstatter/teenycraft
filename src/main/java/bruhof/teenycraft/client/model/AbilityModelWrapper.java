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
        this.overrides = new CustomItemOverrides(originalModel);
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    private static class CustomItemOverrides extends ItemOverrides {
        private final ItemOverrides original;
        private final BakedModel baseModel;

        public CustomItemOverrides(BakedModel baseModel) {
            this.original = baseModel.getOverrides();
            this.baseModel = baseModel;
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

                if (isMineActive) {
                    if (AbilityIconManager.hasIconModel("bat_mine_button")) {
                        return original.resolve(pModel, pStack, pLevel, pEntity, pSeed);
                    }

                    // Preserve a usable detonation icon if the special texture is
                    // absent from the active resources.
                    return Minecraft.getInstance().getItemRenderer().getItemModelShaper()
                            .getItemModel(net.minecraft.world.item.Items.LEVER);
                }

                // Minecraft numeric predicates match thresholds, so resolving the
                // wrapper override table directly would let unimplemented indexes
                // inherit a nearby custom icon. Only ids discovered in the active
                // resource set are allowed to use that table.
                if (AbilityIconManager.hasIconModel(id)) {
                    return original.resolve(pModel, pStack, pLevel, pEntity, pSeed);
                }

                if (AbilityIconManager.FALLBACKS.containsKey(id)) {
                    Item fallbackItem = AbilityIconManager.FALLBACKS.get(id);
                    if (fallbackItem != null) {
                        return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(fallbackItem);
                    }
                }

                return baseModel;
            }

            // Untagged stacks can use normal item override behavior.
            return original.resolve(pModel, pStack, pLevel, pEntity, pSeed);
        }
    }
}
