package com.femclothes.item;

import com.femclothes.region.Orientacion;
import com.femclothes.sublimadora.Variante;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * Componentes de datos custom para el sistema de personalización:
 * - PATTERN_ID: qué patrón está aplicado (ej. "femclothes:stripe_top"),
 *   null/ausente = sin patrón (prenda lisa).
 * - PATTERN_COLOR: color del patrón (el segundo tinte). Antes se
 *   llamaba STRIPE_COLOR — se generalizó de "raya" a "cualquier patrón"
 *   ahora que hay una estación para aplicarlos.
 * El color BASE de la prenda sigue usando DataComponentTypes.DYED_COLOR
 * (el mecanismo vanilla, vía ClothingTrinketItem/ClothingArmorItem).
 */
public final class FemclothesComponents {

    public static final ComponentType<Identifier> PATTERN_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "pattern_id"),
            ComponentType.<Identifier>builder()
                    .codec(Identifier.CODEC)
                    .packetCodec(Identifier.PACKET_CODEC)
                    .build());

    public static final ComponentType<Integer> PATTERN_COLOR = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "pattern_color"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build());

    // --- Overrides de la pierna DERECHA ---
    // Ausentes = la derecha usa lo mismo que la izquierda, que es el caso
    // comun (par igual) y ademas mantiene validas las 16 recetas de crafteo,
    // que solo setean DYED_COLOR. Solo se escriben cuando el jugador elige
    // una pierna puntual en el telar.
    public static final ComponentType<Integer> RIGHT_DYED_COLOR = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "right_dyed_color"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build());

    public static final ComponentType<Identifier> RIGHT_PATTERN_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "right_pattern_id"),
            ComponentType.<Identifier>builder()
                    .codec(Identifier.CODEC)
                    .packetCodec(Identifier.PACKET_CODEC)
                    .build());

    public static final ComponentType<Integer> RIGHT_PATTERN_COLOR = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "right_pattern_color"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build());

    /**
     * Como esta puesta la prenda: girada (frente/espalda) y/o espejada
     * (izquierda/derecha).
     *
     * Ausente = normal, y se BORRA al volver a la normal en vez de guardarse
     * con los dos bits en false: una prenda sin girar tiene que apilar con
     * otra igual, y un componente presente no apila contra uno ausente.
     *
     * No permuta nada guardado — lo aplica RegionResolver al resolver, asi
     * que sacar el flag devuelve la prenda a como estaba.
     */
    public static final ComponentType<Orientacion> ORIENTACION = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "orientacion"),
            ComponentType.<Orientacion>builder()
                    .codec(Orientacion.CODEC)
                    .packetCodec(Orientacion.PACKET_CODEC)
                    .build());

    /**
     * El largo del pantalón (§{@link PantalonLargo}). Ausente = pantalón
     * largo completo, que es lo que sale del crafteo — mismo default que
     * remera con {@code Variante.BASE}.
     */
    public static final ComponentType<PantalonLargo> PANTALON_LARGO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "pantalon_largo"),
            ComponentType.<PantalonLargo>builder()
                    .codec(StringIdentifiable.createCodec(PantalonLargo::values))
                    .packetCodec(PacketCodecs.indexed(i -> PantalonLargo.values()[i], Enum::ordinal))
                    .build());

    /**
     * El tiro del pantalón (§{@link PantalonTiro}): cuánto sube la cintura
     * sobre el torso. Eje independiente de {@code PANTALON_LARGO} — ausente
     * = {@code MEDIO}, la cintura natural.
     */
    public static final ComponentType<PantalonTiro> PANTALON_TIRO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "pantalon_tiro"),
            ComponentType.<PantalonTiro>builder()
                    .codec(StringIdentifiable.createCodec(PantalonTiro::values))
                    .packetCodec(PacketCodecs.indexed(i -> PantalonTiro.values()[i], Enum::ordinal))
                    .build());

    /**
     * El largo de las medias (§{@link MediasLargo}). Ausente = CANCAN, el
     * largo que ya tenían las medias antes de este eje (10 de 12 filas,
     * verificado contra `socks_solid_layer_1.png`) — así una media vieja
     * guardada sigue viéndose exactamente igual.
     */
    public static final ComponentType<MediasLargo> MEDIAS_LARGO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "medias_largo"),
            ComponentType.<MediasLargo>builder()
                    .codec(StringIdentifiable.createCodec(MediasLargo::values))
                    .packetCodec(PacketCodecs.indexed(i -> MediasLargo.values()[i], Enum::ordinal))
                    .build());

    /**
     * La cobertura de calientabrazos (§{@link com.femclothes.sublimadora.Variante.Manga}):
     * cuánto brazo tapa la tela, contado desde la MUÑECA hacia arriba — misma
     * dirección invertida que ya existe entre {@code PANTALON_LARGO} (cintura
     * hacia abajo) y {@code MEDIAS_LARGO} (tobillo hacia arriba). Reusa el
     * MISMO molde cíclico que la manga de la remera (`MoldeItem.Eje.MANGA`),
     * pero en un componente propio: la manga de una remera puesta y la
     * cobertura de un calientabrazos puesto no se pisan entre sí. Ausente =
     * {@code LARGA} (cobertura completa, el estado "recién crafteado").
     */
    public static final ComponentType<Variante.Manga> CALIENTABRAZOS_COBERTURA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "calientabrazos_cobertura"),
            ComponentType.<Variante.Manga>builder()
                    .codec(StringIdentifiable.createCodec(Variante.Manga::values))
                    .packetCodec(PacketCodecs.indexed(i -> Variante.Manga.values()[i], Enum::ordinal))
                    .build());

    /**
     * El tiro de calientabrazos (§{@link PantalonTiro}): una banda extra que
     * sube desde el hombro hacia el torso, misma técnica de runtime que
     * {@code PiezasDelMod.pintarCintura} usa para pantalón (ver
     * {@code pintarHombro}). Reusa el MISMO molde que el tiro de pantalón,
     * en un componente propio — ausente = {@code MEDIO}.
     */
    public static final ComponentType<PantalonTiro> CALIENTABRAZOS_TIRO = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("femclothes", "calientabrazos_tiro"),
            ComponentType.<PantalonTiro>builder()
                    .codec(StringIdentifiable.createCodec(PantalonTiro::values))
                    .packetCodec(PacketCodecs.indexed(i -> PantalonTiro.values()[i], Enum::ordinal))
                    .build());

    public static void init() {
        // fuerza class-loading
    }

    private FemclothesComponents() {}
}
