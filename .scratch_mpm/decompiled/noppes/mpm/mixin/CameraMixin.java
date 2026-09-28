package noppes.mpm.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import noppes.mpm.client.ClientEventHandler;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class CameraMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"getMaxZoom"},
      cancellable = true
   )
   private void getMaxZoom(double zoom, CallbackInfoReturnable<Double> cir) {
      if (ClientEventHandler.camera.enabled) {
         Camera info = (Camera)this;
         Vec3 position = info.m_90583_();
         Vector3f forwards = info.m_253058_();
         Level level = Minecraft.m_91087_().f_91073_;
         zoom = ClientEventHandler.camera.cameraDistance;

         for (int i = 0; i < 8; i++) {
            float f = (i & 1) * 2 - 1;
            float f1 = (i >> 1 & 1) * 2 - 1;
            float f2 = (i >> 2 & 1) * 2 - 1;
            f *= 0.1F;
            f1 *= 0.1F;
            f2 *= 0.1F;
            Vec3 vector3d = position.m_82520_(f, f1, f2);
            Vec3 vector3d1 = new Vec3(
               position.f_82479_ - forwards.x() * zoom + f + f2, position.f_82480_ - forwards.y() * zoom + f1, position.f_82481_ - forwards.z() * zoom + f2
            );
            BlockHitResult raytraceresult = level.m_45547_(new ClipContext(vector3d, vector3d1, Block.VISUAL, Fluid.NONE, info.m_90592_()));
            if (raytraceresult.m_6662_() != Type.MISS) {
               double d0 = raytraceresult.m_82450_().m_82554_(position);
               if (d0 < zoom) {
                  zoom = d0;
               }
            }
         }

         cir.setReturnValue(zoom);
         cir.cancel();
      }
   }
}
