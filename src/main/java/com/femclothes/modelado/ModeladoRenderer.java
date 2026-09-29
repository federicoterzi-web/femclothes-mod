package com.femclothes.modelado;

import com.femclothes.render.LedGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(ModeladoMod.MODELADO_BLOCK_ENTITY, ctx -> new ModeladoRenderer()). */
public class ModeladoRenderer extends GeoBlockRenderer<ModeladoBlockEntity> {
    public ModeladoRenderer() {
        super(new ModeladoGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, ModeladoGeoModel.ANCHO_ATLAS, ModeladoGeoModel.ALTO_ATLAS,
                ModeladoGeoModel.LEDS, ModeladoGeoModel::coloresLed));
        // La prenda cargada, con su ícono real acostado sobre la mesa (2026-09-29,
        // "reemplazar esos huesos por el item nuevo") — centro del hueso REMERA.
        addRenderLayer(new com.femclothes.render.PrendaEnMaquinaLayer<>(this, "REMERA",
                be -> be.prendaVisible(), -0.4f, 11.85f, -0.95f, 10f,
                com.femclothes.render.PrendaEnMaquinaLayer.Apoyo.ACOSTADA_FRENTE_MENOS_Z));
    }
}
