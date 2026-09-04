package com.femclothes.render;

import com.femclothes.item.ClothingArmorItem;
import com.femclothes.item.FemclothesItems;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Render provider del croptop. En vez de tintar por vértice (esas APIs se
 * mueven de versión a versión y ya nos rompieron dos veces), compone la
 * textura final en CPU con NativeImage: lee la textura base del croptop,
 * la tiñe con el color elegido por el jugador, y rellena con el tono de
 * piel muestreado cualquier zona con alpha 0 (la panza expuesta). El
 * resultado se cachea por combinación de (color, tono de piel) y se
 * registra como textura dinámica — el render en sí queda un simple
 * render(model, texture) de 4 argumentos, sin depender de ningún método
 * de tinte de VertexConsumer.
 *
 * OJO de implementación: NativeImage empaqueta el color como ABGR (no
 * ARGB) — R en el byte más bajo, A en el más alto. Todo el manejo de
 * bits acá respeta ese orden.
 */
public final class CroptopArmorRenderProvider implements ArmorRenderer {

    public static final CroptopArmorRenderProvider INSTANCE = new CroptopArmorRenderProvider();
    private static final Identifier BASE_TEXTURE = Identifier.of("femclothes", "textures/models/armor/croptop.png");

    private static final Map<String, Identifier> COMPOSITE_CACHE = new HashMap<>();
    private static NativeImage baseImageCache;
    private static boolean baseImageLoadFailed = false;

    public static void register() {
        ArmorRenderer.register(INSTANCE, FemclothesItems.CROPTOP);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                        LivingEntity entity, EquipmentSlot slot, int light,
                        BipedEntityModel<LivingEntity> contextModel) {

        int dyeColor = 0xFFFFFF;
        if (stack.getItem() instanceof ClothingArmorItem clothing) {
            dyeColor = clothing.getColor(stack);
        }

        int skinTone = 0xFFC49A78; // fallback ABGR-ish beige neutro
        if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity player) {
            SkinTextures skin = player.getSkinTextures();
            NativeImage img = SkinTextureAccess.tryGetImage(skin);
            if (img != null) {
                skinTone = SkinToneSampler.sampleSkinTone(img, skin.model());
            }
        }

        Identifier texture = getOrBuildComposite(dyeColor, skinTone);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(texture));
        contextModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
    }

    private Identifier getOrBuildComposite(int dyeColor, int skinTone) {
        String key = dyeColor + ":" + skinTone;
        Identifier cached = COMPOSITE_CACHE.get(key);
        if (cached != null) return cached;

        NativeImage base = getBaseImage();
        if (base == null) {
            // No se pudo leer la textura base (todavía no existe el arte, o
            // falló la carga) — devolvemos el identifier "crudo" tal cual,
            // Minecraft mostrará el missing-texture checker en vez de crashear.
            return BASE_TEXTURE;
        }

        int width = base.getWidth();
        int height = base.getHeight();
        NativeImage composite = new NativeImage(width, height, true);

        int dr = (dyeColor >> 16) & 0xFF;
        int dg = (dyeColor >> 8) & 0xFF;
        int db = dyeColor & 0xFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = base.getColor(x, y);
                int a = (px >> 24) & 0xFF;

                if (a == 0) {
                    composite.setColor(x, y, skinTone);
                } else {
                    int bChan = (px >> 16) & 0xFF;
                    int gChan = (px >> 8) & 0xFF;
                    int rChan = px & 0xFF;

                    int tr = (rChan * dr) / 255;
                    int tg = (gChan * dg) / 255;
                    int tb = (bChan * db) / 255;

                    composite.setColor(x, y, (a << 24) | (tb << 16) | (tg << 8) | tr);
                }
            }
        }

        Identifier id = Identifier.of("femclothes", "dynamic/croptop_" + Integer.toHexString(key.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(composite));
        COMPOSITE_CACHE.put(key, id);
        return id;
    }

    private NativeImage getBaseImage() {
        if (baseImageCache != null) return baseImageCache;
        if (baseImageLoadFailed) return null;

        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(BASE_TEXTURE);
            if (resource.isEmpty()) {
                baseImageLoadFailed = true;
                return null;
            }
            try (InputStream stream = resource.get().getInputStream()) {
                baseImageCache = NativeImage.read(stream);
            }
        } catch (IOException e) {
            baseImageLoadFailed = true;
            return null;
        }
        return baseImageCache;
    }
}
