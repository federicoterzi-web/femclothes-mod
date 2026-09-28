package noppes.mpm.client.parts;

import java.util.HashMap;
import java.util.Map;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.shared.util.NopVector3f;

public class MpmPartDataClient<T extends MpmPartAbstractClient> implements IMpmPartDataClient<T> {
   private Map<String, MpmPartDataClient<T>.AniWrapper> rotTrans = new HashMap<>();

   public void start(T partc) {
      for (MpmPartDataClient<T>.AniWrapper w : this.rotTrans.values()) {
         w.startupTicks = 0;
      }
   }

   private MpmPartDataClient<T>.AniWrapper getAni(ModelPartWrapper m) {
      MpmPartDataClient<T>.AniWrapper w = this.rotTrans.get(m.name);
      if (w != null) {
         return w;
      } else {
         w = new MpmPartDataClient.AniWrapper(m.oriRot, m.oriPos);
         this.rotTrans.put(m.name, w);
         return w;
      }
   }

   public boolean animation(T partc, EnumAnimation animation, int step, float partialTick) {
      ModelPartWrapper[] models = partc.animations.get(animation);
      if (models != null) {
         for (ModelPartWrapper m : models) {
            AnimationContainer ac = m.animations.get(animation);
            float f = step / 20.0F * ac.speed;
            int i = (int)f;
            float pf = (step - 1) / 20.0F * ac.speed;
            int pi = (int)pf;
            MpmPartDataClient<T>.AniWrapper w = this.getAni(m);
            if (pi != i) {
               this.step(w, m, ac, i, (f - i) * partialTick);
            } else {
               this.step(w, m, ac, i, pf - pi + (f - pf) * partialTick);
            }

            if (w.startupTicks < ac.actualLength && i != pi) {
               w.startupTicks++;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean animation(T partc, EnumAnimation animation, float step) {
      ModelPartWrapper[] models = partc.animations.get(animation);
      if (models == null) {
         return false;
      } else {
         for (ModelPartWrapper m : models) {
            AnimationContainer ac = m.animations.get(animation);
            float f = step * ac.speed * (ac.length - 1);
            int i = (int)f;
            this.step(this.getAni(m), m, ac, i, f - i);
         }

         return true;
      }
   }

   private void step(MpmPartDataClient<T>.AniWrapper w, ModelPartWrapper part, AnimationContainer ac, int step, float progress) {
      int i = step % ac.actualLength;
      int j = (step + 1) % ac.actualLength;
      if (w.startupTicks < 60) {
         if (ac.hasRotation) {
            w.rot = w.rot.lerp(ac.rotations[i].lerp(ac.rotations[j], progress), 0.15F);
         } else {
            w.rot = w.rot.lerp(part.oriRot, 0.15F);
         }

         if (ac.hasTranslate) {
            w.pos = w.pos.lerp(ac.translates[i].lerp(ac.translates[j], progress), 0.15F);
         } else {
            w.pos = w.pos.lerp(part.oriPos, 0.15F);
         }
      } else {
         if (ac.hasRotation) {
            if (ac.loop && j < i) {
               w.rot = ac.rotations[i].subtract(NopVector3f.ROTATION).modulo(NopVector3f.ROTATION).lerp(ac.rotations[j], progress);
            } else {
               w.rot = ac.rotations[i].lerp(ac.rotations[j], progress);
            }
         }

         if (ac.hasTranslate) {
            w.pos = ac.translates[i].lerp(ac.translates[j], progress);
         }
      }

      part.setPos(w.pos);
      part.setRot(w.rot);
   }

   class AniWrapper {
      int startupTicks = Integer.MAX_VALUE;
      NopVector3f rot;
      NopVector3f pos;

      public AniWrapper(NopVector3f rot, NopVector3f pos) {
         this.rot = rot;
         this.pos = pos;
      }
   }
}
