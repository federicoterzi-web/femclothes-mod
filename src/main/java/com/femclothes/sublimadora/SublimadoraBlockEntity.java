package com.femclothes.sublimadora;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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
import software.bernie.geckolib.animation.PlayState;
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
public class SublimadoraBlockEntity extends BlockEntity
        implements GeoBlockEntity, ExtendedScreenHandlerFactory<BlockPos>, SidedInventory {

    public static final int C = 0, M = 1, Y = 2, K = 3;

    // 64 y no 16 (2026-09-18, "subime la capacidad de tinta") — un stack
    // entero de tinte llena el tanque justo.
    /** Cuantas cargas entran por color. Una carga = un tinte. */
    public static final int CARGA_MAXIMA = 64;
    /** 15 segundos a 20 ticks — a pedido (2026-09-21, "que cada maquina tome su tiempo... 15 la sublimadora"). */
    public static final int TICKS_PRENSADO = 300;
    /** Lo que tarda la tapa en bajar: 0.3 s del archivo de animacion. */
    private static final int TICKS_CIERRE = 6;

    public enum Estado { REPOSO, PRENSANDO, LISTO }

    private static final String[] CLAVES = { "TintaC", "TintaM", "TintaY", "TintaK" };

    // Nombres "sublimator" y no "sublimadora" — modelo nuevo (2026-09-19,
    // ver SublimadoraGeoModel), mismo hueso "lid" que el viejo llamaba tapa.
    private static final RawAnimation ABRIR = RawAnimation.begin()
            .thenPlay("animation.sublimator.abrir")
            .thenLoop("animation.sublimator.abierta");
    private static final RawAnimation CERRAR = RawAnimation.begin()
            .thenPlay("animation.sublimator.cerrar")
            .thenLoop("animation.sublimator.cerrada");

    // Poses estaticas, sin transicion: son las que hay que usar al cargar el
    // chunk. ABRIR y CERRAR arrancan desde la pose CONTRARIA, asi que usarlas
    // al cargar hacia que un bloque cerrado apareciera abierto y se cerrara
    // solo delante del jugador.
    private static final RawAnimation ABIERTA = RawAnimation.begin()
            .thenLoop("animation.sublimator.abierta");
    private static final RawAnimation CERRADA = RawAnimation.begin()
            .thenLoop("animation.sublimator.cerrada");

    /** Ventilador de atras — gira SOLO mientras prensa (hueso "fan", 2026-09-19). */
    private static final RawAnimation EN_MARCHA = RawAnimation.begin()
            .thenLoop("animation.sublimator.en_marcha");

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
    /**
     * Tanque de papel — a pedido (2026-09-19, "usa papel y ya no consume la
     * imagen"): la foto queda cargada para reimprimir, y esto es lo que de
     * verdad se gasta por cada prensado (1 papel). Mismo tope que la tinta.
     */
    private int papelCargado = 0;

    /**
     * "Vista" de {@code papelCargado}/{@code cargas[]} como ItemStack real,
     * para el Inventory de hopper (2026-09-20) — bug real jugando: "el item
     * se vacia del hopper pero el indicador nunca sube". Causa: un hopper
     * vanilla, cuando el slot destino YA tiene algo, no vuelve a llamar
     * {@code setStack} — agarra el ItemStack que le devuelve {@code getStack},
     * lo incrementa EN MEMORIA, y listo llama {@code markDirty()} (ver
     * {@code HopperBlockEntity#transfer}). Devolver un ItemStack nuevo y
     * descartable en cada {@code getStack} (como hacía antes) hace que ese
     * incremento se pierda en el aire — nunca vuelve a nuestro lado. Estos
     * dos campos son la MISMA instancia entre llamadas (identidad estable),
     * así que la mutación del hopper cae adentro de un objeto real: se
     * "empuja" (autoridad→vista) cada vez que ESTE código cambia
     * papelCargado/cargas[], y se "tira" de vuelta (vista→autoridad) en
     * {@link #markDirty()} — así se captura tanto nuestros cambios propios
     * como los que un hopper haga directo sobre el objeto.
     */
    private ItemStack papelSlotView = ItemStack.EMPTY;
    private final ItemStack[] tintaSlotView = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

    /** Autoridad → vista: llamar después de cualquier cambio directo a papelCargado/cargas[]. */
    private void sincronizarVistaTanques() {
        papelSlotView = papelCargado <= 0 ? ItemStack.EMPTY : new ItemStack(net.minecraft.item.Items.PAPER, papelCargado);
        for (int i = 0; i < 4; i++) {
            tintaSlotView[i] = cargas[i] <= 0 ? ItemStack.EMPTY : new ItemStack(SublimadoraBlock.TINTES[i], cargas[i]);
        }
    }

    private Estado estado = Estado.REPOSO;
    private int progreso = 0;
    /** Cuantos ticks antes de terminar sale el poof de descarga. */
    private static final int TICKS_DESCARGA = 8;
    private ItemStack remera = ItemStack.EMPTY;
    private ItemStack salida = ItemStack.EMPTY;

    // Una foto y un ajuste de escala/posicion POR CARA: la maquina estampa
    // el frente y la espalda en la misma pasada. Todo esto va indexado por
    // Cara.ordinal().
    private final ItemStack[] fotos = { ItemStack.EMPTY, ItemStack.EMPTY };
    private final java.util.UUID[] pendientes = new java.util.UUID[2];
    /**
     * Escala/posicion libres, POR CARA — a pedido (2026-09-19, "hace gui
     * con preview con escalado y flechitas para posicion... sacamos los
     * controles del frente"): reemplaza los 3 presets viejos (Logo/
     * Centrada/Completo) por control fino de verdad, como ya preveia el
     * comentario de {@link Estampa.Modo} ("agregar control fino despues
     * no rompe nada de esto").
     */
    private final float[] escalaBorrador = { Estampa.ESCALA_DEFECTO, Estampa.ESCALA_DEFECTO };
    private final float[] xBorrador = { 0f, 0f };
    private final float[] yBorrador = { 0f, 0f };
    /** Rotación libre, POR CARA — a pedido (2026-09-20, "posibilidad de rotarla"). */
    private final float[] anguloBorrador = { 0f, 0f };
    private static final float PASO_ESCALA = 0.05f;
    private static final float PASO_POSICION = 0.05f;
    private static final float PASO_ANGULO = 15f;
    /** Que cara esta configurando la pantalla ahora mismo. */
    private Estampa.Cara seleccion = Estampa.Cara.FRENTE;
    /**
     * Chincheta por cara (2026-09-28, "cambiemos la gui de la sublimadora
     * para hacerla sintonizar con sus bloques hermanos"): mismo criterio
     * que los cuadraditos de Tintes — solo se estampan las caras FIJADAS
     * que tengan foto; la vista previa muestra las fijadas más la que se
     * está editando.
     */
    private final boolean[] caraFijada = { false, false };
    /**
     * Simetría lateral (2026-09-28, "a la sublimadora hay que agregarle
     * simetria lateral para medias y cubrebrazos"): el lado izquierdo lleva
     * el espejo del derecho. Solo cuenta en prendas de a pares, ver
     * {@link #admiteSimetria}.
     */
    private boolean simetria = false;

    /**
     * "Save por cada prenda" (a pedido, 2026-09-19): una combinación
     * escala/x/y de las DOS caras, guardada — la lista es POR ÍTEM (todos
     * los cortes de remera comparten lista, igual que categoriza
     * {@code TinturasBlockEntity.Categoria.REMERA}; medias/pantalón/
     * calientabrazos tienen la suya propia, un solo ítem cada una).
     */
    public record EstampaFijada(float escalaFrente, float xFrente, float yFrente, float anguloFrente,
                                 float escalaEspalda, float xEspalda, float yEspalda, float anguloEspalda) {}

    /**
     * Un diseño guardado con nombre (2026-09-28, mismo sistema que
     * Modeladora/Tintes: "Guardar diseño" + 8 casilleros, click carga,
     * click derecho borra) — reemplaza a las "fijadas" sin nombre, que se
     * migran como diseños "#n" al leer un mundo viejo.
     */
    public record DisenoEstampa(String nombre, EstampaFijada ajuste, boolean fijadaFrente, boolean fijadaEspalda,
                                boolean simetria) {}

    /** Medias y calientabrazos: las prendas de a pares que tienen simetría lateral. */
    public static boolean admiteSimetria(net.minecraft.item.Item item) {
        return item == com.femclothes.item.FemclothesItems.SOCKS_SOLID
                || item == com.femclothes.item.FemclothesItems.CALIENTABRAZOS;
    }

    public boolean simetria() { return simetria; }

    public static final int DISENOS_MAXIMO = 8;
    private final java.util.Map<net.minecraft.item.Item, java.util.List<DisenoEstampa>> disenosPorItem = new java.util.HashMap<>();

    /**
     * Qué "categoría" (en realidad el ÍTEM representativo — acá cada
     * categoría es un solo ítem, ver {@link ModItems#esEstampable})
     * muestra/edita la fila de fijadas ahora mismo — a pedido (2026-09-21,
     * "le falta el boton de ciclado de settings de las distintas
     * prendas en la gui que guarde el seteo"): antes {@link #fijadas()}
     * necesitaba una remera FÍSICA puesta para mostrar algo (devolvía
     * vacío si no), así que no había forma de ver/guardar el seteo de
     * "pantalón" sin tener un pantalón en la mano en ese momento. Ahora
     * sigue lo cargado automáticamente ({@link #ponerRemera}) pero
     * también se puede cambiar a mano con {@link #BTN_CATEGORIA}.
     */
    private static final net.minecraft.item.Item[] CATEGORIAS = {
            ModItems.REMERA,
            com.femclothes.item.FemclothesItems.SOCKS_SOLID,
            com.femclothes.item.FemclothesItems.PANTALON,
            com.femclothes.item.FemclothesItems.CALIENTABRAZOS,
    };
    private net.minecraft.item.Item categoria = ModItems.REMERA;

    // Solo para dibujar, no se guardan: hacen que la foto siga a la vista
    // mientras la tapa baja, en vez de evaporarse antes de que la cubra.
    private int arrastre = 0;
    private final java.util.UUID[] ultimasVistas = new java.util.UUID[2];

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

        // Ventilador de atras: gira SOLO mientras prensa, quieto el resto
        // del tiempo (sin animacion pedida, GeckoLib deja el hueso en su
        // pose de reposo del modelo) — a pedido (2026-09-19, "fan es el
        // ventilador de atras").
        controllers.add(new AnimationController<>(this, "ventilador", 0, state ->
                estado == Estado.PRENSANDO ? state.setAndContinue(EN_MARCHA) : PlayState.STOP));
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

        // Arrastre de la foto. Va antes del corte por lado porque es puro
        // dibujo y lo necesita el cliente.
        //
        // El contador se RECARGA mientras haya foto, en vez de arrancar
        // cuando se cierra la tapa. Al cerrar viajan dos cosas por separado
        // -el cambio de estado del bloque y el NBT del block entity- y no
        // llegan juntas: si el NBT sin foto llegaba primero, la foto
        // desaparecia, y un tick despues el cambio de tapa arrancaba el
        // arrastre y la hacia volver. Ese ida y vuelta era el parpadeo.
        // Recargando, el arrastre ya esta lleno pase lo que pase.
        boolean hayAlguna = false;
        for (Estampa.Cara cara : Estampa.Cara.values()) {
            java.util.UUID id = be.getFotoCargada(cara);
            if (id != null) {
                be.ultimasVistas[cara.ordinal()] = id;
                hayAlguna = true;
            }
        }
        if (hayAlguna) be.arrastre = TICKS_CIERRE;
        else if (be.arrastre > 0) be.arrastre--;

        // El avance del prensado lo lleva el SERVIDOR; el cliente solo dibuja
        // lo que le sincroniza el NBT.
        if (world.isClient) return;

        if (be.estado == Estado.LISTO) {
            be.empujarSalida(world, pos);
        }

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

        if (be.progreso >= com.femclothes.util.DebugMaquinas.duracion(TICKS_PRENSADO)) {
            be.progreso = 0;
            be.estado = Estado.LISTO;
            ItemStack hecha = be.remera;
            // pendientes[] NO se borra al terminar (2026-09-19, "ya no
            // consume la imagen") — la foto queda lista para reimprimir en
            // la proxima remera sin volver a cargarla.
            for (Estampa.Cara cara : Estampa.Cara.values()) {
                java.util.UUID id = be.pendientes[cara.ordinal()];
                if (id == null || !be.caraActiva(cara.ordinal())) continue;
                int ci = cara.ordinal();
                // "Cubrir" (full print, recorta en vez de encoger) se
                // deriva de la escala en vez de ser un toggle aparte: a
                // ESCALA_CUBRIR (100%) es exactamente lo que hacia el viejo
                // preset COMPLETO — por ENCIMA de eso ya es sobre-escala
                // (ver Estampa.ESCALA_MAXIMA), sigue siendo "cubrir".
                boolean cubrir = be.escalaBorrador[ci] >= Estampa.ESCALA_CUBRIR - 0.001f;
                hecha = RemeraItem.estampar(hecha, cara,
                        new Estampa(id, be.escalaBorrador[ci], be.xBorrador[ci], be.yBorrador[ci],
                                be.anguloBorrador[ci], cubrir, be.simetria && admiteSimetria(hecha.getItem())));
            }
            be.salida = hecha;
            be.remera = ItemStack.EMPTY;
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

    /**
     * La remera de {@link #remera} con la estampa del borrador YA
     * aplicada (escala/posición/ángulo de las dos caras, en vivo) — a
     * pedido (2026-09-21, "que muestre el preview del setting de la
     * ultima prenda seteada"). Antes esto vivía SOLO adentro de
     * {@code SublimadoraScreen#dibujarPreview}; ahora también lo usa
     * {@link com.femclothes.sublimadora.SublimadoraGeoModel} para la
     * pantallita del bloque, así que se comparte acá — mismo criterio
     * de nombre que {@code TinturasBlockEntity#prendaDeVistaPrevia}.
     */
    public ItemStack prendaDeVistaPrevia() { return prendaDeVistaPrevia(false); }

    /**
     * {@code representativa}=true (la GUI, 2026-09-28): sin prenda cargada
     * ni lista, muestra la de la categoría elegida con el borrador encima —
     * mismo criterio que {@code TinturasBlockEntity#prendaDeVistaPrevia}.
     * La pantallita del bloque sigue pidiendo solo prendas de verdad.
     */
    public ItemStack prendaDeVistaPrevia(boolean representativa) {
        ItemStack base = !remera.isEmpty() ? remera
                : representativa && salida.isEmpty() ? new ItemStack(categoria) : ItemStack.EMPTY;
        if (!base.isEmpty()) {
            ItemStack copia = base.copy();
            for (Estampa.Cara cara : Estampa.Cara.values()) {
                java.util.UUID id = getFotoCargada(cara);
                if (id == null) continue;
                // Solo lo fijado más la cara que se está editando — lo que va a salir.
                if (!caraFijada[cara.ordinal()] && cara != seleccion) continue;
                boolean cubrir = getEscala(cara) >= Estampa.ESCALA_CUBRIR - 0.001f;
                copia = RemeraItem.estampar(copia, cara,
                        new Estampa(id, getEscala(cara), getX(cara), getY(cara), getAngulo(cara), cubrir,
                                simetria && admiteSimetria(copia.getItem())));
            }
            return copia;
        }
        return salida;
    }

    /**
     * La ÚLTIMA vista previa no vacía — mismo criterio que
     * {@code ModeladoBlockEntity#vistaPreviaPersistente} (2026-09-21,
     * "quiero que las 3 muestren la ultima prenda con preview del
     * ultimo seteado"): la pantallita del bloque usa esto en vez de
     * {@link #prendaDeVistaPrevia} directo, para seguir mostrando la
     * última estampa aunque ya se haya retirado la remera.
     */
    private ItemStack ultimaVistaPrevia = ItemStack.EMPTY;

    public ItemStack vistaPreviaPersistente() {
        ItemStack actual = prendaDeVistaPrevia();
        if (!actual.isEmpty()) ultimaVistaPrevia = actual;
        return ultimaVistaPrevia;
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
        sincronizarVistaTanques();
        sincronizar();
        return true;
    }

    /** Carga papel. Devuelve false si el tanque ya esta lleno. */
    public boolean cargarPapel(int cantidad) {
        if (papelCargado >= CARGA_MAXIMA) return false;
        papelCargado = Math.min(CARGA_MAXIMA, papelCargado + cantidad);
        sincronizarVistaTanques();
        sincronizar();
        return true;
    }

    public int getPapel() {
        return papelCargado;
    }

    /** La prenda a estampar: la remera de cualquier corte, o las medias. */
    public boolean ponerRemera(ItemStack stack) {
        if (!remera.isEmpty() || estado != Estado.REPOSO) return false;
        remera = stack.copyWithCount(1);
        // La categoría de fijadas sigue automáticamente lo que se carga
        // — a pedido, ver el javadoc de {@link #categoria}.
        categoria = remera.getItem();
        sincronizar();
        return true;
    }

    /**
     * Carga una foto en una cara concreta — a pedido (2026-09-19, "las
     * imagenes deberian agregarse aqui no en la estampadora"): antes iba
     * a la cara que tuviera elegida el selector físico del bloque; ahora
     * cada cara tiene su propio slot en la pantalla (ver
     * {@link #isValid}/{@link #setStack}), así que la cara la dice el
     * slot, no un selector aparte.
     */
    public boolean ponerFoto(Estampa.Cara cara, ItemStack stack, java.util.UUID id) {
        int i = cara.ordinal();
        if (!fotos[i].isEmpty() || estado != Estado.REPOSO) return false;
        // Sin UUID no hay nada que estampar: se rechaza en vez de gastar
        // tinta para producir una remera en blanco.
        if (id == null) return false;
        // Esa cara ya estampada: rechazar en vez de pisarla en silencio.
        if (!remera.isEmpty() && RemeraItem.estampaDe(remera, cara) != null) return false;
        fotos[i] = stack.copyWithCount(1);
        pendientes[i] = id;
        sincronizar();
        return true;
    }

    // ── Inventory: los 2 slots de foto de la pantalla nueva (índice =
    // Estampa.Cara.ordinal(), 0=FRENTE, 1=ESPALDA) — a pedido (2026-09-19,
    // "las imagenes deberian agregarse aqui no en la estampadora"). Reusa
    // el mismo array fotos[] que ya guardaba esto (antes invisible,
    // cargado por click derecho sobre el bloque) como backing real.
    //
    // 2026-09-20, a pedido ("que carguen por hopper atrás [papel y
    // tintas] y que las 3 carguen prenda por hopper del lado izquierdo"):
    // se suman 6 slots más — remera, papel y las 4 tintas CMYK — cada uno
    // como una "ventana" sobre un campo que YA existía (remera/
    // papelCargado/cargas[]), no datos nuevos. Los tanques (papel/tinta)
    // se exponen como si fueran un slot con un stack del ítem real y
    // count=cantidad cargada — mismo truco que un tanque de combustible
    // expuesto a hoppers: el hopper hace su merge normal vía
    // getStack/setStack, sin que este código tenga que reimplementar esa
    // lógica. Ver getAvailableSlots/canInsert más abajo para qué lado
    // puede tocar qué slot — las fotos (0-1) siguen sin ser alcanzables
    // por hopper, a propósito (se cargan desde la pantalla, no de afuera).
    public static final int SLOT_REMERA = 2;
    public static final int SLOT_PAPEL = 3;
    public static final int SLOT_TINTA_BASE = 4; // .. +4 (C,M,Y,K, mismo orden que SublimadoraBlock.TINTES)
    /**
     * Salida visible — a pedido (2026-09-21, "que las tres tengan slot de
     * entrada y de salida"): antes {@link #salida} vivía solo como campo
     * (se sacaba con {@link #retirar()}, click derecho con la mano
     * vacía), ahora también es un slot real de verdad en pantalla —
     * mismo criterio que {@code SLOT_SALIDA} de Modeladora/Tinturas.
     * {@code retirar()} sigue andando igual, las dos formas conviven.
     */
    public static final int SLOT_SALIDA = SLOT_TINTA_BASE + 4;
    /**
     * Almacén de fotos — a pedido (2026-09-21, "slots" tras "tambien
     * podemos agregarle slots a la derecha para guardar imagenes"): antes
     * Frente/Espalda eran los ÚNICOS lugares donde vivía una foto, así
     * que cambiar de imagen significaba perder la anterior sin volver a
     * cargarla de afuera. Estos 9 son guardado nomás, ninguno alimenta un
     * prensado directo — se arrastran a Frente/Espalda como cualquier
     * slot vanilla. Mismo tamaño que el almacén de Tinturas/Modeladora.
     */
    public static final int ALMACEN_TAMANO = 9;
    public static final int SLOT_ALMACEN_INICIO = SLOT_SALIDA + 1;
    private final net.minecraft.util.collection.DefaultedList<ItemStack> almacen =
            net.minecraft.util.collection.DefaultedList.ofSize(ALMACEN_TAMANO, ItemStack.EMPTY);

    @Override public int size() { return SLOT_ALMACEN_INICIO + ALMACEN_TAMANO; }

    @Override
    public boolean isEmpty() {
        if (!fotos[0].isEmpty() || !fotos[1].isEmpty() || !remera.isEmpty() || !salida.isEmpty()) return false;
        if (papelCargado > 0) return false;
        for (int c : cargas) if (c > 0) return false;
        for (ItemStack s : almacen) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        // Papel/tinta: se devuelve la MISMA instancia de vista entre
        // llamadas (nunca una nueva) — ver el javadoc de papelSlotView/
        // tintaSlotView, es lo que hace que un hopper mezclando de a uno
        // no pierda el incremento.
        if (slot >= SLOT_ALMACEN_INICIO) return almacen.get(slot - SLOT_ALMACEN_INICIO);
        return switch (slot) {
            case 0, 1 -> fotos[slot];
            case SLOT_REMERA -> remera;
            case SLOT_PAPEL -> papelSlotView;
            case SLOT_SALIDA -> salida;
            default -> tintaSlotView[slot - SLOT_TINTA_BASE];
        };
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot >= SLOT_ALMACEN_INICIO) {
            ItemStack resultado = net.minecraft.inventory.Inventories.splitStack(almacen, slot - SLOT_ALMACEN_INICIO, amount);
            if (!resultado.isEmpty()) sincronizar();
            return resultado;
        }
        if (slot <= 1) {
            ItemStack resultado = fotos[slot].split(amount);
            if (!resultado.isEmpty()) {
                if (fotos[slot].isEmpty()) pendientes[slot] = null;
                sincronizar();
            }
            return resultado;
        }
        if (slot == SLOT_REMERA) {
            ItemStack resultado = remera.split(amount);
            if (!resultado.isEmpty()) sincronizar();
            return resultado;
        }
        if (slot == SLOT_SALIDA) {
            ItemStack resultado = salida.split(amount);
            if (!resultado.isEmpty()) {
                if (salida.isEmpty()) estado = Estado.REPOSO;
                sincronizar();
            }
            return resultado;
        }
        // Papel/tinta: no hay un ItemStack de verdad guardado, se
        // reconstruye a partir del contador — remover es simplemente
        // bajar el contador y devolver un stack equivalente.
        int actual = slot == SLOT_PAPEL ? papelCargado : cargas[slot - SLOT_TINTA_BASE];
        int sacado = Math.min(actual, amount);
        if (sacado <= 0) return ItemStack.EMPTY;
        setCarga(slot, actual - sacado);
        ItemStack resultado = getStackDeCarga(slot, sacado);
        sincronizar();
        return resultado;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot >= SLOT_ALMACEN_INICIO) {
            ItemStack resultado = net.minecraft.inventory.Inventories.removeStack(almacen, slot - SLOT_ALMACEN_INICIO);
            if (!resultado.isEmpty()) sincronizar();
            return resultado;
        }
        if (slot <= 1) {
            ItemStack resultado = fotos[slot];
            fotos[slot] = ItemStack.EMPTY;
            pendientes[slot] = null;
            sincronizar();
            return resultado;
        }
        if (slot == SLOT_REMERA) {
            ItemStack resultado = remera;
            remera = ItemStack.EMPTY;
            sincronizar();
            return resultado;
        }
        if (slot == SLOT_SALIDA) {
            ItemStack resultado = salida;
            salida = ItemStack.EMPTY;
            estado = Estado.REPOSO;
            sincronizar();
            return resultado;
        }
        int actual = slot == SLOT_PAPEL ? papelCargado : cargas[slot - SLOT_TINTA_BASE];
        ItemStack resultado = getStackDeCarga(slot, actual);
        setCarga(slot, 0);
        sincronizar();
        return resultado;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= SLOT_ALMACEN_INICIO) {
            almacen.set(slot - SLOT_ALMACEN_INICIO, stack);
            if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
            sincronizar();
            return;
        }
        switch (slot) {
            case 0, 1 -> {
                if (stack.isEmpty()) {
                    fotos[slot] = ItemStack.EMPTY;
                    pendientes[slot] = null;
                } else {
                    ponerFoto(Estampa.Cara.values()[slot], stack, SublimadoraBlock.uuidDeFoto(stack));
                }
            }
            case SLOT_REMERA -> remera = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            case SLOT_SALIDA -> {
                salida = stack;
                if (salida.isEmpty() && estado == Estado.LISTO) estado = Estado.REPOSO;
            }
            default -> setCarga(slot, stack.isEmpty() ? 0
                    : MathHelper.clamp(stack.getCount(), 0, CARGA_MAXIMA));
        }
        sincronizar();
    }

    /** El tanque (papel o el canal de tinta que corresponda) a {@code valor}, sin pasar por cargarTinta/cargarPapel (que solo suman). */
    private void setCarga(int slot, int valor) {
        if (slot == SLOT_PAPEL) {
            papelCargado = valor;
        } else {
            int canal = slot - SLOT_TINTA_BASE;
            cargas[canal] = valor;
            tinta[canal] = cargas[canal] / (float) CARGA_MAXIMA;
        }
        sincronizarVistaTanques();
    }

    private ItemStack getStackDeCarga(int slot, int cantidad) {
        if (cantidad <= 0) return ItemStack.EMPTY;
        return slot == SLOT_PAPEL
                ? new ItemStack(net.minecraft.item.Items.PAPER, cantidad)
                : new ItemStack(SublimadoraBlock.TINTES[slot - SLOT_TINTA_BASE], cantidad);
    }

    /**
     * OJO: NO llamar {@code sincronizar()} acá — esa función llama
     * {@code markDirty()}, y como este método la pisa (override de
     * {@code Inventory}), las dos se llamaban entre sí para siempre
     * (StackOverflowError apenas se tocaba el bloque, 2026-09-20).
     * {@code super.markDirty()} es la de {@code BlockEntity} de toda la
     * vida, no la reimplementada acá.
     */
    @Override
    public void markDirty() {
        // Vista → autoridad, ANTES de todo lo demás (2026-09-20): si un
        // hopper mutó papelSlotView/tintaSlotView[] directo en memoria
        // (ver su javadoc), acá es donde se entera el resto del código.
        // Si nada externo lo tocó, esto no cambia nada (sincronizarVistaTanques
        // ya los había dejado iguales) — comparar antes de reasignar evita
        // relecturas de tinta[]/sincronizar() de más en el camino normal.
        int papelDeVista = papelSlotView.isEmpty() ? 0 : papelSlotView.getCount();
        if (papelDeVista != papelCargado) papelCargado = MathHelper.clamp(papelDeVista, 0, CARGA_MAXIMA);
        for (int i = 0; i < 4; i++) {
            int deVista = tintaSlotView[i].isEmpty() ? 0 : tintaSlotView[i].getCount();
            if (deVista != cargas[i]) {
                cargas[i] = MathHelper.clamp(deVista, 0, CARGA_MAXIMA);
                tinta[i] = cargas[i] / (float) CARGA_MAXIMA;
            }
        }
        super.markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < 2; i++) { fotos[i] = ItemStack.EMPTY; pendientes[i] = null; }
        remera = ItemStack.EMPTY;
        salida = ItemStack.EMPTY;
        papelCargado = 0;
        java.util.Arrays.fill(cargas, 0);
        java.util.Arrays.fill(tinta, 0f);
        almacen.clear();
        sincronizarVistaTanques();
        sincronizar();
    }

    /**
     * Solo fotos de Camerapture, y solo mientras se pueda cargar de
     * verdad — mismos guardas que {@link #ponerFoto} (reposo, cara sin
     * estampar todavía), para que un drag-and-drop rechazado rebote en
     * vez de aceptarse en silencio y fallar después.
     *
     * <p>Los slots 2-7 (remera/papel/tinta, 2026-09-20) mismos guardas
     * que ya usaban {@link #ponerRemera}/{@link #cargarPapel}/
     * {@link #cargarTinta} desde el click derecho — un hopper entra por
     * la misma puerta que un jugador, no una más permisiva.
     */
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // Almacén de fotos: guardado nomás, cualquier foto entra (2026-09-21,
        // "slots"), sin las restricciones de reposo/cara-sin-estampar de
        // Frente/Espalda (slot <= 1) porque no alimenta un prensado directo.
        if (slot >= SLOT_ALMACEN_INICIO) return SublimadoraBlock.esFoto(stack);
        if (slot <= 1) {
            if (estado != Estado.REPOSO) return false;
            if (!fotos[slot].isEmpty()) return false;
            Estampa.Cara cara = Estampa.Cara.values()[slot];
            if (!remera.isEmpty() && RemeraItem.estampaDe(remera, cara) != null) return false;
            return SublimadoraBlock.esFoto(stack) && SublimadoraBlock.uuidDeFoto(stack) != null;
        }
        if (slot == SLOT_REMERA) {
            return remera.isEmpty() && estado == Estado.REPOSO
                    && ModItems.esEstampable(stack) && !SublimadoraBlock.tieneLasDosCaras(stack);
        }
        if (slot == SLOT_PAPEL) {
            return stack.isOf(net.minecraft.item.Items.PAPER) && papelCargado < CARGA_MAXIMA;
        }
        // Salida: solo la máquina escribe acá, igual que en Modeladora/Tinturas.
        if (slot == SLOT_SALIDA) return false;
        int canal = slot - SLOT_TINTA_BASE;
        return stack.isOf(SublimadoraBlock.TINTES[canal]) && cargas[canal] < CARGA_MAXIMA;
    }

    // ── SidedInventory: remera por ARRIBA (2026-09-21, "que las tres
    // carguen por arriba" — reemplaza el costado izquierdo de
    // 2026-09-20), papel/tinta siguen por ATRÁS, relativo a FACING como
    // si un jugador estuviera parado frente al panel mirándolo. Las fotos
    // (0-1) no aparecen en ningún lado: siguen siendo solo-GUI a propósito.
    private Direction ladoIzquierdo() {
        return getCachedState().get(SublimadoraBlock.FACING).getOpposite().rotateYCounterclockwise();
    }

    private Direction ladoAtras() {
        return getCachedState().get(SublimadoraBlock.FACING).getOpposite();
    }

    /** El lado opuesto al de carga — hacia ahí se empuja el resultado (ver {@link #empujarSalida}). */
    private Direction ladoDerecho() {
        return ladoIzquierdo().getOpposite();
    }

    /**
     * Empuja la remera lista hacia la derecha — a pedido (2026-09-20,
     * "las 3 maquinas hacen como el crafter y depositan el resultado en
     * el bloque siguiente si es una de las 3 maquinas o si es
     * contenedor"): mismo mecanismo que un Crafter vanilla, ver
     * {@link com.femclothes.util.InventarioUtil#empujarA}. Si no hay
     * nada del otro lado (o está lleno) la remera simplemente se queda
     * en {@code salida}, recuperable como siempre.
     */
    private void empujarSalida(net.minecraft.world.World world, BlockPos pos) {
        if (salida.isEmpty()) return;
        Direction derecha = ladoDerecho();
        ItemStack sobrante = com.femclothes.util.InventarioUtil.empujarA(
                world, pos.offset(derecha), derecha.getOpposite(), salida);
        if (sobrante.getCount() != salida.getCount()) {
            salida = sobrante;
            if (salida.isEmpty()) estado = Estado.REPOSO;
            sincronizar();
        }
    }

    // La remera entra por ARRIBA (2026-09-21, "que las tres carguen por
    // arriba") — reemplaza el costado izquierdo que usaba hasta ahora;
    // papel/tinta siguen por atrás, sin cambios.
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return new int[]{SLOT_REMERA};
        if (side == ladoAtras()) return new int[]{SLOT_PAPEL, SLOT_TINTA_BASE, SLOT_TINTA_BASE + 1, SLOT_TINTA_BASE + 2, SLOT_TINTA_BASE + 3};
        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        // Solo inserción por ahora (2026-09-20) — extracción automática
        // (ej. sacar la remera terminada) queda pendiente a propósito.
        return false;
    }

    public Estampa.Cara getSeleccion() {
        return seleccion;
    }

    /** Da vuelta el selector de cara. No se puede en medio de un prensado. */
    public boolean cambiarSeleccion() {
        if (estado == Estado.PRENSANDO) return false;
        seleccion = seleccion == Estampa.Cara.FRENTE ? Estampa.Cara.ESPALDA : Estampa.Cara.FRENTE;
        sincronizar();
        return true;
    }

    /** La foto cargada para esa cara, o null. */
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getFotoCargada(Estampa.Cara cara) {
        return fotos[cara.ordinal()].isEmpty() ? null : pendientes[cara.ordinal()];
    }

    /**
     * La foto que hay que DIBUJAR para esa cara, que no es siempre la que
     * esta cargada: el prensado la consume en el mismo tick en que se cierra
     * la tapa, pero la tapa tarda seis en bajar.
     */
    @org.jetbrains.annotations.Nullable
    public java.util.UUID getFotoVisible(Estampa.Cara cara) {
        if (getCachedState().get(SublimadoraBlock.OPEN)) {
            java.util.UUID cargada = getFotoCargada(cara);
            if (cargada != null) return cargada;
        }
        return arrastre > 0 ? ultimasVistas[cara.ordinal()] : null;
    }

    public float getEscala(Estampa.Cara cara) { return escalaBorrador[cara.ordinal()]; }
    public float getX(Estampa.Cara cara) { return xBorrador[cara.ordinal()]; }
    public float getY(Estampa.Cara cara) { return yBorrador[cara.ordinal()]; }
    public float getAngulo(Estampa.Cara cara) { return anguloBorrador[cara.ordinal()]; }

    /** Escala/posicion/angulo de la cara elegida, que es la que muestra la pantalla. */
    public float getEscalaBorrador() { return escalaBorrador[seleccion.ordinal()]; }
    public float getXBorrador() { return xBorrador[seleccion.ordinal()]; }
    public float getYBorrador() { return yBorrador[seleccion.ordinal()]; }
    public float getAnguloBorrador() { return anguloBorrador[seleccion.ordinal()]; }

    /**
     * Ajustan escala/posicion/angulo de la cara elegida — mismo criterio
     * que {@link #cambiarSeleccion()}: cambian SOLO la cara activa, no las
     * dos. {@code direccion} es +1 o -1 (flechitas ‹ ›).
     */
    public boolean cambiarEscala(int direccion) {
        if (estado == Estado.PRENSANDO) return false;
        int i = seleccion.ordinal();
        escalaBorrador[i] = MathHelper.clamp(escalaBorrador[i] + direccion * PASO_ESCALA,
                Estampa.ESCALA_MINIMA, Estampa.ESCALA_MAXIMA);
        sincronizar();
        return true;
    }

    public boolean cambiarX(int direccion) {
        if (estado == Estado.PRENSANDO) return false;
        int i = seleccion.ordinal();
        xBorrador[i] = MathHelper.clamp(xBorrador[i] + direccion * PASO_POSICION, -0.5f, 0.5f);
        sincronizar();
        return true;
    }

    public boolean cambiarY(int direccion) {
        if (estado == Estado.PRENSANDO) return false;
        int i = seleccion.ordinal();
        yBorrador[i] = MathHelper.clamp(yBorrador[i] + direccion * PASO_POSICION, -0.5f, 0.5f);
        sincronizar();
        return true;
    }

    /** Wrappea 0-345, mismo criterio que {@code TinturasBlockEntity#cambiarAngulo}. */
    public boolean cambiarAngulo(int direccion) {
        if (estado == Estado.PRENSANDO) return false;
        int i = seleccion.ordinal();
        float actual = anguloBorrador[i];
        anguloBorrador[i] = ((Math.round(actual) + 360 + direccion * (int) PASO_ANGULO) % 360);
        sincronizar();
        return true;
    }

    // ── botones de la pantalla (mismo mecanismo que TinturasBlockEntity) ──
    public static final int BTN_SELECCION = 0;
    public static final int BTN_ESCALA_MAS = 1;
    public static final int BTN_ESCALA_MENOS = 2;
    public static final int BTN_X_MAS = 3;
    public static final int BTN_X_MENOS = 4;
    public static final int BTN_Y_MAS = 5;
    public static final int BTN_Y_MENOS = 6;
    public static final int BTN_ANGULO_MAS = 7;
    public static final int BTN_ANGULO_MENOS = 8;
    /** Cicla la categoría de diseños — ver el javadoc de {@link #categoria}. */
    public static final int BTN_CATEGORIA = 10;
    /** Click en el slot de foto de una cara: la elige para editar (+ Cara.ordinal()). */
    public static final int BTN_ELEGIR_CARA_BASE = 20;
    /** Chincheta de una cara (+ Cara.ordinal()). */
    public static final int BTN_CHINCHETA_BASE = 22;
    /** Botón Prensar — lo atiende SublimadoraScreenHandler (necesita avisarle al jugador). */
    public static final int BTN_PRENSAR = 25;
    /** Prende/apaga la simetría lateral (medias y calientabrazos). */
    public static final int BTN_SIMETRIA = 26;
    public static final int BTN_CARGAR_DISENO_BASE = 30; // .. + DISENOS_MAXIMO
    public static final int BTN_BORRAR_DISENO_BASE = 40; // .. + DISENOS_MAXIMO

    public boolean onButtonClick(int id) {
        if (id == BTN_CATEGORIA) {
            cambiarCategoria();
            return true;
        }
        if (id >= BTN_ELEGIR_CARA_BASE && id < BTN_ELEGIR_CARA_BASE + 2) {
            Estampa.Cara cara = Estampa.Cara.values()[id - BTN_ELEGIR_CARA_BASE];
            if (cara == seleccion || estado == Estado.PRENSANDO) return false;
            seleccion = cara;
            sincronizar();
            return true;
        }
        if (id >= BTN_CHINCHETA_BASE && id < BTN_CHINCHETA_BASE + 2) {
            if (estado == Estado.PRENSANDO) return false;
            int i = id - BTN_CHINCHETA_BASE;
            caraFijada[i] = !caraFijada[i];
            seleccion = Estampa.Cara.values()[i];
            sincronizar();
            return true;
        }
        if (id == BTN_SIMETRIA) {
            if (estado == Estado.PRENSANDO || !admiteSimetria(categoria)) return false;
            simetria = !simetria;
            sincronizar();
            return true;
        }
        if (id >= BTN_CARGAR_DISENO_BASE && id < BTN_CARGAR_DISENO_BASE + DISENOS_MAXIMO) {
            return cargarDiseno(id - BTN_CARGAR_DISENO_BASE);
        }
        if (id >= BTN_BORRAR_DISENO_BASE && id < BTN_BORRAR_DISENO_BASE + DISENOS_MAXIMO) {
            return borrarDiseno(id - BTN_BORRAR_DISENO_BASE);
        }
        return switch (id) {
            case BTN_SELECCION -> cambiarSeleccion();
            case BTN_ESCALA_MAS -> cambiarEscala(1);
            case BTN_ESCALA_MENOS -> cambiarEscala(-1);
            case BTN_X_MAS -> cambiarX(1);
            case BTN_X_MENOS -> cambiarX(-1);
            case BTN_Y_MAS -> cambiarY(1);
            case BTN_Y_MENOS -> cambiarY(-1);
            case BTN_ANGULO_MAS -> cambiarAngulo(1);
            case BTN_ANGULO_MENOS -> cambiarAngulo(-1);
            default -> false;
        };
    }

    public boolean caraFijada(Estampa.Cara cara) { return caraFijada[cara.ordinal()]; }

    /** Algo fijado para guardar como diseño. */
    public boolean hayFijadas() { return caraFijada[0] || caraFijada[1]; }

    /** Los diseños guardados de la categoría activa — ver {@link #categoria}. */
    public java.util.List<DisenoEstampa> disenos() {
        return disenosPorItem.getOrDefault(categoria, java.util.List.of());
    }

    /** Nombre del diseño de ese casillero (para el hover), o null si está vacío. */
    @org.jetbrains.annotations.Nullable
    public String nombreDiseno(int idx) {
        java.util.List<DisenoEstampa> lista = disenos();
        return idx >= 0 && idx < lista.size() ? lista.get(idx).nombre() : null;
    }

    public net.minecraft.item.Item categoria() { return categoria; }

    /** Cicla la categoría a mano — ver el javadoc de {@link #categoria}. */
    private void cambiarCategoria() {
        int i = java.util.Arrays.asList(CATEGORIAS).indexOf(categoria);
        categoria = CATEGORIAS[(i + 1) % CATEGORIAS.length];
        sincronizar();
    }

    /**
     * Recibido desde {@link GuardarDisenoSublimadoraPayload}: guarda el
     * ajuste de las dos caras y qué caras están fijadas, con nombre, en el
     * próximo casillero libre de la categoría activa.
     */
    public boolean guardarDiseno(String nombreCrudo) {
        java.util.List<DisenoEstampa> lista = disenosPorItem.computeIfAbsent(categoria, k -> new java.util.ArrayList<>());
        if (!hayFijadas() || lista.size() >= DISENOS_MAXIMO) return false;
        String nombre = nombreCrudo == null || nombreCrudo.isBlank() ? "#" + (lista.size() + 1) : nombreCrudo.trim();
        if (nombre.length() > 24) nombre = nombre.substring(0, 24);
        lista.add(new DisenoEstampa(nombre, new EstampaFijada(
                escalaBorrador[0], xBorrador[0], yBorrador[0], anguloBorrador[0],
                escalaBorrador[1], xBorrador[1], yBorrador[1], anguloBorrador[1]),
                caraFijada[0], caraFijada[1], simetria));
        sincronizar();
        return true;
    }

    private boolean cargarDiseno(int idx) {
        java.util.List<DisenoEstampa> lista = disenos();
        if (idx < 0 || idx >= lista.size() || estado == Estado.PRENSANDO) return false;
        DisenoEstampa d = lista.get(idx);
        EstampaFijada f = d.ajuste();
        escalaBorrador[0] = f.escalaFrente(); xBorrador[0] = f.xFrente(); yBorrador[0] = f.yFrente(); anguloBorrador[0] = f.anguloFrente();
        escalaBorrador[1] = f.escalaEspalda(); xBorrador[1] = f.xEspalda(); yBorrador[1] = f.yEspalda(); anguloBorrador[1] = f.anguloEspalda();
        caraFijada[0] = d.fijadaFrente();
        caraFijada[1] = d.fijadaEspalda();
        simetria = d.simetria();
        sincronizar();
        return true;
    }

    private boolean borrarDiseno(int idx) {
        java.util.List<DisenoEstampa> lista = disenosPorItem.get(categoria);
        if (lista == null || idx < 0 || idx >= lista.size()) return false;
        lista.remove(idx);
        sincronizar();
        return true;
    }

    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Text getDisplayName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new SublimadoraScreenHandler(syncId, inv, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    /**
     * Todo lo que la maquina tiene adentro, para devolverlo al romperla.
     *
     * La tinta cargada no se devuelve: son cuatro tanques con hasta 16 dosis
     * cada uno y no hay item que represente una dosis suelta, asi que
     * reintegrarla seria inventar tintes de la nada.
     */
    public java.util.List<ItemStack> contenido() {
        java.util.List<ItemStack> todo = new java.util.ArrayList<>();
        if (!remera.isEmpty()) todo.add(remera);
        for (ItemStack f : fotos) if (!f.isEmpty()) todo.add(f);
        if (!salida.isEmpty()) todo.add(salida);
        return todo;
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
        for (int i = 0; i < 2; i++) {
            if (fotos[i].isEmpty()) continue;
            ItemStack out = fotos[i];
            fotos[i] = ItemStack.EMPTY;
            pendientes[i] = null;
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
        if (remera.isEmpty()) return false;
        int caras = (caraActiva(0) ? 1 : 0) + (caraActiva(1) ? 1 : 0);
        if (caras == 0) return false;
        // Una dosis de cada color POR CARA: hacer las dos en una pasada
        // ahorra el ciclo, no la tinta.
        for (int i = 0; i < 4; i++) if (cargas[i] < caras) return false;
        // 1 papel por prensado (a las dos caras juntas, no una por cara —
        // es UNA hoja de papel de sublimacion, no una por lado) — a pedido
        // (2026-09-19, "usa papel y ya no consume la imagen").
        if (papelCargado < 1) return false;

        for (int i = 0; i < 4; i++) {
            cargas[i] -= caras;
            tinta[i] = cargas[i] / (float) CARGA_MAXIMA;
        }
        papelCargado -= 1;
        sincronizarVistaTanques();
        // Las fotos YA NO se consumen al cerrar la tapa (2026-09-19): quedan
        // cargadas para reimprimir la misma cara sin volver a subirla.
        estado = Estado.PRENSANDO;
        progreso = 0;
        sincronizar();
        return true;
    }

    /** ¿Esta cara se estampa? Foto cargada y chincheta puesta. */
    private boolean caraActiva(int i) {
        return !fotos[i].isEmpty() && pendientes[i] != null && caraFijada[i];
    }

    /**
     * Botón Prensar de la GUI (2026-09-28, como Teñir en Tintes): baja la
     * tapa y arranca, igual que cerrarla a mano.
     *
     * @return null si arrancó; si no, el motivo.
     */
    @org.jetbrains.annotations.Nullable
    public Text prensarDesdeGui() {
        if (!intentarPrensar()) {
            Text motivo = queFalta();
            return motivo != null ? motivo : Text.translatable("femclothes.sublimadora.aviso.cargar");
        }
        if (world != null && getCachedState().get(SublimadoraBlock.OPEN)) {
            world.setBlockState(pos, getCachedState().with(SublimadoraBlock.OPEN, false), net.minecraft.block.Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.2f, 1.0f);
        }
        return null;
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
        if (estado == Estado.PRENSANDO) return Text.translatable("femclothes.sublimadora.aviso.prensando");
        if (estado == Estado.LISTO) return Text.translatable("femclothes.sublimadora.aviso.retirar");

        if (remera.isEmpty() || (fotos[0].isEmpty() && fotos[1].isEmpty())) {
            return Text.translatable("femclothes.sublimadora.aviso.cargar");
        }
        if (!caraActiva(0) && !caraActiva(1)) {
            return Text.translatable("femclothes.sublimadora.aviso.sin_fijar");
        }
        if (papelCargado < 1) {
            return Text.translatable("femclothes.sublimadora.aviso.papel");
        }
        if (!hayTinta()) {
            MutableText colores = Text.empty();
            boolean primero = true;
            for (int i = 0; i < 4; i++) {
                if (cargas[i] > 0) continue;
                if (!primero) colores.append(", ");
                colores.append(Text.translatable("femclothes.sublimadora.tinta." + NOMBRE_TINTA[i]));
                primero = false;
            }
            return Text.translatable("femclothes.sublimadora.aviso.tinta", colores);
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
        nbt.putInt("PapelCargado", papelCargado);
        nbt.putString("Estado", estado.name());
        nbt.putInt("Progreso", progreso);
        if (!remera.isEmpty()) nbt.put("Remera", remera.encode(registries));
        for (int i = 0; i < 2; i++) {
            if (!fotos[i].isEmpty()) nbt.put("Foto" + i, fotos[i].encode(registries));
            if (pendientes[i] != null) nbt.putUuid("Pendiente" + i, pendientes[i]);
            nbt.putFloat("Escala" + i, escalaBorrador[i]);
            nbt.putFloat("X" + i, xBorrador[i]);
            nbt.putFloat("Y" + i, yBorrador[i]);
            nbt.putFloat("Angulo" + i, anguloBorrador[i]);
        }
        nbt.putString("Seleccion", seleccion.name());
        nbt.putString("Categoria", net.minecraft.registry.Registries.ITEM.getId(categoria).toString());
        if (!salida.isEmpty()) nbt.put("Salida", salida.encode(registries));

        for (int i = 0; i < 2; i++) nbt.putBoolean("CaraFijada" + i, caraFijada[i]);
        nbt.putBoolean("Simetria", simetria);
        net.minecraft.nbt.NbtList disenosNbt = new net.minecraft.nbt.NbtList();
        for (var entry : disenosPorItem.entrySet()) {
            String claveItem = net.minecraft.registry.Registries.ITEM.getId(entry.getKey()).toString();
            for (DisenoEstampa d : entry.getValue()) {
                EstampaFijada f = d.ajuste();
                NbtCompound fc = new NbtCompound();
                fc.putString("Item", claveItem);
                fc.putString("Nombre", d.nombre());
                fc.putFloat("EscalaF", f.escalaFrente());
                fc.putFloat("XF", f.xFrente());
                fc.putFloat("YF", f.yFrente());
                fc.putFloat("AnguloF", f.anguloFrente());
                fc.putFloat("EscalaE", f.escalaEspalda());
                fc.putFloat("XE", f.xEspalda());
                fc.putFloat("YE", f.yEspalda());
                fc.putFloat("AnguloE", f.anguloEspalda());
                fc.putBoolean("FijadaF", d.fijadaFrente());
                fc.putBoolean("FijadaE", d.fijadaEspalda());
                fc.putBoolean("Simetria", d.simetria());
                disenosNbt.add(fc);
            }
        }
        nbt.put("Disenos", disenosNbt);
        net.minecraft.inventory.Inventories.writeNbt(nbt, almacen, registries);
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
        papelCargado = MathHelper.clamp(nbt.getInt("PapelCargado"), 0, CARGA_MAXIMA);
        sincronizarVistaTanques();
        estado = nbt.contains("Estado") ? Estado.valueOf(nbt.getString("Estado")) : Estado.REPOSO;
        progreso = nbt.getInt("Progreso");
        remera = nbt.contains("Remera") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Remera")) : ItemStack.EMPTY;
        for (int i = 0; i < 2; i++) {
            fotos[i] = nbt.contains("Foto" + i)
                    ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Foto" + i))
                    : ItemStack.EMPTY;
            pendientes[i] = nbt.containsUuid("Pendiente" + i) ? nbt.getUuid("Pendiente" + i) : null;
            if (nbt.contains("Escala" + i)) {
                escalaBorrador[i] = nbt.getFloat("Escala" + i);
                xBorrador[i] = nbt.getFloat("X" + i);
                yBorrador[i] = nbt.getFloat("Y" + i);
                // Angulo se agregó después (2026-09-20): ausente en
                // guardados viejos, default 0 (como venía la foto).
                anguloBorrador[i] = nbt.contains("Angulo" + i) ? nbt.getFloat("Angulo" + i) : 0f;
            } else if (nbt.contains("Modo" + i)) {
                // Compatibilidad con guardados de antes del control libre
                // (2026-09-19, "sacamos los controles del frente"): los 3
                // presets viejos (Logo/Centrada/Completo) pasan a ser su
                // escala/posicion equivalente.
                Estampa.Modo modoViejo = Estampa.Modo.valueOf(nbt.getString("Modo" + i));
                escalaBorrador[i] = modoViejo.escala;
                xBorrador[i] = modoViejo.x;
                yBorrador[i] = modoViejo.y;
                anguloBorrador[i] = 0f;
            }
        }
        if (nbt.contains("Seleccion")) seleccion = Estampa.Cara.valueOf(nbt.getString("Seleccion"));
        if (nbt.contains("Categoria")) {
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(nbt.getString("Categoria"));
            net.minecraft.item.Item item = id == null ? null : net.minecraft.registry.Registries.ITEM.get(id);
            if (item != null && item != net.minecraft.item.Items.AIR) categoria = item;
        }
        salida = nbt.contains("Salida") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Salida")) : ItemStack.EMPTY;

        // Sin la clave (mundo de antes de las chinchetas, 2026-09-28): las
        // dos caras fijadas, que es como se comportaba — se estampaba
        // toda cara con foto.
        for (int i = 0; i < 2; i++) caraFijada[i] = !nbt.contains("CaraFijada" + i) || nbt.getBoolean("CaraFijada" + i);
        simetria = nbt.getBoolean("Simetria");
        disenosPorItem.clear();
        // "EstampaFijadas" = las fijadas sin nombre de antes: se leen como diseños "#n".
        boolean viejas = !nbt.contains("Disenos");
        net.minecraft.nbt.NbtList disenosNbt = nbt.getList(viejas ? "EstampaFijadas" : "Disenos", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < disenosNbt.size(); i++) {
            NbtCompound fc = disenosNbt.getCompound(i);
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(fc.getString("Item"));
            net.minecraft.item.Item item = id == null ? null : net.minecraft.registry.Registries.ITEM.get(id);
            if (item == null || item == net.minecraft.item.Items.AIR) continue;
            java.util.List<DisenoEstampa> lista = disenosPorItem.computeIfAbsent(item, k -> new java.util.ArrayList<>());
            if (lista.size() >= DISENOS_MAXIMO) continue;
            String nombre = viejas || fc.getString("Nombre").isEmpty() ? "#" + (lista.size() + 1) : fc.getString("Nombre");
            lista.add(new DisenoEstampa(nombre, new EstampaFijada(
                    fc.getFloat("EscalaF"), fc.getFloat("XF"), fc.getFloat("YF"), fc.getFloat("AnguloF"),
                    fc.getFloat("EscalaE"), fc.getFloat("XE"), fc.getFloat("YE"), fc.getFloat("AnguloE")),
                    viejas || fc.getBoolean("FijadaF"), viejas || fc.getBoolean("FijadaE"), fc.getBoolean("Simetria")));
        }
        almacen.clear();
        net.minecraft.inventory.Inventories.readNbt(nbt, almacen, registries);
    }

    // ── la tinta viaja adentro del item ──────────────────────────────
    //
    // collectImplicit/applyImplicit es el mismo mecanismo con el que una caja
    // de shulker se lleva su contenido: la loot table copia el componente al
    // item al romper, y al colocar el bloque vuelve al block entity.

    @Override
    protected void addComponents(net.minecraft.component.ComponentMap.Builder builder) {
        super.addComponents(builder);
        java.util.List<Integer> lista = new java.util.ArrayList<>(4);
        for (int i = 0; i < 4; i++) lista.add(cargas[i]);
        builder.add(ModItems.CARGAS, lista);
        builder.add(ModItems.PAPEL_CARGADO, papelCargado);
    }

    @Override
    protected void readComponents(BlockEntity.ComponentsAccess componentes) {
        super.readComponents(componentes);
        java.util.List<Integer> lista = componentes.get(ModItems.CARGAS);
        if (lista != null) {
            for (int i = 0; i < 4 && i < lista.size(); i++) {
                cargas[i] = MathHelper.clamp(lista.get(i), 0, CARGA_MAXIMA);
                tinta[i] = cargas[i] / (float) CARGA_MAXIMA;
                mostrado[i] = tinta[i];
                anterior[i] = tinta[i];
            }
        }
        Integer papel = componentes.get(ModItems.PAPEL_CARGADO);
        if (papel != null) papelCargado = MathHelper.clamp(papel, 0, CARGA_MAXIMA);
        sincronizarVistaTanques();
    }

    /**
     * Sin esto el item quedaria con las cargas DOS veces -en el componente y
     * en el NBT del block entity copiado- y dos maquinas con la misma tinta
     * no apilarian entre si.
     */
    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        for (String clave : CLAVES) nbt.remove(clave + "Cargas");
        nbt.remove("PapelCargado");
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
