package com.femclothes.tinturas;

import com.femclothes.render.LedGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Registro (ClientModInitializer): BlockEntityRendererFactories.register(TinturasMod.TINTURAS_BLOCK_ENTITY, ctx -> new TinturasRenderer()). */
public class TinturasRenderer extends GeoBlockRenderer<TinturasBlockEntity> {
    public TinturasRenderer() {
        super(new TinturasGeoModel());
        addRenderLayer(new LedGlowLayer<>(this, TinturasGeoModel.ANCHO_ATLAS, TinturasGeoModel.ALTO_ATLAS,
                TinturasGeoModel.LEDS, TinturasGeoModel::coloresLed));
        // La prenda enrollada en el rodillo con su ícono real (2026-09-29,
        // "reemplazar esos huesos por el item nuevo" / "enredar el icono
        // alrededor del rodillo"): ancla en un hijo de "roller", así hereda
        // el giro. Solo mientras tiñe.
        addRenderLayer(new com.femclothes.render.PrendaEnMaquinaLayer<>(this, "prenda_remera",
                be -> be.estado() == TinturasBlockEntity.Estado.TINIENDO ? be.getPrendaEntrada()
                        : net.minecraft.item.ItemStack.EMPTY,
                // Enrollada (2026-09-29): eje del rodillo en (y 12.2, z 3.2), largo 12.4;
                // su sección (dos cuadrados de 2.9 cruzados) llega a ~2.05 del eje.
                0f, 12.2f, 3.2f, 12f, 2.2f, com.femclothes.render.PrendaEnMaquinaLayer.Apoyo.ENROLLADA_EJE_X));
    }
}
