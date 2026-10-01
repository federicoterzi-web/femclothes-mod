package com.femclothes.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class FemclothesItems {

    // --- Slots cosméticos (via Cosmetic Armor Updated) ---
    public static final ClothingArmorItem SOCKS_34 = registerArmor("socks_34",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), true));

    // Migrada a Trinket: se dibuja pegada a la pierna real del jugador,
    // no con la geometría (más ancha) de bota de armadura.
    // maxCount(16): mismo criterio que remera -dos medias con distinto largo/
    // color/patrón tienen distintos componentes y nunca se apilan entre sí,
    // esto solo junta medias realmente iguales.
    public static final ClothingTrinketItem SOCKS_SOLID = register("socks_solid",
            new ClothingTrinketItem(new Item.Settings().maxCount(16), true));

    // socks_stripe_top / socks_stripe_alt SE FUERON: eran un item por
    // combinacion de colores. Ahora son patrones que se aplican sobre
    // socks_solid en el telar (PATTERN_STRIPE_TOP / PATTERN_STRIPE_ALT
    // mas abajo), asi que el aspecto lo definen los componentes del
    // ItemStack y no un Item distinto.

    public static final ClothingArmorItem FISHNET_SOCKS = registerArmor("fishnet_socks",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), false));

    // Reemplaza a SHORTS. Primera prueba real del layering (se dibuja
    // ENCIMA de la media, sin borrarla — ver Capa.PIERNA_EXTERIOR), y ahora
    // ademas la prenda LARGA de pierna: se craftea pantalon completo y se
    // recorta con moldes en el telar. shorts.json se fue: era el antiguo
    // item fijo, sin eje.
    // maxCount(16): mismo criterio que remera/medias.
    public static final PantalonItem PANTALON = register("pantalon", new PantalonItem(new Item.Settings().maxCount(16)));

    /**
     * Pollera — primera pasada (2026-09-16), solo forma, sin eje todavía.
     * Comparte slot con PANTALON (piernas/exterior): alternativa, no
     * simultánea. Ver {@link PolleraItem}.
     */
    public static final PolleraItem POLLERA = register("pollera", new PolleraItem(new Item.Settings().maxCount(16)));

    // El croptop de FemClothes se retiro: era un chestplate del pipeline
    // viejo, nunca se le dibujo el arte -salia en damero- y desde que la
    // remera tiene el eje de largo, el corte crop hace lo mismo pero teñible,
    // estampable y sobre la geometria del cuerpo y no la de armadura.

    public static final ClothingArmorItem MAID_OUTFIT = registerArmor("maid_outfit",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.CHESTPLATE,
                    new Item.Settings().maxCount(1), false));

    public static final ClothingArmorItem OVERSIZED_HOODIE = registerArmor("oversized_hoodie",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.CHESTPLATE,
                    new Item.Settings().maxCount(1), true));

    // --- Slot custom "arms" (via Trinkets) ---
    // Reemplaza a ARMWARMERS: aquel nunca se enganchó al sistema de capas
    // (sin color por lado, sin Garment, sin Pieza — no dibujaba nada). El
    // item viejo no se migra, se retira sin más (mismo criterio que shorts
    // al nacer PantalonItem): un stack viejo en un mundo existente queda
    // como ítem desconocido, no rompe nada.
    // maxCount(16): mismo criterio que remera/medias/pantalón.
    /** Capa personalizable (2026-09-29, "seria una nueva categoria de ropa"). */
    public static final CapaItem CAPA = register("capa", new CapaItem(new Item.Settings().maxCount(16)));

    // Apliques (2026-10-01, Mesa de estilado): moldes que no se gastan y el retazo con los colores.
    public static final com.femclothes.aplique.MoldeApliqueItem MOLDE_APLIQUE_MONO = register("molde_aplique_mono",
            new com.femclothes.aplique.MoldeApliqueItem(new Item.Settings().maxCount(1), com.femclothes.aplique.ModeloAplique.MONO));
    public static final com.femclothes.aplique.MoldeApliqueItem MOLDE_APLIQUE_MARIPOSA = register("molde_aplique_mariposa",
            new com.femclothes.aplique.MoldeApliqueItem(new Item.Settings().maxCount(1), com.femclothes.aplique.ModeloAplique.MARIPOSA));
    public static final com.femclothes.aplique.MoldeApliqueItem MOLDE_APLIQUE_FLOR = register("molde_aplique_flor",
            new com.femclothes.aplique.MoldeApliqueItem(new Item.Settings().maxCount(1), com.femclothes.aplique.ModeloAplique.FLOR));
    public static final com.femclothes.aplique.RetazoApliqueItem RETAZO_APLIQUE = register("retazo_aplique",
            new com.femclothes.aplique.RetazoApliqueItem(new Item.Settings().maxCount(64)));

    /**
     * Hoodie oversize, primera prenda de la categoría Chaqueta (2026-09-30).
     * Sale de fábrica largo, con manga larga, cuello redondo y calce
     * Oversize — todo cambiable en la Modeladora como una remera.
     */
    public static final ChaquetaItem CHAQUETA = register("chaqueta", new ChaquetaItem(new Item.Settings().maxCount(16)
            .component(com.femclothes.sublimadora.ModItems.VARIANTE, new com.femclothes.sublimadora.Variante(
                    com.femclothes.sublimadora.Variante.Largo.LARGO, com.femclothes.sublimadora.Variante.Manga.LARGA,
                    com.femclothes.sublimadora.Variante.Cuello.REDONDO))
            .component(FemclothesComponents.CALCE, Calce.OVERSIZE)));

    /** Muestra de color de la Estación de Tintes (2026-09-30), ver {@link MuestraColorItem}. */
    public static final MuestraColorItem TINTE_MEZCLA = register("tinte_mezcla",
            new MuestraColorItem(new Item.Settings().maxCount(16)));
    public static final CalientabrazosItem CALIENTABRAZOS = register("calientabrazos",
            new CalientabrazosItem(new Item.Settings().maxCount(16)));

    // --- Patrones reusables para la estación de personalización ---
    // Grosor base (escala 8x, tamaño GRANDE) de cada uno — Mediano/Chico
    // lo escalan en tiempo real (ver PatronGenerador/TamanoPatron), no
    // hay archivo por tamaño.
    // SUPERIOR y no ALTERNADO (2026-09-18, "top stripe es una linea en la
    // parte superior de la prenda"): una sola banda pegada arriba, no
    // repetida — coherente con el nombre, a diferencia de los otros dos
    // que sí son un ritmo de rayas de punta a punta.
    public static final ClothingPatternItem PATTERN_STRIPE_TOP = register("pattern_stripe_top",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "stripe_top"),
                    com.femclothes.render.PatronGenerador.Forma.ARRIBA, 8));

    public static final ClothingPatternItem PATTERN_STRIPE_ALT = register("pattern_stripe_alt",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "stripe_alt"),
                    com.femclothes.render.PatronGenerador.Forma.ALTERNADO, 16));

    // TRES_RAYAS y no ALTERNADO (2026-09-19, "las tres rayitas son solo
    // tres, no se repiten"): antes tapizaba toda la prenda como stripe_alt;
    // ahora es un bloque fijo de 3 rayas que además se puede reposicionar
    // (eje "posicion" en RegionResolver.CapaPatron).
    public static final ClothingPatternItem PATTERN_TRIPLE_STRIPE = register("pattern_triple_stripe",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "triple_stripe"),
                    com.femclothes.render.PatronGenerador.Forma.TRES_RAYAS, 12));

    // Moldes de MOTIVO (2026-09-28, Fase 1 de la propuesta de patrones):
    // el último número es la escala del sprite en tamaño GRANDE (px del
    // atlas por px del dibujo), ver PatronGenerador#mascaraMotivo.
    public static final ClothingPatternItem PATTERN_CORAZONES = register("pattern_corazones",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "corazones"), com.femclothes.render.Motivo.CORAZONES, 2));
    public static final ClothingPatternItem PATTERN_ESTRELLAS = register("pattern_estrellas",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "estrellas"), com.femclothes.render.Motivo.ESTRELLAS, 2));
    public static final ClothingPatternItem PATTERN_LUNARES = register("pattern_lunares",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "lunares"), com.femclothes.render.Motivo.LUNARES, 2));
    public static final ClothingPatternItem PATTERN_VICHY = register("pattern_vichy",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "vichy"), com.femclothes.render.Motivo.VICHY, 1));

    private static ClothingArmorItem registerArmor(String path, ClothingArmorItem item) {
        return Registry.register(Registries.ITEM, Identifier.of("femclothes", path), item);
    }

    private static <T extends Item> T register(String path, T item) {
        return Registry.register(Registries.ITEM, Identifier.of("femclothes", path), item);
    }

    public static void init() {
        // fuerza class-loading al llamarse desde Femclothes#onInitialize
    }
}
