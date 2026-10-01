package com.femclothes.render.relieve;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * El busto como geometría propia (2026-10-01, "cambiaria los pechos buscando
 * mas la estrategia del mod only jugs"): como Only Jugs y el Female Gender
 * Mod en el que se inspira, dos cajas en el pecho — una por lado — que salen
 * del frente del torso, caen un poco según el talle y se abren hacia los
 * costados, con la textura de esa zona del pecho (la piel, o la prenda que
 * va encima, con su patrón y sus fotos). Reemplaza al busto del relieve, que
 * se superponía con lo pintado en las máscaras.
 *
 * <p>Se dibuja una vez para el cuerpo y otra por cada pieza del torso que
 * tape el pecho, inflada lo que esa tela está por fuera de la de abajo: así
 * la ropa lo envuelve. Las telas holgadas caen más abajo del busto (como una
 * carpa). El rebote ({@link FisicaBusto}) corre y gira las dos cajas.
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

    /** Fila del torso donde arranca el busto, y su alto base (px). */
    private static final float ARRIBA = 1.6f;

    /** ¿Una pieza que cubre las filas [desde, hasta) del torso tapa el busto? */
    public static boolean cubre(int desde, int hasta) {
        return desde <= 2 && hasta >= 5;
    }

    /**
     * @param inflado cuánto sale la tela por fuera del cuerpo en el pecho (px; 0 = la piel)
     * @param carpa   cuánto más abajo cae la tela holgada debajo del busto (px)
     * @param filaSkin fila de la skin donde empieza el frente del torso: 20 (la
     *                 piel y todas las telas) o 36 (la segunda capa de la skin)
     */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov,
                               float inflado, float carpa, float filaSkin) {
        float t = b.talle() * b.sujecion();
        if (t <= 0.05f) return;
        float hondo = 0.55f + 0.5f * t;                 // cuánto sale hacia adelante
        float alto = 3.4f + 0.32f * t;                  // de arriba a abajo
        float ancho = 4f + Math.min(0.6f, 0.06f * t);   // un poco más ancho que medio torso cuando crece
        float caida = (float) Math.toRadians((6f + 3.2f * t) * (0.6f + 0.4f * b.sujecion()));
        float dy = 0f, dx = 0f;
        if (b.rebote() != null) {
            dy = b.rebote().dy() * 12f;
            dx = b.rebote().dx() * 8f;
        }
        for (int lado = -1; lado <= 1; lado += 2) {
            MatrixStack.Entry e = matrices.peek();
            Matrix4f m = new Matrix4f(e.getPositionMatrix());
            org.joml.Matrix3f n = new org.joml.Matrix3f(e.getNormalMatrix());
            // Pivote arriba, contra el pecho, en el centro de cada lado.
            float cx = lado * 2f;
            Matrix4f local = new Matrix4f()
                    .translate((cx + dx) / 16f, (ARRIBA + dy * 0.45f) / 16f, -2f / 16f)
                    .rotateY((float) Math.toRadians(-lado * (4f + 1.2f * t)))       // se abren hacia afuera
                    .rotateX(caida + dy * 0.12f);                                   // caen (y rebotan)
            m.mul(local);
            org.joml.Matrix3f nl = new org.joml.Matrix3f(local);
            n.mul(nl);
            caja(m, n, vc, luz, ov, lado, ancho, alto, hondo, inflado, carpa, filaSkin);
        }
    }

    /**
     * Una caja: x de −ancho/2 a ancho/2 alrededor del pivote, y de 0 a alto,
     * z de −hondo a 0 (hacia adelante). Las UV salen de la mitad del frente
     * del torso que le toca (der u 20..24, izq 24..28; v desde la fila 1.6).
     */
    private static void caja(Matrix4f m, org.joml.Matrix3f n, VertexConsumer vc, int luz, int ov, int lado,
                             float ancho, float alto, float hondo, float inflado, float carpa, float filaSkin) {
        float x0 = -ancho / 2f - inflado, x1 = ancho / 2f + inflado;
        float y0 = -inflado, y1 = alto + inflado + carpa;
        float z0 = -hondo - inflado, z1 = 0f;
        // Mitad del frente del torso que le corresponde (skin px).
        float u0 = lado < 0 ? 20f : 24f, u1 = u0 + 4f;
        float v0 = filaSkin + ARRIBA, v1 = Math.min(filaSkin + 12f, v0 + alto + carpa * 0.6f);
        // Frente.
        quad(m, n, vc, luz, ov, 0, 0, -1,
                x0, y0, z0, u0, v0,
                x1, y0, z0, u1, v0,
                x1, y1, z0, u1, v1,
                x0, y1, z0, u0, v1);
        // Arriba (la fila de encima del pecho, estirada).
        quad(m, n, vc, luz, ov, 0, -1, 0,
                x0, y0, z1, u0, v0 - 0.5f,
                x1, y0, z1, u1, v0 - 0.5f,
                x1, y0, z0, u1, v0,
                x0, y0, z0, u0, v0);
        // Abajo (la última fila del busto, estirada).
        quad(m, n, vc, luz, ov, 0, 1, 0,
                x0, y1, z0, u0, v1 - 0.5f,
                x1, y1, z0, u1, v1 - 0.5f,
                x1, y1, z1, u1, v1,
                x0, y1, z1, u0, v1);
        // Costados: la columna del borde de cada lado, estirada.
        float uExt = lado < 0 ? u0 : u1 - 0.5f;
        float uInt = lado < 0 ? u1 - 0.5f : u0;
        float xExt = lado < 0 ? x0 : x1, xInt = lado < 0 ? x1 : x0;
        quad(m, n, vc, luz, ov, lado, 0, 0,
                xExt, y0, z1, uExt, v0,
                xExt, y0, z0, uExt + 0.5f, v0,
                xExt, y1, z0, uExt + 0.5f, v1,
                xExt, y1, z1, uExt, v1);
        quad(m, n, vc, luz, ov, -lado, 0, 0,
                xInt, y0, z0, uInt, v0,
                xInt, y0, z1, uInt + 0.5f, v0,
                xInt, y1, z1, uInt + 0.5f, v1,
                xInt, y1, z0, uInt, v1);
    }

    private static void quad(Matrix4f m, org.joml.Matrix3f n, VertexConsumer vc, int luz, int ov,
                             float nx, float ny, float nz, float... d) {
        Vector3f normal = n.transform(new Vector3f(nx, ny, nz)).normalize();
        for (int i = 0; i < 4; i++) {
            int k = i * 5;
            Vector3f p = m.transformPosition(d[k] / 16f, d[k + 1] / 16f, d[k + 2] / 16f, new Vector3f());
            vc.vertex(p.x, p.y, p.z, 0xFFFFFFFF, d[k + 3] / 64f, d[k + 4] / 64f, ov, luz, normal.x, normal.y, normal.z);
        }
    }
}
