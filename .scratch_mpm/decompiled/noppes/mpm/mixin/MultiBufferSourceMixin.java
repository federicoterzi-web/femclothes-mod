package noppes.mpm.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import noppes.mpm.shared.client.model.util.BatchRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BufferSource.class})
public class MultiBufferSourceMixin {
   @Inject(
      at = {@At("TAIL")},
      method = {"getBuffer"}
   )
   private void getBuffer(RenderType type, CallbackInfoReturnable<VertexConsumer> callbackInfo) {
      BatchRenderer.lastType = type;
   }
}
