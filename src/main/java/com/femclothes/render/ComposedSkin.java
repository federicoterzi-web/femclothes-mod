package com.femclothes.render;

import com.femclothes.garment.Garments;
import com.femclothes.garment.Parte;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La skin del jugador con la segunda capa borrada donde manda una prenda.
 *
 * Es lo unico que le queda por hacer a la skin. Antes tambien repintaba piel
 * en las zonas expuestas; eso lo hace ahora el cuerpo base, dibujado como
 * geometria propia abajo de la ropa ({@link CuerpoBaseTextures}).
 *
 * Se hace UNA vez sobre la skin y no una vez por prenda. Y si el jugador no
 * lleva ninguna prenda del mod se devuelve la skin original tal cual: costo
 * cero para cualquiera que no use ropa del mod.
 *
 * El enganche es AbstractClientPlayerEntity.getSkinTextures() y no
 * PlayerEntityRenderer.getTexture(), porque 3D Skin Layers lee de ahi.
 */
public final class ComposedSkin {

    private static final Map<String, SkinTextures> CACHE = new HashMap<>();

    private ComposedSkin() {}

    /** La skin original, o una compuesta si hay prendas que la afecten. */
    public static SkinTextures forPlayer(AbstractClientPlayerEntity player, SkinTextures original) {
        if (original == null || original.texture() == null) return original;

        List<Parte> partes = partesGobernadas(player);
        if (partes.isEmpty()) return original;

        boolean slim = original.model() == SkinTextures.Model.SLIM;
        String key = original.texture() + "|" + SkinRegions.clave(partes, slim);
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

        for (CajaSkin.Rect r : SkinRegions.aBorrar(partes, slim)) borrar(composed, r);

        Identifier id = Identifier.of("femclothes", "dynamic/skin_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, ClothingSkinTexture.create(composed));

        SkinTextures result = new SkinTextures(id, original.textureUrl(),
                original.capeTexture(), original.elytraTexture(), original.model(), original.secure());
        CACHE.put(key, result);
        return result;
    }

    private static void borrar(NativeImage img, CajaSkin.Rect r) {
        for (int y = r.y0(); y < Math.min(r.y1(), img.getHeight()); y++) {
            for (int x = r.x0(); x < Math.min(r.x1(), img.getWidth()); x++) {
                img.setColor(x, y, 0);
            }
        }
    }

    /** Las partes del cuerpo que gobierna alguna prenda del mod. */
    private static List<Parte> partesGobernadas(AbstractClientPlayerEntity player) {
        List<ItemStack> prendas = new ArrayList<>();
        TrinketsApi.getTrinketComponent(player).ifPresent(c -> {
            for (var par : c.getAllEquipped()) {
                ItemStack stack = par.getRight();
                if (Garments.esPrenda(stack)) prendas.add(stack);
            }
        });
        return Garments.partesCubiertas(prendas);
    }
}
