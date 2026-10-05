package com.femclothes.modelado;

import com.femclothes.item.PolleraVolado;
import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de Volado (2026-10-05, "dos moldes de volado, uno recto y uno circular que se puede aplicar tanto al borde
 * inferior como a toda la textura de la pollera"): el mismo molde sirve en el pin Volado inferior (una fila en el
 * ruedo) o en el pin Volado total (tres filas de arriba a abajo) de la categoría Pollera. Exclusivo de la pollera.
 */
public class MoldeVoladoItem extends Item {

    public final PolleraVolado valor;

    public MoldeVoladoItem(Settings settings, PolleraVolado valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "volado", PrendaLore.Maquina.MODELADORA, "pollera");
        tooltip.add(Text.translatable("femclothes.pollera.molde_volado.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
