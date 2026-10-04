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

        // Posición y rumbo en el marco "mira al norte" centrado en el bloque. En las rampas la banda sube/baja
        // un bloque entero en un bloque de recorrido (45°): el ícono se inclina con ella.
        float px, pz, tx, tz;
        float py = 0f, ty = 0f;
        if (forma.esRampa()) {
            // Sube: de 0 a +1 bloque. Baja: arranca un bloque arriba (a la altura de la cinta que le entrega) y baja a 0.
            boolean sube = forma == CintaBlock.Forma.RAMPA_SUBE;
            px = 0f; pz = 0.5f - t; tx = 0f; tz = -1f;
            py = sube ? t : 1f - t; ty = sube ? 1f : -1f;
        } else if (forma == CintaBlock.Forma.RECTA) {
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
        // Rumbo unitario en 3D y normal = derecha × rumbo.
        float lf = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        float fx = tx / lf, fy = ty / lf, fz = tz / lf;
        float nx = 0f * fz - fy * rz * 0f;   // (rx,0,rz) × (fx,fy,fz)
        nx = 0f * fz - rz * fy;
        float ny = rz * fx - rx * fz;
        float nz = rx * fy - 0f * fx;
        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(icono));
        MatrixStack.Entry e = matrices.peek();
        float cy = ALTO + py;
        vertice(vc, e, px - rx * MEDIO + fx * MEDIO, cy + fy * MEDIO, pz - rz * MEDIO + fz * MEDIO, 0, 0, luz, nx, ny, nz);
        vertice(vc, e, px - rx * MEDIO - fx * MEDIO, cy - fy * MEDIO, pz - rz * MEDIO - fz * MEDIO, 0, 1, luz, nx, ny, nz);
        vertice(vc, e, px + rx * MEDIO - fx * MEDIO, cy - fy * MEDIO, pz + rz * MEDIO - fz * MEDIO, 1, 1, luz, nx, ny, nz);
        vertice(vc, e, px + rx * MEDIO + fx * MEDIO, cy + fy * MEDIO, pz + rz * MEDIO + fz * MEDIO, 1, 0, luz, nx, ny, nz);
        matrices.pop();
    }

    private static void vertice(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float v, int luz,
                                float nx, float ny, float nz) {
        vc.vertex(e.getPositionMatrix(), x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(luz)
                .normal(e, nx, ny, nz);
    }
}
