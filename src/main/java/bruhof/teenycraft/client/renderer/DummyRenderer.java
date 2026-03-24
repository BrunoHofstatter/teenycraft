package bruhof.teenycraft.client.renderer;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.client.ClientBattleData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class DummyRenderer extends LivingEntityRenderer<EntityTeenyDummy, PlayerModel<EntityTeenyDummy>> {
    private final PlayerModel<EntityTeenyDummy> wideModel;
    private final PlayerModel<EntityTeenyDummy> slimModel;

    public DummyRenderer(EntityRendererProvider.Context pContext) {
        // Default to wide model in constructor
        super(pContext, new PlayerModel<>(pContext.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wideModel = this.model;
        this.slimModel = new PlayerModel<>(pContext.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(EntityTeenyDummy pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        String modelType = ClientBattleData.getEnemyActiveModelType();
        if ("slim".equals(modelType)) {
            this.model = slimModel;
        } else {
            this.model = wideModel;
        }
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTeenyDummy pEntity) {
        String figureId = ClientBattleData.getEnemyActiveId();
        if (figureId != null && !figureId.equals("none")) {
            return new ResourceLocation(TeenyCraft.MOD_ID, "textures/entity/figure/" + figureId + ".png");
        }
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }
}
