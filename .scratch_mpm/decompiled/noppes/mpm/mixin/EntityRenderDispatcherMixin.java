package noppes.mpm.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import noppes.mpm.client.ClientProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
   @Inject(
      at = {@At("TAIL")},
      method = {"onResourceManagerReload"},
      cancellable = true
   )
   private void getModelName(ResourceManager manager, CallbackInfo cir) {
      ClientProxy.fixModels();
   }
}
