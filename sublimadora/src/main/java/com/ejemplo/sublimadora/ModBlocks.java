package com.ejemplo.sublimadora;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro de bloque, item y block entity type. Fabric 1.21.1. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final String MOD_ID = "sublimadora";

    public static final Block SUBLIMADORA = new SublimadoraBlock(
            AbstractBlock.Settings.create()
                    .strength(2.5f, 4.0f)
                    .requiresTool()
                    .nonOpaque()                       // la tapa abierta sale del cubo
                    .sounds(BlockSoundGroup.METAL)
                    .pistonBehavior(PistonBehavior.BLOCK)
    );

    public static final BlockItem SUBLIMADORA_ITEM =
            new BlockItem(SUBLIMADORA, new Item.Settings());

    public static final BlockEntityType<SublimadoraBlockEntity> SUBLIMADORA_ENTITY =
            BlockEntityType.Builder.create(SublimadoraBlockEntity::new, SUBLIMADORA).build();

    public static void register() {
        Identifier id = Identifier.of(MOD_ID, "sublimadora");
        Registry.register(Registries.BLOCK, id, SUBLIMADORA);
        Registry.register(Registries.ITEM, id, SUBLIMADORA_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, SUBLIMADORA_ENTITY);
    }
}
