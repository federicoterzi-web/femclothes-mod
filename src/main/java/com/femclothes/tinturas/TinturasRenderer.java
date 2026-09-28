package com.femclothes.tinturas;

import com.femclothes.render.LedGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(TinturasMod.TINTURAS_BLOCK_ENTITY, ctx -> new TinturasRenderer()). */
public class TinturasRenderer extends GeoBlockRenderer<TinturasBlockEntity> {
    public TinturasRenderer() {
        super(new TinturasGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, TinturasGeoModel.ANCHO_ATLAS, TinturasGeoModel.ALTO_ATLAS,
                TinturasGeoModel.LEDS, TinturasGeoModel::coloresLed));
    }
}
