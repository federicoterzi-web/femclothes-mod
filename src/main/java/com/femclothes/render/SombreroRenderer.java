package com.femclothes.render;

import com.femclothes.Femclothes;
import com.femclothes.item.SombreroAla;
import com.femclothes.item.SombreroBrujaItem;
import com.femclothes.item.SombreroPunta;
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sombrero de bruja (2026-10-04, "modelemos y agreguemos un sombrero de bruja"): geometría de cajas en el marco de
 * la cabeza, como el cuellito y la capucha de {@link CuelloYCapucha}. Tres zonas con su textura lisa del color
 * elegido: ala (una losa plana de {@link SombreroAla#lado}), cono (cuatro tramos que se afinan) y cinta (un aro un
 * poco más ancho que la base del cono). La punta son dos tramos anidados (hijos del cono) que se mecen con la tela
 * blanda ({@link FisicaApliques#actual}) y llevan la inclinación fija de {@link SombreroPunta}.
 * Y negativo es arriba; el frente de la cabeza es -Z.
 */
public final class SombreroRenderer {

    private SombreroRenderer() {}

    private static final int TEX = 64;
    private static final Set<Direction> TODAS = EnumSet.allOf(Direction.class);
    /** Cuánto gira la punta por bloque de desplazamiento de la tela blanda (rad). */
    private static final float GANANCIA = 0.35f;
    private static final float TOPE = 0.6f;

    // Medidas en px del modelo.
    private static final float Y_ALA = -8.2f, GROSOR_ALA = 0.9f;
    private static final float[] TRAMOS = {9.6f, 8.2f, 6.8f, 5.4f};
    private static final float ALTO_TRAMO = 2f;
    private static final float[] PUNTA1 = {4.0f, 2.8f};
    private static final float[] PUNTA2 = {1.8f, 0.9f};

    public static void dibujar(ItemStack sombrero, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz) {
        if (!biped.head.visible) return;
        SombreroAla ala = SombreroBrujaItem.ala(sombrero);
        SombreroPunta punta = SombreroBrujaItem.punta(sombrero);
        List<Integer> colores = SombreroBrujaItem.colores(sombrero);

        GarmentFeatureRenderer.dibujarModelPart(ala(ala), CuerpoGeometria.Superficie.CUERPO, textura(colores.get(0)),
                biped.head, matrices, vertexConsumers, luz);
        ModelPart cono = cono(punta);
        // La punta: inclinación de fábrica + lo que la inercia le suma (los dos tramos, el de arriba el doble).
        ModelPart p1 = cono.getChild("p1");
        ModelPart p2 = p1.getChild("p2");
        float pitch = 0f, roll = 0f;
        FisicaApliques.Desplazamiento d = FisicaApliques.actual;
        if (d != null) {
            roll = clamp(d.blando().x * GANANCIA);
            pitch = clamp(-d.blando().z * GANANCIA);
        }
        p1.pitch = punta.inclinacion1 + pitch * 0.5f;
        p1.roll = roll * 0.5f;
        p2.pitch = punta.inclinacion2 + pitch * 0.5f;
        p2.roll = roll * 0.5f;
        GarmentFeatureRenderer.dibujarModelPart(cono, CuerpoGeometria.Superficie.CUERPO, textura(colores.get(1)),
                biped.head, matrices, vertexConsumers, luz);
        GarmentFeatureRenderer.dibujarModelPart(cinta(), CuerpoGeometria.Superficie.CUERPO, textura(colores.get(2)),
                biped.head, matrices, vertexConsumers, luz);
    }

    /** Una caja del sombrero en el marco de la cabeza (px): para saber qué zona se clickea en la Mesa de estilado. */
    public record Caja(int zona, float[] min, float[] max) {}

    /**
     * Las cajas de cada zona (0 ala, 1 cono con su punta, 2 cinta) tal como las dibuja {@link #dibujar}, con la punta
     * recta (la inclinación y la tela blanda no se tienen en cuenta: alcanza para apuntar).
     */
    public static List<Caja> cajas(ItemStack sombrero) {
        SombreroAla ala = SombreroBrujaItem.ala(sombrero);
        float l = ala.lado;
        List<Caja> out = new java.util.ArrayList<>();
        out.add(new Caja(0, new float[] {-l / 2f, Y_ALA, -l / 2f}, new float[] {l / 2f, Y_ALA + GROSOR_ALA, l / 2f}));
        float cinta = TRAMOS[0] + 0.4f;
        out.add(new Caja(2, new float[] {-cinta / 2f, Y_ALA - 1.9f, -cinta / 2f}, new float[] {cinta / 2f, Y_ALA - 0.4f, cinta / 2f}));
        float y = Y_ALA;
        for (float s : TRAMOS) {
            out.add(new Caja(1, new float[] {-s / 2f, y - ALTO_TRAMO, -s / 2f}, new float[] {s / 2f, y, s / 2f}));
            y -= ALTO_TRAMO;
        }
        for (float s : PUNTA1) {
            out.add(new Caja(1, new float[] {-s / 2f, y - ALTO_TRAMO, -s / 2f}, new float[] {s / 2f, y, s / 2f}));
            y -= ALTO_TRAMO;
        }
        for (float s : PUNTA2) {
            out.add(new Caja(1, new float[] {-s / 2f, y - ALTO_TRAMO, -s / 2f}, new float[] {s / 2f, y, s / 2f}));
            y -= ALTO_TRAMO;
        }
        return out;
    }

    private static float clamp(float v) {
        return Math.max(-TOPE, Math.min(TOPE, v));
    }

    // ── geometría ─────────────────────────────────────────────────────────
    private static final Map<String, ModelPart> CACHE = new HashMap<>();

    private static ModelPart.Cuboid caja(float x, float y, float z, float sx, float sy, float sz) {
        return new ModelPart.Cuboid(0, 0, x, y, z, sx, sy, sz, 0, 0, 0, false, TEX, TEX, TODAS);
    }

    private static ModelPart ala(SombreroAla ala) {
        String key = "ala|" + ala;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        float l = ala.lado;
        c = new ModelPart(List.of(caja(-l / 2f, Y_ALA, -l / 2f, l, GROSOR_ALA, l)), Map.of());
        CACHE.put(key, c);
        return c;
    }

    private static ModelPart cinta() {
        ModelPart c = CACHE.get("cinta");
        if (c != null) return c;
        float l = TRAMOS[0] + 0.4f;
        c = new ModelPart(List.of(caja(-l / 2f, Y_ALA - 1.9f, -l / 2f, l, 1.5f, l)), Map.of());
        CACHE.put("cinta", c);
        return c;
    }

    /** El cono (raíz) con la punta como hijos {@code p1} y {@code p2}; la pose de la punta se pone en cada dibujo. */
    private static ModelPart cono(SombreroPunta punta) {
        ModelPart c = CACHE.get("cono");
        if (c != null) return c;
        List<ModelPart.Cuboid> base = new java.util.ArrayList<>();
        float y = Y_ALA;
        for (float s : TRAMOS) {
            y -= ALTO_TRAMO;
            // +0.02 de solape para que no se vea la rendija entre tramos.
            base.add(caja(-s / 2f, y, -s / 2f, s, ALTO_TRAMO + 0.02f, s));
        }
        float yCima = y;
        List<ModelPart.Cuboid> t2 = new java.util.ArrayList<>();
        float yy = 0f;
        for (float s : PUNTA2) {
            yy -= ALTO_TRAMO;
            t2.add(caja(-s / 2f, yy, -s / 2f, s, ALTO_TRAMO + 0.02f, s));
        }
        ModelPart parte2 = new ModelPart(t2, Map.of());
        List<ModelPart.Cuboid> t1 = new java.util.ArrayList<>();
        yy = 0f;
        for (float s : PUNTA1) {
            yy -= ALTO_TRAMO;
            t1.add(caja(-s / 2f, yy, -s / 2f, s, ALTO_TRAMO + 0.02f, s));
        }
        // el tramo de arriba se articula en el extremo del de abajo
        parte2.setPivot(0f, yy, 0f);
        ModelPart parte1 = new ModelPart(t1, Map.of("p2", parte2));
        parte1.setPivot(0f, yCima, 0f);
        c = new ModelPart(base, Map.of("p1", parte1));
        CACHE.put("cono", c);
        return c;
    }

    // ── textura lisa por color ────────────────────────────────────────────
    private static final Map<Integer, Identifier> TEXTURAS = new HashMap<>();

    private static Identifier textura(int rgb) {
        int key = rgb & 0xFFFFFF;
        Identifier id = TEXTURAS.get(key);
        if (id != null) return id;
        NativeImage img = new NativeImage(TEX, TEX, true);
        int abgr = 0xFF000000 | ((key & 0xFF) << 16) | (key & 0xFF00) | ((key >> 16) & 0xFF);
        for (int y = 0; y < TEX; y++) for (int x = 0; x < TEX; x++) img.setColor(x, y, abgr);
        id = Identifier.of(Femclothes.MOD_ID, "dynamic/sombrero_" + Integer.toHexString(key));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        TEXTURAS.put(key, id);
        return id;
    }
}
