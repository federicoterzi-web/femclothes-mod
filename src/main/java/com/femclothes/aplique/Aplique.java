package com.femclothes.aplique;

import com.femclothes.garment.Parte;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Un aplique puesto en una prenda (2026-10-01, "poder poner cualquier prenda y
 * agregarles modelos 3d anclados en la geometria de la prenda").
 *
 * <p>La posición es la del click en la Mesa de estilado: un punto sobre la
 * SUPERFICIE de la caja de esa {@link Parte} (sin inflar por el calce), en
 * píxeles de skin del espacio local de la parte (el de {@code ModelPart}: y
 * crece hacia los pies, -z es el frente), y la cara que se tocó ({@code cara},
 * la normal hacia afuera en ese mismo espacio). Al dibujar, el aplique sigue
 * la pose de la parte y se corre hacia afuera lo que infle el calce.
 *
 * @param giro    grados alrededor de la normal
 * @param escala  1 = tamaño del modelo
 * @param colores las 3 zonas (RGB), del retazo con que se puso
 * @param blandura 0..1: cuánto se mueve como tela blanda con el movimiento
 *                 (2026-10-04, "tela blanda afectada por el movimiento... configurable por
 *                 aplique"); 0 = rígido. Ver {@code render.FisicaApliques}.
 */
public record Aplique(ModeloAplique modelo, Parte parte, float x, float y, float z, Direction cara,
                      float giro, float escala, List<Integer> colores, Superficie superficie, float blandura,
                      @org.jetbrains.annotations.Nullable ObjetoAplique objeto) {

    public static final int MAXIMO_POR_PRENDA = 6;

    /**
     * Sobre qué está puesto (2026-10-02, "Pollera y Capa no registran click on
     * garment"): en una caja de parte del cuerpo ({@code CAJA}: x, y, z en px
     * de la parte, como siempre) o en la malla de la pollera o de la capa
     * ({@code x}, {@code y} = u, v de la textura de la tela en px de 64; se
     * ubica sobre la malla del cuadro con {@code render.MallaCapturada}).
     * Va al final del enum si se agregan más.
     */
    public enum Superficie implements net.minecraft.util.StringIdentifiable {
        CAJA, POLLERA, CAPA;

        @Override
        public String asString() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Aplique {
        blandura = Math.max(0f, Math.min(1f, blandura));
    }

    /** Sin objeto (un modelo GeckoLib de siempre). */
    public Aplique(ModeloAplique modelo, Parte parte, float x, float y, float z, Direction cara,
                   float giro, float escala, List<Integer> colores, Superficie superficie, float blandura) {
        this(modelo, parte, x, y, z, cara, giro, escala, colores, superficie, blandura, null);
    }

    /** Rígido (lo de antes de la tela blanda). */
    public Aplique(ModeloAplique modelo, Parte parte, float x, float y, float z, Direction cara,
                   float giro, float escala, List<Integer> colores, Superficie superficie) {
        this(modelo, parte, x, y, z, cara, giro, escala, colores, superficie, 0f);
    }

    /** En una caja de parte del cuerpo (lo de siempre). */
    public Aplique(ModeloAplique modelo, Parte parte, float x, float y, float z, Direction cara,
                   float giro, float escala, List<Integer> colores) {
        this(modelo, parte, x, y, z, cara, giro, escala, colores, Superficie.CAJA);
    }

    public static final Codec<Parte> CODEC_PARTE = Codec.STRING.xmap(Parte::valueOf, Parte::name);

    public static final Codec<Aplique> CODEC = RecordCodecBuilder.create(i -> i.group(
            net.minecraft.util.StringIdentifiable.createCodec(ModeloAplique::values).fieldOf("modelo").forGetter(Aplique::modelo),
            CODEC_PARTE.fieldOf("parte").forGetter(Aplique::parte),
            Codec.FLOAT.fieldOf("x").forGetter(Aplique::x),
            Codec.FLOAT.fieldOf("y").forGetter(Aplique::y),
            Codec.FLOAT.fieldOf("z").forGetter(Aplique::z),
            Direction.CODEC.fieldOf("cara").forGetter(Aplique::cara),
            Codec.FLOAT.optionalFieldOf("giro", 0f).forGetter(Aplique::giro),
            Codec.FLOAT.optionalFieldOf("escala", 1f).forGetter(Aplique::escala),
            Codec.INT.listOf().optionalFieldOf("colores", List.of(0xFFFFFF, 0xFFFFFF, 0xFFFFFF)).forGetter(Aplique::colores),
            net.minecraft.util.StringIdentifiable.createCodec(Superficie::values)
                    .optionalFieldOf("superficie", Superficie.CAJA).forGetter(Aplique::superficie),
            Codec.FLOAT.optionalFieldOf("blandura", 0f).forGetter(Aplique::blandura),
            ObjetoAplique.CODEC.optionalFieldOf("objeto").xmap(o -> o.orElse(null), java.util.Optional::ofNullable)
                    .forGetter(Aplique::objeto)
    ).apply(i, Aplique::new));

    /** Color de la zona {@code zona} (0..2), blanco si falta. */
    public int color(int zona) {
        return zona < colores.size() ? colores.get(zona) : 0xFFFFFF;
    }

    public Aplique conGiro(float g) {
        return new Aplique(modelo, parte, x, y, z, cara, g, escala, colores, superficie, blandura, objeto);
    }

    public Aplique conEscala(float e) {
        return new Aplique(modelo, parte, x, y, z, cara, giro, e, colores, superficie, blandura, objeto);
    }

    public Aplique conObjeto(ObjetoAplique o) {
        return new Aplique(modelo, parte, x, y, z, cara, giro, escala, colores, superficie, blandura, o);
    }

    public Aplique conBlandura(float b) {
        return new Aplique(modelo, parte, x, y, z, cara, giro, escala, colores, superficie, b, objeto);
    }
}
