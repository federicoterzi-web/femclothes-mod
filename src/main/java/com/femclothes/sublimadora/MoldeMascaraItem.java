package com.femclothes.sublimadora;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de máscara de sublimación (2026-10-02, "agregar un mecanismo a la
 * sublimadora mascaras" → "ítem molde de máscara"): va en el slot de máscara
 * de la Sublimadora y le da la forma a las capas que se fijan con él. No se
 * gasta.
 */
public class MoldeMascaraItem extends Item {

    public final FormaMascara forma;

    public MoldeMascaraItem(Settings settings, FormaMascara forma) {
        super(settings);
        this.forma = forma;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("femclothes.sublimadora.mascara.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
