package com.femclothes.render;

import com.femclothes.Femclothes;
import com.femclothes.item.Calce;
import com.femclothes.item.ChaquetaItem;
import com.femclothes.sublimadora.RemeraItem;
import com.femclothes.sublimadora.Variante;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Geometría 3D que no es una caja de skin (2026-09-30):
 * <ul>
 *   <li>el <b>cuellito</b> de la polera ("agregame un cuellito para el
 *       molde polera"): un tubo corto acanalado alrededor de la base de la
 *       cabeza, atado al TORSO (no gira con la cabeza, como un cuello de
 *       verdad);</li>
 *   <li>la <b>capucha</b> del hoodie, caída en la espalda (atada al torso)
 *       o puesta (atada a la cabeza) según {@link ChaquetaItem#capuchaArriba};</li>
 *   <li>los <b>cordones</b> con sus puntitas, colgando del escote.</li>
 * </ul>
 * Todo en píxeles de skin (escala 1, {@code Superficie.CUERPO}) con una
 * textura lisa del color de la prenda generada en runtime
 * ({@link #texturaLisa}): exterior en u 0..32, forro más oscuro en u 32..64.
 * Los patrones y las estampas de la prenda no llegan acá (por ahora).
 */
public final class CuelloYCapucha {

    private CuelloYCapucha() {}

    /**
     * Cómo se ata el cuellito de la polera (2026-10-04, "el cuello de polera se atraviesa mucho... lo fijemos a la
     * cabeza en lugar del torso" + "dame un comando pa alternar entre los dos cuellitos"): {@code TORSO} como era
     * (quieto, la cabeza lo atraviesa al girar), {@code CABEZA} (gira con ella; al mirar abajo se hunde un poco en el
     * pecho) y {@code PARTIDO} (un aro bajo en el torso y el resto en la cabeza). Se cambia con
     * {@code /femclothesdebug cuello [torso|cabeza|partido]}; solo para quien lo usa.
     */
    public enum ModoCuello { TORSO, CABEZA, PARTIDO }

    public static ModoCuello modoCuello = ModoCuello.PARTIDO;

    private static final int TEX = 64;
    /** Por fuera de la segunda capa de la skin (sombrero), que infla 0.5. */
    private static final float SOBRE_SOMBRERO = 0.75F;
    private static final int CORDON = 0xF0EEE8;

    /** Dibuja cuello de polera, capucha y cordones de lo que haya en {@code prendas}. */
    public static void dibujar(List<ItemStack> prendas, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz) {
        ItemStack polera = null, chaqueta = null;
        for (ItemStack s : prendas) {
            if (!(s.getItem() instanceof RemeraItem)) continue;
            if (RemeraItem.variante(s).cuello() == Variante.Cuello.POLERA) polera = s;
            if (s.getItem() instanceof ChaquetaItem) chaqueta = s;
        }
        if (polera != null && biped.body.visible) {
            float d = Math.max(0F, Calce.dilatacionEfectiva(polera));
            Identifier tela = texturaLisa(RemeraItem.color(polera), true);
            switch (modoCuello) {
                case TORSO -> GarmentFeatureRenderer.dibujarModelPart(cuello(d), CuerpoGeometria.Superficie.CUERPO,
                        tela, biped.body, matrices, vertexConsumers, luz);
                case CABEZA -> {
                    if (biped.head.visible) GarmentFeatureRenderer.dibujarModelPart(cuello(d), CuerpoGeometria.Superficie.CUERPO,
                            tela, biped.head, matrices, vertexConsumers, luz);
                }
                case PARTIDO -> {
                    GarmentFeatureRenderer.dibujarModelPart(cuelloAro(d), CuerpoGeometria.Superficie.CUERPO,
                            tela, biped.body, matrices, vertexConsumers, luz);
                    if (biped.head.visible) GarmentFeatureRenderer.dibujarModelPart(cuelloArriba(d), CuerpoGeometria.Superficie.CUERPO,
                            tela, biped.head, matrices, vertexConsumers, luz);
                }
            }
        }
        // La capucha de la capa, puesta con la misma tecla (2026-09-30, "lo mismo
        // con la capucha de la capa activada por tecla"); caída la dibuja CapaMalla.
        ItemStack capa = GarmentFeatureRenderer.capaDe(prendas);
        if (chaqueta == null && capa != null && com.femclothes.item.CapaItem.capucha(capa)
                && ChaquetaItem.capuchaArriba(capa) && biped.head.visible) {
            int color = com.femclothes.region.RegionResolver.colorBase(capa, com.femclothes.region.Lado.IZQUIERDA);
            GarmentFeatureRenderer.dibujarModelPart(capuchaPuesta(0F), CuerpoGeometria.Superficie.CUERPO,
                    texturaLisa(color, false), biped.head, matrices, vertexConsumers, luz);
        }
        if (chaqueta != null) {
            float d = Math.max(0F, Calce.dilatacionEfectiva(chaqueta));
            Identifier tela = texturaLisa(RemeraItem.color(chaqueta), false);
            if (ChaquetaItem.capuchaArriba(chaqueta)) {
                if (biped.head.visible) {
                    GarmentFeatureRenderer.dibujarModelPart(capuchaPuesta(d), CuerpoGeometria.Superficie.CUERPO,
                            tela, biped.head, matrices, vertexConsumers, luz);
                }
            } else if (biped.body.visible) {
                GarmentFeatureRenderer.dibujarModelPart(capuchaCaida(d), CuerpoGeometria.Superficie.CUERPO,
                        tela, biped.body, matrices, vertexConsumers, luz);
            }
            if (biped.body.visible) {
                // La caída abre el frente hacia abajo: los cordones se separan lo mismo.
                Calce calce = Calce.leer(chaqueta);
                GarmentFeatureRenderer.dibujarModelPart(cordones(d + calce.caida * 0.5F), CuerpoGeometria.Superficie.CUERPO,
                        texturaLisa(CORDON, false), biped.body, matrices, vertexConsumers, luz);
            }
        }
    }

    // ── geometría ─────────────────────────────────────────────────────────
    private static final Map<String, ModelPart> CACHE = new HashMap<>();

    private static final Set<Direction> TODAS = EnumSet.allOf(Direction.class);

    private static ModelPart.Cuboid caja(int u, int v, float x, float y, float z, float sx, float sy, float sz,
                                         float dx, float dy, float dz, Set<Direction> caras) {
        return new ModelPart.Cuboid(u, v, x, y, z, sx, sy, sz, dx, dy, dz, false, TEX, TEX, caras);
    }

    private static ModelPart parte(String key, List<ModelPart.Cuboid> cuboides) {
        ModelPart p = new ModelPart(cuboides, Map.of());
        CACHE.put(key, p);
        return p;
    }

    /**
     * Cuello alto: la huella de la cabeza (8x8) apenas inflada, de 1.6 px de
     * alto sobre el torso (+ la mitad de la holgura del calce, para que no
     * lo tape la tapa de arriba de un torso holgado). Tapa la barbilla, no
     * la boca.
     */
    private static ModelPart cuello(float d) {
        String key = "cuello|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float alto = 1.6F + d * 0.5F, dil = SOBRE_SOMBRERO + d * 0.5F;
        return parte(key, List.of(caja(0, 0, -4, -alto, -4, 8, alto, 8, dil, 0, dil, TODAS)));
    }

    /** El aro bajo del cuello partido: 0,8 px sobre el torso, un poco más fino que la parte de la cabeza. */
    private static ModelPart cuelloAro(float d) {
        String key = "cuelloAro|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float dil = SOBRE_SOMBRERO - 0.2F + d * 0.5F;
        return parte(key, List.of(caja(0, 0, -4, -0.8F, -4, 8, 0.8F, 8, dil, 0, dil, TODAS)));
    }

    /** La parte del cuello partido que gira con la cabeza: de 0,8 px arriba del cuello hasta el alto del cuellito. */
    private static ModelPart cuelloArriba(float d) {
        String key = "cuelloArriba|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float alto = 1.6F + d * 0.5F, dil = SOBRE_SOMBRERO + d * 0.5F;
        return parte(key, List.of(caja(0, 0, -4, -alto, -4, 8, alto - 0.8F, 8, dil, 0, dil, TODAS)));
    }

    /**
     * Capucha puesta, en el marco de la cabeza (y -8..0): la caja de la
     * cabeza inflada, sin la cara de adelante (la cara del jugador), con un
     * bulto atrás (la punta) y un marco de forro alrededor de la cara.
     */
    private static ModelPart capuchaPuesta(float d) {
        String key = "puesta|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float dil = SOBRE_SOMBRERO + d * 0.25F;
        // Cerrada abajo (2026-10-02, "Hoodie capucha no cierra"): antes no tenía
        // tapa de abajo ni borde en la barbilla y se veía hueca desde abajo y de
        // costado. La tapa de abajo queda escondida debajo de la cabeza y solo
        // asoma el anillo alrededor del cuello, como una capucha cosida al hoodie.
        Set<Direction> sinCara = EnumSet.complementOf(EnumSet.of(Direction.NORTH));
        List<ModelPart.Cuboid> cs = new ArrayList<>();
        cs.add(caja(0, 0, -4, -8, -4, 8, 8, 8, dil, dil, dil, sinCara));
        // punta de atrás
        cs.add(caja(0, 0, -3, -6.5F, 4 + dil, 6, 5, 1.2F, 0, 0, 0, TODAS));
        // borde de forro alrededor de la cara (arriba y los dos costados)
        float e = 4 + dil, grosor = 1.1F;
        cs.add(caja(32, 0, -e, -8 - dil, -e, 2 * e, grosor, grosor, 0, 0, 0, TODAS));
        cs.add(caja(32, 0, -e, -8 - dil, -e, grosor, 8 + dil, grosor, 0, 0, 0, TODAS));
        cs.add(caja(32, 0, e - grosor, -8 - dil, -e, grosor, 8 + dil, grosor, 0, 0, 0, TODAS));
        // y el de abajo: cierra el marco de la cara por debajo de la barbilla.
        cs.add(caja(32, 0, -e, -0.6F, -e, 2 * e, 0.6F + dil, grosor, 0, 0, 0, TODAS));
        return parte(key, cs);
    }

    /** Capucha caída, en el marco del torso: una bolsa de tela apoyada en la nuca y la espalda, con la boca de forro arriba. */
    private static ModelPart capuchaCaida(float d) {
        String key = "caida|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float atras = 2 + d + 0.2F;
        List<ModelPart.Cuboid> cs = new ArrayList<>();
        cs.add(caja(0, 0, -4, -1.2F, atras, 8, 4.8F, 2.4F, 0, 0, 0, TODAS));
        cs.add(caja(0, 0, -3, 3.6F, atras + 0.2F, 6, 1.2F, 1.6F, 0, 0, 0, TODAS));
        // boca de la capucha: forro, apenas por encima de la tapa
        cs.add(caja(32, 0, -3.2F, -1.25F, atras + 0.4F, 6.4F, 0.05F, 1.6F, 0, 0, 0, TODAS));
        return parte(key, cs);
    }

    /** Dos cordones de 4 px colgando del escote, con una puntita un poco más gruesa. */
    private static ModelPart cordones(float d) {
        String key = "cordones|" + d;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float z = -2 - d - 0.45F;
        List<ModelPart.Cuboid> cs = new ArrayList<>();
        for (float x : new float[] { -1.8F, 1.4F }) {
            cs.add(caja(0, 0, x, 0.3F, z, 0.4F, 4, 0.4F, 0, 0, 0, TODAS));
            cs.add(caja(32, 0, x - 0.1F, 4.3F, z - 0.1F, 0.6F, 0.9F, 0.6F, 0, 0, 0, TODAS));
        }
        return parte(key, cs);
    }

    // ── textura ───────────────────────────────────────────────────────────
    private static final Map<Integer, Identifier> TEXTURAS = new HashMap<>();

    /**
     * Textura lisa de 64x64 del color {@code rgb}: exterior a la izquierda
     * (u 0..32), forro/puntita más oscuro a la derecha (u 32..64). Con
     * {@code rib}, canales verticales como el tejido de un cuello alto.
     */
    static Identifier texturaLisa(int rgb, boolean rib) {
        int key = (rgb & 0xFFFFFF) | (rib ? 0x1000000 : 0);
        Identifier id = TEXTURAS.get(key);
        if (id != null) return id;
        NativeImage img = new NativeImage(TEX, TEX, true);
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                float f = x >= 32 ? 0.72F : rib && (x % 2 == 0) ? 0.82F : 1F;
                int r = (int) (((rgb >> 16) & 0xFF) * f), g = (int) (((rgb >> 8) & 0xFF) * f), b = (int) ((rgb & 0xFF) * f);
                img.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        id = Identifier.of(Femclothes.MOD_ID, "dynamic/liso_" + Integer.toHexString(key));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        TEXTURAS.put(key, id);
        return id;
    }
}
