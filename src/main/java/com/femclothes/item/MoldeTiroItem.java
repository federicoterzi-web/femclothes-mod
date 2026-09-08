package com.femclothes.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de tiro del pantalón: FIJA un valor de {@link PantalonTiro}, no
 * cicla — mismo mecanismo que {@link MoldeLargoRemeraItem} y el molde de
 * largo de pantalón. Tres ítems, uno por valor.
 */
public class MoldeTiroItem extends Item {

    public final PantalonTiro valor;

    public MoldeTiroItem(Settings settings, PantalonTiro valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("pantalon").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
