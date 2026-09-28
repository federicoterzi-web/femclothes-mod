package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.model.ModelHeadwear;
import noppes.mpm.shared.client.model.Model2DRenderer;

public class LayerHeadwear extends LayerInterface implements LayerPreRender {
   private final ModelHeadwear headwear = new ModelHeadwear();

   public LayerHeadwear(PlayerRenderer render) {
      super(render);
   }

   @Override
   public void render(
      PoseStack mStack,
      MultiBufferSource typeBuffer,
      int lightmapUV,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      if (MorePlayerModels.HeadWearType == 1 && this.base.f_102808_.f_104207_ && this.headwear != null) {
         if (this.player.f_20916_ <= 0 && this.player.f_20919_ > 0) {
         }

         this.base.f_102808_.m_104299_(mStack);
         Model2DRenderer.textureOverride = this.player.m_108560_();
         VertexConsumer ivertex = typeBuffer.m_6299_(RenderType.m_110473_(this.player.m_108560_()));
         this.headwear.render(mStack, ivertex, lightmapUV, OverlayTexture.f_118083_);
         Model2DRenderer.textureOverride = null;
      }
   }

   @Override
   public void rotate(float limbSwing, float limbSwingAmount, float partialTicks, float age, float netHeadYaw, float headPitch) {
   }

   @Override
   public void preRender(AbstractClientPlayer player) {
      this.base.f_102809_.f_104207_ = this.base.f_102808_.f_104207_ && MorePlayerModels.HeadWearType != 1;
      if (!this.base.f_102809_.f_104207_) {
         this.headwear.config = null;
      }
   }
}
