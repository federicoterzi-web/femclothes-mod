package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/** Remate de puños y ruedo de la chaqueta (2026-10-07): elástico (hoodie) o recto (campera, chaleco). Por ordinal: nuevos al final. */
public enum ChaquetaRemate implements StringIdentifiable {
    ELASTICO("elastico"),
    RECTO("recto");

    public final String clave;

    ChaquetaRemate(String clave) { this.clave = clave; }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "femclothes.chaqueta.remate." + clave; }
}
