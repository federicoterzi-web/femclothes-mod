package com.femclothes.estilado;

import com.femclothes.Femclothes;
import com.femclothes.bloque.BloqueGeoItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro de la Mesa de estilado (2026-09-30, modelo GeckoLib "styling_table"). */
public final class EstiladoMod {

    public static final EstiladoBlock ESTILADO_BLOCK = new EstiladoBlock(
            AbstractBlock.Settings.create()
                    .strength(2.5f, 4.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    // INVISIBLE (lo dibuja GeckoLib): sin esto cullea las caras vecinas.
                    .nonOpaque());

    public static final BloqueGeoItem ESTILADO_BLOCK_ITEM =
            new BloqueGeoItem(ESTILADO_BLOCK, new Item.Settings(), "styling_table");

    public static final BlockEntityType<EstiladoBlockEntity> ESTILADO_BLOCK_ENTITY =
            BlockEntityType.Builder.create(EstiladoBlockEntity::new, ESTILADO_BLOCK).build();

    public static void register() {
        Identifier id = Identifier.of(Femclothes.MOD_ID, "mesa_estilado");
        Registry.register(Registries.BLOCK, id, ESTILADO_BLOCK);
        Registry.register(Registries.ITEM, id, ESTILADO_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, ESTILADO_BLOCK_ENTITY);
    }

    private EstiladoMod() {}
}
