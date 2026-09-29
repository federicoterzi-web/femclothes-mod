package com.femclothes.client;

import com.femclothes.item.FemclothesItems;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class FemclothesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(FemclothesScreenHandlers.MODELADO, ModeladoScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.TINTURAS, TinturasScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.SUBLIMADORA, SublimadoraScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.GUARDARROPAS, GuardarropasScreen::new);

        // El render de la Mesa de Modelado lo hace GeckoLib desde el block
        // entity (modelo garment_shaper) — mismo patrón que la sublimadora.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.modelado.ModeladoMod.MODELADO_BLOCK_ENTITY,
                ctx -> new com.femclothes.modelado.ModeladoRenderer());
        // garment_shaper_atlas.png tiene alfa (huecos reales entre piezas del
        // modelo) — sin cutout el recorte se rellena y queda un cuadrado.
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.modelado.ModeladoMod.MODELADO_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());

        // El render de la Estación de Tintes lo hace GeckoLib desde el block
        // entity (modelo dye_station, assets reales bajados por el usuario).
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.tinturas.TinturasMod.TINTURAS_BLOCK_ENTITY,
                ctx -> new com.femclothes.tinturas.TinturasRenderer());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.tinturas.TinturasMod.TINTURAS_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());

        PiezasDelMod.init();
        DebugApariencia.init();
        ElegirCuerpoCliente.init();

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

        // Íconos de 64x64 armados con la tela real (2026-09-29, "ok pero los
        // hagamos 64x64" / "B"): muestran capas, patrones, redes, estampas y
        // el largo de verdad. La remera se registra en SublimadoraModClient.
        IconoPrendaItemRenderer icono = new IconoPrendaItemRenderer(null);
        for (var item : new net.minecraft.item.Item[]{FemclothesItems.PANTALON, FemclothesItems.POLLERA,
                FemclothesItems.SOCKS_SOLID, FemclothesItems.CALIENTABRAZOS}) {
            net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(item, icono);
        }

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").

        // Sin esto, F3+T releía los .png del disco pero ClothingTextureCache
        // seguía devolviendo la composición vieja desde su cache estático
        // -encontrado jugando, iterando la textura de calientabrazos: hacía
        // falta relanzar el cliente entero para ver un cambio de asset.
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("femclothes", "clothing_texture_cache");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        ClothingTextureCache.limpiarCache();
                        com.femclothes.render.IconoPrenda.limpiar();
                    }
                });
    }
}
