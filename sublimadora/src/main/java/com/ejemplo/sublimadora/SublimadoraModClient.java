package com.ejemplo.sublimadora;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class SublimadoraModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // El render del bloque lo hace GeckoLib desde el block entity.
        BlockEntityRendererFactories.register(
                ModBlocks.SUBLIMADORA_ENTITY, ctx -> new SublimadoraRenderer());

        // La remera tiene alfa: sin cutout el recorte se rellena y queda un cuadrado.
        // Cubre el modelo JSON del item en el inventario y la variante sin GeckoLib.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SUBLIMADORA, RenderLayer.getCutout());
    }
}
