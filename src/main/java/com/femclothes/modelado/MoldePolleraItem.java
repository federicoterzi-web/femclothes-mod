package com.femclothes.modelado;

import com.femclothes.item.PolleraForma;
import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de Pollera (Campana/Tableada) — a pedido (2026-09-29, "las dos, mira
 * la que ya hay para la tableada"): va en el pin Forma de la categoría
 * Pollera de la Modeladora. Exclusivo de la pollera. Ver {@link PolleraForma}.
 */
public class MoldePolleraItem extends Item {

    public final PolleraForma valor;

    public MoldePolleraItem(Settings settings, PolleraForma valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("pollera").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
