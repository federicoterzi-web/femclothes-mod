package com.femclothes.estilado;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Mesa de estilado — por ahora solo existe para que GeckoLib la dibuje
 * (modelo {@code styling_table}, sin huesos animados). Acá va a vivir el
 * estado de la mesa de apliques (2026-09-30, "una mesa para agregar
 * apliques anclables a las prendas... moños, mariposas... bijouterie,
 * edición de chokers") cuando se defina la mecánica.
 */
public class EstiladoBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public EstiladoBlockEntity(BlockPos pos, BlockState state) {
        super(EstiladoMod.ESTILADO_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
