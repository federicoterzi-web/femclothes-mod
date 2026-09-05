package com.ejemplo.sublimadora;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/**
 * Puente con Camerapture, aislado para que la JVM solo lo cargue si el mod
 * esta presente.
 *
 * De la foto solo se saca el UUID: la imagen ya vive en su almacenamiento, y
 * copiarla al ItemStack de la remera lo haria pesar de mas por cada prenda.
 */
final class CameraptureCompat {

    private CameraptureCompat() {}

    static UUID uuidDe(ItemStack stack) {
        PictureItem.PictureData data = stack.get(Camerapture.PICTURE_DATA);
        return data == null ? null : data.id();
    }
}
