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
 * La GUI de primera interaccion —elegis cuerpo, tono y ropa interior con
 * preview la primera vez que te ponés una prenda— es lo que va arriba de
 * esto, y todavia no esta. Este comando existe para que el modelo de datos
 * se pueda probar en el juego mientras tanto: sin alguna forma de cambiar el
 * perfil, todos los jugadores se ven con el default y la mitad del sistema
 * queda sin ejercitar.
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

        LiteralArgumentBuilder<ServerCommandSource> interior = CommandManager.literal("interior");
        for (RopaInterior r : RopaInterior.values()) {
            interior.then(CommandManager.literal(r.clave).executes(ctx ->
                    aplicar(ctx.getSource(), p -> p.conInterior(r))));
        }

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
                .then(CommandManager.literal("ver").executes(ctx -> ver(ctx.getSource())));
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
                Text.translatable(perfil.interior().traduccion()));
    }
}
