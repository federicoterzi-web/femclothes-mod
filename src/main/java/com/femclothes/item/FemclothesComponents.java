package com.femclothes.item;

import com.femclothes.region.Orientacion;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

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

    public static void init() {
        // fuerza class-loading
    }

    private FemclothesComponents() {}
}
