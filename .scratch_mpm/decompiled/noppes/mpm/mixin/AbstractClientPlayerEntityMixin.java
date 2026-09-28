package noppes.mpm.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import noppes.mpm.ModelData;
import noppes.mpm.client.SkinUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public class AbstractClientPlayerEntityMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"getModelName"},
      cancellable = true
   )
   private void getModelName(CallbackInfoReturnable<String> cir) {
      ModelData data = ModelData.get((Player)this);
      if (data != null && data.modelType != 0) {
         if (data.modelType == 1) {
            cir.setReturnValue("default");
         } else {
            cir.setReturnValue("slim");
         }

         cir.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"getSkinTextureLocation"},
      cancellable = true
   )
   private void getTextureLocation(CallbackInfoReturnable<ResourceLocation> cir) {
      Player player = (Player)this;
      ModelData data = ModelData.get(player);
      SkinUtil.load(data, player);
      if (data.resourceLoaded && data.resourceLocation != null) {
         cir.setReturnValue(data.resourceLocation);
         cir.cancel();
      }

      if (!cir.isCancelled() && data.getEntity(player) != null) {
         EntityRenderer renderer = Minecraft.m_91087_().m_91290_().m_114382_(data.getEntity(player));
         if (renderer != null) {
            ResourceLocation location = renderer.m_5478_(data.getEntity(player));
            if (location != null) {
               cir.setReturnValue(location);
               cir.cancel();
            }
         }
      }
   }
}
