package com.femclothes.render;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.ModelPart;
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

import java.util.function.Function;

/**
 * Renderer genérico para prendas "pegadas al cuerpo". En vez de usar la
 * geometría de armadura (más ancha, pensada para verse como una bota/peto
 * puestos encima), dibuja DIRECTO las ModelPart reales del jugador
 * (pierna, torso, brazo — ya posadas para este frame por el propio
 * renderer del jugador) con nuestra textura. Cero infle extra: la
 * silueta es exactamente la del cuerpo, como pedía el diseño original
 * ("que la skin reemplace la piel del jugador").
 *
 * ModelPart.render(...) ya aplica su propio pivote/rotación internamente
 * (es la misma llamada que usa el juego para dibujarse a sí mismo), así
 * que no hace falta transformar la matriz a mano antes de llamar.
 */
public final class BodyPartTrinketRenderer implements TrinketRenderer {

    public enum Part { LEGS, BODY, ARMS }

    private final Part part;
    private final Function<ItemStack, Identifier> textureProvider;

    public BodyPartTrinketRenderer(Part part, Function<ItemStack, Identifier> textureProvider) {
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

        Identifier texture = textureProvider.apply(stack);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(texture));

        switch (part) {
            case LEGS -> {
                biped.leftLeg.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
                biped.rightLeg.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            }
            case BODY -> biped.body.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            case ARMS -> {
                biped.leftArm.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
                biped.rightArm.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            }
        }
    }
}
