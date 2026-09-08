package com.femclothes.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compositor genérico de texturas de prendas: lee un .png base (escala de
 * grises) y devuelve una versión teñida al color pedido, cacheada. Ya no
 * dependemos del tinte automático de "dyeable" de armadura (eso solo
 * existía en el pipeline de ArmorItem) — este es el mismo mecanismo a
 * mano para cualquier prenda que se dibuje vía BodyPartTrinketRenderer.
 */
public final class ClothingTextureCache {

    private static final Map<Identifier, NativeImage> BASE_IMAGE_CACHE = new HashMap<>();
    private static final Map<String, Identifier> TINTED_CACHE = new HashMap<>();

    /**
     * Volcado a disco de cada textura compuesta, para diagnosticar sin
     * depender de lo que se alcanza a ver en una captura de pantalla.
     * Prender a mano, probar en el juego, apagar — no queda prendido.
     */
    public static boolean DEBUG_DUMP = false;

    private ClothingTextureCache() {}

    /**
     * Vacía los dos caches (imagen cruda + composición final).
     *
     * Hace falta llamarlo en cada recarga de recursos: F3+T vuelve a leer
     * los .png del disco, pero estos mapas son estáticos y sobreviven esa
     * recarga solos — sin esto, cambiar un asset y F3+T seguía mostrando
     * la composición vieja (encontrado jugando, iterando la textura de
     * calientabrazos: "sigue igual" después de F3+T, hacía falta relanzar
     * el cliente entero para ver el cambio).
     */
    public static void limpiarCache() {
        BASE_IMAGE_CACHE.clear();
        TINTED_CACHE.clear();
    }

