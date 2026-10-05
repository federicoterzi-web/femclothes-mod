package com.femclothes.render;

import com.femclothes.correa.Correa;
import com.femclothes.correa.EstiloCorrea;
import com.femclothes.correa.ModoCorrea;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.SombreroPatron;
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
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Correas libres (2026-10-05, "correas libres... rectas u oblicuas, pegadas o colgantes" + "molde de cadenas"): cada
 * {@link Correa} es un camino de nodos sobre o cerca de la caja de su parte del cuerpo (en el espacio local de la
 * parte, px) que se dibuja como cinta (lisa, ojalillos), cadena de eslabones, bolitas o cordón.
 *
 * <ul>
 *   <li>PEGADA: la recta entre los dos puntos se proyecta sobre la superficie de la caja (inflada lo que infla la
 *       prenda), así da la vuelta por las aristas; entre caras opuestas pasa por una tercera.</li>
 *   <li>COLGANTE: solo el 1.º punto la sujeta; el resto cuelga en la dirección del 2.º con tela blanda (la inercia de
 *       {@link FisicaApliques}), sin entrar en la caja.</li>
 * </ul>
 */
public final class CorreaRenderer {

    private CorreaRenderer() {}

    private record Nodo(Vector3f pos, Vector3f normal) {}

    /** Dibuja las correas de {@code item}; {@code marco} es la normal del cuerpo antes de la pose de cada parte. */
    public static void dibujar(ItemStack item, float dil, BipedEntityModel<?> biped, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int luz, Matrix3f marco) {
        List<Correa> lista = item.get(FemclothesComponents.CORREAS);
        if (lista == null || lista.isEmpty()) return;
        for (Correa c : lista) una(c, dil, biped, matrices, vertexConsumers, luz, marco);
    }

    private static void una(Correa c, float dil, BipedEntityModel<?> biped, MatrixStack matrices,
                            VertexConsumerProvider vcp, int luz, Matrix3f marco) {
        ModelPart parte = CuerpoGeometria.delJugador(biped, c.parte());
        if (!parte.visible || parte.cuboids.isEmpty()) return;
        ModelPart.Cuboid cu = parte.cuboids.get(0);
        float[] mn = {cu.minX, cu.minY, cu.minZ}, mx = {cu.maxX, cu.maxY, cu.maxZ};
        float w = c.ancho();
        float espesor = switch (c.estilo()) {
            case LISA, OJALILLOS -> 0.3f;
            case CORDON -> Math.max(0.5f, w * 0.45f);
            case CADENA -> w * 0.9f;
            case CADENA_FINA -> Math.max(1f, w * 0.75f);
        };
        float e = dil + 0.05f + espesor / 2f;
        for (int i = 0; i < 3; i++) {
            mn[i] -= e;
            mx[i] += e;
        }
        Vector3f a = punto(c.desde(), e), b = punto(c.hasta(), e);
        float paso = switch (c.estilo()) {
            case LISA, OJALILLOS -> Math.max(2f, 2f * w);
            case CADENA -> w * 1.5f;
            case CADENA_FINA -> Math.max(1f, w * 0.75f) * 1.05f;
            case CORDON -> 2f;
        };
        matrices.push();
        parte.rotate(matrices);
        List<Nodo> camino;
        if (c.modo() == ModoCorrea.PEGADA) {
            camino = rutaPegada(a, c.desde().cara(), b, c.hasta().cara(), mn, mx);
        } else {
            camino = rutaColgante(a, b, mn, mx, c.blandura(), new Matrix3f(matrices.peek().getNormalMatrix()), marco);
        }
        camino = remuestrear(camino, paso);
        if (camino.size() >= 2) emitir(c, camino, w, espesor, matrices.peek(), vcp, luz);
        matrices.pop();
    }

    private static Vector3f punto(Correa.Punto p, float e) {
        Direction d = p.cara();
        return new Vector3f(p.x() + d.getOffsetX() * e, p.y() + d.getOffsetY() * e, p.z() + d.getOffsetZ() * e);
    }

    // ── caminos ──────────────────────────────────────────────────────────

