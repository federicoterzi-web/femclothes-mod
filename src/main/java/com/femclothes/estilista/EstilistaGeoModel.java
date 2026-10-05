package com.femclothes.estilista;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ata la Estilista a su geometría, textura y animaciones (modelo generado por {@code tools/generar_modelo_estilista.py}:
 * la base de la Modeladora con la tapa del auto_styler). La barra "progress" se llena como en las otras máquinas; los
 * huesos {@code garment}, {@code held} y {@code placed_*} (la prenda y los apliques de juguete del pórtico) quedan
 * ocultos: la prenda real la dibuja {@link com.femclothes.render.PrendaEnMaquinaLayer}.
 */
public class EstilistaGeoModel extends GeoModel<EstilistaBlockEntity> {

    static final Identifier GEO = Identifier.of(Femclothes.MOD_ID, "geo/estilista.geo.json");
    static final Identifier TEX = Identifier.of(Femclothes.MOD_ID, "textures/block/estilista_atlas.png");
    static final Identifier ANIM = Identifier.of(Femclothes.MOD_ID, "animations/estilista.animation.json");
    /** El atlas de la Modeladora ocupa la mitad de arriba (los LED y la pantalla siguen en sus rects de siempre). */
    static final int ANCHO_ATLAS = 128, ALTO_ATLAS = 256;
    static final String[] HUESOS_DE_PRENDA = {"garment", "held", "placed_1", "placed_2", "placed_3", "placed_4"};
    private static final com.femclothes.render.PantallaLed.Rect[] LEDS = {
            new com.femclothes.render.PantallaLed.Rect(96, 16, 1, 1),
            new com.femclothes.render.PantallaLed.Rect(112, 16, 1, 1),
            new com.femclothes.render.PantallaLed.Rect(0, 32, 1, 1),
    };
    private static final int VERDE = 0xFF00FF00, ROJO = 0xFF0000FF;
    private static final float FACTOR_PROGRESO = 4.4f / 2.6371f;   // el mismo cubo de la Modeladora

    public static com.femclothes.render.PantallaLed.Rect[] leds() { return LEDS; }

    static int[] coloresLed(EstilistaBlockEntity be) {
        int verde = 0, rojo = 0;
        if (be.estado() == EstilistaBlockEntity.Estado.LISTO) {
            verde = VERDE;
        } else if (be.estado() == EstilistaBlockEntity.Estado.PROCESANDO) {
            long t = be.getWorld() == null ? 0 : be.getWorld().getTime();
            if (t % 20 < 10) rojo = ROJO;
        }
        return new int[]{verde, rojo, 0};
    }

    @Override public Identifier getModelResource(EstilistaBlockEntity be) { return GEO; }

    @Override
    public Identifier getTextureResource(EstilistaBlockEntity be) {
        return com.femclothes.util.MaquinaCreativa.textura(TEX, com.femclothes.util.MaquinaCreativa.es(be));
    }

    @Override public Identifier getAnimationResource(EstilistaBlockEntity be) { return ANIM; }

    @Override
    public void setCustomAnimations(EstilistaBlockEntity be, long instanceId, AnimationState<EstilistaBlockEntity> state) {
        super.setCustomAnimations(be, instanceId, state);
        GeoBone progreso = getAnimationProcessor().getBone("progress");
        if (progreso != null) {
            float nivel = switch (be.estado()) {
                case PROCESANDO -> be.progreso() / (float) EstilistaBlockEntity.TICKS_PROCESO;
                case LISTO -> 1f;
                default -> 0f;
            };
            progreso.setScaleX(Math.max(0.001f, nivel * FACTOR_PROGRESO));
            progreso.setHidden(nivel <= 0.001f);
        }
        // La prenda real la dibuja la capa; los juguetes del pórtico solo se ven mientras trabaja.
        boolean trabajando = be.estado() == EstilistaBlockEntity.Estado.PROCESANDO;
        for (String h : HUESOS_DE_PRENDA) {
            GeoBone b = getAnimationProcessor().getBone(h);
            if (b == null) continue;
            if (h.equals("garment")) b.setHidden(true);
            else b.setHidden(!trabajando);
        }
    }
}
