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

    /** Versión creativa (2026-10-01): sin retazos y con todos los moldes a mano — ver {@code util.MaquinaCreativa}. */
    public static final EstiladoBlock ESTILADO_CREATIVA = com.femclothes.util.MaquinaCreativa.creativa(new EstiladoBlock(
            AbstractBlock.Settings.create().strength(2.5f, 4.0f).sounds(BlockSoundGroup.WOOD).nonOpaque()));
    public static final BloqueGeoItem ESTILADO_CREATIVA_ITEM =
            new BloqueGeoItem(ESTILADO_CREATIVA, new Item.Settings(), "styling_table");

    public static final BlockEntityType<EstiladoBlockEntity> ESTILADO_BLOCK_ENTITY =
            BlockEntityType.Builder.create(EstiladoBlockEntity::new, ESTILADO_BLOCK, ESTILADO_CREATIVA).build();

    public static void register() {
        ComandoMoldes.init();
        Identifier id = Identifier.of(Femclothes.MOD_ID, "mesa_estilado");
        Registry.register(Registries.BLOCK, id, ESTILADO_BLOCK);
        Registry.register(Registries.ITEM, id, ESTILADO_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id, ESTILADO_BLOCK_ENTITY);
        Identifier creativaId = Identifier.of(Femclothes.MOD_ID, "mesa_estilado_creativa");
        Registry.register(Registries.BLOCK, creativaId, ESTILADO_CREATIVA);
        Registry.register(Registries.ITEM, creativaId, ESTILADO_CREATIVA_ITEM);

        // Click en la vista 3D (2026-10-01).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                .register(PonerApliquePayload.ID, PonerApliquePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(PonerApliquePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    var partes = com.femclothes.garment.Parte.values();
                    var caras = net.minecraft.util.math.Direction.values();
                    var superficies = com.femclothes.aplique.Aplique.Superficie.values();
                    if (payload.parte() < 0 || payload.parte() >= partes.length
                            || payload.cara() < 0 || payload.cara() >= caras.length
                            || payload.superficie() < 0 || payload.superficie() >= superficies.length) return;
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof EstiladoBlockEntity be
                            && be.canPlayerUse(context.player())) {
                        be.poner(partes[payload.parte()], payload.x(), payload.y(), payload.z(), caras[payload.cara()],
                                superficies[payload.superficie()], payload.padre());
                    }
                }));

        // Panel lateral: colocación, oscilación y blandura (2026-10-04).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                .register(AjustarApliquePayload.ID, AjustarApliquePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(AjustarApliquePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof EstiladoBlockEntity be
                            && be.canPlayerUse(context.player())) {
                        be.ajustar(payload.indice(), payload.colocacion(), payload.oscilacion(), payload.blandura());
                    }
                }));

        // Crear molde con nombre (2026-10-04).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S()
                .register(CrearMoldePayload.ID, CrearMoldePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(CrearMoldePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof EstiladoBlockEntity be
                            && be.canPlayerUse(context.player())) {
                        be.crearMolde(context.player(), payload.nombre());
                    }
                }));
    }

    private EstiladoMod() {}
}
