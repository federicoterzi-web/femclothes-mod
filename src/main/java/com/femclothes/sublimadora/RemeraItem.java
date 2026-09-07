package com.femclothes.sublimadora;

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

    /** El corte de la prenda. Sin componente, la remera comun. */
    public static Variante variante(ItemStack stack) {
        Variante v = stack.get(ModItems.VARIANTE);
        return v == null ? Variante.BASE : v;
    }

    /**
     * El nombre cambia con el corte. Un solo item que se llama siempre igual
     * dejaria prendas muy distintas indistinguibles en el inventario.
     *
     * Con 36 cortes no alcanza un nombre por combinacion, asi que se nombra
     * por el rasgo que mas la define -un croptop es un croptop tenga las
     * mangas que tenga- y el resto va al tooltip.
     */
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("femclothes.corte." + variante(stack).nombre());
    }

    /** Blanco por defecto: es el color de la tela sin tenir. */
    public static final int BLANCO = 0xF2F2F6;

    /**
     * El color base de la prenda, en RGB sin alfa.
     *
     * Usa el componente dyed_color de vanilla, que es el mismo que usan el
     * cuero y las prendas de FemClothes: lo pone la receta segun el color de
     * lana, sin codigo de por medio y sin un item por color.
     */
    public static int color(ItemStack stack) {
        return net.minecraft.component.type.DyedColorComponent.getColor(stack, BLANCO);
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

    /**
     * Las lineas de estampa, que sirven para cualquier prenda estampable.
     *
     * Estan aca sueltas y no dentro de appendTooltip porque las medias no son
     * un RemeraItem -son una prenda de FemClothes- y necesitan las mismas.
     */
    public static void tooltipEstampa(ItemStack stack, List<Text> tooltip) {
        Estampa frente = estampaDe(stack, Estampa.Cara.FRENTE);
        Estampa espalda = estampaDe(stack, Estampa.Cara.ESPALDA);
        if (frente == null && espalda == null) {
            tooltip.add(Text.translatable("femclothes.sublimadora.remera.en_blanco")
                    .formatted(Formatting.DARK_GRAY));
            return;
        }
        // Nombra las caras estampadas y no un "estampada" generico: con las
        // dos caras posibles, saber cual tiene dibujo es lo unico util.
        if (frente != null) {
            tooltip.add(Text.translatable("femclothes.sublimadora.remera.estampada",
                    Text.translatable("femclothes.sublimadora.cara.frente")).formatted(Formatting.AQUA));
        }
        if (espalda != null) {
            tooltip.add(Text.translatable("femclothes.sublimadora.remera.estampada",
                    Text.translatable("femclothes.sublimadora.cara.espalda")).formatted(Formatting.AQUA));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        // El corte completo, porque el nombre solo dice el rasgo dominante:
        // dos "remeras" pueden diferir en mangas y en cuello.
        Variante v = variante(stack);
        tooltip.add(Text.translatable("femclothes.sublimadora.corte.manga",
                Text.translatable("femclothes.sublimadora.manga." + v.manga().clave))
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.corte.cuello",
                Text.translatable("femclothes.sublimadora.cuello." + v.cuello().clave))
                .formatted(Formatting.GRAY));

        // Mismo mecanismo que las medias: el nombre del patron EN su color,
        // reusado de ClothingTrinketItem porque no hay nada especifico de
        // prenda en esa linea. Lado.AMBAS y sideKey null porque la remera
        // tiene un solo patron para toda la prenda, no por lado.
        tooltip.add(com.femclothes.item.ClothingTrinketItem.patternLine(
                stack, com.femclothes.region.Lado.AMBAS, null));

        tooltipEstampa(stack, tooltip);
    }
}
