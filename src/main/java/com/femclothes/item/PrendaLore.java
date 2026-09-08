package com.femclothes.item;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Arma la línea "Se usa en: X, Y" de un tooltip de molde o patrón.
 *
 * Existe porque un molde/patrón sin esto es indistinguible de otro en el
 * inventario: 16 moldes comparten el mismo ícono placeholder
 * (`molde_largo.png`) y hasta ahora todos mostraban el mismo texto genérico
 * ("En el telar, con una prenda"). Con calientabrazos reusando moldes de
 * remera y pantalón, la ambigüedad se puso peor — esta línea es la que dice
 * a qué prenda(s) aplica CADA ítem en particular.
 */
public final class PrendaLore {

    private PrendaLore() {}

    public static MutableText seUsaEn(String... prendas) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < prendas.length; i++) {
            if (i > 0) lista.append(", ");
            lista.append(Text.translatable("femclothes.prenda." + prendas[i]).getString());
        }
        return Text.translatable("femclothes.tooltip.se_usa_en", lista.toString());
    }
}
