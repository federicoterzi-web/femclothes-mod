package com.femclothes.render;

import com.femclothes.item.CapaItem;
import com.femclothes.item.CapaRuedo;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

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

    /** Aplica a {@code matrices} lo mismo que la capa vanilla antes de dibujarse. */
    public static void giros(MatrixStack matrices, AbstractClientPlayerEntity p, float h) {
        matrices.translate(0.0F, 0.0F, 0.125F);
        Movimiento mv = movimiento(p, h);
        float q = mv.vertical() + mv.paso();
        if (p.isInSneakingPose()) q += 25.0F;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + mv.atras() / 2.0F + q));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mv.lado() / 2.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - mv.lado() / 2.0F));
    }

    /** El paño (exterior + forro + cantos) y la capucha, ya dentro de {@link #giros}. */
    public static void dibujarPano(MatrixStack matrices, VertexConsumer vc, int luz, ItemStack stack) {
        MatrixStack.Entry e = matrices.peek();
        float largo = CapaItem.largo(stack).pixeles;
        boolean redondeado = CapaItem.ruedo(stack) == CapaRuedo.REDONDEADO;
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
        if (CapaItem.capucha(stack)) dibujarCapucha(vc, e, luz);
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
        MatrixStack.Entry e = matrices.peek();
        int n = 12;
        float radio = 4.7f, alto = 3.5f;
        for (int k = 0; k < n; k++) {
            double f0 = Math.PI * k / n, f1 = Math.PI * (k + 1) / n;
            // En el espacio del cuboide girado, la nuca queda en z = 2 (ver translate de giros).
            float x0 = (float) (radio * Math.cos(f0)), z0 = 2f - (float) (radio * Math.sin(f0));
            float x1 = (float) (radio * Math.cos(f1)), z1 = 2f - (float) (radio * Math.sin(f1));
            float u0 = (24f + 16f * k / n) / 64f, u1 = (24f + 16f * (k + 1) / n) / 64f;
            float nx = (float) Math.cos((f0 + f1) / 2), nz = -(float) Math.sin((f0 + f1) / 2);
            quad(vc, e, luz, x0, -alto, z0, u0, 2 / 64f, x1, -alto, z1, u1, 2 / 64f,
                    x1, 0.3f, z1, u1, 6 / 64f, x0, 0.3f, z0, u0, 6 / 64f, nx, 0, nz);
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

    private static void v(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float vv,
                          int luz, float nx, float ny, float nz) {
        vc.vertex(e.getPositionMatrix(), x / 16f, y / 16f, z / 16f)
                .color(0xFFFFFFFF)
                .texture(u, vv)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(luz)
                .normal(e, nx, ny, nz);
    }
}
