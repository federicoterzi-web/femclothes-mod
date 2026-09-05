package com.ejemplo.sublimadora;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

/**
 * Una foto estampada en una cara de la remera.
 *
 * Guarda el UUID y no la imagen: esta ya vive en el almacenamiento de
 * Camerapture, y copiarla al ItemStack lo haria pesar de mas por prenda.
 *
 * La escala y la posicion van NORMALIZADAS al area imprimible, no en
 * pixeles. Asi el mismo numero sirve para el icono del inventario, para la
 * remera apoyada en la plancha y para cuando se dibuje sobre el cuerpo, que
 * son tres resoluciones distintas.
 *
 * @param foto   la foto de Camerapture
 * @param escala 1.0 cubre la remera entera, 0.15 es un logo
 * @param x      corrimiento horizontal, -0.5 a 0.5, 0 es centrado
 * @param y      corrimiento vertical, -0.5 a 0.5, positivo es hacia arriba
 */
public record Estampa(UUID foto, float escala, float x, float y, boolean cubrir) {

    /** Ni tan chica que no se lea, ni tan grande que se coma las mangas. */
    public static final float ESCALA_DEFECTO = 0.42f;
    public static final float ESCALA_MINIMA = 0.10f;
    public static final float ESCALA_MAXIMA = 1.0f;

    /**
     * Los dos presets de estampado, que es lo que elige la palanca.
     *
     * Son presets y no un slider a proposito: cubren los dos casos reales de
     * una sublimadora -la remera entera, o un logo en el pecho- sin obligar a
     * pelear con controles finos a ciegas, gastando tinta y veinte segundos
     * por intento. El record igual guarda escala y posicion libres, asi que
     * agregar control fino despues no rompe nada de esto.
     */
    public enum Modo {
        /** Chica y arriba a la derecha: el logo bordado del pecho. */
        LOGO("logo", 0.28f, 0.22f, 0.20f, false),
        /** La estampa clasica: centrada, del tamano de un dibujo al frente. */
        CENTRADA("centrada", 0.55f, 0f, 0.02f, false),
        /** Full print: cubre el torso entero, recortando lo que sobre. */
        COMPLETO("completo", 1.0f, 0f, 0f, true);

        public final String clave;
        public final float escala, x, y;
        /** Recortar para llenar en vez de encoger para entrar. */
        public final boolean cubrir;

        Modo(String clave, float escala, float x, float y, boolean cubrir) {
            this.clave = clave;
            this.escala = escala;
            this.x = x;
            this.y = y;
            this.cubrir = cubrir;
        }

        public Estampa aplicar(UUID foto) {
            return new Estampa(foto, escala, x, y, cubrir);
        }

        public Modo siguiente() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** En que cara de la remera va. Una prenda puede tener las dos. */
    public enum Cara {
        FRENTE("frente"),
        ESPALDA("espalda");

        public final String clave;

        Cara(String clave) {
            this.clave = clave;
        }
    }

    public Estampa {
        escala = MathHelper.clamp(escala, ESCALA_MINIMA, ESCALA_MAXIMA);
        x = MathHelper.clamp(x, -0.5f, 0.5f);
        y = MathHelper.clamp(y, -0.5f, 0.5f);
    }

    /** Centrada y del tamano de siempre: como salian las remeras viejas. */
    public static Estampa centrada(UUID foto) {
        return new Estampa(foto, ESCALA_DEFECTO, 0f, 0f, false);
    }

    // Escala y posicion son opcionales a proposito: una estampa centrada y
    // del tamano por defecto no escribe nada en el NBT.
    public static final Codec<Estampa> CODEC = RecordCodecBuilder.create(i -> i.group(
            Uuids.CODEC.fieldOf("foto").forGetter(Estampa::foto),
            Codec.FLOAT.optionalFieldOf("escala", ESCALA_DEFECTO).forGetter(Estampa::escala),
            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(Estampa::x),
            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(Estampa::y),
            Codec.BOOL.optionalFieldOf("cubrir", false).forGetter(Estampa::cubrir)
    ).apply(i, Estampa::new));

    public static final PacketCodec<ByteBuf, Estampa> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, Estampa::foto,
            PacketCodecs.FLOAT, Estampa::escala,
            PacketCodecs.FLOAT, Estampa::x,
            PacketCodecs.FLOAT, Estampa::y,
            PacketCodecs.BOOL, Estampa::cubrir,
            Estampa::new);
}
