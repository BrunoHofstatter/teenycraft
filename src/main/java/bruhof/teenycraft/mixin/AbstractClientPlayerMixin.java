package bruhof.teenycraft.mixin;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.client.ClientBattleData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void teenycraft$getSkinTextureLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientBattleData.isBattling() && player.getUUID().equals(Minecraft.getInstance().getUser().getProfileId())) {
            String figureId = ClientBattleData.getActiveFigureId();
            if (figureId != null && !figureId.equals("none")) {
                cir.setReturnValue(new ResourceLocation(TeenyCraft.MOD_ID, "textures/entity/figure/" + figureId + ".png"));
            }
        }
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void teenycraft$getModelName(CallbackInfoReturnable<String> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientBattleData.isBattling() && player.getUUID().equals(Minecraft.getInstance().getUser().getProfileId())) {
            String modelType = ClientBattleData.getActiveFigureModelType();
            if (modelType != null) {
                cir.setReturnValue(modelType);
            }
        }
    }
}
