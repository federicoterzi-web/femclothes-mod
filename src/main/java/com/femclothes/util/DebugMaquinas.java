package com.femclothes.util;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Alterna el tiempo de procesado de las 3 máquinas (Modeladora,
 * Sublimadora, Estación de Tintes) entre su duración real y
 * instantáneo — a pedido (2026-09-21, "le podemos agregar un comando
 * para alternar entre esa duracion e instantaneo para motivos de
 * debug"). Solo afecta CUÁNTO tarda: la validación y el costo de
 * insumos (tinta/papel) siguen exactamente igual, así que instantáneo
 * sigue sirviendo para probar esa parte sin esperar.
 *
 * Server-side y global (no por jugador) — de ahí que necesite permiso
 * de operador, a diferencia de {@code ComandoCuerpo} (apariencia propia,
 * sin permisos) o {@code DebugApariencia} (cliente, solo afecta lo que
 * VE quien lo usa): esto cambia el juego real para todos.
 */
public final class DebugMaquinas {

    private DebugMaquinas() {}

    private static volatile boolean instantaneo = false;

    /**
     * Máquinas sin insumos (2026-10-01, "agregame un comando pa q las
     * estaciones no requieran insumos pa motivos de debug y
     * experimentacion"): Tintes no pide ni gasta tinta, la Sublimadora ni
     * tinta ni papel, la Mesa de estilado no pide ni gasta retazo (sin
     * retazo, el aplique sale blanco) y Envasar no pide frasco. Global,
     * como {@link #instantaneo}, y no se guarda: al reiniciar vuelve a normal.
     */
    private static volatile boolean gratis = false;

    public static boolean instantaneo() { return instantaneo; }

    public static boolean gratis() { return gratis; }

    /** La duración a usar de verdad: 1 tick si el modo debug está prendido, si no la real. */
    public static int duracion(int ticksReales) {
        return instantaneo ? 1 : ticksReales;
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(raiz()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> raiz() {
        return CommandManager.literal("femclothes")
                .then(CommandManager.literal("debug")
                        .requires(fuente -> fuente.hasPermissionLevel(2))
                        .then(CommandManager.literal("instantaneo").executes(ctx -> cambiar(ctx.getSource(), true)))
                        .then(CommandManager.literal("normal").executes(ctx -> cambiar(ctx.getSource(), false)))
                        .then(CommandManager.literal("ver").executes(ctx -> ver(ctx.getSource())))
                        .then(CommandManager.literal("gratis")
                                .executes(ctx -> cambiarGratis(ctx.getSource(), !gratis))
                                .then(CommandManager.literal("si").executes(ctx -> cambiarGratis(ctx.getSource(), true)))
                                .then(CommandManager.literal("no").executes(ctx -> cambiarGratis(ctx.getSource(), false))))
                        .then(CommandManager.literal("patrones").executes(ctx -> darPatrones(ctx.getSource())))
                        .then(CommandManager.literal("moldes").executes(ctx -> darMoldes(ctx.getSource())))
                        .then(CommandManager.literal("insumos").executes(ctx -> darInsumos(ctx.getSource())))
                        .then(CommandManager.literal("kit").executes(ctx -> {
                            darMoldes(ctx.getSource());
                            darPatrones(ctx.getSource());
                            return darInsumos(ctx.getSource());
                        })));
    }

    /**
     * Entrega shulker box(es) con un ejemplar de CADA patrón registrado
     * — a pedido (2026-09-21, "podemos agregar una o dos shulkers con
     * todos los patrones?"). La lista sale de
     * {@link com.femclothes.item.ClothingPatternItem#todos()} — si se
     * agregan más patrones después, este comando los suma solo, sin
     * tocar código.
     */
    private static int darPatrones(ServerCommandSource fuente) {
        net.minecraft.server.network.ServerPlayerEntity jugador = fuente.getPlayer();
        if (jugador == null) return 0;

        java.util.List<net.minecraft.item.ItemStack> contenido = new java.util.ArrayList<>();
        for (com.femclothes.item.ClothingPatternItem patron : com.femclothes.item.ClothingPatternItem.todos()) {
            contenido.add(new net.minecraft.item.ItemStack(patron));
        }
        int cajas = darEnShulkers(jugador, contenido, "femclothes.debug.patrones.nombre");
        int cantidad = contenido.size();
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.patrones.entregado", cantidad, cajas), false);
        return 1;
    }

    /**
     * Entrega shulker box(es) con un ejemplar de CADA "molde de corte"
     * registrado — a pedido (2026-09-21, "los moldes de corte estan?"),
     * la segunda mitad del mismo pedido de los patrones. A diferencia
     * de los patrones, los moldes NO tienen un registro central propio
     * (están repartidos en {@code ModeladoMod}/{@code ModItems}/
     * {@code FemclothesItems}, ~46 ítems entre
     * {@code MoldeDeCorteItem}/{@code MoldeRangoItem}/{@code MoldeTorsoItem}/
     * {@code MoldeCalceItem}/{@code MoldeRedItem}/{@code MoldeItem}/
     * {@code MoldeLargoRemeraItem}/{@code MoldeCuelloItem}/
     * {@code MoldePantalonItem}/{@code MoldeTiroItem}/{@code MoldeMediaItem}
     * — ninguna interfaz común) — por eso se recorre TODO el registro de
     * ítems y se filtra por el nombre de la clase, en vez de mantener a
     * mano una lista de 46 referencias que se desactualizaría la
     * próxima vez que se agregue un molde nuevo.
     */
    private static int darMoldes(ServerCommandSource fuente) {
        net.minecraft.server.network.ServerPlayerEntity jugador = fuente.getPlayer();
        if (jugador == null) return 0;

        java.util.List<net.minecraft.item.ItemStack> contenido = new java.util.ArrayList<>();
        for (net.minecraft.item.Item item : net.minecraft.registry.Registries.ITEM) {
            if (item.getClass().getSimpleName().contains("Molde")) {
                contenido.add(new net.minecraft.item.ItemStack(item));
            }
        }
        int cajas = darEnShulkers(jugador, contenido, "femclothes.debug.moldes.nombre");
        int cantidad = contenido.size();
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.moldes.entregado", cantidad, cajas), false);
        return 1;
    }

    /**
     * Shulker de insumos (2026-09-29, "agreguemos una shulker con un stack
     * de cada color y uno de papel"): 64 de cada uno de los 16 tintes (los
     * C/M/Y/K cargan la Estación de Tintes y la Sublimadora) y 64 de papel
     * (Sublimadora). {@code kit} da moldes, patrones e insumos de una.
     */
    private static int darInsumos(ServerCommandSource fuente) {
        net.minecraft.server.network.ServerPlayerEntity jugador = fuente.getPlayer();
        if (jugador == null) return 0;

        java.util.List<net.minecraft.item.ItemStack> contenido = new java.util.ArrayList<>();
        for (net.minecraft.util.DyeColor color : net.minecraft.util.DyeColor.values()) {
            contenido.add(new net.minecraft.item.ItemStack(net.minecraft.item.DyeItem.byColor(color), 64));
        }
        contenido.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.PAPER, 64));
        darEnShulkers(jugador, contenido, "femclothes.debug.insumos.nombre");
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.insumos.entregado"), false);
        return 1;
    }

    /** Reparte {@code items} en shulker boxes de 27 (lo que entra en una), tantas como haga falta, y se las da a {@code jugador}. Devuelve cuántas cajas entregó. */
    private static int darEnShulkers(net.minecraft.server.network.ServerPlayerEntity jugador,
                                      java.util.List<net.minecraft.item.ItemStack> items, String claveNombre) {
        int cajas = 0;
        for (int i = 0; i < items.size(); i += 27) {
            java.util.List<net.minecraft.item.ItemStack> parte = items.subList(i, Math.min(i + 27, items.size()));
            net.minecraft.item.ItemStack caja = new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHULKER_BOX);
            caja.set(net.minecraft.component.DataComponentTypes.CONTAINER,
                    net.minecraft.component.type.ContainerComponent.fromStacks(parte));
            caja.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.translatable(claveNombre));
            jugador.giveItemStack(caja);
            cajas++;
        }
        return cajas;
    }

    private static int cambiar(ServerCommandSource fuente, boolean valor) {
        instantaneo = valor;
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.maquinas." + (valor ? "instantaneo" : "normal")), false);
        return 1;
    }

    private static int ver(ServerCommandSource fuente) {
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.maquinas." + (instantaneo ? "instantaneo" : "normal")), false);
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.gratis." + (gratis ? "si" : "no")), false);
        return 1;
    }

    private static int cambiarGratis(ServerCommandSource fuente, boolean valor) {
        gratis = valor;
        fuente.sendFeedback(() -> Text.translatable("femclothes.debug.gratis." + (valor ? "si" : "no")), true);
        return 1;
    }
}
