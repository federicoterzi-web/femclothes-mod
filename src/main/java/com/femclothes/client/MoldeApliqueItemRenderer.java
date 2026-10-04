package com.femclothes.client;

import com.femclothes.aplique.Aplique;
import com.femclothes.aplique.MoldeApliquePersonalizadoItem;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Ícono del molde de aplique personalizado (2026-10-04, "el ícono del molde sea un fondo de papel kraft con el
 * objeto dibujado encima"): el papel de los moldes de aplique, con el grosor de un item/generated, y encima el
 * objeto del aplique dibujado por Minecraft como en el inventario (un bloque se ve en isométrico). Los moldes de
 * un modelo del mod (moño, mariposa, flor) llevan solo el papel. El nombre y el resto salen del tooltip.
 */
public class MoldeApliqueItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final Identifier PAPEL = Identifier.of("femclothes", "textures/item/molde_de_corte.png");
    /** Media del grosor de un item/generated (1/16). */
    private static final float Z = 0.5f / 16f;

    @Override
    public void render(ItemStack stack, ModelTransformationMode modo, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        // vanilla ya hizo translate(-0.5,-0.5,-0.5): el ítem ocupa [0,1]³.
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(PAPEL));
        MatrixStack.Entry e = matrices.peek();
        float zf = 0.5f + Z, za = 0.5f - Z;
        vertice(buffer, e, 0, 0, zf, 0, 1, 0, 0, 1, luz, overlay);
        vertice(buffer, e, 1, 0, zf, 1, 1, 0, 0, 1, luz, overlay);
        vertice(buffer, e, 1, 1, zf, 1, 0, 0, 0, 1, luz, overlay);
        vertice(buffer, e, 0, 1, zf, 0, 0, 0, 0, 1, luz, overlay);
        vertice(buffer, e, 0, 1, za, 0, 0, 0, 0, -1, luz, overlay);
        vertice(buffer, e, 1, 1, za, 1, 0, 0, 0, -1, luz, overlay);
        vertice(buffer, e, 1, 0, za, 1, 1, 0, 0, -1, luz, overlay);
        vertice(buffer, e, 0, 0, za, 0, 1, 0, 0, -1, luz, overlay);

        Aplique a = MoldeApliquePersonalizadoItem.plantilla(stack);
        if (a == null || a.objeto() == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        matrices.push();
        // Al centro de la cara de adelante, achicado, y aplastado para que un bloque no sobresalga del papel.
        matrices.translate(0.5f, 0.5f, zf + 0.02f);
        matrices.scale(0.62f, 0.62f, 0.2f);
        mc.getItemRenderer().renderItem(a.objeto().item(), ModelTransformationMode.GUI, luz, overlay, matrices,
                vertexConsumers, mc.world, 0);
        matrices.pop();
    }

    private static void vertice(VertexConsumer buffer, MatrixStack.Entry e, float x, float y, float z,
                                float u, float v, float nx, float ny, float nz, int luz, int overlay) {
        buffer.vertex(e.getPositionMatrix(), x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(overlay)
                .light(luz)
                .normal(e, nx, ny, nz);
    }
}
