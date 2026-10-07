package com.femclothes.cinta;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Dibuja lo que cruza el empalme (2026-10-05, "la conjunction belt muestra los items diferentes y a diferente velocidad...
 * deberia ser un viaje smooth"): igual que en la cinta —la misma prenda con su ícono, el mismo ítem acostado— y a la misma
 * velocidad de 1 px por tick: de atrás recto, de un costado en arco como las curvas y desde arriba del centro al frente.
 */
public class EmpalmeRenderer implements BlockEntityRenderer<EmpalmeBlockEntity> {

    public EmpalmeRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(EmpalmeBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int luz, int overlay) {
        ItemStack stack = be.carga();
        if (stack.isEmpty() || be.getWorld() == null) return;
        float t = (be.tiempoEfectivo(tickDelta) - be.llegada()) / be.ticks();
        t = Math.max(0f, Math.min(1f, t));
        float px, pz, tx, tz;
        switch (be.lado()) {
            case 1 -> {
                float fi = t * (float) Math.PI / 2f, s = (float) Math.sin(fi), c = (float) Math.cos(fi);
                px = -0.5f + 0.5f * s; pz = -0.5f + 0.5f * c; tx = c; tz = -s;
            }
            case 2 -> {
                float fi = t * (float) Math.PI / 2f, s = (float) Math.sin(fi), c = (float) Math.cos(fi);
                px = 0.5f - 0.5f * s; pz = -0.5f + 0.5f * c; tx = -c; tz = -s;
            }
            case 3 -> { px = 0f; pz = -0.5f * t; tx = 0f; tz = -1f; }
            default -> { px = 0f; pz = 0.5f - t; tx = 0f; tz = -1f; }
        }
        float giro = (be.getCachedState().get(EmpalmeBlock.FACING).asRotation() + 180f) % 360f;   // el mismo giro "y" del blockstate
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-giro));
        CintaRenderer.dibujarCarga(stack, be.getWorld(), matrices, buffers, luz, overlay, px, 0f, pz, tx, 0f, tz);
        matrices.pop();
    }
}
