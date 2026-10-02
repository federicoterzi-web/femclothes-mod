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
 * gustaria que fueran mas redondeados"): en vez de dos cajas, dos cúpulas —
 * media elipse de frente, más llena abajo que arriba — que nacen pegadas al
 * pecho. La caída, la apertura hacia los costados y el rebote ({@link
 * FisicaBusto}) no giran la cúpula: la deforman según lo que sobresale cada
 * punto, así el borde queda siempre apoyado en el pecho, sin rendijas.
 *
 * <p>Se dibuja una vez para el cuerpo y otra por cada pieza del torso que
 * tape el pecho, inflada lo que esa tela está por fuera de la de abajo: así
 * la ropa lo envuelve. Las telas holgadas caen más abajo del busto (como una
 * carpa).
 *
 * <p>Coordenadas: las del torso del modelo (px, Y hacia abajo, el frente en
 * −Z), después de {@code ModelPart.rotate} del torso.
 */
public final class BustoRender {

    private BustoRender() {}

    /** El busto del jugador que se está dibujando (lo pone {@code GarmentFeatureRenderer}). */
    public record Busto(float talle, float sujecion, @Nullable RelieveRender.Rebote rebote) {}

    @Nullable
    public static Busto actual;

    /** Fila del torso donde arranca el busto (px desde los hombros). */
    private static final float ARRIBA = 1.6f;
    /** Anillos (del centro al borde) y gajos alrededor de cada cúpula. */
    private static final int ANILLOS = 7, GAJOS = 18;
    /** Exponente del perfil: 2 = media elipse; un poco más, más lleno hasta el borde. */
    private static final float PERFIL = 2.3f;

    /** ¿Una pieza que cubre las filas [desde, hasta) del torso tapa el busto? */
    public static boolean cubre(int desde, int hasta) {
        return desde <= 2 && hasta >= 5;
    }

    /** La forma de una cúpula ya calculada para este talle, inflado y rebote. */
    private record Forma(float cx, float cy, float rx, float rArriba, float rAbajo, float hondo,
                         float caida, float apertura, float dx, float dy, float plano, int lado) {

        /** Punto de la cúpula en el anillo {@code r} (0 centro .. 1 borde) y el ángulo {@code a}. */
        Vector3f punto(float r, double a) {
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float x = cx + rx * r * c;
            float y = cy + (s > 0 ? rAbajo : rArriba) * r * s;
            // Perfil redondo: alto en el centro, cae a 0 en el borde.
            float sale = hondo * (float) Math.pow(Math.max(0.0, 1.0 - Math.pow(r, PERFIL)), 1.0 / PERFIL);
            float k = hondo <= 1e-4f ? 0f : sale / hondo;          // 0 en el borde, 1 en la punta
            // Lo que sobresale cae, se abre hacia afuera y rebota; la base no se mueve.
            y += caida * sale + dy * k;
            x += lado * apertura * sale + dx * k;
            return new Vector3f(x, y, plano - sale);
        }

        /** UV planas: el frente del torso visto de frente (u 20..28, v desde {@code filaSkin}). */
        float u(float x) { return 24f + x; }
    }

