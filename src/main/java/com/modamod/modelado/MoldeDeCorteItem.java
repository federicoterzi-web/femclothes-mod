package com.modamod.modelado;

import com.modamod.item.ModamodComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * El molde de corte: a diferencia de los moldes de eje (uno por valor, o
 * cíclicos), este ítem encodea una COMBINACIÓN de varios ejes a la vez —
 * "cuello en V + manga corta", por ejemplo — armada en la Mesa de Modelado.
 * Se comparte y se vende como cualquier molde físico; cualquier Mesa de
 * Modelado lo lee.
 */
public class MoldeDeCorteItem extends Item {

    public MoldeDeCorteItem(Settings settings) {
        super(settings);
    }

    public static ComboCorte combo(ItemStack stack) {
        ComboCorte c = stack.get(ModamodComponents.COMBO_CORTE);
        return c == null ? ComboCorte.VACIO : c;
    }

    public static void setCombo(ItemStack stack, ComboCorte combo) {
        if (combo.estaVacio()) stack.remove(ModamodComponents.COMBO_CORTE);
        else stack.set(ModamodComponents.COMBO_CORTE, combo);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        ComboCorte combo = combo(stack);
        if (combo.estaVacio()) {
            tooltip.add(Text.translatable("modamod.modelado.combo.vacio").formatted(Formatting.DARK_GRAY));
            return;
        }
        combo.remeraManga().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.sublimadora.corte.manga",
                                Text.translatable("modamod.sublimadora.manga." + v.clave))
                        .formatted(Formatting.GRAY)));
        combo.remeraLargo().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.sublimadora.largo." + v.clave)
                        .formatted(Formatting.GRAY)));
        combo.remeraCuello().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.sublimadora.cuello." + v.clave)
                        .formatted(Formatting.GRAY)));
        combo.pantalonTiro().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.pantalon.tiro", Text.translatable(v.traduccion()))
                        .formatted(Formatting.GRAY)));
        combo.pantalonLargoSuperior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.superior", Text.translatable(v.traduccion()))
                        .formatted(Formatting.GRAY)));
        combo.pantalonLargoInferior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.inferior", Text.translatable(v.traduccion()))
                        .formatted(Formatting.GRAY)));
        combo.mediasLargoSuperior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.superior", Text.translatable(v.traduccion()))
                        .formatted(Formatting.GRAY)));
        combo.mediasLargoInferior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.inferior", Text.translatable(v.traduccion()))
                        .formatted(Formatting.GRAY)));
        combo.calientabrazosCoberturaSuperior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.superior",
                                Text.translatable("modamod.sublimadora.manga." + v.clave))
                        .formatted(Formatting.GRAY)));
        combo.calientabrazosCoberturaInferior().ifPresent(v -> tooltip.add(
                Text.translatable("modamod.cobertura.inferior",
                                Text.translatable("modamod.sublimadora.manga." + v.clave))
                        .formatted(Formatting.GRAY)));
        combo.calce().ifPresent(v -> tooltip.add(
                Text.translatable(v.traduccion())
                        .formatted(Formatting.GRAY)));
        combo.red().ifPresent(v -> tooltip.add(
                Text.translatable(v.traduccion())
                        .formatted(Formatting.GRAY)));
        if (combo.lado() != com.modamod.region.Lado.AMBAS) {
            tooltip.add(Text.translatable("modamod.region." + combo.lado().clave()).formatted(Formatting.DARK_GRAY));
        }
    }
}
