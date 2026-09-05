package com.ejemplo.sublimadora;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
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

        // La remera la dibuja un renderer propio para poder estamparle la
        // foto. item/remera_base no lo referencia ningun blockstate ni ningun
        // item, asi que hay que pedir que igual se hornee: si no, el renderer
        // no lo encuentra y la remera se ve invisible.
        ModelLoadingPlugin.register(contexto -> contexto.addModels(RemeraItemRenderer.MODELO_BASE));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.REMERA, new RemeraItemRenderer());

        // La remera puesta sobre el cuerpo, solo si Trinkets esta. El puente
        // vive en otra clase para que la JVM no tenga que resolver clases de
        // Trinkets cuando no esta instalado.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            TrinketsClientCompat.registrarRenderer();
        }
    }
}
