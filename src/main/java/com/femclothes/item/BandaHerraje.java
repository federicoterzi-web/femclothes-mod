package com.femclothes.item;

import net.minecraft.util.StringIdentifiable;

/** Herraje del frente de la banda (2026-10-05): ninguno, una placa o un aro. Por ordinal: nuevos al final. */
public enum BandaHerraje implements StringIdentifiable {
    NINGUNO("ninguno"),
    PLACA("placa"),
    ARO("aro");

    public final String clave;

    BandaHerraje(String clave) { this.clave = clave; }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "femclothes.banda.herraje." + clave; }
}