    private static void volcarADisco(String key, NativeImage img) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("femclothes_debug");
            java.nio.file.Files.createDirectories(dir);
            String nombre = Integer.toHexString(key.hashCode()) + ".png";
            img.writeTo(dir.resolve(nombre));
            System.out.println("[femclothes-debug] volcado " + dir.resolve(nombre) + " <- " + key);
        } catch (IOException e) {
            System.out.println("[femclothes-debug] no se pudo volcar: " + e);
        }
    }

    /** Devuelve la textura base sin modificar (para prendas sin tinte, ej. maid_outfit). */
    public static Identifier plain(Identifier baseTexture) {
        return baseTexture;
    }

    /** Tiñe toda la textura base con un solo color (medias, shorts dyeable). */
    public static Identifier tinted(Identifier baseTexture, int rgb) {
        String key = baseTexture + "#" + Integer.toHexString(rgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = imagenBase(baseTexture);
        if (base == null) return baseTexture;

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);
        int dr = (rgb >> 16) & 0xFF;
        int dg = (rgb >> 8) & 0xFF;
        int db = rgb & 0xFF;

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int px = base.getColor(x, y);
                int a = (px >> 24) & 0xFF;
                if (a == 0) {
                    composite.setColor(x, y, 0);
                    continue;
                }
                int bChan = (px >> 16) & 0xFF;
                int gChan = (px >> 8) & 0xFF;
                int rChan = px & 0xFF;
                int tr = (rChan * dr) / 255;
                int tg = (gChan * dg) / 255;
                int tb = (bChan * db) / 255;
                composite.setColor(x, y, (a << 24) | (tb << 16) | (tg << 8) | tr);
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/tint_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /**
     * Tiñe una textura con UN color principal + rellena con OTRO color
     * (piel, o un segundo tinte) las zonas con alpha 0 de la base — usado
     * por el croptop (relleno de piel) y por prendas de dos tonos.
     */
    public static Identifier tintedWithFill(Identifier baseTexture, int mainRgb, int fillRgb) {
        String key = baseTexture + "#" + Integer.toHexString(mainRgb) + "#" + Integer.toHexString(fillRgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = imagenBase(baseTexture);
        if (base == null) return baseTexture;

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);
        int dr = (mainRgb >> 16) & 0xFF, dg = (mainRgb >> 8) & 0xFF, db = mainRgb & 0xFF;

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int px = base.getColor(x, y);
                int a = (px >> 24) & 0xFF;
                if (a == 0) {
                    composite.setColor(x, y, fillRgb);
                    continue;
                }
                int bChan = (px >> 16) & 0xFF, gChan = (px >> 8) & 0xFF, rChan = px & 0xFF;
                int tr = (rChan * dr) / 255, tg = (gChan * dg) / 255, tb = (bChan * db) / 255;
                composite.setColor(x, y, (a << 24) | (tb << 16) | (tg << 8) | tr);
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/tintfill_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /**
     * Convención de ruta de la máscara de un patrón, por prenda:
     *   textures/models/armor/patterns/<prenda>/<patrón>.png
     * Ej: patrón "femclothes:stripe_top" sobre "socks" →
     *   femclothes:textures/models/armor/patterns/socks/stripe_top.png
     *
     * Es por prenda y no global porque la máscara depende del UV map de
     * la prenda: una raya sobre las medias cae en píxeles distintos que
     * la misma raya sobre un croptop. Agregar un patrón a una prenda =
     * soltar UN png en esa carpeta, sin tocar código.
     */
    public static Identifier patternMaskFor(String garment, Identifier patternId) {
        return Identifier.of(patternId.getNamespace(),
                "textures/models/armor/patterns/" + garment + "/" + patternId.getPath() + ".png");
    }

    /**
     * Base teñida con el color base + máscara de patrón teñida con el
     * color de patrón dibujada encima. Donde la máscara es opaca gana el
     * patrón; donde es transparente se ve la base. El patrón viene de un
     * componente del ItemStack (lo escribe el telar) en vez de estar fijo
     * en el Item — por eso una sola prenda cubre todas las variantes que
     * antes eran un Item por combinación de colores.
     *
     * Si la máscara todavía no tiene arte, cae de vuelta a la prenda lisa
     * en vez de mostrar el cuadrado de textura faltante.
     */
    public static Identifier tintedWithPattern(Identifier baseTexture, int baseRgb,
                                               Identifier patternMask, int patternRgb) {
        String key = baseTexture + "#" + Integer.toHexString(baseRgb)
                + "@" + patternMask + "#" + Integer.toHexString(patternRgb);
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = imagenBase(baseTexture);
        if (base == null) return baseTexture;

        NativeImage mask = imagenBase(patternMask);
        if (mask == null) return tinted(baseTexture, baseRgb);

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);

        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int maskPx = (x < mask.getWidth() && y < mask.getHeight()) ? mask.getColor(x, y) : 0;
                if (((maskPx >> 24) & 0xFF) != 0) {
                    composite.setColor(x, y, tintPixel(maskPx, patternRgb));
                    continue;
                }
                int basePx = base.getColor(x, y);
                if (((basePx >> 24) & 0xFF) == 0) {
                    composite.setColor(x, y, 0);
                } else {
                    composite.setColor(x, y, tintPixel(basePx, baseRgb));
                }
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/pattern_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    private static final float FACE_LIGHT = 1.10F;
    private static final float FACE_DARK = 0.84F;
    /** Ancho en pixeles de las caras frontal y trasera de una pierna. */
    private static final int FACE_COLUMNS = 4 * CuerpoGeometria.ESCALA_TELA;

    /**
     * Factor de luz del pixel segun donde cae en el cuboide de la pierna.
     *
     * Las caras INTERIORES de las piernas no se ven con el jugador parado —
     * quedan una contra la otra — asi que sombrearlas no separaba nada. Lo
     * que si se ve es el BORDE interno de las caras frontal y trasera: una
     * columna de 1px oscura ahi hace de sombra entre las dos piernas.
     *
     * Que columna es el borde interno sale del constructor de ModelPart.Cuboid:
     * en cada Quad, vertices[0] toma u2 (columna derecha) y vertices[1] toma u1
     * (izquierda). En la cara NORTH vertices[0] es el vertice de maxX, y en la
     * SOUTH es el de minX — o sea la trasera esta espejada respecto de la
     * frontal, como corresponde a mirarla desde atras.
     *
     * El jugador tiene su derecha en -X (right_arm pivota en x=-5), asi que
     * para la pierna derecha el lado interno es +X y para la izquierda es -X.
     */
    private static float faceFactor(int x, int y, Shading shading) {
        final int S = CuerpoGeometria.ESCALA_TELA;
        final int face = 4 * S;   // ancho de una cara del cuboide, en pixeles

        if (shading == Shading.LEGS) {
            // Pierna derecha, uv(0, 16S): caras laterales en y 20S..32S-1, x 0..16S-1
            if (y >= 20 * S && y < 32 * S && x >= 0 && x < 16 * S) {
                int col = x / face;
                return switch (col) {
                    case 0 -> FACE_LIGHT;                       // WEST, exterior
                    case 1 -> ramp((2 * face - 1) - x);         // NORTH: interno en el borde derecho
                    case 2 -> FACE_DARK;                        // EAST, interior
                    default -> ramp(x - 3 * face);              // SOUTH: interno en el borde izquierdo
                };
            }
            // Pierna izquierda, uv(16S, 48S): caras laterales en y 52S..64S-1, x 16S..32S-1
            if (y >= 52 * S && y < 64 * S && x >= 16 * S && x < 32 * S) {
                int lx = x - 16 * S;
                int col = lx / face;
                return switch (col) {
                    case 0 -> FACE_DARK;                        // WEST, interior
                    case 1 -> ramp(lx - face);                  // NORTH: interno en el borde izquierdo
                    case 2 -> FACE_LIGHT;                       // EAST, exterior
                    default -> ramp((4 * face - 1) - lx);       // SOUTH: interno en el borde derecho
                };
            }
            return 1.0F;
        }

        if (shading == Shading.ARMS) {
            // Mismo mecanismo que LEGS -mismas proporciones de cuboide
            // (ancho=prof=4, alto=12)-, trasladado al UV de los brazos:
            // BRAZO_DER uv(40S,16S), BRAZO_IZQ uv(32S,48S) (ver CuerpoGeometria).
            // Brazo derecho: caras laterales en y 20S..32S-1, x 40S..56S-1
            if (y >= 20 * S && y < 32 * S && x >= 40 * S && x < 56 * S) {
                int lx = x - 40 * S;
                int col = lx / face;
                return switch (col) {
                    case 0 -> FACE_LIGHT;                       // WEST, exterior
                    case 1 -> ramp((2 * face - 1) - lx);        // NORTH: interno en el borde derecho
                    case 2 -> FACE_DARK;                        // EAST, interior
                    default -> ramp(lx - 3 * face);             // SOUTH: interno en el borde izquierdo
                };
            }
            // Brazo izquierdo: caras laterales en y 52S..64S-1, x 32S..48S-1
            if (y >= 52 * S && y < 64 * S && x >= 32 * S && x < 48 * S) {
                int lx = x - 32 * S;
                int col = lx / face;
                return switch (col) {
                    case 0 -> FACE_DARK;                        // WEST, interior
                    case 1 -> ramp(lx - face);                  // NORTH: interno en el borde izquierdo
                    case 2 -> FACE_LIGHT;                       // EAST, exterior
                    default -> ramp((4 * face - 1) - lx);       // SOUTH: interno en el borde derecho
                };
            }
            return 1.0F;
        }

        return 1.0F;
    }

    /**
     * Degrade a lo ancho de una cara frontal o trasera, de oscuro en el borde
     * interno a claro en el externo.
     *
     * Una sola columna oscura cortaba demasiado. Repartirlo en las 4 columnas
     * ademas se parece a como cae la luz en una pierna redonda: sombra del
     * lado que da a la otra pierna, luz del lado de afuera.
     *
     * @param fromInner 0 en el borde interno, 3 en el externo.
     */
    private static float ramp(int fromInner) {
        int span = FACE_COLUMNS - 1;
        float t = Math.min(Math.max(fromInner, 0), span) / (float) span;
        return FACE_DARK + (FACE_LIGHT - FACE_DARK) * t;
    }

    /** Aclara u oscurece un pixel ABGR sin tocarle el alpha. */
    private static int shade(int abgr, float f) {
        if (f == 1.0F) return abgr;
        int a = abgr & 0xFF000000;
        int r = Math.min(255, Math.round((abgr & 0xFF) * f));
        int g = Math.min(255, Math.round(((abgr >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round(((abgr >> 16) & 0xFF) * f));
        return a | (b << 16) | (g << 8) | r;
    }

    /**
     * Multiplica un pixel ABGR por un color RGB (el tinte estilo cuero).
     *
     * Publico porque EstampaTextures pinta patrones sobre remera con esta
     * misma cuenta: es matematica pura de pixeles, no hay motivo para
     * duplicarla en otro paquete.
     */
    public static int tintPixel(int px, int rgb) {
        int a = (px >> 24) & 0xFF;
        int bChan = (px >> 16) & 0xFF, gChan = (px >> 8) & 0xFF, rChan = px & 0xFF;
        int dr = (rgb >> 16) & 0xFF, dg = (rgb >> 8) & 0xFF, db = rgb & 0xFF;
        return (a << 24) | (((bChan * db) / 255) << 16) | (((gChan * dg) / 255) << 8) | ((rChan * dr) / 255);
    }

    /**
     * Algo que se pinta ENCIMA de la prenda ya compuesta.
     *
     * Existe para que la sublimadora pueda meter la foto sin que este cache
     * sepa nada de estampas: recibe la imagen terminada y la firma con la que
     * distinguirla de otra. Asi la dependencia va en un solo sentido.
     */
    public interface Encima {
        /** Parte de la clave: dos estampas distintas no comparten textura. */
        String clave();
        /** false si todavia no se pudo -foto sin bajar-, y entonces no se cachea. */
        boolean aplicar(NativeImage destino);
    }

    /**
     * Todo junto: base tenida + patron encima. Lo que la prenda no cubre
     * queda TRANSPARENTE.
     *
     * Esa transparencia es el contrato del sistema de capas. Hasta la fase 1
     * habia un parametro mas —un tono de piel con el que rellenar el hueco—
     * porque cada prenda tenia que reconstruir sola la pierna desnuda de
     * arriba. Rellenar era justo lo que hacia que dos prendas en la misma
     * pierna se taparan por completo: cada una pintaba la pierna entera. Ese
     * hueco ahora lo llena el cuerpo base, dibujado una sola vez abajo de
     * todo.
     *
     * mask puede ser null (prenda lisa).
     */
    public static Identifier composeGarment(Identifier baseTexture, int baseRgb,
                                            @Nullable Identifier mask, int patternRgb,
                                            Shading shading) {
        return composeGarment(baseTexture, baseRgb, mask, patternRgb, shading, null);
    }

    public static Identifier composeGarment(Identifier baseTexture, int baseRgb,
                                            @Nullable Identifier mask, int patternRgb,
                                            Shading shading,
                                            @Nullable Encima encima) {
        String key = baseTexture + "#" + Integer.toHexString(baseRgb)
                + "@" + mask + "#" + Integer.toHexString(patternRgb) + ":" + shading
                + (encima == null ? "" : "+" + encima.clave());
        Identifier cached = TINTED_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = imagenBase(baseTexture);
        if (base == null) return baseTexture;

        NativeImage maskImg = mask != null ? imagenBase(mask) : null;

        NativeImage composite = new NativeImage(base.getWidth(), base.getHeight(), true);
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                // El factor por cara se aplica a la TELA: si la prenda es un
                // color plano, las dos piernas se leen pegadas, sin volumen
                // ni separacion entre una y otra.
                float f = faceFactor(x, y, shading);
                if (maskImg != null && x < maskImg.getWidth() && y < maskImg.getHeight()) {
                    int maskPx = maskImg.getColor(x, y);
                    if (((maskPx >> 24) & 0xFF) != 0) {
                        composite.setColor(x, y, shade(tintPixel(maskPx, patternRgb), f));
                        continue;
                    }
                }
                int basePx = base.getColor(x, y);
                if (((basePx >> 24) & 0xFF) != 0) {
                    composite.setColor(x, y, shade(tintPixel(basePx, baseRgb), f));
                } else {
                    composite.setColor(x, y, 0);
                }
            }
        }

        // La estampa va DESPUES del tenido y del patron, igual que en una
        // sublimadora de verdad: la foto se imprime sobre la prenda terminada
        // y no se tine con ella.
        if (encima != null && !encima.aplicar(composite)) {
            composite.close();
            return baseTexture;
        }

        if (DEBUG_DUMP) volcarADisco(key, composite);

        // maskImg y no mask: si el patron se pidio pero le falta el PNG, el
        // bucle de arriba ya cayo a lisa (misma trampa que documenta
        // tintedWithPattern), asi que el tamano tiene que seguirla.
        composite = reducirSiHaceFalta(composite, encima != null, maskImg != null);

        Identifier id = Identifier.of("femclothes", "dynamic/garment_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        TINTED_CACHE.put(key, id);
        return id;
    }

    /**
     * Achica la imagen ya compuesta si no necesita toda su resolucion nativa.
     *
     * La composicion (tenido, patron, estampa) siempre corre a la resolucion
     * NATIVA de la textura base (`ESCALA_TELA`, 8x): tocar esa matematica por
     * el ahorro de memoria arriesgaria el sombreado y el recorte de la
     * estampa por nada, ya que estampar es justo el caso que SI necesita esa
     * resolucion. Achicar la imagen DESPUES de componerla, en vez de componer
     * a menos resolucion desde el principio, deja fuera de discusion romper
     * esa matematica.
     *
     * Sin foto y sin patron la prenda es un color plano: 2x. Con patron pero
     * sin foto, un poco mas de detalle para el borde del patron: 4x. Con
     * foto, la resolucion nativa completa — es la unica que de verdad la
     * necesita.
     *
     * `resizeSubRectTo` es de vainilla (usa STBIR, no vecino-mas-cercano), asi
     * que el degrade de {@link #faceFactor} sobrevive el achique, mezclado en
     * vez de cortado en bandas.
     */
    private static NativeImage reducirSiHaceFalta(NativeImage composite, boolean estampada, boolean tienePatron) {
        if (estampada) return composite;

        int escalaDestino = tienePatron ? CuerpoGeometria.ESCALA_TELA_PATRON : CuerpoGeometria.ESCALA_TELA_LISA;
        float factor = escalaDestino / (float) CuerpoGeometria.ESCALA_TELA;
        int anchoDestino = Math.max(1, Math.round(composite.getWidth() * factor));
        int altoDestino = Math.max(1, Math.round(composite.getHeight() * factor));
        if (anchoDestino >= composite.getWidth() && altoDestino >= composite.getHeight()) return composite;

        NativeImage reducida = new NativeImage(anchoDestino, altoDestino, true);
        composite.resizeSubRectTo(0, 0, composite.getWidth(), composite.getHeight(), reducida);
        composite.close();
        return reducida;
    }

    /** Como sombrear la tela segun a que parte del cuerpo va. */
    public enum Shading { NONE, LEGS, ARMS }

    /**
     * El png crudo de una textura del resource pack, cacheado.
     *
     * Publico porque el compositor del cuerpo base lee los mismos archivos
     * (el mapa de sombras del cuerpo, la ropa interior) y no tiene sentido
     * que cada compositor mantenga su propio cache de imagenes de disco.
     */
    public static NativeImage imagenBase(Identifier id) {
        if (BASE_IMAGE_CACHE.containsKey(id)) return BASE_IMAGE_CACHE.get(id);
        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                BASE_IMAGE_CACHE.put(id, null);
                return null;
            }
            try (InputStream stream = resource.get().getInputStream()) {
                NativeImage img = NativeImage.read(stream);
                BASE_IMAGE_CACHE.put(id, img);
                return img;
            }
        } catch (IOException e) {
            BASE_IMAGE_CACHE.put(id, null);
            return null;
        }
    }
}
