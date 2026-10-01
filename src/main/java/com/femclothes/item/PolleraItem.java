package com.femclothes.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Pollera. Comparte slot con {@link PantalonItem} (ver
 * {@code piernas/exterior.json}): es una alternativa al pantalón, no algo
 * que se lleve puesto a la vez.
 *
 * <p>Rehecha (2026-09-29, "resolveme la pollera que se ve horrible a veces
 * y quiero poder hacerle distintos largos y teñirla y sublimarla"): largo
 * ({@link PolleraLargo}) y forma ({@link PolleraForma}) como componentes,
 * malla propia en {@code render.PolleraMalla} y tela con el layout de la
 * caja del torso, así Tintes y la Sublimadora la tratan como a las demás.
 */
public class PolleraItem extends ClothingTrinketItem {

    public PolleraItem(Settings settings) {
        super(settings, true);
    }

    public static PolleraLargo largo(ItemStack stack) {
        PolleraLargo l = stack.get(FemclothesComponents.POLLERA_LARGO);
        return l == null ? PolleraLargo.MEDIO : l;
    }

    public static void setLargo(ItemStack stack, PolleraLargo largo) {
        if (largo == PolleraLargo.MEDIO) stack.remove(FemclothesComponents.POLLERA_LARGO);
        else stack.set(FemclothesComponents.POLLERA_LARGO, largo);
    }

    public static PolleraForma forma(ItemStack stack) {
        PolleraForma f = stack.get(FemclothesComponents.POLLERA_FORMA);
        return f == null ? PolleraForma.CAMPANA : f;
    }

    public static void setForma(ItemStack stack, PolleraForma forma) {
        if (forma == PolleraForma.CAMPANA) stack.remove(FemclothesComponents.POLLERA_FORMA);
        else stack.set(FemclothesComponents.POLLERA_FORMA, forma);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.pollera.tooltip",
                Text.translatable(forma(stack).traduccion()), Text.translatable(largo(stack).traduccion()))
                .formatted(Formatting.GRAY));
    }
}
