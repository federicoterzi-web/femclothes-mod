package noppes.mpm.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Entity.class})
public interface EntityMixin {
   @Accessor("vehicle")
   Entity getVehicle();

   @Accessor("vehicle")
   void setVehicle(Entity var1);

   @Accessor("dimensions")
   EntityDimensions getDimensions();

   @Accessor("dimensions")
   void setDimensions(EntityDimensions var1);

   @Accessor("eyeHeight")
   void setEyeHeight(float var1);
}
