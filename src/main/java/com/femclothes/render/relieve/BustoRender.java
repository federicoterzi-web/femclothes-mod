package com.femclothes.render.relieve;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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
    public record Busto(float talle, float sujecion, @Nullable RelieveRender.Rebote rebote, boolean cuadrado) {
        public Busto(float talle, float sujecion, @Nullable RelieveRender.Rebote rebote) {
            this(talle, sujecion, rebote, false);
        }
    }

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

        /**
         * Dónde se ve el punto de frente con la caída y la apertura pero sin el
         * rebote: de ahí salen las UV (2026-10-02, "la ropa interior... deforma
         * por ahi y el bretel del pecho no coincide con la tapa"): proyectadas
         * de frente sobre la forma ya caída, un bretel sigue derecho y empalma
         * con el del pecho plano; el rebote no hace correr la textura.
         */
        float[] vistoDeFrente(float r, double a) {
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float sale = sale(r, a);
            float x = cx + rx * r * c + lado * apertura * sale;
            float y = cy + (s > 0 ? rAbajo : rArriba) * r * s + caida * sale;
            return new float[]{x, y};
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

    /**
     * Las dos cúpulas (derecha, izquierda) de este busto con este inflado.
     *
     * <p>{@code piel}: la cúpula del cuerpo (o de la skin), que con un inflado
     * negativo se achica por dentro de la tela más ajustada. Si no, es una
     * TELA (2026-10-02, "con el hoodie se deberian tapar en vez de verse
     * gigantes"): cubre la punta del busto apenas por fuera (más afuera
     * cuanto más holgada, así las capas no se pisan) y se abre más ancha en la
     * base; lo que sobresale de su plano es solo lo que el busto pasa de la
     * holgura de la prenda. Antes la cúpula de la tela se inflaba entera y una
     * prenda Oversize sumaba su holgura al busto.
     */
    private static Forma[] formas(Busto b, float inflado, float carpa, boolean rigido, boolean piel) {
        float t = b.talle() * b.sujecion();
        if (t <= 0.05f) return new Forma[0];
        float dy = 0f, dx = 0f;
        if (b.rebote() != null && !rigido) {
            dy = b.rebote().dy() * 12f;
            dx = b.rebote().dx() * 8f;
        }
        float cuerpo = 0.55f + 0.5f * t;                      // lo que sale el busto de la piel
        float hondo;
        float abrir = Math.max(0f, inflado);
        if (piel) {
            // La punta queda en su lugar aunque el plano salga (la piel por delante de la
            // 2.ª capa plana); con inflado negativo se achica por dentro de la tela.
            hondo = cuerpo - Math.max(0f, inflado);
            abrir = 0f;
        } else {
            float punta = Math.max(cuerpo + 0.06f + 0.12f * abrir, inflado);
            hondo = Math.max(0f, punta - inflado);             // por delante del plano de la tela
        }
        float rx = Math.min(2.6f, 1.95f + 0.07f * t) + 0.5f * abrir;
        float rArriba = 2.3f + 0.16f * t + 0.5f * abrir;
        float rAbajo = 1.3f + 0.22f * t + abrir + carpa;
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
     * Desde qué carpa la tela holgada ya no marca dos cúpulas sino un solo
     * manto (2026-10-02, "se marca demasiado en el hoodie no se deberian ver
     * asi como dos tetas"): Suelto y Oversize. Normal sigue marcando las dos.
     */
    private static final float CARPA_MANTO = 1f;
    /** Celdas por px de la grilla del manto. */
    private static final int PASO = 2;

    /** La carpa de la tela de cada calce: cuánto más abajo cae debajo del busto (px). */
    public static float carpaDe(@Nullable com.femclothes.item.Calce calce) {
        if (calce == null) return 0f;
        return switch (calce) {
            case NORMAL -> 0.4f;
            case SUELTO -> 1.5f;
            case OVERSIZE -> 2.5f;
            default -> 0f;
        };
    }

    /**
     * Cuánto sale la tela por fuera del cuerpo en cada fila del torso (la
     * dilatación de la pieza, con su caída hacia el ruedo): el manto se apoya
     * en esa superficie y termina en el ruedo de la pieza.
     */
    public record Tela(float[] fila, int desde, int hasta) {
        float en(float y) {
            if (hasta - desde <= 1) return fila[Math.max(0, desde)];
            float yy = Math.max(desde + 0.5f, Math.min(hasta - 0.5f, y)) - 0.5f;
            int f0 = (int) Math.floor(yy), f1 = Math.min(f0 + 1, hasta - 1);
            float k = yy - f0;
            return fila[f0] + (fila[f1] - fila[f0]) * k;
        }

        static Tela uniforme(float inflado) {
            float[] f = new float[12];
            java.util.Arrays.fill(f, inflado);
            return new Tela(f, 0, 12);
        }
    }

    /**
     * Un vértice de la malla: posición, normal, de qué punto del frente sale
     * la textura ({@code ux, uy}) y qué punto del frente PLANO tiene debajo
     * ({@code bx, by}: con eso se apoyan y se apuntan los apliques).
     */
    private record Vert(Vector3f pos, Vector3f normal, float ux, float uy, float bx, float by) {}

    /**
     * La malla del busto (cuadriláteros), según el estilo y la tela:
     * <ul>
     *   <li>cajas, si el busto es cuadrado (2026-10-02, "agreguemos un
     *       selector de tetas cuadradas en el selector de skin");</li>
     *   <li>un manto, si es una tela holgada (carpa ≥ {@link #CARPA_MANTO});</li>
     *   <li>si no, las dos cúpulas.</li>
     * </ul>
     */
    private static List<Vert[]> malla(Busto b, float inflado, float carpa, boolean rigido, boolean piel,
                                      @Nullable Tela tela) {
        if (b.cuadrado()) return cajas(b, inflado, carpa, rigido, piel);
        Forma[] fs = formas(b, inflado, piel || carpa < CARPA_MANTO ? carpa : 0f, rigido, piel);
        if (fs.length == 0) return List.of();
        if (!piel && carpa >= CARPA_MANTO) return manto(fs, inflado, carpa, tela == null ? Tela.uniforme(inflado) : tela);
        List<Vert[]> q = new ArrayList<>();
        for (Forma f : fs) cupula(f, q);
        return q;
    }

    /**
     * La piel y la ropa del mod (layout de la skin).
     *
     * @param inflado  cuánto sale la tela por fuera del cuerpo en el pecho (px; 0 = la piel)
     * @param carpa    cuánto más abajo cae la tela holgada debajo del busto (px)
     * @param filaSkin fila de la skin donde empieza el frente del torso: 20 (la
     *                 piel y todas las telas) o 36 (la segunda capa de la skin)
     * @param tela     la dilatación de la pieza por fila (para el manto); null = uniforme
     */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov,
                               float inflado, float carpa, float filaSkin, boolean piel, @Nullable Tela tela) {
        dibujar(b, matrices, vc, luz, ov, 0xFFFFFFFF, Uv.skin(filaSkin), inflado, carpa, false, piel, tela);
    }

    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov,
                               float inflado, float carpa, float filaSkin, boolean piel) {
        dibujar(b, matrices, vc, luz, ov, inflado, carpa, filaSkin, piel, null);
    }

    /** Con cualquier mapeo de textura, color y rigidez (modelos de otros mods y armaduras: tela, no piel). */
    public static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov, int color,
                               Uv uv, float inflado, float carpa, boolean rigido) {
        dibujar(b, matrices, vc, luz, ov, color, uv, inflado, carpa, rigido, false, null);
    }

    private static void dibujar(Busto b, MatrixStack matrices, VertexConsumer vc, int luz, int ov, int color,
                                Uv uv, float inflado, float carpa, boolean rigido, boolean piel, @Nullable Tela tela) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        Matrix3f n = matrices.peek().getNormalMatrix();
        for (Vert[] q : malla(b, inflado, carpa, rigido, piel, tela)) {
            for (Vert v : q) vertice(m, n, vc, luz, ov, color, v.pos(), uv.u(v.ux()), uv.v(v.uy()), v.normal());
        }
    }

    // ── cúpulas ──────────────────────────────────────────────────────────

    private static void cupula(Forma f, List<Vert[]> salida) {
        Vert[][] g = new Vert[ANILLOS + 1][GAJOS + 1];
        for (int i = 0; i <= ANILLOS; i++) {
            float r = i / (float) ANILLOS;
            for (int j = 0; j <= GAJOS; j++) {
                double a = Math.PI * 2 * j / GAJOS;
                float[] frente = f.vistoDeFrente(r, a);
                g[i][j] = new Vert(f.punto(r, a), f.normal(r, a), frente[0], frente[1], f.baseX(r, a), f.baseY(r, a));
            }
        }
        for (int i = 0; i < ANILLOS; i++) {
            for (int j = 0; j < GAJOS; j++) {
                salida.add(new Vert[]{g[i][j], g[i + 1][j], g[i + 1][j + 1], g[i][j + 1]});
            }
        }
    }

    // ── manto ────────────────────────────────────────────────────────────

    /**
     * Una tela holgada sobre el busto (2026-10-02, "se marca demasiado en el
     * hoodie no se deberian ver asi como dos tetas"): la tela no entra entre
     * los pechos ni los envuelve uno por uno; queda tirante de una punta a la
     * otra y cae derecho desde las puntas hasta el ruedo, como un estante.
     *
     * <p>Un campo de alturas sobre el frente del torso: se calcan las dos
     * cúpulas (con su caída y su rebote) y se tensa la tela por filas y por
     * columnas (envolvente cóncava, {@link #tensar}), con los bordes pegados a
     * la superficie de la pieza ({@link Tela}). La textura va proyectada de
     * frente: el estampado de la prenda no se corre.
     */
    private static List<Vert[]> manto(Forma[] fs, float inflado, float carpa, Tela tela) {
        float y0 = tela.desde(), y1 = tela.hasta();
        int nx = 8 * PASO, ny = Math.round((y1 - y0) * PASO);
        if (ny < 2) return List.of();
        float[][] h = new float[nx + 1][ny + 1];
        int anillos = ANILLOS * 4, gajos = GAJOS * 3;
        for (Forma f : fs) {
            for (int i = 0; i <= anillos; i++) {
                float r = i / (float) anillos;
                for (int j = 0; j < gajos; j++) {
                    Vector3f p = f.punto(r, Math.PI * 2 * j / gajos);
                    int ix = Math.round((p.x + 4f) * PASO), iy = Math.round((p.y - y0) * PASO);
                    if (ix < 0 || ix > nx || iy < 0 || iy > ny) continue;
                    // Lo que sale por delante de la superficie de la pieza en esa fila.
                    float sale = (f.plano() - p.z) - (tela.en(p.y) - inflado);
                    if (sale > h[ix][iy]) h[ix][iy] = sale;
                }
            }
        }
        // Bordes pegados a la pieza: así no quedan rendijas con los costados ni con el ruedo.
        for (int ix = 0; ix <= nx; ix++) { h[ix][0] = 0f; h[ix][ny] = 0f; }
        for (int iy = 0; iy <= ny; iy++) { h[0][iy] = 0f; h[nx][iy] = 0f; }
        float[] fila = new float[nx + 1];
        for (int iy = 0; iy <= ny; iy++) {
            for (int ix = 0; ix <= nx; ix++) fila[ix] = h[ix][iy];
            tensar(fila);
            for (int ix = 0; ix <= nx; ix++) h[ix][iy] = fila[ix];
        }
        for (int ix = 0; ix <= nx; ix++) tensar(h[ix]);
        // Cuanto más holgada, más chato: la tela sobrante se lo come (Suelto ~0.88, Oversize ~0.65).
        float chato = 1f - 0.35f * Math.max(0f, Math.min(1f, (carpa - CARPA_MANTO) / 1.5f));

        Vert[][] g = new Vert[nx + 1][ny + 1];
        float[][] z = new float[nx + 1][ny + 1];
        for (int ix = 0; ix <= nx; ix++) {
            for (int iy = 0; iy <= ny; iy++) {
                h[ix][iy] *= chato;
                z[ix][iy] = -2f - tela.en(y0 + iy / (float) PASO) - h[ix][iy];
            }
        }
        for (int ix = 0; ix <= nx; ix++) {
            for (int iy = 0; iy <= ny; iy++) {
                float x = -4f + ix / (float) PASO, y = y0 + iy / (float) PASO;
                int a = Math.max(0, ix - 1), c = Math.min(nx, ix + 1);
                int d = Math.max(0, iy - 1), e = Math.min(ny, iy + 1);
                float fx = (z[c][iy] - z[a][iy]) / ((c - a) / (float) PASO);
                float fy = (z[ix][e] - z[ix][d]) / ((e - d) / (float) PASO);
                Vector3f nor = new Vector3f(fx, fy, -1f).normalize();
                g[ix][iy] = new Vert(new Vector3f(x, y, z[ix][iy]), nor, x, y, x, y);
            }
        }
        List<Vert[]> q = new ArrayList<>();
        for (int ix = 0; ix < nx; ix++) {
            for (int iy = 0; iy < ny; iy++) {
                // Lo que queda pegado a la pieza ya lo dibuja la pieza (y así no pelean en Z).
                if (h[ix][iy] <= 1e-4f && h[ix + 1][iy] <= 1e-4f && h[ix + 1][iy + 1] <= 1e-4f && h[ix][iy + 1] <= 1e-4f) continue;
                q.add(new Vert[]{g[ix][iy], g[ix + 1][iy], g[ix + 1][iy + 1], g[ix][iy + 1]});
            }
        }
        return q;
    }

    /** La envolvente cóncava de arriba de {@code h} (puntos equiespaciados): la tela tirante. */
    private static void tensar(float[] h) {
        int n = h.length;
        int[] pila = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k >= 2) {
                int a = pila[k - 2], b = pila[k - 1];
                // b queda por debajo (o sobre) la recta a→i: sobra.
                if ((h[b] - h[a]) * (i - a) <= (h[i] - h[a]) * (b - a)) k--;
                else break;
            }
            pila[k++] = i;
        }
        for (int s = 0; s + 1 < k; s++) {
            int a = pila[s], b = pila[s + 1];
            for (int i = a + 1; i < b; i++) h[i] = h[a] + (h[b] - h[a]) * (i - a) / (float) (b - a);
        }
    }

    // ── cajas ────────────────────────────────────────────────────────────

    /**
     * El busto cuadrado (2026-10-02, "agreguemos un selector de tetas
     * cuadradas en el selector de skin"): una caja por pecho, de 4 px de ancho
     * (la mitad del torso), que sale del pecho y se inclina hacia abajo con la
     * caída, al estilo cúbico de Minecraft. Mismo talle, sujeción y rebote que
     * las cúpulas. Las telas la envuelven apenas por fuera; las holgadas
     * (carpa ≥ {@link #CARPA_MANTO}) en una sola caja que cae en carpa hasta
     * el pecho. La textura va proyectada de frente: el frente lleva el pecho,
     * la tapa y los costados estiran la fila o la columna del borde.
     */
    private static List<Vert[]> cajas(Busto b, float inflado, float carpa, boolean rigido, boolean piel) {
        float t = b.talle() * b.sujecion();
        if (t <= 0.05f) return List.of();
        float cuerpo = 0.55f + 0.5f * t;
        float abrir = Math.max(0f, inflado);
        float hondo;
        if (piel) {
            hondo = cuerpo - Math.max(0f, inflado);
            abrir = 0f;
        } else {
            hondo = Math.max(0f, Math.max(cuerpo + 0.06f + 0.12f * abrir, inflado) - inflado);
        }
        if (hondo <= 0.01f) return List.of();
        float dy = 0f, dx = 0f;
        if (b.rebote() != null && !rigido) {
            dy = b.rebote().dy() * 12f * 0.6f;
            dx = b.rebote().dx() * 8f;
        }
        float plano = -2f - inflado;
        float arriba = ARRIBA + 1.2f - 0.25f * abrir;
        float abajo = ARRIBA + 1.2f + 2.6f + 0.3f * t + 0.3f * abrir;
        // Inclinación: el frente baja lo que sale × la caída (más firme en armaduras).
        float caida = hondo * (0.18f + 0.03f * t) * (0.6f + 0.4f * b.sujecion()) * (rigido ? 0.3f : 1f);
        List<Vert[]> q = new ArrayList<>();
        if (!piel && carpa >= CARPA_MANTO) {
            float chato = 1f - 0.35f * Math.max(0f, Math.min(1f, (carpa - CARPA_MANTO) / 1.5f));
            caja(q, -4f, 4f, arriba, abajo, plano, hondo * chato, caida * chato, 0f, dy, carpa * 1.5f);
        } else {
            float colgar = piel ? 0f : carpa;
            caja(q, -4f, 0f, arriba, abajo, plano, hondo, caida, dx, dy, colgar);
            caja(q, 0f, 4f, arriba, abajo, plano, hondo, caida, dx, dy, colgar);
        }
        return q;
    }

    /**
     * Una caja de x0..x1, y0..y1 sobre el pecho: la cara de atrás en el plano
     * (no se dibuja), el frente {@code hondo} por delante, corrido hacia
     * abajo {@code caida} y el rebote; {@code colgar} baja la arista de atrás
     * de abajo (la tela cae en carpa hasta el pecho).
     */
    private static void caja(List<Vert[]> q, float x0, float x1, float y0, float y1, float plano, float hondo,
                             float caida, float dx, float dy, float colgar) {
        float zf = plano - hondo;
        // Atrás: arriba-izq, arriba-der, abajo-der, abajo-izq. Frente: lo mismo, corrido.
        Vector3f a0 = new Vector3f(x0, y0, plano), a1 = new Vector3f(x1, y0, plano);
        Vector3f a2 = new Vector3f(x1, y1 + colgar, plano), a3 = new Vector3f(x0, y1 + colgar, plano);
        Vector3f f0 = new Vector3f(x0 + dx, y0 + caida + dy, zf), f1 = new Vector3f(x1 + dx, y0 + caida + dy, zf);
        Vector3f f2 = new Vector3f(x1 + dx, y1 + caida + dy, zf), f3 = new Vector3f(x0 + dx, y1 + caida + dy, zf);
        // Textura de frente sin el rebote; debajo, el rectángulo plano.
        float[][] uvA = {{x0, y0}, {x1, y0}, {x1, y1 + colgar}, {x0, y1 + colgar}};
        float[][] uvF = {{x0, y0 + caida}, {x1, y0 + caida}, {x1, y1 + caida}, {x0, y1 + caida}};
        float[][] base = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
        Vector3f[] at = {a0, a1, a2, a3}, fr = {f0, f1, f2, f3};
        // Frente.
        cara(q, new Vector3f(0, 0, -1), new int[][]{{1, 0}, {1, 1}, {1, 2}, {1, 3}}, at, fr, uvA, uvF, base);
        // Tapa de arriba, de abajo y los dos costados.
        cara(q, new Vector3f(0, -1, 0), new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 0}}, at, fr, uvA, uvF, base);
        cara(q, new Vector3f(0, 1, 0), new int[][]{{0, 3}, {0, 2}, {1, 2}, {1, 3}}, at, fr, uvA, uvF, base);
        cara(q, new Vector3f(-1, 0, 0), new int[][]{{0, 0}, {0, 3}, {1, 3}, {1, 0}}, at, fr, uvA, uvF, base);
        cara(q, new Vector3f(1, 0, 0), new int[][]{{0, 1}, {0, 2}, {1, 2}, {1, 1}}, at, fr, uvA, uvF, base);
    }

    /**
     * Una cara de la caja: {@code idx} = {0 atrás | 1 frente, esquina}. La
     * normal sale de los vértices (orientada hacia {@code afuera}), y el orden
     * se da vuelta si hace falta para que gire igual que las cúpulas.
     */
    private static void cara(List<Vert[]> q, Vector3f afuera, int[][] idx, Vector3f[] at, Vector3f[] fr,
                             float[][] uvA, float[][] uvF, float[][] base) {
        Vert[] v = new Vert[4];
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++) p[i] = idx[i][0] == 0 ? at[idx[i][1]] : fr[idx[i][1]];
        Vector3f nor = new Vector3f(p[2]).sub(p[0]).cross(new Vector3f(p[3]).sub(p[1]));
        if (nor.lengthSquared() < 1e-10f) return;                   // cara aplastada (sin hondo)
        boolean alReves = nor.dot(afuera) > 0;
        if (nor.dot(afuera) < 0) nor.negate();
        nor.normalize();
        for (int i = 0; i < 4; i++) {
            float[] uv = idx[i][0] == 0 ? uvA[idx[i][1]] : uvF[idx[i][1]];
            float[] b = base[idx[i][1]];
            v[i] = new Vert(p[i], nor, uv[0], uv[1], b[0], b[1]);
        }
        // Las cúpulas giran con cruz(lado1, lado2) hacia ADENTRO.
        q.add(alReves ? new Vert[]{v[3], v[2], v[1], v[0]} : v);
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
     * aplique se guarda en el frente plano y acá se apoya en el busto (y
     * rebota con él). Null si el punto no cae sobre el busto — o si no hay
     * busto: la prenda sigue sirviendo igual sin él.
     */
    @Nullable
    public static Punto sobreBusto(Busto b, float x, float y, float inflado) {
        return sobreBusto(b, x, y, inflado, 0f);
    }

    /** Con la carpa de la tela (las holgadas son un manto, ver {@link #carpaDe}). */
    @Nullable
    public static Punto sobreBusto(Busto b, float x, float y, float inflado, float carpa) {
        Punto mejor = null;
        for (Vert[] q : malla(b, inflado, carpa, false, false, null)) {
            for (int[] tri : TRIANGULOS) {
                Vert a = q[tri[0]], c = q[tri[1]], d = q[tri[2]];
                // Baricéntricas del punto en el triángulo del frente plano (las caras de costado se aplastan: se saltean).
                float e1x = c.bx() - a.bx(), e1y = c.by() - a.by(), e2x = d.bx() - a.bx(), e2y = d.by() - a.by();
                float det = e1x * e2y - e2x * e1y;
                if (Math.abs(det) < 1e-6f) continue;
                float px = x - a.bx(), py = y - a.by();
                float w1 = (px * e2y - e2x * py) / det, w2 = (e1x * py - px * e1y) / det;
                if (w1 < -1e-4f || w2 < -1e-4f || w1 + w2 > 1f + 1e-4f) continue;
                float w0 = 1f - w1 - w2;
                Vector3f pos = new Vector3f(a.pos()).mul(w0).add(new Vector3f(c.pos()).mul(w1)).add(new Vector3f(d.pos()).mul(w2));
                if (mejor != null && pos.z >= mejor.pos().z) continue;     // el más de adelante
                Vector3f nor = new Vector3f(a.normal()).mul(w0).add(new Vector3f(c.normal()).mul(w1))
                        .add(new Vector3f(d.normal()).mul(w2));
                if (nor.lengthSquared() < 1e-10f) nor.set(0, 0, -1);
                mejor = new Punto(pos, nor.normalize());
            }
        }
        return mejor;
    }

    private static final int[][] TRIANGULOS = {{0, 1, 2}, {0, 2, 3}};

    /**
     * Un rayo contra el busto, en px del torso (2026-10-02, "se puede las
     * dos? cosa que si armo la prenda sin pechos despues siga sirviendo?"):
     * devuelve {t, x, y} del choque más cercano al origen (t en el largo de
     * {@code dir}), con (x, y) el punto del frente PLANO que está debajo — eso
     * es lo que se guarda en el aplique. Null si no le pega.
     */
    @Nullable
    public static float[] rayo(Busto b, Vector3f origen, Vector3f dir, float inflado) {
        return rayo(b, origen, dir, inflado, 0f);
    }

    /** Con la carpa de la tela (las holgadas son un manto, ver {@link #carpaDe}). */
    @Nullable
    public static float[] rayo(Busto b, Vector3f origen, Vector3f dir, float inflado, float carpa) {
        float[] mejor = null;
        for (Vert[] q : malla(b, inflado, carpa, false, false, null)) {
            for (int[] tri : TRIANGULOS) {
                Vert a = q[tri[0]], c = q[tri[1]], d = q[tri[2]];
                float[] k = triangulo(origen, dir, a.pos(), c.pos(), d.pos());
                if (k == null || (mejor != null && k[0] >= mejor[0])) continue;
                float w1 = k[1], w2 = k[2], w0 = 1f - w1 - w2;
                mejor = new float[]{k[0], w0 * a.bx() + w1 * c.bx() + w2 * d.bx(), w0 * a.by() + w1 * c.by() + w2 * d.by()};
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
