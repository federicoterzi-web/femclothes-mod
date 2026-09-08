package com.femclothes.item;

import com.femclothes.sublimadora.Variante;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Calentadores de brazo: prenda base del BRAZO, análoga a las medias en la
 * pierna — se pone debajo de la manga de la remera. Reemplaza al viejo
 * {@code ArmWarmerItem} (nunca enganchado al sistema de capas: ni color por
 * lado, ni Garment, ni Pieza — un cascarón inerte).
 *
 * Un solo eje, el molde reusado de remera (a pedido del dueño: no se
 * craftean moldes nuevos): cobertura ({@link Variante.Manga}, molde de
 * manga), cuánto brazo tapa la tela, contado desde la MUÑECA hacia arriba.
 *
 * NO tiene tiro. Se probó en la sesión del 2026-09-08 (ver docs/CANAL.md
 * v14-v15) y se retiró: según la arquitectura definitiva de
 * docs/MAQUINAS.md §"Categorías de patrones de modelado", tiro es
 * exclusivo de prendas inferiores (pantalón/calza) — nunca de medias ni
 * de cubrebrazos. Mismo criterio que {@link MediasLargo}: cobertura sola.
 */
public class CalientabrazosItem extends ClothingTrinketItem {

    public CalientabrazosItem(Settings settings) {
        super(settings, true);
    }

    /** La cobertura actual. Sin componente, LARGA (cobertura completa). */
    public static Variante.Manga cobertura(ItemStack stack) {
        Variante.Manga v = stack.get(FemclothesComponents.CALIENTABRAZOS_COBERTURA);
        return v == null ? Variante.Manga.LARGA : v;
    }

    public static void setCobertura(ItemStack stack, Variante.Manga cobertura) {
        if (cobertura == Variante.Manga.LARGA) stack.remove(FemclothesComponents.CALIENTABRAZOS_COBERTURA);
        else stack.set(FemclothesComponents.CALIENTABRAZOS_COBERTURA, cobertura);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.calientabrazos.cobertura",
                        Text.translatable("femclothes.sublimadora.manga." + cobertura(stack).clave))
                .formatted(Formatting.GRAY));
    }
}
