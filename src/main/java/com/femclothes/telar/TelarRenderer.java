package com.femclothes.telar;

import com.femclothes.render.LedGlowLayer;
import com.femclothes.render.PrendaEnMaquinaLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(TelarMod.TELAR_BLOCK_ENTITY, ctx -> new TelarRenderer()). */
public class TelarRenderer extends GeoBlockRenderer<TelarBlockEntity> {
    public TelarRenderer() {
        super(new TelarGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, TelarGeoModel.ANCHO_ATLAS, TelarGeoModel.ALTO_ATLAS,
                TelarGeoModel.leds(), TelarGeoModel::coloresLed));
        addRenderLayer(new com.femclothes.render.PantallaGlowLayer<>(this,
                be -> com.femclothes.render.PantallaMaquina.glow(TelarGeoModel.atlas(be),
                        com.femclothes.render.PantallaMaquina.PANEL_128, be.getPos())));
        // La prenda que se está tejiendo, con su ícono real, tendida sobre los hilos de la urdimbre del telar (2026-10-07,
        // "la representacion grafica de la prenda arriba"); el hueso "base" no se mueve, así que el centro es fijo.
        addRenderLayer(new PrendaEnMaquinaLayer<>(this, "base", TelarBlockEntity::prendaTejiendo,
                0f, 13.8f, -0.5f, 8.4f, PrendaEnMaquinaLayer.Apoyo.ACOSTADA_FRENTE_MENOS_Z));
    }
}
