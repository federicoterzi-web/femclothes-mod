package noppes.mpm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.client.RenderEvent;
import noppes.mpm.constants.BodyPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityRenderer.class})
public class LivingRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
   private boolean leftLegVisible;
   private boolean rightLegVisible;
   private boolean leftArmVisible;
   private boolean rightArmVisible;
   private boolean leftPantsVisible;
   private boolean rightPantsVisible;
   private boolean leftSleeveVisible;
   private boolean rightSleeveVisible;
   private boolean bodyVisible;
   private boolean jacketVisible;
   private boolean headVisible;
   private boolean hatVisible;

   @Inject(
      at = {@At("HEAD")},
      method = {"getRenderType"},
      cancellable = true
   )
   private void getModelName(T livingEntity, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_, CallbackInfoReturnable<RenderType> cir) {
      if (RenderEvent.entityResource != null) {
         if (p_230496_3_) {
            cir.setReturnValue(RenderType.m_110467_(RenderEvent.entityResource));
         } else if (p_230496_2_) {
            LivingEntityRenderer r = (LivingEntityRenderer)this;
            cir.setReturnValue(r.m_7200_().m_103119_(RenderEvent.entityResource));
         } else {
            cir.setReturnValue(p_230496_4_ ? RenderType.m_110491_(RenderEvent.entityResource) : null);
         }

         RenderEvent.entityResource = null;
         cir.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"render"},
      cancellable = false
   )
   private void renderPre(T entity, float p_115309_, float p_115310_, PoseStack p_115311_, MultiBufferSource p_115312_, int p_115313_, CallbackInfo cb) {
      LivingEntityRenderer r = (LivingEntityRenderer)this;
      if (entity instanceof AbstractClientPlayer && r.m_7200_() instanceof PlayerModel) {
         ModelData data = ModelData.get((Player)entity);
         PlayerModel model = (PlayerModel)r.m_7200_();
         this.leftLegVisible = model.f_102814_.f_104207_;
         this.rightLegVisible = model.f_102813_.f_104207_;
         this.leftArmVisible = model.f_102812_.f_104207_;
         this.rightArmVisible = model.f_102811_.f_104207_;
         this.leftPantsVisible = model.f_103376_.f_104207_;
         this.rightPantsVisible = model.f_103377_.f_104207_;
         this.leftSleeveVisible = model.f_103374_.f_104207_;
         this.rightSleeveVisible = model.f_103375_.f_104207_;
         this.bodyVisible = model.f_102810_.f_104207_;
         this.jacketVisible = model.f_103378_.f_104207_;
         this.headVisible = model.f_102808_.f_104207_;
         this.hatVisible = model.f_102809_.f_104207_;
         model.f_102814_.f_104207_ = model.f_102814_.f_104207_ && !data.hiddenParts.contains(BodyPart.LEFT_LEG) && !data.hiddenParts.contains(BodyPart.LEGS);
         model.f_103376_.f_104207_ = model.f_103376_.f_104207_ && model.f_102814_.f_104207_;
         model.f_102813_.f_104207_ = model.f_102813_.f_104207_ && !data.hiddenParts.contains(BodyPart.RIGHT_LEG) && !data.hiddenParts.contains(BodyPart.LEGS);
         model.f_103377_.f_104207_ = model.f_103377_.f_104207_ && model.f_102813_.f_104207_;
         model.f_102812_.f_104207_ = model.f_102812_.f_104207_ && !data.hiddenParts.contains(BodyPart.LEFT_ARM) && !data.hiddenParts.contains(BodyPart.ARMS);
         model.f_103374_.f_104207_ = model.f_103374_.f_104207_ && model.f_102812_.f_104207_;
         model.f_102811_.f_104207_ = model.f_102811_.f_104207_ && !data.hiddenParts.contains(BodyPart.RIGHT_ARM) && !data.hiddenParts.contains(BodyPart.ARMS);
         model.f_103375_.f_104207_ = model.f_103375_.f_104207_ && model.f_102811_.f_104207_;
         model.f_102810_.f_104207_ = model.f_102810_.f_104207_ && !data.hiddenParts.contains(BodyPart.BODY);
         model.f_103378_.f_104207_ = model.f_103378_.f_104207_ && model.f_102810_.f_104207_;
         model.f_102808_.f_104207_ = model.f_102808_.f_104207_ && !data.hiddenParts.contains(BodyPart.HEAD);
         model.f_102809_.f_104207_ = model.f_102809_.f_104207_ && model.f_102808_.f_104207_;
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"render"},
      cancellable = false
   )
   private void renderPost(T entity, float p_115309_, float p_115310_, PoseStack p_115311_, MultiBufferSource p_115312_, int p_115313_, CallbackInfo cb) {
      LivingEntityRenderer r = (LivingEntityRenderer)this;
      if (entity instanceof AbstractClientPlayer && r.m_7200_() instanceof PlayerModel) {
         PlayerModel model = (PlayerModel)r.m_7200_();
         model.f_102814_.f_104207_ = this.leftLegVisible;
         model.f_102813_.f_104207_ = this.rightLegVisible;
         model.f_102812_.f_104207_ = this.leftArmVisible;
         model.f_102811_.f_104207_ = this.rightArmVisible;
         model.f_103376_.f_104207_ = this.leftPantsVisible;
         model.f_103377_.f_104207_ = this.rightPantsVisible;
         model.f_103374_.f_104207_ = this.leftSleeveVisible;
         model.f_103375_.f_104207_ = this.rightSleeveVisible;
         model.f_102810_.f_104207_ = this.bodyVisible;
         model.f_103378_.f_104207_ = this.jacketVisible;
         model.f_102808_.f_104207_ = this.headVisible;
         model.f_102809_.f_104207_ = this.hatVisible;
      }
   }
}
