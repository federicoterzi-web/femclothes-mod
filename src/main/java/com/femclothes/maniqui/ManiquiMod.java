package com.femclothes.maniqui;

import com.femclothes.Femclothes;
import com.femclothes.bloque.BloqueGeoItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro del Maniquí (2026-09-30, modelo GeckoLib "mannequin"). */
public final class ManiquiMod {

    public static final ManiquiBlock MANIQUI_BLOCK = new ManiquiBlock(
            AbstractBlock.Settings.create()
                    .strength(1.5f, 3.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    // INVISIBLE (lo dibuja GeckoLib) y no ocupa el bloque entero.
                    .nonOpaque());

    public static final BloqueGeoItem MANIQUI_BLOCK_ITEM =
            new BloqueGeoItem(MANIQUI_BLOCK, new Item.Settings(), "mannequin_item", "mannequin");

    public static final BlockEntityType<ManiquiBlockEntity> MANIQUI_BLOCK_ENTITY =
            BlockEntityType.Builder.create(ManiquiBlockEntity::new, MANIQUI_BLOCK).build();

    public static void register() {
        Identifier id = Identifier.of(Femclothes.MOD_ID, "maniqui");
        Registry.register(Registries.BLOCK, id, MANIQUI_BLOCK);
        Registry.register(Registries.ITEM, id, MANIQUI_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, MANIQUI_BLOCK_ENTITY);

        // Sliders de pose libre (2026-09-30).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                .register(PoseManiquiPayload.ID, PoseManiquiPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(PoseManiquiPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof ManiquiBlockEntity be
                            && be.canPlayerUse(context.player())) {
                        be.setAngulo(payload.indice(), payload.grados());
                    }
                }));
    }

    private ManiquiMod() {}
}
