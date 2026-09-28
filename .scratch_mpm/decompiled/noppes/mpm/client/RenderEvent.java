package noppes.mpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent.Chat;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.mpm.LogWriter;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.layer.LayerPreRender;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.mixin.EntityMixin;
import noppes.mpm.mixin.LivingRenderer2Mixin;
import noppes.mpm.shared.client.model.util.BatchRenderer;
import noppes.mpm.util.PixelmonHelper;

public class RenderEvent {
   public static RenderEvent Instance;
   private static Entity customEntity;
   public static ResourceLocation entityResource = null;

   public RenderEvent() {
      Instance = this;
      Minecraft mc = Minecraft.m_91087_();
   }

   @SubscribeEvent
   public void post(Post event) {
      if (entityResource != null && event.getEntity() != customEntity) {
         customEntity = null;
         entityResource = null;
      }

      if (event.getEntity() instanceof AbstractClientPlayer) {
         AbstractClientPlayer player = (AbstractClientPlayer)event.getEntity();
         ModelData data = ModelData.get(player);
         if (data.moveAnimation == EnumAnimation.SLEEP) {
            player.f_20883_ = player.f_20884_ = player.m_146909_();
         }

         BatchRenderer.getInstance().draw();
         event.getPoseStack().m_85849_();
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public void pre(Pre event) {
      if (event.getEntity() instanceof AbstractClientPlayer && !event.isCanceled()) {
         AbstractClientPlayer player = (AbstractClientPlayer)event.getEntity();
         Minecraft mc = Minecraft.m_91087_();
         PoseStack mStack = event.getPoseStack();
         mStack.m_85836_();
         if (ClientEventHandler.camera.enabled && player == mc.f_91074_) {
            player.f_20885_ = player.f_20883_;
            player.f_20886_ = player.f_20884_;
            player.m_146926_(player.f_19860_ = ClientEventHandler.camera.playerPitch);
            mc.f_91063_.m_109087_(ClientEventHandler.partialTick);
         }

         ModelData data = ModelData.get(player);
         if (data.moveAnimation == EnumAnimation.SLEEP) {
            player.f_20883_ = player.f_20884_ = data.sleepRotation;
            player.f_20885_ = player.f_20886_ = Math.min(Math.max(player.f_20885_, data.sleepRotation - 60.0F), data.sleepRotation + 60.0F);
            player.f_19860_ = Math.min(Math.max(player.m_146909_(), 0.0F), 60.0F);
            player.m_146926_(player.f_19860_);
         }

         float offset = data.getOffsetCamera(player);
         if (((EntityMixin)player).getDimensions().f_20378_ - offset < 0.0F) {
            offset = 0.0F;
         }

         ((EntityMixin)player).setEyeHeight(player.m_20236_(player.m_20089_()) - offset);
         customEntity = data.getEntity(player);
         if (customEntity != null) {
            if (ClientEventHandler.camera.enabled && player == mc.f_91074_) {
               customEntity.m_146922_(player.m_146908_());
               customEntity.f_19859_ = player.f_19859_;
            }

            event.setCanceled(true);
            if (PixelmonHelper.isPixelmon(customEntity)) {
               customEntity.m_20260_(true);
            }

            entityResource = player.m_108560_();
            mc.m_91290_().m_114384_(customEntity, 0.0, 0.0, 0.0, 0.0F, event.getPartialTick(), mStack, event.getMultiBufferSource(), event.getPackedLight());
            mStack.m_85849_();
         } else {
            offset = 0.0F;
            if (!MorePlayerModels.DisableFlyingAnimation && player.m_150110_().f_35935_ && player.m_9236_().m_46859_(player.m_20183_())) {
               offset = Mth.m_14089_(player.f_19797_ * 0.1F) * -0.06F;
            }

            if (data.moveAnimation == EnumAnimation.SIT) {
               offset = (float)(offset + (0.5 - data.getLegsY() * 0.8));
            }

            mStack.m_252880_(0.0F, -offset, 0.0F);

            for (RenderLayer layer : ((LivingRenderer2Mixin)event.getRenderer()).getLayers()) {
               if (layer instanceof LayerPreRender) {
                  ((LayerPreRender)layer).preRender(player);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void hand(RenderHandEvent event) {
      Minecraft mc = Minecraft.m_91087_();
      ModelData data = ModelData.get(mc.f_91074_);
      Pose pose = mc.f_91074_.m_20089_();
      ((EntityMixin)mc.f_91074_).setEyeHeight(mc.f_91074_.m_20236_(pose) - data.getOffsetCamera(mc.f_91074_));
      Entity entity = data.getEntity(mc.f_91074_);
      if (entity != null
         || data.moveAnimation == EnumAnimation.SLEEP
         || data.moveAnimation == EnumAnimation.CRAWL
         || data.animation == EnumAnimation.BOW && mc.f_91074_.m_21205_().m_41619_()) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void chat(ClientChatReceivedEvent event) {
      if (!MorePlayerModels.HasServerSide) {
         try {
            ChatMessages.parseMessage(event.getMessage().getString());
         } catch (Exception var3) {
            LogWriter.warn("Cant handle chatmessage: " + event.getMessage() + ":" + var3.getMessage());
         }
      }
   }

   @SubscribeEvent
   public void selectionBox(RenderHighlightEvent event) {
      if (MorePlayerModels.HideSelectionBox) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void overlay(Chat event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91080_ == null && MorePlayerModels.Tooltips != 0) {
         ItemStack item = mc.f_91074_.m_21205_();
         if (!item.m_41619_()) {
            String name = item.m_41611_().getString();
            int x = mc.m_91268_().m_85445_() - mc.f_91062_.m_92895_(name);
            int posX = 4;
            int posY = 4;
            if (MorePlayerModels.Tooltips % 2 == 0) {
               posX = x - 4;
            }

            if (MorePlayerModels.Tooltips > 2) {
               posY = mc.m_91268_().m_85446_() - 24;
            }

            event.getGuiGraphics().m_280488_(mc.f_91062_, name, posX, posY, 16777215);
            if (item.m_41763_()) {
               int max = item.m_41776_();
               String dam = max - item.m_41773_() + "/" + max;
               x = mc.m_91268_().m_85445_() - mc.f_91062_.m_92895_(dam);
               if (MorePlayerModels.Tooltips == 2 || MorePlayerModels.Tooltips == 4) {
                  posX = x - 4;
               }

               event.getGuiGraphics().m_280488_(mc.f_91062_, dam, posX, posY + 12, 16777215);
            }
         }
      }
   }
}
