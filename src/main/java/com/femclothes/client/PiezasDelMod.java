package com.femclothes.client;

import com.femclothes.Femclothes;
import com.femclothes.garment.Capa;
import com.femclothes.garment.Parte;
import com.femclothes.item.CalientabrazosItem;
import com.femclothes.item.FemclothesItems;
import com.femclothes.item.MediasLargo;
import com.femclothes.item.PantalonItem;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import com.femclothes.render.CajaSkin;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.CuerpoGeometria;
import com.femclothes.render.LayoutSkin;
import com.femclothes.render.Pieza;
import com.femclothes.render.PiezasDePrenda;
import com.femclothes.sublimadora.Estampa;
import com.femclothes.sublimadora.EstampaTextures;
import com.femclothes.sublimadora.ModItems;
import com.femclothes.sublimadora.RemeraItem;
import com.femclothes.sublimadora.Variante;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Como se ve cada prenda puesta: la mitad CLIENTE de PrendasDelMod.
 *
 * Cada prenda devuelve la lista de piezas que dibuja. Lo que antes hacia un
 * TrinketRenderer por prenda —cada uno con su propio modelo, su propia
 * dilatacion y su propio orden— es ahora una lista de datos que el
 * GarmentFeatureRenderer dibuja en el orden que dice la capa.
 *
 * <h2>La regla de la textura</h2>
 * Layout de SKIN a 8x, y TRANSPARENTE donde la prenda no tiene tela. Nada de
 * rellenar con piel: de eso se encarga el cuerpo base, abajo de todo.
 */
public final class PiezasDelMod {

    private PiezasDelMod() {}

    public static void init() {
        PiezasDePrenda.registrar(FemclothesItems.SOCKS_SOLID, PiezasDelMod::medias);
        PiezasDePrenda.registrar(FemclothesItems.PANTALON, PiezasDelMod::pantalon);
        PiezasDePrenda.registrar(ModItems.REMERA, PiezasDelMod::remera);
        PiezasDePrenda.registrar(FemclothesItems.CALIENTABRAZOS, PiezasDelMod::calientabrazos);
    }

    /**
     * Una pieza por pierna, cada una con su textura.
     *
     * Van separadas y no como una sola textura de las dos piernas porque el
     * color y el patron se resuelven POR LADO: un par disparejo son dos
     * composiciones distintas, y el cache las guarda por separado.
     *
     * El LARGO ({@link MediasLargo}) en cambio es ENTERO, no por lado —
     * un componente único, sin variante RIGHT_*: un par de medias de largo
     * disparejo (una hasta la rodilla, la otra zoquete) no es un caso real
     * que hiciera falta cubrir, a diferencia del color.
     */
    private static List<Pieza> medias(ItemStack stack, net.minecraft.entity.LivingEntity entidad) {
        Identifier base = MediasLargo.de(stack).texturaCuerpo();
        return List.of(
                new Pieza(Parte.PIERNA_IZQ, Capa.MEDIA, texturaMedia(base, stack, Lado.IZQUIERDA)),
                new Pieza(Parte.PIERNA_DER, Capa.MEDIA, texturaMedia(base, stack, Lado.DERECHA)));
    }

    private static Identifier texturaMedia(Identifier base, ItemStack stack, Lado lado) {
        int colorBase = RegionResolver.colorBase(stack, lado);
        Identifier patron = RegionResolver.patronId(stack, lado);
        Identifier mascara = patron == null ? null
                : ClothingTextureCache.patternMaskFor("socks", patron);

        // Las medias tambien se subliman: la foto se pinta sobre la media ya
        // tenida y con su patron, que es el orden de una sublimadora de
        // verdad. Sin estampa el gancho no hace nada.
        ClothingTextureCache.Encima estampa = EstampaTextures.tieneEstampa(stack)
                ? new ClothingTextureCache.Encima() {
                    @Override
                    public String clave() {
                        return EstampaTextures.claveEstampas(stack);
                    }
                    @Override
                    public boolean aplicar(NativeImage destino) {
                        return EstampaTextures.estampar(destino, stack);
                    }
                }
                : null;

        return ClothingTextureCache.composeGarment(base, colorBase, mascara,
                RegionResolver.colorPatron(stack, lado),
                ClothingTextureCache.Shading.LEGS, estampa);
    }

