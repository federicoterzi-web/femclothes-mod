package com.ejemplo.sublimadora;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Compone la textura del full print: la foto recortada para llenar, con el
 * alfa de la silueta de la remera.
 *
 * Existe porque dibujar la foto como un cuadrado sobre la remera no puede
 * cubrir las mangas. La remera tiene forma de T, y cualquier rectangulo que
 * llegue hasta las mangas se sale del contorno por las esquinas. La unica
 * forma de que la estampa termine exactamente donde termina la tela es
 * multiplicar la foto por el alfa de la prenda.
 *
 * Se compone a 16x16, la resolucion del sprite del item. La foto se ve
 * gruesa, pero es la resolucion a la que se dibuja todo lo demas del item, y
 * asi el borde de la silueta queda pixel a pixel igual al de la remera lisa
 * en vez de ser mas fino que ella.
 */
final class EstampaTextures {

    private EstampaTextures() {}

    private static final Identifier SPRITE =
            Identifier.of("sublimadora", "textures/item/remera.png");

    private static final Identifier TELA_CUERPO =
            Identifier.of("sublimadora", "textures/entity/remera_cuerpo.png");

    /**
     * Una cara de la prenda: donde vive en la textura, y que pedazo del
     * dibujo le toca.
     *
     * El full print no le da a cada cara su propio recorte de la foto: eso
     * dejaba las mangas con un trozo suelto que no continuaba el torso. En
     * vez de eso hay un LIENZO virtual de 16x12 que es la remera vista de
     * frente -manga, torso, manga- y cada cara toma su rectangulo de ese
     * lienzo. Asi el motivo cruza del torso a la manga sin cortarse.
     *
     * @param rect       {u, v, ancho, alto} en la textura, en unidades de skin
     * @param lienzo     {x, y, ancho, alto} dentro del lienzo
     * @param atras      de que estampa es: la de la espalda o la del frente
     * @param espejar    si el eje horizontal va invertido. Va aparte de
     *                   atras porque no son lo mismo: la mitad trasera de un
     *                   costado PERTENECE a la estampa de atras pero se mira
     *                   de perfil, no desde atras, asi que no se espeja. Solo
     *                   se espejan las caras que de verdad se ven desde
     *                   atras: la espalda del torso y las traseras de las
     *                   mangas.
     * @param compartida caras sin lado propio, que pinta el full print que
     *                   haya. Despues de partir todo lo demas queda solo el
     *                   ruedo, que es la boca de abajo de la remera.
     */
    private record Cara(int[] rect, int[] lienzo, boolean atras,
                        boolean espejar, boolean espejarV, boolean compartida) {
        Cara(int[] rect, int[] lienzo, boolean atras) {
            this(rect, lienzo, atras, false, false, false);
        }
        Cara espejada() {
            return new Cara(rect, lienzo, atras, true, espejarV, compartida);
        }
        /**
         * Para las caras HORIZONTALES de atras: los hombros y las tapas de las
         * mangas. La mitad delantera de una tapa va de la costura hacia el
         * pecho, o sea bajando por el lienzo. La trasera tiene que ir de la
         * costura hacia la ESPALDA, que en el lienzo tambien es bajar, pero su
         * eje v corre al reves. Sin invertirlo repite la franja de arriba en
         * vez de continuar, y las dos mitades se ven iguales.
         */
        Cara volteada() {
            return new Cara(rect, lienzo, atras, espejar, true, compartida);
        }
        Cara compartida_() {
            return new Cara(rect, lienzo, atras, espejar, espejarV, true);
        }
    }

    private static final int LIENZO_ANCHO = 24;
    private static final int LIENZO_ALTO = 14;

    private static final int[] TORSO_FRENTE = { 20, 20, 8, 12 };
    private static final int[] TORSO_ESPALDA = { 32, 20, 8, 12 };

