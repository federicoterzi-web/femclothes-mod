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
 */
public record Aplique(ModeloAplique modelo, Parte parte, float x, float y, float z, Direction cara,
                      float giro, float escala, List<Integer> colores) {

    public static final int MAXIMO_POR_PRENDA = 6;

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
            Codec.INT.listOf().optionalFieldOf("colores", List.of(0xFFFFFF, 0xFFFFFF, 0xFFFFFF)).forGetter(Aplique::colores)
    ).apply(i, Aplique::new));

    /** Color de la zona {@code zona} (0..2), blanco si falta. */
    public int color(int zona) {
        return zona < colores.size() ? colores.get(zona) : 0xFFFFFF;
    }

    public Aplique conGiro(float g) {
        return new Aplique(modelo, parte, x, y, z, cara, g, escala, colores);
    }

    public Aplique conEscala(float e) {
        return new Aplique(modelo, parte, x, y, z, cara, giro, e, colores);
    }
}
