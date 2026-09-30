package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/**
 * Largo de la capa (2026-09-29, "Los 6 rangos"): cuánto baja desde los
 * hombros, en píxeles del modelo. Medio = 16, el largo de la capa vanilla;
 * Máximo llega al tobillo. Viaja por red por ordinal: nuevos al final.
 */
public enum CapaLargo implements StringIdentifiable {
    MINIMO("minimo", 9),
    CORTO("corto", 12),
    MEDIO("medio", 16),
    MEDIOLARGO("mediolargo", 18),
    LARGO("largo", 21),
    MAXIMO("maximo", 24);

    public final String clave;
    public final int pixeles;

    CapaLargo(String clave, int pixeles) {
        this.clave = clave;
        this.pixeles = pixeles;
    }

    public float fraccion() {
        return pixeles / (float) MAXIMO.pixeles;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "femclothes.capa.largo." + clave;
    }
}
