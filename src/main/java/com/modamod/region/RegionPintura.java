package com.modamod.region;

import com.modamod.garment.Parte;
import com.modamod.render.CajaSkin;
import com.modamod.render.LayoutSkin;
import com.modamod.tinturas.TinturasBlockEntity.Categoria;
import net.minecraft.util.StringIdentifiable;

import java.util.List;

/**
 * A qué región anatómica se restringe una capa de color de Tinturas
 * (2026-09-27, "reusar los casilleros de la modeladora para pintar
 * mangas, borde de abajo, cuello etc"). {@link #TODO} es toda la prenda
 * — el comportamiento de siempre, sin recorte — el resto recorta la capa
 * a una banda de filas de una {@link Parte}, mismo cálculo de fila→pixel
 * que ya usa {@code ClothingTextureCache#perforarRed} para el refuerzo de
 * borde de la red.
 */
public enum RegionPintura implements StringIdentifiable {
    TODO("todo"),
    CUELLO("cuello"),
    MANGA_IZQ("manga_izq"), MANGA_DER("manga_der"),
    BORDE_INFERIOR("borde_inferior"),
    SUP_IZQ("sup_izq"), SUP_DER("sup_der"),
    INF_IZQ("inf_izq"), INF_DER("inf_der"),
    // 2026-09-27, capas por cuadradito: el cuadradito del medio de la
    // remera (MAT3) pinta el cuerpo entre cuello y borde, y el Tiro del
    // pantalón pinta la cintura (banda de torso + arranque de las piernas).
    PECHO("pecho"), CINTURA("cintura"),
    // La capa (2026-09-29, "forro aparte"): exterior, forro y detalles
    // (capucha + cuello alto), cada uno con su cuadradito en Tintes. Rects
    // propios, no salen de una Parte (ver render.CapaMalla).
    CAPA_EXTERIOR("capa_exterior"), CAPA_FORRO("capa_forro"), CAPA_DETALLES("capa_detalles");

    public final String clave;

    RegionPintura(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "modamod.region_pintura." + clave;
    }

    /** Filas (de las 12 de la caja) que ocupa el cuello/borde inferior — aproximado, ajustable a ojo jugando. */
    private static final int FILAS_CUELLO = 2;
    private static final int FILAS_BORDE = 2;

    /** Filas de frente/espalda donde puede caer el recorte del escote (la V baja hasta acá). */
    private static final int FILAS_ESCOTE = 6;

    /** El Cuello además de sus rects necesita la máscara del borde del escote (depende del molde de cuello de la prenda). */
    public boolean requiereBordeCuello() {
        return this == CUELLO;
    }

    /** Filas de TORSO que cubre la cintura del pantalón — alcanza para el tiro más alto. */
    private static final int FILAS_CINTURA = 6;
    /** Filas de arranque de pierna que suma la cintura, para que se vea aunque el tiro sea bajo. */
    private static final int FILAS_CINTURA_PIERNA = 2;

    /**
     * La {@link Parte} real a recortar — depende de la categoría porque
     * SUP_ / INF_ significan "pierna" en pantalón/medias pero "brazo" en
     * calientabrazos (mismo eje de anclaje, Parte distinta).
     */
    public Parte parte(Categoria cat) {
        return switch (this) {
            case TODO, CAPA_EXTERIOR, CAPA_FORRO, CAPA_DETALLES -> null;
            case CUELLO, BORDE_INFERIOR, PECHO, CINTURA -> Parte.TORSO;
            case MANGA_IZQ -> Parte.BRAZO_IZQ;
            case MANGA_DER -> Parte.BRAZO_DER;
            case SUP_IZQ, INF_IZQ -> cat == Categoria.CALIENTABRAZOS ? Parte.BRAZO_IZQ : Parte.PIERNA_IZQ;
            case SUP_DER, INF_DER -> cat == Categoria.CALIENTABRAZOS ? Parte.BRAZO_DER : Parte.PIERNA_DER;
        };
    }

    /** Filas [desde,hasta) de las 12 de esa {@link Parte} que ocupa esta región. */
    private int[] filas() {
        return switch (this) {
            case TODO, MANGA_IZQ, MANGA_DER, CAPA_EXTERIOR, CAPA_FORRO, CAPA_DETALLES -> new int[]{0, 12};
            case CUELLO -> new int[]{0, FILAS_CUELLO};
            case BORDE_INFERIOR -> new int[]{12 - FILAS_BORDE, 12};
            case SUP_IZQ, SUP_DER -> new int[]{0, 6};
            case INF_IZQ, INF_DER -> new int[]{6, 12};
            // Desde la fila 0: el Cuello ya no es una banda de filas sino el
            // borde del escote (se pinta encima, ver requiereBordeCuello).
            case PECHO -> new int[]{0, 12 - FILAS_BORDE};
            case CINTURA -> new int[]{12 - FILAS_CINTURA, 12};
        };
    }

