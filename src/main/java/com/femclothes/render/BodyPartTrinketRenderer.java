package com.femclothes.render;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import com.femclothes.item.ClothingStyle;



/**
 * Renderer genérico para prendas "pegadas al cuerpo". Dibuja cajas con la
 * MISMA geometría y los MISMOS UV que las ModelPart del jugador, copiándoles
 * la pose cada frame, así la prenda sigue la animación exacta del cuerpo.
 *
 * Por qué no se dibuja directo sobre las ModelPart del jugador (que era como
 * estaba antes): la skin del jugador tiene una SEGUNDA CAPA (leftPants,
 * jacket, sleeves) que vanilla dibuja inflada 0.25. Una prenda dibujada sobre
 * la parte base queda POR DENTRO de esa capa y el test de profundidad la
 * tapa — se veía en el juego, con la segunda capa de piel comiéndose parte de
 * las medias. Por eso inflamos un poco más que la capa externa.
 *
 * Ojo, esto NO era z-fighting: la capa de render que usamos ya trae
 * VIEW_OFFSET_Z_LAYERING. Era oclusión por profundidad, otra cosa.
 */
public final class BodyPartTrinketRenderer implements TrinketRenderer {

    /**
     * Apenas por afuera de la segunda capa de piel (0.25), para ganarle el
     * test de profundidad sin que la prenda se vea inflada. Subir esto hace
     * la ropa más "holgada"; bajarlo de 0.25 la vuelve a esconder.
     */
    private static final float DILATION = 0.3F;

    public enum Part { LEGS, BODY, ARMS }

    /**
     * Devuelve la textura ya compuesta para un lado del cuerpo. Recibe la
     * entidad porque algunas prendas necesitan el TONO DE PIEL del jugador
     * (una media 3/4 tiene que reconstruir la pierna desnuda de arriba).
     */
    @FunctionalInterface
    public interface TextureProvider {
        Identifier get(ItemStack stack, ClothingStyle.Side side, LivingEntity entity);
    }

    private static ModelPart root;

    private static ModelPart root() {
        if (root != null) return root;
        ModelData data = new ModelData();
        ModelPartData p = data.getRoot();
        Dilation d = new Dilation(DILATION);

        // Mismos uv/cuboid/pivot que PlayerEntityModel, solo cambia la
        // dilatación. Layout de skin 64x64: las dos piernas y los dos brazos
        // viven en regiones SEPARADAS.
        p.addChild("right_leg", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, d), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        p.addChild("left_leg", ModelPartBuilder.create().uv(16, 48)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, d), ModelTransform.pivot(1.9F, 12.0F, 0.0F));
        p.addChild("body", ModelPartBuilder.create().uv(16, 16)
                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, d), ModelTransform.NONE);
        p.addChild("right_arm", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, d), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        p.addChild("left_arm", ModelPartBuilder.create().uv(32, 48)
                .cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, d), ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        root = TexturedModelData.of(data, 64, 64).createModel();
        return root;
    }

    private final Part part;
    private final TextureProvider textureProvider;

    public BodyPartTrinketRenderer(Part part, TextureProvider textureProvider) {
        this.part = part;
        this.textureProvider = textureProvider;
    }

    @Override
    public void render(ItemStack stack, SlotReference slotReference,
                        EntityModel<? extends LivingEntity> entityModel,
                        MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                        int light, LivingEntity entity,
                        float limbAngle, float limbDistance, float tickDelta,
                        float animationProgress, float headYaw, float headPitch) {

        if (!(entityModel instanceof BipedEntityModel<?> biped)) return;

        ModelPart r = root();

        // Cada lado pide su propia textura: asi la pierna izquierda y la
        // derecha pueden tener color y patron distintos sin ser dos Items.
        switch (part) {
            case LEGS -> {
                draw(r.getChild("left_leg"), biped.leftLeg, stack, ClothingStyle.Side.LEFT,
                        entity, matrices, vertexConsumers, light);
                draw(r.getChild("right_leg"), biped.rightLeg, stack, ClothingStyle.Side.RIGHT,
                        entity, matrices, vertexConsumers, light);
            }
            case BODY -> draw(r.getChild("body"), biped.body, stack, ClothingStyle.Side.LEFT,
                    entity, matrices, vertexConsumers, light);
            case ARMS -> {
                draw(r.getChild("left_arm"), biped.leftArm, stack, ClothingStyle.Side.LEFT,
                        entity, matrices, vertexConsumers, light);
                draw(r.getChild("right_arm"), biped.rightArm, stack, ClothingStyle.Side.RIGHT,
                        entity, matrices, vertexConsumers, light);
            }
        }
    }

    /** Copia la pose ya calculada de la parte del jugador y dibuja la nuestra encima. */
    private void draw(ModelPart ours, ModelPart players, ItemStack stack, ClothingStyle.Side side,
                      LivingEntity entity, MatrixStack matrices,
                      VertexConsumerProvider vertexConsumers, int light) {
        Identifier texture = textureProvider.get(stack, side, entity);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(texture));
        ours.copyTransform(players);
        ours.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
    }
}
