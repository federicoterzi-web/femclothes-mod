package com.modamod.estilista;

import com.modamod.Modamod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registro de la Estilista automática (2026-10-05, "rearmemos la estiladora como un bloque de la cadena, copia la base
 * de la modeladora ponele la tapa de la autostyler arriba y vamos a migrar toda la funcionalidad de la mesa
 * estilizadora a esta nueva maquina"). Etapa 1: el bloque, el modelo y la cadena; la Mesa de estilado sigue hasta que
 * esta ande.
 */
public final class EstilistaMod {

    public static final EstilistaBlock ESTILISTA_BLOCK = new EstilistaBlock(
            AbstractBlock.Settings.create().strength(2.5f, 4.0f).sounds(BlockSoundGroup.WOOD).nonOpaque()
                    .luminance(com.modamod.util.LuzMaquina::luminancia));
    public static final EstilistaBlockItem ESTILISTA_ITEM = new EstilistaBlockItem(ESTILISTA_BLOCK, new Item.Settings());

    /** Versión creativa: sin espera ni insumos, como las otras máquinas ({@code util.MaquinaCreativa}). */
    public static final EstilistaBlock ESTILISTA_CREATIVA = com.modamod.util.MaquinaCreativa.creativa(new EstilistaBlock(
            AbstractBlock.Settings.create().strength(2.5f, 4.0f).sounds(BlockSoundGroup.WOOD).nonOpaque()
                    .luminance(com.modamod.util.LuzMaquina::luminancia)));
    public static final EstilistaBlockItem ESTILISTA_CREATIVA_ITEM = new EstilistaBlockItem(ESTILISTA_CREATIVA, new Item.Settings());

    public static final BlockEntityType<EstilistaBlockEntity> ESTILISTA_BLOCK_ENTITY =
            BlockEntityType.Builder.create(EstilistaBlockEntity::new, ESTILISTA_BLOCK, ESTILISTA_CREATIVA).build();

    public static void register() {
        Identifier id = Identifier.of(Modamod.MOD_ID, "estilista");
        Registry.register(Registries.BLOCK, id, ESTILISTA_BLOCK);
        Registry.register(Registries.ITEM, id, ESTILISTA_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, ESTILISTA_BLOCK_ENTITY);
        Identifier creativa = Identifier.of(Modamod.MOD_ID, "estilista_creativa");
        Registry.register(Registries.BLOCK, creativa, ESTILISTA_CREATIVA);
        Registry.register(Registries.ITEM, creativa, ESTILISTA_CREATIVA_ITEM);
    }

    private EstilistaMod() {}
}
