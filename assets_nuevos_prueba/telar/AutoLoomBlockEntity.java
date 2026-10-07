package com.ejemplo.sublimadora;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Controlador "telar": con WORKING en true repite "trabajo" (4 s, cuatro
 * pasadas: lizos se invierten, lanzadera cruza, batan golpea); si no, "quieto".
 */
public class AutoLoomBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation TRABAJO = RawAnimation.begin().thenLoop("animation.auto_loom.trabajo");
    private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("animation.auto_loom.quieto");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AutoLoomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.AUTO_LOOM_ENTITY, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "telar", 4, state ->
                state.setAndContinue(getCachedState().get(AutoLoomBlock.WORKING) ? TRABAJO : QUIETO)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