    /**
     * Torso, mas un brazo por manga.
     *
     * La textura es UNA sola para las tres piezas: los 36 cortes se generan
     * como una imagen en layout de skin donde ya estan el torso y las dos
     * mangas. Que sean tres piezas y no una es lo que deja que el renderer
     * las ordene por separado y, mas adelante, que un remeron sume una cuarta
     * en el muslo.
     *
     * La estampa no va como pieza aparte: se pinta ADENTRO de la textura, asi
     * sigue al cuerpo sin geometria extra y queda recortada a la tela sola.
     */
    private static List<Pieza> remera(ItemStack stack, net.minecraft.entity.LivingEntity entidad) {
        Variante variante = RemeraItem.variante(stack);
        // La remera es Lado.AMBAS siempre para PATRON (un solo color, un
        // solo patron para toda la prenda): ver Garment.regionesDe en
        // PrendasDelMod.
        Identifier textura = EstampaTextures.cuerpoEstampado(variante,
                RemeraItem.estampaDe(stack, Estampa.Cara.FRENTE),
                RemeraItem.estampaDe(stack, Estampa.Cara.ESPALDA),
                RemeraItem.color(stack),
                RegionResolver.patronId(stack, Lado.AMBAS),
                RegionResolver.colorPatron(stack, Lado.AMBAS));
        // Si todavia no se pudo componer -la foto no bajo- se usa la lisa,
        // que es lo correcto mientras tanto.
        if (textura == null) textura = variante.texturaCuerpo();

        List<Pieza> piezas = new ArrayList<>(3);
        piezas.add(new Pieza(Parte.TORSO, Capa.TORSO_EXTERIOR, textura));
        // La musculosa no dibuja los brazos. No alcanza con que la textura
        // tenga esa zona vacia: dibujar dos cajas transparentes por frame y
        // por jugador es trabajo tirado.
        if (variante.tieneMangas()) {
            piezas.add(new Pieza(Parte.BRAZO_IZQ, Capa.TORSO_EXTERIOR, textura));
            piezas.add(new Pieza(Parte.BRAZO_DER, Capa.TORSO_EXTERIOR, textura));
        }
        return piezas;
    }

    /**
     * Pantalón: la prueba del layering, con siete largos posibles. Se dibuja
     * en {@code Capa.PIERNA_EXTERIOR} (20), arriba de la media
     * ({@code Capa.MEDIA}, 10) — donde el pantalón no tiene tela se sigue
     * viendo la media, y donde ninguna de las dos tiene, el cuerpo base.
     *
     * La textura de PIERNA base cambia con {@link PantalonItem#largo} (7
     * PNGs generados por script, mismo criterio que los 36 cortes de
     * remera). Sin patrón ni estampa todavía.
     *
     * <h2>La cintura sube al torso, en runtime</h2>
     * Las 12 filas del cuboide de pierna terminan justo en el pivote de la
     * cadera, que corta seco contra el torso — un pantalón "completo" que
     * llegara solo hasta ahí se veía cortado, no puesto hasta la cintura de
     * verdad. En vez de hornear la banda en cada uno de los 7 PNGs de largo
     * (que multiplicaría por el eje de {@link PantalonItem#tiro}, 7×3=21
     * archivos para algo que es un simple rectángulo), se pinta la banda EN
     * RUNTIME sobre la textura ya compuesta, reusando el gancho
     * {@code Encima} que ya existía para las estampas — {@link #pintarCintura}.
     * El alto de la banda depende del tiro; el largo de pierna, no.
     *
     * Hay una tercera pieza en {@code Parte.TORSO}, a la misma capa que las
     * piernas: por debajo de una remera puesta (`Capa.TORSO_EXTERIOR`, 25 >
     * 20), pero visible si el torso no tiene nada encima. Usa el color de la
     * IZQUIERDA — es una sola pieza central, no tiene sentido partirla por
     * lado como las piernas.
     */
    private static List<Pieza> pantalon(ItemStack stack, net.minecraft.entity.LivingEntity entidad) {
        Identifier base = PantalonItem.largo(stack).texturaCuerpo();
        return List.of(
                new Pieza(Parte.PIERNA_IZQ, Capa.PIERNA_EXTERIOR, texturaPantalon(base, stack, Lado.IZQUIERDA)),
                new Pieza(Parte.PIERNA_DER, Capa.PIERNA_EXTERIOR, texturaPantalon(base, stack, Lado.DERECHA)),
                new Pieza(Parte.TORSO, Capa.PIERNA_EXTERIOR, texturaPantalon(base, stack, Lado.IZQUIERDA)));
    }

