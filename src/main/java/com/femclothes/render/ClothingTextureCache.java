package com.femclothes.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compositor genérico de texturas de prendas: lee un .png base (escala de
 * grises) y devuelve una versión teñida al color pedido, cacheada. Ya no
 * dependemos del tinte automático de "dyeable" de armadura (eso solo
 * existía en el pipeline de ArmorItem) — este es el mismo mecanismo a
 * mano para cualquier prenda que se dibuje vía BodyPartTrinketRenderer.
 */
public final class ClothingTextureCache {

    private static final Map<Identifier, NativeImage> BASE_IMAGE_CACHE = new HashMap<>();
    private static final Map<String, Identifier> TINTED_CACHE = new HashMap<>();

    private ClothingTextureCache() {}

    /** Devuelve la textura base sin modificar (para prendas sin tinte, ej. maid_outfit). */
    public static Identifier plain(Identifier baseTexture) {
        return baseTexture;
    }

    /** Tiñe toda la textura base con un solo color (medias, shorts dyeable). */
    public static Identifier tinted(Identifier baseTexture, int rgb) {
        String key = baseTexture + "#" + Integer.toHexString(rgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = getBaseImage(baseTexture);
        if (base == null) return baseTexture;

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);
        int dr = (rgb >> 16) & 0xFF;
        int dg = (rgb >> 8) & 0xFF;
        int db = rgb & 0xFF;

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int px = base.getColor(x, y);
                int a = (px >> 24) & 0xFF;
                if (a == 0) {
                    composite.setColor(x, y, 0);
                    continue;
                }
                int bChan = (px >> 16) & 0xFF;
                int gChan = (px >> 8) & 0xFF;
                int rChan = px & 0xFF;
                int tr = (rChan * dr) / 255;
                int tg = (gChan * dg) / 255;
                int tb = (bChan * db) / 255;
                composite.setColor(x, y, (a << 24) | (tb << 16) | (tg << 8) | tr);
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/tint_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /**
     * Tiñe una textura con UN color principal + rellena con OTRO color
     * (piel, o un segundo tinte) las zonas con alpha 0 de la base — usado
     * por el croptop (relleno de piel) y por prendas de dos tonos.
     */
    public static Identifier tintedWithFill(Identifier baseTexture, int mainRgb, int fillRgb) {
        String key = baseTexture + "#" + Integer.toHexString(mainRgb) + "#" + Integer.toHexString(fillRgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = getBaseImage(baseTexture);
        if (base == null) return baseTexture;

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);
        int dr = (mainRgb >> 16) & 0xFF, dg = (mainRgb >> 8) & 0xFF, db = mainRgb & 0xFF;

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int px = base.getColor(x, y);
                int a = (px >> 24) & 0xFF;
                if (a == 0) {
                    composite.setColor(x, y, fillRgb);
                    continue;
                }
                int bChan = (px >> 16) & 0xFF, gChan = (px >> 8) & 0xFF, rChan = px & 0xFF;
                int tr = (rChan * dr) / 255, tg = (gChan * dg) / 255, tb = (bChan * db) / 255;
                composite.setColor(x, y, (a << 24) | (tb << 16) | (tg << 8) | tr);
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/tintfill_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /**
     * Convención de ruta de la máscara de un patrón, por prenda:
     *   textures/models/armor/patterns/<prenda>/<patrón>.png
     * Ej: patrón "femclothes:stripe_top" sobre "socks" →
     *   femclothes:textures/models/armor/patterns/socks/stripe_top.png
     *
     * Es por prenda y no global porque la máscara depende del UV map de
     * la prenda: una raya sobre las medias cae en píxeles distintos que
     * la misma raya sobre un croptop. Agregar un patrón a una prenda =
     * soltar UN png en esa carpeta, sin tocar código.
     */
    public static Identifier patternMaskFor(String garment, Identifier patternId) {
        return Identifier.of(patternId.getNamespace(),
                "textures/models/armor/patterns/" + garment + "/" + patternId.getPath() + ".png");
    }

    /**
     * Base teñida con el color base + máscara de patrón teñida con el
     * color de patrón dibujada encima. Donde la máscara es opaca gana el
     * patrón; donde es transparente se ve la base. El patrón viene de un
     * componente del ItemStack (lo escribe el telar) en vez de estar fijo
     * en el Item — por eso una sola prenda cubre todas las variantes que
     * antes eran un Item por combinación de colores.
     *
     * Si la máscara todavía no tiene arte, cae de vuelta a la prenda lisa
     * en vez de mostrar el cuadrado de textura faltante.
     */
    public static Identifier tintedWithPattern(Identifier baseTexture, int baseRgb,
                                               Identifier patternMask, int patternRgb) {
        String key = baseTexture + "#" + Integer.toHexString(baseRgb)
                + "@" + patternMask + "#" + Integer.toHexString(patternRgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = getBaseImage(baseTexture);
        if (base == null) return baseTexture;

        NativeImage mask = getBaseImage(patternMask);
        if (mask == null) return tinted(baseTexture, baseRgb);

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int maskPx = (x < mask.getWidth() && y < mask.getHeight()) ? mask.getColor(x, y) : 0;
                if (((maskPx >> 24) & 0xFF) != 0) {
                    composite.setColor(x, y, tintPixel(maskPx, patternRgb));
                    continue;
                }
                int basePx = base.getColor(x, y);
                if (((basePx >> 24) & 0xFF) == 0) {
                    composite.setColor(x, y, 0);
                } else {
                    composite.setColor(x, y, tintPixel(basePx, baseRgb));
                }
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/pattern_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /** Multiplica un pixel ABGR por un color RGB (el tinte estilo cuero). */
    private static int tintPixel(int px, int rgb) {
        int a = (px >> 24) & 0xFF;
        int bChan = (px >> 16) & 0xFF, gChan = (px >> 8) & 0xFF, rChan = px & 0xFF;
        int dr = (rgb >> 16) & 0xFF, dg = (rgb >> 8) & 0xFF, db = rgb & 0xFF;
        return (a << 24) | (((bChan * db) / 255) << 16) | (((gChan * dg) / 255) << 8) | ((rChan * dr) / 255);
    }

    private static NativeImage getBaseImage(Identifier id) {
        if (BASE_IMAGE_CACHE.containsKey(id)) return BASE_IMAGE_CACHE.get(id);
        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                BASE_IMAGE_CACHE.put(id, null);
                return null;
            }
            try (InputStream stream = resource.get().getInputStream()) {
                NativeImage img = NativeImage.read(stream);
                BASE_IMAGE_CACHE.put(id, img);
                return img;
            }
        } catch (IOException e) {
            BASE_IMAGE_CACHE.put(id, null);
            return null;
        }
    }
}
