package noppes.mpm.client.parts;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.shared.client.model.NopModelPart;
import noppes.mpm.shared.util.NopVector3f;

public class ModelPartWrapper {
   public final String name;
   protected ModelPart mcPart = null;
   protected NopModelPart mpmPart = null;
   public final NopVector3f oriPos;
   public final NopVector3f oriRot;
   public Map<EnumAnimation, AnimationContainer> animations = new HashMap<>();

   public ModelPartWrapper(String name, ModelPart mcPart, NopVector3f oriPos, NopVector3f oriRot) {
      this.name = name;
      this.mcPart = mcPart;
      this.oriRot = oriRot;
      this.oriPos = oriPos;
   }

   public ModelPartWrapper(String name, NopModelPart mpmPart, NopVector3f oriPos, NopVector3f oriRot) {
      this.name = name;
      this.mpmPart = mpmPart;
      this.oriRot = oriRot;
      this.oriPos = oriPos;
   }

   public NopVector3f getPos() {
      return this.mcPart != null
         ? new NopVector3f(this.mcPart.f_104200_, this.mcPart.f_104201_, this.mcPart.f_104202_)
         : new NopVector3f(this.mpmPart.x, this.mpmPart.y, this.mpmPart.z);
   }

   public void setPos(NopVector3f pos) {
      if (this.mcPart != null) {
         this.mcPart.m_104227_(pos.x, pos.y, pos.z);
      } else {
         this.mpmPart.setPos(pos.x, pos.y, pos.z);
      }
   }

   public NopVector3f getRot() {
      return this.mcPart != null
         ? new NopVector3f(this.mcPart.f_104203_, this.mcPart.f_104204_, this.mcPart.f_104205_)
         : new NopVector3f(this.mpmPart.xRot, this.mpmPart.yRot, this.mpmPart.zRot);
   }

   public void setRot(NopVector3f rot) {
      if (this.mcPart != null) {
         this.mcPart.m_171327_(rot.x, rot.y, rot.z);
      } else {
         this.mpmPart.setRotation(rot);
      }
   }

   public void setVisible(boolean b) {
      if (this.mcPart != null) {
         this.mcPart.f_104207_ = b;
      } else {
         this.mpmPart.visible = b;
      }
   }
}
