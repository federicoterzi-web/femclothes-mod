package com.femclothes;

import com.femclothes.item.ClothingPatternItem;
import com.femclothes.item.FemclothesDye;
import com.femclothes.screen.ClothingLoomScreenHandler;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Le da al Telar vanilla su segunda función: si hacés click derecho con
 * una prenda o un patrón de ropa en la mano, abre nuestra estación de
 * personalización en vez de la UI de banderas. Con cualquier otra cosa en
 * la mano el Telar se comporta como siempre.
 *
 * Por qué UseBlockCallback y no un Mixin sobre LoomScreenHandler: parchear
 * el Telar para que aceptara ropa en SU propia UI obligaría a tocar sus
 * slots (son clases anónimas internas) y además LoomScreen del lado
 * cliente, que dibuja una previsualización de bandera y el listado
 * scrolleable de patrones. Este evento de Fabric es un hook público y
 * estable, y nos deja reusar nuestro ScreenHandler entero sin tocar una
 * sola clase de Minecraft.
 */
public final class ClothingLoomInteraction {

    private static final Text TITLE = Text.translatable("container.femclothes.clothing_loom");

    private ClothingLoomInteraction() {}

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.LOOM)) {
                return ActionResult.PASS;
            }
            if (!opensClothingLoom(player.getStackInHand(hand))) {
                return ActionResult.PASS;
            }
            // En el cliente devolvemos PASS a propósito: las pantallas las
            // abre el servidor con un paquete, así que cortar acá dejaría
            // el click sin efecto. El servidor es el que decide.
            if (world.isClient) {
                return ActionResult.PASS;
            }

            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, p) -> new ClothingLoomScreenHandler(
                            syncId, inventory,
                            ScreenHandlerContext.create(world, hitResult.getBlockPos())),
                    TITLE));
            return ActionResult.SUCCESS;
        });
    }

    private static boolean opensClothingLoom(ItemStack stack) {
        return FemclothesDye.isClothing(stack) || stack.getItem() instanceof ClothingPatternItem;
    }
}
