package com.femclothes.tinturas;

import com.femclothes.render.LedGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(TinturasMod.TINTURAS_BLOCK_ENTITY, ctx -> new TinturasRenderer()). */
public class TinturasRenderer extends GeoBlockRenderer<TinturasBlockEntity> {
    public TinturasRenderer() {
        super(new TinturasGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, TinturasGeoModel.ANCHO_ATLAS, TinturasGeoModel.ALTO_ATLAS,
                TinturasGeoModel.LEDS, TinturasGeoModel::coloresLed));
        // La prenda girando en el rodillo con su ícono real (2026-09-29,
        // "reemplazar esos huesos por el item nuevo"): ancla en un hijo de
        // "roller", así hereda el giro. Solo mientras tiñe.
        addRenderLayer(new com.femclothes.render.PrendaEnMaquinaLayer<>(this, "prenda_remera",
                be -> be.estado() == TinturasBlockEntity.Estado.TINIENDO ? be.getPrendaEntrada()
                        : net.minecraft.item.ItemStack.EMPTY,
                0f, 11.2f, 5.75f, 6.5f, com.femclothes.render.PrendaEnMaquinaLayer.Apoyo.PARADA_FRENTE_MAS_Z));
    }
}
