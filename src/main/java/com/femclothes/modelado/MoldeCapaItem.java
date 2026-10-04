package com.femclothes.modelado;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Moldes de Capa — a pedido (2026-09-29, "uy podemos agregar todo eso como
 * patrones de corte?"): el ruedo, la capucha y el cuello alto son moldes de
 * la categoría Capa de la Modeladora, cada uno en su pin (el largo usa el
 * molde de rango de siempre). Los "sin" sacan la capucha o el cuello a una
 * capa que ya los tiene. Exclusivos de la capa.
 */
public class MoldeCapaItem extends Item {

    public enum Tipo {
        RUEDO_RECTO, RUEDO_REDONDEADO, CON_CAPUCHA, SIN_CAPUCHA, CUELLO_ALTO, SIN_CUELLO, RUEDO_COLA;

        public boolean esRuedo() { return this == RUEDO_RECTO || this == RUEDO_REDONDEADO || this == RUEDO_COLA; }
        public boolean esCapucha() { return this == CON_CAPUCHA || this == SIN_CAPUCHA; }
        public boolean esCuello() { return this == CUELLO_ALTO || this == SIN_CUELLO; }
    }

    public final Tipo tipo;

    public MoldeCapaItem(Settings settings, Tipo tipo) {
        super(settings);
        this.tipo = tipo;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("capa").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
