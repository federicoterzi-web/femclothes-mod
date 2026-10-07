package com.femclothes.render;

import com.femclothes.Femclothes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Los detalles del hoodie pintados SOBRE la tela ya compuesta (color,
 * patrones, estampas, red): bolsillo canguro al frente y el tejido
 * acanalado (rib) de los puños y del ruedo — 2026-09-30, "bolsillo
 * canguro" / "puños y ruedo elásticos". Mismo criterio que la banda de
 * cintura del pantalón ({@code PiezasDelMod#pintarCintura}): en runtime,
 * oscureciendo la tela que ya está, así sigue al color y a cualquier
 * estampa sin un PNG por combinación.
 *
 * <p>Trabaja en el layout de SKIN a la escala real de la imagen
 * ({@code ancho / 64}): torso en (16,16) de 8x12x4, brazo derecho en
 * (40,16), izquierdo en (32,48), ambos de 4x12x4. La fila {@code f} del
 * costado de una caja cae en {@code v = vCaja + prof + f}.
 */
public final class DetallesHoodie {

    private DetallesHoodie() {}

    private static final Map<String, Identifier> CACHE = new HashMap<>();

    /**
     * La tela con los detalles pintados, o {@code base} si no se pudo leer.
     * El resultado se registra con {@link ClothingTextureCache#registrarImagenCompuesta}
     * para que el recorte de mangas ({@code PiezasDelMod#recortarMangaYCachear})
     * lo pueda volver a leer.
     */
    public static Identifier pintar(Identifier base, int filasTorso, int filasIzq, int filasDer) {
        String key = base + "#hoodie" + filasTorso + "_" + filasIzq + "_" + filasDer;
        Identifier cacheada = CACHE.get(key);
        if (cacheada != null) return cacheada;
        NativeImage origen = ClothingTextureCache.imagenBase(base);
        if (origen == null) return base;

        NativeImage img = new NativeImage(origen.getWidth(), origen.getHeight(), true);
        img.copyFrom(origen);
        int s = img.getWidth() / LayoutSkin.LADO;

        if (filasTorso > 0) {
            // Ruedo: la última fila del torso, alrededor de las 4 caras.
            rib(img, s, 16, 20 + filasTorso - 1, 24);
            bolsillo(img, s, filasTorso);
        }
        if (filasDer > 0) rib(img, s, 40, 20 + filasDer - 1, 16);
        if (filasIzq > 0) rib(img, s, 32, 52 + filasIzq - 1, 16);

        Identifier id = Identifier.of(Femclothes.MOD_ID, "dynamic/hoodie_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        ClothingTextureCache.registrarImagenCompuesta(id, img);
        CACHE.put(key, id);
        return id;
    }

    /** Una fila de skin ({@code v}) de {@code ancho} píxeles desde {@code u}, con canales verticales. */
    private static void rib(NativeImage img, int s, int u, int v, int ancho) {
        int paso = Math.max(2, s / 2);
        for (int y = v * s; y < (v + 1) * s; y++) {
            for (int x = u * s; x < (u + ancho) * s; x++) {
                oscurecer(img, x, y, (x / paso) % 2 == 0 ? 0.78F : 0.90F);
            }
        }
    }

    /**
     * Bolsillo canguro en la cara de adelante del torso (u 20..28): 3 filas
     * justo arriba del ruedo, 6 píxeles de ancho, con las bocas en diagonal
     * a los costados. Un torso de menos de 7 filas (crop) no lleva bolsillo.
     */
    private static void bolsillo(NativeImage img, int s, int filasTorso) {
        if (filasTorso < 7) return;
        int x0 = 21 * s, x1 = 27 * s;
        int y0 = (20 + filasTorso - 4) * s, y1 = (20 + filasTorso - 1) * s;
        int borde = Math.max(1, s / 4);
        int alto = y1 - y0;
        for (int y = y0; y < y1; y++) {
            // Las bocas: la tela del bolsillo se angosta hacia arriba.
            int entrada = (int) ((y1 - y) / (float) alto * s);
            int a = x0 + entrada, b = x1 - entrada;
            for (int x = a; x < b; x++) {
                boolean costura = y < y0 + borde || y >= y1 - borde || x < a + borde || x >= b - borde;
                oscurecer(img, x, y, costura ? 0.70F : 0.93F);
            }
        }
    }

    /** Multiplica el RGB de un texel (formato ABGR de NativeImage) sin tocar el alfa ni los huecos. */
    private static void oscurecer(NativeImage img, int x, int y, float f) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        int c = img.getColor(x, y);
        int a = c >>> 24;
        if (a == 0) return;
        int b = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), r = (int) ((c & 0xFF) * f);
        img.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
    }
}
