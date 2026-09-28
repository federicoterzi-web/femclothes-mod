package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.ModelPartConfig;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.EnumParts;

public class LayerElytraAlt<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M> {
   public LayerElytraAlt(RenderLayerParent<T, M> renderer, EntityModelSet set) {
      super(renderer, set);
   }

   public void m_6494_(
      PoseStack mStack,
      MultiBufferSource renderTypeBuffer,
      int lightmapUV,
      T entityLiving,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      if (!(entityLiving instanceof Player player)) {
         super.m_6494_(mStack, renderTypeBuffer, lightmapUV, entityLiving, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
      } else {
         ModelData data = ModelData.get(player);
         if (data.wingMode != 1) {
            ModelPartConfig config = data.getPartConfig(EnumParts.BODY);
            mStack.m_85836_();
            if (player.m_6047_() && data.moveAnimation != EnumAnimation.CRAWL) {
               mStack.m_252880_(0.0F, 0.0F, (-2.0F + config.scaleZ) * 0.0625F);
            }

            mStack.m_252880_(config.transX, config.transY, config.transZ + (-1.0F + config.scaleZ) * 0.0625F);
            mStack.m_85841_(config.scaleX, config.scaleY, 1.0F);
            if (data.moveAnimation == EnumAnimation.CRAWL) {
               int rotation = 78;
               if (player.m_6047_()) {
                  mStack.m_252781_(Axis.f_252529_.m_252977_(-25.0F));
               }
            }

            if (player.f_20916_ <= 0 && player.f_20919_ > 0) {
            }

            super.m_6494_(mStack, renderTypeBuffer, lightmapUV, entityLiving, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
            mStack.m_85849_();
         }
      }
   }
}
