package com.ejemplo.sublimadora;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registro del bloque, su item y el tipo de BlockEntity. */
public final class ModBlocks {

    public static final String MOD_ID = "sublimadora";

    public static final SublimadoraBlock SUBLIMADORA = Registry.register(
            Registries.BLOCK,
            Identifier.of(MOD_ID, "sublimadora"),
            new SublimadoraBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .strength(3.0F)
                    .nonOpaque()));

    public static final Item SUBLIMADORA_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "sublimadora"),
            new BlockItem(SUBLIMADORA, new Item.Settings()));

    // SublimadoraBlockEntity lo referencia en su constructor.
    public static final BlockEntityType<SublimadoraBlockEntity> SUBLIMADORA_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MOD_ID, "sublimadora"),
            BlockEntityType.Builder.create(SublimadoraBlockEntity::new, SUBLIMADORA).build(null));

    public static void init() {
        // fuerza class-loading
    }

    private ModBlocks() {}
}
