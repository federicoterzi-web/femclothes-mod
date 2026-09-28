package noppes.mpm.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityMixin extends EntityMixin {
   @Accessor("dead")
   boolean isDead();

   @Accessor("jumping")
   boolean isJumping();

   @Accessor("animStep")
   float getAnimStep();

   @Accessor("animStep")
   void setAnimStep(float var1);

   @Accessor("animStepO")
   float getAnimStepO();

   @Accessor("animStepO")
   void setAnimStepO(float var1);
}
