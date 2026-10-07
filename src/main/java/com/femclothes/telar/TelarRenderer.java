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
        // La prenda tejida sobre la bandeja de salida (el hueso "cargo").
        addRenderLayer(new PrendaEnMaquinaLayer<>(this, "cargo", TelarBlockEntity::salidaVisible,
                5.6f, 5.08f, 0f, 2.4f, PrendaEnMaquinaLayer.Apoyo.ACOSTADA_FRENTE_MENOS_Z));
    }
}
