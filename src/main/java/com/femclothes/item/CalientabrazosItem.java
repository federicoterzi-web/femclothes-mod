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
 * Dos ejes, los DOS moldes reusados de otras prendas (a pedido del dueño: no
 * se craftean moldes nuevos):
 * - cobertura ({@link Variante.Manga}, molde de manga de remera): cuánto
 *   brazo tapa la tela, contado desde la MUÑECA hacia arriba.
 * - tiro ({@link PantalonTiro}, molde de tiro de pantalón): una banda extra
 *   que sube desde el hombro hacia el torso, misma técnica de runtime que la
 *   cintura del pantalón.
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

    /** El tiro actual. Sin componente, MEDIO. */
    public static PantalonTiro tiro(ItemStack stack) {
        PantalonTiro t = stack.get(FemclothesComponents.CALIENTABRAZOS_TIRO);
        return t == null ? PantalonTiro.MEDIO : t;
    }

    public static void setTiro(ItemStack stack, PantalonTiro tiro) {
        if (tiro == PantalonTiro.MEDIO) stack.remove(FemclothesComponents.CALIENTABRAZOS_TIRO);
        else stack.set(FemclothesComponents.CALIENTABRAZOS_TIRO, tiro);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.calientabrazos.cobertura",
                        Text.translatable("femclothes.sublimadora.manga." + cobertura(stack).clave))
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("femclothes.calientabrazos.tiro",
                        Text.translatable(tiro(stack).traduccion()))
                .formatted(Formatting.GRAY));
    }
}
