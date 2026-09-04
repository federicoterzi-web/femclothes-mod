package com.femclothes.item;

import net.minecraft.item.ItemStack;

/** Helper compartido: leer/escribir el color base sin importar si la prenda es un ClothingTrinketItem o un ClothingArmorItem (legacy). */
public final class FemclothesDye {

    public static boolean isClothing(ItemStack stack) {
        return stack.getItem() instanceof ClothingTrinketItem || stack.getItem() instanceof ClothingArmorItem;
    }

    public static void setBaseColor(ItemStack stack, int rgb) {
        if (stack.getItem() instanceof ClothingTrinketItem) {
            ClothingTrinketItem.setColor(stack, rgb);
        } else if (stack.getItem() instanceof ClothingArmorItem) {
            ClothingArmorItem.setColor(stack, rgb);
        }
    }

    private FemclothesDye() {}
}
