package noppes.mpm.mixin;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntityRenderer.class})
public interface LivingRenderer2Mixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityMixin {
   @Accessor("layers")
   List<RenderLayer<T, M>> getLayers();
}
