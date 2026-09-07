package com.femclothes.client;

import com.femclothes.item.FemclothesItems;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;

public class FemclothesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Pantalla del clothing_loom. HandledScreens.register es privado en
        // vanilla; lo abre el accesswidener de fabric-screen-handler-api-v1,
        // que Loom aplica en tiempo de compilacion.
        HandledScreens.register(FemclothesScreenHandlers.CLOTHING_LOOM, ClothingLoomScreen::new);

        PiezasDelMod.init();

        // TODA la ropa del mod se dibuja desde un solo feature renderer.
        //
        // Antes cada prenda se registraba en TrinketRendererRegistry y se
        // dibujaba sola. Eso alcanzaba con una prenda por parte del cuerpo,
        // pero el orden lo decidia Trinkets por el order de los slots: no
        // habia forma de decir "el short va arriba de la media". El ordinal
        // de capa necesita un unico punto de dibujo.
        //
        // LivingEntityFeatureRendererRegistrationCallback es un hook publico
        // de Fabric API — sigue sin hacer falta ningun Mixin para la ropa.
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (tipo, renderer, helper, ctx) -> {
                    if (renderer instanceof PlayerEntityRenderer jugador) {
                        helper.register(new GarmentFeatureRenderer<>(jugador));
                    }
                });

        // Icono: capa 0 = la prenda tenida con el color base, capa 1 (si el
        // modelo la tiene) = las rayas tenidas con el color del patron. Sin
        // patron, la capa 1 se pinta del MISMO color que la base y las rayas
        // desaparecen — un ItemColorProvider no puede ocultar una capa, pero
        // si fundirla.
        //
        // Shorts comparte el mismo callback aunque su modelo sea de una sola
        // capa: tintIndex 1 nunca se pide para ese item, asi que la rama de
        // patron simplemente no se ejecuta.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            int base = RegionResolver.colorBase(stack, Lado.IZQUIERDA);
            int color = base;
            if (tintIndex == 1) {
                Identifier patron = RegionResolver.patronId(stack, Lado.IZQUIERDA);
                if (patron != null) color = RegionResolver.colorPatron(stack, Lado.IZQUIERDA);
            }
            // El tinte de item es ARGB y el alfa CUENTA: vanilla devuelve -1
            // para "sin tinte". Nuestros colores son 0xRRGGBB, o sea alfa 0,
            // y sin este OR el item se dibuja transparente — desaparecia.
            return 0xFF000000 | color;
        }, FemclothesItems.SOCKS_SOLID, FemclothesItems.PANTALON);

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").
    }
}
