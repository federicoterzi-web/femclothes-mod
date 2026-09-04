package com.femclothes.item;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Prenda "pegada al cuerpo" (medias, shorts, croptop, etc). A diferencia
 * de ClothingArmorItem, esta NO usa la geometría de armadura (más ancha)
 * — se registra en un slot custom de Trinkets y se dibuja directo sobre
 * las ModelPart reales del jugador vía BodyPartTrinketRenderer, así que
 * queda pegada al cuerpo como una "segunda piel" en vez de verse como
 * una bota/peto puestos encima.
 */
public class ClothingTrinketItem extends TrinketItem {

    public final boolean dyeable;

    public ClothingTrinketItem(Settings settings, boolean dyeable) {
        super(settings);
        this.dyeable = dyeable;
    }

    public int getColor(ItemStack stack) {
        if (!dyeable) return 0xFFFFFF;
        DyedColorComponent c = stack.get(DataComponentTypes.DYED_COLOR);
        return c != null ? c.rgb() : 0xFFFFFF;
    }

    public static void setColor(ItemStack stack, int rgb) {
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, true));
    }

    /**
     * Muestra que patron tiene puesto la prenda, y de que color. Sin esto la
     * unica forma de saberlo era ponersela.
     *
     * El nombre del patron se pinta CON el color del patron, asi el tooltip
     * dice las dos cosas de un saque. Si las piernas estan configuradas
     * distinto, se listan por separado.
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        Identifier left = ClothingStyle.patternId(stack, ClothingStyle.Side.LEFT);
        Identifier right = ClothingStyle.patternId(stack, ClothingStyle.Side.RIGHT);
        boolean sameStyle = java.util.Objects.equals(left, right)
                && ClothingStyle.baseColor(stack, ClothingStyle.Side.LEFT)
                    == ClothingStyle.baseColor(stack, ClothingStyle.Side.RIGHT)
                && ClothingStyle.patternColor(stack, ClothingStyle.Side.LEFT)
                    == ClothingStyle.patternColor(stack, ClothingStyle.Side.RIGHT);

        if (sameStyle) {
            tooltip.add(patternLine(stack, ClothingStyle.Side.LEFT, null));
        } else {
            tooltip.add(patternLine(stack, ClothingStyle.Side.LEFT, "femclothes.tooltip.left"));
            tooltip.add(patternLine(stack, ClothingStyle.Side.RIGHT, "femclothes.tooltip.right"));
        }
    }

    private static Text patternLine(ItemStack stack, ClothingStyle.Side side, String sideKey) {
        Identifier pattern = ClothingStyle.patternId(stack, side);
        Text name = pattern == null
                ? Text.translatable("femclothes.pattern.none").formatted(Formatting.DARK_GRAY)
                : Text.translatable("femclothes.pattern." + pattern.getPath())
                        .styled(st -> st.withColor(ClothingStyle.patternColor(stack, side)));

        if (sideKey == null) return name;
        return Text.translatable(sideKey).formatted(Formatting.GRAY).append(" ").append(name);
    }
}
