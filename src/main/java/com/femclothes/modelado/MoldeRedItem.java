package com.femclothes.modelado;

import com.femclothes.item.PatronRed;
import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de Red (Fina/Gruesa) — a pedido (2026-09-20): mismo criterio que
 * {@link MoldeCalceItem}, transversal a las 4 categorías, sin anclaje ni
 * lado. Ver {@link PatronRed}.
 */
public class MoldeRedItem extends Item {

    public final PatronRed valor;

    public MoldeRedItem(Settings settings, PatronRed valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "trama", PrendaLore.Maquina.MODELADORA, "remera", "pantalon", "medias", "calientabrazos");
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
