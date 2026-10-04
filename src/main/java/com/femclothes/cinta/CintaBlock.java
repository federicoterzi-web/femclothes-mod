package com.femclothes.cinta;

import com.femclothes.util.ConSalida;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Cinta transportadora (2026-10-04, "activemos la linea de produccion textil"
 * + "geometria de cinta transportadora... agreguemos las curvas"). Lleva UNA
 * prenda por bloque hacia adelante ({@link #FACING}) y la entrega al inventario
 * de enfrente: otra cinta, una máquina por su cara izquierda, un cofre o una
 * tolva. Acepta por atrás, por los costados y por arriba (tolvas), no por
 * adelante.
 *
 * <p>La forma se arma sola como un riel: si no la alimenta nada por atrás pero
 * sí otra cinta (o una máquina que empuja hacia acá) desde un costado, se
 * curva hacia ese lado.
 */
public class CintaBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<Forma> FORMA = EnumProperty.of("forma", Forma.class);

    /** Cómo entra la prenda: de frente, o doblando desde el costado izquierdo/derecho. */
    public enum Forma implements StringIdentifiable {
        RECTA("recta"), CURVA_IZQ("curva_izq"), CURVA_DER("curva_der");

        private final String nombre;
        Forma(String nombre) { this.nombre = nombre; }
        @Override public String asString() { return nombre; }
    }

    private static final MapCodec<CintaBlock> CODEC = createCodec(CintaBlock::new);
    private static final VoxelShape FORMA_CAJA = Block.createCuboidShape(0, 0, 0, 16, 6, 16);

    public CintaBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(FORMA, Forma.RECTA));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMA);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return FORMA_CAJA;
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction frente = ctx.getHorizontalPlayerFacing();
        return getDefaultState().with(FACING, frente).with(FORMA, calcularForma(ctx.getWorld(), ctx.getBlockPos(), frente));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                    WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Forma forma = calcularForma(world, pos, state.get(FACING));
        return forma == state.get(FORMA) ? state : state.with(FORMA, forma);
    }

    /** Recta, salvo que no llegue nada por atrás y sí algo por un costado. */
    private static Forma calcularForma(BlockView mundo, BlockPos pos, Direction frente) {
        if (alimentaDesde(mundo, pos, frente.getOpposite(), frente)) return Forma.RECTA;
        if (alimentaDesde(mundo, pos, frente.rotateYCounterclockwise(), frente.rotateYClockwise())) return Forma.CURVA_IZQ;
        if (alimentaDesde(mundo, pos, frente.rotateYClockwise(), frente.rotateYCounterclockwise())) return Forma.CURVA_DER;
        return Forma.RECTA;
    }

    /** ¿El vecino de ese lado empuja hacia {@code pos}? ({@code haciaMi} = la dirección en que mira/empuja para llegar). */
    private static boolean alimentaDesde(BlockView mundo, BlockPos pos, Direction lado, Direction haciaMi) {
        BlockPos vecino = pos.offset(lado);
        BlockState estado = mundo.getBlockState(vecino);
        if (estado.getBlock() instanceof CintaBlock) return estado.get(FACING) == haciaMi;
        return mundo.getBlockEntity(vecino) instanceof ConSalida maquina && maquina.ladoSalida() == haciaMi;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CintaBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, CintaMod.CINTA_BLOCK_ENTITY, CintaBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof CintaBlockEntity be) {
            ItemScatterer.spawn(world, pos, be);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
