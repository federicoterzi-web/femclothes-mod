package com.femclothes.item;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * El eje de largo del pantalón: de las 12 filas del cuboide de pierna,
 * cuántas tienen tela — mismo mecanismo que {@code Variante.Largo} de la
 * remera (menos filas = prenda más corta), pero con más escalones porque acá
 * el rango real va de un pantalón largo a una tanga.
 *
 * Se craftea el pantalón LARGO (12 filas) y se recorta después con
 * {@link MoldePantalonItem} en el telar, exactamente como el corte de la
 * remera: una prenda, un molde que cicla, en vez de siete recetas.
 */
public enum PantalonLargo implements StringIdentifiable {

    PANTALON("pantalon", 12),
    TRES_CUARTOS("tres_cuartos", 9),
    BERMUDAS("bermudas", 7),
    SHORTS("shorts", 4),
    CALZONCILLOS("calzoncillos", 3),
    SLIP("slip", 2),
    TANGA("tanga", 1);

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

    public PantalonLargo siguiente() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Textura de cuerpo para este largo, en layout de skin. Generada por script. */
    public Identifier texturaCuerpo() {
        return Identifier.of(Femclothes.MOD_ID, "textures/models/armor/pantalon_" + clave + "_layer_1.png");
    }

    public String traduccion() {
        return "item.femclothes.pantalon_" + clave;
    }
}
