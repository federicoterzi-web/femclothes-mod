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
                           int tonoClaro, int tonoOscuro, int tonoRubor, int fuerzaRubor,
                           boolean siempre, int busto, int definicion) {

    /** Busto que suman los Estrógenos (2026-10-01): 0..{@link #BUSTO_MAXIMO}, sobre el del cuerpo. */
    public static final int BUSTO_MAXIMO = 5;
    /** Cuánto se marcan los músculos del relieve, en % (2026-10-01). */
    public static final int DEFINICION_DEFECTO = 100, DEFINICION_MAXIMA = 200;

    /** Sin busto ni definición (lo de antes del relieve). */
    public PerfilCuerpo(CuerpoBase cuerpo, int tono, RopaInterior interior, boolean elegido,
                        int tonoClaro, int tonoOscuro, int tonoRubor, int fuerzaRubor, boolean siempre) {
        this(cuerpo, tono, interior, elegido, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre, 0, DEFINICION_DEFECTO);
    }

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
    /** Fuerza del rubor por defecto, en % (2026-09-29, "poneme un selector de fuerza de rubor"). */
    public static final int FUERZA_RUBOR_DEFECTO = 50;

    /**
     * Lo que tiene un jugador que nunca abrio la GUI.
     *
     * Cuerpo derivado de su propia skin, tono derivado de su propia skin,
     * ropa interior basica. Sin esto, ponerse unas medias le cambiaria el
     * cuerpo a alguien que no pidio nada.
     */
    public static final PerfilCuerpo DEFECTO =
            new PerfilCuerpo(CuerpoBase.SKIN_REAL, TONO_DE_LA_SKIN, RopaInterior.DEFECTO, false,
                    TONO_AUTOMATICO, TONO_AUTOMATICO, TONO_AUTOMATICO, FUERZA_RUBOR_DEFECTO, false);

    public static final Codec<PerfilCuerpo> CODEC = RecordCodecBuilder.create(i -> i.group(
            // Por clave y tolerante (2026-09-29): los cuerpos viejos
            // (plano, curvy, binder) ya no existen — ver CuerpoBase#deClave.
            Codec.STRING.xmap(CuerpoBase::deClave, c -> c.clave)
                    .optionalFieldOf("cuerpo", CuerpoBase.SKIN_REAL).forGetter(PerfilCuerpo::cuerpo),
            Codec.INT.optionalFieldOf("tono", TONO_DE_LA_SKIN).forGetter(PerfilCuerpo::tono),
            // Ropa interior en dos partes + color (2026-09-30). "interior" es
            // el valor viejo (un solo enum): solo se lee, para migrar.
            Codec.STRING.optionalFieldOf("interior").forGetter(p -> java.util.Optional.empty()),
            StringIdentifiable.createCodec(InteriorArriba::values).optionalFieldOf("interior_arriba")
                    .forGetter(p -> java.util.Optional.of(p.interior().arriba())),
            StringIdentifiable.createCodec(InteriorAbajo::values).optionalFieldOf("interior_abajo")
                    .forGetter(p -> java.util.Optional.of(p.interior().abajo())),
            Codec.INT.optionalFieldOf("interior_color")
                    .forGetter(p -> java.util.Optional.of(p.interior().color())),
            Codec.BOOL.optionalFieldOf("elegido", false).forGetter(PerfilCuerpo::elegido),
            Codec.INT.optionalFieldOf("tono_claro", TONO_AUTOMATICO).forGetter(PerfilCuerpo::tonoClaro),
            Codec.INT.optionalFieldOf("tono_oscuro", TONO_AUTOMATICO).forGetter(PerfilCuerpo::tonoOscuro),
            // Rubor (2026-09-29, "3 por default, mas selector propio"): 0 =
            // automático, el color de la zona más saturado y oscuro.
            Codec.INT.optionalFieldOf("tono_rubor", TONO_AUTOMATICO).forGetter(PerfilCuerpo::tonoRubor),
            Codec.INT.optionalFieldOf("fuerza_rubor", FUERZA_RUBOR_DEFECTO).forGetter(PerfilCuerpo::fuerzaRubor),
            // Usar el cuerpo como skin aunque no haya ropa del mod (2026-09-29,
            // "un selector que directamente te deje esa skin de default").
            Codec.BOOL.optionalFieldOf("siempre", false).forGetter(PerfilCuerpo::siempre),
            // Relieve (2026-10-01): busto de los Estrógenos y definición de los músculos.
            Codec.INT.optionalFieldOf("busto", 0).forGetter(PerfilCuerpo::busto),
            Codec.INT.optionalFieldOf("definicion", DEFINICION_DEFECTO).forGetter(PerfilCuerpo::definicion)
    ).apply(i, (cuerpo, tono, viejo, arriba, abajo, color, elegido, claro, oscuro, rubor, fuerza, siempre, busto, definicion) -> {
        RopaInterior base = viejo.map(RopaInterior::deClaveVieja).orElse(RopaInterior.DEFECTO);
        RopaInterior interior = new RopaInterior(arriba.orElse(base.arriba()), abajo.orElse(base.abajo()),
                color.orElse(base.color()));
        return new PerfilCuerpo(cuerpo, tono, interior, elegido, claro, oscuro, rubor, fuerza, siempre, busto, definicion);
    }));

    /** A mano: {@code PacketCodec.tuple} llega hasta 6 campos y son 11. */
    public static final PacketCodec<RegistryByteBuf, PerfilCuerpo> PACKET_CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.cuerpo().ordinal());
                buf.writeInt(p.tono());
                buf.writeVarInt(p.interior().arriba().ordinal());
                buf.writeVarInt(p.interior().abajo().ordinal());
                buf.writeInt(p.interior().color());
                buf.writeBoolean(p.elegido());
                buf.writeInt(p.tonoClaro());
                buf.writeInt(p.tonoOscuro());
                buf.writeInt(p.tonoRubor());
                buf.writeVarInt(p.fuerzaRubor());
                buf.writeBoolean(p.siempre());
                buf.writeVarInt(p.busto());
                buf.writeVarInt(p.definicion());
            },
            buf -> new PerfilCuerpo(CuerpoBase.values()[buf.readVarInt()], buf.readInt(),
                    new RopaInterior(InteriorArriba.values()[buf.readVarInt()], InteriorAbajo.values()[buf.readVarInt()],
                            buf.readInt()), buf.readBoolean(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readVarInt(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt()));

    /** True si el tono todavia hay que sacarlo de la skin del jugador. */
    public boolean tonoDerivado() {
        return tono == TONO_DE_LA_SKIN;
    }

    public PerfilCuerpo conCuerpo(CuerpoBase c) {
        return new PerfilCuerpo(c, tono, interior, true, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre, busto, definicion);
    }

    public PerfilCuerpo conTono(int rgb) {
        return new PerfilCuerpo(cuerpo, rgb, interior, true, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre, busto, definicion);
    }

    /** Los tres colores de una vez (la GUI de elegir cuerpo). */
    public PerfilCuerpo conTonos(int base, int claro, int oscuro, int rubor, int fuerza) {
        return new PerfilCuerpo(cuerpo, base, interior, true, claro, oscuro, rubor, fuerza, siempre, busto, definicion);
    }

    public PerfilCuerpo conSiempre(boolean valor) {
        return new PerfilCuerpo(cuerpo, tono, interior, true, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, valor, busto, definicion);
    }

    public PerfilCuerpo conBusto(int b) {
        return new PerfilCuerpo(cuerpo, tono, interior, elegido, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre,
                Math.max(0, Math.min(BUSTO_MAXIMO, b)), definicion);
    }

    public PerfilCuerpo conDefinicion(int d) {
        return new PerfilCuerpo(cuerpo, tono, interior, elegido, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre,
                busto, Math.max(0, Math.min(DEFINICION_MAXIMA, d)));
    }

    public PerfilCuerpo conInterior(RopaInterior r) {
        return new PerfilCuerpo(cuerpo, tono, r, true, tonoClaro, tonoOscuro, tonoRubor, fuerzaRubor, siempre, busto, definicion);
    }

    /**
     * Con que identificar la textura compuesta en el cache.
     *
     * No incluye el jugador: dos personas con el mismo perfil y la misma
     * skin comparten sustrato, que es justo lo que se quiere.
     */
    public String clave() {
        return cuerpo.clave + "/" + Integer.toHexString(tono) + "/" + interior.clave()
                + "/" + Integer.toHexString(tonoClaro) + "/" + Integer.toHexString(tonoOscuro) + "/" + Integer.toHexString(tonoRubor) + "/" + fuerzaRubor;
    }
}
