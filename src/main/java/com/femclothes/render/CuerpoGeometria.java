package com.femclothes.render;

import com.femclothes.garment.Parte;
import net.minecraft.client.model.ModelPart;
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

        /**
         * El cuerpo base. Calza EXACTO con la caja cruda de la skin (sin
         * inflar) — a pedido (2026-09-16): con 0.30 las dos piernas, que en
         * el esqueleto de Minecraft están pegadas sin espacio entre sí,
         * chocaban por el borde interno en cuanto se ponía cualquier tela
         * (hasta Pegado, que ya usa esta misma dilatación). Bajarlo a 0
         * saca el choque de raíz.
         *
         * <p><b>2026-09-16, segunda vuelta</b>: con CUERPO en 0 exacto
         * apareció un bug real jugando — "la preview de la Mesa y la de
         * inventario muestran la skin original a través de las prendas".
         * La tentación fácil era subir esto a un margen chico (se probó
         * 0.02) para que CUERPO gane siempre el empate de profundidad
         * contra la capa base de vanilla — pero eso es tapar el síntoma:
         * la capa BASE de la skin (a diferencia de la segunda capa/
         * overlay, que {@code SkinRegions} sí borra) no tiene un "hueco"
         * que se le pueda borrar con textura, así que CUALQUIER
         * dilatación mayor a 0 reabre un poco el choque de piernas para
         * nada. A pedido explícito ("no quiero dilatación, quiero que
         * nuestro modelo reemplace el original") se resolvió de raíz en
         * {@code LivingEntityRendererMixin}: apaga la capa base de
         * vanilla (ModelPart.visible=false) en las partes que gobierna
         * una prenda, justo antes de que vanilla la dibuje, y la prende
         * de vuelta justo después — con eso no hay NADA de vanilla ahí
         * con lo que competir, y CUERPO puede quedarse en 0 exacto sin
         * ningún margen.
         */
        CUERPO(ESCALA_CUERPO, 0.0F),

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

    /**
     * Variantes de TELA por dilatación — a pedido, molde de Calce (2026-09-15):
     * cada nivel de calce (Pegado/Ajustado/Normal/Suelto/Oversize) es una
     * dilatación distinta, cacheada por separado (una por valor real, no una
     * por prenda). CUERPO nunca varía de forma UNIFORME (ver
     * {@link #cuerpoSegmentado} para la excepción real, por tramos), sigue
     * cacheado entero en {@link #RAICES}. El "anillo de árbol" que documenta
     * la clase es el riesgo ya aceptado: dos prendas apiladas con calces muy
     * distintos pueden mostrarlo.
     */
    private static final Map<Float, ModelPart> RAICES_TELA_POR_DILATACION = new java.util.HashMap<>();

    /** Un tramo de fila del CUERPO con su propia dilatación — ver {@link #cuerpoSegmentado}. */
    public record SegmentoCuerpo(int filaDesde, int filaHasta, float dilatacion) {}

    private record PlantillaCuerpo(int uvX, int uvY, int originX, int originY, int originZ, int sizeX, int sizeZ) {}

    /**
     * Una entrada por nombre de parte (con el sufijo "_slim" para los
     * brazos finos) — mismos números que usa {@link #raiz}, factorizados
     * acá para que {@link #cuerpoSegmentado} pueda construir SOLO la parte
     * pedida en vez de las seis. CABEZA no está: la cabeza nunca lleva
     * cuerpo base (ver {@code GarmentFeatureRenderer}), nunca hace falta
     * partirla.
     */
    private static Map<String, PlantillaCuerpo> plantillasCuerpo() {
        return plantillas(Superficie.CUERPO.escala);
    }

    /** Las mismas cajas de {@link #raiz} (sin la cabeza) a la escala {@code S}. */
    private static Map<String, PlantillaCuerpo> plantillas(final int S) {
        int prof = 4 * S, brazo = 4 * S, brazoFino = 3 * S;
        Map<String, PlantillaCuerpo> m = new java.util.HashMap<>();
        m.put(Parte.TORSO.clave(), new PlantillaCuerpo(16 * S, 16 * S, -4 * S, 0, -2 * S, 8 * S, prof));
        m.put(Parte.PIERNA_DER.clave(), new PlantillaCuerpo(0, 16 * S, -2 * S, 0, -2 * S, brazo, prof));
        m.put(Parte.PIERNA_IZQ.clave(), new PlantillaCuerpo(16 * S, 48 * S, -2 * S, 0, -2 * S, brazo, prof));
        m.put(Parte.BRAZO_DER.clave(), new PlantillaCuerpo(40 * S, 16 * S, -3 * S, -2 * S, -2 * S, brazo, prof));
        m.put(Parte.BRAZO_IZQ.clave(), new PlantillaCuerpo(32 * S, 48 * S, -1 * S, -2 * S, -2 * S, brazo, prof));
        m.put(Parte.BRAZO_DER.clave() + "_slim", new PlantillaCuerpo(40 * S, 16 * S, -2 * S, -2 * S, -2 * S, brazoFino, prof));
        m.put(Parte.BRAZO_IZQ.clave() + "_slim", new PlantillaCuerpo(32 * S, 48 * S, -1 * S, -2 * S, -2 * S, brazoFino, prof));
        return m;
    }

    private static final Map<String, PlantillaCuerpo> PLANTILLAS_CUERPO = plantillasCuerpo();
    private static final Map<String, PlantillaCuerpo> PLANTILLAS_TELA = plantillas(Superficie.TELA.escala);
    private static final Map<String, ModelPart> RAICES_CUERPO_SEGMENTADO = new java.util.HashMap<>();

    private CuerpoGeometria() {}

    /**
     * La caja de esa parte para esa superficie.
     *
     * @param slim modelo de brazos finos. Solo cambia los brazos; pedirlo
     *             para una pierna no hace nada.
     */
    public static ModelPart parte(Superficie superficie, Parte parte, boolean slim) {
        return raiz(superficie, superficie.dilatacion).getChild(nombre(parte, slim));
    }

    /**
     * Como {@link #parte(Superficie, Parte, boolean)} pero con una
     * dilatación PROPIA en vez de la fija de {@code superficie} — para
     * TELA, el Calce de la prenda (ver {@code Calce.dilatacion}). Con
     * {@code superficie == CUERPO} el override se ignora (el cuerpo base
     * nunca varía).
     */
    public static ModelPart parte(Superficie superficie, Parte parte, boolean slim, float dilatacionPropia) {
        float dilatacion = superficie == Superficie.CUERPO ? superficie.dilatacion : dilatacionPropia;
        return raiz(superficie, dilatacion).getChild(nombre(parte, slim));
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

    /**
     * <h2>La "franja manchada" de la muñeca — bitácora completa (2026-09-16)</h2>
     * Reportado jugando varias veces seguidas, con tres explicaciones
     * DESCARTADAS antes de encontrar la real — se deja documentado en
     * detalle porque cada una parecía razonable y costó plata (tiempo)
     * descartarla:
     *
     * <ol>
     *   <li><b>Sombreado de motor.</b> Se pensó que {@code Direction.DOWN}
     *       de un cuboide se dibuja más oscura por luz direccional de
     *       Minecraft ({@code DiffuseLighting}). Descartado: el jugador
     *       reportó "es blanco, no oscuro" — no encajaba con una sombra.</li>
     *   <li><b>Hueco transparente en el archivo.</b> Cierto en parte: la
     *       tapa de la muñeca SÍ estaba transparente en los 9
     *       {@code cuerpo_*_larga_*.png} de remera y en los 4
     *       {@code calientabrazos_*_layer_1.png} — de ahí el "blanco" (se
     *       veía lo que hay detrás). Se corrigió pintándola, después se
     *       revirtió a transparente a propósito (ver más abajo, "piel
     *       reconstruida") — pero la mancha SIGUIÓ apareciendo con OTRO
     *       color, lo que probó que la causa real era otra.</li>
     *   <li><b>Tono de sombra de {@code CuerpoBaseTextures} muy oscuro.</b>
     *       Real y arreglado (ver {@link CuerpoBaseTextures#pintarCuerpo}):
     *       la tapa de la muñeca de la PIEL se pintaba al nivel ABAJO=0.15
     *       (pensado para la entrepierna/planta del pie, no para una
     *       muñeca), muy oscuro contra el lateral 0.32. Arreglar esto
     *       sacó la mancha para quien mira solo la piel desnuda — pero
     *       con una prenda puesta (remera-manga, calientabrazos) seguía
     *       apareciendo una LÍNEA de color, no un parche.</li>
     * </ol>
     *
     * <p><b>La causa real: sangrado de textura entre celdas UV vecinas.</b>
     * Probado con una calibración a propósito (pintar la tapa de arriba de
     * un color bien distinto a la de abajo, ej. verde vs rojo) — sale un
     * pixel de la tapa de ARRIBA metido adentro de la tapa de ABAJO, SIEMPRE
     * que las dos tengan colores distintos, sin importar qué esté pintado.
     * La causa es geométrica, no de contenido: en el layout de UV de
     * cualquier cuboide de Minecraft (ver {@code CajaSkin}), la tapa de
     * arriba y la de abajo son dos rectángulos PEGADOS uno al lado del otro
     * en la textura, sin ningún margen — el texel del borde compartido cae
     * justo en el límite. Se confirmó en el código fuente de Minecraft que
     * NO es filtrado bilinear/mipmap ({@code RenderLayer.getArmorCutoutNoCull}
     * pide {@code blur=false, mipmap=false} explícitamente) — es la
     * coordenada UV la que, en ese pixel exacto, cae del lado que no toca.
     * Por eso ninguna textura estática lo arregla: mientras arriba y abajo
     * tengan colores distintos (lo normal: hombro pintado vs muñeca vacía,
     * o cualquier tela vs piel), ese pixel de borde SIEMPRE se filtra.
     *
     * <p><b>La solución: que la GPU no tenga ese vértice para muestrear.</b>
     * Sacar la cara de la muñeca de la geometría elimina el sangrado de
     * raíz —sin vértice ahí, no hay UV que pueda caer mal— en vez de seguir
     * persiguiendo qué color "no se nota" al lado de cuál otro. Se saca
     * SOLO en {@link Superficie#TELA} (remera-manga, calientabrazos): ahí
     * la tapa de abajo suele quedar transparente (piel real abajo, ver
     * "piel reconstruida" en el historial) al lado de una tapa de arriba
     * bien opaca — la combinación que más sangra. En {@link Superficie#CUERPO}
     * se deja con las 6 caras: ahora que su propio tono de abajo/lateral
     * son iguales (punto 3 de arriba), no hay dos colores distintos pegados
     * y no hay nada que sangre.
     *
     * <p>Sí o sí las tres explicaciones descartadas quedan en el código y
     * no solo en el mensaje de commit: la próxima vez que alguien vea "una
     * mancha/línea rara en la muñeca" va a probar la sombra o el hueco
     * primero, como acá — que lea esto antes de repetir la vuelta.
     */
    private static final java.util.Set<net.minecraft.util.math.Direction> TODAS_LAS_CARAS =
            java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);

    /**
     * Todas las caras salvo la de ABAJO de verdad (la muñeca — ver
     * {@code Direction.UP} es la de abajo, {@code Direction.DOWN} la de
     * arriba: mismo gotcha de nombres invertidos que ya documenta
     * {@link #cuerpoSegmentado}). Solo para TELA en brazos — ver el
     * javadoc de {@link #TODAS_LAS_CARAS} para el porqué completo.
     *
     * <p>La tapa del HOMBRO (Direction.DOWN acá) se queda: a diferencia de
     * la muñeca, tiene un pixel correcto propio en la textura de la
     * prenda — el sangrado ahí se arregla PINTANDO ese pixel bien, no
     * sacando la cara (ver {@link com.femclothes.render.ClothingTextureCache}).
     */
    private static final java.util.Set<net.minecraft.util.math.Direction> SIN_MUNECA;
    static {
        SIN_MUNECA = java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
        SIN_MUNECA.remove(net.minecraft.util.math.Direction.UP);
    }

    private static ModelPart.Cuboid caja(int u, int v, float x, float y, float z,
                                          float sx, float sy, float sz, float dil, float texW,
                                          java.util.Set<net.minecraft.util.math.Direction> caras) {
        return new ModelPart.Cuboid(u, v, x, y, z, sx, sy, sz, dil, dil, dil, false, texW, texW, caras);
    }

    private static ModelPart raiz(Superficie superficie, float dilatacion) {
        if (superficie == Superficie.CUERPO) {
            ModelPart cacheada = RAICES.get(superficie);
            if (cacheada != null) return cacheada;
        } else {
            ModelPart cacheada = RAICES_TELA_POR_DILATACION.get(dilatacion);
            if (cacheada != null) return cacheada;
        }

        final int S = superficie.escala;
        // La dilatacion va en unidades de modelo, o sea escalada tambien: la
        // parte se achica despues por 1/S y las dos terminan en el mismo
        // grosor real, tenga la superficie la escala que tenga.
        float d = dilatacion * S;
        float texW = LayoutSkin.LADO * S;

        float alto = 12 * S, prof = 4 * S, brazo = 4 * S, brazoFino = 3 * S;

        // Mismo layout que PlayerEntityModel, todo multiplicado por S. Los
        // pivotes no importan: copyTransform los pisa con los del jugador.
        Map<String, ModelPart> hijos = new java.util.HashMap<>();
        hijos.put(Parte.CABEZA.clave(), new ModelPart(java.util.List.of(
                caja(0, 0, -4 * S, -8 * S, -4 * S, 8 * S, 8 * S, 8 * S, d, texW, TODAS_LAS_CARAS)), Map.of()));
        hijos.put(Parte.TORSO.clave(), new ModelPart(java.util.List.of(
                caja(16 * S, 16 * S, -4 * S, 0, -2 * S, 8 * S, alto, prof, d, texW, TODAS_LAS_CARAS)), Map.of()));
        hijos.put(Parte.PIERNA_DER.clave(), new ModelPart(java.util.List.of(
                caja(0, 16 * S, -2 * S, 0, -2 * S, brazo, alto, prof, d, texW, TODAS_LAS_CARAS)), Map.of()));
        hijos.put(Parte.PIERNA_IZQ.clave(), new ModelPart(java.util.List.of(
                caja(16 * S, 48 * S, -2 * S, 0, -2 * S, brazo, alto, prof, d, texW, TODAS_LAS_CARAS)), Map.of()));
        // La cara de la muñeca se saca SOLO en TELA (sangrado de UV entre
        // celdas vecinas — ver el javadoc largo de TODAS_LAS_CARAS arriba).
        java.util.Set<net.minecraft.util.math.Direction> carasBrazo =
                superficie == Superficie.TELA ? SIN_MUNECA : TODAS_LAS_CARAS;
        hijos.put(Parte.BRAZO_DER.clave(), new ModelPart(java.util.List.of(
                caja(40 * S, 16 * S, -3 * S, -2 * S, -2 * S, brazo, alto, prof, d, texW, carasBrazo)), Map.of()));
        hijos.put(Parte.BRAZO_IZQ.clave(), new ModelPart(java.util.List.of(
                caja(32 * S, 48 * S, -1 * S, -2 * S, -2 * S, brazo, alto, prof, d, texW, carasBrazo)), Map.of()));

        // Los brazos de una skin slim (Alex) son de 3 y arrancan en otro x.
        hijos.put(Parte.BRAZO_DER.clave() + "_slim", new ModelPart(java.util.List.of(
                caja(40 * S, 16 * S, -2 * S, -2 * S, -2 * S, brazoFino, alto, prof, d, texW, carasBrazo)), Map.of()));
        hijos.put(Parte.BRAZO_IZQ.clave() + "_slim", new ModelPart(java.util.List.of(
                caja(32 * S, 48 * S, -1 * S, -2 * S, -2 * S, brazoFino, alto, prof, d, texW, carasBrazo)), Map.of()));

        ModelPart raiz = new ModelPart(java.util.List.of(), hijos);
        if (superficie == Superficie.CUERPO) RAICES.put(superficie, raiz);
        else RAICES_TELA_POR_DILATACION.put(dilatacion, raiz);
        return raiz;
    }

    /**
     * Perfil de la pierna con volumen (2026-09-26, "el muslo empieza arriba
     * aunque no haya media, desde el tamaño normal de pierna, y en vez de
     * ensanchar reduce hacia la rodilla"): la cadera queda en el tamaño
     * NORMAL de la pierna (0) y desde ahí se va RESTANDO — el muslo se afina
     * hacia la rodilla, la pantorrilla recupera un poco, el tobillo se
     * afina de nuevo. Cada valor es la dilatación de esa fila respecto de la
     * base, en unidades de modelo (px de skin); negativo = más fina que la
     * caja vanilla. Fila 0 = cadera, fila 11 = tobillo/pie.
     */
    private static final float[] PERFIL_PIERNA = {
            0.00F, -0.03F, -0.08F, -0.15F, -0.23F, -0.31F, -0.38F, -0.31F, -0.25F, -0.28F, -0.45F, -0.36F};

    /**
     * Cuánto se marca el perfil según el calce: cuanto MÁS ajustada la
     * media, MÁS se marca (tela que aprieta = carne que abulta; a pedido).
     * Desde 2026-09-30 sale de {@code Calce.factorVolumen}: antes se
     * derivaba de la dilatación con una recta calibrada para los valores
     * viejos (normal 0.02), que con los calces nuevos daba cualquier cosa
     * — Ajustado ya aprieta con su propia dilatación (-0.25), así que no
     * necesita además exagerar el perfil.
     */
    private static float factorVolumen(float dilatacionBase) {
        com.femclothes.item.Calce calce = com.femclothes.item.Calce.de(dilatacionBase);
        return calce == null ? 1.0F : calce.factorVolumen;
    }

    private static float dilTelaFila(float dilatacionBase, int fila) {
        return dilatacionBase + PERFIL_PIERNA[fila] * factorVolumen(dilatacionBase);
    }

    private static final Map<String, ModelPart> RAICES_TELA_VOLUMEN = new java.util.HashMap<>();
    private static final Map<String, ModelPart> RAICES_CUERPO_VOLUMEN = new java.util.HashMap<>();

    /** Cuánto queda la piel por DENTRO de la tela en cada fila (para no atravesarla). */
    private static final float MARGEN_PIEL = 0.04F;

    /**
     * La TELA de una pierna partida en 12 filas, cada una con el ancho del
     * {@link #PERFIL_PIERNA} — misma técnica que {@link #cuerpoSegmentado} pero
     * a la escala de la tela. El grosor es solo en x/z (cada fila mantiene su
     * altura exacta, así el perfil puede ser negativo sin colapsar). La tapa
     * de arriba solo en la fila 0; la de abajo va aparte, con la caja entera
     * (su franja de UV no corre con la fila); y una PUNTA del pie hacia
     * adelante — la pierna vanilla no tiene pie, es una caja recta.
     */
    public static ModelPart telaConVolumenDePierna(Parte parte, float dilatacionBase) {
        String key = parte.clave() + "|" + dilatacionBase;
        ModelPart cacheada = RAICES_TELA_VOLUMEN.get(key);
        if (cacheada != null) return cacheada;

        final int S = Superficie.TELA.escala;
        float texW = LayoutSkin.LADO * S;
        int uvX = parte == Parte.PIERNA_DER ? 0 : 16 * S;
        int uvY = parte == Parte.PIERNA_DER ? 16 * S : 48 * S;
        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();
        for (int fila = 0; fila < 12; fila++) {
            java.util.EnumSet<net.minecraft.util.math.Direction> caras =
                    java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
            caras.remove(net.minecraft.util.math.Direction.UP);
            if (fila != 0) caras.remove(net.minecraft.util.math.Direction.DOWN);
            float dil = dilTelaFila(dilatacionBase, fila) * S;
            cuboides.add(new ModelPart.Cuboid(
                    uvX, uvY + fila * S,
                    -2 * S, fila * S, -2 * S,
                    4 * S, S, 4 * S,
                    dil, 0F, dil,
                    false, texW, texW, caras));
        }
        float dilPie = dilTelaFila(dilatacionBase, 11) * S;
        // tapa de abajo (la planta): caja entera, solo esa cara
        cuboides.add(new ModelPart.Cuboid(
                uvX, uvY,
                -2 * S, 0, -2 * S,
                4 * S, 12 * S, 4 * S,
                dilPie, 0F, dilPie,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
        // Punta del pie: 3px hacia adelante, últimas 2 filas. Su UV se acomoda para que la
        // cara de adelante caiga justo en las filas 10-11 de la cara delantera de la pierna
        // y los costados en el borde delantero de los laterales (continuidad de la tela).
        int prof = 3 * S;
        cuboides.add(new ModelPart.Cuboid(
                uvX + 4 * S - prof, uvY + 14 * S - prof,
                -2 * S, 10 * S, -2 * S - prof,
                4 * S, 2 * S, prof,
                dilPie, 0F, dilPie,
                false, texW, texW, java.util.EnumSet.complementOf(java.util.EnumSet.of(net.minecraft.util.math.Direction.UP))));
        // Planta de la punta: sin altura, solo la cara de abajo, con el UV corrido para que
        // caiga en los primeros 3px de la tapa de abajo de la pierna (la del pie de verdad).
        cuboides.add(new ModelPart.Cuboid(
                uvX + 8 * S - prof - 4 * S, uvY,
                -2 * S, 12 * S, -2 * S - prof,
                4 * S, 0, prof,
                dilPie, 0F, dilPie,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
        ModelPart resultado = new ModelPart(cuboides, java.util.Map.of());
        RAICES_TELA_VOLUMEN.put(key, resultado);
        return resultado;
    }

    private static final Map<String, ModelPart> RAICES_TELA_POR_FILAS = new java.util.HashMap<>();

    /**
     * La TELA de una parte partida en sus 12 filas, cada una con su propia
     * dilatación — para el calce (2026-09-30, "el suelto mas holgado y si se
     * puede con caida y el oversize super grande" / "además cuelga más
     * abajo") y para que una capa de arriba nunca quede por dentro de una de
     * abajo (ver {@code GarmentFeatureRenderer#dibujarPiezas}). Misma técnica
     * que {@link #telaConVolumenDePierna}: el grosor extra solo en x/z, cada
     * fila con su altura exacta y su franja de UV.
     *
     * <p>A diferencia de la caja entera de {@link #raiz} (que infla también
     * en y y estira la textura), acá las filas no se inflan en y. Para que
     * no quede una rendija arriba, la tapa de arriba va a -dil como en la
     * caja entera y una tira sin alto (UV de la primera fila) cierra el
     * costado entre la tapa y la fila 0. Abajo, la tapa solo va si la tela
     * llega a la fila 12 sin colgar (y nunca en brazos: ver {@link #SIN_MUNECA}).
     *
     * @param dilFila  dilatación de cada una de las 12 filas, en píxeles de skin
     * @param hasta    fila (exclusiva) donde corta la tela — de ahí cuelga
     * @param colgado  filas extra por debajo de {@code hasta}, repitiendo la
     *                 franja de UV de la última fila con tela
     */
    public static ModelPart telaPorFilas(Parte parte, boolean slim, float[] dilFila, int hasta, int colgado,
                                         float dilColgado) {
        return telaPorFilas(parte, slim, dilFila, hasta, colgado, dilColgado, -1F);
    }

    /**
     * Cuánto se corre (px) cada costado en x de una tela con holgura {@code dil}
     * cuando el lado que da al cuerpo va con {@code interno} (2026-10-02,
     * "probar no ensanchar los lados internos que coexisten en torso y
     * brazos"): {mínimo x, máximo x}. El torso tiene los dos costados contra
     * los brazos; el brazo derecho (x local −3..1) da al torso por +x y el
     * izquierdo (−1..3) por −x. Con {@code interno} negativo, los dos {@code dil}.
     */
    public static float[] costadosX(Parte parte, float dil, float interno) {
        if (interno < 0F || interno >= dil) return new float[]{dil, dil};
        return switch (parte) {
            case TORSO -> new float[]{interno, interno};
            case BRAZO_DER -> new float[]{dil, interno};
            case BRAZO_IZQ -> new float[]{interno, dil};
            default -> new float[]{dil, dil};
        };
    }

    /**
     * Con {@code interno} ≥ 0: los costados que dan al cuerpo (ver
     * {@link #costadosX}) se inflan solo eso, así la manga y el torso de una
     * prenda holgada no se meten uno adentro del otro.
     */
    public static ModelPart telaPorFilas(Parte parte, boolean slim, float[] dilFila, int hasta, int colgado,
                                         float dilColgado, float interno) {
        String key = parte.clave() + "|" + slim + "|" + java.util.Arrays.toString(dilFila) + "|" + hasta
                + "|" + colgado + "|" + dilColgado + "|" + interno;
        ModelPart cacheada = RAICES_TELA_POR_FILAS.get(key);
        if (cacheada != null) return cacheada;
        // Cada combinación de calces/largos/capas es una entrada: tope para
        // que no crezca sin fin en una sesión larga.
        if (RAICES_TELA_POR_FILAS.size() > 512) RAICES_TELA_POR_FILAS.clear();

        boolean brazo = parte == Parte.BRAZO_DER || parte == Parte.BRAZO_IZQ;
        PlantillaCuerpo t = PLANTILLAS_TELA.get(nombre(parte, slim));
        // La cabeza no tiene plantilla (nunca lleva cuerpo): caja entera.
        if (t == null) return parte(Superficie.TELA, parte, slim, dilFila[0]);
        final int S = Superficie.TELA.escala;
        float texW = LayoutSkin.LADO * S;
        java.util.Set<net.minecraft.util.math.Direction> costados = java.util.EnumSet.of(
                net.minecraft.util.math.Direction.NORTH, net.minecraft.util.math.Direction.SOUTH,
                net.minecraft.util.math.Direction.EAST, net.minecraft.util.math.Direction.WEST);
        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();

        for (int fila = 0; fila < 12; fila++) {
            float dil = dilFila[fila] * S;
            // Costados en x: inflado y corrimiento para que el lado interno quede en "interno".
            float[] cx = costadosX(parte, dilFila[fila], interno);
            float ex = (cx[0] + cx[1]) / 2F * S, ox = (cx[1] - cx[0]) / 2F * S;
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY() + fila * S,
                    t.originX() + ox, t.originY() + fila * S, t.originZ(),
                    t.sizeX(), S, t.sizeZ(),
                    ex, 0F, dil,
                    false, texW, texW, costados));
        }

        // Tapa de arriba (Direction.DOWN = arriba, nombres invertidos) a -dil,
        // como la caja entera, más la tira que cierra el costado hasta la fila 0.
        float dilArriba = dilFila[0] * S;
        float[] cxA = costadosX(parte, dilFila[0], interno);
        float exA = (cxA[0] + cxA[1]) / 2F * S, oxA = (cxA[1] - cxA[0]) / 2F * S;
        cuboides.add(new ModelPart.Cuboid(
                t.uvX(), t.uvY(), t.originX() + oxA, t.originY(), t.originZ(),
                t.sizeX(), 12 * S, t.sizeZ(), exA, dilArriba, dilArriba,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.DOWN)));
        if (dilArriba > 0) {
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY(), t.originX() + oxA, t.originY() - dilArriba / 2F, t.originZ(),
                    t.sizeX(), 0F, t.sizeZ(), exA, dilArriba / 2F, dilArriba,
                    false, texW, texW, costados));
        }

        // Lo que cuelga: filas extra con la franja de UV de la última fila con tela.
        int ultima = Math.max(0, Math.min(11, hasta - 1));
        float dilC = dilColgado * S;
        for (int k = 0; k < colgado; k++) {
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY() + ultima * S,
                    t.originX(), t.originY() + (hasta + k) * S, t.originZ(),
                    t.sizeX(), S, t.sizeZ(),
                    dilC, 0F, dilC,
                    false, texW, texW, costados));
        }

        if (!brazo && hasta >= 12 && colgado == 0) {
            float dilAbajo = dilFila[11] * S;
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY(), t.originX(), t.originY(), t.originZ(),
                    t.sizeX(), 12 * S, t.sizeZ(), dilAbajo, dilAbajo, dilAbajo,
                    false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
            if (dilAbajo > 0) {
                cuboides.add(new ModelPart.Cuboid(
                        t.uvX(), t.uvY() + 12 * S - 1, t.originX(), t.originY() + 12 * S + dilAbajo / 2F, t.originZ(),
                        t.sizeX(), 0F, t.sizeZ(), dilAbajo, dilAbajo / 2F, dilAbajo,
                        false, texW, texW, costados));
            }
        }

        ModelPart resultado = new ModelPart(cuboides, Map.of());
        RAICES_TELA_POR_FILAS.put(key, resultado);
        return resultado;
    }

    /**
     * La PIEL de la pierna con la misma forma que la tela de arriba (apenas
     * por dentro) en las filas [desde, hasta) donde hay tela — "el muslo
     * tiene que incluir la piel, sino veo una media embolsada con el pie
     * vanilla flotando en el medio". Fuera de esas filas queda la caja normal.
     */
    public static ModelPart cuerpoConVolumenDePierna(Parte parte, float dilatacionBase, int desde, int hasta) {
        String key = parte.clave() + "|" + dilatacionBase + "|" + desde + "|" + hasta;
        ModelPart cacheada = RAICES_CUERPO_VOLUMEN.get(key);
        if (cacheada != null) return cacheada;

        PlantillaCuerpo t = PLANTILLAS_CUERPO.get(parte.clave());
        final int S = Superficie.CUERPO.escala;
        float texW = LayoutSkin.LADO * S;
        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();
        for (int fila = 0; fila < 12; fila++) {
            java.util.EnumSet<net.minecraft.util.math.Direction> caras =
                    java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
            caras.remove(net.minecraft.util.math.Direction.UP);
            if (fila != 0) caras.remove(net.minecraft.util.math.Direction.DOWN);
            // Con tela arriba, apenas por dentro de ella; sin tela (media corta), el perfil
            // pelado: el muslo tiene forma aunque no haya media ahí.
            float dil = (fila >= desde && fila < hasta)
                    ? (dilTelaFila(dilatacionBase, fila) - MARGEN_PIEL) * S
                    : dilTelaFila(dilatacionBase, fila) * S;
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY() + fila * S,
                    t.originX(), t.originY() + fila * S, t.originZ(),
                    t.sizeX(), S, t.sizeZ(),
                    dil, 0F, dil,
                    false, texW, texW, caras));
        }
        // Planta de la piel (pierna y punta): sin esto, desde abajo se ve el cielo a través
        // de la media (las filas sacan su tapa de abajo, ver arriba).
        float dilSuela = (11 >= desde && 11 < hasta)
                ? (dilTelaFila(dilatacionBase, 11) - MARGEN_PIEL) * S
                : dilTelaFila(dilatacionBase, 11) * S;
        cuboides.add(new ModelPart.Cuboid(
                t.uvX(), t.uvY(),
                t.originX(), 0, t.originZ(),
                t.sizeX(), 12 * S, t.sizeZ(),
                dilSuela, 0F, dilSuela,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
        cuboides.add(new ModelPart.Cuboid(
                t.uvX() + 8 * S - 3 * S - t.sizeX(), t.uvY(),
                t.originX(), 12 * S, t.originZ() - 3 * S,
                t.sizeX(), 0, 3 * S,
                dilSuela, 0F, dilSuela,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
        // Piel adentro de la punta de la media (si no, el pie queda hueco): mismo acomodo
        // de UV que la punta de tela, a escala de cuerpo.
        int profPie = 3 * S;
        float dilPieta = (11 >= desde && 11 < hasta)
                ? (dilTelaFila(dilatacionBase, 11) - MARGEN_PIEL) * S
                : dilTelaFila(dilatacionBase, 11) * S;
        cuboides.add(new ModelPart.Cuboid(
                t.uvX() + 4 * S - profPie, t.uvY() + 14 * S - profPie,
                t.originX(), 10 * S, t.originZ() - profPie,
                t.sizeX(), 2 * S, profPie,
                dilPieta, 0F, dilPieta,
                false, texW, texW, java.util.EnumSet.complementOf(java.util.EnumSet.of(net.minecraft.util.math.Direction.UP))));
        ModelPart resultado = new ModelPart(cuboides, java.util.Map.of());
        RAICES_CUERPO_VOLUMEN.put(key, resultado);
        return resultado;
    }

    /**
     * El CUERPO de UNA parte, partido en tramos de fila con su propia
     * dilatación cada uno — a pedido (2026-09-16): Calce Ajustado tiene que
     * verse más flaco que la piel SOLO donde la tela realmente tapa, no en
     * toda la parte (la panza que asoma bajo una musculosa ajustada sigue
     * con la piel normal). {@link GarmentFeatureRenderer} arma la lista de
     * tramos a partir de {@code Pieza#filaDesde}/{@code filaHasta} de cada
     * prenda puesta en esa parte, y solo llama acá cuando alguna realmente
     * comprime — con un tramo único [0,12) a 0.30 esto sería lo mismo que
     * {@link #parte(Superficie, Parte, boolean)} con CUERPO, así que el
     * caller usa ese camino más simple (y más cacheado) en el caso normal.
     *
     * <h2>Las tapas de un corte interno no tienen pixel correcto — se sacan</h2>
     * Probado en juego (2026-09-16): el UV de un cuboide de Minecraft ubica
     * las caras de ARRIBA y de ABAJO en una franja FIJA de la textura,
     * {@code [v, v+profundidad)}, sin importar la altura real del cuboide
     * — sólo las 4 caras LATERALES corren con la altura. Correr {@code v}
     * por tramo (para que esas laterales muestren la franja de piel que
     * les toca) hace que la tapa de un corte que no es el borde real de la
     * parte caiga en cualquier otro pixel de la piel — a veces pintado (se
     * ve como una franja de color de más) y a veces transparente (se ve
     * como un hueco). Esto es geometría vista desde arriba/abajo, no algo
     * cosmético: por eso en vez de aceptarlo se construye el
     * {@link ModelPart.Cuboid} A MANO (con su constructor público, que sí
     * deja elegir dilatación Y caras a la vez — {@code ModelPartBuilder}
     * no deja combinar ambas) y directamente NO se agregan esas caras:
     * <ul>
     *   <li>ABAJO de verdad (muñeca/tobillo) nunca — prácticamente nunca
     *       visible bajo la bota/el cuerpo, y ningún tramo puede mostrarla
     *       bien partida (su franja fija tampoco corre con la altura);</li>
     *   <li>ARRIBA de verdad (hombro/cadera) solo en el tramo que empieza
     *       en la fila 0 — esa sí es la tapa real y con {@code v} sin
     *       correr muestra el pixel que le corresponde.</li>
     * </ul>
     * <b>Ojo con el nombre del enum</b> (mismo gotcha que {@link #raiz} —
     * comprobado jugando): acá {@code Direction.DOWN} es la cara de
     * ARRIBA de verdad y {@code Direction.UP} la de ABAJO de verdad, al
     * revés de la intuición. Por eso abajo se saca SIEMPRE
     * {@code Direction.UP}, y {@code Direction.DOWN} solo cuando
     * {@code desde != 0}.
     * <p>Sin esas caras el corte no queda con un hueco real: es horizontal,
     * de canto para una cámara mirando al costado, y el tramo vecino (más
     * ancho, porque siempre es el que tiene MÁS dilatación de los dos —
     * ver {@code GarmentFeatureRenderer#segmentosCuerpo}) ya tapa esa
     * franja con sus propias caras laterales, correctamente texturadas.
     */
    public static ModelPart cuerpoSegmentado(Parte parte, boolean slim, java.util.List<SegmentoCuerpo> segmentos) {
        String nombre = nombre(parte, slim);
        String key = nombre + "|" + segmentos;
        ModelPart cacheada = RAICES_CUERPO_SEGMENTADO.get(key);
        if (cacheada != null) return cacheada;

        PlantillaCuerpo t = PLANTILLAS_CUERPO.get(nombre);
        final int S = Superficie.CUERPO.escala;
        float texW = LayoutSkin.LADO * S, texH = LayoutSkin.LADO * S;

        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();
        for (SegmentoCuerpo seg : segmentos) {
            int desde = seg.filaDesde(), hasta = seg.filaHasta();
            java.util.EnumSet<net.minecraft.util.math.Direction> caras =
                    java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
            caras.remove(net.minecraft.util.math.Direction.UP);
            if (desde != 0) caras.remove(net.minecraft.util.math.Direction.DOWN);
            float dil = seg.dilatacion() * S;
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY() + desde * S,
                    t.originX(), t.originY() + desde * S, t.originZ(),
                    t.sizeX(), (hasta - desde) * S, t.sizeZ(),
                    dil, dil, dil,
                    false, texW, texH, caras));
        }
        ModelPart resultado = new ModelPart(cuboides, java.util.Map.of());
        RAICES_CUERPO_SEGMENTADO.put(key, resultado);
        return resultado;
    }
}
