package noppes.mpm.client.parts;

import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.shared.util.NopVector3f;

public class AnimationContainer {
   public final EnumAnimation animation;
   public final String part;
   public final int length;
   public final int actualLength;
   public final float speed;
   public final boolean additional;
   public final boolean loop;
   public boolean hasRotation = false;
   public boolean hasTranslate = false;
   public final NopVector3f[] rotations;
   public final NopVector3f[] translates;

   public AnimationContainer(EnumAnimation animation, String part, int length, float speed, boolean additional, boolean loop) {
      this.animation = animation;
      this.part = part;
      this.length = length;
      this.speed = speed;
      this.additional = additional;
      this.loop = loop;
      if (loop) {
         this.actualLength = length;
      } else {
         this.actualLength = length > 2 ? length * 2 - 2 : length;
      }

      this.rotations = new NopVector3f[this.actualLength];
      this.translates = new NopVector3f[this.actualLength];
   }

   public void animation(MpmPartDataClient data, ModelPartWrapper part, int step, float partialTick) {
   }

   public void animation(MpmPartData data, ModelPartWrapper part, float step) {
   }

   public AnimationContainer copy() {
      AnimationContainer container = new AnimationContainer(this.animation, this.part, this.length, this.speed, this.additional, this.loop);
      container.hasRotation = this.hasRotation;
      container.hasTranslate = this.hasTranslate;

      for (int i = 0; i < this.actualLength; i++) {
         if (i < this.length) {
            if (this.hasTranslate) {
               container.translates[i] = this.translates[i];
            }

            if (this.hasRotation) {
               container.rotations[i] = this.rotations[i];
            }
         } else {
            if (this.hasTranslate) {
               container.translates[i] = container.translates[this.length - i % this.length - 2];
            }

            if (this.hasRotation) {
               container.rotations[i] = container.rotations[this.length - i % this.length - 2];
            }
         }
      }

      return container;
   }
}
