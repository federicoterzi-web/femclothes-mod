package com.femclothes.correa;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de correa (2026-10-05, "correas libres" + "molde de cadenas"): elige el estilo de la correa que se pone en la
 * Mesa de estilado (lisa, cadena, cadena fina, ojalillos o cordón). No se gasta.
 */
public class MoldeCorreaItem extends Item {

    public final EstiloCorrea estilo;

    public MoldeCorreaItem(Settings settings, EstiloCorrea estilo) {
        super(settings);
        this.estilo = estilo;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("femclothes.correa.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
