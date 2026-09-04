package com.femclothes.item;

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

    public static void init() {
        // fuerza class-loading
    }

    private FemclothesComponents() {}
}
