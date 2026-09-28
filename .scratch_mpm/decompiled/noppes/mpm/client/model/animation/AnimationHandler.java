package noppes.mpm.client.model.animation;

import java.util.HashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.mpm.ModelData;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.constants.EnumParts;

public class AnimationHandler {
   private static final HashMap<EnumAnimation, AnimationBase> ANIMATIONS = new HashMap<>();

   public static void animateBipedPre(
      ModelData data,
      HumanoidModel bipedModel,
      LivingEntity livingEntity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      bipedModel.f_102810_.f_104200_ = bipedModel.f_102810_.f_104201_ = bipedModel.f_102810_.f_104202_ = 0.0F;
      bipedModel.f_102810_.f_104203_ = bipedModel.f_102810_.f_104204_ = bipedModel.f_102810_.f_104205_ = 0.0F;
      bipedModel.f_102809_.f_104203_ = bipedModel.f_102808_.f_104203_ = 0.0F;
      bipedModel.f_102809_.f_104205_ = bipedModel.f_102808_.f_104205_ = 0.0F;
      bipedModel.f_102809_.f_104200_ = bipedModel.f_102808_.f_104200_ = 0.0F;
      bipedModel.f_102809_.f_104201_ = bipedModel.f_102808_.f_104201_ = 0.0F;
      bipedModel.f_102809_.f_104202_ = bipedModel.f_102808_.f_104202_ = 0.0F;
      bipedModel.f_102814_.f_104203_ = 0.0F;
      bipedModel.f_102814_.f_104204_ = 0.0F;
      bipedModel.f_102814_.f_104205_ = 0.0F;
      bipedModel.f_102813_.f_104203_ = 0.0F;
      bipedModel.f_102813_.f_104204_ = 0.0F;
      bipedModel.f_102813_.f_104205_ = 0.0F;
      bipedModel.f_102812_.f_104200_ = 0.0F;
      bipedModel.f_102812_.f_104201_ = 2.0F;
      bipedModel.f_102812_.f_104202_ = 0.0F;
      bipedModel.f_102811_.f_104200_ = 0.0F;
      bipedModel.f_102811_.f_104201_ = 2.0F;
      bipedModel.f_102811_.f_104202_ = 0.0F;
      AnimationBase animation = getAnimationFor(data.moveAnimation);
      if (animation != null) {
         animation.animatePre(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, livingEntity, bipedModel, data.animationStart);
      }

      animation = getAnimationFor(data.animation);
      if (animation != null) {
         animation.animatePre(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, livingEntity, bipedModel, data.animationStart);
      }

      if (bipedModel.f_102817_ && data.moveAnimation == EnumAnimation.CRAWL) {
         bipedModel.f_102817_ = false;
      }
   }

   public static void animateBipedPost(
      ModelData data,
      HumanoidModel bipedModel,
      LivingEntity livingEntity,
      float limbSwing,
      float limbSwingAmount,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      AnimationBase animation = getAnimationFor(data.moveAnimation);
      if (animation != null) {
         animation.animatePost(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, livingEntity, bipedModel, data.animationStart);
      }

      animation = getAnimationFor(data.animation);
      if (animation != null) {
         animation.animatePost(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, livingEntity, bipedModel, data.animationStart);
      }

      if (bipedModel.f_102817_ && data.moveAnimation != EnumAnimation.CRAWL) {
         bipedModel.f_102810_.f_104203_ = 0.5F / data.getPartConfig(EnumParts.BODY).scaleY;
      }

      if (bipedModel instanceof PlayerModel playerModel) {
         playerModel.f_103376_.m_104315_(playerModel.f_102814_);
         playerModel.f_103377_.m_104315_(playerModel.f_102813_);
         playerModel.f_103374_.m_104315_(playerModel.f_102812_);
         playerModel.f_103375_.m_104315_(playerModel.f_102811_);
         playerModel.f_103378_.m_104315_(playerModel.f_102810_);
      }

      bipedModel.f_102809_.m_104315_(bipedModel.f_102808_);
   }

   public static void addAnimation(EnumAnimation enumAnimation, AnimationBase animationBase) {
      ANIMATIONS.put(enumAnimation, animationBase);
   }

   public static HashMap<EnumAnimation, AnimationBase> getAllAnimations() {
      return ANIMATIONS;
   }

   public static AnimationBase getAnimationFor(EnumAnimation animation) {
      try {
         if (!ANIMATIONS.containsKey(animation)) {
            throw new IllegalAccessException("Animation " + animation.name() + " is not registered, maybe you forgot?");
         }
      } catch (Exception var2) {
      }

      return ANIMATIONS.get(animation);
   }

   public static void initAnimations() {
      addAnimation(EnumAnimation.NONE, new AniBlank());
      addAnimation(EnumAnimation.SLEEP, new AniBlank());
      addAnimation(EnumAnimation.CRAWL, new AniCrawling());
      addAnimation(EnumAnimation.HUG, new AniHug());
      addAnimation(EnumAnimation.DANCE, new AniDancing());
      addAnimation(EnumAnimation.WAVE, new AniWaving());
      addAnimation(EnumAnimation.WAG, new AniBlank());
      addAnimation(EnumAnimation.BOW, new AniBow());
      addAnimation(EnumAnimation.YES, new AniYes());
      addAnimation(EnumAnimation.NO, new AniNo());
      addAnimation(EnumAnimation.POINT, new AniPoint());
      addAnimation(EnumAnimation.DEATH, new AniBlank());
      addAnimation(
         EnumAnimation.CRY,
         new AnimationBase() {
            @Override
            public void animatePre(
               float limbSwing,
               float limbSwingAmount,
               float ageInTicks,
               float netHeadYaw,
               float headPitch,
               Entity entity,
               HumanoidModel model,
               int animationStart
            ) {
            }

            @Override
            public void animatePost(
               float limbSwing,
               float limbSwingAmount,
               float ageInTicks,
               float netHeadYaw,
               float headPitch,
               Entity entity,
               HumanoidModel model,
               int animationStart
            ) {
               model.f_102809_.f_104203_ = model.f_102808_.f_104203_ = 0.7F;
            }
         }
      );
      addAnimation(
         EnumAnimation.SIT,
         new AnimationBase() {
            @Override
            public void animatePre(
               float limbSwing,
               float limbSwingAmount,
               float ageInTicks,
               float netHeadYaw,
               float headPitch,
               Entity entity,
               HumanoidModel model,
               int animationStart
            ) {
               model.f_102609_ = true;
            }

            @Override
            public void animatePost(
               float limbSwing,
               float limbSwingAmount,
               float ageInTicks,
               float netHeadYaw,
               float headPitch,
               Entity entity,
               HumanoidModel model,
               int animationStart
            ) {
            }
         }
      );
   }
}