    private static List<Nodo> rutaPegada(Vector3f a, Direction ca, Vector3f b, Direction cb, float[] mn, float[] mx) {
        List<Nodo> out = new ArrayList<>();
        if (ca.getOpposite() == cb) {
            // Caras opuestas: por la tercera cara más corta (la recta pasaría por el medio de la caja).
            Vector3f medio = new Vector3f(a).add(b).mul(0.5f);
            Vector3f mejorQ = null;
            float mejorLargo = Float.MAX_VALUE;
            for (Direction d : Direction.values()) {
                if (d.getAxis() == ca.getAxis()) continue;
                Vector3f q = new Vector3f(medio);
                int eje = d.getAxis().ordinal();
                q.setComponent(eje, d.getDirection() == Direction.AxisDirection.POSITIVE ? mx[eje] : mn[eje]);
                for (int i = 0; i < 3; i++) q.setComponent(i, Math.max(mn[i], Math.min(mx[i], q.get(i))));
                float largo = a.distance(q) + q.distance(b);
                if (largo < mejorLargo) {
                    mejorLargo = largo;
                    mejorQ = q;
                }
            }
            tramo(out, a, mejorQ, mn, mx);
            tramo(out, mejorQ, b, mn, mx);
        } else {
            tramo(out, a, b, mn, mx);
        }
        return out;
    }

    /** La recta de {@code a} a {@code b} muestreada y proyectada sobre la superficie de la caja. */
    private static void tramo(List<Nodo> out, Vector3f a, Vector3f b, float[] mn, float[] mx) {
        final int n = 16;
        for (int k = 0; k <= n; k++) {
            if (k == 0 && !out.isEmpty()) continue;
            Vector3f p = new Vector3f(a).lerp(b, k / (float) n);
            out.add(proyectar(p, mn, mx, true));
        }
    }

    private static List<Nodo> rutaColgante(Vector3f a, Vector3f b, float[] mn, float[] mx, float blandura,
                                           Matrix3f normalLocal, Matrix3f marco) {
        Vector3f desp = new Vector3f();
        FisicaApliques.Desplazamiento d = FisicaApliques.actual;
        if (d != null && blandura > 0f) {
            // Del marco del cuerpo al de esta parte (las dos son rotaciones: la inversa es la transpuesta).
            Matrix3f aLocal = new Matrix3f(normalLocal).transpose().mul(marco);
            desp = aLocal.transform(new Vector3f(d.blando())).mul(8f * blandura);
            if (desp.length() > 5f) desp.normalize(5f);
        }
        final int k = 12;
        List<Nodo> out = new ArrayList<>();
        for (int i = 0; i <= k; i++) {
            float s = i / (float) k;
            Vector3f p = new Vector3f(a).lerp(b, s).add(new Vector3f(desp).mul(s * s));
            out.add(proyectar(p, mn, mx, false));
        }
        return out;
    }

    /**
     * El punto {@code p} sobre la superficie de la caja: con {@code pegar}, también si está afuera; sin él, solo si
     * está adentro (lo de afuera queda libre y su normal apunta lejos de la caja).
     */
    private static Nodo proyectar(Vector3f p, float[] mn, float[] mx, boolean pegar) {
        final float eps = 1e-4f;
        boolean dentro = true;
        for (int i = 0; i < 3; i++) if (p.get(i) <= mn[i] + eps || p.get(i) >= mx[i] - eps) dentro = false;
        Vector3f q = new Vector3f(p), nrm = new Vector3f();
        if (dentro) {
            float mejor = Float.MAX_VALUE;
            int eje = 0;
            float lado = 0f;
            for (int i = 0; i < 3; i++) {
                float dMin = p.get(i) - mn[i], dMax = mx[i] - p.get(i);
                if (dMin < mejor) { mejor = dMin; eje = i; lado = -1f; }
                if (dMax < mejor) { mejor = dMax; eje = i; lado = 1f; }
            }
            q.setComponent(eje, lado < 0 ? mn[eje] : mx[eje]);
            nrm.setComponent(eje, lado);
        } else {
            Vector3f c = new Vector3f();
            for (int i = 0; i < 3; i++) c.setComponent(i, Math.max(mn[i], Math.min(mx[i], p.get(i))));
            for (int i = 0; i < 3; i++) {
                if (c.get(i) <= mn[i] + eps) nrm.setComponent(i, nrm.get(i) - 1f);
                if (c.get(i) >= mx[i] - eps) nrm.setComponent(i, nrm.get(i) + 1f);
            }
            if (pegar) {
                q = c;
            } else {
                Vector3f lejos = new Vector3f(p).sub(c);
                if (lejos.lengthSquared() > 1e-8f) nrm = lejos;
            }
        }
        if (nrm.lengthSquared() < 1e-8f) nrm.set(0, -1, 0);
        return new Nodo(q, nrm.normalize());
    }