    private static Identifier texturaPantalon(Identifier base, ItemStack stack, Lado lado) {
        int colorBase = RegionResolver.colorBase(stack, lado);
        int filasTiro = PantalonItem.tiro(stack).filas;
        // Nota: usar Encima acá hace que reducirSiHaceFalta trate al
        // pantalón como si tuviera estampa (8x, sin achicar). Es una
        // sobra chica -12 filas de torso- frente a complicar esa deteccion
        // para distinguir "encima real" de "banda de cintura".
        ClothingTextureCache.Encima cintura = new ClothingTextureCache.Encima() {
            @Override public String clave() { return "cintura" + filasTiro; }
            @Override public boolean aplicar(NativeImage destino) {
                pintarCintura(destino, colorBase, filasTiro);
                return true;
            }
        };
        return ClothingTextureCache.composeGarment(base, colorBase, null, 0,
                ClothingTextureCache.Shading.LEGS, cintura);
    }

    /** Pinta una banda lisa en las últimas {@code filas} del cuboide de TORSO. */
    private static void pintarCintura(NativeImage img, int colorRgb, int filas) {
        int escala = CuerpoGeometria.ESCALA_TELA;
        CajaSkin torso = LayoutSkin.base(Parte.TORSO, false).escalada(escala);
        int alto = filas * escala;
        int blanco = 0xFFFFFFFF; // ABGR: blanco es el mismo valor en cualquier orden de canales
        for (CajaSkin.Rect cara : new CajaSkin.Rect[]{
                torso.derecha(), torso.frente(), torso.izquierda(), torso.atras()}) {
            int y0 = Math.max(cara.y0(), cara.y1() - alto);
            int color = ClothingTextureCache.tintPixel(blanco, colorRgb);
            for (int y = y0; y < cara.y1(); y++) {
                for (int x = cara.x0(); x < cara.x1(); x++) {
                    img.setColor(x, y, color);
                }
            }
        }
    }

    /**
     * Calientabrazos: una pieza por brazo, cobertura sola — mismo patrón
     * que {@link #medias}. Sin tiro: se probó (dos rediseños, ver
     * docs/CANAL.md v14-v15) y se retiró — tiro es exclusivo de prendas
     * inferiores según la arquitectura definitiva de
     * docs/MAQUINAS.md §"Categorías de patrones de modelado".
     *
     * Cobertura llena desde la MUÑECA hacia arriba (archivo base elegido
     * por {@link Variante.Manga}). Con SIN no hay archivo ni Pieza — no
     * hay nada que dibujar.
     */
    private static List<Pieza> calientabrazos(ItemStack stack, net.minecraft.entity.LivingEntity entidad) {
        Variante.Manga cobertura = CalientabrazosItem.cobertura(stack);
        if (cobertura == Variante.Manga.SIN) return List.of();

        Identifier base = texturaBaseCalientabrazos(cobertura);
        return List.of(
                new Pieza(Parte.BRAZO_IZQ, Capa.MANGA_INTERIOR,
                        texturaCalientabrazos(base, stack, Lado.IZQUIERDA)),
                new Pieza(Parte.BRAZO_DER, Capa.MANGA_INTERIOR,
                        texturaCalientabrazos(base, stack, Lado.DERECHA)));
    }

    private static Identifier texturaBaseCalientabrazos(Variante.Manga cobertura) {
        return Identifier.of(Femclothes.MOD_ID,
                "textures/models/armor/calientabrazos_" + cobertura.clave + "_layer_1.png");
    }

    private static Identifier texturaCalientabrazos(Identifier base, ItemStack stack, Lado lado) {
        int colorBase = RegionResolver.colorBase(stack, lado);
        return ClothingTextureCache.composeGarment(base, colorBase, null, 0,
                ClothingTextureCache.Shading.NONE);
    }
}
