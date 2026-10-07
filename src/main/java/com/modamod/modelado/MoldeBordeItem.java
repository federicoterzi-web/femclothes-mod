package com.modamod.modelado;

import com.modamod.item.PolleraBorde;
import com.modamod.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Molde de borde del ruedo (2026-10-05, "un ruedo distinto"): ondulado, festoneado o con pico. Va en el pin Custom de la Pollera. */
public class MoldeBordeItem extends Item {

    public final PolleraBorde valor;

    public MoldeBordeItem(Settings settings, PolleraBorde valor) {
        super(settings);
        this.valor = valor;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "borde", PrendaLore.Maquina.MODELADORA, "pollera");
        tooltip.add(Text.translatable("modamod.pollera.molde_borde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