    /** Nodos a distancias parejas ({@code paso} aproximado) a lo largo del camino. */
    private static List<Nodo> remuestrear(List<Nodo> in, float paso) {
        List<Nodo> out = new ArrayList<>();
        if (in.size() < 2) return out;
        float[] acum = new float[in.size()];
        for (int i = 1; i < in.size(); i++) acum[i] = acum[i - 1] + in.get(i).pos().distance(in.get(i - 1).pos());
        float total = acum[in.size() - 1];
        if (total < 1e-4f) return out;
        int n = Math.max(1, Math.round(total / paso));
        int seg = 1;
        for (int k = 0; k <= n; k++) {
            float dist = total * k / n;
            while (seg < in.size() - 1 && acum[seg] < dist) seg++;
            float t0 = acum[seg - 1], t1 = acum[seg];
            float t = t1 - t0 < 1e-6f ? 0f : (dist - t0) / (t1 - t0);
            Nodo n0 = in.get(seg - 1), n1 = in.get(seg);
            Vector3f pos = new Vector3f(n0.pos()).lerp(n1.pos(), t);
            Vector3f nrm = new Vector3f(n0.normal()).lerp(n1.normal(), t);
            if (nrm.lengthSquared() < 1e-8f) nrm.set(n0.normal());
            out.add(new Nodo(pos, nrm.normalize()));
        }
        return out;
    }

    // ── dibujo ───────────────────────────────────────────────────────────

