package com.femclothes.render;

import com.femclothes.item.BandaAncho;
import com.femclothes.item.BandaHerraje;
import com.femclothes.item.BandaItem;
import com.femclothes.item.BandaZona;
import com.femclothes.item.SombreroPatron;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Banda (2026-10-05, "correas y cintos"): un aro de cajas en el marco de una parte del cuerpo — el torso para el
 * cinto, la cabeza para el choker — con 3 zonas: banda (cuatro losas), borde (dos tiras finas arriba y abajo, apenas
 * más afuera) y herraje (placa o aro al frente, -Z). Y positivo es hacia abajo. El cinto se infla según lo holgada
 * que es la ropa de abajo ({@code dil}); los colgantes son apliques de la Mesa de estilado, que dibuja
 * {@link ApliqueRenderer} en el mismo marco.
 */
public final class BandaRenderer {

    private BandaRenderer() {}

    /** Altura de la cintura en el torso (px desde arriba) y del choker bajo la cabeza. */
    private static final float Y_CINTURA = 7.6f, Y_CUELLO = 0f;
    private static final float GROSOR = 0.7f, BORDE_ALTO = 0.35f, BORDE_SALE = 0.12f;

    public static void dibujar(ItemStack banda, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz, float dil) {
        BandaZona zona = BandaItem.zona(banda);
        ModelPart marco = zona == BandaZona.CINTURA ? biped.body : biped.head;
        if (!marco.visible) return;
        List<Integer> colores = BandaItem.colores(banda);
        List<SombreroPatron> patrones = BandaItem.patrones(banda);
        List<SombreroRenderer.Caja> cajas = cajas(banda, dil);
        for (int z = 0; z < 3; z++) {
            ModelPart parte = parte(cajas, z, zona, banda, dil);
            if (parte == null) continue;
            GarmentFeatureRenderer.dibujarModelPart(parte, CuerpoGeometria.Superficie.CUERPO,
                    SombreroRenderer.textura(colores.get(z), patrones.get(z)), marco, matrices, vertexConsumers, luz);
        }
        // Los colgantes: apliques apoyados en la banda (el punto es de una de sus cajas, en el marco de la parte).
        ApliqueRenderer.dibujar(banda, 0f, biped, matrices, vertexConsumers, luz);
    }

    /** Las cajas de cada zona con la holgura por defecto — para apuntar el mouse en la Mesa de estilado. */
    public static List<SombreroRenderer.Caja> cajas(ItemStack banda) {
        return cajas(banda, BandaItem.zona(banda) == BandaZona.CINTURA ? DIL_MESA : 0.3f);
    }

    /** Holgura del cinto sobre la piel sola (en la Mesa de estilado y el Maniquí). */
    public static final float DIL_MESA = 0.35f;

    private static final Map<String, ModelPart> CACHE = new HashMap<>();

    private static ModelPart parte(List<SombreroRenderer.Caja> cajas, int zona, BandaZona z, ItemStack banda, float dil) {
        String key = z + "|" + BandaItem.ancho(banda) + "|" + BandaItem.herraje(banda) + "|" + Math.round(dil * 20) + "|" + zona;
        ModelPart c = CACHE.get(key);
        if (c != null) return c;
        List<ModelPart.Cuboid> cubos = new ArrayList<>();
        for (SombreroRenderer.Caja caja : cajas) {
            if (caja.zona() != zona) continue;
            float[] a = caja.min(), b = caja.max();
            cubos.add(SombreroRenderer.caja(a[0], a[1], a[2], b[0] - a[0], b[1] - a[1], b[2] - a[2]));
        }
        if (cubos.isEmpty()) return null;
        c = new ModelPart(cubos, Map.of());
        CACHE.put(key, c);
        return c;
    }

    private static List<SombreroRenderer.Caja> cajas(ItemStack banda, float dil) {
        BandaZona zona = BandaItem.zona(banda);
        BandaAncho ancho = BandaItem.ancho(banda);
        BandaHerraje herraje = BandaItem.herraje(banda);
        boolean cintura = zona == BandaZona.CINTURA;
        float y0 = cintura ? Y_CINTURA : Y_CUELLO;
        float h = ancho.alto(zona);
        float hx = (cintura ? 4f : 3.1f) + dil, hz = (cintura ? 2f : 2.7f) + dil, t = GROSOR;
        List<SombreroRenderer.Caja> out = new ArrayList<>();
        // Banda: frente, espalda y los dos costados.
        losas(out, 0, hx, hz, t, y0, y0 + h, 0f);
        // Borde: tiras de arriba y abajo, un poquito más afuera y más altas que la banda para no pelear el plano.
        losas(out, 1, hx, hz, t, y0 - 0.1f, y0 + BORDE_ALTO, BORDE_SALE);
        losas(out, 1, hx, hz, t, y0 + h - BORDE_ALTO, y0 + h + 0.1f, BORDE_SALE);
        // Herraje al frente, apoyado en la cara de afuera.
        float escala = cintura ? 1f : 0.6f;
        float zf0 = -hz - t - BORDE_SALE - 0.5f * escala, zf1 = -hz - t - BORDE_SALE;
        float ya = y0 - 0.25f, yb = y0 + h + 0.25f;
        float mx = 1.9f * escala;
        switch (herraje) {
            case PLACA -> out.add(caja(2, -mx, ya, zf0, mx, yb, zf1));
            case ARO -> {
                float ancha = 2.3f * escala, barra = 0.6f * escala;
                out.add(caja(2, -ancha, ya, zf0, ancha, ya + barra, zf1));
                out.add(caja(2, -ancha, yb - barra, zf0, ancha, yb, zf1));
                out.add(caja(2, -ancha, ya, zf0, -ancha + barra, yb, zf1));
                out.add(caja(2, ancha - barra, ya, zf0, ancha, yb, zf1));
            }
            default -> { }
        }
        return out;
    }

    private static SombreroRenderer.Caja caja(int zona, float x0, float y0, float z0, float x1, float y1, float z1) {
        return new SombreroRenderer.Caja(zona, new float[] {x0, y0, z0}, new float[] {x1, y1, z1});
    }

    /** Las cuatro losas del aro, {@code sale} px más afuera. */
    private static void losas(List<SombreroRenderer.Caja> out, int zona, float hx, float hz, float t,
                              float ya, float yb, float sale) {
        float ox = hx + t + sale, oz = hz + t + sale;
        out.add(caja(zona, -ox, ya, -oz, ox, yb, -hz));                    // frente
        out.add(caja(zona, -ox, ya, hz, ox, yb, oz));                      // espalda
        out.add(caja(zona, -ox, ya, -hz, -hx, yb, hz));                    // costado izquierdo
        out.add(caja(zona, hx, ya, -hz, ox, yb, hz));                      // costado derecho
    }
}
