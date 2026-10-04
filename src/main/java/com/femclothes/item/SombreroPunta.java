package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/** Punta del sombrero de bruja (2026-10-04): recta o doblada hacia atrás. Por ordinal: nuevos al final. */
public enum SombreroPunta implements StringIdentifiable {
    RECTA("recta", 0f, 0f),
    DOBLADA("doblada", -0.35f, -0.55f);

    public final String clave;
    /** Cuánto se inclina de fábrica cada tramo de la punta (rad; negativo = hacia atrás). */
    public final float inclinacion1, inclinacion2;

    SombreroPunta(String clave, float inclinacion1, float inclinacion2) {
        this.clave = clave;
        this.inclinacion1 = inclinacion1;
        this.inclinacion2 = inclinacion2;
    }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "femclothes.sombrero.punta." + clave; }
}
