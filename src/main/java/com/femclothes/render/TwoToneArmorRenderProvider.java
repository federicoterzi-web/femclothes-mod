package com.femclothes.render;

import com.femclothes.item.ClothingArmorItem;
import com.femclothes.item.FemclothesComponents;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Renderer reusable para prendas de DOS colores simultáneos (rayas,
 * alternado). Mismo truco de composición en CPU vía NativeImage que el
 * croptop, pero con dos texturas de zona en vez de una + tono de piel:
 * cada zona es un .png separado, opaco solo donde corresponde a esa
 * zona y transparente en el resto — se combinan y tiñen en un solo
 * bitmap final, cacheado por combinación de colores.
 *
 * zoneA se tiñe con DataComponentTypes.DYED_COLOR (color "principal",
 * el mismo mecanismo que ya usan las prendas de un solo color).
 * zoneB se tiñe con FemclothesComponents.STRIPE_COLOR (color secundario).
 * Si ambas zonas tienen pixel opaco en el mismo punto, gana zoneB (para
 * que la raya se dibuje "encima" del cuerpo si se solaparan por error
 * de arte).
 */
public final class TwoToneArmorRenderProvider implements ArmorRenderer {

    private final String cacheKeyPrefix;
    private final Identifier zoneATexture;
    private final Identifier zoneBTexture;

    private final Map<String, Identifier> compositeCache = new HashMap<>();
    private NativeImage zoneAImageCache;
    private NativeImage zoneBImageCache;
    private boolean loadFailed = false;

    private TwoToneArmorRenderProvider(String cacheKeyPrefix, Identifier zoneATexture, Identifier zoneBTexture) {
        this.cacheKeyPrefix = cacheKeyPrefix;
        this.zoneATexture = zoneATexture;
        this.zoneBTexture = zoneBTexture;
    }

    public static void register(Item item, String cacheKeyPrefix, Identifier zoneATexture, Identifier zoneBTexture) {
        ArmorRenderer.register(new TwoToneArmorRenderProvider(cacheKeyPrefix, zoneATexture, zoneBTexture), item);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                        LivingEntity entity, EquipmentSlot slot, int light,
                        BipedEntityModel<LivingEntity> contextModel) {

        int zoneAColor = 0xFFFFFF;
        if (stack.getItem() instanceof ClothingArmorItem clothing) {
            zoneAColor = clothing.getColor(stack);
        }
        Integer stripe = stack.get(FemclothesComponents.PATTERN_COLOR);
        int zoneBColor = stripe != null ? stripe : 0xFFFFFF;

        Identifier texture = getOrBuildComposite(zoneAColor, zoneBColor);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(texture));
        contextModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
    }

    private Identifier getOrBuildComposite(int zoneAColor, int zoneBColor) {
        String key = zoneAColor + ":" + zoneBColor;
        Identifier cached = compositeCache.get(key);
        if (cached != null) return cached;

        NativeImage zoneA = getImage(true);
        NativeImage zoneB = getImage(false);
        if (zoneA == null || zoneB == null) {
            return zoneATexture; // falta arte todavía: se ve la textura cruda
        }

        int width = zoneA.getWidth();
        int height = zoneA.getHeight();
        NativeImage composite = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pxB = zoneB.getColor(x, y);
                int aB = (pxB >> 24) & 0xFF;
                if (aB != 0) {
                    composite.setColor(x, y, tint(pxB, zoneBColor));
                    continue;
                }
                int pxA = zoneA.getColor(x, y);
                int aA = (pxA >> 24) & 0xFF;
                if (aA != 0) {
                    composite.setColor(x, y, tint(pxA, zoneAColor));
                } else {
                    composite.setColor(x, y, 0);
                }
            }
        }

        Identifier id = Identifier.of("femclothes",
                "dynamic/" + cacheKeyPrefix + "_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        compositeCache.put(key, id);
        return id;
    }

    private int tint(int px, int rgb) {
        int a = (px >> 24) & 0xFF;
        int bChan = (px >> 16) & 0xFF;
        int gChan = (px >> 8) & 0xFF;
        int rChan = px & 0xFF;

        int dr = (rgb >> 16) & 0xFF;
        int dg = (rgb >> 8) & 0xFF;
        int db = rgb & 0xFF;

        int tr = (rChan * dr) / 255;
        int tg = (gChan * dg) / 255;
        int tb = (bChan * db) / 255;

        return (a << 24) | (tb << 16) | (tg << 8) | tr;
    }

    private NativeImage getImage(boolean zoneA) {
        if (loadFailed) return null;
        if (zoneA && zoneAImageCache != null) return zoneAImageCache;
        if (!zoneA && zoneBImageCache != null) return zoneBImageCache;

        Identifier id = zoneA ? zoneATexture : zoneBTexture;
        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) {
                loadFailed = true;
                return null;
            }
            try (InputStream stream = resource.get().getInputStream()) {
                NativeImage img = NativeImage.read(stream);
                if (zoneA) {
                    zoneAImageCache = img;
                } else {
                    zoneBImageCache = img;
                }
                return img;
            }
        } catch (IOException e) {
            loadFailed = true;
            return null;
        }
    }
}
