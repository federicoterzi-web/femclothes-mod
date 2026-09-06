package com.femclothes.sublimadora;

import com.femclothes.Femclothes;
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
    /** Las filas de arriba del lienzo son el hombro, no el cuerpo. */
    private static final int BANDA_HOMBRO = 2;
    // El largo del torso y el de las mangas salen de la Variante, asi que el
    // lienzo y la tabla de caras se arman por corte. Tienen que coincidir con
    // lo que pinta el generador de texturas: si el lienzo cuenta 12 y la tela
    // llega a 9, la estampa se encuadra sobre prenda que no existe.
    //
    // Manda el mas largo de los dos, no el torso: una manga larga sobre un
    // croptop baja mas que el ruedo, y si el lienzo terminara en el ruedo la
    // manga se saldria de el.
    private static int lienzoAlto(Variante v) {
        return BANDA_HOMBRO + Math.max(v.largo().filas, v.manga().filas);
    }

    /**
     * Las caras de un corte, con su ventana del lienzo.
     *
     * Se arma por variante porque el largo del torso cambia y la musculosa no
     * tiene mangas. La regla sigue siendo que dos caras VISIBLES no compartan
     * rectangulo: si lo hacen, se ve el mismo pedazo de foto dos veces.
     *
     * Las unicas superposiciones que quedan son a proposito, entre
     * superficies que estan en el mismo lugar del cuerpo una delante de la
     * otra: la externa de la manga contra el costado del torso, y la interna
     * contra el frente. Ahi compartir es lo que da continuidad, y la de
     * adentro no se ve nunca.
     */
    private static Cara[] caras(Variante v) {
        Cara[] hechas = CARAS.get(v);
        if (hechas != null) return hechas;

        int largo = v.largo().filas;
        int manga = v.manga().filas;
        java.util.List<Cara> lista = new java.util.ArrayList<>();

        // ── torso ────────────────────────────────────────────────────
        lista.add(new Cara(new int[] { 20, 20, 8, largo }, new int[] { 8, 2, 8, largo }, false));
        lista.add(new Cara(new int[] { 32, 20, 8, largo }, new int[] { 8, 2, 8, largo }, true).espejada());
        lista.add(new Cara(new int[] { 20, 18, 8, 2 }, new int[] { 8, 0, 8, 2 }, false));            // hombro delantero
        lista.add(new Cara(new int[] { 20, 16, 8, 2 }, new int[] { 8, 0, 8, 2 }, true).volteada());  // hombro trasero
        lista.add(new Cara(new int[] { 18, 20, 2, largo }, new int[] { 2, 2, 2, largo }, false));    // costado der delantero
        lista.add(new Cara(new int[] { 16, 20, 2, largo }, new int[] { 0, 2, 2, largo }, true));     // costado der trasero
        lista.add(new Cara(new int[] { 28, 20, 2, largo }, new int[] { 20, 2, 2, largo }, false));   // costado izq delantero
        lista.add(new Cara(new int[] { 30, 20, 2, largo }, new int[] { 22, 2, 2, largo }, true));    // costado izq trasero

        if (v.tieneMangas()) {
            // manga derecha, brazo en -X
            lista.add(new Cara(new int[] { 44, 20, 4, manga }, new int[] { 4, 2, 4, manga }, false));
            lista.add(new Cara(new int[] { 52, 20, 4, manga }, new int[] { 4, 2, 4, manga }, true).espejada());
            lista.add(new Cara(new int[] { 42, 20, 2, manga }, new int[] { 2, 2, 2, manga }, false));   // externa delantera
            lista.add(new Cara(new int[] { 40, 20, 2, manga }, new int[] { 0, 2, 2, manga }, true));    // externa trasera
            lista.add(new Cara(new int[] { 48, 20, 2, manga }, new int[] { 8, 2, 2, manga }, false));   // interna delantera
            lista.add(new Cara(new int[] { 50, 20, 2, manga }, new int[] { 8, 2, 2, manga }, true));    // interna trasera
            lista.add(new Cara(new int[] { 44, 18, 4, 2 }, new int[] { 4, 0, 4, 2 }, false));            // superior delantera
            lista.add(new Cara(new int[] { 44, 16, 4, 2 }, new int[] { 4, 0, 4, 2 }, true).volteada());  // superior trasera

            // manga izquierda, brazo en +X. Ojo: el desdoblado pone interna y
            // externa en columnas distintas que en la derecha, porque el orden
            // es siempre der/frente/izq/atras en coordenadas del modelo.
            lista.add(new Cara(new int[] { 36, 52, 4, manga }, new int[] { 16, 2, 4, manga }, false));
            lista.add(new Cara(new int[] { 44, 52, 4, manga }, new int[] { 16, 2, 4, manga }, true).espejada());
            lista.add(new Cara(new int[] { 40, 52, 2, manga }, new int[] { 20, 2, 2, manga }, false));  // externa delantera
            lista.add(new Cara(new int[] { 42, 52, 2, manga }, new int[] { 22, 2, 2, manga }, true));   // externa trasera
            lista.add(new Cara(new int[] { 34, 52, 2, manga }, new int[] { 14, 2, 2, manga }, false));  // interna delantera
            lista.add(new Cara(new int[] { 32, 52, 2, manga }, new int[] { 14, 2, 2, manga }, true));   // interna trasera
            lista.add(new Cara(new int[] { 36, 50, 4, 2 }, new int[] { 16, 0, 4, 2 }, false));            // superior delantera
            lista.add(new Cara(new int[] { 36, 48, 4, 2 }, new int[] { 16, 0, 4, 2 }, true).volteada());  // superior trasera
        }

        hechas = lista.toArray(new Cara[0]);
        CARAS.put(v, hechas);
        return hechas;
    }

    private static final Map<Variante, Cara[]> CARAS = new HashMap<>();

    /** El pecho y la espalda del corte, que es donde van logo y centrada. */
    private static boolean esElTorso(Cara cara) {
        return cara.rect()[0] == 20 && cara.rect()[2] == 8 && cara.rect()[1] == 20
                || cara.rect()[0] == 32;
    }

    private static int[] escalar(int[] r) {
        int e = RemeraTrinketRenderer.ESCALA;
        return new int[] { r[0] * e, r[1] * e, r[2] * e, r[3] * e };
    }

    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final Map<String, Identifier> CACHE_CUERPO = new HashMap<>();
    /** UUIDs que ya fallaron, para no reintentar la lectura en cada frame. */
    private static final Map<Object, Boolean> FALLADAS = new HashMap<>();

    /** El sprite de cada corte, que es de donde sale su silueta. */
    private static final Map<Variante, NativeImage> MASCARAS = new HashMap<>();

    /**
     * Textura de la remera con la foto adentro, o null si todavia no se pudo
     * componer. Devolver null es una respuesta valida: el renderer cae al
     * cuadrado recortado al torso, que es peor pero nunca se ve roto.
     */
    @Nullable
    static Identifier fullPrint(Variante variante, UUID id, Identifier texturaFoto,
                                int ancho, int alto) {
        // La silueta depende del corte: un croptop recorta la foto mas arriba
        // que un remeron, y una musculosa no la deja llegar a las mangas.
        String clave = variante.clave() + "|" + id;
        Identifier hecha = CACHE.get(clave);
        if (hecha != null) return hecha;
        if (FALLADAS.containsKey(clave)) return null;

        NativeImage silueta = mascara(variante);
        if (silueta == null) {
            FALLADAS.put(clave, true);
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
            Identifier destino = Identifier.of(Femclothes.MOD_ID,
                    "estampada/" + variante.clave() + "_" + id);
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(destino, new NativeImageBackedTexture(compuesta));
            CACHE.put(clave, destino);
            return destino;
        } catch (Throwable e) {
            FALLADAS.put(clave, true);
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
    static Identifier cuerpoEstampado(Variante variante, @Nullable Estampa frente,
                                      @Nullable Estampa espalda, int color) {
        // Sin estampas y sin tenir no hay nada que componer: se usa la
        // textura del pack tal cual.
        if (frente == null && espalda == null && color == RemeraItem.BLANCO) return null;
        String clave = variante.clave() + "|" + frente + "|" + espalda + "|" + color;
        Identifier hecha = CACHE_CUERPO.get(clave);
        if (hecha != null) return hecha;
        if (FALLADAS.containsKey(clave)) return null;

        NativeImage base = telaCuerpo(variante);
        if (base == null) {
            FALLADAS.put(clave, true);
            return null;
        }
        NativeImage salida = new NativeImage(NativeImage.Format.RGBA,
                base.getWidth(), base.getHeight(), false);
        salida.copyFrom(base);
        if (color != RemeraItem.BLANCO) tenir(salida, color);

        // Si alguna foto todavia no esta lista se abandona SIN cachear: en el
        // proximo frame puede estar, y cachear a medias dejaria una remera con
        // una sola de sus dos estampas para siempre.
        // El full print que se lleva las caras sin lado propio. Se prefiere el
        // de adelante nada mas que por desempatar.
        Estampa cubreCompartidas = frente != null && frente.cubrir() ? frente
                : (espalda != null && espalda.cubrir() ? espalda : null);

        boolean listo = true;
        for (Cara cara : caras(variante)) {
            // Cada cara la pinta la estampa de SU lado. Antes un full print
            // en el frente pintaba tambien la espalda, porque el bucle usaba
            // la misma estampa para todas.
            Estampa suya = cara.compartida() ? cubreCompartidas
                    : (cara.atras() ? espalda : frente);
            if (suya == null) continue;

            if (suya.cubrir()) {
                listo &= pintarDelLienzo(salida, suya, cara, variante);
            } else if (esElTorso(cara)) {
                // El logo y la centrada van SOLO en el pecho y la espalda.
                // Antes la condicion era "cualquier cara no compartida", que
                // funcionaba de casualidad mientras los costados y las mangas
                // estaban marcados como compartidos. Al partirlos en mitades
                // dejaron de estarlo y una estampa centrada empezo a
                // repetirse en cada manga.
                listo &= pintar(salida, suya, escalar(cara.rect()));
            }
        }
        if (!listo) {
            salida.close();
            return null;
        }

        Identifier destino = Identifier.of(Femclothes.MOD_ID,
                "cuerpo/" + Long.toHexString(clave.hashCode() & 0xFFFFFFFFL));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(destino, new NativeImageBackedTexture(salida));
        CACHE_CUERPO.put(clave, destino);
        return destino;
    }

    /**
     * Cuanto se achica un diseno con transparencia dentro del lienzo, para
     * que no quede pegado al borde de la prenda.
     */
    private static final float MARGEN_DISENO = 0.88f;

    /**
     * Tine la tela multiplicando por el color, que es como tine vanilla.
     *
     * Va ANTES de las estampas a proposito: la foto se imprime sobre la
     * prenda ya tenida y no se tine con ella, igual que en una sublimadora de
     * verdad.
     */
    private static void tenir(NativeImage tela, int color) {
        // El color viene en RGB pero NativeImage empaqueta ABGR: el rojo del
        // color es el byte bajo de la imagen y el azul el alto.
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        for (int y = 0; y < tela.getHeight(); y++) {
            for (int x = 0; x < tela.getWidth(); x++) {
                int px = tela.getColor(x, y);
                int alfa = (px >>> 24) & 0xFF;
                if (alfa == 0) continue;
                int pr = px & 0xFF, pg = (px >>> 8) & 0xFF, pb = (px >>> 16) & 0xFF;
                tela.setColor(x, y, (alfa << 24)
                        | ((pb * b / 255) << 16)
                        | ((pg * g / 255) << 8)
                        | (pr * r / 255));
            }
        }
    }

    /**
     * Pinta el pedazo de foto que le toca a una cara segun el lienzo.
     *
     * La foto se coloca UNA sola vez sobre el lienzo entero -no cara por
     * cara- y de esa colocacion cada cara toma su ventana. Es lo que hace
     * que el dibujo continue de una cara a la otra en vez de repetirse.
     */
    private static boolean pintarDelLienzo(NativeImage salida, Estampa estampa, Cara cara,
                                           Variante variante) {
        CameraptureClientCompat.Foto info = fotoDe(estampa.foto());
        if (info == null) return false;
        NativeImage foto = leerDeLaGpu(info.textura(), info.ancho(), info.alto());
        if (foto == null) return false;

        try {
            float relFoto = (float) foto.getWidth() / foto.getHeight();
            float relLienzo = (float) LIENZO_ANCHO / lienzoAlto(variante);
            float ancho, alto;

            float y0;
            if (tieneTransparencia(foto)) {
                // Un diseno con fondo transparente se pone ENTERO y un poco
                // mas chico que la prenda. Recortarlo como a una foto le
                // comeria justo los bordes, que en un logo es donde vive la
                // forma; y como el fondo no pinta nada, lo que sobra queda de
                // tela lisa en vez de quedar vacio.
                //
                // Se centra sobre el CUERPO y no sobre el lienzo entero: las
                // dos filas de arriba son la banda del hombro, y centrar
                // sobre ellas subia el diseno un renglon.
                float caben = Math.min(LIENZO_ANCHO / relFoto, (float) variante.largo().filas);
                alto = caben * MARGEN_DISENO;
                ancho = alto * relFoto;
                y0 = BANDA_HOMBRO + (variante.largo().filas - alto) / 2f;
            } else {
                // Una foto opaca se recorta para llenar, y llena el lienzo
                // ENTERO: si solo cubriera el cuerpo, los hombros quedarian
                // sin estampar.
                float necesario = Math.max(LIENZO_ANCHO / relFoto, (float) lienzoAlto(variante));
                alto = necesario;
                ancho = alto * relFoto;
                y0 = (lienzoAlto(variante) - alto) / 2f;
            }
            float x0 = (LIENZO_ANCHO - ancho) / 2f;

            volcar(salida, foto, escalar(cara.rect()), cara, x0, y0, ancho, alto);
            return true;
        } finally {
            foto.close();
        }
    }

    /**
     * Si la imagen trae algun pixel no opaco.
     *
     * Es lo que distingue un diseno de una foto sin tener que preguntarselo
     * al jugador: una foto de Camerapture es opaca de punta a punta, y un PNG
     * que alguien subio para estampar casi siempre tiene fondo transparente.
     */
    private static boolean tieneTransparencia(NativeImage foto) {
        // De a saltos: con mirar una grilla alcanza para distinguir un fondo
        // transparente, y recorrer millones de pixeles por frame no.
        int paso = Math.max(1, Math.min(foto.getWidth(), foto.getHeight()) / 64);
        for (int y = 0; y < foto.getHeight(); y += paso) {
            for (int x = 0; x < foto.getWidth(); x += paso) {
                if (((foto.getColor(x, y) >>> 24) & 0xFF) < 250) return true;
            }
        }
        return false;
    }

    /**
     * Vuelca sobre una cara el pedazo de foto que le toca.
     *
     * Trabaja en coordenadas de LIENZO y no de foto: para cada pixel de la
     * cara calcula donde cae en el lienzo, y de ahi donde cae en la foto. Los
     * que caen fuera de la foto se dejan como estan, que es lo que permite
     * que un diseno mas chico que la prenda deje tela lisa alrededor.
     */
    private static void volcar(NativeImage salida, NativeImage foto, int[] r, Cara cara,
                               float fx0, float fy0, float fAncho, float fAlto) {
        int rx = r[0], ry = r[1], rw = r[2], rh = r[3];
        int[] l = cara.lienzo();

        for (int y = 0; y < rh; y++) {
            for (int x = 0; x < rw; x++) {
                int fondo = salida.getColor(rx + x, ry + y);
                int alfaPrenda = (fondo >>> 24) & 0xFF;
                if (alfaPrenda == 0) continue;   // fuera de la tela

                float px = (x + 0.5f) / rw;
                float py = (y + 0.5f) / rh;
                if (cara.espejar()) px = 1f - px;
                if (cara.espejarV()) py = 1f - py;

                // Donde cae este pixel en el lienzo, y de ahi en la foto.
                float u = (l[0] + l[2] * px - fx0) / fAncho;
                float v = (l[1] + l[3] * py - fy0) / fAlto;
                if (u < 0f || u >= 1f || v < 0f || v >= 1f) continue;

                int sx = Math.min((int) (u * foto.getWidth()), foto.getWidth() - 1);
                int sy = Math.min((int) (v * foto.getHeight()), foto.getHeight() - 1);
                int pixel = foto.getColor(sx, sy);
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
                float relFoto = (float) foto.getWidth() / foto.getHeight();
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
                // Las medidas salen de la TEXTURA y no de las que reporta
                // Camerapture: se muestrea sobre la textura, y si alguna vez
                // difieren -por padding, por ejemplo- mezclar las dos fuentes
                // descuadra la imagen.
                float relFoto = (float) foto.getWidth() / foto.getHeight();
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

    private static final Map<Variante, NativeImage> TELAS = new HashMap<>();

    @Nullable
    private static NativeImage telaCuerpo(Variante v) {
        if (TELAS.containsKey(v)) return TELAS.get(v);
        NativeImage img = leerRecurso(v.texturaCuerpo());
        TELAS.put(v, img);
        return img;
    }

    /** El sprite del corte liso, de donde sale su silueta. */
    @Nullable
    private static NativeImage mascara(Variante v) {
        if (MASCARAS.containsKey(v)) return MASCARAS.get(v);
        NativeImage img = leerRecurso(Identifier.of(Femclothes.MOD_ID,
                "textures/item/corte_" + v.clave() + ".png"));
        MASCARAS.put(v, img);
        return img;
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
