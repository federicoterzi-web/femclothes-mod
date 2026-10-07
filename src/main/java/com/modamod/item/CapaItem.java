package com.modamod.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Capa — a pedido (2026-09-29, "capas que se puedan procesar por todas las
 * maquinas que sean como las capas vanilla (misma dinamica de tela) pero
 * usable y personalizable, seria una nueva categoria de ropa"). Va en su
 * propio slot de Trinkets ({@code espalda/capa}). Se mueve igual que la capa
 * vanilla ({@code render.CapaMalla}); los ejes de corte — largo, ruedo,
 * capucha y cuello alto — salen de la Modeladora; la tela tiene exterior y
 * forro aparte para Tintes y la Sublimadora.
 */
public class CapaItem extends ClothingTrinketItem {

    public CapaItem(Settings settings) {
        super(settings, true);
    }

    public static CapaLargo largo(ItemStack stack) {
        CapaLargo l = stack.get(ModamodComponents.CAPA_LARGO);
        return l == null ? CapaLargo.MEDIO : l;
    }

    public static void setLargo(ItemStack stack, CapaLargo v) {
        if (v == CapaLargo.MEDIO) stack.remove(ModamodComponents.CAPA_LARGO);
        else stack.set(ModamodComponents.CAPA_LARGO, v);
    }

    public static CapaRuedo ruedo(ItemStack stack) {
        CapaRuedo r = stack.get(ModamodComponents.CAPA_RUEDO);
        return r == null ? CapaRuedo.RECTO : r;
    }

    public static void setRuedo(ItemStack stack, CapaRuedo v) {
        if (v == CapaRuedo.RECTO) stack.remove(ModamodComponents.CAPA_RUEDO);
        else stack.set(ModamodComponents.CAPA_RUEDO, v);
    }

    public static boolean capucha(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModamodComponents.CAPA_CAPUCHA));
    }

    public static void setCapucha(ItemStack stack, boolean v) {
        if (!v) stack.remove(ModamodComponents.CAPA_CAPUCHA);
        else stack.set(ModamodComponents.CAPA_CAPUCHA, true);
    }

    public static boolean cuelloAlto(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModamodComponents.CAPA_CUELLO));
    }

    public static void setCuelloAlto(ItemStack stack, boolean v) {
        if (!v) stack.remove(ModamodComponents.CAPA_CUELLO);
        else stack.set(ModamodComponents.CAPA_CUELLO, true);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("modamod.capa.tooltip",
                Text.translatable(largo(stack).traduccion()), Text.translatable(ruedo(stack).traduccion()))
                .formatted(Formatting.GRAY));
        if (capucha(stack)) tooltip.add(Text.translatable("modamod.capa.con_capucha").formatted(Formatting.GRAY));
        if (cuelloAlto(stack)) tooltip.add(Text.translatable("modamod.capa.con_cuello").formatted(Formatting.GRAY));
    }
}
