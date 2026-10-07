package com.femclothes.aplique;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de aplique (2026-10-01): define la FORMA (moño, mariposa, flor). Va en
 * la Mesa de estilado y no se gasta, como los moldes de patrón; los colores
 * salen del {@link RetazoApliqueItem}.
 */
public class MoldeApliqueItem extends Item {

    public final ModeloAplique modelo;

    public MoldeApliqueItem(Settings settings, ModeloAplique modelo) {
        super(settings);
        this.modelo = modelo;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "aplique", PrendaLore.Maquina.ESTILADO, "todas");
        tooltip.add(Text.translatable("femclothes.aplique.molde.tooltip").formatted(Formatting.GRAY));
    }
}
