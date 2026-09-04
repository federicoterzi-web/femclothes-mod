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

    // Rayas/alternado: se renderizan con TwoToneArmorRenderProvider, no con
    // la capa dyeable automática — por eso reusan el material CLOTH (su
    // textura de material no se usa para nada, el renderer custom pisa todo).
    public static final ClothingArmorItem SOCKS_STRIPE_TOP = registerArmor("socks_stripe_top",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), true));

    public static final ClothingArmorItem SOCKS_STRIPE_ALT = registerArmor("socks_stripe_alt",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), true));

    public static final ClothingArmorItem FISHNET_SOCKS = registerArmor("fishnet_socks",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.BOOTS,
                    new Item.Settings().maxCount(1), false));

    public static final ClothingArmorItem SHORTS = registerArmor("shorts",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.LEGGINGS,
                    new Item.Settings().maxCount(1), true));

    public static final ClothingArmorItem CROPTOP = registerArmor("croptop",
            new ClothingArmorItem(FemclothesArmorMaterials.CLOTH, ArmorItem.Type.CHESTPLATE,
                    new Item.Settings().maxCount(1), true, /* skinFillTorso */ true));

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
