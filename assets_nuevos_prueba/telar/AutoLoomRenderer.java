package com.ejemplo.sublimadora;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AutoLoomRenderer extends GeoBlockRenderer<AutoLoomBlockEntity> {
    public AutoLoomRenderer() {
        super(new AutoLoomGeoModel());
    }
}
