package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/**
 * Largo de la pollera — a pedido (2026-09-29, "quiero poder hacerle
 * distintos largos"): los mismos 6 escalones que el Molde de Rango del
 * pantalón ("Los 6 rangos del pantalón"). {@link #pixeles} = cuánto baja
 * el ruedo desde la cintura (fila 9 del torso), en píxeles del modelo: la
 * rodilla cae en 18 (Medio) y el tobillo en 24 (Máximo). Viaja por red
 * por ordinal: valores nuevos, siempre al final.
 */
public enum PolleraLargo implements StringIdentifiable {
    MINIMO("minimo", 5),
    CORTO("corto", 7),
    MEDIO("medio", 9),
    MEDIOLARGO("mediolargo", 11),
    LARGO("largo", 13),
    MAXIMO("maximo", 15);

    public final String clave;
    public final int pixeles;

    PolleraLargo(String clave, int pixeles) {
        this.clave = clave;
        this.pixeles = pixeles;
    }

    /** Fracción del largo máximo (para recortar el ícono). */
    public float fraccion() {
        return pixeles / (float) MAXIMO.pixeles;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "femclothes.pollera.largo." + clave;
    }
}
