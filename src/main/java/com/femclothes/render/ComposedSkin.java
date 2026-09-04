package com.femclothes.render;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma una version de la skin del jugador preparada para la ropa del mod:
 * limpia la segunda capa donde va una prenda y repinta con tono de piel lo
 * que la prenda deja expuesto.
 *
 * Se hace UNA vez sobre la skin, no una vez por prenda. Antes cada prenda
 * rellenaba de piel todo lo que dejaba transparente, y por eso dos prendas
 * en la misma parte del cuerpo se pisaban entera una a la otra.
 *
 * Si el jugador no lleva ninguna prenda que toque la skin, esto no hace nada
 * y se devuelve la skin original tal cual: costo cero para cualquiera que no
 * use ropa del mod.
 */
public final class ComposedSkin {

    private static final Map<String, SkinTextures> CACHE = new HashMap<>();

    private ComposedSkin() {}

    /** La skin original, o una compuesta si hay prendas que la afecten. */
    public static SkinTextures forPlayer(AbstractClientPlayerEntity player, SkinTextures original) {
        if (original == null || original.texture() == null) return original;

        List<ItemStack> prendas = prendasQueAfectan(player);
        if (prendas.isEmpty()) return original;

        String key = original.texture() + "|" + firma(prendas);
        SkinTextures cached = CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = SkinTextureAccess.tryGetImage(original);
        if (base == null) return original;

        NativeImage composed;
        try {
            composed = new NativeImage(base.getWidth(), base.getHeight(), false);
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    composed.setColor(x, y, base.getColor(x, y));
                }
            }
        } catch (Exception e) {
            return original;
        }

        SkinToneSampler.Tones tono = SkinToneSampler.sampleTones(base, original.model());
        for (ItemStack stack : prendas) {
            SkinRegions.Effect ef = SkinRegions.of(stack.getItem());
            if (ef == null) continue;
            // Capa externa: se borra. Ademas de sacar el pantalon pintado,
            // hace que 3D Skin Layers no extruya geometria ahi, porque solo
            // extruye pixeles solidos.
            for (SkinRegions.Rect r : ef.clearOverlay()) pintar(composed, r, 0);
            // Capa base: piel plana donde la prenda deja el cuerpo a la vista.
            for (SkinRegions.Rect r : ef.bareSkin()) pintar(composed, r, tono.mid());
        }

        Identifier id = Identifier.of("femclothes", "dynamic/skin_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, ClothingSkinTexture.create(composed));

        SkinTextures result = new SkinTextures(id, original.textureUrl(),
                original.capeTexture(), original.elytraTexture(), original.model(), original.secure());
        CACHE.put(key, result);
        return result;
    }

    private static void pintar(NativeImage img, SkinRegions.Rect r, int abgr) {
        for (int y = r.y0(); y < Math.min(r.y1(), img.getHeight()); y++) {
            for (int x = r.x0(); x < Math.min(r.x1(), img.getWidth()); x++) {
                img.setColor(x, y, abgr);
            }
        }
    }

    private static List<ItemStack> prendasQueAfectan(AbstractClientPlayerEntity player) {
        List<ItemStack> out = new ArrayList<>();
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            for (var par : c.getAllEquipped()) {
                ItemStack stack = par.getRight();
                if (!stack.isEmpty() && SkinRegions.afecta(stack.getItem())) out.add(stack);
            }
        });
        return out;
    }

    /** Identifica el conjunto de prendas puestas, para cachear. */
    private static String firma(List<ItemStack> prendas) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : prendas) sb.append(s.getItem()).append(';');
        return sb.toString();
    }
}
