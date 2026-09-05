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
 * Es UN solo item, no dos. Lo que cambia es el componente PICTURE_ID: sin el
 * esta en blanco, con el lleva estampada la foto de ese UUID. Guardar el UUID
 * y no la imagen es lo que hace que esto escale — la imagen ya vive en el
 * almacenamiento de Camerapture, y el ItemStack pesa lo mismo con o sin
 * estampa.
 */
public final class ModItems {

    /** UUID de la foto de Camerapture estampada. Ausente = remera en blanco. */
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

    public static void register() {
        // fuerza class-loading
    }

    private ModItems() {}
}
