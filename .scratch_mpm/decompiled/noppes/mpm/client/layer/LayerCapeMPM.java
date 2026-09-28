package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import noppes.mpm.ModelData;
import noppes.mpm.ModelPartConfig;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.EnumParts;

public class LayerCapeMPM extends CapeLayer {
   private PlayerRenderer render;

   public LayerCapeMPM(PlayerRenderer render) {
      super(render);
      this.render = render;
   }

   public void m_6494_(
      PoseStack mStack,
      MultiBufferSource renderTypeBuffer,
      int lightmapUV,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      ModelData data = ModelData.get(player);
      ModelPartConfig config = data.getPartConfig(EnumParts.BODY);
      mStack.m_85836_();
      if (player.m_6047_() && data.moveAnimation != EnumAnimation.CRAWL) {
         mStack.m_85837_(0.0, 0.0, (-2.0F + config.scaleZ) * 0.0625);
      }

      mStack.m_85837_(config.transX, config.transY, config.transZ + (-1.0F + config.scaleZ) * 0.0625);
      mStack.m_85841_(config.scaleX, config.scaleY, 1.0F);
      if (data.moveAnimation == EnumAnimation.CRAWL) {
         int rotation = 78;
         if (player.m_6047_()) {
            mStack.m_252781_(Axis.f_252529_.m_252977_(-25.0F));
         }
      }

      if (player.f_20916_ <= 0 && player.f_20919_ > 0) {
      }

      super.m_6494_(mStack, renderTypeBuffer, lightmapUV, player, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
      mStack.m_85849_();
   }
}
