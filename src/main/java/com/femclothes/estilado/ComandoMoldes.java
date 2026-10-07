package com.femclothes.estilado;

import com.femclothes.aplique.Aplique;
import com.femclothes.aplique.MoldeApliquePersonalizadoItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * {@code /femclothes moldes} (2026-10-04, "dame un comando para ver moldes hechos por otros players"):
 * <ul>
 *   <li>{@code /femclothes moldes}: tus moldes de aplique fabricados en la Mesa creativa;</li>
 *   <li>{@code /femclothes moldes dar <n>}: una copia del tuyo número n;</li>
 *   <li>{@code /femclothes moldes de <jugador>}: los moldes que hizo otro jugador (cualquiera puede verlos);</li>
 *   <li>{@code /femclothes moldes de <jugador> dar <n>}: una copia del molde de otro — solo operadores (nivel 2),
 *       porque fabricar moldes es cosa de creativo.</li>
 * </ul>
 */
public final class ComandoMoldes {

    private ComandoMoldes() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("femclothes").then(raiz())));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> raiz() {
        return CommandManager.literal("moldes")
                .executes(ctx -> listar(ctx.getSource(), ctx.getSource().getPlayerOrThrow().getUuid(), null))
                .then(CommandManager.literal("dar")
                        .then(CommandManager.argument("numero", IntegerArgumentType.integer(1, MoldesGuardados.MAXIMO_POR_JUGADOR))
                                .executes(ctx -> dar(ctx.getSource(), ctx.getSource().getPlayerOrThrow().getUuid(),
                                        IntegerArgumentType.getInteger(ctx, "numero")))))
                .then(CommandManager.literal("de")
                        .then(CommandManager.argument("jugador", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(
                                        MoldesGuardados.de(ctx.getSource().getServer()).autores(), b))
                                .executes(ctx -> {
                                    String nombre = StringArgumentType.getString(ctx, "jugador");
                                    UUID id = MoldesGuardados.de(ctx.getSource().getServer()).autorPorNombre(nombre);
                                    if (id == null) {
                                        ctx.getSource().sendError(Text.translatable("femclothes.moldes.sin_autor", nombre));
                                        return 0;
                                    }
                                    return listar(ctx.getSource(), id, nombre);
                                })
                                .then(CommandManager.literal("dar")
                                        .requires(f -> f.hasPermissionLevel(2))
                                        .then(CommandManager.argument("numero", IntegerArgumentType.integer(1, MoldesGuardados.MAXIMO_POR_JUGADOR))
                                                .executes(ctx -> {
                                                    String nombre = StringArgumentType.getString(ctx, "jugador");
                                                    UUID id = MoldesGuardados.de(ctx.getSource().getServer()).autorPorNombre(nombre);
                                                    if (id == null) {
                                                        ctx.getSource().sendError(Text.translatable("femclothes.moldes.sin_autor", nombre));
                                                        return 0;
                                                    }
                                                    return dar(ctx.getSource(), id, IntegerArgumentType.getInteger(ctx, "numero"));
                                                })))));
    }

    /** Qué es el molde, en una frase: el modelo del mod o el objeto. */
    private static Text descripcion(ItemStack molde) {
        Aplique p = molde.getItem() instanceof MoldeApliquePersonalizadoItem ? MoldeApliquePersonalizadoItem.plantilla(molde) : null;
        if (p == null) return Text.empty();
        if (p.objeto() != null) return p.objeto().item().getName();
        return Text.translatable("femclothes.moldes.modelo." + p.modelo().clave);
    }

    private static int listar(ServerCommandSource fuente, UUID autor, String nombre) {
        List<MoldesGuardados.Entrada> lista = MoldesGuardados.de(fuente.getServer()).de(autor);
        if (lista.isEmpty()) {
            fuente.sendFeedback(() -> nombre == null ? Text.translatable("femclothes.moldes.vacio")
                    : Text.translatable("femclothes.moldes.vacio_de", nombre), false);
            return 0;
        }
        fuente.sendFeedback(() -> (nombre == null ? Text.translatable("femclothes.moldes.titulo", lista.size())
                : Text.translatable("femclothes.moldes.titulo_de", lista.size(), nombre)).formatted(Formatting.GOLD), false);
        // Del más nuevo al más viejo, con el número que se usa en "dar".
        for (int i = lista.size() - 1; i >= 0; i--) {
            MoldesGuardados.Entrada e = lista.get(i);
            int numero = lista.size() - i;
            fuente.sendFeedback(() -> Text.literal(numero + ". ").formatted(Formatting.GRAY)
                    .append(e.molde().getName()).append(Text.literal(" — ").formatted(Formatting.DARK_GRAY))
                    .append(descripcion(e.molde()).copy().formatted(Formatting.GRAY)), false);
        }
        return lista.size();
    }

    /** {@code numero}: 1 = el más nuevo (el mismo que muestra {@link #listar}). */
    private static int dar(ServerCommandSource fuente, UUID autor, int numero) {
        ServerPlayerEntity jugador = fuente.getPlayer();
        List<MoldesGuardados.Entrada> lista = MoldesGuardados.de(fuente.getServer()).de(autor);
        if (jugador == null || numero > lista.size()) {
            fuente.sendError(Text.translatable("femclothes.moldes.no_existe", numero));
            return 0;
        }
        ItemStack copia = lista.get(lista.size() - numero).molde().copy();
        jugador.getInventory().offerOrDrop(copia);
        fuente.sendFeedback(() -> Text.translatable("femclothes.moldes.dado", copia.getName()), false);
        return 1;
    }
}
