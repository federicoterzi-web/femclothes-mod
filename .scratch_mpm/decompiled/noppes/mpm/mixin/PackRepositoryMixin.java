package noppes.mpm.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackRepository;
import noppes.mpm.MorePlayerModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PackRepository.class})
public class PackRepositoryMixin {
   @Inject(
      at = {@At("TAIL")},
      method = {"openAllSelected"},
      cancellable = true
   )
   private void reload(CallbackInfoReturnable<List<PackResources>> ci) {
      List<PackResources> l = new ArrayList<>((Collection<? extends PackResources>)ci.getReturnValue());
      l.add(new PathPackResources(MorePlayerModels.dir.getName(), MorePlayerModels.dir.toPath(), false));
      ci.setReturnValue(l);
   }
}
