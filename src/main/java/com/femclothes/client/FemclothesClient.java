package com.femclothes.client;

import com.femclothes.item.ClothingStyle;
import com.femclothes.item.FemclothesItems;
import com.femclothes.render.BodyPartTrinketRenderer;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.CroptopArmorRenderProvider;
import com.femclothes.render.SkinTextureAccess;
import com.femclothes.render.SkinToneSampler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.LivingEntity;
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
                new BodyPartTrinketRenderer(BodyPartTrinketRenderer.Part.LEGS, (stack, side, entity) -> {
                    int baseColor = ClothingStyle.baseColor(stack, side);
                    Identifier patternId = ClothingStyle.patternId(stack, side);
                    Identifier mask = patternId == null ? null
                            : ClothingTextureCache.patternMaskFor("socks", patternId);
                    // Arriba de la media hay que reconstruir la pierna
                    // desnuda: dejarla transparente mostraria el pantalon
                    // pintado en la skin del jugador, no piel.
                    return ClothingTextureCache.composeGarment(socksSolidBase, baseColor,
                            mask, ClothingStyle.patternColor(stack, side), skinTones(entity),
                            ClothingTextureCache.Shading.LEGS);
                }));

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").
    }

    /** Tonos de piel del jugador, para reconstruir con sombreado la piel que la prenda expone. */
    private static SkinToneSampler.Tones skinTones(LivingEntity entity) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            SkinTextures skin = player.getSkinTextures();
            NativeImage img = SkinTextureAccess.tryGetImage(skin);
            if (img != null) {
                return SkinToneSampler.sampleTones(img, skin.model());
            }
        }
        return SkinToneSampler.fallbackTones();
    }
}
