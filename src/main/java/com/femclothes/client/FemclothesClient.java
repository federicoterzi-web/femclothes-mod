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
        HandledScreens.register(FemclothesScreenHandlers.MANIQUI, ManiquiScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.ROPA, RopaScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.ESTILISTA, EstilistaScreen::new);
        HandledScreens.register(FemclothesScreenHandlers.ESTILADO, EstiladoScreen::new);

        // Los 3 bloques del zip "Bloque de sublimadora Minecraft" (2026-09-30):
        // Guardarropas y Mesa de estilado con el GeoModel genérico; el
        // Maniquí con su renderer propio (dibuja además la ropa puesta).
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.guardarropas.GuardarropasMod.GUARDARROPAS_BLOCK_ENTITY,
                ctx -> new software.bernie.geckolib.renderer.GeoBlockRenderer<>(
                        new com.femclothes.bloque.ModeloGeo<com.femclothes.guardarropas.GuardarropasBlockEntity>("wardrobe")));
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.estilado.EstiladoMod.ESTILADO_BLOCK_ENTITY,
                ctx -> new software.bernie.geckolib.renderer.GeoBlockRenderer<>(
                        new com.femclothes.bloque.ModeloGeo<com.femclothes.estilado.EstiladoBlockEntity>("styling_table")));
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.maniqui.ManiquiMod.MANIQUI_BLOCK_ENTITY,
                com.femclothes.maniqui.ManiquiRenderer::new);

        // Cinta transportadora (2026-10-04): la banda tiene huecos (alfa) en las curvas.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.cinta.CintaMod.CINTA_BLOCK_ENTITY, com.femclothes.cinta.CintaRenderer::new);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.cinta.CintaMod.CINTA_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.cinta.CintaMod.EMPALME_BLOCK_ENTITY, com.femclothes.cinta.EmpalmeRenderer::new);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.cinta.CintaMod.EMPALME_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());

        // El render de la Mesa de Modelado lo hace GeckoLib desde el block
        // entity (modelo garment_shaper) — mismo patrón que la sublimadora.
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.modelado.ModeladoMod.MODELADO_BLOCK_ENTITY,
                ctx -> new com.femclothes.modelado.ModeladoRenderer());
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.estilista.EstilistaMod.ESTILISTA_BLOCK_ENTITY, ctx -> new com.femclothes.estilista.EstilistaRenderer());
        // garment_shaper_atlas.png tiene alfa (huecos reales entre piezas del
        // modelo) — sin cutout el recorte se rellena y queda un cuadrado.
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.modelado.ModeladoMod.MODELADO_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.modelado.ModeladoMod.MODELADO_CREATIVA, net.minecraft.client.render.RenderLayer.getCutout());

        // El render de la Estación de Tintes lo hace GeckoLib desde el block
        // entity (modelo dye_station, assets reales bajados por el usuario).
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                com.femclothes.tinturas.TinturasMod.TINTURAS_BLOCK_ENTITY,
                ctx -> new com.femclothes.tinturas.TinturasRenderer());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.tinturas.TinturasMod.TINTURAS_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.femclothes.tinturas.TinturasMod.TINTURAS_CREATIVA, net.minecraft.client.render.RenderLayer.getCutout());

        PiezasDelMod.init();
        DebugApariencia.init();
        ElegirCuerpoCliente.init();
        TwirlCliente.init();
        CapuchaCliente.init();
        RopaCliente.init();

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
        // Muestra de color (2026-09-30): el líquido (capa 0) toma el color de la mezcla.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 0) return -1;
            int[] m = com.femclothes.item.MuestraColorItem.mezcla(stack);
            return m == null ? 0xFFB0B0B0 : 0xFF000000 | com.femclothes.item.MuestraColorItem.rgb(m);
        }, FemclothesItems.TINTE_MEZCLA);
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
                FemclothesItems.SOCKS_SOLID, FemclothesItems.CALIENTABRAZOS, FemclothesItems.CAPA}) {
            net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(item, icono);
        }

        // Molde de aplique personalizado (2026-10-04): papel kraft con el objeto encima.
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
                FemclothesItems.MOLDE_APLIQUE_PERSONALIZADO, new MoldeApliqueItemRenderer());

        // Retazo de aplique (2026-10-01): el ícono provisorio se tiñe con la zona 1.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0
                ? 0xFF000000 | com.femclothes.aplique.RetazoApliqueItem.colores(stack).get(0) : -1,
                FemclothesItems.RETAZO_APLIQUE);

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
