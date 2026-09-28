package noppes.mpm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import noppes.mpm.ModelData;
import noppes.mpm.client.ChatMessages;
import noppes.mpm.constants.EnumAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class PlayerRendererMixin {
   @Inject(
      at = {@At("TAIL")},
      method = {"setupRotations"}
   )
   private void setupRotations(AbstractClientPlayer player, PoseStack poseStack, float bob, float rotation, float partialTick, CallbackInfo callbackInfo) {
      ModelData data = ModelData.get(player);
      if (data.moveAnimation == EnumAnimation.SLEEP) {
         poseStack.m_252781_(Axis.f_252529_.m_252977_(90.0F));
         poseStack.m_252880_(0.0F, -(1.8F + data.offsetY()) / 2.0F, 0.0F);
      } else if (data.moveAnimation == EnumAnimation.CRAWL) {
         if (player.m_6047_()) {
            poseStack.m_252880_(0.0F, 0.075F, 0.0F);
         }

         poseStack.m_252781_(Axis.f_252495_.m_252977_(90.0F));
         poseStack.m_252880_(0.0F, -(1.8F + data.offsetY()) / 2.0F, 0.0F);
      }
   }

   @Inject(
      at = {@At("TAIL")},
      method = {"renderNameTag"}
   )
   private void renderNameTag(
      AbstractClientPlayer player, Component p_117809_, PoseStack mStack, MultiBufferSource buffer, int lightmapUV, CallbackInfo callbackInfo
   ) {
      Minecraft mc = Minecraft.m_91087_();
      ChatMessages chat = ChatMessages.getChatMessages(player.m_7755_().getString());
      if (chat.hasMessage()) {
         boolean inRange = player.m_20270_(mc.m_91288_()) <= 4.0F;
         mStack.m_85836_();
         mStack.m_85837_(0.0, 0.7 + player.m_20206_(), 0.0);
         chat.renderMessages(mStack, buffer, 1.0F, inRange, lightmapUV);
         mStack.m_85849_();
      }
   }
}
