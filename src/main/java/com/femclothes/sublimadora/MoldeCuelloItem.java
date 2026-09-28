package com.femclothes.sublimadora;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de cuello: FIJA un valor de {@link Variante.Cuello}, no cicla — a
 * pedido, mismo cambio que ya tuvieron largo y manga (un ítem por valor en
 * vez de un dial cíclico). {@link MoldeItem} (eje CUELLO) sigue existiendo
 * y cíclico para quien lo prefiera; este es la alternativa "elegís por
 * nombre" para la Mesa de Modelado.
 */
public class MoldeCuelloItem extends Item {

    public final Variante.Cuello valor;

    public MoldeCuelloItem(Settings settings, Variante.Cuello valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("remera").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
