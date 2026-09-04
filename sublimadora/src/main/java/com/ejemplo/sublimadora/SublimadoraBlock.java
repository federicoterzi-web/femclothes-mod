package com.ejemplo.sublimadora;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Sublimadora: bloque de 16x16x16 con tapa abisagrada.
 * El render lo hace GeckoLib desde el block entity, asi que para el
 * renderer de mundo el bloque es INVISIBLE (asi lo pide la wiki de GeckoLib).
 */
public class SublimadoraBlock extends BlockWithEntity {
    public static final MapCodec<SublimadoraBlock> CODEC = createCodec(SublimadoraBlock::new);

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

    public SublimadoraBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            boolean abriendo = !state.get(OPEN);
            world.setBlockState(pos, state.with(OPEN, abriendo), Block.NOTIFY_ALL);
            world.playSound(null, pos,
                abriendo ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
                SoundCategory.BLOCKS, 0.6f, 1.2f);
            if (!abriendo && world.getBlockEntity(pos) instanceof SublimadoraBlockEntity be) {
                be.consumir(0.02f);   // cerrar la tapa = prensar: gasta tinta
            }
            // Alternativa: disparar la animacion desde el server en vez de leerla del estado.
            // if (world.getBlockEntity(pos) instanceof SublimadoraBlockEntity be)
            //     be.triggerAnim("tapa", abriendo ? "abrir" : "cerrar");
        }
        return ActionResult.SUCCESS;
    }

    /** GeckoLib dibuja el bloque completo desde el block entity renderer. */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;   // alternativa: ENTITYBLOCK_ANIMATED
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SublimadoraBlockEntity(pos, state);
    }

    /** Ticker en los dos lados: el cliente lo necesita para suavizar las barras de tinta. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlocks.SUBLIMADORA_ENTITY, SublimadoraBlockEntity::tick);
    }
}
