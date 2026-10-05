package com.femclothes.modelado;

import com.femclothes.item.PrendaLore;
import com.femclothes.item.SombreroAla;
import com.femclothes.item.SombreroPunta;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Moldes del sombrero de bruja (2026-10-05, "segunda tanda del sombrero"): el ala (ancha o corta) y la punta (recta o
 * doblada) son moldes de la categoría Sombrero de la Modeladora, cada uno en su pin. Exclusivos del sombrero.
 */
public class MoldeSombreroItem extends Item {

    public enum Tipo {
        ALA_ANCHA(SombreroAla.ANCHA, null), ALA_CORTA(SombreroAla.CORTA, null),
        PUNTA_RECTA(null, SombreroPunta.RECTA), PUNTA_DOBLADA(null, SombreroPunta.DOBLADA);

        public final SombreroAla ala;
        public final SombreroPunta punta;

        Tipo(SombreroAla ala, SombreroPunta punta) {
            this.ala = ala;
            this.punta = punta;
        }

        public boolean esAla() { return ala != null; }
        public boolean esPunta() { return punta != null; }
    }

    public final Tipo tipo;

    public MoldeSombreroItem(Settings settings, Tipo tipo) {
        super(settings);
        this.tipo = tipo;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("sombrero").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
