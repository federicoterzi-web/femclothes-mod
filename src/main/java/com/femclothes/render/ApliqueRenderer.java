package com.femclothes.render;

import com.femclothes.Femclothes;
import com.femclothes.aplique.Aplique;
import com.femclothes.garment.Parte;
import com.femclothes.item.Calce;
import com.femclothes.item.FemclothesComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dibuja los apliques de una prenda (2026-10-01, Mesa de estilado) pegados a
 * la parte del cuerpo donde se pusieron, con la pose de esa parte — así siguen
 * al brazo, a la pierna o a la cabeza como la tela.
 *
 * <p>El modelo es un {@code .geo.json} de GeckoLib (ver {@code tools/generar_apliques.py})
 * que se dibuja a mano recorriendo sus huesos y cubos ({@link RenderUtil} para
 * los pivotes y giros, como {@code GeoRenderer#renderRecursively}): no hay
 * entidad ni bloque detrás, y así queda en la misma pila de matrices que la
 * ropa. El modelo mira a -Z; acá esa dirección se apunta a la normal de la cara
 * donde se hizo click, con "arriba" hacia la cabeza.
 */
public final class ApliqueRenderer {

    private ApliqueRenderer() {}

    private static final Identifier ATLAS = Identifier.of(Femclothes.MOD_ID, "textures/entity/aplique_atlas.png");
    private static final int ANCHO_ZONA = 32;

    /** Los apliques de todas las prendas de {@code prendas} (los de caja; pollera y capa van en su malla). */
    public static void dibujar(List<ItemStack> prendas, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz) {
        for (ItemStack prenda : prendas) {
            dibujar(prenda, Math.max(0f, Calce.dilatacionEfectiva(prenda)), biped, matrices, vertexConsumers, luz);
        }
    }

    /**
     * Los apliques de caja de un ítem con {@code dil} de inflado: las prendas
     * del mod (su calce) y cualquier armadura o wearable (2026-10-02,
     * "extender apliques para toda armadura o wearable vanilla o de mods": lo
     * que sale del cuerpo según su slot, ver {@link #dilatacionDeSlot}).
     */
    public static void dibujar(ItemStack item, float dil, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz) {
        List<Aplique> apliques = item.get(FemclothesComponents.APLIQUES);
        if (apliques == null || apliques.isEmpty()) return;
        // El marco del cuerpo (antes de la pose de cada parte): a él pertenecen los desplazamientos de la tela blanda.
        Matrix3f marco = new Matrix3f(matrices.peek().getNormalMatrix());
        for (Aplique a : apliques) {
            if (a.superficie() == Aplique.Superficie.CAJA) dibujarUno(a, dil, biped, matrices, vertexConsumers, luz, marco);
        }
    }

    /**
     * Cuánto sale del cuerpo un ítem puesto en {@code slot} (px): lo de la
     * armadura vanilla (1 px casco/pechera/botas, 0.5 las piernas); los de
     * Trinkets y otros, medio px.
     */
    public static float dilatacionDeSlot(@org.jetbrains.annotations.Nullable net.minecraft.entity.EquipmentSlot slot) {
        if (slot == null) return 0.5f;
        return switch (slot) {
            case HEAD, CHEST, FEET -> 1.0f;
            case LEGS -> 0.5f;
            default -> 0.5f;
        };
    }

    /** Los apliques de {@code item} puestos sobre la malla {@code sup} (pollera o capa). */
    public static List<Aplique> apliquesEn(ItemStack item, Aplique.Superficie sup) {
        List<Aplique> apliques = item.get(FemclothesComponents.APLIQUES);
        if (apliques == null || apliques.isEmpty()) return List.of();
        return apliques.stream().filter(a -> a.superficie() == sup).toList();
    }

    /**
     * Los apliques anclados por UV a una malla que se acaba de dibujar
     * ({@link MallaCapturada}, 2026-10-02, "no se pueden generar apliques en
     * pollera ni capa"): se ubican donde quedó ese punto de la tela este
     * cuadro, con su normal y "arriba" hacia la cintura/los hombros, así
     * siguen el movimiento de la tela.
     */
    public static void dibujarEnMalla(List<Aplique> apliques, MallaCapturada malla,
                                      VertexConsumerProvider vertexConsumers, int luz, Matrix3f marco) {
        if (apliques.isEmpty() || malla.vacia()) return;
        float s = malla.escala();
        for (Aplique a : apliques) {
            MallaCapturada.Ubicacion ub = malla.enUv(a.x() / 64f, a.y() / 64f);
            if (ub == null) continue;
            BakedGeoModel modelo = GeckoLibCache.getBakedModels().get(a.modelo().geo());
            if (modelo == null) continue;
            Vector3f atras = new Vector3f(ub.normal()).negate();          // +Z del modelo
            Vector3f arriba = new Vector3f(ub.arriba());
            Vector3f derecha = new Vector3f(arriba).cross(atras);
            Matrix4f base = new Matrix4f(
                    derecha.x, derecha.y, derecha.z, 0,
                    arriba.x, arriba.y, arriba.z, 0,
                    atras.x, atras.y, atras.z, 0,
                    0, 0, 0, 1).rotateZ((float) Math.toRadians(a.giro()));
            Vector3f pos = new Vector3f(ub.pos()).add(new Vector3f(ub.normal()).mul(0.03f * s / 16f));
            MatrixStack ms = new MatrixStack();
            ms.peek().getPositionMatrix().translation(pos).mul(base).scale(s * a.escala());
            ms.peek().getNormalMatrix().set(base.get3x3(new Matrix3f()));
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(textura(a)));
            Blando blando = Blando.de(a, base.get3x3(new Matrix3f()), marco);
            for (GeoBone hueso : modelo.topLevelBones()) dibujarHueso(hueso, ms, vc, luz, blando);
        }
    }

    private static void dibujarUno(Aplique a, float dil, BipedEntityModel<?> biped, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int luz, Matrix3f marco) {
        ModelPart parte = CuerpoGeometria.delJugador(biped, a.parte());
        if (!parte.visible) return;
        BakedGeoModel modelo = GeckoLibCache.getBakedModels().get(a.modelo().geo());
        if (modelo == null) return;

        Vector3f n = new Vector3f(a.cara().getOffsetX(), a.cara().getOffsetY(), a.cara().getOffsetZ());
        matrices.push();
        parte.rotate(matrices);
        // Sobre la superficie de la tela: el punto del click (en la caja sin
        // inflar) corrido hacia afuera lo que infla el calce, + un pelito.
        float afuera = dil + 0.02f;
        // Sobre el busto (2026-10-02, "hay que contemplar los pechos para los
        // apliques"): el punto se guarda en el frente plano del torso y, si hay
        // busto, se apoya en la cúpula y rebota con ella. Sin busto, plano.
        com.femclothes.render.relieve.BustoRender.Punto enBusto =
                a.parte() == Parte.TORSO && a.cara() == net.minecraft.util.math.Direction.NORTH
                        && com.femclothes.render.relieve.BustoRender.actual != null
                        ? com.femclothes.render.relieve.BustoRender.sobreBusto(
                                com.femclothes.render.relieve.BustoRender.actual, a.x(), a.y(), afuera,
                                com.femclothes.render.relieve.BustoRender.carpaDe(com.femclothes.item.Calce.de(dil)))
                        : null;
        // Sobre la cola (2026-10-02): los de la espalda del torso, igual.
        if (enBusto == null && a.parte() == Parte.TORSO && a.cara() == net.minecraft.util.math.Direction.SOUTH
                && com.femclothes.render.relieve.BustoRender.actual != null) {
            enBusto = com.femclothes.render.relieve.BustoRender.sobreCola(
                    com.femclothes.render.relieve.BustoRender.actual, a.x(), a.y(), afuera,
                    com.femclothes.render.relieve.BustoRender.carpaDe(com.femclothes.item.Calce.de(dil)));
        }
        if (enBusto != null) {
            matrices.translate(enBusto.pos().x / 16f, enBusto.pos().y / 16f, enBusto.pos().z / 16f);
            n = enBusto.normal();
        } else {
            matrices.translate((a.x() + n.x * afuera) / 16f, (a.y() + n.y * afuera) / 16f, (a.z() + n.z * afuera) / 16f);
        }
        matrices.multiplyPositionMatrix(orientacion(n, a.giro()));
        matrices.scale(a.escala(), a.escala(), a.escala());

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(textura(a)));
        Blando blando = Blando.de(a, matrices.peek().getNormalMatrix(), marco);
        for (GeoBone hueso : modelo.topLevelBones()) dibujarHueso(hueso, matrices, vc, luz, blando);
        matrices.pop();
    }

    /**
     * De los ejes del modelo (Y arriba, -Z al frente) a los de la parte (y
     * hacia los pies): -Z → la normal de la cara, Y → hacia la cabeza
     * proyectado sobre la cara (en las tapas de arriba/abajo, hacia el frente).
     */
    private static Matrix4f orientacion(Vector3f normal, float giroGrados) {
        Vector3f arriba = new Vector3f(0, -1, 0);
        if (Math.abs(normal.y) > 0.9f) arriba.set(0, 0, -1);
        arriba.sub(new Vector3f(normal).mul(arriba.dot(normal))).normalize();
        Vector3f atras = new Vector3f(normal).negate();          // +Z del modelo
        Vector3f derecha = new Vector3f(arriba).cross(atras);    // +X = Y × Z
        Matrix4f m = new Matrix4f(
                derecha.x, derecha.y, derecha.z, 0,
                arriba.x, arriba.y, arriba.z, 0,
                atras.x, atras.y, atras.z, 0,
                0, 0, 0, 1);
        return m.rotateZ((float) Math.toRadians(giroGrados));
    }

    private static void dibujarHueso(GeoBone hueso, MatrixStack matrices, VertexConsumer vc, int luz,
                                     @org.jetbrains.annotations.Nullable Blando blando) {
        matrices.push();
        if (blando != null) blando.mover(hueso, matrices);
        RenderUtil.prepMatrixForBone(matrices, hueso);
        for (GeoCube cubo : hueso.getCubes()) {
            matrices.push();
            RenderUtil.translateToPivotPoint(matrices, cubo);
            RenderUtil.rotateMatrixAroundCube(matrices, cubo);
            RenderUtil.translateAwayFromPivotPoint(matrices, cubo);
            Matrix4f pos = matrices.peek().getPositionMatrix();
            Matrix3f normales = matrices.peek().getNormalMatrix();
            for (GeoQuad quad : cubo.quads()) {
                if (quad == null) continue;
                Vector3f nq = normales.transform(new Vector3f(quad.normal()));
                for (GeoVertex v : quad.vertices()) {
                    Vector4f p = pos.transform(new Vector4f(v.position().x(), v.position().y(), v.position().z(), 1f));
                    vc.vertex(p.x(), p.y(), p.z()).color(0xFFFFFFFF).texture(v.texU(), v.texV())
                            .overlay(OverlayTexture.DEFAULT_UV).light(luz).normal(nq.x(), nq.y(), nq.z());
                }
            }
            matrices.pop();
        }
        for (GeoBone hijo : hueso.getChildBones()) dibujarHueso(hijo, matrices, vc, luz, blando);
        matrices.pop();
    }

    /**
     * La tela blanda de UN aplique (2026-10-04): los desplazamientos de
     * {@link FisicaApliques} pasados al espacio del modelo del aplique y
     * escalados por su blandura, y el giro que le toca a cada hueso según su
     * nombre:
     * <ul>
     *   <li>{@code cola*}: cadena de tramos anidados; cada tramo se inclina un poco
     *       hacia donde lo empuja la inercia (blando), así la cinta se curva;</li>
     *   <li>{@code ala*}: aleteo alrededor del eje largo, simétrico entre izq y der (firme);</li>
     *   <li>{@code petalo*}: la corola se inclina apenas (firme);</li>
     *   <li>{@code hojas}: cuelgan como una cola corta (blando).</li>
     * </ul>
     * El resto ({@code nudo}, {@code cuerpo}, {@code centro}) no se mueve.
     */
    private static final class Blando {
        final float blandura;
        /** Desplazamientos en el espacio del modelo del aplique (x, y, z). */
        final Vector3f blando, firme;

        Blando(float blandura, Vector3f blando, Vector3f firme) {
            this.blandura = blandura;
            this.blando = blando;
            this.firme = firme;
        }

        /**
         * null si el aplique es rígido o nada se mueve. {@code local} lleva del
         * modelo del aplique al espacio de dibujo; {@code marco} del modelo del
         * cuerpo al mismo espacio (las dos son rotaciones: la inversa es la transpuesta).
         */
        @org.jetbrains.annotations.Nullable
        static Blando de(Aplique a, Matrix3f local, Matrix3f marco) {
            FisicaApliques.Desplazamiento d = FisicaApliques.actual;
            if (d == null || a.blandura() <= 0f) return null;
            Matrix3f aLocal = new Matrix3f(local).transpose().mul(marco);
            return new Blando(a.blandura(), aLocal.transform(new Vector3f(d.blando())), aLocal.transform(new Vector3f(d.firme())));
        }

        void mover(GeoBone hueso, MatrixStack matrices) {
            String n = hueso.getName();
            float rx = 0f, ry = 0f, rz = 0f;
            if (n.startsWith("cola") || n.startsWith("cinta")) {
                rz = blando.x * 0.28f;
                rx = -blando.z * 0.28f;
            } else if (n.equals("hojas")) {
                rz = blando.x * 0.22f;
                rx = -blando.z * 0.2f;
            } else if (n.startsWith("ala")) {
                // En el espacio horneado el ala "izq" se abre hacia +x: gira para un lado y la "der" para el otro.
                float aleteo = (firme.y * 0.55f + firme.z * 0.3f) * (n.endsWith("izq") ? 1f : -1f);
                ry = aleteo;
                rz = firme.x * 0.1f;
            } else if (n.startsWith("petalo")) {
                rz = firme.x * 0.1f;
                rx = -firme.z * 0.1f;
            } else {
                return;
            }
            rx *= blandura;
            ry *= blandura;
            rz *= blandura;
            // Alrededor del pivote del hueso, en el marco del padre (antes del giro propio del hueso).
            matrices.translate(hueso.getPivotX() / 16f, hueso.getPivotY() / 16f, hueso.getPivotZ() / 16f);
            if (rz != 0f) matrices.multiply(new Quaternionf().rotationZ(rz));
            if (ry != 0f) matrices.multiply(new Quaternionf().rotationY(ry));
            if (rx != 0f) matrices.multiply(new Quaternionf().rotationX(rx));
            matrices.translate(-hueso.getPivotX() / 16f, -hueso.getPivotY() / 16f, -hueso.getPivotZ() / 16f);
        }
    }

    // ── textura teñida por zona ────────────────────────────────────────────
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    /** El atlas gris con cada columna de zona multiplicada por su color. */
    private static Identifier textura(Aplique a) {
        String key = Integer.toHexString(a.color(0)) + "_" + Integer.toHexString(a.color(1)) + "_" + Integer.toHexString(a.color(2));
        Identifier id = CACHE.get(key);
        if (id != null) return id;
        NativeImage base = ClothingTextureCache.imagenBase(ATLAS);
        if (base == null) return ATLAS;
        NativeImage img = new NativeImage(base.getWidth(), base.getHeight(), true);
        img.copyFrom(base);
        int escala = Math.max(1, base.getWidth() / 96);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int zona = Math.min(2, x / (ANCHO_ZONA * escala));
                int c = img.getColor(x, y);            // ABGR
                int rgb = a.color(zona);
                int r = ((c & 0xFF) * ((rgb >> 16) & 0xFF)) / 255;
                int g = (((c >> 8) & 0xFF) * ((rgb >> 8) & 0xFF)) / 255;
                int b = (((c >> 16) & 0xFF) * (rgb & 0xFF)) / 255;
                img.setColor(x, y, (c & 0xFF000000) | (b << 16) | (g << 8) | r);
            }
        }
        id = Identifier.of(Femclothes.MOD_ID, "dynamic/aplique_" + key);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        CACHE.put(key, id);
        return id;
    }

    /** Parte por parte, la caja de tela en píxeles (sin inflar) — la misma que usa el picking de la mesa. */
    public static float[] caja(Parte parte, boolean slim) {
        return switch (parte) {
            case CABEZA -> new float[] { -4, -8, -4, 8, 8, 8 };
            case TORSO -> new float[] { -4, 0, -2, 8, 12, 4 };
            case BRAZO_DER -> slim ? new float[] { -2, -2, -2, 3, 12, 4 } : new float[] { -3, -2, -2, 4, 12, 4 };
            case BRAZO_IZQ -> slim ? new float[] { -1, -2, -2, 3, 12, 4 } : new float[] { -1, -2, -2, 4, 12, 4 };
            case PIERNA_DER, PIERNA_IZQ -> new float[] { -2, 0, -2, 4, 12, 4 };
        };
    }
}
