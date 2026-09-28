package noppes.mpm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import noppes.mpm.ModelPartConfig;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.ClientProxy;
import noppes.mpm.constants.EnumParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ModelPart.class})
public class ModelRendererMixin {
   private ModelPartConfig mpmconfig;

   @Inject(
      at = {@At("HEAD")},
      method = {"translateAndRotate"}
   )
   private void translateAndRotatePre(PoseStack mStack, CallbackInfo callbackInfo) {
      if (!MorePlayerModels.Compatibility) {
         this.mpmconfig = this.getMpmconfig();
         if (this.mpmconfig != null) {
            mStack.m_252880_(this.mpmconfig.transX, this.mpmconfig.transY, this.mpmconfig.transZ);
         }
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"translateAndRotate"}
   )
   private void translateAndRotatePost(PoseStack mStack, CallbackInfo callbackInfo) {
      if (!MorePlayerModels.Compatibility) {
         this.mpmconfig = this.getMpmconfig();
         if (this.mpmconfig != null) {
            mStack.m_85841_(this.mpmconfig.scaleX, this.mpmconfig.scaleY, this.mpmconfig.scaleZ);
         }
      }
   }

   private ModelPartConfig getMpmconfig() {
      if (ClientProxy.data == null) {
         return null;
      } else {
         ModelPart model = (ModelPart)this;
         if (model == ClientProxy.playerModel.f_102810_
            || model == ClientProxy.playerModel.f_103378_
            || model == ClientProxy.armorLayer.getOuter().f_102810_
            || model == ClientProxy.armorLayerSlim.getOuter().f_102810_
            || model == ClientProxy.armorLayer.getInner().f_102810_
            || model == ClientProxy.armorLayerSlim.getInner().f_102810_) {
            return ClientProxy.data.getPartConfig(EnumParts.BODY);
         } else if (model == ClientProxy.playerModel.f_102808_
            || model == ClientProxy.playerModel.f_102809_
            || model == ClientProxy.armorLayer.getOuter().f_102808_
            || model == ClientProxy.armorLayerSlim.getOuter().f_102808_) {
            return ClientProxy.data.getPartConfig(EnumParts.HEAD);
         } else if (model == ClientProxy.playerModel.f_102814_
            || model == ClientProxy.playerModel.f_103376_
            || model == ClientProxy.armorLayer.getOuter().f_102814_
            || model == ClientProxy.armorLayer.getInner().f_102814_
            || model == ClientProxy.armorLayerSlim.getOuter().f_102814_
            || model == ClientProxy.armorLayerSlim.getInner().f_102814_) {
            return ClientProxy.data.getPartConfig(EnumParts.LEG_LEFT);
         } else if (model == ClientProxy.playerModel.f_102813_
            || model == ClientProxy.playerModel.f_103377_
            || model == ClientProxy.armorLayer.getOuter().f_102813_
            || model == ClientProxy.armorLayer.getInner().f_102813_
            || model == ClientProxy.armorLayerSlim.getOuter().f_102813_
            || model == ClientProxy.armorLayerSlim.getInner().f_102813_) {
            return ClientProxy.data.getPartConfig(EnumParts.LEG_RIGHT);
         } else if (model == ClientProxy.playerModel.f_102812_
            || model == ClientProxy.playerModel.f_103374_
            || model == ClientProxy.armorLayer.getOuter().f_102812_
            || model == ClientProxy.armorLayerSlim.getOuter().f_102812_) {
            return ClientProxy.data.getPartConfig(EnumParts.ARM_LEFT);
         } else {
            return model != ClientProxy.playerModel.f_102811_
                  && model != ClientProxy.playerModel.f_103375_
                  && model != ClientProxy.armorLayer.getOuter().f_102811_
                  && model != ClientProxy.armorLayerSlim.getOuter().f_102811_
               ? null
               : ClientProxy.data.getPartConfig(EnumParts.ARM_RIGHT);
         }
      }
   }
}
