package com.modamod.render;

import com.modamod.item.CapaItem;
import com.modamod.item.CapaRuedo;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * La capa del mod (2026-09-29, "capas... como las capas vanilla (misma
 * dinamica de tela) pero usable y personalizable").
 *
 * <h2>Dinámica</h2>
 * La MISMA cuenta que {@code CapeFeatureRenderer} de la 1.21.1: se inclina
 * hacia atrás según la velocidad (0..150°), sube y baja con los saltos
 * (-6..32), se balancea de costado (±20°), acompaña el paso y suma 25°
 * agachado. {@link #giros} la reproduce paso a paso.
 *
 * <h2>Tela</h2>
 * El paño usa el mismo mapeo que el cuboide de la capa vanilla (10x16x1 en
 * uv 0,0): el EXTERIOR es la cara del frente del cuboide (u 1..11, v 1..17,
 * la que queda mirando hacia atrás), el FORRO la de atrás (u 12..22). El
 * largo real se estira sobre esos 16 de alto. Capucha y cuello alto sacan la
 * tela de una segunda caja (12x8x2 en uv 24,0; ver {@code PatronGenerador}).
 *
 * <p>Coordenadas en píxeles, en el espacio del cuboide vanilla (antes de los
 * giros): x -5..5, y hacia abajo desde los hombros, exterior en z=-1 y forro
 * en z=0.
 */
public final class CapaMalla {

    private CapaMalla() {}

    private static final int COLS = 10, FILAS = 8;

    /**
     * Lo que la capa vanilla lee del jugador para moverse, ya acotado igual
     * que en {@code CapeFeatureRenderer}: {@code atras} = velocidad hacia
     * adelante (0..150), {@code vertical} = caída/subida (-6..32, sin el paso),
     * {@code lado} = velocidad de costado (-20..20), {@code paso} = el vaivén
     * de la caminata (-32..32). También lo usa la pollera ({@link PolleraMalla}).
     */
    public record Movimiento(float atras, float vertical, float lado, float paso) {
        public static final Movimiento QUIETO = new Movimiento(0f, 0f, 0f, 0f);
    }

    public static Movimiento movimiento(AbstractClientPlayerEntity p, float h) {
        double d = MathHelper.lerp(h, p.prevCapeX, p.capeX) - MathHelper.lerp(h, p.prevX, p.getX());
        double e = MathHelper.lerp(h, p.prevCapeY, p.capeY) - MathHelper.lerp(h, p.prevY, p.getY());
        double m = MathHelper.lerp(h, p.prevCapeZ, p.capeZ) - MathHelper.lerp(h, p.prevZ, p.getZ());
        float n = MathHelper.lerpAngleDegrees(h, p.prevBodyYaw, p.bodyYaw);
        double o = MathHelper.sin(n * 0.017453292F);
        double q0 = -MathHelper.cos(n * 0.017453292F);
        float q = MathHelper.clamp((float) e * 10.0F, -6.0F, 32.0F);
        float r = MathHelper.clamp((float) (d * o + m * q0) * 100.0F, 0.0F, 150.0F);
        float s = MathHelper.clamp((float) (d * q0 - m * o) * 100.0F, -20.0F, 20.0F);
        float t = MathHelper.lerp(h, p.prevStrideDistance, p.strideDistance);
        float paso = MathHelper.sin(MathHelper.lerp(h, p.prevHorizontalSpeed, p.horizontalSpeed) * 6.0F) * 32.0F * t;
        return new Movimiento(r, q, s, paso);
    }

    // ── la tela doblada ─────────────────────────────────────────────────
    // 2026-09-30, "es una placa tiesa, no hay render de tela": la capa
    // vanilla es un cuboide rígido que gira entero desde los hombros, y esta
    // la copiaba. Ahora el paño es una cadena de TRAMOS tramos: arriba sigue a
    // la espalda y cada tramo se inclina un poco más que el anterior (la tela
    // se curva y el ruedo es lo que más se levanta), con una onda que baja
    // por la tela al moverse, un vaivén suave quieta, el balanceo de costado,
    // y cada tramo se abre lo necesario para no atravesar las piernas.
    // Sin estado: todo sale de la inercia de la capa vanilla y del tiempo.

    private static final int TRAMOS = 16;
    private static final float[] NODO_Y = new float[TRAMOS + 1], NODO_Z = new float[TRAMOS + 1], NODO_A = new float[TRAMOS + 1];
    private static float tramo = 1f, corrimientoLado = 0f;
    private static final float[] MUESTRAS_X = {-4.5f, -3f, -1.5f, 0f, 1.5f, 3f, 4.5f};

    /**
     * Arma la cadena para este frame. Coordenadas en píxeles en el marco de la
     * capa (el de {@code CapeFeatureRenderer} después de su corrimiento de
     * 2 px hacia atrás): y hacia abajo, z hacia atrás.
     *
     * @param piernas las piernas llevadas a este marco, o null
     */
    private static void doblar(float largo, Movimiento mv, boolean agachado, float tiempo,
                               @Nullable PolleraMalla.Piernas piernas, float piso) {
        float total = 6f + mv.atras() / 2f + mv.vertical() + mv.paso() + (agachado ? 25f : 0f);
        float onda = Math.min(1f, mv.atras() / 60f) * 9f;
        tramo = largo / TRAMOS;
        corrimientoLado = (float) Math.sin(Math.toRadians(mv.lado() / 2f)) * 0.9f;
        NODO_Y[0] = 0f;
        NODO_Z[0] = 0f;
        Vector3f v = new Vector3f();
        for (int k = 0; k < TRAMOS; k++) {
            float t = (k + 0.5f) / TRAMOS;
            float grados = 6f + (total - 6f) * (0.3f + 0.9f * t)
                    + onda * t * (float) Math.sin(tiempo * 0.45f - t * 6f)
                    + 1.8f * t * (float) Math.sin(tiempo * 0.07f - t * 2.5f);
            float a = (float) Math.toRadians(Math.max(2f, Math.min(160f, grados)));
            // Se abre de a 3° hasta que ningún punto del borde del tramo quede adentro de una pierna.
            for (int intento = 0; piernas != null && intento < 40; intento++) {
                float y = NODO_Y[k] + tramo * (float) Math.cos(a), z = NODO_Z[k] + tramo * (float) Math.sin(a);
                boolean choca = false;
                for (float x : MUESTRAS_X) {
                    for (int i = 0; i < 2 && !choca; i++) {
                        piernas.aLocal[i].transformPosition(v.set(x + corrimientoLado * (k + 1) * tramo, y, z));
                        choca = v.y > 0.5f && v.y < 12.5f && PolleraMalla.dentroDePierna(v.x, v.z);
                    }
                    if (choca) break;
                }
                if (!choca) break;
                a += (float) Math.toRadians(3f);
            }
            float yn = NODO_Y[k] + tramo * (float) Math.cos(a), zn = NODO_Z[k] + tramo * (float) Math.sin(a);
            if (yn > piso) {
                // Ruedo con cola (2026-10-04): la tela que pasaría del piso se apoya en él y sigue hacia atrás.
                float dy = Math.max(0f, piso - NODO_Y[k]);
                float dz = (float) Math.sqrt(Math.max(0f, tramo * tramo - dy * dy));
                a = (float) Math.atan2(dz, dy);
                yn = NODO_Y[k] + dy;
                zn = NODO_Z[k] + dz;
            }
            NODO_A[k] = a;
            NODO_Y[k + 1] = yn;
            NODO_Z[k + 1] = zn;
        }
        NODO_A[TRAMOS] = NODO_A[TRAMOS - 1];
    }

    /**
     * Local del cuboide vanilla (x -5..5, y = distancia bajando por la tela,
     * z -1 exterior .. 0 forro) → marco de la capa, doblado. Devuelve x, y, z.
     */
    private static void doblado(float lx, float ly, float lz, float[] salida, float[] normal) {
        float d = Math.max(0f, ly) / tramo;
        int k = Math.min(TRAMOS - 1, (int) d);
        float f = Math.min(1f, d - k);
        float cy = NODO_Y[k] + (NODO_Y[k + 1] - NODO_Y[k]) * f;
        float cz = NODO_Z[k] + (NODO_Z[k + 1] - NODO_Z[k]) * f;
        // El ángulo, parejo entre tramos (así la tela no se quiebra en las uniones).
        float ak = k == 0 ? NODO_A[0] : (NODO_A[k - 1] + NODO_A[k]) / 2f;
        float ak1 = (NODO_A[k] + NODO_A[Math.min(TRAMOS, k + 1)]) / 2f;
        float a = ak + (ak1 - ak) * f;
        float sin = (float) Math.sin(a), cos = (float) Math.cos(a);
        // Vuelta de 180° de la capa vanilla: x y z cambian de signo; w = cuánto se aleja de la espalda.
        float w = -lz;
        salida[0] = -lx + corrimientoLado * ly;
        salida[1] = cy - w * sin;
        salida[2] = cz + w * cos;
        if (normal != null) {
            float ny = normal[1], nz = -normal[2];
            normal[0] = -normal[0];
            normal[1] = ny * cos - nz * sin;
            normal[2] = ny * sin + nz * cos;
        }
    }

    /**
     * El paño (exterior + forro + cantos) y la capucha, doblados como tela.
     * {@code matrices} ya tiene el corrimiento de la capa (2 px hacia atrás, y
     * el de agachado), sin giros: los pone {@link #doblar} vértice por vértice.
     */
    public static void dibujarPano(MatrixStack matrices, VertexConsumer vc, int luz, ItemStack stack,
                                   Movimiento mv, boolean agachado, float tiempo,
                                   @Nullable PolleraMalla.Piernas piernas) {
        dibujarPano(matrices, vc, luz, stack, mv, agachado, tiempo, piernas, false);
    }

    /** @param sinCapucha con un hoodie puesto su capucha manda: la de la capa no se dibuja (2026-10-02) */
    public static void dibujarPano(MatrixStack matrices, VertexConsumer vc, int luz, ItemStack stack,
                                   Movimiento mv, boolean agachado, float tiempo,
                                   @Nullable PolleraMalla.Piernas piernas, boolean sinCapucha) {
        dibujarPano(matrices, vc, luz, stack, mv, agachado, tiempo, piernas, sinCapucha, true);
    }

    /** @param conCola false sentado, montado, nadando, durmiendo o gateando: la cola no se apoya en ningún piso */
    public static void dibujarPano(MatrixStack matrices, VertexConsumer vc, int luz, ItemStack stack,
                                   Movimiento mv, boolean agachado, float tiempo,
                                   @Nullable PolleraMalla.Piernas piernas, boolean sinCapucha, boolean conCola) {
        MatrixStack.Entry e = matrices.peek();
        CapaRuedo ruedo = CapaItem.ruedo(stack);
        float cola = ruedo == CapaRuedo.COLA && conCola ? CapaRuedo.COLA_PIXELES : 0f;
        float largo = CapaItem.largo(stack).pixeles + cola;
        // El piso en el marco de la capa: los pies están 24 px debajo de los hombros (menos lo que baja agachado).
        float piso = cola > 0f ? 24f - (agachado ? 1.85f : 0f) - 0.03f : Float.MAX_VALUE;
        doblar(largo, mv, agachado, tiempo, piernas, piso);
        boolean redondeado = ruedo == CapaRuedo.REDONDEADO;
        // Largo de cada columna: con ruedo redondeado las puntas suben en arco.
        float[] l = new float[COLS + 1];
        for (int c = 0; c <= COLS; c++) {
            float x = -5f + c;
            float k = x / 5f;
            float sube = redondeado ? Math.min(largo * 0.35f, 4.5f) * (1f - (float) Math.sqrt(Math.max(0f, 1f - k * k))) : 0f;
            l[c] = largo - sube;
        }
        for (int f = 0; f < FILAS; f++) {
            float t0 = f / (float) FILAS, t1 = (f + 1) / (float) FILAS;
            for (int c = 0; c < COLS; c++) {
                float xa = -5f + c, xb = xa + 1f;
                float ya0 = t0 * l[c], yb0 = t0 * l[c + 1], ya1 = t1 * l[c], yb1 = t1 * l[c + 1];
                float v0 = (1f + 16f * t0) / 64f, v1 = (1f + 16f * t1) / 64f;
                // Exterior (z = -1), u crece con x como la cara norte del cuboide.
                float ua = (1f + (xa + 5f)) / 64f, ub = (1f + (xb + 5f)) / 64f;
                quad(vc, e, luz,
                        xb, yb0, -1, ub, v0, xa, ya0, -1, ua, v0, xa, ya1, -1, ua, v1, xb, yb1, -1, ub, v1, 0, 0, -1);
                // Forro (z = 0), u decrece con x como la cara sur.
                float fa = (12f + (5f - xa)) / 64f, fb = (12f + (5f - xb)) / 64f;
                quad(vc, e, luz,
                        xa, ya0, 0, fa, v0, xb, yb0, 0, fb, v0, xb, yb1, 0, fb, v1, xa, ya1, 0, fa, v1, 0, 0, 1);
            }
        }
        // Cantos de los costados y del ruedo, con la columna del costado de la textura.
        for (int f = 0; f < FILAS; f++) {
            float t0 = f / (float) FILAS, t1 = (f + 1) / (float) FILAS;
            float v0 = (1f + 16f * t0) / 64f, v1 = (1f + 16f * t1) / 64f;
            quad(vc, e, luz, -5, t0 * l[0], 0, 0, v0, -5, t0 * l[0], -1, 1 / 64f, v0,
                    -5, t1 * l[0], -1, 1 / 64f, v1, -5, t1 * l[0], 0, 0, v1, -1, 0, 0);
            quad(vc, e, luz, 5, t0 * l[COLS], -1, 11 / 64f, v0, 5, t0 * l[COLS], 0, 12 / 64f, v0,
                    5, t1 * l[COLS], 0, 12 / 64f, v1, 5, t1 * l[COLS], -1, 11 / 64f, v1, 1, 0, 0);
        }
        for (int c = 0; c < COLS; c++) {
            float xa = -5f + c, xb = xa + 1f;
            float ua = (11f + (xa + 5f)) / 64f, ub = (11f + (xb + 5f)) / 64f;
            quad(vc, e, luz, xa, l[c], -1, ua, 0, xb, l[c + 1], -1, ub, 0,
                    xb, l[c + 1], 0, ub, 1 / 64f, xa, l[c], 0, ua, 1 / 64f, 0, 1, 0);
        }
        // Con la capucha puesta (tecla, 2026-09-30) la dibuja CuelloYCapucha en la cabeza.
        if (CapaItem.capucha(stack) && !sinCapucha && !com.modamod.item.TopCorte.capuchaArriba(stack)) dibujarCapucha(vc, e, luz);
    }

    /**
     * Capucha caída sobre la espalda: una bolsa que sale del exterior en la
     * parte de arriba (x -4..4, y 0..7), abombada hasta 2.6 px. Tela: la cara
     * del frente de la caja de detalles (u 26..38, v 2..10).
     */
    private static void dibujarCapucha(VertexConsumer vc, MatrixStack.Entry e, int luz) {
        int cx = 8, fy = 6;
        float[][][] p = new float[fy + 1][cx + 1][];
        for (int j = 0; j <= fy; j++) {
            float y = 7f * j / fy;
            for (int i = 0; i <= cx; i++) {
                float x = -4f + 8f * i / cx;
                float bulto = 2.6f * (float) Math.sin(Math.PI * y / 7f) * (float) Math.sqrt(Math.max(0f, 1f - (x / 4f) * (x / 4f)));
                p[j][i] = new float[]{x, y, -1.05f - bulto, (26f + 12f * i / cx) / 64f, (2f + 8f * j / fy) / 64f};
            }
        }
        for (int j = 0; j < fy; j++) {
            for (int i = 0; i < cx; i++) {
                float[] a = p[j][i + 1], b = p[j][i], c = p[j + 1][i], d = p[j + 1][i + 1];
                quad(vc, e, luz, a[0], a[1], a[2], a[3], a[4], b[0], b[1], b[2], b[3], b[4],
                        c[0], c[1], c[2], c[3], c[4], d[0], d[1], d[2], d[3], d[4], 0, 0, -1);
            }
        }
    }

    /**
     * Cuello alto: media vuelta parada detrás de la nuca, de 3.5 px de alto
     * sobre los hombros. Va FUERA de {@link #giros} (solo el corrimiento de
     * la capa y el giro de 180°): no se inclina con la tela. Tela: la franja
     * de la caja de detalles (u 24..40, v 2..6).
     */
    public static void dibujarCuello(MatrixStack matrices, VertexConsumer vc, int luz) {
        sinDoblar = true;
        try {
            dibujarCuelloSinDoblar(matrices, vc, luz);
        } finally {
            sinDoblar = false;
        }
    }

    private static void dibujarCuelloSinDoblar(MatrixStack matrices, VertexConsumer vc, int luz) {
        MatrixStack.Entry e = matrices.peek();
        int n = 12;
        // 2026-09-30, "el cuello alto de la capa hay q abrirlo porque se esconde
        // en la cabeza": antes era media vuelta de radio 4.7 centrada en la
        // NUCA (z = 2 en este espacio, ver translate de giros), así que pasaba
        // por el medio de la cabeza (8 de profundidad: su centro queda 4 px
        // adelante de la nuca). Ahora se centra en la cabeza, rodea la parte
        // de atrás por fuera del sombrero de la skin (radio 5.3 abajo) y se
        // abre hacia arriba (6.6), como un cuello de capa de verdad.
        float centro = 2f - 4f, abajo = 5.3f, arriba = 6.6f, alto = 3.5f;
        for (int k = 0; k < n; k++) {
            double f0 = Math.PI * k / n, f1 = Math.PI * (k + 1) / n;
            float c0 = (float) Math.cos(f0), s0 = (float) Math.sin(f0);
            float c1 = (float) Math.cos(f1), s1 = (float) Math.sin(f1);
            float u0 = (24f + 16f * k / n) / 64f, u1 = (24f + 16f * (k + 1) / n) / 64f;
            float nx = (float) Math.cos((f0 + f1) / 2), nz = (float) Math.sin((f0 + f1) / 2);
            quad(vc, e, luz, arriba * c0, -alto, centro + arriba * s0, u0, 2 / 64f,
                    arriba * c1, -alto, centro + arriba * s1, u1, 2 / 64f,
                    abajo * c1, 0.3f, centro + abajo * s1, u1, 6 / 64f,
                    abajo * c0, 0.3f, centro + abajo * s0, u0, 6 / 64f, nx, 0, nz);
        }
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, int luz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float nx, float ny, float nz) {
        v(vc, e, x0, y0, z0, u0, v0, luz, nx, ny, nz);
        v(vc, e, x1, y1, z1, u1, v1, luz, nx, ny, nz);
        v(vc, e, x2, y2, z2, u2, v2, luz, nx, ny, nz);
        v(vc, e, x3, y3, z3, u3, v3, luz, nx, ny, nz);
    }

    private static final float[] PUNTO = new float[3], NORMAL = new float[3];
    /** Mientras se dibuja el cuello (que no se dobla con la tela), los vértices van tal cual. */
    private static boolean sinDoblar = false;

    private static void v(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float vv,
                          int luz, float nx, float ny, float nz) {
        if (!sinDoblar) {
            NORMAL[0] = nx;
            NORMAL[1] = ny;
            NORMAL[2] = nz;
            doblado(x, y, z, PUNTO, NORMAL);
            x = PUNTO[0];
            y = PUNTO[1];
            z = PUNTO[2];
            nx = NORMAL[0];
            ny = NORMAL[1];
            nz = NORMAL[2];
        }
        if (MallaCapturada.grabando != null) {
            MallaCapturada.grabando.agregar(e.getPositionMatrix(), e.getNormalMatrix(),
                    x / 16f, y / 16f, z / 16f, u, vv, nx, ny, nz);
        }
        vc.vertex(e.getPositionMatrix(), x / 16f, y / 16f, z / 16f)
                .color(0xFFFFFFFF)
                .texture(u, vv)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(luz)
                .normal(e, nx, ny, nz);
    }
}
