package com.femclothes.cinta;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Empalme de cintas (2026-10-05, "un empalme que una hasta tres entradas laterales de conveyor belts y una superior
 * de tolva y una salida"): recibe por atrás, por los dos costados y por arriba (tolva) y entrega por el frente
 * ({@link #FACING}). Rotativo: alterna entre las entradas que tienen algo esperando.
 */
public class EmpalmeBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final MapCodec<EmpalmeBlock> CODEC = createCodec(EmpalmeBlock::new);
    private static final VoxelShape FORMA = Block.createCuboidShape(0, 0, 0, 16, 8, 16);

    public EmpalmeBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }

    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return FORMA; }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing())
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        boolean senal = world.isReceivingRedstonePower(pos);
        if (senal != state.get(POWERED)) world.setBlockState(pos, state.with(POWERED, senal), Block.NOTIFY_LISTENERS);
    }

    /** Click derecho: saca lo que lleva o pone un ítem (igual que la cinta; un bloque en la mano sin agacharse se coloca). */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.isSneaking() && stack.getItem() instanceof BlockItem) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(world.getBlockEntity(pos) instanceof EmpalmeBlockEntity be) || !be.carga().isEmpty()) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!world.isClient) {
            be.setStack(0, stack);
            if (!player.isCreative()) stack.decrement(1);
        }
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof EmpalmeBlockEntity be) || be.carga().isEmpty()) return ActionResult.PASS;
        if (!world.isClient) player.getInventory().offerOrDrop(be.removeStack(0));
        return ActionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new EmpalmeBlockEntity(pos, state); }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, CintaMod.EMPALME_BLOCK_ENTITY, EmpalmeBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof EmpalmeBlockEntity be) {
            ItemScatterer.spawn(world, pos, be);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
