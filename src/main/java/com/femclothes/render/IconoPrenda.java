package com.femclothes.render;

import com.femclothes.Femclothes;
import com.femclothes.garment.Parte;
import com.femclothes.item.PolleraItem;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ícono de 64x64 de una prenda armado con su tela REAL — a pedido
 * (2026-09-29, "haceme una propuesta de items de cada prenda superior a la
 * actual y que representen mejor a la prenda" → "ok pero los hagamos
 * 64x64" → "B"): el color de cada píxel sale de la misma textura que se
 * dibuja sobre el cuerpo ({@link PiezasDePrenda}), así el ícono muestra
 * todo lo que se le hizo en las máquinas — capas y patrones de Tintes,
 * cada pierna/manga distinta, redes y arneses, fotos de la Sublimadora — y
 * el largo real: las filas que la pieza no tapa ({@code filaDesde}/
 * {@code filaHasta}) se recortan (manga corta, crop, shorts, zoquetes).
 *
 * <p>Por ícono hay dos PNG en {@code textures/item/icono/}, generados por
 * {@code tools/generar_iconos_prendas.py}: {@code _sombra} (silueta + relieve
 * en gris, plano = {@link #GRIS_PLANO}) y {@code _mapa} (R = código de cara
 * × 20, G/B = u/v dentro de esa cara, v = fila de la parte). El contorno lo
 * pone este código, así también bordea lo recortado; los agujeros de la
 * tela (redes) quedan sin contorno.
 */
public final class IconoPrenda {

    public static final int LADO = 64;
    /** Gris del relieve en una zona plana: el color de la tela sale tal cual. */
    private static final float GRIS_PLANO = 219f;
    private static final float OSCURO_CONTORNO = 0.3f;
    /** Cada cuánto se vuelven a leer las piezas de un mismo stack (la foto puede terminar de bajar). */
    private static final long REVISAR_MS = 1000;
    private static final int MAXIMO_ICONOS = 256;

    // Códigos del mapa — mismos que tools/generar_iconos_prendas.py.
    private static final int TORSO = 1, BRAZO_DER = 2, BRAZO_IZQ = 3, PIERNA_DER = 4, PIERNA_IZQ = 5,
            TORSO_ATRAS = 6, POLLERA = 7;

    /** Cara grande de un gajo de la pollera en su textura de 64 (ver PolleraGeometria: UV en (0,0), ANCHO_GAJO 1.79, LARGO_GAJO 9). */
    private static final float POLLERA_X = 0f, POLLERA_Y = 1.79f, POLLERA_ANCHO = 1.79f, POLLERA_ALTO = 9f;

    private record Plantilla(NativeImage sombra, NativeImage mapa) {}

    private static final class Icono {
        final Identifier id;
        final NativeImageBackedTexture textura;
        boolean completo;
        long ultimoIntento;

        Icono(Identifier id, NativeImageBackedTexture textura) {
            this.id = id;
            this.textura = textura;
        }
    }

    private record PorStack(String clave, long cuando) {}

    private static final Map<String, Plantilla> PLANTILLAS = new HashMap<>();
    private static final Map<Integer, PorStack> POR_STACK = new HashMap<>();
    private static final LinkedHashMap<String, Icono> ICONOS = new LinkedHashMap<>(64, 0.75f, true);

    private IconoPrenda() {}

    /** Nombre de la plantilla de cada prenda, o null si la prenda no tiene ícono armado. */
    @Nullable
    private static String plantillaDe(ItemStack stack) {
        var item = stack.getItem();
        if (item == com.femclothes.sublimadora.ModItems.REMERA) {
            return "remera_" + com.femclothes.sublimadora.RemeraItem.variante(stack).cuello().asString();
        }
        if (item == com.femclothes.item.FemclothesItems.PANTALON) return "pantalon";
        if (item instanceof PolleraItem) return "pollera";
        if (item == com.femclothes.item.FemclothesItems.SOCKS_SOLID) return "medias";
        if (item == com.femclothes.item.FemclothesItems.CALIENTABRAZOS) return "calientabrazos";
        return null;
    }

    public static boolean tieneIcono(ItemStack stack) {
        return plantillaDe(stack) != null;
    }

    /** La textura del ícono de este stack, o null si no se pudo armar (sin plantilla). */
    @Nullable
    public static Identifier de(ItemStack stack) {
        String nombre = plantillaDe(stack);
        if (nombre == null) return null;
        long ahora = System.currentTimeMillis();

        int hash = ItemStack.hashCode(stack);
        PorStack cacheado = POR_STACK.get(hash);
        String clave;
        List<Pieza> piezas = null;
        Identifier texturaPollera = null;
        if (cacheado != null && ahora - cacheado.cuando() < REVISAR_MS) {
            clave = cacheado.clave();
        } else {
            piezas = PiezasDePrenda.de(stack, null);
            if (stack.getItem() instanceof PolleraItem) texturaPollera = GarmentFeatureRenderer.texturaPollera(stack);
            clave = claveDe(nombre, piezas, texturaPollera);
            if (POR_STACK.size() > 4 * MAXIMO_ICONOS) POR_STACK.clear();
            POR_STACK.put(hash, new PorStack(clave, ahora));
        }

        Icono icono = ICONOS.get(clave);
        if (icono != null && (icono.completo || ahora - icono.ultimoIntento < REVISAR_MS)) return icono.id;

        Plantilla plantilla = plantilla(nombre);
        if (plantilla == null) return null;
        if (piezas == null) {
            piezas = PiezasDePrenda.de(stack, null);
            if (stack.getItem() instanceof PolleraItem) texturaPollera = GarmentFeatureRenderer.texturaPollera(stack);
        }

        if (icono == null) {
            NativeImage img = new NativeImage(LADO, LADO, true);
            NativeImageBackedTexture textura = new NativeImageBackedTexture(img);
            Identifier id = Identifier.of(Femclothes.MOD_ID, "icono/" + Integer.toHexString(clave.hashCode()));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, textura);
            icono = new Icono(id, textura);
            ICONOS.put(clave, icono);
            recortarCache();
        }
        icono.completo = componer(icono.textura.getImage(), plantilla, stack, piezas, texturaPollera);
        icono.ultimoIntento = ahora;
        icono.textura.upload();
        return icono.id;
    }

    private static String claveDe(String nombre, List<Pieza> piezas, @Nullable Identifier pollera) {
        StringBuilder sb = new StringBuilder(nombre);
        for (Pieza p : piezas) {
            sb.append('|').append(p.parte().ordinal()).append(':').append(p.textura())
                    .append(':').append(p.filaDesde()).append('-').append(p.filaHasta());
        }
        if (pollera != null) sb.append("|pollera:").append(pollera);
        return sb.toString();
    }

    private static void recortarCache() {
        while (ICONOS.size() > MAXIMO_ICONOS) {
            var it = ICONOS.entrySet().iterator();
            Icono viejo = it.next().getValue();
            it.remove();
            MinecraftClient.getInstance().getTextureManager().destroyTexture(viejo.id);
        }
    }

    /** Recarga de recursos (F3+T): se tiran las plantillas y los íconos armados. */
    public static void limpiar() {
        PLANTILLAS.clear();
        POR_STACK.clear();
        for (Icono icono : ICONOS.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(icono.id);
        }
        ICONOS.clear();
    }

    @Nullable
    private static Plantilla plantilla(String nombre) {
        if (PLANTILLAS.containsKey(nombre)) return PLANTILLAS.get(nombre);
        NativeImage sombra = ClothingTextureCache.imagenBase(
                Identifier.of(Femclothes.MOD_ID, "textures/item/icono/" + nombre + "_sombra.png"));
        NativeImage mapa = ClothingTextureCache.imagenBase(
                Identifier.of(Femclothes.MOD_ID, "textures/item/icono/" + nombre + "_mapa.png"));
        Plantilla p = sombra == null || mapa == null ? null : new Plantilla(sombra, mapa);
        PLANTILLAS.put(nombre, p);
        return p;
    }

    /** La imagen de una textura de prenda: la compuesta en memoria o la del resource pack. */
    @Nullable
    private static NativeImage imagen(Identifier id) {
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getOrDefault(id, null);
        if (tex instanceof NativeImageBackedTexture backed && backed.getImage() != null) return backed.getImage();
        return ClothingTextureCache.imagenBase(id);
    }

    /** @return false si faltó alguna textura (todavía componiéndose): se reintenta en un rato. */
    private static boolean componer(NativeImage destino, Plantilla plantilla, ItemStack stack,
                                    List<Pieza> piezas, @Nullable Identifier texturaPollera) {
        boolean completo = true;
        // La pieza de más arriba de cada parte (la remera tiene una por parte;
        // si hubiera dos, la de capa mayor es la que se ve).
        Map<Parte, Pieza> porParte = new HashMap<>();
        for (Pieza p : piezas) {
            Pieza actual = porParte.get(p.parte());
            if (actual == null || p.capa() >= actual.capa()) porParte.put(p.parte(), p);
        }
        Map<Identifier, NativeImage> imagenes = new HashMap<>();
        NativeImage pollera = texturaPollera == null ? null : imagen(texturaPollera);
        if (texturaPollera != null && pollera == null) completo = false;
        int colorLiso = RegionResolver.colorBase(stack, Lado.IZQUIERDA);

        boolean[] cortado = new boolean[LADO * LADO];
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                destino.setColor(x, y, 0);
                int s = plantilla.sombra().getColor(x, y);
                if ((s >>> 24) == 0) continue;
                int m = plantilla.mapa().getColor(x, y);
                int codigo = Math.round((m & 0xFF) / 20f);
                float u = (((m >> 8) & 0xFF) + 0.5f) / 256f;
                float v = (((m >> 16) & 0xFF) + 0.5f) / 256f;

                int texel;   // ABGR
                if (codigo == POLLERA) {
                    if (pollera == null) {
                        texel = abgrDe(colorLiso);
                    } else {
                        float esc = pollera.getWidth() / 64f;
                        texel = muestra(pollera, (POLLERA_X + u * POLLERA_ANCHO) * esc, (POLLERA_Y + v * POLLERA_ALTO) * esc);
                    }
                } else {
                    Parte parte = parteDe(codigo);
                    Pieza pieza = parte == null ? null : porParte.get(parte);
                    float fila = v * 12f;
                    if (pieza == null || fila < pieza.filaDesde() || fila >= pieza.filaHasta()) {
                        cortado[y * LADO + x] = true;
                        continue;
                    }
                    NativeImage img = imagenes.computeIfAbsent(pieza.textura(), IconoPrenda::imagen);
                    if (img == null) {
                        completo = false;
                        texel = abgrDe(colorLiso);
                    } else {
                        CajaSkin caja = LayoutSkin.base(parte, false);
                        CajaSkin.Rect cara = codigo == TORSO_ATRAS ? caja.atras() : caja.frente();
                        float esc = img.getWidth() / 64f;
                        texel = muestra(img, (cara.x0() + u * cara.anchoRect()) * esc,
                                (cara.y0() + v * cara.altoRect()) * esc);
                    }
                }
                // Agujero de la tela (red, arnés): transparente y sin contorno.
                if ((texel >>> 24) < 128) continue;
                float f = (s & 0xFF) / GRIS_PLANO;
                destino.setColor(x, y, escalar(texel, f));
            }
        }

        // Contorno: todo píxel con tela que toca afuera de la silueta o algo recortado.
        int[] oscurecer = new int[LADO * LADO];
        int n = 0;
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                if ((destino.getColor(x, y) >>> 24) == 0) continue;
                if (borde(plantilla.sombra(), cortado, x - 1, y) || borde(plantilla.sombra(), cortado, x + 1, y)
                        || borde(plantilla.sombra(), cortado, x, y - 1) || borde(plantilla.sombra(), cortado, x, y + 1)) {
                    oscurecer[n++] = y * LADO + x;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            int x = oscurecer[i] % LADO, y = oscurecer[i] / LADO;
            destino.setColor(x, y, escalar(destino.getColor(x, y), OSCURO_CONTORNO));
        }
        return completo;
    }

    private static boolean borde(NativeImage sombra, boolean[] cortado, int x, int y) {
        if (x < 0 || y < 0 || x >= LADO || y >= LADO) return true;
        return (sombra.getColor(x, y) >>> 24) == 0 || cortado[y * LADO + x];
    }

    @Nullable
    private static Parte parteDe(int codigo) {
        return switch (codigo) {
            case TORSO, TORSO_ATRAS -> Parte.TORSO;
            case BRAZO_DER -> Parte.BRAZO_DER;
            case BRAZO_IZQ -> Parte.BRAZO_IZQ;
            case PIERNA_DER -> Parte.PIERNA_DER;
            case PIERNA_IZQ -> Parte.PIERNA_IZQ;
            default -> null;
        };
    }

    private static int muestra(NativeImage img, float x, float y) {
        // getColor solo anda en RGBA: una textura en otro formato se ve blanca en vez de crashear.
        if (img.getFormat() != NativeImage.Format.RGBA) return 0xFFFFFFFF;
        int ix = Math.min(img.getWidth() - 1, Math.max(0, (int) x));
        int iy = Math.min(img.getHeight() - 1, Math.max(0, (int) y));
        return img.getColor(ix, iy);
    }

    /** 0xRRGGBB → ABGR opaco. */
    private static int abgrDe(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    /** Multiplica RGB (ABGR) por f, alfa opaco. */
    private static int escalar(int abgr, float f) {
        int r = Math.min(255, Math.round((abgr & 0xFF) * f));
        int g = Math.min(255, Math.round(((abgr >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round(((abgr >> 16) & 0xFF) * f));
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}
