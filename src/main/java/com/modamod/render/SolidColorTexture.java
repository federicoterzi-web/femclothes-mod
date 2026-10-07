package com.modamod.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Textura 1x1 sólida, cacheada por color ARGB, usada como relleno de piel
 * en la pasada previa al render del croptop. Un solo texel de color plano
 * combinado con el tinte (r,g,b) del render es más barato que generar una
 * NativeImage del tamaño completo del modelo por cada tono de piel.
 */
public final class SolidColorTexture {

    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    public static Identifier get(int argb) {
        return CACHE.computeIfAbsent(argb, SolidColorTexture::create);
    }

    private static Identifier create(int argb) {
        NativeImage image = new NativeImage(1, 1, false);
        image.setColor(0, 0, argb);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        Identifier id = Identifier.of("modamod", "dynamic/skin_fill_" + Integer.toHexString(argb));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        return id;
    }

    private SolidColorTexture() {}
}
