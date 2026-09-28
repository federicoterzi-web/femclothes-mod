package noppes.mpm.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.client.ClientProxy;
import noppes.mpm.client.model.animation.AnimationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidModel.class})
public class BipedBodyMixin<T extends LivingEntity> {
   @Inject(
      at = {@At("HEAD")},
      method = {"setupAnim"}
   )
   private void setupAnimPre(
      T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo
   ) {
      HumanoidModel bipedModel = (HumanoidModel)this;
      if (livingEntity instanceof Player && bipedModel instanceof PlayerModel) {
         Player playerEntity = (Player)livingEntity;
         ClientProxy.playerModel = (PlayerModel)bipedModel;
         ClientProxy.data = ModelData.get(playerEntity);
         AnimationHandler.animateBipedPre(ClientProxy.data, bipedModel, livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"setupAnim"}
   )
   private void setupAnimPost(
      T livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo
   ) {
      HumanoidModel bipedModel = (HumanoidModel)this;
      if (livingEntity instanceof Player && bipedModel instanceof PlayerModel) {
         AnimationHandler.animateBipedPost(ClientProxy.data, bipedModel, livingEntity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      }
   }
}
