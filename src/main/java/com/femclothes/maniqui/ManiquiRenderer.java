package com.femclothes.maniqui;

import com.femclothes.render.GarmentFeatureRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.List;

/**
 * Dibuja el Maniquí (GeckoLib) y, encima, la ropa que tiene puesta.
 *
 * <p>La ropa no es parte del modelo GeckoLib: se dibuja con
 * {@link GarmentFeatureRenderer#dibujarTela} sobre un modelo de jugador
 * SLIM invisible (solo se usan sus pivotes, nunca se dibuja), achicado a
 * {@link #ESCALA} y parado sobre el plato. La figura del {@code mannequin.geo.json}
 * está hecha a esa misma escala (torso 4.8 x 7 x 2.4 = 8 x 12 x 4 * 0.6;
 * brazos finos, pies en y=7.6, cabeza hasta y=27.2), así que la ropa le
 * calza como a un jugador.
 */
public class ManiquiRenderer extends GeoBlockRenderer<ManiquiBlockEntity> {

    /** Escala de la figura respecto de un jugador. */
    private static final float ESCALA = 0.6f;
    /** Altura de los pies de la figura (arriba del escalón del plato), en bloques. */
    private static final float ALTURA_PIES = 7.6f / 16f;

    private final PlayerEntityModel<LivingEntity> cuerpo;

    public ManiquiRenderer(BlockEntityRendererFactory.Context ctx) {
        super(new ManiquiGeoModel());
        this.cuerpo = new PlayerEntityModel<>(ctx.getLayerModelPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(ManiquiBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        // Primero el ángulo de este frame: lo leen tanto el hueso turntable
        // (ManiquiGeoModel, dentro de super.render) como la ropa de abajo.
        double tiempo = be.getWorld() == null ? 0 : be.getWorld().getTime() + (double) tickDelta;
        float angulo = be.avanzarAngulo(tiempo);

        super.render(be, tickDelta, matrices, vertexConsumers, luz, overlay);

        List<ItemStack> prendas = be.prendasPuestas();
        if (prendas.isEmpty()) return;

        matrices.push();
        // Misma transformación que GeoBlockRenderer le aplica al modelo
        // (centro del bloque + giro por FACING) y después el giro del plato
        // alrededor del mismo eje vertical que el hueso turntable.
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(giroPorFacing(be)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angulo));
        matrices.translate(0, ALTURA_PIES, 0);
        matrices.scale(ESCALA, ESCALA, ESCALA);
        // Lo mismo que hace LivingEntityRenderer antes de dibujar un modelo
        // de entidad: invertir X/Y y bajar 1.501 para que los pies queden en 0.
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0, -1.501f, 0);

        GarmentFeatureRenderer.dibujarTela(cuerpo, true, prendas, matrices, vertexConsumers, luz);
        matrices.pop();
    }

    /** Igual que {@code GeoBlockRenderer#rotateBlock} para los 4 horizontales. */
    private static float giroPorFacing(ManiquiBlockEntity be) {
        var estado = be.getCachedState();
        if (!estado.contains(ManiquiBlock.FACING)) return 0f;
        Direction facing = estado.get(ManiquiBlock.FACING);
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
    }
}
