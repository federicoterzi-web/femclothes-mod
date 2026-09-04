package com.femclothes.item;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Item de patrón reusable, como los patrones de estandarte de vanilla:
 * cada uno lleva un PATTERN_ID fijo (ej. "femclothes:stripe_top") que
 * se transfiere a la prenda en la estación de personalización. El item
 * de patrón NO se consume al usarlo (igual que un patrón de estandarte
 * en el Telar vanilla) — lo que se consume es el tinte.
 */
public class ClothingPatternItem extends Item {

    public final Identifier patternId;

    public ClothingPatternItem(Settings settings, Identifier patternId) {
        super(settings);
        this.patternId = patternId;
    }
}
