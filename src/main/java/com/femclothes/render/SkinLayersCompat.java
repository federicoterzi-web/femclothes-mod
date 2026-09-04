package com.femclothes.render;

import dev.tr7zw.skinlayers.accessor.HttpTextureAccessor;
import net.minecraft.client.texture.NativeImage;

/**
 * Puente con 3D Skin Layers, aislado en su propia clase para que la JVM no la
 * resuelva si el mod no esta.
 *
 * HttpTextureAccessor es una interfaz suya que pide getImage(); la heredada de
 * NativeImageBackedTexture ya cumple esa firma, asi que alcanza con declararla.
 * Con eso su instanceof da true, lee nuestra skin compuesta y reconstruye sus
 * meshes — las capas 3D siguen andando en todo el cuerpo salvo donde la prenda
 * las borro a proposito.
 *
 * Ojo: es un contrato interno de tr7zw, no una API publica versionada.
 */
final class SkinLayersCompat {

    private SkinLayersCompat() {}

    static ClothingSkinTexture create(NativeImage image) {
        return new Puente(image);
    }

    private static final class Puente extends ClothingSkinTexture implements HttpTextureAccessor {
        Puente(NativeImage image) {
            super(image);
        }
    }
}
