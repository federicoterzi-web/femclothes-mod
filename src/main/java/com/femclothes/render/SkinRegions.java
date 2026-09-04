package com.femclothes.render;

import com.femclothes.item.FemclothesItems;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Que parte de la SKIN toca cada prenda.
 *
 * Esto existe porque la skin del jugador casi siempre trae ropa pintada, y
 * ademas una segunda capa (leftPants, jacket, sleeves) que vanilla dibuja
 * inflada y que 3D Skin Layers convierte en geometria 3D. Una prenda que se
 * dibuja encima queda tapada por esa capa, o compite con una remera pintada
 * que no deberia estar ahi.
 *
 * La solucion es limpiar esas zonas EN LA SKIN, una sola vez, en vez de que
 * cada prenda intente arreglarlo por su cuenta — que es lo que hacia que dos
 * prendas en la misma parte del cuerpo se pisaran.
 */
public final class SkinRegions {

    /** Un rectangulo de la skin 64x64, en pixeles. */
    public record Rect(int x0, int y0, int x1, int y1) {
        public boolean contains(int x, int y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /**
     * Lo que una prenda le hace a la skin.
     *
     * @param clearOverlay zonas donde se borra la segunda capa: ahi la prenda
     *                     manda, y ademas 3DSL deja de extruir geometria
     *                     porque solo extruye pixeles solidos.
     * @param bareSkin     zonas de la capa BASE que se repintan con el tono de
     *                     piel: la parte que la prenda deja a la vista y que
     *                     en la skin tendria ropa pintada.
     */
    public record Effect(Rect[] clearOverlay, Rect[] bareSkin) {}

    // --- Layout de skin 64x64 ---
    // Pierna derecha: base uv(0,16), overlay uv(0,32). Izquierda: base
    // uv(16,48), overlay uv(0,48). Cada una ocupa 16 de ancho por 16 de alto
    // (4 caras de 4px + las tapas de arriba y abajo).
    private static final Rect PIERNA_DER_BASE     = new Rect(0, 16, 16, 32);
    private static final Rect PIERNA_IZQ_BASE     = new Rect(16, 48, 32, 64);
    private static final Rect PIERNA_DER_OVERLAY  = new Rect(0, 32, 16, 48);
    private static final Rect PIERNA_IZQ_OVERLAY  = new Rect(0, 48, 16, 64);

    private static final Map<Item, Effect> EFECTOS = new HashMap<>();

    static {
        // Medias: ocupan las dos piernas enteras. Se borra la capa externa de
        // ambas (para que ni el pantalon pintado ni su version 3D compitan) y
        // se repinta la base con piel, porque arriba de la media va pierna
        // desnuda y la skin ahi suele tener pantalon.
        EFECTOS.put(FemclothesItems.SOCKS_SOLID, new Effect(
                new Rect[]{ PIERNA_DER_OVERLAY, PIERNA_IZQ_OVERLAY },
                new Rect[]{ PIERNA_DER_BASE, PIERNA_IZQ_BASE }));
    }

    private SkinRegions() {}

    /** null si la prenda no necesita tocar la skin. */
    public static Effect of(Item item) {
        return EFECTOS.get(item);
    }

    public static boolean afecta(Item item) {
        return EFECTOS.containsKey(item);
    }
}
