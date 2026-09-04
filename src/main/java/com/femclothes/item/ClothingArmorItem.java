package com.femclothes.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Prenda que ocupa un slot vanilla (helmet/chest/legs/boots) — pensada para
 * usarse en el slot COSMÉTICO que agrega Cosmetic Armor Updated, no en el
 * slot real de combate. Dyeable igual que el cuero.
 */
public class ClothingArmorItem extends ArmorItem {

    public final boolean dyeable;
    public final boolean skinFillTorso;

    public ClothingArmorItem(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Settings settings,
                              boolean dyeable, boolean skinFillTorso) {
        super(material, type, settings);
        this.dyeable = dyeable;
        this.skinFillTorso = skinFillTorso;
    }

    public ClothingArmorItem(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Settings settings,
                              boolean dyeable) {
        this(material, type, settings, dyeable, false);
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
