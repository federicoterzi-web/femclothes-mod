package com.femclothes.bloque;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeoModel genérico para los bloques que no necesitan lógica propia en el
 * modelo: geo/textura/animación salen del nombre del asset
 * ({@code geo/<nombre>.geo.json}, {@code textures/block/<nombre>_atlas.png},
 * {@code animations/<nombre>.animation.json}). Lo usan el Guardarropas y la
 * Mesa de estilado (bloque e ítem), traídos del zip "Bloque de sublimadora
 * Minecraft" (2026-09-30, "hay 3 bloques nuevos que hay que hacer funcionar")
 * — antes cada máquina tenía su trío GeoModel/ItemGeoModel/ItemRenderer
 * copiado a mano con solo el nombre cambiado.
 */
public class ModeloGeo<T extends GeoAnimatable> extends GeoModel<T> {

    private final Identifier geo;
    private final Identifier textura;
    private final Identifier animacion;

    public ModeloGeo(String nombre) {
        this.geo = Identifier.of(Femclothes.MOD_ID, "geo/" + nombre + ".geo.json");
        this.textura = Identifier.of(Femclothes.MOD_ID, "textures/block/" + nombre + "_atlas.png");
        this.animacion = Identifier.of(Femclothes.MOD_ID, "animations/" + nombre + ".animation.json");
    }

    @Override public Identifier getModelResource(T animatable) { return geo; }
    @Override public Identifier getTextureResource(T animatable) { return textura; }
    @Override public Identifier getAnimationResource(T animatable) { return animacion; }
}
