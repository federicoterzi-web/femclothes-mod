package com.femclothes.render;

import com.femclothes.item.FemclothesItems;
import com.femclothes.sublimadora.ModItems;
import com.femclothes.sublimadora.RemeraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
     * Una zona a repintar con piel, con el ancho de las caras del cuboide que
     * la componen, en orden desde x0.
     *
     * Hacen falta para sombrear: una zona pintada de un solo color se ve
     * como un recorte de papel. Sabiendo donde empieza y termina cada cara se
     * le puede dar volumen, oscura en los bordes y clara en el medio, que es
     * como se ve una caja iluminada.
     */
    public record Piel(Rect zona, Franja[] caras) {}

    /**
     * Una cara del cuboide dentro de una zona: cuanto mide de ancho y que tan
     * iluminada esta, de 0 a 1.
     *
     * Los valores siguen la convencion con la que estan pintadas las skins de
     * Minecraft: el frente y la espalda al tono base, los costados mas
     * oscuros, la tapa de arriba mas clara y la de abajo la mas oscura. Es
     * plano por cara y no un degrade, que es como se ve una caja de verdad y
     * como esta pintado el resto del pixel art.
     */
    public record Franja(int ancho, float nivel) {}

    public static final float FRENTE = 0.55f;
    public static final float ATRAS = 0.50f;
    public static final float LADO = 0.32f;
    public static final float ARRIBA = 0.85f;
    public static final float ABAJO = 0.15f;

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
    public record Effect(Rect[] clearOverlay, Piel[] bareSkin) {}

    // --- Layout de skin 64x64 ---
    // Pierna derecha: base uv(0,16), overlay uv(0,32). Izquierda: base
    // uv(16,48), overlay uv(0,48). Cada una ocupa 16 de ancho por 16 de alto
    // (4 caras de 4px + las tapas de arriba y abajo).
    private static final Rect PIERNA_DER_BASE     = new Rect(0, 16, 16, 32);
    private static final Rect PIERNA_IZQ_BASE     = new Rect(16, 48, 32, 64);
    private static final Rect PIERNA_DER_OVERLAY  = new Rect(0, 32, 16, 48);
    private static final Rect PIERNA_IZQ_OVERLAY  = new Rect(0, 48, 16, 64);

    // Torso: base uv(16,16), overlay uv(16,32). 24 de ancho por 16 de alto,
    // contando las cuatro caras mas las tapas.
    private static final Rect TORSO_OVERLAY = new Rect(16, 32, 40, 48);
    // Brazos: derecho base uv(40,16) y overlay uv(40,32); izquierdo base
    // uv(32,48) y overlay uv(48,48).
    private static final Rect BRAZO_DER_OVERLAY = new Rect(40, 32, 56, 48);
    private static final Rect BRAZO_IZQ_OVERLAY = new Rect(48, 48, 64, 64);

    /** Las cuatro laterales de un brazo o una pierna: der, frente, izq, atras. */
    private static final Franja[] CUATRO = {
            new Franja(4, LADO), new Franja(4, FRENTE),
            new Franja(4, LADO), new Franja(4, ATRAS) };
    /** Las del torso, que es mas ancho de frente y de atras. */
    private static final Franja[] TORSO_CARAS = {
            new Franja(4, LADO), new Franja(8, FRENTE),
            new Franja(4, LADO), new Franja(8, ATRAS) };
    /**
     * Solo la tapa de arriba del torso. La de abajo no se toca: esta a la
     * altura de la cintura y ahi manda el pantalon.
     */
    private static final Franja[] UNA_OCHO = { new Franja(8, ARRIBA) };

    /**
     * Hasta que fila del torso llega la prenda antes de empezar el pantalon.
     * De las 12 del cuboide, las ultimas tres son la cintura.
     */
    private static final int CINTURA = 9;
    private static final Franja[] DOS_CUATRO = {
            new Franja(4, ARRIBA), new Franja(4, ABAJO) };

    private static final Map<Item, Effect> EFECTOS = new HashMap<>();

    static {
        // Medias: ocupan las dos piernas enteras. Se borra la capa externa de
        // ambas (para que ni el pantalon pintado ni su version 3D compitan) y
        // se repinta la base con piel, porque arriba de la media va pierna
        // desnuda y la skin ahi suele tener pantalon.
        EFECTOS.put(FemclothesItems.SOCKS_SOLID, new Effect(
                new Rect[]{ PIERNA_DER_OVERLAY, PIERNA_IZQ_OVERLAY },
                new Piel[]{ new Piel(PIERNA_DER_BASE, CUATRO),
                            new Piel(PIERNA_IZQ_BASE, CUATRO) }));
    }

    private SkinRegions() {}

    /**
     * null si la prenda no necesita tocar la skin.
     *
     * Recibe el ItemStack y no el Item porque la remera es UN solo item con
     * muchos cortes, y el corte vive en un componente del stack.
     */
    public static Effect of(ItemStack stack) {
        if (stack.getItem() == ModItems.REMERA) return REMERA;
        return EFECTOS.get(stack.getItem());
    }

    /**
     * Lo que la remera le hace a la skin.
     *
     * La regla es la misma que con las medias: donde la prenda NO llega, la
     * skin se repinta con tono de piel. Sin esto, un croptop deja a la vista
     * la remera pintada de la skin en vez de la panza, y una musculosa
     * muestra mangas pintadas donde tendria que haber brazo.
     *
     * Es UNA sola para los 36 cortes, y no una por corte, porque se repinta
     * el torso y los brazos ENTEROS, no solo lo que la prenda deja asomar.
     * Recortar la piel al borde de la tela dejaba la skin vieja visible por
     * el escote y las sisas, y por la costura de abajo cuando la prenda se
     * mueve. Debajo de la tela no se ve nada, asi que pintar de mas no cuesta
     * nada y pintar de menos se nota — y eso vale igual con mangas largas que
     * sin mangas.
     *
     * La unica excepcion es hacia abajo: el torso se repinta HASTA LA
     * CINTURA. Repintarlo completo borraria la cintura del pantalon, que en
     * una skin va pintada en las ultimas filas del TORSO y no en las piernas.
     * La prenda manda de los hombros a la cintura, el pantalon de ahi abajo.
     */
    private static final Effect REMERA = new Effect(
            new Rect[]{ TORSO_OVERLAY, BRAZO_DER_OVERLAY, BRAZO_IZQ_OVERLAY },
            new Piel[]{
                    new Piel(new Rect(20, 16, 28, 20), UNA_OCHO),           // hombros
                    new Piel(new Rect(16, 20, 40, 20 + CINTURA), TORSO_CARAS),
                    new Piel(new Rect(44, 16, 52, 20), DOS_CUATRO),         // tapas brazo der
                    new Piel(new Rect(40, 20, 56, 32), CUATRO),             // brazo der
                    new Piel(new Rect(36, 48, 44, 52), DOS_CUATRO),         // tapas brazo izq
                    new Piel(new Rect(32, 52, 48, 64), CUATRO),             // brazo izq
            });

    public static boolean afecta(Item item) {
        return item == ModItems.REMERA || EFECTOS.containsKey(item);
    }

    /**
     * Con que identificar la prenda al cachear la skin compuesta.
     *
     * El item solo no alcanza desde que hay cortes: dos remeras distintas
     * darian la misma clave y la segunda reusaria la piel recortada de la
     * primera.
     */
    public static String clave(ItemStack stack) {
        if (stack.getItem() == ModItems.REMERA) {
            return "remera:" + RemeraItem.variante(stack).clave();
        }
        return String.valueOf(stack.getItem());
    }
}
