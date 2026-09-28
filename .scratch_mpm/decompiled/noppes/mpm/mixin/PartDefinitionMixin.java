package noppes.mpm.mixin;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PartDefinition.class})
public interface PartDefinitionMixin {
   @Accessor("partPose")
   PartPose getPose();
}
