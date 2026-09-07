package com.femclothes.item;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * El eje de largo del pantalón: de las 12 filas del cuboide de pierna,
 * cuántas tienen tela — mismo mecanismo que {@code Variante.Largo} de la
 * remera (menos filas = prenda más corta). Es el eje "botamanga" del diseño
 * de cobertura de 5 valores (sin/corto/medio/largo/extralargo), con nombres
 * propios de pantalón en cada posición.
 *
 * Se craftea el pantalón LARGO (12 filas) y se recorta después con
 * {@link MoldePantalonItem} en el telar, uno por valor.
 *
 * Calzoncillos, slip y tanga se fusionaron en {@link #ROPA_INTERIOR}: son
 * lo bastante parecidos en largo que no justifican tres escalones — a pedido
 * del dueño, y porque además así quedan exactos 5 valores para calzar con el
 * mismo eje de 5 que usan medias.
 */
public enum PantalonLargo implements StringIdentifiable {

    PANTALON("pantalon", 12),
    TRES_CUARTOS("tres_cuartos", 9),
    BERMUDAS("bermudas", 7),
    SHORTS("shorts", 4),
    ROPA_INTERIOR("ropa_interior", 2);

    public final String clave;
    public final int filas;

    PantalonLargo(String clave, int filas) {
        this.clave = clave;
        this.filas = filas;
    }

    @Override
    public String asString() {
        return clave;
    }

    /** Textura de cuerpo para este largo, en layout de skin. Generada por script. */
    public Identifier texturaCuerpo() {
        return Identifier.of(Femclothes.MOD_ID, "textures/models/armor/pantalon_" + clave + "_layer_1.png");
    }

    public String traduccion() {
        return "item.femclothes.pantalon_" + clave;
    }
}
