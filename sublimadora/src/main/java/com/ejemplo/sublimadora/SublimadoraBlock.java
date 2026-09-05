package com.ejemplo.sublimadora;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
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
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof SublimadoraBlockEntity be)) return ActionResult.PASS;

        ItemStack enMano = player.getStackInHand(player.getActiveHand());

        // Con la tapa abierta, el click derecho CARGA cosas. Con la mano vacia
        // o la tapa cerrada, abre y cierra.
        if (state.get(OPEN) && !enMano.isEmpty()) {
            int canal = canalDeTinte(enMano);
            if (canal >= 0) {
                if (be.cargarTinta(canal, 1)) {
                    if (!player.isCreative()) enMano.decrement(1);
                    sonar(world, pos, SoundEvents.ITEM_BUCKET_FILL, 0.8f);
                }
                return ActionResult.CONSUME;
            }
            if (enMano.getItem() == ModItems.REMERA && !RemeraItem.estaEstampada(enMano)) {
                if (be.ponerRemera(enMano)) {
                    if (!player.isCreative()) enMano.decrement(1);
                    sonar(world, pos, SoundEvents.BLOCK_WOOL_PLACE, 1.0f);
                }
                return ActionResult.CONSUME;
            }
            if (esFoto(enMano)) {
                if (be.ponerFoto(enMano, uuidDeFoto(enMano))) {
                    if (!player.isCreative()) enMano.decrement(1);
                    sonar(world, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f);
                }
                return ActionResult.CONSUME;
            }
        }

        // Mano vacia con la tapa abierta: retirar lo que haya.
        if (state.get(OPEN) && enMano.isEmpty()) {
            ItemStack sacado = be.retirar();
            if (!sacado.isEmpty()) {
                player.getInventory().offerOrDrop(sacado);
                sonar(world, pos, SoundEvents.ENTITY_ITEM_PICKUP, 1.2f);
                return ActionResult.CONSUME;
            }
        }

        boolean abriendo = !state.get(OPEN);
        world.setBlockState(pos, state.with(OPEN, abriendo), Block.NOTIFY_ALL);
        sonar(world, pos, abriendo ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN
                                   : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.2f);
        // Cerrar la tapa es lo que dispara el prensado.
        if (!abriendo) be.intentarPrensar();
        return ActionResult.SUCCESS;
    }

    private static void sonar(World world, BlockPos pos, net.minecraft.sound.SoundEvent ev, float tono) {
        world.playSound(null, pos, ev, SoundCategory.BLOCKS, 0.6f, tono);
    }

    /** Canal CMYK del tinte que se tenga en la mano, o -1. */
    private static int canalDeTinte(ItemStack stack) {
        if (stack.getItem() == net.minecraft.item.Items.CYAN_DYE) return SublimadoraBlockEntity.C;
        if (stack.getItem() == net.minecraft.item.Items.MAGENTA_DYE) return SublimadoraBlockEntity.M;
        if (stack.getItem() == net.minecraft.item.Items.YELLOW_DYE) return SublimadoraBlockEntity.Y;
        if (stack.getItem() == net.minecraft.item.Items.BLACK_DYE) return SublimadoraBlockEntity.K;
        return -1;
    }

    /**
     * Reconoce la foto de Camerapture SIN depender del mod: se compara el id
     * registrado. Asi la sublimadora compila y corre igual si Camerapture no
     * esta instalado — simplemente no vas a tener fotos que poner.
     */
    private static boolean esFoto(ItemStack stack) {
        return net.minecraft.registry.Registries.ITEM.getId(stack.getItem())
                .toString().equals("camerapture:picture");
    }

    /**
     * UUID de la foto. TODO: leerlo del componente PICTURE_DATA de Camerapture
     * cuando lo agreguemos como dependencia; por ahora se deriva del stack
     * para poder probar el ciclo completo.
     */
    private static java.util.UUID uuidDeFoto(ItemStack stack) {
        return java.util.UUID.nameUUIDFromBytes(stack.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
