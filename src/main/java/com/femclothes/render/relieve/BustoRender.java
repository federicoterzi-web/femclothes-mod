package com.femclothes.render.relieve;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * El busto como geometría propia (2026-10-01, "cambiaria los pechos buscando
 * mas la estrategia del mod only jugs"): volumen real que sale del pecho, con
 * la textura de esa zona (la piel, o la prenda de encima con su patrón y sus
 * fotos). Reemplaza al busto del relieve, que se superponía con lo pintado en
 * las máscaras.
 *
 * <p>Forma (2026-10-02, "me gustan mas los pechos con volumen real pero me
 * gustaria que fueran mas redondeados"): dos cúpulas — media elipse de frente,
 * más llena abajo que arriba — que nacen pegadas al pecho. La caída, la
 * apertura hacia los costados y el rebote ({@link FisicaBusto}) no giran la
 * cúpula: la deforman según lo que sobresale cada punto, así el borde queda
 * siempre apoyado en el pecho, sin rendijas.
 *
 * <p>Quién la dibuja:
 * <ul>
 *   <li>{@code GarmentFeatureRenderer}: la piel y cada pieza del torso que
 *       tape el pecho, inflada lo que esa tela está por fuera de la de abajo
 *       (las telas holgadas caen más abajo, como una carpa);</li>
 *   <li>{@code mixin/BustoEnModelosMixin} (2026-10-02, "armar un algo que
 *       adapte las prendas y armaduras de cualquier mod (quizas con armaduras
 *       pueden mantenerse rigidas"): cualquier modelo humanoide que se dibuje
 *       sobre el jugador (armaduras, cosméticos, ropa de otros mods), con las
 *       UV de su propio torso ({@link Uv#deCaraDelTorso}); las armaduras,
 *       rígidas.</li>
 * </ul>
 * Los apliques del pecho se apoyan en la cúpula ({@link #sobreBusto}) y la
 * Mesa de estilado los apunta con {@link #rayo}.
 *
 * <p>Coordenadas: las del torso del modelo (px, Y hacia abajo, el frente en
 * −Z), después de {@code ModelPart.rotate} del torso.
 */
public final class BustoRender {

    private BustoRender() {}

    /** El busto del jugador que se está dibujando. */
    public record Busto(float talle, float sujecion, @Nullable RelieveRender.Rebote rebote) {}

    /** El de la ropa del mod (lo pone {@code GarmentFeatureRenderer} mientras dibuja). */
    @Nullable
    public static Busto actual;

    /**
     * Fila del torso donde arranca el busto (px desde los hombros): bien
     * arriba, para que baje en pendiente hasta la punta (2026-10-02, "integrar
     * el inicio del pecho mas arriba para que caiga hasta la punta mejor").
     */
    private static final float ARRIBA = 0.8f;
    /** Anillos (del centro al borde) y gajos alrededor de cada cúpula. */
    private static final int ANILLOS = 7, GAJOS = 18;
    /**
     * Exponente del perfil: 2 = media elipse; más, más lleno hasta el borde.
     * Abajo es redondo ({@link #PERFIL_ABAJO}); hacia arriba baja a
     * {@link #PERFIL_ARRIBA}, una pendiente suave que nace del pecho.
     */
    private static final float PERFIL_ABAJO = 2.4f, PERFIL_ARRIBA = 1.45f;

    /** ¿Una pieza que cubre las filas [desde, hasta) del torso tapa el busto? */
    public static boolean cubre(int desde, int hasta) {
        return desde <= 2 && hasta >= 5;
    }

    /** De dónde sale la textura: un punto (x, y) del frente del torso → UV normalizadas. */
    public interface Uv {
        float u(float x);
        float v(float y);

        /** El layout de la skin (piel y telas del mod): frente del torso en u 20..28, v desde {@code fila}. */
        static Uv skin(float fila) {
            return new Uv() {
                public float u(float x) { return Math.max(20f, Math.min(28f, 24f + x)) / 64f; }
                public float v(float y) { return Math.max(fila, Math.min(fila + 12f, fila + y)) / 64f; }
            };
        }

        /**
         * Las UV de la cara del frente del torso de cualquier modelo (otro mod,
         * otra textura, otro tamaño): lineales entre los bordes de esa cara.
         * {@code x0..x1}/{@code y0..y1} son los bordes de la cara en px y
         * {@code u0..u1}/{@code v0..v1} sus UV en esos bordes (pueden ir al revés).
         */
        static Uv deCaraDelTorso(float x0, float x1, float u0, float u1, float y0, float y1, float v0, float v1) {
            return new Uv() {
                public float u(float x) {
                    float k = Math.max(0f, Math.min(1f, (x - x0) / (x1 - x0)));
                    return u0 + (u1 - u0) * k;
                }
                public float v(float y) {
                    float k = Math.max(0f, Math.min(1f, (y - y0) / (y1 - y0)));
                    return v0 + (v1 - v0) * k;
                }
            };
        }
    }

    /** La forma de una cúpula ya calculada para este talle, inflado y rebote. */
    private record Forma(float cx, float cy, float rx, float rArriba, float rAbajo, float hondo,
                         float caida, float apertura, float dx, float dy, float plano, int lado) {

        /** Punto de la cúpula en el anillo {@code r} (0 centro .. 1 borde) y el ángulo {@code a}. */
        Vector3f punto(float r, double a) {
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float x = cx + rx * r * c;
            float y = cy + (s > 0 ? rAbajo : rArriba) * r * s;
            float sale = sale(r, a);
            float k = hondo <= 1e-4f ? 0f : sale / hondo;          // 0 en el borde, 1 en la punta
            // Lo que sobresale cae, se abre hacia afuera y rebota; la base no se mueve.
            y += caida * sale + dy * k;
            x += lado * apertura * sale + dx * k;
            return new Vector3f(x, y, plano - sale);
        }

        /** Cuánto sobresale en el anillo {@code r} hacia el ángulo {@code a}: 0 en el borde. */
        float sale(float r, double a) {
            // Arriba (sen < 0, Y hacia abajo) en pendiente; abajo redondo.
            float arriba = (float) Math.max(0.0, -Math.sin(a));
            float p = PERFIL_ABAJO + (PERFIL_ARRIBA - PERFIL_ABAJO) * arriba;
            return hondo * (float) Math.pow(Math.max(0.0, 1.0 - Math.pow(Math.min(1f, r), p)), 1.0 / p);
        }

        /** El punto de la base (sin caída ni rebote) del anillo {@code r} y el ángulo {@code a}. */
        float baseX(float r, double a) { return cx + rx * r * (float) Math.cos(a); }
        float baseY(float r, double a) {
            float s = (float) Math.sin(a);
            return cy + (s > 0 ? rAbajo : rArriba) * r * s;
        }

        /** (r, ángulo) del punto de la base (x, y); r > 1 = afuera de la cúpula. */
        float[] polar(float x, float y) {
            float u = (x - cx) / rx;
            float dyb = y - cy;
            float v = dyb / (dyb > 0 ? rAbajo : rArriba);
            return new float[]{(float) Math.sqrt(u * u + v * v), (float) Math.atan2(v, u)};
        }

        Vector3f normal(float r, double a) {
            float er = 0.02f;
            float rr = Math.max(r, 0.02f);
            Vector3f dr = punto(Math.min(1f, r + er), a).sub(punto(Math.max(0f, r - er), a));
            Vector3f da = punto(rr, a + 0.05).sub(punto(rr, a - 0.05));
            Vector3f nn = dr.cross(da);
            if (nn.z > 0) nn.negate();
            if (nn.lengthSquared() < 1e-10f) nn.set(0, 0, -1);
            return nn.normalize();
        }
    }

    /** Las dos cúpulas (derecha, izquierda) de este busto con este inflado. */
    private static Forma[] formas(Busto b, float inflado, float carpa, boolean rigido) {
        float t = b.talle() * b.sujecion();
        if (t <= 0.05f) return new Forma[0];
        float dy = 0f, dx = 0f;
        if (b.rebote() != null && !rigido) {
            dy = b.rebote().dy() * 12f;
            dx = b.rebote().dx() * 8f;
        }
        float hondo = (0.55f + 0.5f * t) + inflado;
        float rx = Math.min(2.6f, 1.95f + 0.07f * t) + inflado;
        float rArriba = 2.3f + 0.16f * t + inflado;
        float rAbajo = 1.3f + 0.22f * t + inflado + carpa;
        float caida = (0.06f + 0.025f * t) * (0.6f + 0.4f * b.sujecion());
        float apertura = 0.04f + 0.012f * t;
        if (rigido) {
            // Armadura: un peto moldeado, firme (2026-10-02, "con armaduras pueden mantenerse rigidas").
            caida *= 0.25f;
            apertura *= 0.5f;
        }
        Forma[] f = new Forma[2];
        for (int i = 0; i < 2; i++) {
            int lado = i == 0 ? -1 : 1;
            f[i] = new Forma(lado * 2f, ARRIBA + 2.3f + 0.16f * t, rx, rArriba, rAbajo, hondo,
                    caida, apertura, dx, dy * 0.6f, -2f - inflado, lado);
        }
        return f;
    }

    /**
     * La piel y la ropa del mod (layout de la skin).
     *
     * @param inflado  cuánto sale la tela por fuera del cuerpo en el pecho (px; 0 = la piel)
     * @param carpa    cuánto más abajo cae la tela holgada debajo del busto (px)
     * @param filaSkin fila de la skin donde empieza el frente del torso: 20 (la
     *                 piel y todas las telas) o 36 (la segunda capa de la skin)
     */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov,
                               float inflado, float carpa, float filaSkin) {
        dibujar(b, matrices, vc, luz, ov, 0xFFFFFFFF, Uv.skin(filaSkin), inflado, carpa, false);
    }

    /** Con cualquier mapeo de textura, color y rigidez (modelos de otros mods y armaduras). */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov, int color,
                               Uv uv, float inflado, float carpa, boolean rigido) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f n = matrices.peek().getNormalMatrix();
        for (Forma f : formas(b, inflado, carpa, rigido)) cupula(f, m, n, vc, luz, ov, color, uv);
    }

    private static void cupula(Forma f, Matrix4f m, Matrix3f n, VertexConsumer vc, int luz, int ov, int color, Uv uv) {
        Vector3f[][] p = new Vector3f[ANILLOS + 1][GAJOS + 1];
        Vector3f[][] nor = new Vector3f[ANILLOS + 1][GAJOS + 1];
        float[][] uu = new float[ANILLOS + 1][GAJOS + 1], vv = new float[ANILLOS + 1][GAJOS + 1];
        for (int i = 0; i <= ANILLOS; i++) {
            float r = i / (float) ANILLOS;
            for (int j = 0; j <= GAJOS; j++) {
                double a = Math.PI * 2 * j / GAJOS;
                p[i][j] = f.punto(r, a);
                nor[i][j] = f.normal(r, a);
                // UV de la base (sin caída ni rebote): la piel no se corre al moverse.
                uu[i][j] = uv.u(f.baseX(r, a));
                vv[i][j] = uv.v(f.baseY(r, a));
            }
        }
        for (int i = 0; i < ANILLOS; i++) {
            for (int j = 0; j < GAJOS; j++) {
                vertice(m, n, vc, luz, ov, color, p[i][j], uu[i][j], vv[i][j], nor[i][j]);
                vertice(m, n, vc, luz, ov, color, p[i + 1][j], uu[i + 1][j], vv[i + 1][j], nor[i + 1][j]);
                vertice(m, n, vc, luz, ov, color, p[i + 1][j + 1], uu[i + 1][j + 1], vv[i + 1][j + 1], nor[i + 1][j + 1]);
                vertice(m, n, vc, luz, ov, color, p[i][j + 1], uu[i][j + 1], vv[i][j + 1], nor[i][j + 1]);
            }
        }
    }

    private static void vertice(Matrix4f m, Matrix3f n, VertexConsumer vc, int luz, int ov, int color,
                                Vector3f p, float u, float v, Vector3f normal) {
        Vector3f w = m.transformPosition(p.x / 16f, p.y / 16f, p.z / 16f, new Vector3f());
        Vector3f nn = n.transform(new Vector3f(normal)).normalize();
        vc.vertex(w.x, w.y, w.z, color, u, v, ov, luz, nn.x, nn.y, nn.z);
    }

    // ── apliques ─────────────────────────────────────────────────────────

    /** Un punto de la superficie del busto y su normal (px, coordenadas del torso). */
    public record Punto(Vector3f pos, Vector3f normal) {}

    /**
     * Dónde queda, sobre el busto, el punto (x, y) del frente plano del torso
     * (2026-10-02, "hay que contemplar los pechos para los apliques"): el
     * aplique se guarda en el frente plano y acá se apoya en la cúpula (y
     * rebota con ella). Null si el punto no cae en ninguna cúpula — o si no
     * hay busto: la prenda sigue sirviendo igual sin él.
     */
    @Nullable
    public static Punto sobreBusto(Busto b, float x, float y, float inflado) {
        Punto mejor = null;
        float sale = -1f;
        for (Forma f : formas(b, inflado, 0f, false)) {
            float[] pol = f.polar(x, y);
            if (pol[0] > 1f) continue;
            float s = f.sale(pol[0], pol[1]);
            if (s > sale) {
                sale = s;
                mejor = new Punto(f.punto(pol[0], pol[1]), f.normal(pol[0], pol[1]));
            }
        }
        return mejor;
    }

    /**
     * Un rayo contra las cúpulas, en px del torso (2026-10-02, "se puede las
     * dos? cosa que si armo la prenda sin pechos despues siga sirviendo?"):
     * devuelve {t, x, y} del choque más cercano al origen (t en el largo de
     * {@code dir}), con (x, y) el punto del frente PLANO que está debajo — eso
     * es lo que se guarda en el aplique. Null si no le pega.
     */
    @Nullable
    public static float[] rayo(Busto b, Vector3f origen, Vector3f dir, float inflado) {
        float[] mejor = null;
        for (Forma f : formas(b, inflado, 0f, false)) {
            for (int i = 0; i < ANILLOS; i++) {
                for (int j = 0; j < GAJOS; j++) {
                    float r0 = i / (float) ANILLOS, r1 = (i + 1) / (float) ANILLOS;
                    double a0 = Math.PI * 2 * j / GAJOS, a1 = Math.PI * 2 * (j + 1) / GAJOS;
                    Vector3f p00 = f.punto(r0, a0), p10 = f.punto(r1, a0), p11 = f.punto(r1, a1), p01 = f.punto(r0, a1);
                    float[] h = triangulo(origen, dir, p00, p10, p11);
                    float[] k = h;
                    float ra = r0, rb = r1, rc = r1;
                    double aa = a0, ab = a0, ac = a1;
                    if (h == null) {
                        k = triangulo(origen, dir, p00, p11, p01);
                        rb = r1; rc = r0; ab = a1; ac = a1;
                    }
                    if (k == null || (mejor != null && k[0] >= mejor[0])) continue;
                    // Baricéntricas → punto de la base debajo del choque.
                    float w1 = k[1], w2 = k[2], w0 = 1f - w1 - w2;
                    float bx = w0 * f.baseX(ra, aa) + w1 * f.baseX(rb, ab) + w2 * f.baseX(rc, ac);
                    float by = w0 * f.baseY(ra, aa) + w1 * f.baseY(rb, ab) + w2 * f.baseY(rc, ac);
                    mejor = new float[]{k[0], bx, by};
                }
            }
        }
        return mejor;
    }

    /** Möller–Trumbore: {t, u, v} o null. */
    @Nullable
    private static float[] triangulo(Vector3f o, Vector3f d, Vector3f a, Vector3f b, Vector3f c) {
        Vector3f e1 = new Vector3f(b).sub(a), e2 = new Vector3f(c).sub(a);
        Vector3f p = new Vector3f(d).cross(e2);
        float det = e1.dot(p);
        if (Math.abs(det) < 1e-9f) return null;
        float inv = 1f / det;
        Vector3f s = new Vector3f(o).sub(a);
        float u = s.dot(p) * inv;
        if (u < 0f || u > 1f) return null;
        Vector3f q = new Vector3f(s).cross(e1);
        float v = d.dot(q) * inv;
        if (v < 0f || u + v > 1f) return null;
        float t = e2.dot(q) * inv;
        return t < 0f ? null : new float[]{t, u, v};
    }
}
