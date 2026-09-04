package com.ejemplo.sublimadora;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renderer del bloque. GeckoLib ya lo orienta con la propiedad
 * HORIZONTAL_FACING del estado, no hace falta rotar a mano.
 *
 * Registro (Fabric, en el ClientModInitializer):
 *   BlockEntityRendererFactories.register(ModBlocks.SUBLIMADORA_ENTITY,
 *       ctx -> new SublimadoraRenderer());
 */
public class SublimadoraRenderer extends GeoBlockRenderer<SublimadoraBlockEntity> {
    public SublimadoraRenderer() {
        super(new SublimadoraGeoModel());
    }
}
