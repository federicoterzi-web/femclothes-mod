package com.femclothes.cinta;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/** Dibuja lo que está cruzando el empalme (2026-10-05): el ítem apoyado en el centro, sobre la losa. */
public class EmpalmeRenderer implements BlockEntityRenderer<EmpalmeBlockEntity> {

    public EmpalmeRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(EmpalmeBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int luz, int overlay) {
        ItemStack stack = be.carga();
        if (stack.isEmpty() || be.getWorld() == null) return;
        // Las prendas van como en la cinta (el ícono se ve plano); el resto como ítem suelto.
        CintaRenderer.dibujarItem(stack, be.getWorld(), matrices, buffers, luz, overlay, 0.5f, 4.5f / 16f, 0.5f);
    }
}
