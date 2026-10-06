package com.femclothes.render;

import com.femclothes.Femclothes;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.sublimadora.EstampaTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Acabados de tela con un trim (2026-10-06, "los trims puedan agregar a las prendas texturas animadas, brillo,
 * policromatismo, reflejos especiales, relacionados quizas a la estructura en q se genera" + "los trims tienen q ser
 * epicos"): el patrón del trim elige un efecto y la tela se dibuja DOS VECES, la de siempre y una capa emisiva
 * (fullbright, aditiva) animada que sigue la silueta de la prenda.
 *
 * <p>Cómo: de la textura de la tela se saca una máscara (qué texeles son tela, a 256×256) y de ella se arman
 * {@link #CUADROS} cuadros del efecto, que se generan de a uno por dibujo para no trabar el juego. El efecto se
 * anima pasando de un cuadro al siguiente con un fundido (el alfa de cada cuadro sube y baja), así que ocho cuadros se
 * ven continuos. Todos los efectos son periódicos en el tiempo y en la textura: el bucle no tiene corte.
 */
public final class EfectoTrim {

    private EfectoTrim() {}

    /**
     * Un efecto por patrón de trim (2026-10-06, "18"): cada uno de los 18 de vanilla tiene el suyo; los de otros mods
     * caen en {@link #METAL}. Agregar valores nuevos al final.
     */
    public enum Tipo {
        SENTRY, DUNE, COAST, WILD, WARD, EYE, VEX, TIDE, SNOUT, RIB, SPIRE, WAYFINDER, SHAPER, SILENCE, RAISER, HOST,
        FLOW, BOLT, METAL
    }

    /** El efecto de un patrón de trim (por el nombre del patrón; los desconocidos, de otros mods, son metálicos). */
    public static Tipo tipoDe(Identifier patron) {
        for (Tipo t : Tipo.values()) if (t.name().equalsIgnoreCase(patron.getPath())) return t;
        return Tipo.METAL;
    }

    /** El color base (RGB) de un material de trim de vanilla, o -1 si no se conoce (otro mod). */
    public static int colorDeMaterial(@Nullable Identifier material) {
        if (material == null) return -1;
        return switch (material.getPath()) {
            case "quartz" -> 0xE3D4C4;
            case "iron" -> 0xECECEC;
            case "netherite" -> 0x625859;
            case "redstone" -> 0x971607;
            case "copper" -> 0xB4684D;
            case "gold" -> 0xDEB12D;
            case "emerald" -> 0x11A036;
            case "diamond" -> 0x6EECD2;
            case "lapis" -> 0x416E97;
            case "amethyst" -> 0x9A5CC6;
            default -> -1;
        };
    }

    /**
     * Cuántos cuadros tiene el bucle, su duración y cada cuánto se genera un cuadro. 2026-10-06, "puede tener mas
     * definicion el efecto y mas fps": 24 cuadros en 2,4 s (10 por segundo) y el efecto se calcula a la resolución
     * de la propia tela (antes, a 256² y estirado).
     */
    private static final int CUADROS = 24, PERIODO_MS = 2400, ESPERA_MS = 20;

    /**
     * Lo que se dibuja con el acabado de {@code prenda}: el proveedor de siempre si no lleva ninguno.
     *
     * <p>2026-10-06, "se ve el tooltip pero no el efecto": la segunda capa emisiva (vértices anotados y repetidos
     * después) no llegaba a verse en el mundo aunque sí en la vista previa. Ahora el efecto se mezcla EN la textura de
     * la tela (un cuadro por vez, con la silueta y el dibujo de la tela de verdad) y la prenda se dibuja una sola vez,
     * con esa textura y a máxima luz: usa el mismo camino que la tela sin acabado, que sí se ve en el mundo.
     */
    public static VertexConsumerProvider envolver(@Nullable ItemStack prenda, VertexConsumerProvider real) {
        if (prenda == null) return real;
        Identifier patron = prenda.get(FemclothesComponents.ACABADO_TRIM);
        if (patron == null) return real;
        return new Proveedor(real, tipoDe(patron), colorDeMaterial(prenda.get(FemclothesComponents.ACABADO_MATERIAL)));
    }

    private static final class Proveedor implements VertexConsumerProvider {
        private final VertexConsumerProvider real;
        private final Tipo tipo;
        private final int material;

        Proveedor(VertexConsumerProvider real, Tipo tipo, int material) {
            this.real = real;
            this.tipo = tipo;
            this.material = material;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer capa) {
            Identifier base = ClothingTextureCache.ultimaTextura;
            ClothingTextureCache.ultimaTextura = null;   // vale para este pedido solamente
            if (base == null) return real.getBuffer(capa);
            Identifier cuadro = cuadroActual(tipo, material, base);
            if (cuadro == null) return real.getBuffer(capa);   // todavía no hay ninguno: se ve la tela sola
            RenderLayer nueva = ClothingTextureCache.esTranslucida(base)
                    ? RenderLayer.getEntityTranslucent(cuadro)
                    : RenderLayer.getArmorCutoutNoCull(cuadro);
            return new Brillante(real.getBuffer(nueva));
        }
    }

    /** Todo igual, pero con la luz al máximo: el acabado brilla por sí solo. */
    private static final class Brillante implements VertexConsumer {
        private final VertexConsumer vc;

        Brillante(VertexConsumer vc) { this.vc = vc; }

        @Override public VertexConsumer vertex(float x, float y, float z) { vc.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { vc.color(r, g, b, a); return this; }
        @Override public VertexConsumer texture(float u, float v) { vc.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { vc.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { vc.light(0xF0, 0xF0); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { vc.normal(x, y, z); return this; }
    }

    // ── los cuadros de cada tela ─────────────────────────────────────────────

    private static final class Cuadros {
        NativeImage base;                        // la tela tal cual; se suelta cuando están todos los cuadros
        final Tipo tipo;
        final int[][] paleta;
        final Identifier[] ids = new Identifier[CUADROS];
        int hechos = 0;

        Cuadros(Tipo tipo, int[][] paleta, NativeImage base) { this.tipo = tipo; this.paleta = paleta; this.base = base; }
    }

    private static final int MAXIMO_EN_CACHE = 4;
    private static final Map<String, Cuadros> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Long> REINTENTO = new LinkedHashMap<>();
    private static int numero = 0;
    private static long ultimaGeneracion = 0;

    /** El cuadro del efecto que toca ahora (por el reloj real) sobre esa tela, o null si todavía no hay ninguno. */
    @Nullable
    private static Identifier cuadroActual(Tipo tipo, int material, Identifier base) {
        String clave = tipo.name() + "|" + Integer.toHexString(material) + "|" + base;
        long ahora = System.currentTimeMillis();
        Cuadros c = CACHE.get(clave);
        if (c == null) {
            Long luego = REINTENTO.get(clave);
            if (luego != null && ahora < luego) return null;
            NativeImage img = EstampaTextures.leerTextura(base);
            if (img == null) {
                REINTENTO.put(clave, ahora + 500);
                return null;
            }
            c = new Cuadros(tipo, paletaDe(tipo, material), img);
            CACHE.put(clave, c);
            if (CACHE.size() > MAXIMO_EN_CACHE) {
                var it = CACHE.entrySet().iterator();
                Cuadros viejo = it.next().getValue();
                it.remove();
                liberar(viejo);
            }
        }
        if (c.hechos < CUADROS && ahora - ultimaGeneracion >= ESPERA_MS) {   // de a uno por vez
            ultimaGeneracion = ahora;
            generarSiguiente(c);
        }
        if (c.hechos == 0) return null;
        int a = (int) ((ahora % PERIODO_MS) / (float) PERIODO_MS * CUADROS);
        return c.ids[Math.min(a, c.hechos - 1)];
    }

    private static void liberar(Cuadros c) {
        var tm = MinecraftClient.getInstance().getTextureManager();
        for (int i = 0; i < c.hechos; i++) tm.destroyTexture(c.ids[i]);
        if (c.base != null) { c.base.close(); c.base = null; }
    }

    /** La tela con el efecto del cuadro {@code c.hechos} mezclado encima (solo donde hay tela, a la resolución de la tela). */
    private static void generarSiguiente(Cuadros c) {
        int i = c.hechos;
        float t = i / (float) CUADROS;
        int w = c.base.getWidth(), h = c.base.getHeight();
        NativeImage img = new NativeImage(w, h, true);
        float[] o = new float[3];
        for (int y = 0; y < h; y++) {
            float v = (y + 0.5f) / h;
            for (int x = 0; x < w; x++) {
                int px = c.base.getColor(x, y);
                int alfa = px >>> 24;
                if (alfa > 0) {
                    int e = pixel(c.tipo, c.paleta, (x + 0.5f) / w, v, t, i, o);
                    int ea = e >>> 24;
                    int r = ((px & 0xFF) * (255 - ea) + (e & 0xFF) * ea) / 255;
                    int g = (((px >> 8) & 0xFF) * (255 - ea) + ((e >> 8) & 0xFF) * ea) / 255;
                    int b = (((px >> 16) & 0xFF) * (255 - ea) + ((e >> 16) & 0xFF) * ea) / 255;
                    px = (alfa << 24) | (b << 16) | (g << 8) | r;
                }
                img.setColor(x, y, px);
            }
        }
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);   // se sube al crearla
        tex.setImage(null);   // ya está en la placa: la copia en memoria sobra (24 cuadros por tela pesan)
        c.ids[i] = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(Femclothes.MOD_ID + "_trim_" + (numero++), tex);
        c.hechos = i + 1;
        if (c.hechos == CUADROS && c.base != null) { c.base.close(); c.base = null; }   // ya no hace falta
    }

    // ── paletas ───────────────────────────────────────────────────────────────

    /** La rampa de color (oscuro, medio, claro) de cada efecto, en RGB. */
    private static int[][] paletaDeFabrica(Tipo tipo) {
        return switch (tipo) {
            case SENTRY -> rampa(50, 60, 80, 140, 160, 190, 235, 245, 255);
            case DUNE -> rampa(150, 105, 45, 225, 180, 95, 255, 240, 185);
            case COAST -> rampa(15, 85, 120, 50, 165, 185, 235, 255, 255);
            case WILD -> rampa(15, 65, 25, 55, 150, 45, 190, 255, 130);
            case WARD -> rampa(10, 60, 70, 30, 170, 185, 120, 255, 245);
            case EYE -> rampa(60, 20, 110, 130, 60, 200, 230, 170, 255);
            case VEX -> rampa(120, 170, 200, 180, 220, 245, 240, 252, 255);
            case TIDE -> rampa(5, 40, 90, 20, 110, 170, 120, 215, 235);
            case SNOUT -> rampa(50, 5, 0, 230, 80, 10, 255, 235, 130);
            case RIB -> rampa(45, 5, 5, 200, 40, 15, 255, 170, 60);
            case SPIRE -> rampa(40, 190, 220, 130, 70, 210, 255, 120, 230);   // cian → violeta → magenta: iridiscente
            case WAYFINDER -> rampa(90, 70, 30, 190, 155, 70, 255, 240, 170);
            case SHAPER -> rampa(80, 40, 25, 200, 110, 60, 255, 215, 170);
            case SILENCE -> rampa(3, 8, 16, 15, 45, 60, 120, 230, 240);
            case RAISER -> rampa(80, 25, 10, 230, 120, 30, 255, 220, 120);
            case HOST -> rampa(70, 40, 20, 215, 120, 50, 255, 225, 160);
            case FLOW -> rampa(110, 170, 200, 200, 235, 250, 255, 255, 255);
            case BOLT -> rampa(170, 90, 50, 110, 210, 190, 255, 255, 255);
            default -> rampa(120, 130, 150, 220, 230, 245, 255, 255, 255);
        };
    }

    private static int[][] rampa(int... v) {
        return new int[][]{{v[0], v[1], v[2]}, {v[3], v[4], v[5]}, {v[6], v[7], v[8]}};
    }

    /** Con material (2026-10-06, "el material por separado, opcional"): la rampa sale del color del material. */
    private static int[][] paletaDe(Tipo tipo, int material) {
        if (material < 0) return paletaDeFabrica(tipo);
        int r = (material >> 16) & 0xFF, g = (material >> 8) & 0xFF, b = material & 0xFF;
        int[][] p = {
                {Math.round(r * 0.35f), Math.round(g * 0.35f), Math.round(b * 0.35f)},
                {r, g, b},
                {Math.round(r + (255 - r) * 0.6f), Math.round(g + (255 - g) * 0.6f), Math.round(b + (255 - b) * 0.6f)}};
        // La oscuridad del Silencio sigue siendo oscura: solo se tiñe.
        if (tipo == Tipo.SILENCE) {
            p[0] = new int[]{3, 6, 12};
            p[1] = new int[]{Math.round(r * 0.18f), Math.round(g * 0.18f), Math.round(b * 0.18f)};
        }
        return p;
    }

    // ── los efectos (todos periódicos en u, v y t) ──────────────────────────

    private static final float TAU = (float) (Math.PI * 2);

    private static float sen(float x) { return (float) Math.sin(x); }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static float lim(float x) { return Math.max(0f, Math.min(1f, x)); }

    private static float suave(float h) { h = lim(h); return h * h * (3 - 2 * h); }

    private static float azar(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Ruido de valores suave, periódico: {@code n} celdas por vuelta en u y en v. */
    private static float ruido(float u, float v, int n, int semilla) {
        float x = frac(u) * n, y = frac(v) * n;
        int x0 = (int) x, y0 = (int) y;
        float fx = suave(x - x0), fy = suave(y - y0);
        int x1 = (x0 + 1) % n, y1 = (y0 + 1) % n;
        float a = azar(x0 + semilla * 31, y0), b = azar(x1 + semilla * 31, y0),
                c = azar(x0 + semilla * 31, y1), d = azar(x1 + semilla * 31, y1);
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    /**
     * Voronoi periódico de {@code n} celdas por vuelta: deja en {@code o} la distancia al punto más cercano, al segundo
     * más cercano y el id de la celda del más cercano.
     */
    private static void voronoi(float u, float v, int n, int semilla, float[] o) {
        float x = frac(u) * n, y = frac(v) * n;
        int cx = (int) x, cy = (int) y;
        float d1 = 9f, d2 = 9f;
        int id = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int gx = cx + dx, gy = cy + dy;
                int wx = Math.floorMod(gx, n), wy = Math.floorMod(gy, n);
                float px = gx + 0.15f + 0.7f * azar(wx + semilla * 13, wy);
                float py = gy + 0.15f + 0.7f * azar(wx, wy + semilla * 7 + 101);
                float d = (float) Math.sqrt((px - x) * (px - x) + (py - y) * (py - y));
                if (d < d1) { d2 = d1; d1 = d; id = wx * 131 + wy; }
                else if (d < d2) d2 = d;
            }
        }
        o[0] = d1;
        o[1] = d2;
        o[2] = id;
    }

    private static int mezclar(int[] a, int[] b, float k, int canal) {
        return Math.round(a[canal] + (b[canal] - a[canal]) * k);
    }

    /** ABGR con alfa: la rampa según {@code h}, aclarada hacia el blanco según {@code brillo}. */
    private static int color(int[][] p, float h, float brillo, float alfa) {
        h = lim(h);
        int[] c = new int[3];
        for (int k = 0; k < 3; k++) {
            c[k] = h < 0.5f ? mezclar(p[0], p[1], h * 2f, k) : mezclar(p[1], p[2], (h - 0.5f) * 2f, k);
            c[k] = Math.round(c[k] + (255 - c[k]) * lim(brillo));
        }
        int ai = Math.max(0, Math.min(255, Math.round(255 * lim(alfa))));
        return (ai << 24) | (Math.min(255, c[2]) << 16) | (Math.min(255, c[1]) << 8) | Math.min(255, c[0]);
    }

    /** Un punto de un cuadro; {@code o} es un arreglo de trabajo ({@code h, alfa, brillo}). */
    private static int pixel(Tipo tipo, int[][] p, float u, float v, float t, int cuadro, float[] o) {
        float h = 0f, alfa = 0f, brillo = 0f;
        switch (tipo) {
            case SENTRY -> {   // un barrido de acero frío en diagonal sobre líneas finas de escaneo
                float banda = Math.max(0f, 1f - Math.abs(frac(u + v - t) - 0.5f) * 8f);
                float linea = 0.5f + 0.5f * sen(TAU * v * 48);
                h = banda * banda * 0.8f + 0.18f * linea;
                alfa = 0.22f + 0.6f * banda;
                brillo = banda * banda * 0.55f;
            }
            case DUNE -> {   // ondas de arena empujadas por el viento y granos que centellean
                float onda = 0.5f + 0.5f * sen(TAU * (4 * v + 2 * ruido(u, v, 4, 3) + t));
                int cx = (int) (u * 40), cy = (int) (v * 40);
                float e = azar(cx + 5, cy + 9);
                float grano = e > 0.9f ? Math.max(0f, sen(TAU * (t + e * 7))) : 0f;
                h = 0.25f + 0.6f * onda;
                alfa = 0.5f + 0.15f * onda + 0.3f * grano;
                brillo = grano * 0.8f;
            }
            case COAST -> {   // olas que suben y rompen en espuma
                float w = sen(TAU * (3 * v + 0.3f * sen(TAU * 2 * u) - t));
                float ola = suave(w * 1.3f);
                float espuma = (float) Math.pow(Math.max(0f, w), 6);
                h = 0.3f + 0.5f * ola;
                alfa = 0.42f + 0.4f * ola;
                brillo = espuma;
            }
            case WILD -> {   // venas de hoja y motas de luz de selva
                float x = 3 * u + 1.2f * sen(TAU * (2 * v + t));
                float vena = (float) Math.pow(1f - Math.abs(sen(TAU * x)), 6);
                int cx = (int) (u * 30), cy = (int) (v * 30);
                float e = azar(cx + 2, cy + 6);
                float mota = e > 0.92f ? Math.max(0f, sen(TAU * (t + e * 5))) : 0f;
                h = 0.3f + 0.7f * vena;
                alfa = 0.38f + 0.5f * vena;
                brillo = mota * 0.8f;
            }
            case WARD -> {   // pulsos lentos de sculk: un latido de manchas cian
                float n = 6f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fase = azar(cx, cy), fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float blob = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 2.2f);
                float pulso = 0.5f + 0.5f * sen(TAU * (t + fase));
                float i = 0.10f + blob * blob * pulso * 1.3f;
                h = lim(i);
                alfa = i;
                brillo = blob * blob * pulso * 0.3f;
            }
            case EYE -> {   // ojos del End que parpadean sobre una bruma violeta
                int n = 3;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float ciclo = frac(t + azar(cx, cy));
                float apert = ciclo < 0.82f ? 1f : Math.abs(ciclo - 0.91f) / 0.09f;
                float ex = fu / 0.44f, ey = fv / (0.06f + 0.2f * apert);
                float ojo = ex * ex + ey * ey;
                if (ojo < 1f) {
                    float ir = (float) Math.sqrt((fu / 0.17f) * (fu / 0.17f) + (fv / (0.02f + 0.15f * apert)) * (fv / (0.02f + 0.15f * apert)));
                    if (ir < 0.45f) { h = 0f; alfa = 1f; }
                    else if (ir < 1f) { h = 0.55f; alfa = 0.95f; brillo = 0.15f; }
                    else { h = 0.92f; alfa = 0.9f; brillo = 0.4f; }
                } else {
                    h = 0.35f + 0.1f * sen(TAU * (u + v + t));
                    alfa = 0.32f;
                }
            }
            case VEX -> {   // niebla espectral que sube
                float n1 = ruido(u, v - t, 6, 5), n2 = ruido(u, v + t, 12, 8);
                float n = lim(n1 * 0.7f + n2 * 0.3f);
                h = n;
                alfa = 0.9f * n * n;
                brillo = n * n * 0.5f;
            }
            case TIDE -> {   // cáusticas profundas, de ondas largas
                float s1 = sen(TAU * (2 * u + 0.25f * sen(TAU * (2 * v + t))));
                float s2 = sen(TAU * (2 * v + 0.25f * sen(TAU * (2 * u - t))));
                float vena = (float) Math.pow(1f - Math.abs(s1 * s2), 5);
                h = 0.15f + 0.85f * vena;
                alfa = 0.5f + 0.4f * vena;
                brillo = vena * vena * 0.3f;
            }
            case SNOUT -> {
                // Magma (2026-10-06, "mas que tanto movimiento el magma es mas un shifteo de brillos y endurecimientos
                // magmaticos"): placas de corteza quietas con grietas incandescentes; lo que se mueve es el calor: cada
                // placa se enfría y se endurece y vuelve a calentarse, y el brillo de las grietas se corre despacio.
                voronoi(u, v, 7, 3, o);
                float borde = o[1] - o[0];
                float fase = azar((int) o[2], 77);
                float grieta = lim(1f - borde / 0.2f);
                grieta *= grieta;
                float calor = 0.5f + 0.5f * sen(TAU * (t + fase));
                float pulso = 0.6f + 0.4f * sen(TAU * (t * 2 + fase * 3 + u));
                float corteza = ruido(u, v, 28, 5);
                float placa = 0.06f + 0.10f * corteza + 0.30f * calor * calor * (0.5f + 0.5f * corteza);
                h = grieta * (0.55f + 0.45f * pulso * (0.5f + 0.5f * calor)) + (1f - grieta) * placa;
                alfa = 0.94f;
                brillo = grieta * 0.35f * pulso * calor;
            }
            case RIB -> {   // brasas sueltas que se encienden y se apagan sobre una corteza quemada
                int n = 18;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx + 11, cy + 4);
                float brasa = 0f;
                if (e > 0.68f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float vida = frac(t + e * 7);
                    float forma = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 2.4f);
                    brasa = forma * (1f - vida) * (1f - vida);
                }
                h = 0.12f + 0.88f * brasa;
                alfa = 0.3f + 0.7f * brasa;
                brillo = brasa * 0.5f;
            }
            case SPIRE -> {   // iridiscencia cian → violeta → magenta, con estrellas
                h = 0.5f + 0.5f * sen(TAU * (u + v + t));
                int n = 22;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx, cy);
                float estrella = 0f;
                if (e > 0.84f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float br = 0.5f + 0.5f * sen(TAU * (t * 2 + azar(cy, cx)));
                    estrella = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 5f) * br;
                }
                alfa = 0.58f + 0.4f * estrella;
                brillo = estrella;
            }
            case WAYFINDER -> {   // un haz que gira como la aguja de una brújula
                float dx = u - 0.5f, dy = v - 0.5f;
                float ang = (float) Math.atan2(dy, dx) / TAU;
                float cola = frac(ang - t);
                float haz = (float) Math.pow(1f - cola, 4);
                float anillo = 0.5f + 0.5f * sen(TAU * 8 * (float) Math.sqrt(dx * dx + dy * dy));
                h = haz * 0.9f + 0.1f * anillo;
                alfa = 0.2f + 0.7f * haz;
                brillo = haz * haz * haz * 0.7f;
            }
            case SHAPER -> {   // líneas de cantera con un pulso de luz que las recorre
                float fu = frac(u * 4), fv = frac(v * 4);
                float d = Math.min(Math.min(fu, 1f - fu), Math.min(fv, 1f - fv));
                float linea = Math.max(0f, 1f - d / 0.05f);
                float pulso = Math.max(0f, 1f - frac(2 * u + 2 * v - t) * 3f);
                h = 0.1f + linea * (0.3f + 0.7f * pulso);
                alfa = 0.1f + 0.8f * linea;
                brillo = linea * pulso * 0.7f;
            }
            case SILENCE -> {   // una oscuridad que absorbe la luz, con puntitos pálidos
                float niebla = ruido(u, v, 5, 12);
                alfa = 0.34f + 0.16f * sen(TAU * t) + 0.25f * niebla;
                int n = 30;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx + 3, cy + 17);
                if (e > 0.94f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float punto = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 3f);
                    float pulso = 0.5f + 0.5f * sen(TAU * (t + e * 9));
                    h = punto * pulso;
                    alfa = Math.max(alfa, punto * (0.5f + 0.5f * pulso));
                    brillo = punto * pulso;
                }
            }
            case RAISER -> {   // calor que asciende en columnas
                float col = 0.5f + 0.5f * sen(TAU * (6 * u + 1.5f * ruido(u, v, 4, 9)));
                float pluma = Math.max(0f, 1f - frac(3 * v + t) * 1.5f);
                h = 0.25f * col + 0.75f * pluma * col;
                alfa = 0.22f + 0.6f * pluma * col;
                brillo = pluma * col * 0.5f;
            }
            case HOST -> {   // runas de vasija que se encienden de a una
                int n = 7;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float e = azar(cx, cy);
                float pulso = (float) Math.pow(Math.max(0f, sen(TAU * (t + e))), 8);
                boolean runa = (Math.abs(fu) < 0.07f && Math.abs(fv) < 0.3f) || (Math.abs(fv) < 0.07f && Math.abs(fu) < 0.3f)
                        || (Math.abs(fu) < 0.07f && Math.abs(fv - 0.38f) < 0.07f);
                if (runa) { h = 0.5f + 0.5f * pulso; alfa = 0.35f + 0.65f * pulso; brillo = pulso * 0.8f; }
                else { h = 0.1f; alfa = 0.06f; }
            }
            case FLOW -> {   // remolinos de viento que giran
                int n = 3;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float r = (float) Math.sqrt(fu * fu + fv * fv);
                float ang = (float) Math.atan2(fv, fu) / TAU;
                float espiral = 0.5f + 0.5f * sen(TAU * (2 * ang + 4 * r - t));
                float borde = lim(1f - r * 2f);
                h = espiral;
                alfa = (0.18f + 0.62f * borde) * (0.4f + 0.6f * espiral);
                brillo = (float) Math.pow(espiral, 6) * borde;
            }
            case BOLT -> {   // arcos eléctricos que parpadean
                float x = 3 * v + 4.8f * (ruido(u, v, 10, 4) - 0.5f);
                float fino = (float) Math.pow(1f - Math.abs(sen(TAU * x)), 20);
                float parpadeo = cuadro % 3 == 0 ? 1f : 0.15f;
                h = fino;
                alfa = 0.05f + 0.9f * fino * parpadeo;
                brillo = fino * parpadeo * 0.8f;
            }
            default -> {   // METAL: un barrido de brillo en diagonal
                float banda = Math.max(0f, 1f - Math.abs(frac(u * 0.7f + v * 0.7f - t) - 0.5f) * 9f);
                float debil = Math.max(0f, 1f - Math.abs(frac(u * 0.7f + v * 0.7f - t + 0.5f) - 0.5f) * 14f) * 0.35f;
                h = banda * banda * 0.9f + debil;
                alfa = h;
                brillo = banda * banda * 0.5f;
            }
        }
        return color(p, h, brillo, alfa);
    }
}
