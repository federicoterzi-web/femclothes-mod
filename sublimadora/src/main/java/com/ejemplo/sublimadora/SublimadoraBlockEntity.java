package com.ejemplo.sublimadora;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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

    /** Cuantas cargas entran por color. Una carga = un tinte. */
    public static final int CARGA_MAXIMA = 16;
    /** 20 segundos a 20 ticks. */
    public static final int TICKS_PRENSADO = 400;
    /** Lo que tarda la tapa en bajar: 0.3 s del archivo de animacion. */
    private static final int TICKS_CIERRE = 6;

    public enum Estado { REPOSO, PRENSANDO, LISTO }

    private static final String[] CLAVES = { "TintaC", "TintaM", "TintaY", "TintaK" };

    private static final RawAnimation ABRIR = RawAnimation.begin()
            .thenPlay("animation.sublimadora.abrir")
            .thenLoop("animation.sublimadora.abierta");
    private static final RawAnimation CERRAR = RawAnimation.begin()
            .thenPlay("animation.sublimadora.cerrar")
            .thenLoop("animation.sublimadora.cerrada");

    // Poses estaticas, sin transicion: son las que hay que usar al cargar el
    // chunk. ABRIR y CERRAR arrancan desde la pose CONTRARIA, asi que usarlas
    // al cargar hacia que un bloque cerrado apareciera abierto y se cerrara
    // solo delante del jugador.
    private static final RawAnimation ABIERTA = RawAnimation.begin()
            .thenLoop("animation.sublimadora.abierta");
    private static final RawAnimation CERRADA = RawAnimation.begin()
            .thenLoop("animation.sublimadora.cerrada");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean ultimoEstado = false;
    /**
     * Que animacion pedirle al controlador. Tiene que ser ESTABLE entre
     * cambios de estado: si cada frame se pide la transicion, GeckoLib la
     * reinicia y la tapa se queda reproduciendo "cerrar" para siempre, que
     * arranca en -104 grados. O sea, abierta.
     */
    private RawAnimation actual = null;

    /** tinta restante 0..1 por canal (valor real, objetivo del suavizado) */
    private final float[] tinta = { 0.85f, 0.60f, 0.35f, 0.70f };
    /** valor mostrado y valor del tick anterior, para interpolar el llenado */
    private final float[] mostrado = tinta.clone();
    private final float[] anterior = tinta.clone();

    /** Cargas 0..16 por canal. El nivel de la barra sale de aca. */
    private final int[] cargas = new int[4];

    private Estado estado = Estado.REPOSO;
    private int progreso = 0;
    /** Cuantos ticks antes de terminar sale el poof de descarga. */
    private static final int TICKS_DESCARGA = 8;
    private ItemStack remera = ItemStack.EMPTY;
    private ItemStack foto = ItemStack.EMPTY;
    private ItemStack salida = ItemStack.EMPTY;
    private java.util.UUID fotoPendiente = null;
    /** En que cara se va a estampar. La elige el jugador al poner la foto. */
    private Estampa.Cara caraPendiente = Estampa.Cara.FRENTE;
    /** Full print o logo. Lo elige la palanca del frente de la maquina. */
    private Estampa.Modo modo = Estampa.Modo.COMPLETO;

    // Solo para dibujar, no se guardan: hacen que la foto siga a la vista
    // mientras la tapa baja, en vez de evaporarse antes de que la cubra.
    private boolean tapaAbiertaVista = true;
    private int cerrandose = 0;
    private java.util.UUID ultimaFotoVista = null;

    public SublimadoraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SUBLIMADORA_ENTITY, pos, state);
    }

    // ── animacion de la tapa ─────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "tapa", 0, state -> {
            boolean abierta = getCachedState().get(SublimadoraBlock.OPEN);
            if (actual == null) {
                // Al cargar el chunk: la POSE, no la transicion.
                ultimoEstado = abierta;
                actual = abierta ? ABIERTA : CERRADA;
            } else if (abierta != ultimoEstado) {
                // Cambio real: recien ahi la transicion, que ya termina
                // encadenando la pose por su thenLoop.
                ultimoEstado = abierta;
                actual = abierta ? ABRIR : CERRAR;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(actual);
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

        // Arrastre de la tapa. Va antes del corte por lado porque es puro
        // dibujo y lo necesita el cliente.
        boolean abierta = state.get(SublimadoraBlock.OPEN);
        if (abierta != be.tapaAbiertaVista) {
            be.tapaAbiertaVista = abierta;
            if (!abierta) be.cerrandose = TICKS_CIERRE;
        }
        if (be.cerrandose > 0) be.cerrandose--;
        java.util.UUID cargadaAhora = be.getFotoCargada();
        if (cargadaAhora != null) be.ultimaFotoVista = cargadaAhora;

        // El avance del prensado lo lleva el SERVIDOR; el cliente solo dibuja
        // lo que le sincroniza el NBT.
        if (world.isClient) return;

        if (be.estado != Estado.PRENSANDO) return;

        be.progreso++;

        // Sonido y vapor salen del servidor con spawnParticles y
        // playSound(null, ...): asi los ve y los oye cualquiera que este
        // cerca, no solo el que cerro la tapa.
        if (world instanceof ServerWorld servidor) {
            if (be.progreso == 1) {
                // El golpe de la plancha al apoyar.
                be.sonar(SoundEvents.BLOCK_PISTON_CONTRACT, 0.9f, 0.7f);
                vapor(servidor, pos, 14, 0.12);
            }
            if (be.progreso % 5 == 0) vapor(servidor, pos, 2, 0.12);

            // El poof de descarga sale ANTES de terminar, no al terminar. Asi
            // la secuencia queda: poof, y ocho ticks despues la luz verde y la
            // campanita juntas, que es como se lee que la maquina termino.
            if (be.progreso == TICKS_PRENSADO - TICKS_DESCARGA) {
                vapor(servidor, pos, 22, 0.16);
                be.sonar(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.3f);
            }

            // Siseo continuo. El fizz dura casi un segundo, asi que
            // repitiendolo cada 14 ticks los coletazos se solapan y suena como
            // una sola perdida de vapor sostenida en vez de golpes sueltos.
            if (be.progreso % 14 == 0) be.sonar(SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.3f, 1.6f);

            // Un pitido por cada encendido del LED rojo. El LED lo prende
            // SublimadoraGeoModel con (tiempo % 20) < 10 leyendo el reloj del
            // mundo, que es el mismo de los dos lados: mirando ese mismo reloj
            // aca, el pitido cae exactamente en el flanco de encendido.
            if (world.getTime() % 20 == 0) be.sonar(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.25f, 2.0f);
        }

        if (be.progreso >= TICKS_PRENSADO) {
            be.progreso = 0;
            be.estado = Estado.LISTO;
            be.salida = RemeraItem.estampar(be.remera, be.caraPendiente,
                    be.modo.aplicar(be.fotoPendiente));
            be.remera = ItemStack.EMPTY;
            be.fotoPendiente = null;
            // Esto es exactamente el tick en que se prende el LED verde, que
            // sale de estado == LISTO. La campanita va aca y no agendada
            // aparte: atada al mismo cambio de estado no se pueden desfasar.
            be.sonar(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
            be.sonar(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            be.sincronizar();
        } else if (be.progreso % 20 == 0) {
            // Una sincronizacion por segundo alcanza para el parpadeo y la
            // barra; no hace falta mandar un paquete por tick.
            be.sincronizar();
        }
    }

    /**
     * Vaporcito por la junta entre la base y la tapa.
     *
     * Usa WHITE_SMOKE y no CLOUD, por dos razones que solo se ven mirando el
     * codigo de las particulas:
     *
     *   - PlayerCloudParticle busca al jugador mas cercano dentro de 2 bloques
     *     y, si lo encuentra, arrastra la particula hacia la altura de sus
     *     PIES un 20% por tick. Como para usar la maquina hay que estar al
     *     lado, el vapor se venia para abajo por mas velocidad hacia arriba
     *     que se le pusiera. Eso era lo que quedaba raro.
     *   - WhiteSmokeParticle tiene gravedad -0.1, o sea flotabilidad: sube
     *     sola, despacio y para siempre. Justo lo que queriamos, y gratis.
     *
     * Entonces la velocidad que le damos es casi toda lateral: sale disparada
     * al costado, la friccion de 0.96 la frena en un bloque, y de ahi la
     * flotabilidad la levanta sola.
     *
     * Las particulas colisionan (hasPhysics), asi que nacen APENAS AFUERA de
     * la cara del bloque y no en un circulo: un circulo de radio 0.48 cae
     * entero adentro del cubo, y las que nacian trabadas salian para
     * cualquier lado al ser expulsadas.
     */
    private static void vapor(ServerWorld world, BlockPos pos, int cantidad, double lateral) {
        double cy = pos.getY() + 11.0 / 16.0;
        for (int i = 0; i < cantidad; i++) {
            int lado = world.getRandom().nextInt(4);
            double corrida = (world.getRandom().nextDouble() - 0.5) * 0.9;
            double afuera = 0.62;
            double dx, dz, vx, vz;
            switch (lado) {
                case 0  -> { dx = corrida; dz = -afuera; vx = 0;        vz = -lateral; }
                case 1  -> { dx = corrida; dz =  afuera; vx = 0;        vz =  lateral; }
                case 2  -> { dx = -afuera; dz = corrida; vx = -lateral; vz = 0; }
                default -> { dx =  afuera; dz = corrida; vx =  lateral; vz = 0; }
            }
            // count = 0 NO significa "ninguna particula": cambia que son los
            // tres deltas. Con count > 0 sale una por cada count, los deltas
            // son dispersion de posicion y la velocidad es azar gaussiano por
            // "speed" en las tres direcciones. Con count = 0 sale una sola y
            // los deltas son su velocidad, que es lo que queremos.
            world.spawnParticles(ParticleTypes.WHITE_SMOKE,
                    pos.getX() + 0.5 + dx,
                    cy + (world.getRandom().nextDouble() - 0.5) * 0.12,
                    pos.getZ() + 0.5 + dz,
                    0,
                    vx, 0.015, vz,
                    1.0);
        }
    }

    private void sonar(SoundEvent evento, float volumen, float tono) {
        if (world != null) world.playSound(null, pos, evento, SoundCategory.BLOCKS, volumen, tono);
    }

    // ── ciclo de prensado ────────────────────────────────────────────

    public Estado getEstado() {
        return estado;
    }

    /** 0..1, para animar. */
    public float getProgreso() {
        return estado == Estado.PRENSANDO ? progreso / (float) TICKS_PRENSADO : 0f;
    }

    public ItemStack getRemera() {
        return remera;
    }

    public ItemStack getSalida() {
        return salida;
    }

    /** True si hay al menos una carga de cada color. */
    public boolean hayTinta() {
        for (int i = 0; i < 4; i++) if (cargas[i] <= 0) return false;
        return true;
    }

    /** Carga un tinte. Devuelve false si ese tanque ya esta lleno. */
    public boolean cargarTinta(int canal, int cantidad) {
        if (cargas[canal] >= CARGA_MAXIMA) return false;
        cargas[canal] = Math.min(CARGA_MAXIMA, cargas[canal] + cantidad);
        tinta[canal] = cargas[canal] / (float) CARGA_MAXIMA;
        sincronizar();
        return true;
    }

    public boolean ponerRemera(ItemStack stack) {
        if (!remera.isEmpty() || estado != Estado.REPOSO) return false;
        remera = stack.copyWithCount(1);
        sincronizar();
        return true;
    }

    public boolean ponerFoto(ItemStack stack, java.util.UUID id, Estampa.Cara cara) {
        if (!foto.isEmpty() || estado != Estado.REPOSO) return false;
        // Sin UUID no hay nada que estampar: se rechaza en vez de gastar
        // tinta para producir una remera en blanco.
        if (id == null) return false;
        // Esa cara ya estampada: rechazar en vez de pisarla en silencio.
        if (!remera.isEmpty() && RemeraItem.estampaDe(remera, cara) != null) return false;
        foto = stack.copyWithCount(1);
        fotoPendiente = id;
        caraPendiente = cara;
        sincronizar();
        return true;
    }

    public Estampa.Cara getCaraPendiente() {
        return caraPendiente;
    }

    /** UUID de la foto cargada esperando ser prensada, o null. */
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getFotoCargada() {
        return foto.isEmpty() ? null : fotoPendiente;
    }

    /**
     * La foto que hay que DIBUJAR, que no es siempre la que esta cargada.
     *
     * Al cerrar la tapa el prensado consume la foto en el mismo tick, pero la
     * animacion de la tapa tarda seis en bajar: sin este arrastre el papel
     * desaparecia a la vista, con la plancha todavia abierta.
     */
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getFotoVisible() {
        if (getCachedState().get(SublimadoraBlock.OPEN)) return getFotoCargada();
        if (cerrandose <= 0) return null;
        java.util.UUID cargada = getFotoCargada();
        return cargada != null ? cargada : ultimaFotoVista;
    }

    public Estampa.Modo getModo() {
        return modo;
    }

    /** Da vuelta la palanca. No se puede en medio de un prensado. */
    public boolean cambiarModo() {
        if (estado == Estado.PRENSANDO) return false;
        modo = modo.siguiente();
        sincronizar();
        return true;
    }

    /** Saca lo que haya para retirar: la remera lista, o lo que este cargado. */
    public ItemStack retirar() {
        if (!salida.isEmpty()) {
            ItemStack out = salida;
            salida = ItemStack.EMPTY;
            estado = Estado.REPOSO;
            sincronizar();
            return out;
        }
        if (!foto.isEmpty()) {
            ItemStack out = foto;
            foto = ItemStack.EMPTY;
            fotoPendiente = null;
            sincronizar();
            return out;
        }
        if (!remera.isEmpty()) {
            ItemStack out = remera;
            remera = ItemStack.EMPTY;
            sincronizar();
            return out;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Se llama al CERRAR la tapa. Arranca el prensado si estan las tres
     * condiciones, y ahi mismo se consume la foto y una carga de cada tinta.
     */
    public boolean intentarPrensar() {
        if (estado != Estado.REPOSO) return false;
        if (remera.isEmpty() || foto.isEmpty() || !hayTinta()) return false;

        for (int i = 0; i < 4; i++) {
            cargas[i]--;
            tinta[i] = cargas[i] / (float) CARGA_MAXIMA;
        }
        foto = ItemStack.EMPTY;   // la foto se consume al cerrar la tapa
        estado = Estado.PRENSANDO;
        progreso = 0;
        sincronizar();
        return true;
    }

    /** Nombres de los cuatro tanques, para poder decir cual quedo vacio. */
    private static final String[] NOMBRE_TINTA = { "cian", "magenta", "amarillo", "negro" };

    /**
     * Por que no puede prensar, o null si podria arrancar.
     *
     * Devuelve Text y no String para que el texto viva en los archivos de
     * idioma: antes armaba la frase en castellano aca adentro y el jugador en
     * ingles la recibia igual en castellano.
     *
     * Son dos avisos distintos y salen de a uno, en orden. Que falte la remera
     * o la foto es no saber usar la maquina, y se contesta explicando el
     * procedimiento entero; recien cuando eso esta resuelto tiene sentido
     * hablar de insumos. Mostrar los dos juntos hacia un parrafo que se lee
     * como una lista de reproches en vez de como el proximo paso.
     *
     * El de tinta nombra los colores en cero y no los cuatro tanques con sus
     * numeros: lo unico accionable es cual hay que ir a buscar.
     */
    @org.jetbrains.annotations.Nullable
    public Text queFalta() {
        if (estado == Estado.PRENSANDO) return Text.translatable("sublimadora.aviso.prensando");
        if (estado == Estado.LISTO) return Text.translatable("sublimadora.aviso.retirar");

        if (remera.isEmpty() || foto.isEmpty()) {
            return Text.translatable("sublimadora.aviso.cargar");
        }
        if (!hayTinta()) {
            MutableText colores = Text.empty();
            boolean primero = true;
            for (int i = 0; i < 4; i++) {
                if (cargas[i] > 0) continue;
                if (!primero) colores.append(", ");
                colores.append(Text.translatable("sublimadora.tinta." + NOMBRE_TINTA[i]));
                primero = false;
            }
            return Text.translatable("sublimadora.aviso.tinta", colores);
        }
        return null;
    }

    private void sincronizar() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
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
        for (int i = 0; i < 4; i++) {
            nbt.putFloat(CLAVES[i], tinta[i]);
            nbt.putInt(CLAVES[i] + "Cargas", cargas[i]);
        }
        nbt.putString("Estado", estado.name());
        nbt.putInt("Progreso", progreso);
        if (!remera.isEmpty()) nbt.put("Remera", remera.encode(registries));
        if (!foto.isEmpty()) nbt.put("Foto", foto.encode(registries));
        if (!salida.isEmpty()) nbt.put("Salida", salida.encode(registries));
        if (fotoPendiente != null) nbt.putUuid("FotoPendiente", fotoPendiente);
        nbt.putString("CaraPendiente", caraPendiente.name());
        nbt.putString("Modo", modo.name());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        for (int i = 0; i < 4; i++) {
            if (nbt.contains(CLAVES[i] + "Cargas")) {
                cargas[i] = nbt.getInt(CLAVES[i] + "Cargas");
                tinta[i] = cargas[i] / (float) CARGA_MAXIMA;
            } else if (nbt.contains(CLAVES[i])) {
                // Mundos de antes de que la tinta se contara por cargas.
                tinta[i] = nbt.getFloat(CLAVES[i]);
                cargas[i] = Math.round(tinta[i] * CARGA_MAXIMA);
            }
            mostrado[i] = tinta[i];
            anterior[i] = tinta[i];
        }
        estado = nbt.contains("Estado") ? Estado.valueOf(nbt.getString("Estado")) : Estado.REPOSO;
        progreso = nbt.getInt("Progreso");
        remera = nbt.contains("Remera") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Remera")) : ItemStack.EMPTY;
        foto = nbt.contains("Foto") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Foto")) : ItemStack.EMPTY;
        salida = nbt.contains("Salida") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Salida")) : ItemStack.EMPTY;
        fotoPendiente = nbt.containsUuid("FotoPendiente") ? nbt.getUuid("FotoPendiente") : null;
        caraPendiente = nbt.contains("CaraPendiente")
                ? Estampa.Cara.valueOf(nbt.getString("CaraPendiente"))
                : Estampa.Cara.FRENTE;
        modo = nbt.contains("Modo")
                ? Estampa.Modo.valueOf(nbt.getString("Modo"))
                : Estampa.Modo.COMPLETO;
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
