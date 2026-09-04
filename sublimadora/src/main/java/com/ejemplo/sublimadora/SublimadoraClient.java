package com.ejemplo.sublimadora;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class SublimadoraClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // El bloque es INVISIBLE en su blockstate: todo lo dibuja este
        // renderer de GeckoLib, incluida la tapa animada.
        BlockEntityRendererFactories.register(ModBlocks.SUBLIMADORA_ENTITY,
                ctx -> new SublimadoraRenderer());
    }
}
