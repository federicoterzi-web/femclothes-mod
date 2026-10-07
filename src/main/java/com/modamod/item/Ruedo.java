package com.modamod.item;

import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

/**
 * Remate de un borde libre de una prenda (2026-10-07, "un ruedo ajustado que podria servir para ruedo de remera mangas
 * botamangas y hasta polleras" + "fusionemos borde pollera con ruedo asi tenemos mas opciones"): un solo molde y un solo
 * pin por zona ({@link ZonaRuedo}). Reemplaza al remate de la chaqueta ({@code ChaquetaRemate}) y al borde de la
 * pollera (el molde de borde). Por ordinal: nuevos al final.
 *
 * <p>RECTO, AJUSTADO y CAMPANA valen en cualquier zona de tela; ONDULADO, FESTONEADO y PICO cambian el largo a lo largo
 * de la vuelta y hoy solo los dibuja la pollera ({@link PolleraBorde}).
 */
public enum Ruedo implements StringIdentifiable {
    RECTO("recto"),
    /** Elástico: la última fila aprieta hacia el cuerpo y la tela no cuelga (puños y ruedo del hoodie). */
    AJUSTADO("ajustado"),
    /** La tela se abre hacia afuera en las últimas filas. */
    CAMPANA("campana"),
    ONDULADO("ondulado"),
    FESTONEADO("festoneado"),
    PICO("pico");

    public final String clave;

    Ruedo(String clave) { this.clave = clave; }

    @Override
    public String asString() { return clave; }

    public String traduccion() { return "modamod.ruedo." + clave; }

    /** El borde decorativo de la pollera que corresponde, o null si no cambia el largo de la vuelta. */
    @Nullable
    public PolleraBorde borde() {
        return switch (this) {
            case ONDULADO -> PolleraBorde.ONDULADO;
            case FESTONEADO -> PolleraBorde.FESTONEADO;
            case PICO -> PolleraBorde.PICO;
            default -> null;
        };
    }

    /** Si vale para una zona que no es la pollera (el borde decorativo hoy solo existe ahí). */
    public boolean valeEnTela() { return borde() == null; }
}
