package com.femclothes.tinturas;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/** Misma geometría/textura que el bloque, para el ícono del inventario. */
public class TinturasItemGeoModel extends GeoModel<TinturasBlockItem> {
    private static final Identifier GEO =
            Identifier.of(Femclothes.MOD_ID, "geo/dye_station.geo.json");
    private static final Identifier TEX =
            Identifier.of(Femclothes.MOD_ID, "textures/block/dye_station_atlas.png");
    private static final Identifier ANIM =
            Identifier.of(Femclothes.MOD_ID, "animations/dye_station.animation.json");

    @Override public Identifier getModelResource(TinturasBlockItem item) { return GEO; }
    @Override public Identifier getTextureResource(TinturasBlockItem item) { return TEX; }
    @Override public Identifier getAnimationResource(TinturasBlockItem item) { return ANIM; }
}
