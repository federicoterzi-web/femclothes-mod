package noppes.mpm.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import noppes.mpm.shared.client.model.NopModelPart;

public class ModelWings extends Model {
   public NopModelPart body;
   public NopModelPart head;
   public NopModelPart left_wing_1;
   public NopModelPart right_wing_1;
   public NopModelPart left_wing_2;
   public NopModelPart left_wing_0;
   public NopModelPart left_wing_3;
   public NopModelPart left_wing_4;
   public NopModelPart right_wing_2 = new NopModelPart(64, 64, 42, 0);
   public NopModelPart right_wing_0;
   public NopModelPart right_wing_3;
   public NopModelPart right_wing_4;

   public ModelWings() {
      super(RenderType::m_110458_);
      this.right_wing_2.setPos(0.0F, 4.0F, -1.0F);
      this.right_wing_2.addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.right_wing_2, 1.2292354F, 0.0F, 0.0F);
      this.left_wing_3 = new NopModelPart(64, 64, 26, 0);
      this.left_wing_3.setPos(0.0F, 7.0F, 2.0F);
      this.left_wing_3.addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.left_wing_3, -1.2292354F, 0.0F, 0.0F);
      this.right_wing_1 = new NopModelPart(64, 64, 8, 0);
      this.right_wing_1.setPos(-2.4F, 2.0F, 1.5F);
      this.right_wing_1.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.right_wing_1, 1.5358897F, (float) (-Math.PI * 3.0 / 10.0), 0.0F);
      this.left_wing_0 = new NopModelPart(64, 64, 6, 0);
      this.left_wing_0.setPos(2.4F, 2.0F, 1.5F);
      this.left_wing_0.addBox(-3.4F, -2.0F, -15.0F, 1.0F, 11.0F, 18.0F, 0.0F);
      this.right_wing_3 = new NopModelPart(64, 64, 50, 0);
      this.right_wing_3.setPos(0.0F, 7.0F, 2.0F);
      this.right_wing_3.addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.right_wing_3, -1.2292354F, 0.0F, 0.0F);
      this.left_wing_2 = new NopModelPart(64, 64, 16, 0);
      this.left_wing_2.setPos(0.0F, 4.0F, -1.0F);
      this.left_wing_2.addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.left_wing_2, 1.2292354F, 0.0F, 0.0F);
      this.body = new NopModelPart(64, 64, 0, 0);
      this.body.setPos(0.0F, 0.0F, 0.0F);
      this.body.addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.head = new NopModelPart(64, 64, 0, 34);
      this.head.setPos(0.0F, 0.0F, 0.0F);
      this.head.addBox(-4.0F, -8.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.left_wing_1 = new NopModelPart(64, 64, 0, 0);
      this.left_wing_1.setPos(2.4F, 2.0F, 1.5F);
      this.left_wing_1.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.left_wing_1, 1.5358897F, (float) (Math.PI * 3.0 / 10.0), 0.0F);
      this.right_wing_4 = new NopModelPart(64, 64, 64, 0);
      this.right_wing_4.setPos(0.0F, 5.0F, 0.0F);
      this.right_wing_4.addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.right_wing_4, -1.1383038F, 0.0F, 0.0F);
      this.left_wing_4 = new NopModelPart(64, 64, 34, 0);
      this.left_wing_4.setPos(0.0F, 5.0F, 0.0F);
      this.left_wing_4.addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, 0.0F);
      this.setRotateAngle(this.left_wing_4, -1.1383038F, 0.0F, 0.0F);
      this.right_wing_0 = new NopModelPart(64, 64, 44, 0);
      this.right_wing_0.setPos(-2.4F, 2.0F, 1.5F);
      this.right_wing_0.addBox(2.4F, -2.0F, -15.0F, 1.0F, 11.0F, 18.0F, 0.0F);
      this.right_wing_1.addChild(this.right_wing_2);
      this.left_wing_2.addChild(this.left_wing_3);
      this.body.addChild(this.right_wing_1);
      this.left_wing_1.addChild(this.left_wing_0);
      this.right_wing_2.addChild(this.right_wing_3);
      this.left_wing_1.addChild(this.left_wing_2);
      this.body.addChild(this.left_wing_1);
      this.right_wing_3.addChild(this.right_wing_4);
      this.left_wing_3.addChild(this.left_wing_4);
      this.right_wing_1.addChild(this.right_wing_0);
   }

   public void render(
      Entity entityIn,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      PoseStack mStack,
      VertexConsumer ivertex,
      int lightmapUV
   ) {
      mStack.m_85836_();
      if (entityIn.m_6047_()) {
         mStack.m_252880_(0.0F, 0.2F, 0.0F);
      }

      this.renderWings(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, mStack, ivertex, lightmapUV);
      mStack.m_85849_();
   }

   public void renderWings(
      Entity player,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      PoseStack mStack,
      VertexConsumer ivertex,
      int lightmapUV
   ) {
      float motion = Math.abs(Mth.m_14031_(limbSwing * 0.033F + (float) Math.PI) * 0.4F) * limbSwingAmount;
      boolean flapWings = player.m_9236_().m_46859_(player.m_20183_().m_7495_());
      float speed = 0.55F + 0.5F * motion;
      float y = Mth.m_14031_(ageInTicks * 0.35F);
      float flap = y * 0.5F * speed;
      mStack.m_85836_();
      if (flapWings) {
         Axis.f_252436_.m_252977_(flap * 20.0F);
      }

      this.left_wing_1.render(mStack, ivertex, lightmapUV, OverlayTexture.f_118083_);
      mStack.m_85849_();
      mStack.m_85836_();
      if (flapWings) {
         Axis.f_252436_.m_252977_(-flap * 20.0F);
      }

      this.right_wing_1.render(mStack, ivertex, lightmapUV, OverlayTexture.f_118083_);
      mStack.m_85849_();
   }

   public void setRotateAngle(NopModelPart modelRenderer, float x, float y, float z) {
      modelRenderer.xRot = x;
      modelRenderer.yRot = y;
      modelRenderer.zRot = z;
   }

   public void m_7695_(PoseStack poseStack, VertexConsumer iVertexBuilder, int i, int i1, float v, float v1, float v2, float v3) {
   }
}
