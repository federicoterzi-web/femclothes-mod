package com.femclothes.modelado;

import com.femclothes.item.Calce;
import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de Calce (Pegado/Ajustado/Normal/Suelto/Oversize) — a pedido
 * (2026-09-15): transversal a las 4 categorías de la Mesa, sin anclaje ni
 * lado (un solo valor por prenda, como el largo de torso). Ver
 * {@link Calce}.
 */
public class MoldeCalceItem extends Item {

    public final Calce valor;

    public MoldeCalceItem(Settings settings, Calce valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "calce", PrendaLore.Maquina.MODELADORA, "remera", "pantalon", "medias", "calientabrazos");
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
