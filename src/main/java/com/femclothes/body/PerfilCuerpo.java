package com.femclothes.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

/**
 * Como es el cuerpo de este jugador. Vive en el jugador, no en la prenda.
 *
 * Va aca y no en un componente de item porque es del JUGADOR: la misma remera
 * puesta por dos personas tiene que verse sobre dos cuerpos distintos. Se
 * guarda como attachment persistente y se sincroniza al cliente, que es quien
 * compone la textura del sustrato.
 *
 * <p>Tres colores (2026-09-29, "la skin que uso yo es de un osito, entonces
 * tiene un tono de pelaje y un tono en la pancita"): {@code tono} es la zona
 * Base (piel, pelaje), {@code tonoClaro} la Clara (pancita, hocico, manchas
 * claras) y {@code tonoOscuro} la Oscura (rayas, manchas negras). Las zonas
 * salen del gris de la mascara del cuerpo, ver {@code CuerpoBaseTextures}.
 * Clara y Oscura en 0 = automáticas: salen del color Base, como antes.
 */
public record PerfilCuerpo(CuerpoBase cuerpo, int tono, RopaInterior interior, boolean elegido,
                           int tonoClaro, int tonoOscuro) {

    /**
     * Tono "sin elegir": lo saca de la skin del jugador.
     *
     * Es 0 y no un beige cualquiera porque 0 es un ARGB con alfa 0, o sea un
     * color que nadie puede haber elegido a mano. Asi "todavia no elegi" y
     * "elegi negro" no se confunden.
     */
    public static final int TONO_DE_LA_SKIN = 0;
    /** Zona Clara u Oscura sin elegir: sale del color Base. */
    public static final int TONO_AUTOMATICO = 0;

    /**
     * Lo que tiene un jugador que nunca abrio la GUI.
     *
     * Cuerpo derivado de su propia skin, tono derivado de su propia skin,
     * ropa interior basica. Sin esto, ponerse unas medias le cambiaria el
     * cuerpo a alguien que no pidio nada.
     */
    public static final PerfilCuerpo DEFECTO =
            new PerfilCuerpo(CuerpoBase.SKIN_REAL, TONO_DE_LA_SKIN, RopaInterior.BASICA, false,
                    TONO_AUTOMATICO, TONO_AUTOMATICO);

    public static final Codec<PerfilCuerpo> CODEC = RecordCodecBuilder.create(i -> i.group(
            // Por clave y tolerante (2026-09-29): los cuerpos viejos
            // (plano, curvy, binder) ya no existen — ver CuerpoBase#deClave.
            Codec.STRING.xmap(CuerpoBase::deClave, c -> c.clave)
                    .optionalFieldOf("cuerpo", CuerpoBase.SKIN_REAL).forGetter(PerfilCuerpo::cuerpo),
            Codec.INT.optionalFieldOf("tono", TONO_DE_LA_SKIN).forGetter(PerfilCuerpo::tono),
            StringIdentifiable.createCodec(RopaInterior::values)
                    .optionalFieldOf("interior", RopaInterior.BASICA).forGetter(PerfilCuerpo::interior),
            Codec.BOOL.optionalFieldOf("elegido", false).forGetter(PerfilCuerpo::elegido),
            Codec.INT.optionalFieldOf("tono_claro", TONO_AUTOMATICO).forGetter(PerfilCuerpo::tonoClaro),
            Codec.INT.optionalFieldOf("tono_oscuro", TONO_AUTOMATICO).forGetter(PerfilCuerpo::tonoOscuro)
    ).apply(i, PerfilCuerpo::new));

    public static final PacketCodec<RegistryByteBuf, PerfilCuerpo> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.indexed(i -> CuerpoBase.values()[i], Enum::ordinal), PerfilCuerpo::cuerpo,
            PacketCodecs.INTEGER, PerfilCuerpo::tono,
            PacketCodecs.indexed(i -> RopaInterior.values()[i], Enum::ordinal), PerfilCuerpo::interior,
            PacketCodecs.BOOL, PerfilCuerpo::elegido,
            PacketCodecs.INTEGER, PerfilCuerpo::tonoClaro,
            PacketCodecs.INTEGER, PerfilCuerpo::tonoOscuro,
            PerfilCuerpo::new);

    /** True si el tono todavia hay que sacarlo de la skin del jugador. */
    public boolean tonoDerivado() {
        return tono == TONO_DE_LA_SKIN;
    }

    public PerfilCuerpo conCuerpo(CuerpoBase c) {
        return new PerfilCuerpo(c, tono, interior, true, tonoClaro, tonoOscuro);
    }

    public PerfilCuerpo conTono(int rgb) {
        return new PerfilCuerpo(cuerpo, rgb, interior, true, tonoClaro, tonoOscuro);
    }

    /** Los tres colores de una vez (la GUI de elegir cuerpo). */
    public PerfilCuerpo conTonos(int base, int claro, int oscuro) {
        return new PerfilCuerpo(cuerpo, base, interior, true, claro, oscuro);
    }

    public PerfilCuerpo conInterior(RopaInterior r) {
        return new PerfilCuerpo(cuerpo, tono, r, true, tonoClaro, tonoOscuro);
    }

    /**
     * Con que identificar la textura compuesta en el cache.
     *
     * No incluye el jugador: dos personas con el mismo perfil y la misma
     * skin comparten sustrato, que es justo lo que se quiere.
     */
    public String clave() {
        return cuerpo.clave + "/" + Integer.toHexString(tono) + "/" + interior.clave
                + "/" + Integer.toHexString(tonoClaro) + "/" + Integer.toHexString(tonoOscuro);
    }
}
