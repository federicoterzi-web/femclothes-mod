package com.femclothes.item;

import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Arma el tooltip de los moldes, tramas, máscaras, plantillas y motivos (2026-10-05, "tooltip va a decir nombre: Cuello
 * en V, Capucha, etc, abajo Modelo de corte de cuello, Modelo de corte de calce etc, abajo prendas aplicables y abajo
 * aplicable en la maquina en q se usa"): tipo, prendas y máquina. Con más de tres prendas la lista se abrevia y con
 * Shift se ve completa ("2 si").
 *
 * <p>Existe porque un molde sin esto es indistinguible de otro en el inventario: esta línea dice qué es, a qué
 * prenda(s) aplica y en qué máquina se usa CADA ítem en particular.
 */
public final class PrendaLore {

    private PrendaLore() {}

    /** Dónde se usa el ítem, con el color del tema de cada máquina (cobre, verdín, oro, lila). */
    public enum Maquina {
        MODELADORA("modeladora", Formatting.GOLD),
        TINTES("tinturas", Formatting.GREEN),
        SUBLIMADORA("sublimadora", Formatting.YELLOW),
        ESTILADO("mesa_estilado", Formatting.LIGHT_PURPLE);

        final String bloque;
        final Formatting color;

        Maquina(String bloque, Formatting color) {
            this.bloque = bloque;
            this.color = color;
        }
    }

    /** Líneas de abajo del nombre: tipo, prendas aplicables y máquina. */
    public static void molde(List<Text> tooltip, String tipo, Maquina maquina, String... prendas) {
        tooltip.add(Text.translatable("femclothes.tipo." + tipo).formatted(Formatting.GRAY));
        if (prendas.length > 0) {
            boolean completa = prendas.length <= 3 || shift();
            int n = completa ? prendas.length : 3;
            StringBuilder lista = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) lista.append(", ");
                lista.append(Text.translatable("femclothes.prenda." + prendas[i]).getString());
            }
            if (!completa) lista.append("…");
            tooltip.add(Text.translatable("femclothes.tooltip.prendas", lista.toString()).formatted(Formatting.DARK_GRAY));
            if (!completa) tooltip.add(Text.translatable("femclothes.tooltip.shift").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
        tooltip.add(Text.translatable("femclothes.tooltip.maquina", Text.translatable("block.femclothes." + maquina.bloque))
                .formatted(maquina.color));
    }

    /** Shift apretado; solo existe en el cliente (el servidor nunca arma tooltips). */
    private static boolean shift() {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.CLIENT) return false;
        return com.femclothes.client.ShiftCliente.apretado();
    }

    /** Compatibilidad: la línea "Se usa en: X, Y" de antes. */
    public static MutableText seUsaEn(String... prendas) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < prendas.length; i++) {
            if (i > 0) lista.append(", ");
            lista.append(Text.translatable("femclothes.prenda." + prendas[i]).getString());
        }
        return Text.translatable("femclothes.tooltip.se_usa_en", lista.toString());
    }
}
