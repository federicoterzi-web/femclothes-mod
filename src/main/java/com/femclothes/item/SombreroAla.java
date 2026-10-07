package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/** Ala del sombrero de bruja (2026-10-04). Por ordinal: nuevos al final. */
public enum SombreroAla implements StringIdentifiable {
    ANCHA("ancha", 19f),
    CORTA("corta", 13f);

    public final String clave;
    /** Lado del ala en píxeles del modelo. */
    public final float lado;

    SombreroAla(String clave, float lado) {
        this.clave = clave;
        this.lado = lado;
    }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "femclothes.sombrero.ala." + clave; }
}
