package com.ejemplo.sublimadora;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * El item del bloque, dibujado por GeckoLib con la MISMA malla que el bloque
 * puesto en el mundo.
 *
 * Antes el icono del inventario salia de un modelo vanilla escrito a mano
 * aparte, con su propia docena de texturas sueltas. Eso significaba mantener
 * dos veces la misma maquina y que el icono no se enterara nunca de lo que se
 * editara en Blockbench: la queja "no estan las medidas nuevas" era eso.
 */
public class SublimadoraBlockItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SublimadoraBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    /** El icono es una pose fija, no hay nada que animar. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * El renderer se construye adentro de la clase anonima a proposito: asi la
     * JVM no tiene que resolver clases de cliente cuando este item se registra
     * en un servidor dedicado, donde este metodo no se llama nunca.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SublimadoraItemRenderer renderer;

            @Override
            public net.minecraft.client.render.item.BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new SublimadoraItemRenderer();
                return this.renderer;
            }
        });
    }
}