    /**
     * @param inflado  cuánto sale la tela por fuera del cuerpo en el pecho (px; 0 = la piel)
     * @param carpa    cuánto más abajo cae la tela holgada debajo del busto (px)
     * @param filaSkin fila de la skin donde empieza el frente del torso: 20 (la
     *                 piel y todas las telas) o 36 (la segunda capa de la skin)
     */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov,
                               float inflado, float carpa, float filaSkin) {
        float t = b.talle() * b.sujecion();
        if (t <= 0.05f) return;
        float dy = 0f, dx = 0f;
        if (b.rebote() != null) {
            dy = b.rebote().dy() * 12f;
            dx = b.rebote().dx() * 8f;
        }
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f n = matrices.peek().getNormalMatrix();
        float hondo = (0.55f + 0.5f * t) + inflado;
        float rx = Math.min(2.6f, 1.95f + 0.07f * t) + inflado;
        float rArriba = 1.5f + 0.12f * t + inflado;
        float rAbajo = 1.3f + 0.22f * t + inflado + carpa;
        float caida = (0.06f + 0.025f * t) * (0.6f + 0.4f * b.sujecion());
        float apertura = 0.04f + 0.012f * t;
        for (int lado = -1; lado <= 1; lado += 2) {
            Forma f = new Forma(lado * 2f, ARRIBA + 1.5f + 0.12f * t, rx, rArriba, rAbajo, hondo,
                    caida, apertura, dx, dy * 0.6f, -2f - inflado, lado);
            cupula(f, m, n, vc, luz, ov, filaSkin);
        }
    }

    private static void cupula(Forma f, Matrix4f m, Matrix3f n, VertexConsumer vc, int luz, int ov, float filaSkin) {
        Vector3f[][] p = new Vector3f[ANILLOS + 1][GAJOS + 1];
        Vector3f[][] nor = new Vector3f[ANILLOS + 1][GAJOS + 1];
        float[][] uu = new float[ANILLOS + 1][GAJOS + 1], vv = new float[ANILLOS + 1][GAJOS + 1];
        for (int i = 0; i <= ANILLOS; i++) {
            float r = i / (float) ANILLOS;
            for (int j = 0; j <= GAJOS; j++) {
                double a = Math.PI * 2 * j / GAJOS;
                p[i][j] = f.punto(r, a);
                // Normal por diferencias en r y en el ángulo (en el centro, hacia adelante).
                float er = 0.02f;
                Vector3f dr = f.punto(Math.min(1f, r + er), a).sub(f.punto(Math.max(0f, r - er), a));
                Vector3f da = f.punto(Math.max(r, 0.02f), a + 0.05).sub(f.punto(Math.max(r, 0.02f), a - 0.05));
                Vector3f nn = dr.cross(da);
                if (nn.z > 0) nn.negate();
                if (nn.lengthSquared() < 1e-10f) nn.set(0, 0, -1);
                nor[i][j] = nn.normalize();
                // UV de la base (sin caída ni rebote): la piel no se corre al moverse.
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float bx = f.cx() + f.rx() * r * c, by = f.cy() + (s > 0 ? f.rAbajo() : f.rArriba()) * r * s;
                uu[i][j] = Math.max(20f, Math.min(28f, f.u(bx))) / 64f;
                vv[i][j] = Math.max(filaSkin, Math.min(filaSkin + 12f, filaSkin + by)) / 64f;
            }
        }
        for (int i = 0; i < ANILLOS; i++) {
            for (int j = 0; j < GAJOS; j++) {
                vertice(m, n, vc, luz, ov, p[i][j], uu[i][j], vv[i][j], nor[i][j]);
                vertice(m, n, vc, luz, ov, p[i + 1][j], uu[i + 1][j], vv[i + 1][j], nor[i + 1][j]);
                vertice(m, n, vc, luz, ov, p[i + 1][j + 1], uu[i + 1][j + 1], vv[i + 1][j + 1], nor[i + 1][j + 1]);
                vertice(m, n, vc, luz, ov, p[i][j + 1], uu[i][j + 1], vv[i][j + 1], nor[i][j + 1]);
            }
        }
    }

    private static void vertice(Matrix4f m, Matrix3f n, VertexConsumer vc, int luz, int ov,
                                Vector3f p, float u, float v, Vector3f normal) {
        Vector3f w = m.transformPosition(p.x / 16f, p.y / 16f, p.z / 16f, new Vector3f());
        Vector3f nn = n.transform(new Vector3f(normal)).normalize();
        vc.vertex(w.x, w.y, w.z, 0xFFFFFFFF, u, v, ov, luz, nn.x, nn.y, nn.z);
    }
}
