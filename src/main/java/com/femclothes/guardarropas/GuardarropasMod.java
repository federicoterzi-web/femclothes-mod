package com.femclothes.guardarropas;

import com.femclothes.Femclothes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro del Guardarropas — PLACEHOLDER, ver GuardarropasBlockEntity. */
public final class GuardarropasMod {

    public static final GuardarropasBlock GUARDARROPAS_BLOCK = new GuardarropasBlock(
            AbstractBlock.Settings.create()
                    .strength(2.5f, 4.0f)
                    .sounds(BlockSoundGroup.WOOD));

    public static final BlockItem GUARDARROPAS_BLOCK_ITEM = new BlockItem(GUARDARROPAS_BLOCK, new Item.Settings());

    public static final BlockEntityType<GuardarropasBlockEntity> GUARDARROPAS_BLOCK_ENTITY =
            BlockEntityType.Builder.create(GuardarropasBlockEntity::new, GUARDARROPAS_BLOCK).build();

    public static void register() {
        Identifier id = Identifier.of(Femclothes.MOD_ID, "guardarropas");
        Registry.register(Registries.BLOCK, id, GUARDARROPAS_BLOCK);
        Registry.register(Registries.ITEM, id, GUARDARROPAS_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, GUARDARROPAS_BLOCK_ENTITY);
    }

    private GuardarropasMod() {}
}
