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
     * Un molde de largo POR VALOR (7), no uno que cicla. Se probó ciclico y
     * se cambio de inmediato: con siete pasos, ciclar significa clickear
     * hasta seis veces para llegar a "tanga" — siete items con nombre propio
     * es mejor que un dial de siete posiciones.
     */
    public static final MoldePantalonItem MOLDE_PANTALON_PANTALON =
            moldePantalon("molde_pantalon_pantalon", PantalonLargo.PANTALON);
    public static final MoldePantalonItem MOLDE_PANTALON_TRES_CUARTOS =
            moldePantalon("molde_pantalon_tres_cuartos", PantalonLargo.TRES_CUARTOS);
    public static final MoldePantalonItem MOLDE_PANTALON_BERMUDAS =
            moldePantalon("molde_pantalon_bermudas", PantalonLargo.BERMUDAS);
    public static final MoldePantalonItem MOLDE_PANTALON_SHORTS =
            moldePantalon("molde_pantalon_shorts", PantalonLargo.SHORTS);
    // Calzoncillos, slip y tanga se fusionaron en un solo valor: ver
    // PantalonLargo.ROPA_INTERIOR.
    public static final MoldePantalonItem MOLDE_PANTALON_ROPA_INTERIOR =
            moldePantalon("molde_pantalon_ropa_interior", PantalonLargo.ROPA_INTERIOR);

    /** Molde de tiro, uno por valor (3) — mismo criterio que el de largo. */
    public static final MoldeTiroItem MOLDE_TIRO_CORTO = moldeTiro("molde_tiro_corto", PantalonTiro.CORTO);
    public static final MoldeTiroItem MOLDE_TIRO_MEDIO = moldeTiro("molde_tiro_medio", PantalonTiro.MEDIO);
    public static final MoldeTiroItem MOLDE_TIRO_LARGO = moldeTiro("molde_tiro_largo", PantalonTiro.LARGO);

    /**
     * Molde de largo de MEDIAS, uno por valor (5) — eje nuevo, medias no
     * tenían ninguno. CANCAN es el default (sin componente): el largo que
     * ya tenían las medias antes de este eje.
     */
    public static final MoldeMediaItem MOLDE_MEDIA_ZOQUETES = moldeMedia("molde_media_zoquetes", MediasLargo.ZOQUETES);
    public static final MoldeMediaItem MOLDE_MEDIA_MEDIAS = moldeMedia("molde_media_medias", MediasLargo.MEDIAS);
    public static final MoldeMediaItem MOLDE_MEDIA_RODILLA = moldeMedia("molde_media_rodilla", MediasLargo.RODILLA);
    public static final MoldeMediaItem MOLDE_MEDIA_TRES_CUARTOS =
            moldeMedia("molde_media_tres_cuartos", MediasLargo.TRES_CUARTOS);
    public static final MoldeMediaItem MOLDE_MEDIA_CANCAN = moldeMedia("molde_media_cancan", MediasLargo.CANCAN);

    private static MoldePantalonItem moldePantalon(String id, PantalonLargo valor) {
        return register(id, new MoldePantalonItem(new Item.Settings().maxCount(1), valor));
    }

    private static MoldeTiroItem moldeTiro(String id, PantalonTiro valor) {
        return register(id, new MoldeTiroItem(new Item.Settings().maxCount(1), valor));
    }

    private static MoldeMediaItem moldeMedia(String id, MediasLargo valor) {
        return register(id, new MoldeMediaItem(new Item.Settings().maxCount(1), valor));
    }

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
    public static final CalientabrazosItem CALIENTABRAZOS = register("calientabrazos",
            new CalientabrazosItem(new Item.Settings().maxCount(16)));

    // --- Patrones reusables para la estación de personalización ---
    public static final ClothingPatternItem PATTERN_STRIPE_TOP = register("pattern_stripe_top",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "stripe_top")));

    public static final ClothingPatternItem PATTERN_STRIPE_ALT = register("pattern_stripe_alt",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "stripe_alt")));

    public static final ClothingPatternItem PATTERN_TRIPLE_STRIPE = register("pattern_triple_stripe",
            new ClothingPatternItem(new Item.Settings().maxCount(1),
                    Identifier.of("femclothes", "triple_stripe")));

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
