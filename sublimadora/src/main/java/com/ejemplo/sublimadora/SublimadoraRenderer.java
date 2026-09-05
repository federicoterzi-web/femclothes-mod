package com.ejemplo.sublimadora;

import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

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
        // Capa emisiva: los pixeles marcados en sublimadora_atlas_glowmask.png
        // se dibujan a luz plena, asi los LEDs se ven prendidos aunque el
        // bloque este en penumbra. El sufijo _glowmask lo resuelve GeckoLib
        // solo, a partir del nombre de la textura del modelo.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
