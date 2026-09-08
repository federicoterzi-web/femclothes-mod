package com.femclothes.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Item de patrón reusable, como los patrones de estandarte de vanilla:
 * cada uno lleva un PATTERN_ID fijo (ej. "femclothes:stripe_top") que
 * se transfiere a la prenda en la estación de personalización. El item
 * de patrón NO se consume al usarlo (igual que un patrón de estandarte
 * en el Telar vanilla) — lo que se consume es el tinte.
 *
 * A diferencia de los moldes (cambian el CORTE), un patrón pinta un motivo
 * sobre la tela y necesita un tinte puesto junto a él para saber de qué
 * color — por eso la categoría y el texto de ayuda son distintos, y el
 * color de la categoría (AQUA) es a propósito el opuesto del de los moldes
 * (GOLD): son las dos familias que más se confundían en el inventario.
 */
public class ClothingPatternItem extends Item {

    public final Identifier patternId;

    public ClothingPatternItem(Settings settings, Identifier patternId) {
        super(settings);
        this.patternId = patternId;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.categoria.patron").formatted(Formatting.AQUA));
        // Las únicas dos prendas cuyo Garment acepta la operación PATRON hoy
        // (ver PrendasDelMod.regionesDe) — pantalón y calientabrazos todavía
        // no tienen patrón, por diseño, no por olvido.
        tooltip.add(PrendaLore.seUsaEn("remera", "medias").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("femclothes.pattern.tooltip.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
