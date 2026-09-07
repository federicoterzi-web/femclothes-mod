package com.femclothes.item;

import com.femclothes.Femclothes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * El largo de las medias: de las 12 filas del cuboide de pierna, cuántas
 * tienen tela — pero al REVÉS que {@link PantalonLargo}: una media arranca
 * en el TOBILLO y crece hacia arriba, un pantalón arranca en la CINTURA y
 * crece hacia abajo. Verificado contra el asset real: la textura validada
 * de siempre (`socks_solid_layer_1.png`) cubre 10 de las 12 filas contadas
 * desde el tobillo — por eso {@link #CANCAN} vale 10 y no 12.
 *
 * Es el eje "cobertura" (sin/corto/medio/largo/extralargo) con nombres
 * propios de medias en cada posición — mismo diseño que el largo de
 * pantalón, eje independiente y no compartido (moldes propios).
 */
public enum MediasLargo implements StringIdentifiable {

    ZOQUETES("zoquetes", 2),
    MEDIAS("medias", 4),
    RODILLA("rodilla", 6),
    TRES_CUARTOS("tres_cuartos", 8),
    CANCAN("cancan", 10);

    public final String clave;
    public final int filas;

    MediasLargo(String clave, int filas) {
        this.clave = clave;
        this.filas = filas;
    }

    @Override
    public String asString() {
        return clave;
    }

    /** Textura de cuerpo para este largo, en layout de skin. Generada por script. */
    public Identifier texturaCuerpo() {
        return Identifier.of(Femclothes.MOD_ID, "textures/models/armor/medias_" + clave + "_layer_1.png");
    }

    public String traduccion() {
        return "femclothes.medias." + clave;
    }

    /**
     * Sin clase de ítem dedicada para medias (a diferencia de pantalón, el
     * nombre no cambia con el largo) — estos dos estáticos son el lugar
     * único de lectura/escritura, para no repetir la lógica de default en
     * cada llamador.
     */
    public static MediasLargo de(ItemStack stack) {
        MediasLargo l = stack.get(FemclothesComponents.MEDIAS_LARGO);
        return l == null ? CANCAN : l;
    }

    public static void aplicar(ItemStack stack, MediasLargo largo) {
        // CANCAN es el default: no guardar nada mantiene compatible una
        // media vieja (o recien crafteada) sin componente.
        if (largo == CANCAN) stack.remove(FemclothesComponents.MEDIAS_LARGO);
        else stack.set(FemclothesComponents.MEDIAS_LARGO, largo);
    }
}
