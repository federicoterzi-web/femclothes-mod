package com.femclothes.item;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;

/**
 * Calentadores de brazo. TrinketItem YA extiende Item (no es una interfaz),
 * así que esta clase hereda directo de TrinketItem — no "extends Item
 * implements TrinketItem" como en el borrador anterior, eso no compila.
 *
 * Entra en el slot group custom "arms" declarado en
 * data/trinkets/slots/arms/*.json. Trinkets se encarga de guardarlo,
 * sincronizarlo y mostrarlo en su UI.
 */
public class ArmWarmerItem extends TrinketItem {

    public ArmWarmerItem(Settings settings) {
        super(settings);
    }

    public int getColor(ItemStack stack) {
        DyedColorComponent c = stack.get(DataComponentTypes.DYED_COLOR);
        return c != null ? c.rgb() : 0xFFFFFF;
    }

    public static void setColor(ItemStack stack, int rgb) {
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, true));
    }
}
