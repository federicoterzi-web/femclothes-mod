package com.ejemplo.sublimadora;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * La misma geometria y la misma textura que usa el bloque, pero para el icono
 * del inventario. Apunta a los mismos archivos: lo que edites en Blockbench
 * cambia los dos a la vez.
 */
public class SublimadoraItemGeoModel extends GeoModel<SublimadoraBlockItem> {

    private static final Identifier GEO =
        Identifier.of("sublimadora", "geo/sublimadora.geo.json");
    private static final Identifier TEX =
        Identifier.of("sublimadora", "textures/block/sublimadora_atlas.png");
    private static final Identifier ANIM =
        Identifier.of("sublimadora", "animations/sublimadora.animation.json");

    @Override public Identifier getModelResource(SublimadoraBlockItem item) { return GEO; }
    @Override public Identifier getTextureResource(SublimadoraBlockItem item) { return TEX; }
    @Override public Identifier getAnimationResource(SublimadoraBlockItem item) { return ANIM; }

    @Override
    public void setCustomAnimations(SublimadoraBlockItem item, long instanceId,
                                    AnimationState<SublimadoraBlockItem> state) {
        super.setCustomAnimations(item, instanceId, state);

        // Sin controlador de animacion la tapa se queda en su pose de reposo,
        // que es cerrada. Lo unico que hay que apagar a mano son las piezas
        // que dependen del estado de una maquina que aca no existe: el item
        // es una maquina vacia y apagada.
        esconder("remera");
        esconder("led_rojo");
        esconder("led_verde");

        // Los tanques del icono se ven llenos. Es mentira -sale vacia- pero
        // sin esto las cuatro barras desaparecen y el frente queda muerto.
        for (String barra : new String[] { "ink_c", "ink_m", "ink_y", "ink_k" }) {
            GeoBone hueso = getAnimationProcessor().getBone(barra);
            if (hueso != null) {
                hueso.setScaleY(1f);
                hueso.setHidden(false);
            }
        }
    }

    private void esconder(String nombre) {
        GeoBone hueso = getAnimationProcessor().getBone(nombre);
        if (hueso != null) hueso.setHidden(true);
    }
}
