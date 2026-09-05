package com.ejemplo.sublimadora;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public class RemeraItem extends Item {

    public RemeraItem(Settings settings) {
        super(settings);
    }

    public static boolean estaEstampada(ItemStack stack) {
        return stack.get(ModItems.PICTURE_ID) != null;
    }

    /** Copia una remera en blanco con la foto ya estampada. */
    public static ItemStack estampar(ItemStack blanca, UUID foto) {
        ItemStack out = blanca.copyWithCount(1);
        out.set(ModItems.PICTURE_ID, foto);
        return out;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        UUID foto = stack.get(ModItems.PICTURE_ID);
        if (foto == null) {
            tooltip.add(Text.translatable("sublimadora.remera.en_blanco").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("sublimadora.remera.estampada").formatted(Formatting.AQUA));
        }
    }
}
