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
    public static final ClothingTrinketItem SOCKS_SOLID = register("socks_solid",
            new ClothingTrinketItem(new Item.Settings().maxCount(1), true));

    // socks_stripe_top / socks_stripe_alt SE FUERON: eran un item por
    // combinacion de colores. Ahora son patrones que se aplican sobre
    // socks_solid en el telar (PATTERN_STRIPE_TOP / PATTERN_STRIPE_ALT
    // mas abajo), asi que el aspecto lo definen los componentes del
    // ItemStack y no un Item distinto.

    public static final ClothingArmorItem FISHNET_SOCKS = registerArmor("fishnet_socks",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), false));

    // Migrado al sistema de capas: primera prueba real del layering (short
    // dibujado ENCIMA de la media, sin borrarla — ver Capa.PIERNA_EXTERIOR).
    // Antes era ClothingArmorItem con geometria de armadura y sin textura de
    // armadura siquiera (cloth_layer_1.png nunca existio), asi que puesto no
    // se veia nada: no habia comportamiento previo que preservar.
    public static final ClothingTrinketItem SHORTS = register("shorts",
            new ClothingTrinketItem(new Item.Settings().maxCount(1), true));

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
    public static final ArmWarmerItem ARMWARMERS = register("armwarmers",
            new ArmWarmerItem(new Item.Settings().maxCount(1)));

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
