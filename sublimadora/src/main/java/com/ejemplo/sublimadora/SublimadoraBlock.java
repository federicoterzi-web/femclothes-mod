package com.ejemplo.sublimadora;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
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

    /**
     * Interaccion CON item en la mano. En 1.21.1 esto es lo que se llama
     * primero; onUse solo corre despues, y solo si aca devolvemos PASS.
     * Antes tenia esta logica dentro de onUse leyendo getActiveHand(), que es
     * la mano de "usar" un item (comer, tensar el arco) y no la de la
     * interaccion — por eso no matcheaba y terminaba cerrando la tapa.
     */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
                                             BlockPos pos, PlayerEntity player, Hand hand,
                                             BlockHitResult hit) {
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof SublimadoraBlockEntity be)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Solo se carga con la tapa abierta.
        if (!state.get(OPEN)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        int canal = canalDeTinte(stack);
        if (canal >= 0) {
            if (be.cargarTinta(canal, 1)) {
                if (!player.isCreative()) stack.decrement(1);
                sonar(world, pos, SoundEvents.ITEM_BUCKET_FILL, 0.8f);
            }
            return ItemActionResult.CONSUME;
        }
        if (stack.getItem() == ModItems.REMERA && !RemeraItem.estaEstampada(stack)) {
            if (be.ponerRemera(stack)) {
                if (!player.isCreative()) stack.decrement(1);
                sonar(world, pos, SoundEvents.BLOCK_WOOL_PLACE, 1.0f);
            }
            return ItemActionResult.CONSUME;
        }
        if (esFoto(stack)) {
            if (be.ponerFoto(stack, uuidDeFoto(stack))) {
                if (!player.isCreative()) stack.decrement(1);
                sonar(world, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f);
            }
            return ItemActionResult.CONSUME;
        }
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Interaccion con la MANO VACIA: retirar, o abrir y cerrar la tapa. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof SublimadoraBlockEntity be)) return ActionResult.PASS;

        // Retirar NO puede tener prioridad sobre cerrar: si la tuviera, una vez
        // cargada la remera el click te la sacaria en vez de arrancar el
        // prensado, y no habria forma de empezar.
        //
        // - Remera lista (luz verde): el click la retira. Es lo que uno espera.
        // - Agachado: descarga lo que haya, para poder corregir.
        // - Si no: abre y cierra.
        if (state.get(OPEN) && (!be.getSalida().isEmpty() || player.isSneaking())) {
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

        // Cerrar la tapa es lo que dispara el prensado. Si no arranca, la
        // maquina lo dice: antes cerrabas la tapa, no pasaba nada, y no habia
        // forma de saber si faltaba tinta, la remera, la foto, o si estaba
        // rota. El silencio era indistinguible de un bug.
        if (!abriendo && !be.intentarPrensar()) {
            net.minecraft.text.Text aviso = be.queFalta();
            if (aviso != null) {
                sonar(world, pos, SoundEvents.BLOCK_DISPENSER_FAIL, 1.0f);
                player.sendMessage(aviso.copy().formatted(net.minecraft.util.Formatting.GOLD), true);
            }
        }
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
     * UUID de la foto, leido del componente PICTURE_DATA de Camerapture.
     *
     * El acceso vive en una clase aparte para que la JVM no la resuelva si
     * Camerapture no esta instalado — mismo patron que el puente con 3D Skin
     * Layers en femclothes.
     */
    private static java.util.UUID uuidDeFoto(ItemStack stack) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("camerapture")) return null;
        try {
            return CameraptureCompat.uuidDe(stack);
        } catch (Throwable ignorado) {
            return null;   // cambio de version del mod: mejor sin estampa que crashear
        }
    }
    /** GeckoLib dibuja el bloque completo desde el block entity renderer. */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
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
