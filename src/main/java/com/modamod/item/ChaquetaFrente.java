package com.modamod.item;

import net.minecraft.util.StringIdentifiable;

/** Frente de la chaqueta (2026-10-07, "campera abierta, en abierto se ve lo de abajo"). Por ordinal: nuevos al final. */
public enum ChaquetaFrente implements StringIdentifiable {
    CERRADA("cerrada"),
    ABIERTA("abierta");

    public final String clave;

    ChaquetaFrente(String clave) { this.clave = clave; }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "modamod.chaqueta.frente." + clave; }
}