    private static void emitir(Correa c, List<Nodo> nodos, float w, float espesor, MatrixStack.Entry e,
                               VertexConsumerProvider vcp, int luz) {
        int banda = c.colores().get(0), borde = c.colores().get(1), herraje = c.colores().get(2);
        int n = nodos.size();
        Vector3f[] t = new Vector3f[n], nn = new Vector3f[n], bb = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f d = new Vector3f(nodos.get(Math.min(n - 1, i + 1)).pos()).sub(nodos.get(Math.max(0, i - 1)).pos());
            if (d.lengthSquared() < 1e-8f) d.set(1, 0, 0);
            t[i] = d.normalize();
            Vector3f nrm = new Vector3f(nodos.get(i).normal());
            nrm.sub(new Vector3f(t[i]).mul(nrm.dot(t[i])));
            if (nrm.lengthSquared() < 1e-6f) {
                // La tira va a lo largo de la normal: cualquier perpendicular sirve.
                nrm = Math.abs(t[i].y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
                nrm.sub(new Vector3f(t[i]).mul(nrm.dot(t[i])));
            }
            nn[i] = nrm.normalize();
            bb[i] = new Vector3f(t[i]).cross(nn[i]).normalize();
        }
        switch (c.estilo()) {
            case LISA, OJALILLOS -> {
                VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(texturaCorrea(c.estilo(), banda, borde)));
                cinta(vc, e, luz, nodos, bb, nn, w / 2f, 0f, 1f);
            }
            case CORDON -> {
                float wc = Math.max(0.8f, w * 0.55f);
                VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SombreroRenderer.textura(banda, SombreroPatron.LISO)));
                cinta(vc, e, luz, nodos, bb, nn, wc / 2f, 0.5f, 0.5f);
                cinta(vc, e, luz, nodos, nn, bb, wc / 2f, 0.5f, 0.5f);
                // Las puntas (aglets): una caja de herraje en cada extremo.
                VertexConsumer vm = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SombreroRenderer.textura(herraje, SombreroPatron.LISO)));
                for (int i : new int[] {0, n - 1}) {
                    if (i == 0 && c.modo() == ModoCorrea.COLGANTE) continue;     // el de arriba está sujeto
                    Vector3f dir = new Vector3f(t[i]).mul(i == 0 ? -1f : 1f);
                    Vector3f centro = new Vector3f(nodos.get(i).pos()).add(new Vector3f(dir).mul(0.7f));
                    caja(vm, e, luz, centro, t[i], bb[i], nn[i], 0.9f, wc * 0.65f, wc * 0.65f);
                }
            }
            case CADENA -> {
                VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SombreroRenderer.textura(herraje, SombreroPatron.LISO)));
                for (int i = 0; i < n; i++) {
                    boolean par = i % 2 == 0;
                    // Eslabones alternados 90°: uno de plano, el otro de canto.
                    caja(vc, e, luz, nodos.get(i).pos(), t[i], bb[i], nn[i], w * 0.85f,
                            par ? w * 0.5f : w * 0.22f, par ? w * 0.22f : w * 0.5f);
                }
            }
            case CADENA_FINA -> {
                VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(SombreroRenderer.textura(herraje, SombreroPatron.LISO)));
                float lado = Math.max(1f, w * 0.75f) / 2f;
                for (int i = 0; i < n; i++) {
                    caja(vc, e, luz, nodos.get(i).pos(), t[i], bb[i], nn[i], lado, lado, lado);
                }
            }
        }
    }

    /** Una cinta de ancho 2·{@code medio} siguiendo los nodos; {@code lados} es el vector de costado de cada nodo. */
    private static void cinta(VertexConsumer vc, MatrixStack.Entry e, int luz, List<Nodo> nodos, Vector3f[] lados,
                              Vector3f[] normales, float medio, float u0, float u1) {
        for (int i = 0; i + 1 < nodos.size(); i++) {
            Vector3f a = nodos.get(i).pos(), b = nodos.get(i + 1).pos();
            float ua = u0 == u1 ? u0 : 0f, ub = u0 == u1 ? u1 : 1f;
            vertice(vc, e, luz, new Vector3f(a).add(new Vector3f(lados[i]).mul(medio)), normales[i], ua, 0f, u0 == u1);
            vertice(vc, e, luz, new Vector3f(a).sub(new Vector3f(lados[i]).mul(medio)), normales[i], ub, 0f, u0 == u1);
            vertice(vc, e, luz, new Vector3f(b).sub(new Vector3f(lados[i + 1]).mul(medio)), normales[i + 1], ub, 1f, u0 == u1);
            vertice(vc, e, luz, new Vector3f(b).add(new Vector3f(lados[i + 1]).mul(medio)), normales[i + 1], ua, 1f, u0 == u1);
        }
    }

    private static void vertice(VertexConsumer vc, MatrixStack.Entry e, int luz, Vector3f p, Vector3f nrm,
                                float u, float v, boolean plano) {
        // Textura lisa: todos los vértices leen el mismo texel (el centro del lienzo).
        vc.vertex(e.getPositionMatrix(), p.x / 16f, p.y / 16f, p.z / 16f)
                .color(0xFFFFFFFF)
                .texture(plano ? 0.5f : u, plano ? 0.5f : v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(luz)
                .normal(e, nrm.x, nrm.y, nrm.z);
    }

    /** Una caja orientada (ejes unitarios {@code ex}, {@code ey}, {@code ez}; medidas medias {@code hx}, {@code hy}, {@code hz}) en px. */
    private static void caja(VertexConsumer vc, MatrixStack.Entry e, int luz, Vector3f centro, Vector3f ex, Vector3f ey,
                             Vector3f ez, float hx, float hy, float hz) {
        Vector3f[] ejes = {ex, ey, ez};
        float[] h = {hx, hy, hz};
        for (int ax = 0; ax < 3; ax++) {
            int u = (ax + 1) % 3, v = (ax + 2) % 3;
            for (int s = -1; s <= 1; s += 2) {
                Vector3f cara = new Vector3f(centro).add(new Vector3f(ejes[ax]).mul(s * h[ax]));
                Vector3f nrm = new Vector3f(ejes[ax]).mul(s);
                Vector3f du = new Vector3f(ejes[u]).mul(h[u]), dv = new Vector3f(ejes[v]).mul(h[v]);
                // Sentido antihorario visto desde afuera (el layer no descarta caras, pero la luz lo agradece).
                int[][] esq = s > 0 ? new int[][] {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}} : new int[][] {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
                for (int[] q : esq) {
                    Vector3f p = new Vector3f(cara).add(new Vector3f(du).mul(q[0])).add(new Vector3f(dv).mul(q[1]));
                    vertice(vc, e, luz, p, nrm, 0.5f, 0.5f, true);
                }
            }
        }
    }

    // ── texturas de las cintas ───────────────────────────────────────────

    private static final Map<String, Identifier> TEXTURAS = new HashMap<>();

    /** 16x16: la banda al centro, el borde a los lados, puntadas claras y (ojalillos) dos agujeros. */
    private static Identifier texturaCorrea(EstiloCorrea estilo, int banda, int borde) {
        String clave = estilo.clave + "_" + Integer.toHexString(banda) + "_" + Integer.toHexString(borde);
        Identifier id = TEXTURAS.get(clave);
        if (id != null) return id;
        NativeImage img = new NativeImage(16, 16, true);
        int cb = abgr(banda), ce = abgr(borde), cp = abgr(SombreroPatron.tonoDeContraste(banda)), co = abgr(0x201408);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int col = (x < 2 || x > 13) ? ce : cb;
                if ((x == 3 || x == 12) && y % 4 < 2) col = cp;
                if (estilo == EstiloCorrea.OJALILLOS && x >= 7 && x <= 8 && ((y >= 3 && y <= 4) || (y >= 11 && y <= 12))) col = co;
                img.setColor(x, y, col);
            }
        }
        id = Identifier.of("femclothes", "dynamic/correa_" + clave);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
        TEXTURAS.put(clave, id);
        return id;
    }

    private static int abgr(int rgb) {
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }
}
