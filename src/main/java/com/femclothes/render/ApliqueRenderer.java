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

    /** Los apliques de todas las prendas de {@code prendas}. */
    public static void dibujar(List<ItemStack> prendas, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz) {
        for (ItemStack prenda : prendas) {
            List<Aplique> apliques = prenda.get(FemclothesComponents.APLIQUES);
            if (apliques == null || apliques.isEmpty()) continue;
            float dil = Math.max(0f, Calce.dilatacionEfectiva(prenda));
            for (Aplique a : apliques) dibujarUno(a, dil, biped, matrices, vertexConsumers, luz);
        }
    }

    private static void dibujarUno(Aplique a, float dil, BipedEntityModel<?> biped, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int luz) {
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
        for (GeoBone hueso : modelo.topLevelBones()) dibujarHueso(hueso, matrices, vc, luz);
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

    private static void dibujarHueso(GeoBone hueso, MatrixStack matrices, VertexConsumer vc, int luz) {
        matrices.push();
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
        for (GeoBone hijo : hueso.getChildBones()) dibujarHueso(hijo, matrices, vc, luz);
        matrices.pop();
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
