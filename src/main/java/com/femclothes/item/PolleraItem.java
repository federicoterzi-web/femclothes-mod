package com.femclothes.item;

/**
 * Pollera — primera pasada (2026-09-16), solo la FORMA: sin eje de largo
 * ni de vuelo todavía, un {@link ClothingTrinketItem} liso. Comparte slot
 * con {@link PantalonItem} (ver {@code piernas/exterior.json}): es una
 * alternativa al pantalón, no algo que se lleve puesto a la vez.
 *
 * La geometría (16 pétalos radiales, técnica calcada de un mod de
 * referencia — ver {@code PolleraGeometria}) se construye a mano con el
 * mismo constructor público de {@code ModelPart.Cuboid} que ya usa
 * {@code CuerpoGeometria#cuerpoSegmentado}, sin GeckoLib: la forma no
 * depende de esa librería, solo su animación (pendiente, aparte).
 */
public class PolleraItem extends ClothingTrinketItem {

    public PolleraItem(Settings settings) {
        super(settings, true);
    }
}
