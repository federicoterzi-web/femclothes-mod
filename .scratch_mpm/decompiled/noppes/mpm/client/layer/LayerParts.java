package noppes.mpm.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import noppes.mpm.ModelData;
import noppes.mpm.ModelPartConfig;
import noppes.mpm.client.parts.ModelPartWrapper;
import noppes.mpm.client.parts.MpmPart;
import noppes.mpm.client.parts.MpmPartAbstractClient;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.client.parts.MpmPartDataClient;
import noppes.mpm.constants.BodyPart;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.EnumParts;
import noppes.mpm.constants.PartBehaviorType;
import noppes.mpm.constants.PartRenderType;
import noppes.mpm.shared.util.NopVector3f;

public class LayerParts extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public LayerParts(PlayerRenderer render) {
      super(render);
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
      ModelData data = ModelData.get(player);

      for (MpmPartData part : data.mpmParts) {
         MpmPart mp = part.getPart();
         if (mp != null && mp.renderType != PartRenderType.NONE && mp.isEnabled) {
            MpmPartAbstractClient partc = (MpmPartAbstractClient)mp;
            this.rotate(
               (MpmPartDataClient)part.clientData,
               data,
               partc,
               player,
               (PlayerModel)this.m_117386_(),
               limbSwing,
               limbSwingAmount,
               partialTicks,
               age,
               netHeadYaw,
               headPitch
            );
            renderPart(part, partc, mStack, typeBuffer, lightmapUV, player, (PlayerModel)this.m_117386_(), data);
         }
      }

