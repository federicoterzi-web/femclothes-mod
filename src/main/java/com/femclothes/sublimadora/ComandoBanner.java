package com.femclothes.sublimadora;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * {@code /femclothes banner} (2026-10-06, base de los banners con foto a 16x, hasta que la Sublimadora los acepte):
 * el banner o escudo de la mano principal recibe la foto de Camerapture de la mano secundaria, centrada y cubriendo
 * toda la cara. Solo operadores (nivel 2). Sin foto en la otra mano, se la saca.
 */
public final class ComandoBanner {

    private ComandoBanner() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("femclothes").then(CommandManager.literal("banner")
                        .requires(f -> f.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity jugador = ctx.getSource().getPlayerOrThrow();
                            ItemStack mano = jugador.getMainHandStack();
                            if (!(mano.getItem() instanceof BannerItem) && !mano.isOf(Items.SHIELD)) {
                                ctx.getSource().sendError(Text.translatable("femclothes.banner.sin_banner"));
                                return 0;
                            }
                            ItemStack otra = jugador.getOffHandStack();
                            UUID foto = SublimadoraBlock.esFoto(otra) ? SublimadoraBlock.uuidDeFoto(otra) : null;
                            if (foto == null) {
                                mano.remove(ModItems.BANNER_ESTAMPA);
                                ctx.getSource().sendFeedback(() -> Text.translatable("femclothes.banner.quitada"), false);
                            } else {
                                mano.set(ModItems.BANNER_ESTAMPA, new Estampa(foto, Estampa.ESCALA_CUBRIR, 0f, 0f, 0f, true));
                                ctx.getSource().sendFeedback(() -> Text.translatable("femclothes.banner.puesta"), false);
                            }
                            return 1;
                        }))));
    }
}
