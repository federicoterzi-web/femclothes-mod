package com.ejemplo.sublimadora;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * La remera: en blanco al craftearla, estampada despues de pasar por la
 * sublimadora.
 *
 * Es UN solo item, no dos. Lo que cambia son los componentes de estampa: sin
 * ninguno esta en blanco, y puede tener frente, espalda o las dos. Se guarda
 * el UUID de la foto y no la imagen, que es lo que hace que esto escale — la
 * imagen ya vive en el almacenamiento de Camerapture, y el ItemStack pesa lo
 * mismo con o sin estampa.
 */
public final class ModItems {

    /** Foto estampada en el frente. Ausente = ese lado en blanco. */
    public static final ComponentType<Estampa> ESTAMPA_FRENTE = registrarEstampa("estampa_frente");
    /** Idem en la espalda. Estampar las dos caras cuesta dos pasadas. */
    public static final ComponentType<Estampa> ESTAMPA_ESPALDA = registrarEstampa("estampa_espalda");

    /**
     * El componente viejo, de cuando la estampa era un UUID pelado y siempre
     * iba centrada en el frente. Sigue registrado para poder leer las remeras
     * que ya existen en mundos guardados; nada lo escribe.
     */
    public static final ComponentType<UUID> PICTURE_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(ModBlocks.MOD_ID, "picture_id"),
            ComponentType.<UUID>builder()
                    .codec(Uuids.CODEC)
                    .packetCodec(Uuids.PACKET_CODEC)
                    .build());

    public static final RemeraItem REMERA = Registry.register(
            Registries.ITEM,
            Identifier.of(ModBlocks.MOD_ID, "remera"),
            new RemeraItem(new Item.Settings().maxCount(16)));

    private static ComponentType<Estampa> registrarEstampa(String nombre) {
        return Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(ModBlocks.MOD_ID, nombre),
                ComponentType.<Estampa>builder()
                        .codec(Estampa.CODEC)
                        .packetCodec(Estampa.PACKET_CODEC)
                        .build());
    }

    public static void register() {
        // fuerza class-loading
    }

    private ModItems() {}
}