    /**
     * Cada cara con su ventana. La regla es que dos caras VISIBLES no pueden
     * compartir rectangulo: si lo hacen, se ve el mismo pedazo de foto dos
     * veces y la prenda deja de leerse como una sola imagen.
     *
     * Las unicas superposiciones que quedan son a proposito, entre superficies
     * que estan en el mismo lugar del cuerpo una delante de la otra: la
     * externa de la manga contra el costado del torso, y la interna de la
     * manga contra el frente del torso. Ahi compartir es justamente lo que da
     * continuidad, y la de adentro no se ve nunca.
     */
    private static final Cara[] PRENDA = {
            // ── torso ────────────────────────────────────────────────
            new Cara(TORSO_FRENTE,                new int[] { 8, 2, 8, 12 }, false),
            new Cara(TORSO_ESPALDA,               new int[] { 8, 2, 8, 12 }, true).espejada(),
            new Cara(new int[] { 20, 18, 8, 2 },  new int[] { 8, 0, 8, 2 }, false),   // hombro delantero
            new Cara(new int[] { 20, 16, 8, 2 },  new int[] { 8, 0, 8, 2 }, true).volteada(),  // hombro trasero
            // Los costados partidos al medio en el eje de profundidad. En la
            // plantilla el borde que toca la cara frontal es el del FRENTE:
            // u=20 para el costado derecho, u=28 para el izquierdo.
            new Cara(new int[] { 18, 20, 2, 12 }, new int[] { 2, 2, 2, 12 }, false),  // costado der delantero
            new Cara(new int[] { 16, 20, 2, 12 }, new int[] { 0, 2, 2, 12 }, true),   // costado der trasero
            new Cara(new int[] { 28, 20, 2, 12 }, new int[] { 20, 2, 2, 12 }, false), // costado izq delantero
            new Cara(new int[] { 30, 20, 2, 12 }, new int[] { 22, 2, 2, 12 }, true),  // costado izq trasero
            new Cara(new int[] { 28, 16, 8, 4 },  new int[] { 8, 12, 8, 2 }, false).compartida_(), // ruedo

            // ── manga derecha, brazo en -X ───────────────────────────
            new Cara(new int[] { 44, 20, 4, 4 }, new int[] { 4, 2, 4, 4 }, false),           // delantera
            new Cara(new int[] { 52, 20, 4, 4 }, new int[] { 4, 2, 4, 4 }, true).espejada(), // trasera
            new Cara(new int[] { 42, 20, 2, 4 }, new int[] { 2, 2, 2, 4 }, false),  // externa delantera
            new Cara(new int[] { 40, 20, 2, 4 }, new int[] { 0, 2, 2, 4 }, true),   // externa trasera
            new Cara(new int[] { 48, 20, 2, 4 }, new int[] { 8, 2, 2, 4 }, false),  // interna delantera
            new Cara(new int[] { 50, 20, 2, 4 }, new int[] { 8, 2, 2, 4 }, true),   // interna trasera
            new Cara(new int[] { 44, 18, 4, 2 }, new int[] { 4, 0, 4, 2 }, false),  // superior delantera
            new Cara(new int[] { 44, 16, 4, 2 }, new int[] { 4, 0, 4, 2 }, true).volteada(),  // superior trasera

            // ── manga izquierda, brazo en +X ─────────────────────────
            // Ojo: el desdoblado pone interna y externa en columnas distintas
            // que en la derecha, porque el orden es siempre der/frente/izq/
            // atras en coordenadas del modelo.
            new Cara(new int[] { 36, 52, 4, 4 }, new int[] { 16, 2, 4, 4 }, false),           // delantera
            new Cara(new int[] { 44, 52, 4, 4 }, new int[] { 16, 2, 4, 4 }, true).espejada(), // trasera
            new Cara(new int[] { 40, 52, 2, 4 }, new int[] { 20, 2, 2, 4 }, false), // externa delantera
            new Cara(new int[] { 42, 52, 2, 4 }, new int[] { 22, 2, 2, 4 }, true),  // externa trasera
            new Cara(new int[] { 34, 52, 2, 4 }, new int[] { 14, 2, 2, 4 }, false), // interna delantera
            new Cara(new int[] { 32, 52, 2, 4 }, new int[] { 14, 2, 2, 4 }, true),  // interna trasera
            new Cara(new int[] { 36, 50, 4, 2 }, new int[] { 16, 0, 4, 2 }, false), // superior delantera
            new Cara(new int[] { 36, 48, 4, 2 }, new int[] { 16, 0, 4, 2 }, true).volteada(), // superior trasera
    };

