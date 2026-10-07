package com.ejemplo.sublimadora;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class AutoLoomGeoModel extends GeoModel<AutoLoomBlockEntity> {
    private static final Identifier GEO = Identifier.of("sublimadora", "geo/auto_loom.geo.json");
    private static final Identifier TEX = Identifier.of("sublimadora", "textures/block/auto_loom_atlas.png");
    private static final Identifier ANIM = Identifier.of("sublimadora", "animations/auto_loom.animation.json");

    @Override public Identifier getModelResource(AutoLoomBlockEntity be) { return GEO; }
    @Override public Identifier getTextureResource(AutoLoomBlockEntity be) { return TEX; }
    @Override public Identifier getAnimationResource(AutoLoomBlockEntity be) { return ANIM; }
}
