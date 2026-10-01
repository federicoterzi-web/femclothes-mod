package com.femclothes.body;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Elegir cuerpo base sin GUI.
 *
 * La GUI de primera interaccion (2026-09-29, {@code ElegirCuerpoScreen}:
 * salta la primera vez que te ponés una prenda) va arriba de esto; los
 * comandos quedan para overridear directo, y {@code /femclothes elegir}
 * vuelve a abrir la GUI.
 *
 * Sin permisos especiales: cada quien cambia el suyo. No es una operacion de
 * administrador, es la apariencia del propio jugador.
 */
public final class ComandoCuerpo {

    private ComandoCuerpo() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(raiz()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> raiz() {
        LiteralArgumentBuilder<ServerCommandSource> cuerpo = CommandManager.literal("cuerpo");
        for (CuerpoBase c : CuerpoBase.values()) {
            cuerpo.then(CommandManager.literal(c.clave).executes(ctx ->
                    aplicar(ctx.getSource(), p -> p.conCuerpo(c))));
        }

        // Dos partes + color (2026-09-30): /femclothes interior arriba|abajo <tipo>, interior color <rgb>.
        LiteralArgumentBuilder<ServerCommandSource> interiorArriba = CommandManager.literal("arriba");
        for (InteriorArriba a : InteriorArriba.values()) {
            interiorArriba.then(CommandManager.literal(a.clave).executes(ctx ->
                    aplicar(ctx.getSource(), p -> p.conInterior(p.interior().conArriba(a)))));
        }
        LiteralArgumentBuilder<ServerCommandSource> interiorAbajo = CommandManager.literal("abajo");
        for (InteriorAbajo b : InteriorAbajo.values()) {
            interiorAbajo.then(CommandManager.literal(b.clave).executes(ctx ->
                    aplicar(ctx.getSource(), p -> p.conInterior(p.interior().conAbajo(b)))));
        }
        LiteralArgumentBuilder<ServerCommandSource> interior = CommandManager.literal("interior")
                .then(interiorArriba)
                .then(interiorAbajo)
                .then(CommandManager.literal("color").then(CommandManager.argument("rgb", IntegerArgumentType.integer(0, 0xFFFFFF))
                        .executes(ctx -> aplicar(ctx.getSource(),
                                p -> p.conInterior(p.interior().conColor(IntegerArgumentType.getInteger(ctx, "rgb")))))));

        LiteralArgumentBuilder<ServerCommandSource> tono = CommandManager.literal("tono")
                // "skin" y no un color: es volver a derivarlo de la propia
                // skin del jugador, que es el default y no un valor concreto.
                .then(CommandManager.literal("skin").executes(ctx ->
                        aplicar(ctx.getSource(), p -> p.conTono(PerfilCuerpo.TONO_DE_LA_SKIN))))
                .then(CommandManager.argument("rgb", IntegerArgumentType.integer(0, 0xFFFFFF))
                        .executes(ctx -> aplicar(ctx.getSource(),
                                p -> p.conTono(IntegerArgumentType.getInteger(ctx, "rgb")))));

        return CommandManager.literal("femclothes")
                .then(cuerpo)
                .then(interior)
                .then(tono)
                .then(CommandManager.literal("reset").executes(ctx ->
                        aplicar(ctx.getSource(), p -> PerfilCuerpo.DEFECTO)))
                // Relieve (2026-10-01): busto (lo que suman los Estrógenos) y
                // cuánto se marcan los músculos, para probar sin la GUI.
                .then(CommandManager.literal("busto").then(CommandManager.argument("tamano",
                                IntegerArgumentType.integer(0, PerfilCuerpo.BUSTO_MAXIMO))
                        .executes(ctx -> aplicar(ctx.getSource(),
                                p -> p.conBusto(IntegerArgumentType.getInteger(ctx, "tamano"),
                                        System.currentTimeMillis() + PerfilCuerpo.DURACION_ESTROGENOS)))))
                .then(CommandManager.literal("definicion").then(CommandManager.argument("porcentaje",
                                IntegerArgumentType.integer(0, PerfilCuerpo.DEFINICION_MAXIMA))
                        .executes(ctx -> aplicar(ctx.getSource(),
                                p -> p.conDefinicion(IntegerArgumentType.getInteger(ctx, "porcentaje"))))))
                .then(CommandManager.literal("ver").executes(ctx -> ver(ctx.getSource())))
                // Vuelve a abrir la GUI de elegir cuerpo (2026-09-29).
                .then(CommandManager.literal("elegir").executes(ctx -> {
                    ServerPlayerEntity jugador = ctx.getSource().getPlayer();
                    if (jugador == null) return 0;
                    RedCuerpo.abrirEn(jugador);
                    return 1;
                }));
    }

    private interface Cambio {
        PerfilCuerpo aplicar(PerfilCuerpo actual);
    }

    private static int aplicar(ServerCommandSource fuente, Cambio cambio) {
        ServerPlayerEntity jugador = fuente.getPlayer();
        if (jugador == null) return 0;
        PerfilCuerpo nuevo = cambio.aplicar(PerfilesDeCuerpo.de(jugador));
        PerfilesDeCuerpo.poner(jugador, nuevo);
        fuente.sendFeedback(() -> describir(nuevo), false);
        return 1;
    }

    private static int ver(ServerCommandSource fuente) {
        ServerPlayerEntity jugador = fuente.getPlayer();
        if (jugador == null) return 0;
        fuente.sendFeedback(() -> describir(PerfilesDeCuerpo.de(jugador)), false);
        return 1;
    }

    private static Text describir(PerfilCuerpo perfil) {
        return Text.translatable("femclothes.cuerpo.actual",
                Text.translatable(perfil.cuerpo().traduccion()),
                perfil.tonoDerivado()
                        ? Text.translatable("femclothes.cuerpo.tono_de_la_skin")
                        : Text.literal("#" + String.format("%06X", perfil.tono())),
                Text.translatable(perfil.interior().arriba().traduccion()).append(" + ")
                        .append(Text.translatable(perfil.interior().abajo().traduccion())))
                .append(Text.translatable("femclothes.cuerpo.relieve", perfil.busto(), perfil.definicion()));
    }
}
