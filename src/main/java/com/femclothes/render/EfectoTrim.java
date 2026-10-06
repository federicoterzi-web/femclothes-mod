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

    /** Cuántos cuadros tiene el bucle, su duración, el lado en que se calcula el efecto y cada cuánto se genera un cuadro. */
    private static final int CUADROS = 8, PERIODO_MS = 2400, LADO = 256, ESPERA_MS = 20;

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
            Identifier base = ClothingTextureCache.ultimaTextura;
            ClothingTextureCache.ultimaTextura = null;   // vale para este pedido solamente
            if (base == null) return real.getBuffer(capa);
            Identifier cuadro = cuadroActual(tipo, base);
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
        final Identifier[] ids = new Identifier[CUADROS];
        int hechos = 0;

        Cuadros(Tipo tipo, NativeImage base) { this.tipo = tipo; this.base = base; }
    }

    private static final int MAXIMO_EN_CACHE = 4;
    private static final Map<String, Cuadros> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Long> REINTENTO = new LinkedHashMap<>();
    private static int numero = 0;
    private static long ultimaGeneracion = 0;

    /** El cuadro del efecto que toca ahora (por el reloj real) sobre esa tela, o null si todavía no hay ninguno. */
    @Nullable
    private static Identifier cuadroActual(Tipo tipo, Identifier base) {
        String clave = tipo.name() + "|" + base;
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
            c = new Cuadros(tipo, img);
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

    /** La tela con el efecto del cuadro {@code c.hechos} mezclado encima (solo donde hay tela). */
    private static void generarSiguiente(Cuadros c) {
        int i = c.hechos;
        float t = i / (float) CUADROS;
        int[] efecto = new int[LADO * LADO];
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) efecto[y * LADO + x] = pixel(c.tipo, (x + 0.5f) / LADO, (y + 0.5f) / LADO, t, i);
        }
        int w = c.base.getWidth(), h = c.base.getHeight();
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            int fy = Math.min(LADO - 1, y * LADO / h);
            for (int x = 0; x < w; x++) {
                int px = c.base.getColor(x, y);
                int alfa = px >>> 24;
                if (alfa > 0) {
                    int e = efecto[fy * LADO + Math.min(LADO - 1, x * LADO / w)];
                    int ea = e >>> 24;
                    int r = ((px & 0xFF) * (255 - ea) + (e & 0xFF) * ea) / 255;
                    int g = (((px >> 8) & 0xFF) * (255 - ea) + ((e >> 8) & 0xFF) * ea) / 255;
                    int b = (((px >> 16) & 0xFF) * (255 - ea) + ((e >> 16) & 0xFF) * ea) / 255;
                    px = (alfa << 24) | (b << 16) | (g << 8) | r;
                }
                img.setColor(x, y, px);
            }
        }
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        c.ids[i] = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture(Femclothes.MOD_ID + "_trim_" + (numero++), tex);
        c.hechos = i + 1;
        if (c.hechos == CUADROS && c.base != null) { c.base.close(); c.base = null; }   // ya no hace falta
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

    /** ABGR con alfa: el efecto es una pintura emisiva con transparencia, no una suma de luz (de día, sumar luz sobre una tela clara no se ve). */
    private static int rgba(float r, float g, float b, float a) {
        int ai = Math.max(0, Math.min(255, Math.round(a)));
        return (ai << 24) | (rgb(r, g, b) & 0x00FFFFFF);
    }

    private static int pixel(Tipo tipo, float u, float v, float t, int cuadro) {
        switch (tipo) {
            case LAVA: {
                float a = sen(TAU * (4 * u + 0.35f * sen(TAU * (3 * v + t))));
                float b = sen(TAU * (5 * v + 0.35f * sen(TAU * (2 * u - t))));
                float c = sen(TAU * (3 * u + 3 * v + t));
                float h = suave(Math.max(0f, Math.min(1f, (a + b + c) / 6f + 0.5f)));
                // Corteza oscura → naranja → amarillo incandescente, casi opaca.
                if (h < 0.5f) return rgba(70 + 370 * h, 10 + 150 * h, 0, 235);
                float k = (h - 0.5f) * 2f;
                return rgba(255, 90 + 140 * k, 20 + 130 * k, 245);
            }
            case OCEANO: {
                float s1 = sen(TAU * (3 * u + 0.25f * sen(TAU * (2 * v + t))));
                float s2 = sen(TAU * (3 * v + 0.25f * sen(TAU * (2 * u - t))));
                float c = 1f - Math.abs(s1 * s2);
                float vena = c * c * c * c * c;
                // Agua turquesa translúcida con venas de luz.
                return rgba(10 + 140 * vena, 90 + 165 * vena, 120 + 135 * vena, 120 + 135 * vena);
            }
            case ESCULK: {
                float n = 6f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float fase = azar(cx, cy), fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                float blob = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 2.2f);
                float pulso = 0.5f + 0.5f * sen(TAU * (t + fase));
                float i = 0.10f + blob * blob * pulso * 1.3f;
                return rgba(40, 235, 245, 255 * Math.min(1f, i));
            }
            case ENDER: {
                float tono = 0.62f + 0.2f * sen(TAU * (u + v + t));   // entre cian, violeta y magenta
                int fondo = hsv(tono, 0.70f, 0.85f);
                float n = 22f;
                int cx = (int) (u * n), cy = (int) (v * n);
                float e = azar(cx, cy);
                float estrella = 0f;
                if (e > 0.84f) {
                    float fu = u * n - cx - 0.5f, fv = v * n - cy - 0.5f;
                    float brillo = 0.5f + 0.5f * sen(TAU * (t * 2 + azar(cy, cx)));
                    estrella = Math.max(0f, 1f - (float) Math.sqrt(fu * fu + fv * fv) * 5f) * brillo;
                }
                float r = (fondo & 0xFF) + (255 - (fondo & 0xFF)) * estrella;
                float g = ((fondo >> 8) & 0xFF) + (255 - ((fondo >> 8) & 0xFF)) * estrella;
                float b = ((fondo >> 16) & 0xFF) + (255 - ((fondo >> 16) & 0xFF)) * estrella;
                return rgba(r, g, b, 150 + 105 * estrella);
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
                return rgba(255, 215 + 40 * chispa, 120 + 135 * chispa, 255 * Math.min(1f, suave * 1.4f + chispa));
            }
            case RAYO: {
                float l = 1f - Math.abs(sen(TAU * (4 * u + 1.5f * sen(TAU * 3 * v))));
                float fino = (float) Math.pow(l, 24);
                float parpadeo = cuadro % 3 == 0 ? 1f : 0.2f;
                return rgba(170, 205, 255, 30 + 225 * fino * parpadeo);
            }
            default: {   // METAL: un barrido de brillo en diagonal
                float d = u * 0.7f + v * 0.7f - t;
                d -= (float) Math.floor(d);
                float banda = Math.max(0f, 1f - Math.abs(d - 0.5f) * 9f);
                float d2 = u * 0.7f + v * 0.7f - t + 0.5f;
                d2 -= (float) Math.floor(d2);
                float debil = Math.max(0f, 1f - Math.abs(d2 - 0.5f) * 14f) * 0.35f;
                return rgba(235, 240, 255, 255 * Math.min(1f, banda * banda * 0.9f + debil));
            }
        }
    }
}
