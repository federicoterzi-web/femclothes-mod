package com.ejemplo.sublimadora;

import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Dibuja la remera con la foto estampada encima.
 *
 * La remera en si la sigue dibujando vanilla: no reimplementamos su aspecto,
 * se le pide al ItemRenderer el modelo item/remera_base -un item/generated
 * normal- y despues se le pega la foto adelante. Reimplementarla a mano
 * habria costado el relieve y el sombreado que vanilla le da gratis a
 * cualquier item plano.
 */
public class RemeraItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    /**
     * El modelo con el aspecto real de la remera. Tiene que ser OTRO archivo
     * que item/remera: ese heredo de builtin/entity para llegar hasta aca, y
     * pedirlo de nuevo seria recursion infinita.
     */
    static final Identifier MODELO_BASE = Identifier.of("sublimadora", "item/remera_base");

    /**
     * Alto y ancho del area imprimible sobre el item, en unidades de modelo.
     * La remera dibujada va de x=2 a x=14 de los 16, asi que una escala de
     * 1.0 cubre la tela sin pisar el borde. Todo lo que guarda la Estampa se
     * multiplica por esto.
     */
    private static final float AREA = 12f / 16f;
    /** Centro del area, un poco arriba del medio: el pecho, no la panza. */
    private static final float CENTRO_Y = 0.02f;
    /**
     * Medio pixel de item: el modelo item/generated tiene 1/16 de espesor
     * centrado en el origen, asi que sus caras estan en +-0.0313. Poner la
     * estampa justo ahi la deja pegada a la tela sin pelearse en el z-buffer.
     */
    private static final float Z_TELA = 0.0313f;

    @Override
    public void render(ItemStack stack, ModelTransformationMode modo, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        MinecraftClient cliente = MinecraftClient.getInstance();
        BakedModel base = ((FabricBakedModelManager) cliente.getBakedModelManager()).getModel(MODELO_BASE);

        if (base != null) {
            matrices.push();
            // renderItem vuelve a hacer translate(-0.5,-0.5,-0.5) por su
            // cuenta, y vanilla ya lo hizo antes de llamarnos. Sin esto la
            // remera sale corrida un bloque entero.
            matrices.translate(0.5f, 0.5f, 0.5f);
            // Con NONE no vuelve a aplicar la transformacion de perspectiva:
            // esa ya la aplico vanilla con el modelo builtin.
            cliente.getItemRenderer().renderItem(stack, ModelTransformationMode.NONE, false,
                    matrices, vertexConsumers, luz, overlay, base);
            matrices.pop();
        }

        matrices.push();
        matrices.translate(0.5f, 0.5f, 0.5f);
        for (Estampa.Cara cara : Estampa.Cara.values()) {
            Estampa estampa = RemeraItem.estampaDe(stack, cara);
            if (estampa == null) continue;
            CameraptureClientCompat.Foto foto = fotoDe(estampa.foto());
            if (foto == null) continue;   // todavia descargando, o sin Camerapture
            dibujar(matrices, vertexConsumers, foto, estampa, cara, luz, overlay);
        }
        matrices.pop();
    }

    /**
     * Se llama por frame, pero el acceso a Camerapture esta aislado detras de
     * isModLoaded para que la clase puente no se cargue si el mod no esta.
     */
    private static CameraptureClientCompat.Foto fotoDe(UUID id) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("camerapture")) return null;
        try {
            return CameraptureClientCompat.foto(id);
        } catch (Throwable ignorado) {
            return null;   // cambio de version del mod: mejor sin estampa que crashear
        }
    }

    /**
     * Un cuadrado con la foto, en una sola cara de la remera.
     *
     * Va con getEntityTranslucent y no getEntityCutout: cutout hace alfa
     * BINARIO -o solido o invisible- y se come cualquier degrade de un PNG
     * con transparencia parcial, que es justo lo que uno sube para un logo.
     */
    private static void dibujar(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                CameraptureClientCompat.Foto foto, Estampa estampa,
                                Estampa.Cara cara, int luz, int overlay) {
        // La foto entra en su caja sin deformarse: la apaisada queda mas
        // baja, la vertical mas angosta.
        float lado = AREA * estampa.escala();
        float ancho = lado, alto = lado;
        if (foto.ancho() >= foto.alto()) {
            alto = lado * foto.alto() / foto.ancho();
        } else {
            ancho = lado * foto.ancho() / foto.alto();
        }

        float cx = estampa.x() * AREA;
        float cy = CENTRO_Y + estampa.y() * AREA;
        float x0 = cx - ancho / 2f, x1 = cx + ancho / 2f;
        float y0 = cy - alto / 2f, y1 = cy + alto / 2f;

        VertexConsumer buffer = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucent(foto.textura()));
        MatrixStack.Entry entrada = matrices.peek();

        if (cara == Estampa.Cara.FRENTE) {
            // La v va invertida porque el eje Y del item crece hacia arriba y
            // el de la textura hacia abajo.
            vertice(buffer, entrada, x0, y0, Z_TELA, 0f, 1f, 0f, 0f, 1f, luz, overlay);
            vertice(buffer, entrada, x1, y0, Z_TELA, 1f, 1f, 0f, 0f, 1f, luz, overlay);
            vertice(buffer, entrada, x1, y1, Z_TELA, 1f, 0f, 0f, 0f, 1f, luz, overlay);
            vertice(buffer, entrada, x0, y1, Z_TELA, 0f, 0f, 0f, 0f, 1f, luz, overlay);
        } else {
            // Orden de vertices al reves para que la cara mire para atras y
            // no la descarte el culling, y la u espejada porque la espalda se
            // ve desde el otro lado.
            vertice(buffer, entrada, x0, y1, -Z_TELA, 1f, 0f, 0f, 0f, -1f, luz, overlay);
            vertice(buffer, entrada, x1, y1, -Z_TELA, 0f, 0f, 0f, 0f, -1f, luz, overlay);
            vertice(buffer, entrada, x1, y0, -Z_TELA, 0f, 1f, 0f, 0f, -1f, luz, overlay);
            vertice(buffer, entrada, x0, y0, -Z_TELA, 1f, 1f, 0f, 0f, -1f, luz, overlay);
        }
    }

    private static void vertice(VertexConsumer buffer, MatrixStack.Entry entrada,
                                float x, float y, float z, float u, float v,
                                float nx, float ny, float nz, int luz, int overlay) {
        buffer.vertex(entrada.getPositionMatrix(), x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(overlay)
                .light(luz)
                .normal(entrada, nx, ny, nz);
    }
}
