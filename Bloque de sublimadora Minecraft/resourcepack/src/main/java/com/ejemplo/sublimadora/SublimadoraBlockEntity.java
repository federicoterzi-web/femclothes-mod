package com.ejemplo.sublimadora;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity animado con GeckoLib 4.x.
 *
 * Dos cosas animadas: la tapa (controlador "tapa", lee la propiedad OPEN) y
 * las cuatro barras del display CMYK, que no son una animacion keyframeada
 * sino escala de hueso aplicada en SublimadoraGeoModel#setCustomAnimations.
 * Aca solo vive el dato: cuanta tinta queda, y su suavizado por tick.
 */
public class SublimadoraBlockEntity extends BlockEntity implements GeoBlockEntity {

    public static final int C = 0, M = 1, Y = 2, K = 3;
    private static final String[] CLAVES = { "TintaC", "TintaM", "TintaY", "TintaK" };

    private static final RawAnimation ABRIR = RawAnimation.begin()
            .thenPlay("animation.sublimadora.abrir")
            .thenLoop("animation.sublimadora.abierta");
    private static final RawAnimation CERRAR = RawAnimation.begin()
            .thenPlay("animation.sublimadora.cerrar")
            .thenLoop("animation.sublimadora.cerrada");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean ultimoEstado = false;

    /** tinta restante 0..1 por canal (valor real, objetivo del suavizado) */
    private final float[] tinta = { 0.85f, 0.60f, 0.35f, 0.70f };
    /** valor mostrado y valor del tick anterior, para interpolar el llenado */
    private final float[] mostrado = tinta.clone();
    private final float[] anterior = tinta.clone();

    public SublimadoraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SUBLIMADORA_ENTITY, pos, state);
    }

    // ── animacion de la tapa ─────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "tapa", 0, state -> {
            boolean abierta = getCachedState().get(SublimadoraBlock.OPEN);
            if (abierta != ultimoEstado) {
                ultimoEstado = abierta;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(abierta ? ABRIR : CERRAR);
        })
        .triggerableAnim("abrir", ABRIR)
        .triggerableAnim("cerrar", CERRAR));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ── tinta ────────────────────────────────────────────────────────

    /** Ticker (cliente y server): suaviza la barra hacia su valor real. */
    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, SublimadoraBlockEntity be) {
        for (int i = 0; i < 4; i++) {
            be.anterior[i] = be.mostrado[i];
            be.mostrado[i] += (be.tinta[i] - be.mostrado[i]) * 0.15f;
            if (Math.abs(be.tinta[i] - be.mostrado[i]) < 0.001f) be.mostrado[i] = be.tinta[i];
        }
    }

    /** Nivel a dibujar este frame, interpolado entre ticks. */
    public float getNivelInterpolado(int canal, float parcial) {
        return MathHelper.lerp(parcial, anterior[canal], mostrado[canal]);
    }

    public float getTinta(int canal) {
        return tinta[canal];
    }

    /** Llena un canal (cartucho nuevo). */
    public void setTinta(int canal, float valor) {
        tinta[canal] = MathHelper.clamp(valor, 0f, 1f);
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    /** Consume tinta al prensar. Devuelve false si algun canal esta vacio. */
    public boolean consumir(float cantidad) {
        for (int i = 0; i < 4; i++) if (tinta[i] <= 0f) return false;
        for (int i = 0; i < 4; i++) tinta[i] = Math.max(0f, tinta[i] - cantidad);
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        return true;
    }

    // ── persistencia y sincronizacion ────────────────────────────────
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        for (int i = 0; i < 4; i++) nbt.putFloat(CLAVES[i], tinta[i]);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        for (int i = 0; i < 4; i++) {
            if (nbt.contains(CLAVES[i])) tinta[i] = nbt.getFloat(CLAVES[i]);
            mostrado[i] = tinta[i];
            anterior[i] = tinta[i];
        }
    }

    /** El cliente necesita los niveles para dibujar el display. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
