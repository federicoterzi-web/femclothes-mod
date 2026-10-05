package com.femclothes.correa;

import com.femclothes.garment.Parte;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Una correa libre puesta en una prenda o wearable (2026-10-05, "correas libres... rectas u oblicuas, pegadas o
 * colgantes"). Va en el componente {@code femclothes:correas} (aparte de los apliques: {@code Aplique} ya no admite
 * campos). Los dos puntos son sobre la superficie de la caja de la {@link Parte} sin inflar, en px de skin del espacio
 * local de la parte, como el punto de un aplique.
 *
 * @param ancho   ancho de la tira en px (1..4)
 * @param colores banda, borde y herraje (RGB)
 * @param blandura 0..1: cuánto se mueve la correa colgante con el movimiento
 */
public record Correa(Parte parte, Punto desde, Punto hasta, EstiloCorrea estilo, ModoCorrea modo, float ancho,
                     List<Integer> colores, float blandura, Superficie superficie) {

    /**
     * Sobre qué va (2026-10-05, "superame esos limites"): la caja de la parte del cuerpo o las cajas del sombrero y la
     * banda ({@code CAJA}; x, y, z en px del marco de su parte), o la malla de la pollera o la capa (x, y = u, v de la
     * tela en px de 64, como un aplique).
     */
    public enum Superficie implements StringIdentifiable {
        CAJA, POLLERA, CAPA;

        @Override
        public String asString() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final int MAXIMO_POR_PRENDA = 8;
    public static final float ANCHO_MIN = 1f, ANCHO_MAX = 4f;
    public static final List<Integer> DE_FABRICA = List.of(0x5A3A24, 0x8A6240, 0xC9A24A);

    /** Un punto de la superficie de la caja y la cara (normal) donde está. */
    public record Punto(float x, float y, float z, Direction cara) {
        public static final Codec<Punto> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("x").forGetter(Punto::x),
                Codec.FLOAT.fieldOf("y").forGetter(Punto::y),
                Codec.FLOAT.fieldOf("z").forGetter(Punto::z),
                Direction.CODEC.fieldOf("cara").forGetter(Punto::cara)
        ).apply(i, Punto::new));
    }

    public static final Codec<Correa> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.xmap(n -> Parte.values()[Math.floorMod(n, Parte.values().length)], Parte::ordinal).fieldOf("parte").forGetter(Correa::parte),
            Punto.CODEC.fieldOf("desde").forGetter(Correa::desde),
            Punto.CODEC.fieldOf("hasta").forGetter(Correa::hasta),
            StringIdentifiable.createCodec(EstiloCorrea::values).fieldOf("estilo").forGetter(Correa::estilo),
            StringIdentifiable.createCodec(ModoCorrea::values).fieldOf("modo").forGetter(Correa::modo),
            Codec.FLOAT.fieldOf("ancho").forGetter(Correa::ancho),
            Codec.INT.listOf().fieldOf("colores").forGetter(Correa::colores),
            Codec.FLOAT.fieldOf("blandura").forGetter(Correa::blandura),
            StringIdentifiable.createCodec(Superficie::values).optionalFieldOf("superficie", Superficie.CAJA)
                    .forGetter(Correa::superficie)
    ).apply(i, Correa::new));

    public Correa(Parte parte, Punto desde, Punto hasta, EstiloCorrea estilo, ModoCorrea modo, float ancho,
                  List<Integer> colores, float blandura) {
        this(parte, desde, hasta, estilo, modo, ancho, colores, blandura, Superficie.CAJA);
    }

    public Correa conColores(List<Integer> c) { return new Correa(parte, desde, hasta, estilo, modo, ancho, c, blandura, superficie); }

    public Correa conModo(ModoCorrea m) { return new Correa(parte, desde, hasta, estilo, m, ancho, colores, blandura, superficie); }

    public Correa conAncho(float a) { return new Correa(parte, desde, hasta, estilo, modo, a, colores, blandura, superficie); }

    public Correa {
        if (colores == null || colores.size() < 3) colores = DE_FABRICA;
        ancho = Math.max(ANCHO_MIN, Math.min(ANCHO_MAX, ancho));
        blandura = Math.max(0f, Math.min(1f, blandura));
    }
}
