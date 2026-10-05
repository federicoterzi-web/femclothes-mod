package com.femclothes.correa;

import net.minecraft.util.StringIdentifiable;

/** PEGADA: da la vuelta por la superficie de la caja entre sus dos puntos. COLGANTE: solo el 1.º punto la sujeta y cuelga con tela blanda. */
public enum ModoCorrea implements StringIdentifiable {
    PEGADA("pegada"),
    COLGANTE("colgante");

    public final String clave;

    ModoCorrea(String clave) { this.clave = clave; }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "femclothes.correa.modo." + clave; }

    public ModoCorrea siguiente() { return values()[(ordinal() + 1) % values().length]; }
}
