package com.ejemplo.sublimadora;

import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RemeraItem extends Item {

    public RemeraItem(Settings settings) {
        super(settings);
    }

    public static ComponentType<Estampa> componente(Estampa.Cara cara) {
        return cara == Estampa.Cara.FRENTE ? ModItems.ESTAMPA_FRENTE : ModItems.ESTAMPA_ESPALDA;
    }

    /**
     * La estampa de esa cara, o null.
     *
     * El frente cae al componente viejo si no encuentra el nuevo: las remeras
     * de antes guardaban un UUID pelado y siempre iban centradas adelante.
     */
    @Nullable
    public static Estampa estampaDe(ItemStack stack, Estampa.Cara cara) {
        Estampa estampa = stack.get(componente(cara));
        if (estampa != null) return estampa;
        if (cara == Estampa.Cara.FRENTE) {
            UUID viejo = stack.get(ModItems.PICTURE_ID);
            if (viejo != null) return Estampa.centrada(viejo);
        }
        return null;
    }

    public static boolean estaEstampada(ItemStack stack) {
        return estampaDe(stack, Estampa.Cara.FRENTE) != null
                || estampaDe(stack, Estampa.Cara.ESPALDA) != null;
    }

    /** Copia la remera con la estampa aplicada en esa cara. */
    public static ItemStack estampar(ItemStack base, Estampa.Cara cara, Estampa estampa) {
        ItemStack out = base.copyWithCount(1);
        out.set(componente(cara), estampa);
        return out;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        Estampa frente = estampaDe(stack, Estampa.Cara.FRENTE);
        Estampa espalda = estampaDe(stack, Estampa.Cara.ESPALDA);
        if (frente == null && espalda == null) {
            tooltip.add(Text.translatable("sublimadora.remera.en_blanco").formatted(Formatting.DARK_GRAY));
            return;
        }
        // Nombra las caras estampadas y no un "estampada" generico: con las
        // dos caras posibles, saber cual tiene dibujo es lo unico util.
        if (frente != null) {
            tooltip.add(Text.translatable("sublimadora.remera.estampada",
                    Text.translatable("sublimadora.cara.frente")).formatted(Formatting.AQUA));
        }
        if (espalda != null) {
            tooltip.add(Text.translatable("sublimadora.remera.estampada",
                    Text.translatable("sublimadora.cara.espalda")).formatted(Formatting.AQUA));
        }
    }
}
