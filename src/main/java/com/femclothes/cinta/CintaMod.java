package com.femclothes.cinta;

import com.femclothes.Femclothes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro de la cinta transportadora (2026-10-04, "geometria de cinta transportadora"). */
public final class CintaMod {

    public static final CintaBlock CINTA_BLOCK = new CintaBlock(
            AbstractBlock.Settings.create()
                    .strength(1.5f, 3.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque());

    public static final BlockItem CINTA_ITEM = new BlockItem(CINTA_BLOCK, new Item.Settings());

    public static final BlockEntityType<CintaBlockEntity> CINTA_BLOCK_ENTITY =
            BlockEntityType.Builder.create(CintaBlockEntity::new, CINTA_BLOCK).build();

    public static void register() {
        Identifier id = Identifier.of(Femclothes.MOD_ID, "cinta");
        Registry.register(Registries.BLOCK, id, CINTA_BLOCK);
        Registry.register(Registries.ITEM, id, CINTA_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, CINTA_BLOCK_ENTITY);
    }

    private CintaMod() {}
}
