package com.ejemplo.sublimadora;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.UUID;

/**
 * Renderer del bloque. GeckoLib ya lo orienta con la propiedad
 * HORIZONTAL_FACING del estado, no hace falta rotar a mano.
 *
 * Registro (Fabric, en el ClientModInitializer):
 *   BlockEntityRendererFactories.register(ModBlocks.SUBLIMADORA_ENTITY,
 *       ctx -> new SublimadoraRenderer());
 */
public class SublimadoraRenderer extends GeoBlockRenderer<SublimadoraBlockEntity> {

    /** Altura de la foto apoyada: la remera va de 10.98 a 11.40. */
    private static final float Y_FOTO = 11.55f / 16f;
    /** Lado del papel sobre la plancha, en bloques. */
    private static final float LADO = 7f / 16f;

    public SublimadoraRenderer() {
        super(new SublimadoraGeoModel());
        // Capa emisiva: los pixeles marcados en sublimadora_atlas_glowmask.png
        // se dibujan a luz plena, asi los LEDs se ven prendidos aunque el
        // bloque este en penumbra. El sufijo _glowmask lo resuelve GeckoLib
        // solo, a partir del nombre de la textura del modelo.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(SublimadoraBlockEntity be, float parcial, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        super.render(be, parcial, matrices, vertexConsumers, luz, overlay);
        dibujarFotoCargada(be, matrices, vertexConsumers, luz, overlay);
    }

    /**
     * La foto esperando ser prensada, apoyada sobre la remera.
     *
     * Es el unico indicio de que hay una foto puesta: la maquina se traga el
     * papel y hasta que no termina el ciclo no hay forma de saber si estaba
     * cargada. No es un hueso del modelo porque la textura cambia con cada
     * foto, y un hueso tiene una sola textura, la del atlas.
     */
    private void dibujarFotoCargada(SublimadoraBlockEntity be, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        // getFotoVisible y no getFotoCargada: la de dibujar sigue un rato
        // mas mientras la tapa baja, y ya devuelve null con la tapa cerrada.
        UUID id = be.getFotoVisible();
        if (id == null) return;

        Identifier textura = texturaDe(id);
        if (textura == null) return;

        matrices.push();
        // Mismo centrado y misma rotacion que le aplica GeckoLib al modelo,
        // asi el papel acompana al bloque cuando esta girado.
        matrices.translate(0.5f, 0f, 0.5f);
        rotateBlock(getFacing(be), matrices);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(textura));
        MatrixStack.Entry entrada = matrices.peek();
        float m = LADO / 2f;

        // Boca arriba, mirando al cielo.
        vertice(buffer, entrada, -m, Y_FOTO, m, 0f, 1f, luz, overlay);
        vertice(buffer, entrada, m, Y_FOTO, m, 1f, 1f, luz, overlay);
        vertice(buffer, entrada, m, Y_FOTO, -m, 1f, 0f, luz, overlay);
        vertice(buffer, entrada, -m, Y_FOTO, -m, 0f, 0f, luz, overlay);

        matrices.pop();
    }

    private static void vertice(VertexConsumer buffer, MatrixStack.Entry entrada,
                                float x, float y, float z, float u, float v, int luz, int overlay) {
        buffer.vertex(entrada.getPositionMatrix(), x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(overlay)
                .light(luz)
                .normal(entrada, 0f, 1f, 0f);
    }

    private static Identifier texturaDe(UUID id) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("camerapture")) return null;
        try {
            CameraptureClientCompat.Foto foto = CameraptureClientCompat.foto(id);
            return foto == null ? null : foto.textura();
        } catch (Throwable ignorado) {
            return null;
        }
    }
}
