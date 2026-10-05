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

    /** Blanco de fábrica (2026-10-05, "sale blanco de fabrica"): igual que el retazo sin teñir; los colores se ponen en la Mesa de estilado. */
    public static final List<Integer> DE_FABRICA = List.of(0xF2F2F6, 0xF2F2F6, 0xF2F2F6);

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

    /** El dibujo de las 3 zonas (ala, cono, cinta); liso si no se eligió. */
    public static List<SombreroPatron> patrones(ItemStack stack) {
        List<Integer> p = stack.get(FemclothesComponents.PATRONES_SOMBRERO);
        SombreroPatron[] v = SombreroPatron.values();
        List<SombreroPatron> out = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int o = p != null && i < p.size() ? p.get(i) : 0;
            out.add(o >= 0 && o < v.length ? v[o] : SombreroPatron.LISO);
        }
        return out;
    }

    public static ItemStack conPatron(ItemStack stack, int zona, SombreroPatron patron) {
        List<SombreroPatron> actuales = patrones(stack);
        int[] n = {actuales.get(0).ordinal(), actuales.get(1).ordinal(), actuales.get(2).ordinal()};
        n[zona] = patron.ordinal();
        if (n[0] == 0 && n[1] == 0 && n[2] == 0) stack.remove(FemclothesComponents.PATRONES_SOMBRERO);
        else stack.set(FemclothesComponents.PATRONES_SOMBRERO, List.of(n[0], n[1], n[2]));
        return stack;
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
