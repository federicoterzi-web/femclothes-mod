package com.femclothes.modelado;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de TORSO unificado (corto/medio/largo) — a pedido, mismo espíritu
 * que {@link MoldeRangoItem} pero para los dos ejes de torso (un solo
 * valor, sin anclaje ni lado): largo de remera y tiro de pantalón. Un solo
 * set físico de 3, cada categoría lo traduce a SU propia escala real al
 * fijar (ver {@code ModeladoBlockEntity#fijar}, {@code largoDeRango}/
 * {@code tiroDeRango}).
 */
public class MoldeTorsoItem extends Item {

    public enum Rango { CORTO, MEDIO, LARGO }

    public final Rango rango;

    public MoldeTorsoItem(Settings settings, Rango rango) {
        super(settings);
        this.rango = rango;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("remera", "pantalon").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
