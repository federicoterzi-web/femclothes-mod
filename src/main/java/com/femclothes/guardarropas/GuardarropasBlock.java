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
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Guardarropas. Click derecho (con cualquier mano) abre la pantalla con los
 * 16 slots y el preview combinado.
 *
 * <p>Desde 2026-09-30 ("hay 3 bloques nuevos que hay que hacer funcionar")
 * usa el modelo GeckoLib {@code wardrobe} del zip: orientable ({@link #FACING})
 * y con la puerta animada — {@link #OPEN} queda en true mientras alguien
 * tenga la pantalla abierta (mismo criterio que un cofre, ver
 * {@code GuardarropasBlockEntity#onOpen}), y el controlador de GeckoLib lee
 * ese estado igual que la tapa de la Sublimadora.
 */
public class GuardarropasBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final MapCodec<GuardarropasBlock> CODEC = createCodec(GuardarropasBlock::new);
    private static final VoxelShape FORMA = VoxelShapes.cuboid(0, 0, 0, 1, 1, 1);

    public GuardarropasBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    /**
     * Con {@code getOpposite()}, como Modeladora/Sublimadora: el frente del
     * modelo {@code wardrobe} (la puerta) está en -Z, la convención que
     * asume GeckoLib — a diferencia de {@code dye_station}, que lo tiene
     * al revés (ver {@code TinturasBlock#getPlacementState}).
     */
    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return FORMA;
    }

    /**
     * INVISIBLE: desde 2026-09-30 lo dibuja GeckoLib desde el block entity
     * (modelo {@code wardrobe}). Antes era MODEL (cubo vanilla placeholder)
     * — ver la trampa de {@code BlockWithEntity.getRenderType} en CLAUDE.md.
     */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
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
