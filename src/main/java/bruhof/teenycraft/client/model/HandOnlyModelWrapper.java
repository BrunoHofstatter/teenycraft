package bruhof.teenycraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Keeps the normal item model in GUI/world contexts while substituting a
 * presentation-only model when the item is rendered in either hand.
 */
public final class HandOnlyModelWrapper extends BakedModelWrapper<BakedModel> {
    private final BakedModel heldModel;

    public HandOnlyModelWrapper(BakedModel originalModel, BakedModel heldModel) {
        super(originalModel);
        this.heldModel = heldModel;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        if (isHandContext(context)) {
            return heldModel.applyTransform(context, poseStack, leftHand);
        }
        return originalModel.applyTransform(context, poseStack, leftHand);
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
