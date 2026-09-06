package com.femclothes.render;

import com.femclothes.garment.Parte;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Las cajas con las que se dibuja todo lo pegado al cuerpo.
 *
 * Es la fusion de lo que hacian por separado BodyPartTrinketRenderer y
 * RemeraTrinketRenderer, que armaban el mismo modelo con los mismos numeros.
 * Tenerlo dos veces era tolerable mientras cada prenda se dibujaba sola; con
 * el sistema de capas hace falta un solo lugar que sepa la geometria, porque
 * el sustrato y las prendas TIENEN que coincidir pixel a pixel.
 *
 * <h2>Por que no se dibuja sobre las ModelPart del jugador</h2>
 * La skin tiene una SEGUNDA CAPA (jacket, sleeves, pants) que vanilla dibuja
 * inflada 0.25. Una prenda dibujada sobre la parte base queda por DENTRO y la
 * oclusion por profundidad se la come. Hay que quedar apenas por afuera.
 * Ojo: NO es z-fighting. La capa de render ya trae VIEW_OFFSET_Z_LAYERING.
 *
 * <h2>Dos dilataciones, no mas</h2>
 * El cuerpo base va un pelin adentro (0.30) para no asomar nunca por fuera de
 * una prenda, y TODA la tela va a la misma (0.32). Apilar una dilatacion por
 * capa —0.30 / 0.32 / 0.34 / 0.36— deja un anillo de arbol en la silueta: se
 * ve el canto de cada prenda alrededor de la de abajo.
 *
 * <h2>La escala</h2>
 * En ModelPart.Cuboid el rectangulo UV se calcula con el TAMANO DEL CUBOIDE
 * en unidades de modelo, no con el de la textura: subir textureWidth solo no
 * da mas definicion. El truco es armar el cuboide a ESCALA veces el tamano
 * real y escalar la parte por 1/ESCALA. Misma silueta, ESCALA veces los
 * pixeles.
 */
public final class CuerpoGeometria {

    /**
     * Ocho veces la skin: 512x512.
     *
     * Fueron 2x hasta que las medias se volvieron estampables. A esa escala
     * una cara de pierna eran 8x24 texels y ninguna foto se lee ahi.
     */
    public static final int ESCALA = 8;

    /** El sustrato. Adentro de la tela para no asomar por el borde de una prenda. */
    public static final float DILATACION_CUERPO = 0.30F;

    /** Toda la tela, de cualquier prenda y de cualquier capa. */
    public static final float DILATACION_TELA = 0.32F;

    private static final Map<Float, ModelPart> RAICES = new HashMap<>();

    private CuerpoGeometria() {}

    /**
     * La caja de esa parte, a la dilatacion pedida.
     *
     * @param slim modelo de brazos finos. Solo cambia los brazos; pedirlo
     *             para una pierna no hace nada.
     */
    public static ModelPart parte(Parte parte, boolean slim, float dilatacion) {
        return raiz(dilatacion).getChild(nombre(parte, slim));
    }

    /** La ModelPart del jugador que le corresponde, para copiarle la pose. */
    public static ModelPart delJugador(BipedEntityModel<?> biped, Parte parte) {
        return switch (parte) {
            case CABEZA     -> biped.head;
            case TORSO      -> biped.body;
            case BRAZO_DER  -> biped.rightArm;
            case BRAZO_IZQ  -> biped.leftArm;
            case PIERNA_DER -> biped.rightLeg;
            case PIERNA_IZQ -> biped.leftLeg;
        };
    }

    private static String nombre(Parte parte, boolean slim) {
        boolean brazo = parte == Parte.BRAZO_DER || parte == Parte.BRAZO_IZQ;
        return parte.clave() + (slim && brazo ? "_slim" : "");
    }

    private static ModelPart raiz(float dilatacion) {
        ModelPart cacheada = RAICES.get(dilatacion);
        if (cacheada != null) return cacheada;

        ModelData datos = new ModelData();
        ModelPartData p = datos.getRoot();
        Dilation d = new Dilation(dilatacion * ESCALA);

        final int S = ESCALA;
        float alto = 12 * S, prof = 4 * S, brazo = 4 * S, brazoFino = 3 * S;

        // Mismo layout que PlayerEntityModel, todo multiplicado por ESCALA.
        // Los pivotes no importan: copyTransform los pisa con los del jugador.
        p.addChild(Parte.CABEZA.clave(), ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4 * S, -8 * S, -4 * S, 8 * S, 8 * S, 8 * S, d), ModelTransform.NONE);
        p.addChild(Parte.TORSO.clave(), ModelPartBuilder.create().uv(16 * S, 16 * S)
                .cuboid(-4 * S, 0, -2 * S, 8 * S, alto, prof, d), ModelTransform.NONE);
        p.addChild(Parte.PIERNA_DER.clave(), ModelPartBuilder.create().uv(0, 16 * S)
                .cuboid(-2 * S, 0, -2 * S, brazo, alto, prof, d), ModelTransform.NONE);
        p.addChild(Parte.PIERNA_IZQ.clave(), ModelPartBuilder.create().uv(16 * S, 48 * S)
                .cuboid(-2 * S, 0, -2 * S, brazo, alto, prof, d), ModelTransform.NONE);
        p.addChild(Parte.BRAZO_DER.clave(), ModelPartBuilder.create().uv(40 * S, 16 * S)
                .cuboid(-3 * S, -2 * S, -2 * S, brazo, alto, prof, d), ModelTransform.NONE);
        p.addChild(Parte.BRAZO_IZQ.clave(), ModelPartBuilder.create().uv(32 * S, 48 * S)
                .cuboid(-1 * S, -2 * S, -2 * S, brazo, alto, prof, d), ModelTransform.NONE);

        // Los brazos de una skin slim (Alex) son de 3 y arrancan en otro x.
        p.addChild(Parte.BRAZO_DER.clave() + "_slim", ModelPartBuilder.create().uv(40 * S, 16 * S)
                .cuboid(-2 * S, -2 * S, -2 * S, brazoFino, alto, prof, d), ModelTransform.NONE);
        p.addChild(Parte.BRAZO_IZQ.clave() + "_slim", ModelPartBuilder.create().uv(32 * S, 48 * S)
                .cuboid(-1 * S, -2 * S, -2 * S, brazoFino, alto, prof, d), ModelTransform.NONE);

        ModelPart raiz = TexturedModelData.of(datos, LayoutSkin.LADO * S, LayoutSkin.LADO * S)
                .createModel();
        RAICES.put(dilatacion, raiz);
        return raiz;
    }
}
