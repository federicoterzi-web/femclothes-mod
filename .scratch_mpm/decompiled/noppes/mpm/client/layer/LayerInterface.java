package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.mpm.ModelData;
import noppes.mpm.ModelPartData;
import noppes.mpm.shared.client.model.NopModelPart;

public abstract class LayerInterface extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   protected AbstractClientPlayer player;
   protected ModelData playerdata;
   protected PlayerModel base;

   public LayerInterface(PlayerRenderer render) {
      super(render);
      this.setBase((PlayerModel)this.m_117386_());
   }

   public void setBase(PlayerModel base) {
      this.base = base;
      this.createParts();
   }

   public void setColor(ModelPartData data, LivingEntity entity) {
   }

   protected void createParts() {
   }

   public ResourceLocation getResource(ModelPartData data) {
      return data.playerTexture ? this.player.m_108560_() : data.getResource();
   }

   protected float red(ModelPartData data) {
      return this.player.f_20916_ <= 0 && this.player.f_20919_ <= 0 ? (data.color >> 16 & 0xFF) / 255.0F : 1.0F;
   }

   protected float green(ModelPartData data) {
      return this.player.f_20916_ <= 0 && this.player.f_20919_ <= 0 ? (data.color >> 8 & 0xFF) / 255.0F : 0.0F;
   }

   protected float blue(ModelPartData data) {
      return this.player.f_20916_ <= 0 && this.player.f_20919_ <= 0 ? (data.color & 0xFF) / 255.0F : 0.0F;
   }

   protected float alpha() {
      return this.player.f_20916_ <= 0 && this.player.f_20919_ <= 0 ? 0.99F : 0.3F;
   }

   public void render(
      PoseStack mStack,
      MultiBufferSource typeBuffer,
      int lightmapUV,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      if (!player.m_20145_()) {
         if (this.base != this.m_117386_()) {
            this.setBase((PlayerModel)this.m_117386_());
         }

         this.player = player;
         this.playerdata = ModelData.get(player);
         this.rotate(limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
         mStack.m_85836_();
         if (player.m_6047_()) {
         }

         this.render(mStack, typeBuffer, lightmapUV, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
         mStack.m_85849_();
      }
   }

   public void setRotation(NopModelPart model, float x, float y, float z) {
      model.xRot = x;
      model.yRot = y;
      model.zRot = z;
   }

   public abstract void render(PoseStack var1, MultiBufferSource var2, int var3, float var4, float var5, float var6, float var7, float var8, float var9);

   public abstract void rotate(float var1, float var2, float var3, float var4, float var5, float var6);
}
