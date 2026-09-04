package com.femclothes.mixin;

import com.femclothes.render.ComposedSkin;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Devuelve una skin preparada para la ropa del mod.
 *
 * El enganche es getSkinTextures() y NO PlayerEntityRenderer.getTexture():
 * 3D Skin Layers lee de aca (SkinUtil.setup3dLayers -> PlayerUtil.getPlayerSkin
 * -> getSkinTextures().texture()), asi que enganchar en el renderer lo dejaria
 * construyendo su geometria 3D desde la skin original y la prenda seguiria
 * tapada.
 *
 * Si el jugador no lleva prendas que toquen la skin, ComposedSkin devuelve el
 * mismo objeto y esto no hace nada.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void femclothes$vestirSkin(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures original = cir.getReturnValue();
        SkinTextures compuesta = ComposedSkin.forPlayer(
                (AbstractClientPlayerEntity) (Object) this, original);
        if (compuesta != original) {
            cir.setReturnValue(compuesta);
        }
    }
}