    /**
     * Rects reales de píxeles del atlas para esta región, a la escala
     * dada — {@code null} si es {@link #TODO} (sin recorte). Manga usa
     * el desdoblado COMPLETO del brazo (tapas incluidas, no hay más
     * subdivisión pedida); cuello/borde/sup/inf recortan solo la banda de
     * caras (der+frente+izq+atrás juntas) a las filas que les tocan.
     */
    public List<CajaSkin.Rect> rects(Categoria cat, int escala) {
        // Capa: en px de la textura de 64 (cuboide 10x16x1 en uv 0,0 y
        // detalles 12x8x2 en uv 24,0). Exterior = cara del frente + la tapa
        // de arriba y el canto derecho; forro = cara de atrás + tapa de abajo
        // y canto izquierdo.
        switch (this) {
            case CAPA_EXTERIOR -> {
                return List.of(new CajaSkin.Rect(0, escala, 11 * escala, 17 * escala),
                        new CajaSkin.Rect(escala, 0, 11 * escala, escala));
            }
            case CAPA_FORRO -> {
                return List.of(new CajaSkin.Rect(11 * escala, escala, 22 * escala, 17 * escala),
                        new CajaSkin.Rect(11 * escala, 0, 21 * escala, escala));
            }
            case CAPA_DETALLES -> {
                return List.of(new CajaSkin.Rect(24 * escala, 0, 52 * escala, 10 * escala));
            }
            default -> { }
        }
        Parte parte = parte(cat);
        if (parte == null) return null;
        CajaSkin caja = LayoutSkin.base(parte, false).escalada(escala);
        if (this == MANGA_IZQ || this == MANGA_DER) {
            return List.of(caja.todo());
        }
        List<CajaSkin.Rect> rects = new java.util.ArrayList<>(4);
        if (this == CUELLO) {
            // Zona CANDIDATA: la tapa de arriba y las primeras filas del
            // frente y la espalda (donde vive el recorte del escote, redondo
            // o en V). El borde de verdad lo recorta después la máscara de
            // ClothingTextureCache#mascaraBordeCuello, que sigue el alfa real
            // del molde de cuello (2026-09-28, "cuello es solo el borde del
            // cuello segun el patron").
            rects.add(caja.arriba());
            CajaSkin.Rect frente = caja.frente(), atras = caja.atras();
            rects.add(new CajaSkin.Rect(frente.x0(), frente.y0(), frente.x1(), frente.y0() + FILAS_ESCOTE * escala));
            rects.add(new CajaSkin.Rect(atras.x0(), atras.y0(), atras.x1(), atras.y0() + FILAS_ESCOTE * escala));
            return rects;
        }
        rects.add(banda(caja, filas(), escala));
        // Tapas (2026-09-28, "la parte de arriba de la remera no se esta
        // pintando"): la de arriba del torso va con el Pecho (los hombros de
        // verdad son la tapa de cada brazo, que ya es de la Manga); la de
        // abajo con el Borde inferior. En pierna/brazo, la de abajo (planta,
        // puño) va con Inferior y la de arriba con Superior.
        switch (this) {
            case PECHO, SUP_IZQ, SUP_DER -> rects.add(caja.arriba());
            case BORDE_INFERIOR, INF_IZQ, INF_DER -> rects.add(caja.abajo());
            default -> { }
        }
        if (this == CINTURA) {
            for (Parte pierna : new Parte[]{Parte.PIERNA_IZQ, Parte.PIERNA_DER}) {
                rects.add(banda(LayoutSkin.base(pierna, false).escalada(escala),
                        new int[]{0, FILAS_CINTURA_PIERNA}, escala));
            }
        }
        return rects;
    }

    /** La banda de caras (der+frente+izq+atrás) de {@code caja} recortada a las filas [desde,hasta). */
    private static CajaSkin.Rect banda(CajaSkin caja, int[] filas, int escala) {
        CajaSkin.Rect caras = caja.caras();
        return new CajaSkin.Rect(caras.x0(), caras.y0() + filas[0] * escala, caras.x1(), caras.y0() + filas[1] * escala);
    }
}
