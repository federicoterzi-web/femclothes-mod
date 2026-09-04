package com.femclothes.item;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;

/**
 * Prenda "pegada al cuerpo" (medias, shorts, croptop, etc). A diferencia
 * de ClothingArmorItem, esta NO usa la geometría de armadura (más ancha)
 * — se registra en un slot custom de Trinkets y se dibuja directo sobre
 * las ModelPart reales del jugador vía BodyPartTrinketRenderer, así que
 * queda pegada al cuerpo como una "segunda piel" en vez de verse como
 * una bota/peto puestos encima.
 */
public class ClothingTrinketItem extends TrinketItem {

    public final boolean dyeable;

    public ClothingTrinketItem(Settings settings, boolean dyeable) {
        super(settings);
        this.dyeable = dyeable;
    }

    public int getColor(ItemStack stack) {
        if (!dyeable) return 0xFFFFFF;
        DyedColorComponent c = stack.get(DataComponentTypes.DYED_COLOR);
        return c != null ? c.rgb() : 0xFFFFFF;
    }

    public static void setColor(ItemStack stack, int rgb) {
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, true));
    }
}