    private static int[] escalar(int[] r) {
        int e = RemeraTrinketRenderer.ESCALA;
        return new int[] { r[0] * e, r[1] * e, r[2] * e, r[3] * e };
    }

    private static final Map<UUID, Identifier> CACHE = new HashMap<>();
    private static final Map<String, Identifier> CACHE_CUERPO = new HashMap<>();
    @Nullable
    private static NativeImage telaCuerpo;
    private static boolean telaCuerpoIntentada;
    /** UUIDs que ya fallaron, para no reintentar la lectura en cada frame. */
    private static final Map<Object, Boolean> FALLADAS = new HashMap<>();

    @Nullable
    private static NativeImage mascara;
    private static boolean mascaraIntentada;

    /**
     * Textura de la remera con la foto adentro, o null si todavia no se pudo
     * componer. Devolver null es una respuesta valida: el renderer cae al
     * cuadrado recortado al torso, que es peor pero nunca se ve roto.
     */
    @Nullable
    static Identifier fullPrint(UUID id, Identifier texturaFoto, int ancho, int alto) {
        Identifier hecha = CACHE.get(id);
        if (hecha != null) return hecha;
        if (FALLADAS.containsKey(id)) return null;

        NativeImage silueta = mascara();
        if (silueta == null) {
            FALLADAS.put(id, true);
            return null;
        }
        NativeImage foto = leerDeLaGpu(texturaFoto, ancho, alto);
        if (foto == null) {
            // Sin marcarla como fallada: puede ser que la textura todavia no
            // este subida y ande en el proximo frame.
            return null;
        }

        try {
            NativeImage compuesta = componer(silueta, foto);
            Identifier destino = Identifier.of("sublimadora", "estampada/" + id);
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(destino, new NativeImageBackedTexture(compuesta));
            CACHE.put(id, destino);
            return destino;
        } catch (Throwable e) {
            FALLADAS.put(id, true);
            return null;
        } finally {
            foto.close();
        }
    }

    /**
     * Recorta la foto para llenar el cuadrado de la remera y le aplica el
     * alfa de la silueta.
     *
     * NativeImage empaqueta ABGR, no ARGB: el rojo esta en los bits bajos.
     * Por eso el color se copia entero y solo se rearma el byte de alfa, en
     * vez de descomponer y volver a componer los canales.
     */
    private static NativeImage componer(NativeImage silueta, NativeImage foto) {
        int lado = silueta.getWidth();
        NativeImage salida = new NativeImage(NativeImage.Format.RGBA, lado, silueta.getHeight(), false);

        // Cuadrado centrado de la foto: se descarta el eje que sobra.
        int corte = Math.min(foto.getWidth(), foto.getHeight());
        int desdeX = (foto.getWidth() - corte) / 2;
        int desdeY = (foto.getHeight() - corte) / 2;

        for (int y = 0; y < salida.getHeight(); y++) {
            for (int x = 0; x < lado; x++) {
                int alfaPrenda = (silueta.getColor(x, y) >>> 24) & 0xFF;
                if (alfaPrenda == 0) {
                    salida.setColor(x, y, 0);
                    continue;
                }
                int fx = desdeX + x * corte / lado;
                int fy = desdeY + y * corte / salida.getHeight();
                int pixel = foto.getColor(fx, fy);
                int alfaFoto = (pixel >>> 24) & 0xFF;
                int alfa = alfaFoto * alfaPrenda / 255;
                salida.setColor(x, y, (alfa << 24) | (pixel & 0x00FFFFFF));
            }
        }
        return salida;
    }

