package com.femclothes.item;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Pantalón: la prenda LARGA de pierna, análoga a la remera del torso — se
 * craftea de una sola longitud (largo completo) y se recorta después con
 * {@link MoldePantalonItem} en el telar, ciclando {@link PantalonLargo}.
 *
 * Es un {@link ClothingTrinketItem} normal —el tinte, el slot y el resto del
 * mecanismo son los mismos que cualquier otra prenda bilateral (medias,
 * antes shorts)—; lo único propio es el largo, guardado en
 * {@code FemclothesComponents.PANTALON_LARGO}.
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

    /**
     * El nombre cambia con el largo — igual que la remera, pero sin la
     * ambigüedad de tener que elegir "el rasgo que más define": acá cada uno
     * de los siete valores ya es un nombre propio y sin superposición.
     */
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(largo(stack).traduccion());
    }
}
