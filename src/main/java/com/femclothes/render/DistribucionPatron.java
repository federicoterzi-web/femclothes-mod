package com.femclothes.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Cómo se reparte un patrón además de su ángulo (2026-09-30, "me gustaria q
 * los patrones si se puedan girar y que haya un slider vertical y horizontal
 * para ponerlos mas juntos"):
 * <ul>
 *   <li>{@code giroMotivo}: cada motivo gira en su lugar (grados). El ángulo
 *       de la capa ({@code CapaPatron#angulo}) gira la grilla entera — a
 *       pedido, "los dos, por separado";</li>
 *   <li>{@code distanciaH}/{@code distanciaV}: separación horizontal/vertical
 *       como factor de la de siempre (1 = como antes; 0.5 = pegados o
 *       apenas superpuestos; 3 = el triple). En las rayas es la distancia
 *       entre rayas, mezclando las dos según hacia dónde corran.</li>
 * </ul>
 * Va aparte del {@code CapaPatron} porque su codec ya tiene los 16 campos
 * que admite {@code RecordCodecBuilder}.
 */
public record DistribucionPatron(float giroMotivo, float distanciaH, float distanciaV) {

    public static final float MINIMO = 0.5f, MAXIMO = 3.0f, PASO = 0.05f;
    public static final DistribucionPatron DEFECTO = new DistribucionPatron(0f, 1f, 1f);

    public static final Codec<DistribucionPatron> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("giro_motivo", 0f).forGetter(DistribucionPatron::giroMotivo),
            Codec.FLOAT.optionalFieldOf("distancia_h", 1f).forGetter(DistribucionPatron::distanciaH),
            Codec.FLOAT.optionalFieldOf("distancia_v", 1f).forGetter(DistribucionPatron::distanciaV)
    ).apply(i, DistribucionPatron::new));

    /** Cantidad de escalones de un slider de distancia (0.5..3.0 de a 0.05). */
    public static final int NIVELES = Math.round((MAXIMO - MINIMO) / PASO) + 1;

    public static float distanciaDeNivel(int nivel) {
        return MINIMO + Math.max(0, Math.min(NIVELES - 1, nivel)) * PASO;
    }

    public static int nivelDeDistancia(float distancia) {
        return Math.max(0, Math.min(NIVELES - 1, Math.round((distancia - MINIMO) / PASO)));
    }

    /** Clave estable para caches (redondeada a los escalones). */
    public String clave() {
        return Math.round(giroMotivo) + "/" + nivelDeDistancia(distanciaH) + "/" + nivelDeDistancia(distanciaV);
    }
}
