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

    /** Los efectos. Agregar valores nuevos al final. */
    public enum Tipo {
        /** Lava en movimiento, brasas: Bastión y Fortaleza. */
        LAVA,
        /** Cáusticas del mar: Monumento y Naufragio. */
        OCEANO,
        /** Pulsos suaves de sculk y espectros: Ciudad ancestral, Silencio y Mansión. */
        ESCULK,
        /** Iridiscencia entre violeta, cian y magenta con estrellas: Ciudad del End y Fortaleza del End. */
        ENDER,
        /** Destellos de arena y de selva. */
        DESTELLO,
        /** Un barrido de brillo metálico: los demás. */
        METAL,
        /** Chispas eléctricas y remolinos: Cámaras de prueba. */
        RAYO
    }

    /** El efecto de un patrón de trim (por el nombre del patrón; los desconocidos, de otros mods, son metálicos). */
    public static Tipo tipoDe(Identifier patron) {
        return switch (patron.getPath()) {
            case "snout", "rib" -> Tipo.LAVA;
            case "tide", "coast" -> Tipo.OCEANO;
            case "ward", "silence", "vex" -> Tipo.ESCULK;
            case "spire", "eye" -> Tipo.ENDER;
            case "dune", "wild" -> Tipo.DESTELLO;
            case "bolt", "flow" -> Tipo.RAYO;
            default -> Tipo.METAL;
        };
    }

    /** Cuántos cuadros tiene el bucle, su duración y el lado de cada uno. */
    private static final int CUADROS = 8, PERIODO_MS = 3200, LADO = 256;

    /** Lo que se dibuja con el acabado de {@code prenda}: el proveedor de siempre si no lleva ninguno. */
    public static VertexConsumerProvider envolver(@Nullable ItemStack prenda, VertexConsumerProvider real) {
        if (prenda == null) return real;
        Identifier patron = prenda.get(FemclothesComponents.ACABADO_TRIM);
        return patron == null ? real : new Proveedor(real, tipoDe(patron));
    }

    private static final class Proveedor implements VertexConsumerProvider {
        private final VertexConsumerProvider real;
        private final Tipo tipo;

        Proveedor(VertexConsumerProvider real, Tipo tipo) {
            this.real = real;
            this.tipo = tipo;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer capa) {
            VertexConsumer normal = real.getBuffer(capa);
            Identifier base = ClothingTextureCache.ultimaTextura;
            ClothingTextureCache.ultimaTextura = null;   // vale para este pedido solamente
            if (base == null) return normal;
            // No se piden dos buffers a la vez: el proveedor inmediato cierra el anterior al cambiar de capa
            // ("Not building!"). Se anotan los vértices y la capa de efecto se dibuja después, con volcarTodo().
            Registro r = new Registro(normal, real, tipo, base);
            REGISTROS.add(r);
            return r;
        }
    }

    private static final java.util.List<Registro> REGISTROS = new java.util.ArrayList<>();

    /** Dibuja las capas de efecto de todo lo anotado hasta ahora (cada pieza terminada, y después de las translúcidas). */
    public static void volcarTodo() {
        if (REGISTROS.isEmpty()) return;
        java.util.List<Registro> lista = new java.util.ArrayList<>(REGISTROS);
        REGISTROS.clear();
        for (Registro r : lista) r.volcar();
    }

    /** Deja pasar todo al buffer normal y se acuerda de cada vértice para repetirlo en la capa de efecto. */
    private static final class Registro implements VertexConsumer {
        private final VertexConsumer normal;
        private final VertexConsumerProvider real;
        private final Tipo tipo;
        private final Identifier base;
        private float[] datos = new float[12 * 64];   // por vértice: x y z u v ou ov lu lv nx ny nz
        private int n = 0;
        private float x, y, z, u, v, nx, ny, nz;
        private int ou, ov, lu, lv;

        Registro(VertexConsumer normal, VertexConsumerProvider real, Tipo tipo, Identifier base) {
            this.normal = normal;
            this.real = real;
            this.tipo = tipo;
            this.base = base;
        }

        @Override public VertexConsumer vertex(float x, float y, float z) { normal.vertex(x, y, z); this.x = x; this.y = y; this.z = z; return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { normal.color(r, g, b, a); return this; }
        @Override public VertexConsumer texture(float u, float v) { normal.texture(u, v); this.u = u; this.v = v; return this; }
        @Override public VertexConsumer overlay(int u, int v) { normal.overlay(u, v); ou = u; ov = v; return this; }
        @Override public VertexConsumer light(int u, int v) { normal.light(u, v); lu = u; lv = v; return this; }

        @Override
        public VertexConsumer normal(float nx, float ny, float nz) {
            normal.normal(nx, ny, nz);
            if ((n + 1) * 12 > datos.length) datos = java.util.Arrays.copyOf(datos, datos.length * 2);
            int o = n * 12;
            datos[o] = x; datos[o + 1] = y; datos[o + 2] = z; datos[o + 3] = u; datos[o + 4] = v;
            datos[o + 5] = ou; datos[o + 6] = ov; datos[o + 7] = lu; datos[o + 8] = lv;
            datos[o + 9] = nx; datos[o + 10] = ny; datos[o + 11] = nz;
            n++;
            return this;
        }

        void volcar() {
            if (n == 0) return;
            Cuadros c = cuadros(tipo, base);
            if (c == null || c.hechos == 0) return;
            float fase = (System.currentTimeMillis() % PERIODO_MS) / (float) PERIODO_MS * c.hechos;
            int a = Math.min((int) fase, c.hechos - 1);
            int b = (a + 1) % c.hechos;
            float t = fase - (int) fase;
            repetir(real.getBuffer(RenderLayer.getEyes(c.ids[a])), 1f - t);
            if (b != a) repetir(real.getBuffer(RenderLayer.getEyes(c.ids[b])), t);
        }

        private void repetir(VertexConsumer vc, float peso) {
            int alfa = Math.round(255 * peso);
            if (alfa <= 0) return;
            for (int i = 0; i < n; i++) {
                int o = i * 12;
                vc.vertex(datos[o], datos[o + 1], datos[o + 2]).color(255, 255, 255, alfa).texture(datos[o + 3], datos[o + 4])
                        .overlay((int) datos[o + 5], (int) datos[o + 6]).light((int) datos[o + 7], (int) datos[o + 8])
                        .normal(datos[o + 9], datos[o + 10], datos[o + 11]);
            }
        }
    }

    // ── los cuadros de cada tela ─────────────────────────────────────────────

    private static final class Cuadros {
        final Tipo tipo;
        final byte[] mascara;                    // LADO*LADO: 1 = es tela
        final Identifier[] ids = new Identifier[CUADROS];
        final NativeImageBackedTexture[] texturas = new NativeImageBackedTexture[CUADROS];
        int hechos = 0;

        Cuadros(Tipo tipo, byte[] mascara) { this.tipo = tipo; this.mascara = mascara; }
    }

    private static final int MAXIMO_EN_CACHE = 8;
    private static final Map<String, Cuadros> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Long> REINTENTO = new LinkedHashMap<>();
    private static int numero = 0;

    @Nullable
    private static Cuadros cuadros(Tipo tipo, Identifier base) {
        String clave = tipo.name() + "|" + base;
        Cuadros c = CACHE.get(clave);
        if (c == null) {
            long ahora = System.currentTimeMillis();
            Long luego = REINTENTO.get(clave);
            if (luego != null && ahora < luego) return null;
            byte[] m = mascaraDe(base);
            if (m == null) {
                REINTENTO.put(clave, ahora + 500);
                return null;
            }
            c = new Cuadros(tipo, m);
            CACHE.put(clave, c);
            if (CACHE.size() > MAXIMO_EN_CACHE) {
                var it = CACHE.entrySet().iterator();
                Cuadros viejo = it.next().getValue();
                it.remove();
                liberar(viejo);
            }
        }
        if (c.hechos < CUADROS) generarSiguiente(c);   // de a uno por dibujo
        return c;
    }

    private static void liberar(Cuadros c) {
        var tm = MinecraftClient.getInstance().getTextureManager();
        for (int i = 0; i < c.hechos; i++) tm.destroyTexture(c.ids[i]);
    }

    /** Qué texeles de la tela lo son (alfa > 0), a {@link #LADO}. Null si la textura todavía no se puede leer. */
    @Nullable
    private static byte[] mascaraDe(Identifier base) {
        NativeImage img = EstampaTextures.leerTextura(base);
        if (img == null) return null;
        try {
            byte[] m = new byte[LADO * LADO];
            int w = img.getWidth(), h = img.getHeight();
            for (int y = 0; y < LADO; y++) {
                for (int x = 0; x < LADO; x++) {
                    // El bloque de texeles que cubre este: con que uno sea tela, lo es.
                    int x0 = x * w / LADO, x1 = Math.max(x0 + 1, (x + 1) * w / LADO);
                    int y0 = y * h / LADO, y1 = Math.max(y0 + 1, (y + 1) * h / LADO);
                    boolean tela = false;
                    for (int yy = y0; yy < y1 && !tela; yy++) {
                        for (int xx = x0; xx < x1; xx++) {
                            if (((img.getColor(Math.min(xx, w - 1), Math.min(yy, h - 1)) >>> 24) & 0xFF) > 8) { tela = true; break; }
                        }
                    }
                    if (tela) m[y * LADO + x] = 1;
                }
            }
            return m;
        } finally {
            img.close();
        }
    }

    private static void generarSiguiente(Cuadros c) {
        int i = c.hechos;
        float t = i / (float) CUADROS;
        NativeImage img = new NativeImage(LADO, LADO, true);
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                int px = c.mascara[y * LADO + x] == 0 ? 0 : pixel(c.tipo, (x + 0.5f) / LADO, (y + 0.5f) / LADO, t, i);
                img.setColor(x, y, px);
            }
        }
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        c.texturas[i] = tex;
        c.ids[i] = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(Femclothes.MOD_ID + "_trim_" + (numero++), tex);
        c.hechos = i + 1;
    }

    // ── los efectos (todos periódicos en u, v y t) ──────────────────────────

    private static final float TAU = (float) (Math.PI * 2);

    private static float sen(float x) { return (float) Math.sin(x); }

    private static float azar(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /** ABGR opaco. */
    private static int rgb(float r, float g, float b) {
        int ri = Math.max(0, Math.min(255, Math.round(r))), gi = Math.max(0, Math.min(255, Math.round(g))),
                bi = Math.max(0, Math.min(255, Math.round(b)));
        return 0xFF000000 | (bi << 16) | (gi << 8) | ri;
    }

    private static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        float f6 = h * 6f;
        int sector = (int) f6;
        float f = f6 - sector, p = v * (1 - s), q = v * (1 - s * f), tt = v * (1 - s * (1 - f));
        switch (sector % 6) {
            case 0 -> { r = v; g = tt; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = tt; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = tt; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return rgb(r * 255, g * 255, b * 255);
    }

    private static float suave(float h) { return h * h * (3 - 2 * h); }

    private static int pixel(Tipo tipo, float u, float v, float t, int cuadro) {
        switch (tipo) {
            case LAVA: {
                float a = sen(TAU * (4 * u + 0.35f * sen(TAU * (3 * v + t))));
                float b = sen(TAU * (5 * v + 0.35f * sen(TAU * (2 * u - t))));
                float c = sen(TAU * (3 * u + 3 * v + t));
                float h = suave(Math.max(0f, Math.min(1f, (a + b + c) / 6f + 0.5f)));
                // Corteza oscura → naranja → amarillo incandescente.
                if (h < 0.5f) return rgb(60 + 390 * h, 8 + 164 * h, 0);
                float k = (h - 0.5f) * 2f;
                return rgb(255, 90 + 140 * k, 20 + 130 * k);
            }
            case OCEANO: {
                float s1 = sen(TAU * (3 * u + 0.25f * sen(TAU * (2 * v + t))));
                float s2 = sen(TAU * (3 * v + 0.25f * sen(TAU * (2 * u - t))));
                float c = 1f - Math.abs(s1 * s2);
                float vena = c * c * c * c * c;
                return rgb(0 + 150 * vena, 40 + 215 * vena, 70 + 165 * vena);
            }
            case ESCULK: {
                float n = 6f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fase = azar(cx, cy), fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float blob = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 2.2f);
                float pulso = 0.5f + 0.5f * sen(TAU * (t + fase));
                float i = 0.06f + blob * blob * pulso;
                return rgb(40 * i, 235 * i, 245 * i);
            }
            case ENDER: {
                float tono = 0.62f + 0.2f * sen(TAU * (u + v + t));   // entre cian, violeta y magenta
                int fondo = hsv(tono, 0.75f, 0.42f);
                float n = 22f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx, cy);
                if (e > 0.84f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float brillo = 0.5f + 0.5f * sen(TAU * (t * 2 + azar(cy, cx)));
                    float estrella = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 5f) * brillo;
                    if (estrella > 0f) {
                        float r = (fondo & 0xFF) + 255 * estrella, g = ((fondo >> 8) & 0xFF) + 255 * estrella, b = ((fondo >> 16) & 0xFF) + 255 * estrella;
                        return rgb(r, g, b);
                    }
                }
                return fondo;
            }
            case DESTELLO: {
                float n = 34f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx + 7, cy + 3);
                float suave = 0.10f + 0.08f * sen(TAU * (u * 2 + v + t));
                float chispa = 0f;
                if (e > 0.88f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float brillo = 0.5f + 0.5f * sen(TAU * (t + e * 13));
                    chispa = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 3.2f) * brillo;
                }
                float i = suave + chispa;
                return rgb(255 * i, 215 * i, 120 * i + 90 * chispa);
            }
            case RAYO: {
                float l = 1f - Math.abs(sen(TAU * (4 * u + 1.5f * sen(TAU * 3 * v))));
                float fino = (float) Math.pow(l, 24);
                float parpadeo = cuadro % 3 == 0 ? 1f : 0.2f;
                float i = fino * parpadeo + 0.05f;
                return rgb(150 * i, 190 * i, 255 * i);
            }
            default: {   // METAL: un barrido de brillo en diagonal
                float d = u * 0.7f + v * 0.7f - t;
                d -= (float) Math.floor(d);
                float banda = Math.max(0f, 1f - Math.abs(d - 0.5f) * 9f);
                float d2 = u * 0.7f + v * 0.7f - t + 0.5f;
                d2 -= (float) Math.floor(d2);
                float debil = Math.max(0f, 1f - Math.abs(d2 - 0.5f) * 14f) * 0.35f;
                float i = banda * banda + debil + 0.04f;
                return rgb(225 * i, 232 * i, 255 * i);
            }
        }
    }
}
