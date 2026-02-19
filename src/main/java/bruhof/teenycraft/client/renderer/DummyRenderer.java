package bruhof.teenycraft.client.renderer;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class DummyRenderer extends LivingEntityRenderer<EntityTeenyDummy, PlayerModel<EntityTeenyDummy>> {
    public DummyRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new PlayerModel<>(pContext.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTeenyDummy pEntity) {
        // Use Steve skin for now
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }
}
