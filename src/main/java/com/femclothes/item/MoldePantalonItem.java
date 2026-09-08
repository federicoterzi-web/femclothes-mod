package com.femclothes.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de largo de pantalón: FIJA un valor de {@link PantalonLargo}, no
 * cicla — a pedido del dueño, un ítem por cada uno de los siete valores en
 * vez de uno que va ciclando a ciegas. Mismo mecanismo que
 * {@link MoldeLargoRemeraItem} (remera) y {@link MoldeTiroItem} (tiro).
 *
 * Empezó cíclico y se cambió apenas se probó en el juego: con solo 3 pasos
 * ciclar es cómodo (remera, antes de este cambio), pero con siete hay que
 * clickear hasta seis veces para llegar a "tanga" — siete ítems con nombre
 * propio es mejor UX que un dial de siete posiciones.
 */
public class MoldePantalonItem extends Item {

    public final PantalonLargo valor;

    public MoldePantalonItem(Settings settings, PantalonLargo valor) {
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
