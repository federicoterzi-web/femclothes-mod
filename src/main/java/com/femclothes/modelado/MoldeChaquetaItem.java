package com.femclothes.modelado;

import com.femclothes.item.ChaquetaFrente;
import com.femclothes.item.ChaquetaRemate;
import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Moldes de la chaqueta (2026-10-07, "como se te ocurre que mejor hacemos las chaquetas"): el frente (cerrada o
 * abierta), la capucha (con o sin) y el remate de puños y ruedo (elástico o recto) son moldes de la categoría
 * Chaqueta de la Modeladora, cada uno en su pin. El cuello, las mangas, el largo y el calce usan los moldes de
 * siempre. Exclusivos de la chaqueta.
 */
public class MoldeChaquetaItem extends Item {

    public enum Tipo {
        FRENTE_CERRADA(ChaquetaFrente.CERRADA, null, null), FRENTE_ABIERTA(ChaquetaFrente.ABIERTA, null, null),
        CAPUCHA_CON(null, true, null), CAPUCHA_SIN(null, false, null),
        REMATE_ELASTICO(null, null, ChaquetaRemate.ELASTICO), REMATE_RECTO(null, null, ChaquetaRemate.RECTO);

        public final ChaquetaFrente frente;
        public final Boolean capucha;
        public final ChaquetaRemate remate;

        Tipo(ChaquetaFrente frente, Boolean capucha, ChaquetaRemate remate) {
            this.frente = frente;
            this.capucha = capucha;
            this.remate = remate;
        }

        public boolean esFrente() { return frente != null; }
        public boolean esCapucha() { return capucha != null; }
        public boolean esRemate() { return remate != null; }
    }

    public final Tipo tipo;

    public MoldeChaquetaItem(Settings settings, Tipo tipo) {
        super(settings);
        this.tipo = tipo;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PrendaLore.molde(tooltip, "chaqueta", PrendaLore.Maquina.MODELADORA, "chaqueta");
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
