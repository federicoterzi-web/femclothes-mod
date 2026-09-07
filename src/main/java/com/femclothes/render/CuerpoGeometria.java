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

import java.util.EnumMap;
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
 * <h2>Dos superficies, no mas</h2>
 * El cuerpo base va un pelin adentro (0.30) para no asomar nunca por fuera de
 * una prenda, y TODA la tela va a la misma (0.32). Apilar una dilatacion por
 * capa —0.30 / 0.32 / 0.34 / 0.36— deja un anillo de arbol en la silueta: se
 * ve el canto de cada prenda alrededor de la de abajo. Ver {@link Superficie}.
 *
 * <h2>La escala, y por que cada superficie tiene la suya</h2>
 * En ModelPart.Cuboid el rectangulo UV se calcula con el TAMANO DEL CUBOIDE
 * en unidades de modelo, no con el de la textura: subir textureWidth solo no
 * da mas definicion. El truco es armar el cuboide a ESCALA veces el tamano
 * real y escalar la parte por 1/ESCALA. Misma silueta, ESCALA veces los
 * pixeles.
 *
 * Lo que se deduce de esa cuenta es que la escala **no cambia la geometria**:
 * la fraccion de UV que ocupa una cara es {@code (tamano * S) / (64 * S)}, o
 * sea la misma para cualquier S. S decide unicamente cuantos texels caen
 * adentro. Por eso el cuerpo y la tela pueden tener escalas DISTINTAS sin que
 * nada se corra ni haya que alinear una grilla con la otra: son dos cajas
 * separadas, cada una con su textura, y el borde de una prenda lo define el
 * alfa de SU textura, no el texel del cuerpo que hay debajo.
 */
public final class CuerpoGeometria {

    /**
     * Ocho veces la skin: 512x512. La resolucion de una prenda ESTAMPADA.
     *
     * Fueron 2x hasta que las medias se volvieron estampables. A esa escala
     * una cara de pierna eran 8x24 texels y ninguna foto se lee ahi. Es una
     * resolucion que existe SOLO por las estampas — ver {@link #ESCALA_TELA_LISA}
     * y {@link #ESCALA_TELA_PATRON} para cuando no hace falta.
     */
    public static final int ESCALA_TELA = 8;

    /**
     * Dos veces la skin. La resolucion de una prenda sin foto y sin patron.
     *
     * Es el valor que ya tenia TODA la tela antes de que las medias se
     * volvieran estampables (ver el javadoc de {@link #ESCALA_TELA}): no es
     * un numero elegido a ciegas, es el que ya se probo que se ve bien.
     *
     * ⚠️ Esto NO es una escala de geometria como {@link #ESCALA_CUERPO}: la
     * fraccion de UV de un cuboide se cancela sola respecto de la escala con
     * la que se construyo (ver el javadoc de la clase), asi que una sola
     * geometria de tela sirve para CUALQUIER resolucion real de textura. Este
     * numero se usa del lado de la COMPOSICION (`ClothingTextureCache`), para
     * decidir a que tamano real reducir la imagen ya compuesta antes de
     * subirla a la GPU — ahi es donde esta la memoria que se ahorra.
     */
    public static final int ESCALA_TELA_LISA = 2;

    /**
     * Cuatro veces la skin. Una prenda con patron mas fino que un color
     * plano, pero que la sublimadora no puede estampar por foto.
     */
    public static final int ESCALA_TELA_PATRON = 4;

    /**
     * El cuerpo va a 1x: una skin de 64x64 y nada mas.
     *
     * Sobre el cuerpo no se sublima nunca —no hay fotos, no hay patrones, no
     * hay nada mas fino que un pixel de skin— asi que los 8x de la tela ahi
     * son 64 veces los texels para dibujar lo mismo. A 1x el cuerpo tiene
     * EXACTAMENTE la densidad de pixeles del jugador que esta debajo, que es
     * la que corresponde: es un cuerpo, no una prenda estampada.
     *
     * Tres cosas mas que salen de regalo:
     * <ul>
     *   <li>el arte del cuerpo se dibuja como lo que es, una skin, en
     *       cualquier editor de skins;</li>
     *   <li>1 MB por cuerpo pasa a 16 KB, y componerlo pasa de 262144 pixeles
     *       a 4096;</li>
     *   <li>es el formato al que hay que llegar igual para la ruta 3DSL (§8
     *       de PRENDAS.md), que compone dentro de la capa externa de la skin
     *       — o sea a 64x64.</li>
     * </ul>
     *
     * Si algun dia el arte pide sombreado mas fino que un pixel de skin, esto
     * sube a 2 y no hay que tocar nada mas.
     */
    public static final int ESCALA_CUERPO = 1;

    /**
     * Las dos superficies que dibuja el mod, cada una con su escala y su
     * dilatacion.
     *
     * Estan juntas en un enum y no sueltas para que no se pueda pedir una
     * combinacion que no existe. En particular no hay forma de dibujar tela a
     * una dilatacion propia: TODA la tela va a la misma, y apilar una por
     * capa deja un anillo de arbol en la silueta.
     */
    public enum Superficie {

        /** El cuerpo base. Adentro de la tela, para no asomar por el borde de una prenda. */
        CUERPO(ESCALA_CUERPO, 0.30F),

        /** Cualquier prenda, de cualquier capa. */
        TELA(ESCALA_TELA, 0.32F);

        public final int escala;
        public final float dilatacion;

        Superficie(int escala, float dilatacion) {
            this.escala = escala;
            this.dilatacion = dilatacion;
        }

        /** Lo que hay que ponerle a la ModelPart despues de copyTransform. */
        public float escalaDeParte() {
            return 1.0F / escala;
        }
    }

    private static final Map<Superficie, ModelPart> RAICES = new EnumMap<>(Superficie.class);

    private CuerpoGeometria() {}

    /**
     * La caja de esa parte para esa superficie.
     *
     * @param slim modelo de brazos finos. Solo cambia los brazos; pedirlo
     *             para una pierna no hace nada.
     */
    public static ModelPart parte(Superficie superficie, Parte parte, boolean slim) {
        return raiz(superficie).getChild(nombre(parte, slim));
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

    private static ModelPart raiz(Superficie superficie) {
        ModelPart cacheada = RAICES.get(superficie);
        if (cacheada != null) return cacheada;

        final int S = superficie.escala;

        ModelData datos = new ModelData();
        ModelPartData p = datos.getRoot();
        // La dilatacion va en unidades de modelo, o sea escalada tambien: la
        // parte se achica despues por 1/S y las dos terminan en el mismo
        // grosor real, tenga la superficie la escala que tenga.
        Dilation d = new Dilation(superficie.dilatacion * S);

        float alto = 12 * S, prof = 4 * S, brazo = 4 * S, brazoFino = 3 * S;

        // Mismo layout que PlayerEntityModel, todo multiplicado por S. Los
        // pivotes no importan: copyTransform los pisa con los del jugador.
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
        RAICES.put(superficie, raiz);
        return raiz;
    }
}
