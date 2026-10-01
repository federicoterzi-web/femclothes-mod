package com.femclothes.render;

import com.femclothes.item.TamanoPatron;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.StringIdentifiable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera las máscaras de patrón (rayas, etc) EN CÓDIGO en vez de un PNG
 * por cada combinación prenda×patrón×tamaño — a pedido (2026-09-18):
 * "que tal si lo hacemos generado proceduralmente, [...] tengo otro
 * [patrón] que es las rayas superiores y como van a saber donde
 * ubicarse". La respuesta es {@link Forma}: cada patrón declara CÓMO se
 * ubica (alternado por toda la caja, o pegado a un extremo/al medio), y
 * esta clase sabe DÓNDE está esa caja para cada prenda (mismas
 * coordenadas que antes vivían a mano en los scripts de generación de
 * PNGs).
 *
 * <p>Segunda vuelta (2026-09-18, "pensaba en agregar rotación... si
 * además se agrega ubicación"; ampliado 2026-09-19 a ángulo libre, "girarlo
 * en angulo"): el ángulo (0°=horizontal de siempre, 90°=vertical, o
 * cualquier diagonal entre medio) rota la rayita — es un eje aparte de
 * {@link Forma} porque cualquier combinación tiene sentido (una raya "al
 * medio" puede ser horizontal, vertical o diagonal).
 *
 * <h2>Convención de coordenadas</h2>
 * Todo en el atlas "SKIN a 8x" (512×512) que ya usa {@link
 * ClothingTextureCache}/{@code PiezasDelMod} — una {@link Caja} es la
 * fila de las 4 caras laterales (derecha+frente+izquierda+atrás) de un
 * cuboide del layout de skin, calculadas a mano una sola vez acá (antes
 * repetidas en cada script de Python que generaba un PNG).
 */
public final class PatronGenerador {

    private PatronGenerador() {}

    private static final int LADO_ATLAS = 512;

    /**
     * Una caja de caras dentro del atlas: x0..x1, y arrancando en yTop, alto
     * filas; {@code prof} = ancho en px de las caras der/izq (y alto de las
     * tapas). Las tapas de arriba/abajo quedan justo encima de la fila de
     * caras (ver {@link #recorrer}).
     */
    public record Caja(int x0, int yTop, int x1, int alto, int prof) {
        int ancho() { return (x1 - x0 - 2 * prof) / 2; }
    }

    // PIERNA_DER (u=0,v=16,ancho=4,prof=4) / PIERNA_IZQ (u=16,v=48) a escala 8:
    // caras en x0..x0+128 (4 caras de 32px), y desde (v+prof)*8.
    private static final Caja PIERNA_DER = new Caja(0, 160, 128, 96, 32);
    private static final Caja PIERNA_IZQ = new Caja(128, 416, 256, 96, 32);
    // BRAZO_DER (u=40,v=16) / BRAZO_IZQ (u=32,v=48), mismo cálculo.
    private static final Caja BRAZO_DER = new Caja(320, 160, 448, 96, 32);
    private static final Caja BRAZO_IZQ = new Caja(256, 416, 384, 96, 32);
    // TORSO (u=16,v=16,ancho=8,prof=4): caras 2*prof+2*ancho=192 de ancho.
    private static final Caja TORSO = new Caja(128, 160, 320, 96, 32);
    // La capa (2026-09-29, "capas... como las capas vanilla"): el paño es la
    // caja 10x16x1 de la capa vanilla en uv 0,0 (exterior = frente, forro =
    // atrás) y capucha/cuello salen de una caja de detalles 12x8x2 en uv 24,0
    // (ver render.CapaMalla).
    private static final Caja CAPA = new Caja(0, 8, 176, 128, 8);
    private static final Caja CAPA_DETALLES = new Caja(192, 16, 416, 64, 16);

    /** El valor (pixel de máscara) de un punto de la tira de 4 caras, en coordenadas de la tira. */
    @FunctionalInterface
    private interface ValorTira {
        int valor(int lx, int ly);
    }

    /**
     * Pinta en {@code img} las 4 caras de la caja Y sus 2 tapas (2026-09-28,
     * "la parte de arriba de la remera no se esta pintando"): antes solo se
     * pintaba la fila de caras, así que los hombros (tapa de arriba del
     * torso) y la planta/el ruedo (tapa de abajo) quedaban siempre con el
     * color base. Cada pixel de tapa se traduce a la CONTINUACIÓN de la tira:
     * la mitad de la tapa pegada al frente sigue al frente (filas negativas,
     * "por encima" de la fila 0), la mitad pegada a la espalda sigue a la
     * espalda (espejada en x, como está la espalda en el atlas) — así una
     * raya o un motivo cruza el hombro sin cortarse, igual que el arnés.
     * Abajo es lo mismo, con la tapa dada vuelta (en la tapa de abajo el
     * frente queda del lado de arriba del atlas).
     */
    private static void recorrer(Caja caja, NativeImage img, ValorTira f) {
        int w = caja.x1() - caja.x0(), h = caja.alto(), prof = caja.prof(), ancho = caja.ancho();
        for (int ly = 0; ly < h; ly++) {
            for (int lx = 0; lx < w; lx++) {
                img.setColor(caja.x0() + lx, caja.yTop() + ly, f.valor(lx, ly));
            }
        }
        int frente = prof, espalda = 2 * prof + ancho;
        int yTapa = caja.yTop() - prof;
        for (int cy = 0; cy < prof; cy++) {
            for (int cx = 0; cx < ancho; cx++) {
                // Tapa de arriba: su borde de abajo toca el frente.
                boolean ladoFrente = cy >= prof / 2;
                int lxA = ladoFrente ? frente + cx : espalda + (ancho - 1 - cx);
                int lyA = ladoFrente ? -(prof - cy) : -(cy + 1);
                img.setColor(caja.x0() + prof + cx, yTapa + cy, f.valor(lxA, lyA));
                // Tapa de abajo: su borde de arriba toca el frente.
                boolean abajoFrente = cy < prof / 2;
                int lxB = abajoFrente ? frente + cx : espalda + (ancho - 1 - cx);
                int lyB = abajoFrente ? h + cy : h + (prof - 1 - cy);
                img.setColor(caja.x0() + prof + ancho + cx, yTapa + cy, f.valor(lxB, lyB));
            }
        }
    }

    /**
     * Qué cajas pinta un patrón para cada prenda — la pierna/brazo que
     * gobierna esa prenda (medias y pantalón comparten pierna, calienta-
     * brazos usa brazo, remera usa torso+los dos brazos). Prenda sin
     * entrada acá → sin patrón procedural, cae a la prenda lisa.
     */
    private static final Map<String, List<Caja>> CAJAS_POR_PRENDA = Map.of(
            "socks", List.of(PIERNA_DER, PIERNA_IZQ),
            // TORSO acá también (a pedido, 2026-09-18: "sigo sin entender
            // porque la parte del torso del pantalon no se pinta") — la
            // banda de cintura (PiezasDelMod#pintarCintura) pinta las
            // últimas filas de este mismo cuadro, y ahora puede leer
            // patrón de acá en vez de quedar SIEMPRE lisa.
            "pantalon", List.of(PIERNA_DER, PIERNA_IZQ, TORSO),
            "calientabrazos", List.of(BRAZO_DER, BRAZO_IZQ),
            "remera", List.of(TORSO, BRAZO_DER, BRAZO_IZQ),
            // La pollera nueva (2026-09-29, "quiero poder... teñirla"): su tela
            // usa el layout de la caja del TORSO, envuelto en la campana (ver
            // render.PolleraMalla), así que los patrones van en esa caja.
            "pollera", List.of(TORSO),
            "capa", List.of(CAPA, CAPA_DETALLES));

    /**
     * Dónde cae la rayita a lo largo del eje que le toca según
     * {@link Orientacion} (alto de la caja si es HORIZONTAL, ancho si es
     * VERTICAL):
     * <ul>
     *   <li>{@code ALTERNADO}: banda y hueco del mismo grosor, repetida
     *   de punta a punta — no importa cuánta tela tenga la prenda, el
     *   recorte final (fuera de esta clase) se encarga de mostrar solo lo
     *   que corresponde.</li>
     *   <li>{@code ARRIBA}/{@code ABAJO}: una sola banda pegada a ese
     *   extremo, sin repetirse (ej. "rayas superiores").</li>
     *   <li>{@code MEDIO}: una sola banda centrada.</li>
     * </ul>
     */
    /**
     * A pedido (2026-09-19, "desarrollame mas opcion 2"): la forma dejó de
     * venir pegada al ítem del patrón — {@code StringIdentifiable} para
     * poder guardarla como componente de dato por capa, mismo mecanismo
     * que {@link com.femclothes.item.TamanoPatron}.
     */
    public enum Forma implements StringIdentifiable {
        ALTERNADO("alternado"), ARRIBA("arriba"), ABAJO("abajo"), MEDIO("medio"), TRES_RAYAS("tres_rayas");

        public final String clave;

        Forma(String clave) {
            this.clave = clave;
        }

        @Override
        public String asString() {
            return clave;
        }

        public String traduccion() {
            return "femclothes.patronforma." + clave;
        }
    }

    private static final Map<String, NativeImage> CACHE = new HashMap<>();

    /** Overload de compatibilidad: horizontal (0°), como todo el patrón antes de la rotación. */
    public static NativeImage mascaraPara(String prenda, Forma forma, int grosorBase, TamanoPatron tamano) {
        return mascaraPara(prenda, forma, 0f, grosorBase, tamano, 0.5f);
    }

    /** Overload de compatibilidad: sin posición (formas fijas, que la ignoran). */
    public static NativeImage mascaraPara(String prenda, Forma forma, float anguloGrados,
                                          int grosorBase, TamanoPatron tamano) {
        return mascaraPara(prenda, forma, anguloGrados, grosorBase, tamano, 0.5f);
    }

    /**
     * La máscara para {@code prenda} con esta forma/ángulo/grosor/tamaño/
     * posición, o null si la prenda no tiene caja mapeada (cae a la
     * prenda lisa). Cacheada por combinación exacta (ángulo redondeado a
     * un paso de 15° — mismo criterio que {@code posicion}, no explotar
     * el cache con floats infinitos) — el grosor final ya incluye la
     * escala del tamaño, así que Grande/Mediano/Chico son entradas de
     * cache distintas, generadas una sola vez cada una.
     *
     * <h2>Ángulo (reemplaza a la vieja Horizontal/Vertical, 2026-09-19)</h2>
     * 0°=horizontal (de siempre, banda cruza TODO el ancho de la caja),
     * 90°=vertical (banda sube TODA la altura) — cualquier valor entre
     * medio es una diagonal. En vez de sweepear una sola fila/columna por
     * "d" como antes, cada pixel (x,y) de la caja proyecta sobre el eje
     * rotado: {@code d = x*sin(ang) + y*cos(ang)}, y {@code largo} es el
     * rango real de esa proyección sobre las 4 esquinas de la caja (en
     * 0°/90° da EXACTO lo mismo que antes: alto o ancho de la caja).
     *
     * <p>{@code posicion} (0.0..1.0, a lo largo de "d") solo mueve algo en
     * {@link Forma#TRES_RAYAS}: las formas fijas (ARRIBA/ABAJO/MEDIO/
     * ALTERNADO) la ignoran.
     */
    public static NativeImage mascaraPara(String prenda, Forma forma, float anguloGrados,
                                          int grosorBase, TamanoPatron tamano, float posicion) {
        return mascaraPara(prenda, forma, anguloGrados, grosorBase, tamano, posicion, 0);
    }

    /**
     * Como el de arriba, con la {@code semilla} que alimenta el valor al
     * azar de cada raya (Variación: Aleatorio, 2026-09-28).
     */
    public static NativeImage mascaraPara(String prenda, Forma forma, float anguloGrados,
                                          int grosorBase, TamanoPatron tamano, float posicion, int semilla) {
        return mascaraPara(prenda, forma, anguloGrados, grosorBase, tamano, posicion, semilla, 1f);
    }

    /**
     * Con {@code distancia} (2026-09-30, sliders de distancia): factor del
     * período de las rayas — 1 = raya y hueco del mismo ancho, como siempre;
     * 0.5 = sin hueco (pegadas); 3 = el triple de período. Solo cambia
     * ALTERNADO y TRES_RAYAS (las de una sola raya no tienen distancia).
     */
    public static NativeImage mascaraPara(String prenda, Forma forma, float anguloGrados,
                                          int grosorBase, TamanoPatron tamano, float posicion, int semilla,
                                          float distancia) {
        return mascaraPara(prenda, forma, anguloGrados, grosorBase, tamano, posicion, semilla, distancia, false);
    }

    /** Con {@code simetria} del torso (2026-09-30): las rayas en diagonal quedan en V, ver {@link #reflejoTorso}. */
    public static NativeImage mascaraPara(String prenda, Forma forma, float anguloGrados,
                                          int grosorBase, TamanoPatron tamano, float posicion, int semilla,
                                          float distancia, boolean simetria) {
        List<Caja> cajas = CAJAS_POR_PRENDA.get(prenda);
        if (cajas == null) return null;

        int grosor = Math.max(1, Math.round(grosorBase * tamano.escala));
        int posPaso = forma == Forma.TRES_RAYAS ? Math.round(Math.max(0f, Math.min(1f, posicion)) * 100) : 0;
        int anguloPaso = Math.round(anguloGrados / 15f) * 15;
        int periodo = Math.max(grosor, Math.round(grosor * 2 * distancia));
        String key = prenda + "|" + forma + "|" + anguloPaso + "|" + grosor + "|" + posPaso + "|" + semilla + "|" + periodo + "|" + simetria;
        NativeImage cacheada = CACHE.get(key);
        if (cacheada != null) return cacheada;

        NativeImage img = mascaraVacia();

        // Espejo brazo/pierna IZQ (2026-09-20/21): un brazo está en -X y el
        // otro en +X, así que la columna der()/izq() de este atlas es la
        // cara de AFUERA en uno y la de ADENTRO en el otro. El espejo de
        // verdad del perímetro cíclico der-frente-izq-atras alrededor del
        // eje que pasa por el centro de frente y de atrás es
        // "x' = 3·cuarto − x (mod ancho)": intercambia der↔izq (cada uno
        // invertido) e invierte frente/atrás DENTRO de su columna — así una
        // diagonal sale con la inclinación opuesta en el otro brazo.
        // TRES_RAYAS: bloque fijo hueco,raya,hueco,raya,hueco,raya que se
        // desliza con posPaso a lo largo del eje (3 períodos).
        int bloque = periodo * 3;
        // Grosor del contorno de una raya: un cuarto de su ancho, mínimo 1px.
        int contorno = Math.max(1, grosor / 4);
        double rad = Math.toRadians(anguloPaso);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        for (Caja caja : cajas) {
            int w = caja.x1() - caja.x0(), h = caja.alto();
            boolean espejar = caja == PIERNA_IZQ || caja == BRAZO_IZQ;
            int cuarto = w / 4;
            double p00 = 0, p10 = w * sin, p01 = h * cos, p11 = w * sin + h * cos;
            double min = Math.min(Math.min(p00, p10), Math.min(p01, p11));
            double max = Math.max(Math.max(p00, p10), Math.max(p01, p11));
            int largo = (int) Math.round(max - min);
            int inicioBloque = Math.round(posPaso / 100f * Math.max(0, largo - bloque));
            double minFinal = min;
            boolean simetrica = simetria && caja == TORSO;
            recorrer(caja, img, (lx, ly) -> {
                int lxEspejado = espejar ? Math.floorMod(3 * cuarto - lx, w) : lx;
                if (simetrica) lxEspejado = reflejoTorso(lxEspejado, w);
                int d = (int) Math.round(lxEspejado * sin + ly * cos - minFinal);
                boolean opaco = rayaOpaca(forma, d, grosor, periodo, largo, inicioBloque, bloque);
                boolean esContorno = !opaco && (rayaOpaca(forma, d - contorno, grosor, periodo, largo, inicioBloque, bloque)
                        || rayaOpaca(forma, d + contorno, grosor, periodo, largo, inicioBloque, bloque));
                // Número de raya: el de la banda de "d" (el contorno cae
                // al lado de una raya, toma el número de esa).
                int dRaya = esContorno && rayaOpaca(forma, d - contorno, grosor, periodo, largo, inicioBloque, bloque)
                        ? d - contorno : d + (esContorno ? contorno : 0);
                int indice = forma == Forma.TRES_RAYAS
                        ? Math.floorDiv(dRaya - inicioBloque, periodo) : Math.floorDiv(dRaya, periodo);
                int cob = opaco || esContorno ? 255 : 0;
                return pixelMascara(cob, esContorno, indice, 0, semilla, ly, h);
            });
        }

        CACHE.put(key, img);
        return img;
    }

    /** ¿La raya de esta forma pinta en la distancia {@code d} del eje? */
    private static boolean rayaOpaca(Forma forma, int d, int grosor, int periodo, int largo, int inicioBloque, int bloque) {
        // ARRIBA/ABAJO dejan un margen del mismo grosor que la rayita antes
        // de empezar (2026-09-18, "un poquito mas abajo del borde").
        return switch (forma) {
            case ALTERNADO -> Math.floorMod(d, periodo) < grosor;
            case ARRIBA -> d >= grosor && d < grosor * 2;
            case ABAJO -> d >= largo - grosor * 2 && d < largo - grosor;
            case MEDIO -> d >= (largo - grosor) / 2 && d < (largo - grosor) / 2 + grosor;
            case TRES_RAYAS -> {
                // La raya ocupa el final de cada período (con período = 2
                // grosores es lo mismo de siempre: hueco, raya).
                int rel = d - inicioBloque;
                yield rel >= 0 && rel < bloque && rel % periodo >= periodo - grosor;
            }
        };
    }

    // ── Canales de la máscara (2026-09-28, Fase 2: varios colores) ──────
    // La máscara ya no dice solo "pinta / no pinta": cada pixel lleva en
    // sus canales lo necesario para elegir el color AL COMPONER, sin tener
    // que generar una máscara por combinación de colores:
    //   alfa  = cobertura 0..255 (el vichy usa 128)
    //   rojo  = bit 7: es CONTORNO; bits 0..6: valor al azar de esta
    //           repetición (Variación: Aleatorio)
    //   verde = número de repetición (Variación: Alternar)
    //   azul  = altura dentro de la pieza, 0 arriba .. 255 abajo (Degradé)
    // Se escribe en TODOS los pixeles de la caja, también los de cobertura
    // 0: una capa invertida pinta justo ahí y necesita los mismos datos.

    private static int pixelMascara(int cobertura, boolean contorno, int indiceA, int indiceB, int semilla, int ly, int h) {
        int azar = (int) (mezclarHash(indiceA, indiceB, semilla) & 0x7F);
        int rojo = (contorno ? 0x80 : 0) | azar;
        int verde = Math.floorMod(indiceA + indiceB, 256);
        int azul = h <= 1 ? 0 : Math.max(0, Math.min(255, ly * 255 / (h - 1)));
        // NativeImage es ABGR.
        return (cobertura << 24) | (azul << 16) | (verde << 8) | rojo;
    }

    /** Pixel de contorno según los canales de arriba. */
    public static boolean esContorno(int pixel) { return (pixel & 0x80) != 0; }
    /** Valor al azar (0..127) de la repetición de este pixel. */
    public static int azar(int pixel) { return pixel & 0x7F; }
    /** Número de repetición (0..255) de este pixel. */
    public static int repeticion(int pixel) { return (pixel >> 8) & 0xFF; }
    /** Altura (0 arriba .. 255 abajo) de este pixel dentro de su pieza. */
    public static int altura(int pixel) { return (pixel >> 16) & 0xFF; }

    private static NativeImage mascaraVacia() {
        NativeImage img = new NativeImage(LADO_ATLAS, LADO_ATLAS, true);
        for (int y = 0; y < LADO_ATLAS; y++) for (int x = 0; x < LADO_ATLAS; x++) img.setColor(x, y, 0);
        return img;
    }

    /**
     * Punto de entrada único para una capa (2026-09-28): rayas si el molde
     * es de rayas, motivo repetido si es de motivo. Para una capa LISA
     * devuelve la máscara de degradé si la necesita (Variación: Degradé
     * con más de un color), o null (= liso de un color, cubre todo).
     * También null si la prenda no tiene caja mapeada o el molde no
     * existe — para una capa con molde, quien llama la saltea.
     */
    @org.jetbrains.annotations.Nullable
    public static NativeImage mascaraDeCapa(String prenda, com.femclothes.region.RegionResolver.CapaPatron capa) {
        if (capa.lisa()) {
            return capa.variacion() == Variacion.DEGRADE && !capa.extras().isEmpty() ? mascaraGradiente(prenda) : null;
        }
        com.femclothes.item.ClothingPatternItem item = com.femclothes.item.ClothingPatternItem.porId(capa.patronId());
        if (item == null) return null;
        DistribucionPatron dist = capa.distribucion() == null ? DistribucionPatron.DEFECTO : capa.distribucion();
        if (item.motivo != null) {
            return mascaraMotivo(prenda, item.motivo, item.grosorBase * capa.tamano().escala,
                    capa.repeticion(), capa.semilla(), capa.posicion(), capa.angulo(), dist);
        }
        // Rayas: la distancia que cuenta es la que cruza las rayas — 0° son
        // horizontales (se separan en vertical), 90° verticales (en horizontal).
        double rad = Math.toRadians(capa.angulo());
        double s2 = Math.sin(rad) * Math.sin(rad);
        float distancia = (float) (dist.distanciaV() * (1 - s2) + dist.distanciaH() * s2);
        return mascaraPara(prenda, capa.forma(), capa.angulo(), item.grosorBase, capa.tamano(), capa.posicion(),
                capa.semilla(), distancia, dist.simetria());
    }

    /** Capa lisa con degradé: cubre todas las cajas de la prenda, con la altura en el canal azul. */
    @org.jetbrains.annotations.Nullable
    public static NativeImage mascaraGradiente(String prenda) {
        List<Caja> cajas = CAJAS_POR_PRENDA.get(prenda);
        if (cajas == null) return null;
        String key = prenda + "|gradiente";
        NativeImage cacheada = CACHE.get(key);
        if (cacheada != null) return cacheada;
        NativeImage img = mascaraVacia();
        for (Caja caja : cajas) {
            recorrer(caja, img, (lx, ly) -> pixelMascara(255, false, 0, 0, 0, ly, caja.alto()));
        }
        CACHE.put(key, img);
        return img;
    }

    /** Separación entre motivos: la celda mide esto por el tamaño del motivo. */
    private static final double MOTIVO_CELDA = 1.8;
    /** Cuánto más grande es el motivo en {@link Repeticion#UNICO} (logo) que en la grilla. */
    private static final double MOTIVO_UNICO = 2.5;
    /** En {@link Repeticion#DISPERSO}, fracción de celdas que quedan vacías — si no, se lee como grilla movida. */
    private static final double MOTIVO_VACIAS = 0.3;
    /** Período del vichy (franja + hueco) por unidad de escala. */
    private static final double VICHY_PERIODO = 8;

    /**
     * Máscara de un {@link Motivo} repetido (Fase 1 de la propuesta de
     * patrones, 2026-09-28). {@code escala} = pixeles del atlas por pixel
     * del sprite (grosorBase del molde × tamaño). Canales: ver "Canales de
     * la máscara" — el contorno de un sprite es un pixel de sprite
     * alrededor del dibujo (escala con el motivo).
     *
     * <p>Horizontalmente la celda se ajusta para que entre una cantidad
     * ENTERA de celdas en el perímetro de las 4 caras: así el motivo que
     * cae en la costura de atrás sigue del otro lado sin cortarse.
     */
    public static NativeImage mascaraMotivo(String prenda, Motivo motivo, double escala,
                                            Repeticion repeticion, int semilla, float posicion) {
        return mascaraMotivo(prenda, motivo, escala, repeticion, semilla, posicion, 0f, DistribucionPatron.DEFECTO);
    }

    /**
     * Con giro de grilla ({@code anguloGrilla}, el ángulo de la capa) y
     * {@link DistribucionPatron} (giro de cada motivo y distancias) —
     * 2026-09-30, "que los patrones si se puedan girar y que haya un slider
     * vertical y horizontal para ponerlos mas juntos". Con la grilla girada
     * ya no entra una cantidad entera de columnas en el perímetro: en la
     * costura de atrás el dibujo puede no empalmar.
     */
    public static NativeImage mascaraMotivo(String prenda, Motivo motivo, double escala,
                                            Repeticion repeticion, int semilla, float posicion,
                                            float anguloGrilla, DistribucionPatron dist) {
        List<Caja> cajas = CAJAS_POR_PRENDA.get(prenda);
        if (cajas == null) return null;
        double k = Math.max(0.75, Math.round(escala * 4) / 4.0);
        int posPaso = Math.round(Math.max(0f, Math.min(1f, posicion)) * 20);
        int grillaPaso = Math.floorMod(Math.round(anguloGrilla / 15f) * 15, 360);
        String key = prenda + "|motivo|" + motivo + "|" + k + "|" + repeticion + "|" + semilla + "|" + posPaso
                + "|" + grillaPaso + "|" + dist.clave();
        NativeImage cacheada = CACHE.get(key);
        if (cacheada != null) return cacheada;

        NativeImage img = mascaraVacia();
        int[] celda = new int[2];
        for (Caja caja : cajas) {
            int w = caja.x1() - caja.x0(), h = caja.alto();
            boolean espejar = caja == PIERNA_IZQ || caja == BRAZO_IZQ;
            int cuarto = w / 4;
            // Centro del FRENTE de esta caja: en brazo/pierna las 4 caras
            // miden lo mismo (frente = 2ª columna); el torso es der 32 |
            // frente 64 | izq 32 | atrás 64.
            double centroFrente = caja == TORSO ? 64 : cuarto * 1.5;
            boolean simetrica = dist.simetria() && caja == TORSO;
            recorrer(caja, img, (lx, ly) -> {
                int u = espejar ? Math.floorMod(3 * cuarto - lx, w) : lx;
                if (simetrica) u = reflejoTorso(u, w);
                int rol = motivo.esProcedural()
                        ? coberturaVichy(u, ly, w, h, k, posPaso / 20.0, centroFrente, grillaPaso, dist, celda)
                        : rolSprite(motivo, repeticion, u + 0.5, ly + 0.5, w, h, k, semilla,
                                posPaso / 20.0, centroFrente, grillaPaso, dist, celda);
                int cob = rol == ROL_NADA ? 0 : rol == ROL_MEDIO ? 128 : 255;
                return pixelMascara(cob, rol == ROL_CONTORNO, celda[0], celda[1], semilla, ly, h);
            });
        }
        CACHE.put(key, img);
        return img;
    }

    private static final int ROL_NADA = 0, ROL_RELLENO = 1, ROL_CONTORNO = 2, ROL_MEDIO = 3;

    /**
     * Qué es este pixel dentro del motivo, y de qué celda (en {@code celda}:
     * columna, fila). Mira la celda del pixel y sus 8 vecinas: con los
     * motivos girados o con poca distancia (2026-09-30) un motivo se sale de
     * su celda, y sin mirar las vecinas quedaría cortado en el borde.
     */
    private static int rolSprite(Motivo m, Repeticion rep, double u, double v, int w, int h,
                                 double k, int semilla, double posicion, double centroFrente,
                                 int anguloGrilla, DistribucionPatron dist, int[] celda) {
        double giro = Math.toRadians(dist.giroMotivo());
        double anchoM = m.ancho * k, altoM = m.alto * k;
        if (rep == Repeticion.UNICO) {
            celda[0] = 0;
            celda[1] = 0;
            double kk = k * MOTIVO_UNICO;
            // El logo gira con los dos ángulos (no hay grilla que girar aparte).
            return rolEn(m, u - centroFrente, v - posicion * h, giro + Math.toRadians(anguloGrilla), kk,
                    false, dist.espejo());
        }
        double tam = Math.max(anchoM, altoM) * MOTIVO_CELDA;
        double celdaY = tam * dist.distanciaV();
        double celdaX;
        int columnas;
        double gu, gv;
        if (anguloGrilla == 0) {
            // Grilla derecha: entra una cantidad entera de columnas en el
            // perímetro (el motivo de la costura de atrás sigue del otro lado).
            columnas = Math.max(1, (int) Math.round(w / (tam * dist.distanciaH())));
            celdaX = (double) w / columnas;
            gu = u;
            gv = v;
        } else {
            columnas = 0;
            celdaX = tam * dist.distanciaH();
            double a = -Math.toRadians(anguloGrilla), cos = Math.cos(a), sin = Math.sin(a);
            double du = u - centroFrente, dv = v - h / 2.0;
            gu = du * cos - dv * sin + centroFrente;
            gv = du * sin + dv * cos + h / 2.0;
        }
        // Posición corre la grilla en vertical (0.5 = sin corrimiento).
        gv += (posicion - 0.5) * celdaY;

        int fila0 = (int) Math.floor(gv / celdaY);
        int resultado = ROL_NADA;
        for (int df = -1; df <= 1; df++) {
            int fila = fila0 + df;
            double corrimiento = rep == Repeticion.LADRILLO && Math.floorMod(fila, 2) == 1 ? celdaX / 2 : 0;
            int col0 = (int) Math.floor((gu - corrimiento) / celdaX);
            for (int dc = -1; dc <= 1; dc++) {
                int col = col0 + dc;
                int colId = columnas > 0 ? Math.floorMod(col, columnas) : col;
                double lu = gu - ((col + 0.5) * celdaX + corrimiento);
                double lv = gv - (fila + 0.5) * celdaY;
                if (rep == Repeticion.DISPERSO) {
                    long hash = mezclarHash(colId, fila, semilla);
                    if ((hash & 0xFF) / 255.0 < MOTIVO_VACIAS) continue;
                    double libreX = Math.max(0, (celdaX - anchoM) / 2 - k), libreY = Math.max(0, (celdaY - altoM) / 2 - k);
                    lu -= (((hash >> 8) & 0xFF) / 255.0 * 2 - 1) * libreX;
                    lv -= (((hash >> 16) & 0xFF) / 255.0 * 2 - 1) * libreY;
                }
                int rol = rolEn(m, lu, lv, giro, k, dist.alternancia().espeja(colId, fila), dist.espejo());
                if (rol == ROL_RELLENO) {
                    celda[0] = colId;
                    celda[1] = fila;
                    return ROL_RELLENO;
                }
                if (rol == ROL_CONTORNO && resultado == ROL_NADA) {
                    resultado = ROL_CONTORNO;
                    celda[0] = colId;
                    celda[1] = fila;
                }
            }
        }
        if (resultado == ROL_NADA) {
            celda[0] = columnas > 0 ? Math.floorMod((int) Math.floor(gu / celdaX), columnas) : (int) Math.floor(gu / celdaX);
            celda[1] = fila0;
        }
        return resultado;
    }

    /**
     * Simetría del torso (2026-09-30): la mitad que va del centro del frente
     * (u 64, a escala 8) hasta el centro de la espalda (64 + w/2) toma el
     * pixel reflejado de la otra mitad — así el lado izquierdo es el espejo
     * del derecho, en el frente y en la espalda.
     */
    private static int reflejoTorso(int u, int w) {
        int centro = w / 3;          // der w/6 + medio frente w/6 (192: 32 + 32 = 64)
        int mitad = w / 2;
        if (u >= centro && u < centro + mitad) return Math.floorMod(2 * centro - 1 - u, w);
        return u;
    }

    /** Relleno / contorno / nada de un motivo centrado en (0,0), girado {@code giro} radianes, a escala {@code kk}. */
    private static int rolEn(Motivo m, double lu, double lv, double giro, double kk) {
        return rolEn(m, lu, lv, giro, kk, false, DistribucionPatron.Espejo.NINGUNO);
    }

    /**
     * Con espejos (2026-09-30): el motivo final es
     * {@code alternado( girado( espejado(motivo) ) )}, así que al pixel se le
     * aplica la inversa en el orden contrario — primero la alternancia (dar
     * vuelta el motivo YA girado: con Giro sale un zigzag), después el giro,
     * y al final el espejo propio del motivo.
     */
    private static int rolEn(Motivo m, double lu, double lv, double giro, double kk,
                             boolean alternado, DistribucionPatron.Espejo espejo) {
        if (alternado) lu = -lu;
        if (giro != 0) {
            double cos = Math.cos(-giro), sin = Math.sin(-giro);
            double ru = lu * cos - lv * sin, rv = lu * sin + lv * cos;
            lu = ru;
            lv = rv;
        }
        if (espejo.horizontal) lu = -lu;
        if (espejo.vertical) lv = -lv;
        int sx = (int) Math.floor(lu / kk + m.ancho / 2.0), sy = (int) Math.floor(lv / kk + m.alto / 2.0);
        if (m.pinta(sx, sy)) return ROL_RELLENO;
        // Contorno: un pixel de sprite alrededor del dibujo (8 vecinos).
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && m.pinta(sx + dx, sy + dy)) return ROL_CONTORNO;
            }
        }
        return ROL_NADA;
    }

    /**
     * Vichy: franjas horizontales y verticales del mismo color que se
     * cruzan — 50% donde pasa una sola, 100% en el cruce (así sale el
     * cuadro de 3 tonos de verdad con un solo color de capa). Sin
     * contorno; cada cuadro es una "repetición" para Alternar/Aleatorio.
     */
    private static int coberturaVichy(int u, int v, int w, int h, double k, double posicion, double centroFrente,
                                      int anguloGrilla, DistribucionPatron dist, int[] celda) {
        double deseado = VICHY_PERIODO * k * 2;
        double periodoY = deseado * dist.distanciaV();
        double periodoX, uu, vv;
        if (anguloGrilla == 0) {
            int columnas = Math.max(1, (int) Math.round(w / (deseado * dist.distanciaH())));
            periodoX = (double) w / columnas;
            uu = u;
            vv = v;
        } else {
            // Vichy en diagonal (2026-09-30): el cuadriculado gira alrededor del centro del frente.
            periodoX = deseado * dist.distanciaH();
            double a = -Math.toRadians(anguloGrilla), cos = Math.cos(a), sin = Math.sin(a);
            double du = u - centroFrente, dv = v - h / 2.0;
            uu = du * cos - dv * sin + centroFrente;
            vv = du * sin + dv * cos + h / 2.0;
        }
        vv += (posicion - 0.5) * periodoY;
        celda[0] = (int) Math.floor(uu / periodoX);
        celda[1] = (int) Math.floor(vv / periodoY);
        boolean vertical = (((uu % periodoX) + periodoX) % periodoX) < periodoX / 2;
        boolean horizontal = (((vv % periodoY) + periodoY) % periodoY) < periodoY / 2;
        if (vertical && horizontal) return ROL_RELLENO;
        return vertical || horizontal ? ROL_MEDIO : ROL_NADA;
    }

    /** Hash entero chico y estable (celda + semilla) — Disperso/Aleatorio tienen que dar SIEMPRE lo mismo con la misma semilla. */
    private static long mezclarHash(int a, int b, int semilla) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + semilla * 0x165667B19E3779F9L;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return h;
    }

}
