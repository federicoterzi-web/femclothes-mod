package com.femclothes.render;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.SkinTextures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saca el tono de piel de un jugador desde su textura de skin, para repintar
 * las zonas que las prendas dejan expuestas (panza del croptop, muslos con
 * shorts, pierna sobre la media). Hace falta repintarlas porque la skin del
 * jugador casi siempre tiene ropa dibujada encima del torso y las piernas:
 * dejar alpha 0 mostraría esa ropa pintada, no piel.
 *
 * Método: MODA (color más frecuente) sobre varias regiones que en la enorme
 * mayoría de las skins son piel, no un pixel fijo. La versión vieja
 * sampleaba solo (44,20) y su comentario decía que era "la parte interna del
 * antebrazo" — en el layout 64x64 ese pixel es en realidad la esquina
 * superior de la cara FRONTAL del brazo derecho, o sea el hombro, que en la
 * mayoría de las skins está tapado por la manga de la remera.
 *
 * Dentro de la cara los ojos, la boca y el pelo son minoría, así que la moda
 * cae en el tono de piel. Sumar los antebrazos da margen para las skins con
 * casco o máscara.
 */
public final class SkinToneSampler {

    // NativeImage empaqueta los pixeles como 0xAABBGGRR, no ARGB. Este
    // valor estaba escrito como ARGB, asi que el beige de respaldo salia
    // azulado cuando no se podia leer la skin.
    private static final int NEAR_THRESHOLD = 90;
    private static final int MIN_SPREAD = 18;

    private static final int FALLBACK_ABGR = 0xFF789AC4;

    /** Rectángulo de muestreo en la textura: [x0, y0, x1, y1] inclusive. */
    private record Region(int x0, int y0, int x1, int y1) {}

    // Cara frontal de la cabeza: capa base, x 8..15 / y 8..15.
    private static final Region FACE = new Region(8, 8, 15, 15);

    // Antebrazos: mitad BAJA de la cara frontal de cada brazo. La mitad alta
    // es el hombro, que suele ser manga. Los UV del brazo cambian entre skins
    // wide (brazo de 4px) y slim/Alex (3px), por eso hay dos juegos.
    private static final Region RIGHT_FOREARM_WIDE = new Region(44, 26, 47, 31);
    private static final Region LEFT_FOREARM_WIDE = new Region(36, 58, 39, 63);
    private static final Region RIGHT_FOREARM_SLIM = new Region(43, 26, 45, 31);
    private static final Region LEFT_FOREARM_SLIM = new Region(35, 58, 37, 63);

    private SkinToneSampler() {}

    /** Tono neutro para cuando no se puede leer la skin. */
    public static int fallbackTone() {
        return FALLBACK_ABGR;
    }

    public static int sampleSkinTone(NativeImage skinImage) {
        return sampleSkinTone(skinImage, SkinTextures.Model.WIDE);
    }

    public static int sampleSkinTone(NativeImage skinImage, SkinTextures.Model model) {
        if (skinImage == null) return FALLBACK_ABGR;
        boolean slim = model == SkinTextures.Model.SLIM;

        Map<Integer, Integer> counts = new HashMap<>();
        tally(counts, skinImage, FACE);
        tally(counts, skinImage, slim ? RIGHT_FOREARM_SLIM : RIGHT_FOREARM_WIDE);
        tally(counts, skinImage, slim ? LEFT_FOREARM_SLIM : LEFT_FOREARM_WIDE);

        int best = FALLBACK_ABGR;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return bestCount > 0 ? best : FALLBACK_ABGR;
    }

    /** Tres tonos de la misma piel, para poder sombrear en vez de rellenar plano. */
    public record Tones(int mid, int light, int dark) {}

    public static Tones fallbackTones() {
        return new Tones(FALLBACK_ABGR, scale(FALLBACK_ABGR, 1.18F), scale(FALLBACK_ABGR, 0.72F));
    }

    /**
     * Saca un tono medio (la moda) mas uno claro y uno oscuro. Sirve para
     * sombrear la piel reconstruida: con un solo color plano las dos piernas
     * se leen pegadas, sin volumen ni separacion entre una y otra.
     *
     * Los tonos claro y oscuro salen de pixeles REALES de la skin, pero solo
     * de los que estan cerca de la moda — asi el pelo, los ojos o la boca no
     * se cuelan como si fueran piel. Si la skin no tiene sombreado propio, o
     * quedan muy pocas muestras, se derivan escalando la moda.
     */
    public static Tones sampleTones(NativeImage skinImage, SkinTextures.Model model) {
        if (skinImage == null) return fallbackTones();
        boolean slim = model == SkinTextures.Model.SLIM;

        Map<Integer, Integer> counts = new HashMap<>();
        tally(counts, skinImage, FACE);
        tally(counts, skinImage, slim ? RIGHT_FOREARM_SLIM : RIGHT_FOREARM_WIDE);
        tally(counts, skinImage, slim ? LEFT_FOREARM_SLIM : LEFT_FOREARM_WIDE);
        if (counts.isEmpty()) return fallbackTones();

        int mid = FALLBACK_ABGR;
        int best = 0;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                mid = e.getKey();
            }
        }

        List<Integer> near = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (distance(e.getKey(), mid) <= NEAR_THRESHOLD) {
                for (int i = 0; i < e.getValue(); i++) near.add(e.getKey());
            }
        }
        near.sort(Comparator.comparingInt(SkinToneSampler::luminance));

        if (near.size() >= 8) {
            int dark = near.get((int) (near.size() * 0.15));
            int light = near.get((int) (near.size() * 0.85));
            // Si la skin es plana en esa zona, el sombreado propio no alcanza
            // para separar las piernas: se ensancha a mano.
            if (luminance(light) - luminance(dark) >= MIN_SPREAD) {
                return new Tones(mid, light, dark);
            }
        }
        return new Tones(mid, scale(mid, 1.18F), scale(mid, 0.72F));
    }

    private static int luminance(int abgr) {
        int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static int distance(int a, int b) {
        return Math.abs((a & 0xFF) - (b & 0xFF))
                + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
    }

    private static int scale(int abgr, float f) {
        int r = Math.min(255, Math.round((abgr & 0xFF) * f));
        int g = Math.min(255, Math.round(((abgr >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round(((abgr >> 16) & 0xFF) * f));
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static void tally(Map<Integer, Integer> counts, NativeImage img, Region r) {
        for (int y = r.y0(); y <= r.y1(); y++) {
            for (int x = r.x0(); x <= r.x1(); x++) {
                if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) continue;
                int px;
                try {
                    px = img.getColor(x, y);
                } catch (Exception e) {
                    continue;
                }
                // Pixeles transparentes no son piel (skins con zonas vacías).
                if (((px >> 24) & 0xFF) < 250) continue;
                counts.merge(px, 1, Integer::sum);
            }
        }
    }
}
