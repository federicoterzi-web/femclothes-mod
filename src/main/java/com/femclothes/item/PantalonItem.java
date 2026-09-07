package com.femclothes.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Pantalón: la prenda LARGA de pierna, análoga a la remera del torso — se
 * craftea de una sola longitud (largo completo) y se recorta después con
 * {@link MoldePantalonItem} en el telar, ciclando {@link PantalonLargo}.
 *
 * Es un {@link ClothingTrinketItem} normal —el tinte, el slot y el resto del
 * mecanismo son los mismos que cualquier otra prenda bilateral (medias,
 * antes shorts)—; lo propio son dos ejes independientes: el largo
 * ({@code PANTALON_LARGO}, hasta dónde llega la pierna) y el tiro
 * ({@code PANTALON_TIRO}, cuánto sube la cintura sobre el torso).
 */
public class PantalonItem extends ClothingTrinketItem {

    public PantalonItem(Settings settings) {
        super(settings, true);
    }

    /** El largo actual. Sin componente, pantalón completo (lo que sale del crafteo). */
    public static PantalonLargo largo(ItemStack stack) {
        PantalonLargo l = stack.get(FemclothesComponents.PANTALON_LARGO);
        return l == null ? PantalonLargo.PANTALON : l;
    }

    public static void setLargo(ItemStack stack, PantalonLargo largo) {
        // Pantalon completo es el default: no guardar nada para ese caso
        // mantiene compatible el pantalon recien crafteado sin pisarlo con
        // un componente redundante.
        if (largo == PantalonLargo.PANTALON) stack.remove(FemclothesComponents.PANTALON_LARGO);
        else stack.set(FemclothesComponents.PANTALON_LARGO, largo);
    }

    /** El tiro actual. Sin componente, MEDIO (cintura natural). */
    public static PantalonTiro tiro(ItemStack stack) {
        PantalonTiro t = stack.get(FemclothesComponents.PANTALON_TIRO);
        return t == null ? PantalonTiro.MEDIO : t;
    }

    public static void setTiro(ItemStack stack, PantalonTiro tiro) {
        if (tiro == PantalonTiro.MEDIO) stack.remove(FemclothesComponents.PANTALON_TIRO);
        else stack.set(FemclothesComponents.PANTALON_TIRO, tiro);
    }

    /**
     * El nombre cambia con el largo — igual que la remera, pero sin la
     * ambigüedad de tener que elegir "el rasgo que más define": acá cada uno
     * de los siete valores ya es un nombre propio y sin superposición. El
     * tiro no entra en el nombre (es un detalle secundario, como manga y
     * cuello en la remera) — va en el tooltip.
     */
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(largo(stack).traduccion());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.pantalon.tiro", Text.translatable(tiro(stack).traduccion()))
                .formatted(Formatting.GRAY));
    }
}
