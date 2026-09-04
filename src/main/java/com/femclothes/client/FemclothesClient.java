package com.femclothes.client;

import com.femclothes.item.ClothingTrinketItem;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesItems;
import com.femclothes.render.BodyPartTrinketRenderer;
import com.femclothes.render.ClothingTextureCache;
import com.femclothes.render.CroptopArmorRenderProvider;
import com.femclothes.render.TwoToneArmorRenderProvider;
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

        // TODO (en migración): CroptopArmorRenderProvider y
        // TwoToneArmorRenderProvider todavía usan la geometría de armadura
        // vieja (más ancha) — quedan pendientes de pasar a
        // BodyPartTrinketRenderer en la próxima tanda, junto con el resto
        // de las prendas. Por ahora solo SOCKS_SOLID está migrada, como
        // prueba del nuevo sistema.
        CroptopArmorRenderProvider.register();

        TwoToneArmorRenderProvider.register(FemclothesItems.SOCKS_STRIPE_TOP, "socks_stripe_top",
                Identifier.of("femclothes", "textures/models/armor/socks_stripe_top_body.png"),
                Identifier.of("femclothes", "textures/models/armor/socks_stripe_top_stripe.png"));

        TwoToneArmorRenderProvider.register(FemclothesItems.SOCKS_STRIPE_ALT, "socks_stripe_alt",
                Identifier.of("femclothes", "textures/models/armor/socks_stripe_alt_outer.png"),
                Identifier.of("femclothes", "textures/models/armor/socks_stripe_alt_middle.png"));

        // --- SOCKS_SOLID: primera prenda migrada al sistema nuevo ---
        // Se dibuja pegada a la pierna real (BodyPartTrinketRenderer.Part.LEGS)
        // en vez de con geometría de bota. La textura base se tiñe en
        // runtime con ClothingTextureCache, igual que antes pero ya no
        // depende del "dyeable" automático de ArmorItem (eso solo existe
        // en el pipeline viejo).
        Identifier socksSolidBase = Identifier.of("femclothes", "textures/models/armor/socks_solid_layer_1.png");
        TrinketRendererRegistry.registerRenderer(FemclothesItems.SOCKS_SOLID,
                new BodyPartTrinketRenderer(BodyPartTrinketRenderer.Part.LEGS, stack -> {
                    int baseColor = 0xFFFFFF;
                    if (stack.getItem() instanceof ClothingTrinketItem clothing) {
                        baseColor = clothing.getColor(stack);
                    }
                    // El patrón lo escribe el clothing_loom en el ItemStack.
                    // Sin PATTERN_ID la prenda es lisa y alcanza con teñir
                    // la base — el camino barato es el caso comun.
                    Identifier patternId = stack.get(FemclothesComponents.PATTERN_ID);
                    if (patternId == null) {
                        return ClothingTextureCache.tinted(socksSolidBase, baseColor);
                    }
                    Integer patternColor = stack.get(FemclothesComponents.PATTERN_COLOR);
                    return ClothingTextureCache.tintedWithPattern(socksSolidBase, baseColor,
                            ClothingTextureCache.patternMaskFor("socks", patternId),
                            patternColor != null ? patternColor : 0xFFFFFF);
                }));

        // TODO: acá también va el registro de la geometría custom del
        // buzo oversize y la falda del traje de maid vía Armor Model API
        // (ver README, sección "Buzo oversize y Armor Model API").
    }
}
