package com.femclothes.modelado;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Misma geometría/textura que el bloque, para el ícono del inventario. */
public class ModeladoItemGeoModel extends GeoModel<ModeladoBlockItem> {
    private static final Identifier GEO =
            Identifier.of(Femclothes.MOD_ID, "geo/garment_shaper.geo.json");
    private static final Identifier TEX =
            Identifier.of(Femclothes.MOD_ID, "textures/block/garment_shaper_atlas.png");
    private static final Identifier ANIM =
            Identifier.of(Femclothes.MOD_ID, "animations/garment_shaper.animation.json");

    @Override public Identifier getModelResource(ModeladoBlockItem item) { return GEO; }
    @Override public Identifier getTextureResource(ModeladoBlockItem item) { return com.femclothes.util.MaquinaCreativa.textura(TEX, com.femclothes.util.MaquinaCreativa.es(item)); }
    @Override public Identifier getAnimationResource(ModeladoBlockItem item) { return ANIM; }

    @Override
    public void setCustomAnimations(ModeladoBlockItem item, long instanceId, AnimationState<ModeladoBlockItem> state) {
        super.setCustomAnimations(item, instanceId, state);
        // El icono es una pose fija: la barra de progreso ocultarse en 0
        // dejaría un hueco raro, así que se muestra a mitad de camino.
        GeoBone progreso = getAnimationProcessor().getBone("progress");
        if (progreso != null) {
            progreso.setScaleX(0.5f);
            progreso.setHidden(false);
        }
    }
}
