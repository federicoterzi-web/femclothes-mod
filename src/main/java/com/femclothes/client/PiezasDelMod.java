package com.femclothes.client;

import com.femclothes.garment.Capa;
import com.femclothes.garment.Parte;
import com.femclothes.item.FemclothesItems;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import com.femclothes.render.ClothingTextureCache;
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

    private static final Identifier MEDIAS_BASE =
            Identifier.of("femclothes", "textures/models/armor/socks_solid_layer_1.png");

    private PiezasDelMod() {}

    public static void init() {
        PiezasDePrenda.registrar(FemclothesItems.SOCKS_SOLID, PiezasDelMod::medias);
        PiezasDePrenda.registrar(ModItems.REMERA, PiezasDelMod::remera);
    }

    /**
     * Una pieza por pierna, cada una con su textura.
     *
     * Van separadas y no como una sola textura de las dos piernas porque el
     * color y el patron se resuelven POR LADO: un par disparejo son dos
     * composiciones distintas, y el cache las guarda por separado.
     */
    private static List<Pieza> medias(ItemStack stack, net.minecraft.entity.LivingEntity entidad) {
        return List.of(
                new Pieza(Parte.PIERNA_IZQ, Capa.MEDIA, texturaMedia(stack, Lado.IZQUIERDA)),
                new Pieza(Parte.PIERNA_DER, Capa.MEDIA, texturaMedia(stack, Lado.DERECHA)));
    }

    private static Identifier texturaMedia(ItemStack stack, Lado lado) {
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

        return ClothingTextureCache.composeGarment(MEDIAS_BASE, colorBase, mascara,
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
}
