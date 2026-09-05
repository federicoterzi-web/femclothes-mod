package com.ejemplo.sublimadora;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ata el block entity a su geometria, su textura y sus animaciones, y
 * escala las cuatro barras del display CMYK segun la tinta restante.
 *
 * Cada barra es un hueso propio (ink_c, ink_m, ink_y, ink_k) con el pivote
 * en su base, asi que escalar Y de 0 a 1 la llena de abajo hacia arriba.
 */
public class SublimadoraGeoModel extends GeoModel<SublimadoraBlockEntity> {
    private static final Identifier GEO =
        Identifier.of("sublimadora", "geo/sublimadora.geo.json");
    private static final Identifier TEX =
        Identifier.of("sublimadora", "textures/block/sublimadora_atlas.png");
    private static final Identifier ANIM =
        Identifier.of("sublimadora", "animations/sublimadora.animation.json");

    private static final String[] HUESOS = { "ink_c", "ink_m", "ink_y", "ink_k" };

    @Override public Identifier getModelResource(SublimadoraBlockEntity be) { return GEO; }
    @Override public Identifier getTextureResource(SublimadoraBlockEntity be) { return TEX; }
    @Override public Identifier getAnimationResource(SublimadoraBlockEntity be) { return ANIM; }

    @Override
    public void setCustomAnimations(SublimadoraBlockEntity be, long instanceId, AnimationState<SublimadoraBlockEntity> state) {
        super.setCustomAnimations(be, instanceId, state);

        // LEDs del panel: rojo parpadeando mientras prensa, verde fijo cuando
        // esta lista, los dos apagados en reposo. Se prenden y apagan con
        // setHidden, igual que las barras de tinta.
        SublimadoraBlockEntity.Estado estado = be.getEstado();
        boolean rojo = false, verde = false;
        if (estado == SublimadoraBlockEntity.Estado.PRENSANDO) {
            // Medio segundo prendido, medio apagado.
            long t = be.getWorld() == null ? 0 : be.getWorld().getTime();
            rojo = (t % 20) < 10;
        } else if (estado == SublimadoraBlockEntity.Estado.LISTO) {
            verde = true;
        }
        GeoBone ledRojo = getAnimationProcessor().getBone("led_rojo");
        if (ledRojo != null) ledRojo.setHidden(!rojo);
        GeoBone ledVerde = getAnimationProcessor().getBone("led_verde");
        if (ledVerde != null) ledVerde.setHidden(!verde);

        // La remera sobre la plancha solo se ve si hay una cargada, o si la
        // recien estampada todavia no se retiro.
        GeoBone remera = getAnimationProcessor().getBone("remera");
        if (remera != null) {
            remera.setHidden(be.getRemera().isEmpty() && be.getSalida().isEmpty());
        }

        float parcial = state == null ? 1f : (float) state.getPartialTick();
        for (int i = 0; i < HUESOS.length; i++) {
            GeoBone barra = getAnimationProcessor().getBone(HUESOS[i]);
            if (barra == null) continue;
            float nivel = be.getNivelInterpolado(i, parcial);
            barra.setScaleY(Math.max(0.001f, nivel));   // 0 = vacia, 1 = llena
            barra.setHidden(nivel <= 0.001f);           // tanque vacio: barra invisible
        }
    }
}
