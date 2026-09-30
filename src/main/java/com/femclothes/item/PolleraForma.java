package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/**
 * Forma de la pollera (2026-09-29, "las dos, mira la que ya hay para la
 * tableada"): campana lisa o tableada (pliegues en V, como la de gajos que
 * había). Se elige con el Molde de Pollera en la Modeladora. Viaja por red
 * por ordinal: valores nuevos, siempre al final.
 */
public enum PolleraForma implements StringIdentifiable {
    CAMPANA("campana"),
    TABLEADA("tableada");

    public final String clave;

    PolleraForma(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "femclothes.pollera.forma." + clave;
    }
}
