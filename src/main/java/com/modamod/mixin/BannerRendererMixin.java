package com.modamod.mixin;

import com.modamod.sublimadora.BannerConImagen;
import com.modamod.sublimadora.BannerHD;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Banner con foto a 16x (2026-10-06): el bloque (puesto, de pared, en la mano y en el inventario) y el escudo pasan por
 * {@code renderCanvas}; si hay una foto en juego se dibuja el mismo {@code ModelPart} con la textura de 1024×1024.
 */
@Mixin(BannerBlockEntityRenderer.class)
public abstract class BannerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At("HEAD"))
    private void modamod$entra(BannerBlockEntity be, float delta, MatrixStack m, VertexConsumerProvider v, int luz, int overlay, CallbackInfo ci) {
        BannerHD.contexto = be instanceof BannerConImagen c ? c.modamod$imagen() : null;
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At("RETURN"))
    private void modamod$sale(BannerBlockEntity be, float delta, MatrixStack m, VertexConsumerProvider v, int luz, int overlay, CallbackInfo ci) {
        BannerHD.contexto = null;
    }

    @Inject(method = "renderCanvas(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;Z)V",
            at = @At("HEAD"), cancellable = true)
    private static void modamod$hd(MatrixStack matrices, VertexConsumerProvider vcp, int luz, int overlay, ModelPart lienzo,
                                      SpriteIdentifier base, boolean esBanner, DyeColor color, BannerPatternsComponent patrones,
                                      boolean brillo, CallbackInfo ci) {
        if (BannerHD.contexto == null) return;
        Identifier tex = BannerHD.textura(esBanner ? BannerHD.Forma.BANNER : BannerHD.Forma.ESCUDO, color);
        if (tex == null) return;   // la foto todavía no bajó: se dibuja el de siempre
        RenderLayer capa = RenderLayer.getEntityCutoutNoCull(tex);
        VertexConsumer vc = brillo ? ItemRenderer.getDirectItemGlintConsumer(vcp, capa, true, true) : vcp.getBuffer(capa);
        lienzo.render(matrices, vc, luz, overlay);
        ci.cancel();
    }
}
