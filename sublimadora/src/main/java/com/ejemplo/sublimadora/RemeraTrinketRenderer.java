package com.ejemplo.sublimadora;

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

/**
 * Dibuja la remera puesta sobre el cuerpo.
 *
 * Arma sus propias cajas con la MISMA geometria y los MISMOS UV que las
 * ModelPart del jugador, y les copia la pose cada frame: asi la prenda sigue
 * exactamente la animacion del cuerpo en vez de flotar.
 *
 * La textura va en LAYOUT DE SKIN, que no tiene nada que ver con el sprite de
 * 16x16 del item: cada region es una cara de una ModelPart. Confundir los dos
 * layouts es el error mas facil de cometer aca.
 */
public class RemeraTrinketRenderer implements TrinketRenderer {

    private static final Identifier TELA =
            Identifier.of("sublimadora", "textures/entity/remera_cuerpo.png");

    /**
     * Cuanto se infla la prenda respecto del cuerpo.
     *
     * NO es para evitar z-fighting: la capa de render ya trae
     * VIEW_OFFSET_Z_LAYERING. Es que la skin del jugador tiene una SEGUNDA
     * CAPA -jacket, sleeves- que vanilla dibuja inflada 0.25, y una prenda
     * dibujada sobre la parte base queda por DENTRO de ella y la oclusion por
     * profundidad se la come. Hay que quedar apenas por afuera de esa capa:
     * bajar esto de 0.25 esconde la remera, subirlo la infla.
     */
    private static final float DILATACION = 0.3f;

    /**
     * Resolucion de la textura respecto de la skin.
     *
     * Subir el tamano de la textura solo no da mas definicion: en
     * ModelPart.Cuboid el rectangulo UV sale del TAMANO DEL CUBOIDE en
     * unidades de modelo, asi que la cara seguiria ocupando los mismos
     * texels. El truco es armar el cuboide a 8x y escalar la parte a un
     * octavo: misma silueta, ocho veces los pixeles para la estampa. Es
     * donde mas se nota, porque el logo y la centrada ocupan una fraccion
     * chica del torso y a 4x salian gruesos.
     */
    static final int ESCALA = 8;

    private static ModelPart raiz;

    private static ModelPart raiz() {
        if (raiz != null) return raiz;
        ModelData datos = new ModelData();
        ModelPartData p = datos.getRoot();
        Dilation d = new Dilation(DILATACION * ESCALA);
        float brazo = 4 * ESCALA, alto = 12 * ESCALA, prof = 4 * ESCALA;

        // Mismo layout que PlayerEntityModel, todo por ESCALA. Los pivotes no
        // importan: copyTransform los pisa con los del jugador.
        p.addChild("body", ModelPartBuilder.create().uv(16 * ESCALA, 16 * ESCALA)
                .cuboid(-4 * ESCALA, 0, -2 * ESCALA, 8 * ESCALA, alto, prof, d), ModelTransform.NONE);
        p.addChild("right_arm", ModelPartBuilder.create().uv(40 * ESCALA, 16 * ESCALA)
                .cuboid(-3 * ESCALA, -2 * ESCALA, -2 * ESCALA, brazo, alto, prof, d), ModelTransform.NONE);
        p.addChild("left_arm", ModelPartBuilder.create().uv(32 * ESCALA, 48 * ESCALA)
                .cuboid(-1 * ESCALA, -2 * ESCALA, -2 * ESCALA, brazo, alto, prof, d), ModelTransform.NONE);

        // Los brazos de una skin slim (Alex) son de 3 y no de 4, y arrancan en
        // otro x. Sin este par la manga queda visiblemente ancha, flotando por
        // fuera del brazo.
        float brazoFino = 3 * ESCALA;
        p.addChild("right_arm_slim", ModelPartBuilder.create().uv(40 * ESCALA, 16 * ESCALA)
                .cuboid(-2 * ESCALA, -2 * ESCALA, -2 * ESCALA, brazoFino, alto, prof, d), ModelTransform.NONE);
        p.addChild("left_arm_slim", ModelPartBuilder.create().uv(32 * ESCALA, 48 * ESCALA)
                .cuboid(-1 * ESCALA, -2 * ESCALA, -2 * ESCALA, brazoFino, alto, prof, d), ModelTransform.NONE);

        raiz = TexturedModelData.of(datos, 64 * ESCALA, 64 * ESCALA).createModel();
        return raiz;
    }

    @Override
    public void render(ItemStack stack, SlotReference slot, EntityModel<? extends LivingEntity> modelo,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz,
                       LivingEntity entidad, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (!(modelo instanceof BipedEntityModel<?> biped)) return;

        ModelPart r = raiz();
        String sufijo = esSlim(entidad) ? "_slim" : "";

        // La estampa no se dibuja aparte: va pintada ADENTRO de la textura de
        // la prenda. Asi sigue al cuerpo sin geometria extra y queda recortada
        // a la tela sola. Si todavia no se pudo componer -la foto no llego- se
        // usa la lisa, que es lo correcto mientras tanto.
        Identifier textura = EstampaTextures.cuerpoEstampado(
                RemeraItem.estampaDe(stack, Estampa.Cara.FRENTE),
                RemeraItem.estampaDe(stack, Estampa.Cara.ESPALDA),
                RemeraItem.color(stack));
        if (textura == null) textura = TELA;

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(textura));
        dibujar(r.getChild("body"), biped.body, matrices, buffer, luz);
        dibujar(r.getChild("right_arm" + sufijo), biped.rightArm, matrices, buffer, luz);
        dibujar(r.getChild("left_arm" + sufijo), biped.leftArm, matrices, buffer, luz);
    }

    /**
     * Si el jugador usa el modelo de brazos finos.
     *
     * Se lee de la skin y no del modelo porque PlayerEntityModel guarda su
     * thinArms privado y no lo expone.
     */
    private static boolean esSlim(LivingEntity entidad) {
        return entidad instanceof net.minecraft.client.network.AbstractClientPlayerEntity jugador
                && jugador.getSkinTextures().model()
                        == net.minecraft.client.util.SkinTextures.Model.SLIM;
    }

    /** Copia la pose ya calculada de la parte del jugador y dibuja la nuestra encima. */
    private static void dibujar(ModelPart nuestra, ModelPart delJugador, MatrixStack matrices,
                                VertexConsumer buffer, int luz) {
        if (!delJugador.visible) return;
        nuestra.copyTransform(delJugador);
        // copyTransform tambien copia la escala, asi que la nuestra va DESPUES.
        nuestra.xScale = nuestra.yScale = nuestra.zScale = 1.0F / ESCALA;
        nuestra.render(matrices, buffer, luz, OverlayTexture.DEFAULT_UV);
    }
}