      data.startMoveAnimation = false;
      data.startAnimation = false;
   }

   public static void renderPart(
      MpmPartData data,
      MpmPartAbstractClient partc,
      PoseStack mStack,
      MultiBufferSource typeBuffer,
      int lightmapUV,
      AbstractClientPlayer player,
      PlayerModel model,
      ModelData pdata
   ) {
      mStack.m_85836_();
      boolean shouldRender = true;
      if (partc.bodyPart == BodyPart.HEAD) {
         model.f_102808_.m_104299_(mStack);
      }

      if (partc.bodyPart == BodyPart.BODY) {
         model.f_102810_.m_104299_(mStack);
      }

      if (partc.bodyPart == BodyPart.LEGS) {
         ModelPartWrapper rmodelPart = partc.getPart("right_leg");
         ModelPartWrapper lmodelPart = partc.getPart("left_leg");
         if (rmodelPart != null) {
            shouldRender = false;
            mStack.m_85836_();
            ModelPartConfig config = pdata.getPartConfig(EnumParts.LEG_RIGHT);
            mStack.m_252880_(0.0F, config.transY * 2.0F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
            if (lmodelPart != null) {
               lmodelPart.setVisible(false);
            }

            rmodelPart.setVisible(true);
            partc.render(data, mStack, typeBuffer, lightmapUV, player);
            mStack.m_85849_();
         }

         if (lmodelPart != null) {
            shouldRender = false;
            mStack.m_85836_();
            ModelPartConfig config = pdata.getPartConfig(EnumParts.LEG_LEFT);
            mStack.m_252880_(0.0F, config.transY * 2.0F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
            if (rmodelPart != null) {
               rmodelPart.setVisible(false);
            }

            lmodelPart.setVisible(true);
            partc.render(data, mStack, typeBuffer, lightmapUV, player);
            mStack.m_85849_();
         }

         if (shouldRender) {
            ModelPartConfig config = pdata.getPartConfig(EnumParts.LEG_LEFT);
            mStack.m_252880_(0.0F, config.transY * 2.0F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
         }
      }

      if (partc.bodyPart == BodyPart.ARMS) {
         ModelPartWrapper rmodelPartx = partc.getPart("right_arm");
         ModelPartWrapper lmodelPartx = partc.getPart("left_arm");
         if (rmodelPartx != null) {
            shouldRender = false;
            mStack.m_85836_();
            ModelPartConfig config = pdata.getPartConfig(EnumParts.ARM_RIGHT);
            mStack.m_252880_(0.0F, config.transY + (1.0F - config.scaleY) * 0.125F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
            if (lmodelPartx != null) {
               lmodelPartx.setVisible(false);
            }

            rmodelPartx.setVisible(true);
            partc.render(data, mStack, typeBuffer, lightmapUV, player);
            mStack.m_85849_();
         }

         if (lmodelPartx != null) {
            shouldRender = false;
            mStack.m_85836_();
            ModelPartConfig config = pdata.getPartConfig(EnumParts.ARM_LEFT);
            mStack.m_252880_(0.0F, config.transY + (1.0F - config.scaleY) * 0.125F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
            if (rmodelPartx != null) {
               rmodelPartx.setVisible(false);
            }

            lmodelPartx.setVisible(true);
            partc.render(data, mStack, typeBuffer, lightmapUV, player);
            mStack.m_85849_();
         }

         if (shouldRender) {
            ModelPartConfig config = pdata.getPartConfig(EnumParts.ARM_LEFT);
            mStack.m_252880_(0.0F, config.transY + (1.0F - config.scaleY) * 0.125F, 0.0F);
            mStack.m_85841_(config.scaleX, config.scaleY, config.scaleZ);
         }
      }

      if (shouldRender) {
         partc.render(data, mStack, typeBuffer, lightmapUV, player);
      }

      mStack.m_85849_();
   }

   private void rotate(
      MpmPartDataClient partData,
      ModelData playerdata,
      MpmPartAbstractClient part,
      AbstractClientPlayer player,
      PlayerModel base,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      partData.animation(part, EnumAnimation.STATIC, (int)age, partialTicks);
      EnumAnimation moveAnimation = playerdata.getMoveAnimtion(player);
      if (playerdata.startMoveAnimation) {
         partData.start(part);
      }

      boolean didAnimation = false;
      if (playerdata.animation != EnumAnimation.NONE) {
         if (playerdata.startAnimation) {
            partData.start(part);
         }

         didAnimation = partData.animation(part, playerdata.animation, (int)age, partialTicks);
      }

      if (didAnimation || moveAnimation != EnumAnimation.IDLE && moveAnimation != EnumAnimation.FLY_IDLE) {
         partData.animation(part, moveAnimation, Mth.m_14089_(limbSwing * 0.6662F) * limbSwingAmount / 2.0F + 0.5F);
      } else {
         partData.animation(part, moveAnimation, (int)age, partialTicks);
      }

      if (part.animationType == PartBehaviorType.LEGS) {
         PlayerModel model = (PlayerModel)this.m_117386_();
         ModelPartWrapper modelPart = part.getPart("right_leg");
         if (modelPart != null) {
            modelPart.setRot(new NopVector3f(model.f_102813_.f_104203_, model.f_102813_.f_104204_, model.f_102813_.f_104205_));
            modelPart.setPos(new NopVector3f(model.f_102813_.f_104200_, model.f_102813_.f_104201_, model.f_102813_.f_104202_));
         }

         modelPart = part.getPart("left_leg");
         if (modelPart != null) {
            modelPart.setRot(new NopVector3f(model.f_102814_.f_104203_, model.f_102814_.f_104204_, model.f_102814_.f_104205_));
            modelPart.setPos(new NopVector3f(model.f_102814_.f_104200_, model.f_102814_.f_104201_, model.f_102814_.f_104202_));
         }
      }

      if (part.animationType == PartBehaviorType.ARMS) {
         PlayerModel modelx = (PlayerModel)this.m_117386_();
         ModelPartWrapper modelPartx = part.getPart("right_arm");
         if (modelPartx != null) {
            modelPartx.setRot(new NopVector3f(modelx.f_102811_.f_104203_, modelx.f_102811_.f_104204_, modelx.f_102811_.f_104205_));
            modelPartx.setPos(new NopVector3f(modelx.f_102811_.f_104200_, modelx.f_102811_.f_104201_, modelx.f_102811_.f_104202_));
         }

         modelPartx = part.getPart("left_arm");
         if (modelPartx != null) {
            modelPartx.setRot(new NopVector3f(modelx.f_102812_.f_104203_, modelx.f_102812_.f_104204_, modelx.f_102812_.f_104205_));
            modelPartx.setPos(new NopVector3f(modelx.f_102812_.f_104200_, modelx.f_102812_.f_104201_, modelx.f_102812_.f_104202_));
         }
      }

      if (part.animationType == PartBehaviorType.BEARD) {
         part.rot = part.rot.set(base.f_102808_.f_104203_ < 0.0F ? 0.0F : -base.f_102808_.f_104203_, part.rot.y, part.rot.z);
      }

      if (part.animationType == PartBehaviorType.HAIR) {
         ModelPart head = base.f_102808_;
         if (head.f_104203_ < 0.0F) {
            part.rot = part.rot.set(-head.f_104203_ * 1.2F, part.rot.y, part.rot.z);
            if (head.f_104203_ > -1.0F) {
               part.pos = part.pos.set(part.pos.x, -head.f_104203_ * 1.5F, -head.f_104203_ * 1.5F);
            }
         } else {
            part.pos = NopVector3f.ZERO;
         }
      }

      if (part.animationType == PartBehaviorType.WINGS) {
         ModelPartWrapper modelPartxx = part.getPart("right_wing");
         ModelPartWrapper modelPartL = part.getPart("left_wing");
         float xRot;
         float zRot;
         if (player.m_9236_().m_46859_(player.m_20183_().m_7495_())) {
            float motion = Math.abs(Mth.m_14031_(limbSwing * 0.033F + (float) Math.PI) * 0.4F) * limbSwingAmount;
            float speed = 0.55F + 0.5F * motion;
            float y = Mth.m_14031_(age * 0.35F);
            xRot = zRot = y * 0.5F * speed;
         } else {
            zRot = Mth.m_14089_(age * 0.09F) * 0.05F + 0.05F;
            xRot = Mth.m_14031_(age * 0.067F) * 0.05F;
         }

         modelPartxx.setRot(modelPartxx.oriRot.add(xRot, xRot, zRot));
         modelPartL.setRot(modelPartL.oriRot.add(xRot, -xRot, -zRot));
      }

      if (part.animationType == PartBehaviorType.WINGS2) {
         ModelPartWrapper modelPartxx = part.getPart("right_wing");
         ModelPartWrapper modelPartL = part.getPart("left_wing");
         float yRot;
         if (player.m_9236_().m_46859_(player.m_20183_().m_7495_())) {
            float motion = Math.abs(Mth.m_14031_(limbSwing * 0.033F + (float) Math.PI) * 0.4F) * limbSwingAmount;
            float speed = 0.55F + 0.5F * motion;
            float y = Mth.m_14031_(age * 0.35F);
            yRot = y * 0.5F * speed;
         } else {
            yRot = Mth.m_14031_(age * 0.07F) * 0.44F;
         }

         modelPartxx.setRot(modelPartxx.oriRot.add(0.0F, yRot, 0.0F));
         modelPartL.setRot(modelPartL.oriRot.add(0.0F, -yRot, 0.0F));
      }
   }
}