    /**
     * La remera del cuerpo con las estampas ya pintadas encima, o null si no
     * hay ninguna o no se pudo componer.
     *
     * Se compone una textura entera en vez de dibujar cuadrados sobre el
     * cuerpo porque la prenda YA es una textura en layout de skin: meter la
     * foto adentro de ella la hace seguir al cuerpo sin geometria extra, y de
     * paso queda recortada a la tela sola.
     */
    @Nullable
    static Identifier cuerpoEstampado(@Nullable Estampa frente, @Nullable Estampa espalda) {
        if (frente == null && espalda == null) return null;
        String clave = frente + "|" + espalda;
        Identifier hecha = CACHE_CUERPO.get(clave);
        if (hecha != null) return hecha;
        if (FALLADAS.containsKey(clave)) return null;

        NativeImage base = telaCuerpo();
        if (base == null) {
            FALLADAS.put(clave, true);
            return null;
        }
        NativeImage salida = new NativeImage(NativeImage.Format.RGBA,
                base.getWidth(), base.getHeight(), false);
        salida.copyFrom(base);

        // Si alguna foto todavia no esta lista se abandona SIN cachear: en el
        // proximo frame puede estar, y cachear a medias dejaria una remera con
        // una sola de sus dos estampas para siempre.
        // El full print que se lleva las caras sin lado propio. Se prefiere el
        // de adelante nada mas que por desempatar.
        Estampa cubreCompartidas = frente != null && frente.cubrir() ? frente
                : (espalda != null && espalda.cubrir() ? espalda : null);

        boolean listo = true;
        for (Cara cara : PRENDA) {
            // Cada cara la pinta la estampa de SU lado. Antes un full print
            // en el frente pintaba tambien la espalda, porque el bucle usaba
            // la misma estampa para todas.
            Estampa suya = cara.compartida() ? cubreCompartidas
                    : (cara.atras() ? espalda : frente);
            if (suya == null) continue;

            if (suya.cubrir()) {
                listo &= pintarDelLienzo(salida, suya, cara);
            } else if (!cara.compartida()) {
                // Logo y centrada solo van en el torso, no en los costados.
                listo &= pintar(salida, suya, escalar(cara.rect()));
            }
        }
        if (!listo) {
            salida.close();
            return null;
        }

        Identifier destino = Identifier.of("sublimadora",
                "cuerpo/" + Long.toHexString(clave.hashCode() & 0xFFFFFFFFL));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(destino, new NativeImageBackedTexture(salida));
        CACHE_CUERPO.put(clave, destino);
        return destino;
    }

    /**
     * Pinta el pedazo de foto que le toca a una cara segun el lienzo.
     *
     * La foto se recorta una sola vez para cubrir el lienzo de 16x12 -no cara
     * por cara- y de ese recorte cada cara toma su ventana. Es lo que hace
     * que el dibujo continue de una cara a la otra en vez de repetirse.
     */
    private static boolean pintarDelLienzo(NativeImage salida, Estampa estampa, Cara cara) {
        CameraptureClientCompat.Foto info = fotoDe(estampa.foto());
        if (info == null) return false;
        NativeImage foto = leerDeLaGpu(info.textura(), info.ancho(), info.alto());
        if (foto == null) return false;

        try {
            // Recorte que hace entrar el lienzo entero adentro de la foto.
            float pu0 = 0f, pv0 = 0f, pu1 = 1f, pv1 = 1f;
            float relFoto = (float) info.ancho() / info.alto();
            float relLienzo = (float) LIENZO_ANCHO / LIENZO_ALTO;
            if (relFoto > relLienzo) {
                float visible = relLienzo / relFoto;
                pu0 = (1f - visible) / 2f;
                pu1 = pu0 + visible;
            } else if (relFoto < relLienzo) {
                float visible = relFoto / relLienzo;
                pv0 = (1f - visible) / 2f;
                pv1 = pv0 + visible;
            }

            int[] l = cara.lienzo();
            float u0 = pu0 + (pu1 - pu0) * l[0] / LIENZO_ANCHO;
            float u1 = pu0 + (pu1 - pu0) * (l[0] + l[2]) / LIENZO_ANCHO;
            float v0 = pv0 + (pv1 - pv0) * l[1] / LIENZO_ALTO;
            float v1 = pv0 + (pv1 - pv0) * (l[1] + l[3]) / LIENZO_ALTO;
            // Solo las caras que se MIRAN desde atras van espejadas, no todas
            // las que pertenecen a la estampa de atras.
            if (cara.espejar()) {
                float t = u0;
                u0 = u1;
                u1 = t;
            }
            if (cara.espejarV()) {
                float t = v0;
                v0 = v1;
                v1 = t;
            }

            int[] r = escalar(cara.rect());
            volcar(salida, foto, r[0], r[1], r[2], r[3], u0, v0, u1, v1);
            return true;
        } finally {
            foto.close();
        }
    }

