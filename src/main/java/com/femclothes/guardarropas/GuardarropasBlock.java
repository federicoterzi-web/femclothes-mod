package com.femclothes.guardarropas;

import net.minecraft.item.ItemStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Guardarropas — PLACEHOLDER: bloque simple, sin GeckoLib ni orientación
 * (no tiene panel físico como las otras 3), modelo vanilla normal. Click
 * derecho (con cualquier mano) abre la pantalla con los 4 slots y el
 * preview combinado.
 */
public class GuardarropasBlock extends BlockWithEntity {

    private static final MapCodec<GuardarropasBlock> CODEC = createCodec(GuardarropasBlock::new);
    private static final VoxelShape FORMA = VoxelShapes.cuboid(0, 0, 0, 1, 1, 1);

    public GuardarropasBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return FORMA;
    }

    /**
     * {@code BlockWithEntity} es INVISIBLE por defecto (ver su propio
     * javadoc) — pensado para bloques tipo GeckoLib que dibujan desde el
     * block entity. Este es un modelo vanilla normal, así que hay que
     * pisarlo — a las otras 3 estaciones (que SÍ son GeckoLib) también
     * les tocó acordarse de esto (2026-09-20, "anota eso porque paso con
     * todos"), solo que ahí el valor correcto es INVISIBLE, no MODEL.
     */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GuardarropasBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof GuardarropasBlockEntity be) {
            player.openHandledScreen(be);
        }
        return ActionResult.SUCCESS;
    }

    /** Cae con todo adentro, como una shulker (ver {@link com.femclothes.util.DropMaquina}). */
    @Override
    protected java.util.List<ItemStack> getDroppedStacks(BlockState state,
            net.minecraft.loot.context.LootContextParameterSet.Builder builder) {
        return com.femclothes.util.DropMaquina.drops(this, builder);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        com.femclothes.util.DropMaquina.enCreativo(world, pos, player, this,
                !(world.getBlockEntity(pos) instanceof GuardarropasBlockEntity be) || be.isEmpty());
        return super.onBreak(world, pos, state, player);
    }
}
