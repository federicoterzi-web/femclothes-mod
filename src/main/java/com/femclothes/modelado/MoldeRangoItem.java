package com.femclothes.modelado;

import com.femclothes.item.PrendaLore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de rango ABSTRACTO (mínimo/corto/medio/mediolargo/largo/máximo) —
 * a pedido, "sintetizar todos en esos dos moldes aunque cada prenda tenga
 * su propia medida": un solo set de ítems físicos sirve para las 4
 * categorías de extremidad (pantalón, medias, calientabrazos, manga de
 * remera), cada una traduciendo el mismo rango a SU propia escala real al
 * fijar (ver {@code ModeladoBlockEntity#fijar} y los métodos
 * {@code *DeRango}) — así se puede probar Anclaje/Lado variados sin tener
 * que cargar con 4 sets de moldes por-eje distintos.
 *
 * {@code MEDIOLARGO} se agregó después (a pedido, para que medias tenga
 * los 6 escalones parejos 2/4/6/8/10/12) — las otras 3 categorías no
 * tienen un 6to valor propio todavía, así que por ahora {@code
 * MEDIOLARGO} les mapea igual que {@code MEDIO} en esos ejes (ver
 * {@code pantalonDeRango}/{@code mangaDeRango}) hasta que se pida
 * diferenciarlo ahí también.
 *
 * A diferencia de los presets de combo directo (que ya traen SUS anclajes
 * horneados), este es un molde por-eje más: Anclaje/Lado de la Mesa siguen
 * aplicando normal, la única diferencia es que el VALOR sale de un rango
 * compartido en vez de un enum propio de la prenda.
 */
public class MoldeRangoItem extends Item {

    public enum Rango { MINIMO, CORTO, MEDIO, MEDIOLARGO, LARGO, MAXIMO }

    public final Rango rango;

    public MoldeRangoItem(Settings settings, Rango rango) {
        super(settings);
        this.rango = rango;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.molde").formatted(Formatting.GOLD));
        tooltip.add(PrendaLore.seUsaEn("pantalon", "medias", "calientabrazos", "remera").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
