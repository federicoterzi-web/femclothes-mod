package noppes.mpm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import noppes.mpm.mixin.MouseHelperMixin;

public class MpmCamera {
   public boolean enabled = false;
   public float cameraYaw = 0.0F;
   public float cameraPitch = 0.0F;
   public float playerYaw = 0.0F;
   public float playerPitch = 0.0F;
   public float cameraDistance = 4.0F;
   private double mouseX;
   private double mouseY;

   public void update(boolean start) {
      Minecraft mc = Minecraft.m_91087_();
      Entity view = mc.m_91288_();
      if (!mc.f_91063_.m_109153_().m_90594_()) {
         if (this.enabled) {
            this.reset();
         }
      } else if (this.enabled && view != null) {
         this.updateCamera();
         if (start) {
            view.m_146926_(view.f_19860_ = this.cameraPitch);
            view.m_146922_(view.f_19859_ = this.cameraYaw);
         } else {
            view.m_146926_(mc.f_91074_.m_146909_() - this.playerPitch);
            view.f_19860_ = mc.f_91074_.f_19860_ - this.playerPitch;
            view.m_146922_(this.playerYaw);
            view.f_19859_ = this.playerYaw;
         }
      }
   }

   private void updateCamera() {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.m_91302_()) {
         double f = (Double)mc.f_91066_.m_231964_().m_231551_() * 0.6 + 0.2;
         double f1 = f * f * f * 8.0;
         double dx = (mc.f_91067_.m_91589_() - this.mouseX) * f1 * 0.15;
         double dy = (mc.f_91067_.m_91594_() - this.mouseY) * f1 * 0.15;
         if (MpmKeys.Camera.m_90857_()) {
            this.cameraYaw = (float)(this.cameraYaw + dx);
            this.cameraPitch = (float)(this.cameraPitch + dy);
            this.cameraPitch = Mth.m_14036_(this.cameraPitch, -90.0F, 90.0F);
         } else {
            this.playerYaw = (float)(this.playerYaw + dx);
            this.playerPitch = (float)(this.playerPitch + dy);
            this.playerPitch = Mth.m_14036_(this.playerPitch, -90.0F, 90.0F);
         }

         this.mouseX = mc.f_91067_.m_91589_();
         this.mouseY = mc.f_91067_.m_91594_();
      }
   }

   public void reset() {
      this.enabled = false;
      this.cameraYaw = 0.0F;
      this.cameraPitch = 0.0F;
      this.playerYaw = 0.0F;
      this.playerPitch = 0.0F;
      this.cameraDistance = 4.0F;
   }

   public void enable() {
      Minecraft mc = Minecraft.m_91087_();
      if (!this.enabled) {
         this.cameraPitch = this.playerPitch = mc.f_91074_.m_146909_();
         this.cameraYaw = this.playerYaw = mc.f_91074_.m_146908_();
      }

      this.enabled = true;
      this.mouseX = ((MouseHelperMixin)mc.f_91067_).getX();
      this.mouseY = ((MouseHelperMixin)mc.f_91067_).getY();
   }
}
