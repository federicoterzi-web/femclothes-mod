package com.femclothes.item;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Sombrero de bruja (2026-10-04, "modelemos y agreguemos un sombrero de bruja"): va en su propio slot de Trinkets
 * ({@code head/sombrero}), junto al casco. NO extiende {@link ClothingTrinketItem} a propósito: las máquinas de
 * prendas (Modeladora, Sublimadora, Guardarropas) lo tratarían como ropa de cuerpo. Tiene 3 zonas teñibles (ala,
 * cono y cinta, en {@code femclothes:colores_sombrero}), un ala (ancha/corta) y una punta (recta/doblada) y la
 * dibuja {@link com.femclothes.render.SombreroRenderer}; la punta se mece con la tela blanda de los apliques.
 */
public class SombreroBrujaItem extends TrinketItem {

    /** Violeta casi negro para el ala y el cono, y la cinta en un violeta más vivo. */
    public static final List<Integer> DE_FABRICA = List.of(0x2B1E3D, 0x2B1E3D, 0x8E44AD);

    public SombreroBrujaItem(Settings settings) {
        super(settings);
    }

    public static SombreroAla ala(ItemStack stack) {
        SombreroAla a = stack.get(FemclothesComponents.SOMBRERO_ALA);
        return a == null ? SombreroAla.ANCHA : a;
    }

    public static SombreroPunta punta(ItemStack stack) {
        SombreroPunta p = stack.get(FemclothesComponents.SOMBRERO_PUNTA);
        return p == null ? SombreroPunta.RECTA : p;
    }

    public static void setAla(ItemStack stack, SombreroAla v) {
        if (v == SombreroAla.ANCHA) stack.remove(FemclothesComponents.SOMBRERO_ALA);
        else stack.set(FemclothesComponents.SOMBRERO_ALA, v);
    }

    public static void setPunta(ItemStack stack, SombreroPunta v) {
        if (v == SombreroPunta.RECTA) stack.remove(FemclothesComponents.SOMBRERO_PUNTA);
        else stack.set(FemclothesComponents.SOMBRERO_PUNTA, v);
    }

    /** Los 3 colores (ala, cono, cinta); los de fábrica si el sombrero no se tiñó. */
    public static List<Integer> colores(ItemStack stack) {
        List<Integer> c = stack.get(FemclothesComponents.COLORES_SOMBRERO);
        return c == null || c.size() < 3 ? DE_FABRICA : c;
    }

    public static ItemStack conColores(ItemStack stack, int ala, int cono, int cinta) {
        stack.set(FemclothesComponents.COLORES_SOMBRERO, List.of(ala, cono, cinta));
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.sombrero.tooltip",
                Text.translatable(ala(stack).traduccion()), Text.translatable(punta(stack).traduccion()))
                .formatted(Formatting.GRAY));
        List<Integer> c = colores(stack);
        for (int i = 0; i < 3; i++) {
            int color = c.get(i);
            tooltip.add(Text.translatable("femclothes.sombrero.zona." + (i + 1)).formatted(Formatting.GRAY)
                    .append(Text.literal(" ■■■").styled(s -> s.withColor(TextColor.fromRgb(color)))));
        }
    }
}
