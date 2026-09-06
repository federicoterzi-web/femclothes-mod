package com.femclothes.sublimadora;

import com.femclothes.Femclothes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro de bloque, item y block entity type. Fabric 1.21.1. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final Block SUBLIMADORA = new SublimadoraBlock(
            AbstractBlock.Settings.create()
                    // Sin requiresTool: a mano tarda unos 3.7 s y devuelve el
                    // bloque igual, con pico de hierro menos de uno. Con
                    // requiresTool la mano no dropeaba nada, que para una
                    // maquina que puede tener una remera adentro es cruel.
                    .strength(2.5f, 4.0f)
                    .nonOpaque()                       // la tapa abierta sale del cubo
                    .sounds(BlockSoundGroup.METAL)
                    .pistonBehavior(PistonBehavior.BLOCK)
    );

    // BlockItem propio para que el icono del inventario lo dibuje GeckoLib
    // con la misma malla que el bloque, en vez de un modelo vanilla paralelo.
    public static final SublimadoraBlockItem SUBLIMADORA_ITEM =
            new SublimadoraBlockItem(SUBLIMADORA, new Item.Settings());

    public static final BlockEntityType<SublimadoraBlockEntity> SUBLIMADORA_ENTITY =
            BlockEntityType.Builder.create(SublimadoraBlockEntity::new, SUBLIMADORA).build();

    public static void register() {
        Identifier id = Identifier.of(Femclothes.MOD_ID, "sublimadora");
        Registry.register(Registries.BLOCK, id, SUBLIMADORA);
        Registry.register(Registries.ITEM, id, SUBLIMADORA_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, SUBLIMADORA_ENTITY);
    }
}
