package com.femclothes.client;

import com.femclothes.item.ClothingStyle;
import com.femclothes.item.FemclothesItems;
import com.femclothes.render.BodyPartTrinketRenderer;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.CroptopArmorRenderProvider;
import com.femclothes.screen.FemclothesScreenHandlers;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;

public class FemclothesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Pantalla del clothing_loom. HandledScreens.register es privado en
        // vanilla; lo abre el accesswidener de fabric-screen-handler-api-v1,
        // que Loom aplica en tiempo de compilacion.
        HandledScreens.register(FemclothesScreenHandlers.CLOTHING_LOOM, ClothingLoomScreen::new);

        // TODO (en migración): CroptopArmorRenderProvider todavía usa la
        // geometría de armadura vieja (más ancha) — queda pendiente de
        // pasar a BodyPartTrinketRenderer junto con shorts.
        CroptopArmorRenderProvider.register();

        // --- SOCKS_SOLID: primera prenda migrada al sistema nuevo ---
        // Se dibuja pegada a la pierna real (BodyPartTrinketRenderer.Part.LEGS)
        // en vez de con geometría de bota. La textura base se tiñe en
        // runtime con ClothingTextureCache, igual que antes pero ya no
        // depende del "dyeable" automático de ArmorItem (eso solo existe
        // en el pipeline viejo).
        // Icono del item: capa 0 = la media tenida con el color base,
        // capa 1 = las rayas tenidas con el color del patron. Sin patron, la
        // capa 1 se pinta del MISMO color que la base y las rayas desaparecen
        // — un ItemColorProvider no puede ocultar una capa, pero si fundirla.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            int base = ClothingStyle.baseColor(stack, ClothingStyle.Side.LEFT);
            int color = base;
            if (tintIndex == 1) {
                Identifier pattern = ClothingStyle.patternId(stack, ClothingStyle.Side.LEFT);
                if (pattern != null) color = ClothingStyle.patternColor(stack, ClothingStyle.Side.LEFT);
            }
            // El tinte de item es ARGB y el alfa CUENTA: vanilla devuelve -1
            // para "sin tinte". Nuestros colores son 0xRRGGBB, o sea alfa 0,
            // y sin este OR el item se dibuja transparente — desaparecia.
            return 0xFF000000 | color;
        }, FemclothesItems.SOCKS_SOLID);

        Identifier socksSolidBase = Identifier.of("femclothes", "textures/models/armor/socks_solid_layer_1.png");
        TrinketRendererRegistry.registerRenderer(FemclothesItems.SOCKS_SOLID,
                new BodyPartTrinketRenderer(BodyPartTrinketRenderer.Part.LEGS, (stack, side, entity) -> {
                    int baseColor = ClothingStyle.baseColor(stack, side);
                    Identifier patternId = ClothingStyle.patternId(stack, side);
                    Identifier mask = patternId == null ? null
                            : ClothingTextureCache.patternMaskFor("socks", patternId);
                    // Arriba de la media hay que reconstruir la pierna
                    // desnuda: dejarla transparente mostraria el pantalon
                    // pintado en la skin del jugador, no piel.
                    // Sin relleno de piel: de eso se encarga ComposedSkin, una
                    // vez sobre la skin. Ademas evita leer la skin desde la GPU
                    // en pleno render, que es lo que hacia esta llamada.
                    return ClothingTextureCache.composeGarment(socksSolidBase, baseColor,
                            mask, ClothingStyle.patternColor(stack, side), null,
                            ClothingTextureCache.Shading.LEGS);
                }));

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").
    }

}
