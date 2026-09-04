package com.femclothes.client;

import com.femclothes.item.ClothingStyle;
import com.femclothes.item.FemclothesItems;
import com.femclothes.render.BodyPartTrinketRenderer;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.CroptopArmorRenderProvider;
import com.femclothes.screen.FemclothesScreenHandlers;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
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
        Identifier socksSolidBase = Identifier.of("femclothes", "textures/models/armor/socks_solid_layer_1.png");
        TrinketRendererRegistry.registerRenderer(FemclothesItems.SOCKS_SOLID,
                new BodyPartTrinketRenderer(BodyPartTrinketRenderer.Part.LEGS, (stack, side) -> {
                    int baseColor = ClothingStyle.baseColor(stack, side);
                    // Sin patron en ESE lado, la pierna es lisa y alcanza con
                    // tenir la base — el camino barato es el caso comun.
                    Identifier patternId = ClothingStyle.patternId(stack, side);
                    if (patternId == null) {
                        return ClothingTextureCache.tinted(socksSolidBase, baseColor);
                    }
                    return ClothingTextureCache.tintedWithPattern(socksSolidBase, baseColor,
                            ClothingTextureCache.patternMaskFor("socks", patternId),
                            ClothingStyle.patternColor(stack, side));
                }));

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").
    }
}
