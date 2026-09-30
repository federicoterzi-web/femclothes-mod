package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/** Borde de abajo de la capa (2026-09-29): recto como la vanilla o redondeado. Por ordinal: nuevos al final. */
public enum CapaRuedo implements StringIdentifiable {
    RECTO("recto"),
    REDONDEADO("redondeado");

    public final String clave;

    CapaRuedo(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    public String traduccion() {
        return "femclothes.capa.ruedo." + clave;
    }
}