    /** Vuelca una ventana de la foto sobre un rectangulo de la textura. */
    private static void volcar(NativeImage salida, NativeImage foto,
                               int rx, int ry, int rw, int rh,
                               float u0, float v0, float u1, float v1) {
        for (int y = 0; y < rh; y++) {
            for (int x = 0; x < rw; x++) {
                int fondo = salida.getColor(rx + x, ry + y);
                int alfaPrenda = (fondo >>> 24) & 0xFF;
                if (alfaPrenda == 0) continue;   // fuera de la tela

                int fx = (int) ((u0 + (u1 - u0) * (x + 0.5f) / rw) * foto.getWidth());
                int fy = (int) ((v0 + (v1 - v0) * (y + 0.5f) / rh) * foto.getHeight());
                fx = Math.clamp(fx, 0, foto.getWidth() - 1);
                fy = Math.clamp(fy, 0, foto.getHeight() - 1);
                int pixel = foto.getColor(fx, fy);
                int alfaFoto = (pixel >>> 24) & 0xFF;
                if (alfaFoto == 0) continue;
                salida.setColor(rx + x, ry + y, (alfaPrenda << 24) | sobre(fondo, pixel, alfaFoto));
            }
        }
    }

    /**
     * Pinta una estampa adentro de su rectangulo respetando el alfa de la
     * prenda. Devuelve false si la foto todavia no se pudo leer.
     */
    private static boolean pintar(NativeImage salida, @Nullable Estampa estampa, int[] rect) {
        if (estampa == null) return true;
        CameraptureClientCompat.Foto info = fotoDe(estampa.foto());
        if (info == null) return false;
        NativeImage foto = leerDeLaGpu(info.textura(), info.ancho(), info.alto());
        if (foto == null) return false;

        try {
            int rx = rect[0], ry = rect[1], rw = rect[2], rh = rect[3];
            int cajaW, cajaH;
            float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;
            if (estampa.cubrir()) {
                // Cubrir: la caja es la cara entera y lo que se achica es el
                // pedazo de foto que se muestra, recortado desde el centro.
                cajaW = rw;
                cajaH = rh;
                float relFoto = (float) info.ancho() / info.alto();
                float relCaja = (float) rw / rh;
                if (relFoto > relCaja) {
                    float visible = relCaja / relFoto;
                    u0 = (1f - visible) / 2f;
                    u1 = u0 + visible;
                } else if (relFoto < relCaja) {
                    float visible = relFoto / relCaja;
                    v0 = (1f - visible) / 2f;
                    v1 = v0 + visible;
                }
            } else {
                // Contener: la foto entra ENTERA y conserva su relacion de
                // aspecto. Antes la caja era cuadrada y la foto se deformaba
                // para llenarla, que es lo que se veia mal en la centrada.
                float lado = rw * estampa.escala();
                float relFoto = (float) info.ancho() / info.alto();
                cajaW = Math.max(1, Math.round(relFoto >= 1f ? lado : lado * relFoto));
                cajaH = Math.max(1, Math.round(relFoto >= 1f ? lado / relFoto : lado));
            }
            // La y de la Estampa crece hacia ARRIBA y la de la textura hacia
            // abajo: de ahi el signo cambiado.
            int cx = Math.round(rx + rw / 2f + estampa.x() * rw);
            int cy = Math.round(ry + rh / 2f - estampa.y() * rh);
            int x0 = cx - cajaW / 2, y0 = cy - cajaH / 2;

            for (int y = 0; y < cajaH; y++) {
                for (int x = 0; x < cajaW; x++) {
                    int px = x0 + x, py = y0 + y;
                    if (px < rx || px >= rx + rw || py < ry || py >= ry + rh) continue;
                    int fondo = salida.getColor(px, py);
                    int alfaPrenda = (fondo >>> 24) & 0xFF;
                    if (alfaPrenda == 0) continue;   // fuera de la tela

                    int fx = (int) ((u0 + (u1 - u0) * x / cajaW) * foto.getWidth());
                    int fy = (int) ((v0 + (v1 - v0) * y / cajaH) * foto.getHeight());
                    fx = Math.min(fx, foto.getWidth() - 1);
                    fy = Math.min(fy, foto.getHeight() - 1);
                    int pixel = foto.getColor(fx, fy);
                    int alfaFoto = (pixel >>> 24) & 0xFF;
                    if (alfaFoto == 0) continue;
                    salida.setColor(px, py, (alfaPrenda << 24) | sobre(fondo, pixel, alfaFoto));
                }
            }
            return true;
        } finally {
            foto.close();
        }
    }

