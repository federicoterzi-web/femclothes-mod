package com.femclothes.modelado;

import com.femclothes.render.LedGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(ModeladoMod.MODELADO_BLOCK_ENTITY, ctx -> new ModeladoRenderer()). */
public class ModeladoRenderer extends GeoBlockRenderer<ModeladoBlockEntity> {
    public ModeladoRenderer() {
        super(new ModeladoGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, ModeladoGeoModel.ANCHO_ATLAS, ModeladoGeoModel.ALTO_ATLAS,
                ModeladoGeoModel.LEDS, ModeladoGeoModel::coloresLed));
    }
}
