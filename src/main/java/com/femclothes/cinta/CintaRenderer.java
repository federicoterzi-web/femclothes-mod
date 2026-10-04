package com.femclothes.cinta;

import com.femclothes.render.IconoPrenda;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * Dibuja la prenda que viaja por la cinta (2026-10-04, "hace vos los assets"):
 * el ícono de 64x64 acostado sobre la banda, con el cuello hacia donde va. El
 * avance sale de la hora del mundo, así cuadra con la banda animada
 * (1 px por tick).
 */
public class CintaRenderer implements BlockEntityRenderer<CintaBlockEntity> {

    /** Mitad del lado del ícono, en bloques. */
    private static final float MEDIO = 0.3f;
    /** Altura de la banda (4,05 px) más un pelo. */
    private static final float ALTO = 4.2f / 16f;

    public CintaRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(CintaBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int luz, int overlay) {
        ItemStack stack = be.carga();
        if (stack.isEmpty() || be.getWorld() == null) return;
        Identifier icono = IconoPrenda.de(stack);
        if (icono == null) return;

        var estado = be.getCachedState();
        CintaBlock.Forma forma = estado.get(CintaBlock.FORMA);
        float t = (be.getWorld().getTime() + tickDelta - be.llegada()) / CintaBlockEntity.ticksDe(estado);
        t = Math.max(0f, Math.min(1f, t));

        // Posición y rumbo en el marco "mira al norte" centrado en el bloque.
        float px, pz, tx, tz;
        if (forma == CintaBlock.Forma.RECTA) {
            px = 0f; pz = 0.5f - t; tx = 0f; tz = -1f;
        } else {
            float fi = t * (float) Math.PI / 2f;
            float s = (float) Math.sin(fi), c = (float) Math.cos(fi);
            if (forma == CintaBlock.Forma.CURVA_IZQ) {
                px = -0.5f + 0.5f * s; pz = -0.5f + 0.5f * c; tx = c; tz = -s;
            } else {
                px = 0.5f - 0.5f * s; pz = -0.5f + 0.5f * c; tx = -c; tz = -s;
            }
        }

        Direction frente = estado.get(CintaBlock.FACING);
        float giroBloque = (frente.asRotation() + 180f) % 360f;   // el mismo giro "y" del blockstate
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-giroBloque));

        // Arriba de la prenda = hacia donde avanza; su derecha = ese rumbo girado 90° en sentido horario.
        float rx = -tz, rz = tx;
        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(icono));
        MatrixStack.Entry e = matrices.peek();
        vertice(vc, e, px - rx * MEDIO + tx * MEDIO, pz - rz * MEDIO + tz * MEDIO, 0, 0, luz);
        vertice(vc, e, px - rx * MEDIO - tx * MEDIO, pz - rz * MEDIO - tz * MEDIO, 0, 1, luz);
        vertice(vc, e, px + rx * MEDIO - tx * MEDIO, pz + rz * MEDIO - tz * MEDIO, 1, 1, luz);
        vertice(vc, e, px + rx * MEDIO + tx * MEDIO, pz + rz * MEDIO + tz * MEDIO, 1, 0, luz);
        matrices.pop();
    }

    private static void vertice(VertexConsumer vc, MatrixStack.Entry e, float x, float z, float u, float v, int luz) {
        vc.vertex(e.getPositionMatrix(), x, ALTO, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(luz)
                .normal(e, 0, 1, 0);
    }
}
