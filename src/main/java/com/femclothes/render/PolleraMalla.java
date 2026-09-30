package com.femclothes.render;

import com.femclothes.item.PolleraForma;
import com.femclothes.item.PolleraLargo;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * La pollera como malla propia — quinta vuelta (2026-09-29, "resolveme la
 * pollera que se ve horrible a veces y quiero poder hacerle distintos
 * largos y teñirla y sublimarla"). Reemplaza a {@link PolleraGeometria}
 * (20 gajos rígidos): los gajos no se podían afinar ni doblar, las piernas
 * los atravesaban al caminar y todos compartían un rinconcito de 1.8x9 de
 * textura, así que no entraba ningún patrón ni foto.
 *
 * <h2>Forma</h2>
 * Un tubo de {@link #COLUMNAS}x{@link #FILAS} cuadriláteros que nace en la
 * cintura (fila {@link #Y_CINTURA} del torso) con la sección del torso (caja
 * de esquinas redondeadas) y se abre hacia el ruedo, cada vez más redondo.
 * {@link PolleraForma#TABLEADA} le suma un zigzag radial (pliegues en V,
 * {@link #TABLAS} tablas) que crece hacia abajo — la idea del pliegue en V
 * de la vieja de gajos, pero continua.
 *
 * <h2>Tela</h2>
 * El perímetro recorre la franja de caras de la caja del TORSO de la skin
 * (u 16..40: costado derecho, frente, costado izquierdo, espalda — el mismo
 * orden y sentido que la caja) y el largo va de v 20 a 32. Así la textura
 * de la pollera tiene el layout de una caja de torso y la tiñen, estampan y
 * perforan las mismas rutinas que al resto de las prendas.
 *
 * <h2>Piernas y movimiento</h2>
 * {@link #piernasAbiertas} (2026-09-29, "probaria las dos"; se cambia con
 * {@code /femclothesdebug pollera abierta|rigida}): abierta = la tela choca
 * con las piernas ({@link #chocarConPiernas}, contra su caja real) y se
 * mueve con el cuerpo; rígida = quieta, sin piernas ni movimiento, más ancha
 * adelante y atrás. El movimiento (2026-09-30, "habria que animarlas segun
 * el movimiento") usa la inercia de la capa vanilla ({@link CapaMalla#movimiento}):
 * el ruedo queda atrás al caminar/correr, se abre al caer, se achica al
 * saltar, se balancea de costado y se retuerce un poco con cada paso. Todo
 * crece hacia el ruedo (la cintura no se mueve).
 *
 * <p>Coordenadas: las del torso del jugador (píxeles, Y hacia abajo, el
 * frente en -Z), dibujadas después de {@code ModelPart.rotate} del torso.
 */
public final class PolleraMalla {

    private PolleraMalla() {}

    /** Modo de prueba de las piernas — ver el javadoc de la clase. */
    public static boolean piernasAbiertas = true;

    private static final float Y_CINTURA = 9f;
    private static final int COLUMNAS = 48;
    private static final int FILAS = 10;
    private static final int TABLAS = 12;
    /** Largo del perímetro de la caja del torso en la textura (4+8+4+8). */
    private static final float PERIMETRO = 24f;

    /**
     * Las dos piernas del modelo, para que la tela choque con ellas
     * (2026-09-30, "hay forma de hacer q la tela de la pollera colisione con
     * las piernas?" → opción A, colisión geométrica sin estado).
     * Cada pierna es su caja real de 4x12x4 con la pose de este frame (pivote
     * y giros de {@link ModelPart}, agachada incluida), llevada al marco del
     * torso donde vive la malla.
     */
    public static final class Piernas {
        final Matrix4f[] aLocal = new Matrix4f[2];
        final Matrix4f[] aTorso = new Matrix4f[2];

        public Piernas(ModelPart torso, ModelPart der, ModelPart izq) {
            this(marco(torso), der, izq);
        }

        /**
         * Con un marco cualquiera en vez del torso (2026-09-30, la capa): {@code base}
         * lleva de ese marco al del modelo, en píxeles.
         */
        public Piernas(Matrix4f base, ModelPart der, ModelPart izq) {
            Matrix4f mTorso = base;
            ModelPart[] piernas = {der, izq};
            for (int i = 0; i < 2; i++) {
                aTorso[i] = mTorso.invert(new Matrix4f()).mul(marco(piernas[i]));
                aLocal[i] = aTorso[i].invert(new Matrix4f());
            }
        }

        /** Lo mismo que {@code ModelPart#rotate}, en píxeles. */
        private static Matrix4f marco(ModelPart parte) {
            return new Matrix4f().translation(parte.pivotX, parte.pivotY, parte.pivotZ)
                    .rotateZYX(parte.roll, parte.yaw, parte.pitch);
        }
    }

    /** Media caja de la pierna (2) + la tela del pantalón/medias y un poco de aire. */
    private static final float MEDIO_ANCHO_PIERNA = 2.55f;
    /** Radio de las esquinas de la sección de la pierna (así la tela no se quiebra en la arista). */
    private static final float ESQUINA_PIERNA = 1.2f;

    /**
     * @param dil     dilatación de la tela (calce): la cintura sale un poco por fuera del torso
     * @param piernas las piernas contra las que choca la tela, o null (sin colisión)
     */
    public static void dibujar(MatrixStack matrices, VertexConsumer vc, int luz, PolleraForma forma,
                               PolleraLargo largo, float dil, @Nullable Piernas piernas,
                               CapaMalla.Movimiento mov, float twirl) {
        float[][][] p = new float[FILAS + 1][COLUMNAS + 1][];
        float l = largo.pixeles;
        float a0 = 4f + dil + 0.1f, b0 = 2f + dil + 0.1f;
        float vueloX = 0.8f + 0.18f * l, vueloZ = 1.2f + 0.26f * l;
        if (!piernasAbiertas) {
            vueloZ *= 1.5f;
            mov = CapaMalla.Movimiento.QUIETO;
            piernas = null;
        }
        boolean tableada = forma == PolleraForma.TABLEADA;
        // Cuánto se mueve el ruedo (en px, a t = 1): hacia atrás con la
        // velocidad, de costado con el giro, y cuánto se abre al caer.
        float atras = l * 0.5f * Math.min(1f, mov.atras() / 80f);
        float costado = -l * 0.3f * mov.lado() / 20f;
        float abrir = mov.vertical() > 0f ? 0.6f * Math.min(1f, mov.vertical() / 20f)
                : -0.12f * Math.min(1f, -mov.vertical() / 6f);
        float giro = 0.18f * mov.paso() / 32f;
        // Twirl (2026-09-30, "que se abran y giren"): la campana se abre casi
        // horizontal a mitad de la vuelta y el ruedo queda atrasado respecto
        // del giro del cuerpo (la tela arrastra). Vale también en rígida.
        if (twirl >= 0f) {
            float fuerza = (float) Math.sin(Math.PI * twirl);
            abrir = Math.max(abrir, 1.3f * fuerza);
            giro += 0.7f * fuerza;
            atras *= 1f - fuerza;
        }

        for (int f = 0; f <= FILAS; f++) {
            float t = f / (float) FILAS;
            float y = Y_CINTURA + t * l;
            float curva = (float) Math.pow(t, 1.15);
            float peso = (float) Math.pow(t, 1.6);
            float a = (a0 + vueloX * curva) * (1f + abrir * peso), b = (b0 + vueloZ * curva) * (1f + abrir * peso);
            float cosG = (float) Math.cos(giro * t), sinG = (float) Math.sin(giro * t);
            // De caja (4) a casi elipse (2.4) hacia el ruedo.
            float n = 4f - 1.6f * t;
            for (int c = 0; c <= COLUMNAS; c++) {
                float s = c * PERIMETRO / COLUMNAS;
                float[] q = seccion(s, a, b, n);
                float x = q[0], z = q[1];
                if (tableada) {
                    // Zigzag: 4 columnas por tabla, picos afuera/adentro alternados.
                    float fase = (s * TABLAS / PERIMETRO) % 1f;
                    float tri = (fase < 0.5f ? fase * 2f : 2f - fase * 2f) * 2f - 1f;
                    float amp = (0.45f + 0.035f * l) * t;
                    float nx = x / (a * a), nz = z / (b * b);
                    float len = (float) Math.sqrt(nx * nx + nz * nz);
                    if (len > 1e-4f) {
                        x += nx / len * amp * tri;
                        z += nz / len * amp * tri;
                    }
                }
                // Vaivén del paso: la tela se retuerce un poco alrededor del eje del cuerpo.
                float xg = x * cosG - z * sinG, zg = x * sinG + z * cosG;
                x = xg;
                z = zg;
                float yy = y;
                // Inercia: el ruedo se queda atrás (+Z) y de costado; al irse
                // para atrás/afuera también sube un poco (la tela no se estira).
                float dz = atras * peso, dx = costado * peso;
                z += dz;
                x += dx;
                yy -= (dz * 0.35f + Math.abs(dx) * 0.2f + abrir * l * 0.2f * peso);
                p[f][c] = new float[]{x, yy, z, (16f + s) / 64f, (20f + 12f * t) / 64f};
            }
        }

        if (piernas != null) chocarConPiernas(p, piernas);

        MatrixStack.Entry e = matrices.peek();
        int ov = OverlayTexture.DEFAULT_UV;
        for (int f = 0; f < FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                emitir(vc, e, p, f, c, luz, ov);
                emitir(vc, e, p, f, c + 1, luz, ov);
                emitir(vc, e, p, f + 1, c + 1, luz, ov);
                emitir(vc, e, p, f + 1, c, luz, ov);
            }
        }
    }

    /**
     * Punto del perímetro {@code s} (0..24, mismo recorrido que la franja de
     * la caja: derecho de atrás hacia adelante, frente, izquierdo, espalda),
     * redondeado a una superelipse de exponente {@code n}.
     */
    private static float[] seccion(float s, float a, float b, float n) {
        float rx, rz;
        if (s < 4f) { rx = -1f; rz = 1f - s / 2f; }
        else if (s < 12f) { rx = -1f + (s - 4f) / 4f; rz = -1f; }
        else if (s < 16f) { rx = 1f; rz = -1f + (s - 12f) / 2f; }
        else { rx = 1f - (s - 16f) / 4f; rz = 1f; }
        double th = Math.atan2(rz, rx);
        double co = Math.cos(th), si = Math.sin(th);
        float x = (float) (a * Math.signum(co) * Math.pow(Math.abs(co), 2.0 / n));
        float z = (float) (b * Math.signum(si) * Math.pow(Math.abs(si), 2.0 / n));
        return new float[]{x, z};
    }

    /**
     * Colisión con las piernas: todo punto de tela que quede adentro de una
     * pierna (caja con esquinas redondeadas, un poco inflada) sale hacia
     * AFUERA de la pollera — por el radio de la malla, no por la cara más
     * cercana, así la tela nunca se mete entre las piernas. Después se
     * suavizan los corrimientos con los vecinos (sin eso queda un pico donde
     * la pierna "pincha") y se vuelve a chocar, para que el suavizado no
     * deje nada adentro. La cintura (fila 0) no se toca.
     */
    private static void chocarConPiernas(float[][][] p, Piernas piernas) {
        float[][][] despl = new float[FILAS + 1][COLUMNAS][];
        boolean alguno = false;
        for (int f = 1; f <= FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                float[] d = empujar(p[f][c], piernas);
                despl[f][c] = d;
                alguno |= d != null;
            }
        }
        if (!alguno) return;
        for (int f = 1; f <= FILAS; f++) {
            for (int c = 0; c < COLUMNAS; c++) {
                float sx = 0, sy = 0, sz = 0, w = 0;
                for (int df = -1; df <= 1; df++) {
                    int ff = f + df;
                    if (ff < 1 || ff > FILAS) continue;
                    for (int dc = -2; dc <= 2; dc++) {
                        float[] d = despl[ff][Math.floorMod(c + dc, COLUMNAS)];
                        float peso = (3 - Math.abs(dc)) * (df == 0 ? 2 : 1);
                        w += peso;
                        if (d == null) continue;
                        sx += d[0] * peso;
                        sy += d[1] * peso;
                        sz += d[2] * peso;
                    }
                }
                if (sx == 0 && sy == 0 && sz == 0) continue;
                float[] q = p[f][c];
                q[0] += sx / w;
                q[1] += sy / w;
                q[2] += sz / w;
                empujar(q, piernas);
            }
            // La columna del final es la misma que la 0 (cierra el tubo).
            p[f][COLUMNAS][0] = p[f][0][0];
            p[f][COLUMNAS][1] = p[f][0][1];
            p[f][COLUMNAS][2] = p[f][0][2];
        }
    }

    /**
     * Saca {@code q} (x, y, z en el marco del torso; se modifica) de las dos
     * piernas. Devuelve el corrimiento aplicado, o null si no estaba adentro.
     */
    @Nullable
    private static float[] empujar(float[] q, Piernas piernas) {
        float x0 = q[0], y0 = q[1], z0 = q[2];
        boolean movido = false;
        Vector3f v = new Vector3f();
        Vector3f dir = new Vector3f();
        for (int i = 0; i < 2; i++) {
            piernas.aLocal[i].transformPosition(v.set(q[0], q[1], q[2]));
            // Solo a lo largo de la pierna (arriba de la cadera la tela está en el torso).
            if (v.y < 0.5f || v.y > 12.5f || !dentroDePierna(v.x, v.z)) continue;
            // Hacia afuera de la pollera: el radio de la malla en el torso, llevado a la pierna.
            piernas.aLocal[i].transformDirection(dir.set(q[0], 0f, q[2]));
            dir.y = 0f;
            if (dir.lengthSquared() < 1e-6f) dir.set(v.x, 0f, v.z);
            if (dir.lengthSquared() < 1e-6f) dir.set(0f, 0f, -1f);
            dir.normalize(0.2f);
            for (int k = 0; k < 60 && dentroDePierna(v.x, v.z); k++) v.add(dir);
            piernas.aTorso[i].transformPosition(v);
            q[0] = v.x;
            q[1] = v.y;
            q[2] = v.z;
            movido = true;
        }
        return movido ? new float[]{q[0] - x0, q[1] - y0, q[2] - z0} : null;
    }

    /** ¿El punto (x, z) de la sección de la pierna cae adentro de la caja inflada con esquinas redondeadas? */
    static boolean dentroDePierna(float x, float z) {
        float lado = MEDIO_ANCHO_PIERNA - ESQUINA_PIERNA;
        float cx = Math.max(-lado, Math.min(lado, x)), cz = Math.max(-lado, Math.min(lado, z));
        float dx = x - cx, dz = z - cz;
        return dx * dx + dz * dz < ESQUINA_PIERNA * ESQUINA_PIERNA;
    }

    private static void emitir(VertexConsumer vc, MatrixStack.Entry e, float[][][] p, int f, int c, int luz, int ov) {
        float[] v = p[f][c];
        // Normal por diferencias con los vecinos (la columna 0 y la última son el mismo punto).
        float[] cIzq = p[f][c == 0 ? COLUMNAS - 1 : c - 1];
        float[] cDer = p[f][c == COLUMNAS ? 1 : c + 1];
        float[] fArr = p[Math.max(0, f - 1)][c];
        float[] fAba = p[Math.min(FILAS, f + 1)][c];
        float tx = cDer[0] - cIzq[0], ty = cDer[1] - cIzq[1], tz = cDer[2] - cIzq[2];
        float lx = fAba[0] - fArr[0], ly = fAba[1] - fArr[1], lz = fAba[2] - fArr[2];
        float nx = ty * lz - tz * ly, ny = tz * lx - tx * lz, nz = tx * ly - ty * lx;
        // Que apunte hacia afuera del tubo.
        if (nx * v[0] + nz * v[2] < 0f) { nx = -nx; ny = -ny; nz = -nz; }
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1e-5f) { nx /= len; ny /= len; nz /= len; } else { nx = 0; ny = 0; nz = -1; }
        vc.vertex(e.getPositionMatrix(), v[0] / 16f, v[1] / 16f, v[2] / 16f)
                .color(0xFFFFFFFF)
                .texture(v[3], v[4])
                .overlay(ov)
                .light(luz)
                .normal(e, nx, ny, nz);
    }
}
