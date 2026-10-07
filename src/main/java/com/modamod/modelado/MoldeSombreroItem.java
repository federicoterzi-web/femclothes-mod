package com.modamod.modelado;

import com.modamod.item.PrendaLore;
import com.modamod.item.SombreroAla;
import com.modamod.item.SombreroPunta;
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
        PrendaLore.molde(tooltip, "sombrero", PrendaLore.Maquina.MODELADORA, "sombrero");
        tooltip.add(Text.translatable("modamod.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
