package com.femclothes.sublimadora;

import net.minecraft.client.texture.NativeImage;

/**
 * Los cuellos que se recortan en runtime (2026-10-04, "hagamos el cambio del sistema del cuello"): cuadrado y corazón
 * no tienen un archivo por corte — se parte de la textura del cuello REDONDO del mismo largo y se le borra (alfa 0)
 * el escote del frente y el agujero de la tapa de arriba, como hacía {@code tools/generar_cuellos.py}. Sumar un cuello
 * nuevo es agregar su forma acá, sin archivos. Las coordenadas son las del atlas de la skin (64 de ancho), así que
 * sirve a cualquier escala de textura.
 *
 * <p>Formas (texeles del frente del torso, x de 0 a 8, y hacia abajo): el cuadrado va de x 2 a 6 hasta 1,75 de
 * hondo; el corazón son dos lóbulos que siguen la parte de arriba de cada pecho (centros en x 2 y 6, radio 2) y se
 * juntan en una punta al centro, de x 1 a 7, con los puntos más altos de la tela en y 3,0 y la punta en 5,0
 * ({@link #CORAZON_BAJA}; 2026-10-04, "lo bajaria para que empiecen a cubrir justo encima del medio de la teta").
 * La tapa de arriba tiene el mismo ancho que el corte del frente, las 4 filas de profundidad.
 */
public final class CuelloRecorte {

    private CuelloRecorte() {}

    /** Dónde queda la punta del medio del corazón (el borde de los lóbulos está 2 por arriba). */
    private static final float CORAZON_BAJA = 5.0f;

    public static boolean recortaEnRuntime(Variante.Cuello cuello) {
        return cuello == Variante.Cuello.CUADRADO || cuello == Variante.Cuello.CORAZON;
    }

    /** x, y en texeles del frente: ¿ese punto queda recortado? */
    private static boolean agujero(Variante.Cuello cuello, float x, float y) {
        switch (cuello) {
            case CUADRADO:
                return x >= 2f && x <= 6f && y < 1.75f;
            case CORAZON: {
                if (x < 1f || x > 7f) return false;
                float cx = x <= 4f ? 2f : 6f;
                float borde = CORAZON_BAJA - (float) Math.sqrt(Math.max(0f, 4f - (x - cx) * (x - cx)));
                return y < borde;
            }
            default:
                return false;
        }
    }

    /** Ancho (texeles, de 0 a 8) del agujero de la tapa de arriba. */
    private static float[] anchoTapa(Variante.Cuello cuello) {
        return cuello == Variante.Cuello.CORAZON ? new float[]{1f, 7f} : new float[]{2f, 6f};
    }

    /** Recorta {@code img} (la textura del cuello redondo, del atlas de la skin) con el escote de {@code cuello}. */
    public static void aplicar(NativeImage img, Variante.Cuello cuello) {
        if (!recortaEnRuntime(cuello)) return;
        float k = img.getWidth() / 64f;                // píxeles por texel de la skin
        int x0 = Math.round(20 * k), y0 = Math.round(20 * k);   // esquina del frente del torso
        int ancho = Math.round(8 * k);
        // La tapa de arriba: x como el frente, y de 16 a 20.
        float[] tapa = anchoTapa(cuello);
        for (int y = Math.round(16 * k); y < y0; y++) {
            for (int x = x0; x < x0 + ancho; x++) {
                float tx = (x - x0 + 0.5f) / k;
                if (tx >= tapa[0] && tx <= tapa[1]) img.setColor(x, y, 0);
            }
        }
        // El frente: la forma, con un texel de margen de abajo.
        int alto = Math.round(6 * k);
        for (int y = y0; y < y0 + alto && y < img.getHeight(); y++) {
            for (int x = x0; x < x0 + ancho; x++) {
                if (agujero(cuello, (x - x0 + 0.5f) / k, (y - y0 + 0.5f) / k)) img.setColor(x, y, 0);
            }
        }
    }
}