    /** Mezcla los tres canales bajos de ABGR, dejando el alfa afuera. */
    private static int sobre(int fondo, int encima, int alfa) {
        int salida = 0;
        for (int desp = 0; desp < 24; desp += 8) {
            int f = (fondo >>> desp) & 0xFF;
            int e = (encima >>> desp) & 0xFF;
            salida |= (((e * alfa + f * (255 - alfa)) / 255) & 0xFF) << desp;
        }
        return salida;
    }

    @Nullable
    private static CameraptureClientCompat.Foto fotoDe(UUID id) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("camerapture")) return null;
        try {
            return CameraptureClientCompat.foto(id);
        } catch (Throwable ignorado) {
            return null;
        }
    }

    @Nullable
    private static NativeImage telaCuerpo() {
        if (telaCuerpoIntentada) return telaCuerpo;
        telaCuerpoIntentada = true;
        telaCuerpo = leerRecurso(TELA_CUERPO);
        return telaCuerpo;
    }

    /** El sprite de la remera lisa, de donde sale la silueta. */
    @Nullable
    private static NativeImage mascara() {
        if (mascaraIntentada) return mascara;
        mascaraIntentada = true;
        mascara = leerRecurso(SPRITE);
        return mascara;
    }

    @Nullable
    private static NativeImage leerRecurso(Identifier id) {
        try {
            Resource recurso = MinecraftClient.getInstance().getResourceManager()
                    .getResourceOrThrow(id);
            try (InputStream in = recurso.getInputStream()) {
                return NativeImage.read(in);
            }
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Los pixeles de una textura ya subida a la placa.
     *
     * La foto de Camerapture no es un recurso del resource pack: se registra
     * en runtime, asi que la unica forma de leerla es pedirsela de vuelta a
     * la GPU. El tamano se consulta a OpenGL y no al mod, porque la textura
     * puede estar paddeada y leer con el tamano equivocado corrompe la imagen.
     */
    @Nullable
    private static NativeImage leerDeLaGpu(Identifier textura, int anchoSugerido, int altoSugerido) {
        if (!RenderSystem.isOnRenderThread()) return null;
        try {
            AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(textura);
            if (tex == null) return null;
            int glId = tex.getGlId();
            if (glId <= 0) return null;

            RenderSystem.bindTexture(glId);
            int ancho = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int alto = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (ancho <= 0 || alto <= 0) {
                ancho = anchoSugerido;
                alto = altoSugerido;
            }
            if (ancho <= 0 || alto <= 0) return null;

            NativeImage imagen = new NativeImage(ancho, alto, false);
            imagen.loadFromTextureImage(0, false);
            return imagen;
        } catch (Throwable e) {
            return null;
        }
    }
}
