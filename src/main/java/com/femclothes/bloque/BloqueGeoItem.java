package com.femclothes.bloque;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Ítem de un bloque GeckoLib, dibujado con la misma malla que el bloque
 * puesto (icono = bloque real) — mismo patrón que {@code TinturasBlockItem}/
 * {@code ModeladoBlockItem}, pero genérico por nombre de asset (ver
 * {@link ModeloGeo}). El modelo JSON del ítem tiene que ser
 * {@code builtin/entity} para que GeckoLib se enganche.
 */
public class BloqueGeoItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String modelo;

    public BloqueGeoItem(Block block, Settings settings, String modelo) {
        super(block, settings);
        this.modelo = modelo;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<BloqueGeoItem> renderer;

            @Override
            public net.minecraft.client.render.item.BuiltinModelItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) this.renderer = new GeoItemRenderer<>(new ModeloGeo<>(modelo));
                return this.renderer;
            }
        });
    }
}
