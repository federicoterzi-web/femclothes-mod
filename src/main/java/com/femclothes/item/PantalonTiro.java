package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/**
 * El tiro del pantalón: cuánto sube la cintura sobre el cuboide de TORSO,
 * en filas de las 12 (misma unidad que {@link Botamanga}, otra región de
 * UV — ver {@code PiezasDelMod.pantalon}).
 *
 * Es un eje aparte de {@code largo} y no un valor más de largo: largo
 * decide hasta dónde llega la pierna, tiro decide dónde arranca la cintura.
 * Los dos son independientes — un pantalón de tiro alto puede ser bermudas o
 * puede ser largo, no cambia una cosa a la otra.
 */
public enum PantalonTiro implements StringIdentifiable {

    CORTO("corto", 1),
    MEDIO("medio", 2),
    LARGO("largo", 4);

    public final String clave;
    public final int filas;

    PantalonTiro(String clave, int filas) {
        this.clave = clave;
        this.filas = filas;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "femclothes.tiro." + clave;
    }
}
