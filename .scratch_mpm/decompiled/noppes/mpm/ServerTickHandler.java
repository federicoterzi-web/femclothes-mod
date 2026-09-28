package noppes.mpm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import noppes.mpm.client.parts.MpmPartData;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketBackItemUpdate;

public class ServerTickHandler {
   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent event) {
      if (event.side != LogicalSide.CLIENT && event.phase != Phase.START) {
         ServerPlayer player = (ServerPlayer)event.player;
         ModelData data = ModelData.get(player);
         ItemStack item = (ItemStack)player.m_150109_().f_35974_.get(0);
         if (data.backItem != item) {
            Packets.send(player, new PacketBackItemUpdate(player.m_20148_(), item));
            data.backItem = item;
         }

         for (MpmPartData pd : data.mpmParts) {
            if (pd instanceof ModelEyeData) {
               ((ModelEyeData)pd).update(player);
            }
         }

         checkMovementAnimation(player, data);
         if (data.animation != EnumAnimation.NONE) {
            checkAnimation(player, data);
         }

         data.prevPosX = player.m_20185_();
         data.prevPosY = player.m_20186_();
         data.prevPosZ = player.m_20189_();
      }
   }

   public static void checkMovementAnimation(Player player, ModelData data) {
      double motionX = data.prevPosX - player.m_20185_();
      double motionY = data.prevPosY - player.m_20186_();
      double motionZ = data.prevPosZ - player.m_20189_();
      double speed = motionX * motionX + motionZ * motionZ;
      boolean isMoving = speed > 0.006 && !player.m_6144_();
      boolean isJumping = motionY * motionY > 0.08;
      boolean isFlying = player.m_9236_().m_46859_(player.m_20183_()) && player.m_9236_().m_46859_(player.m_20183_().m_7495_());
      if (player.m_6067_()) {
         data.setMoveAnimation(EnumAnimation.SWIM);
      } else if (isFlying) {
         data.setMoveAnimation(isMoving ? EnumAnimation.FLY : EnumAnimation.FLY_IDLE);
      } else if (!(speed < 0.001) || isJumping || data.moveAnimation != EnumAnimation.DEATH && data.moveAnimation != EnumAnimation.SLEEP) {
         if (data.moveAnimation != EnumAnimation.CRAWL && data.moveAnimation != EnumAnimation.SIT) {
            if (data.moveAnimation != EnumAnimation.CROUCH || isMoving) {
               data.setMoveAnimation(isMoving ? EnumAnimation.WALK : EnumAnimation.IDLE);
            }
         } else {
            if (isMoving || isJumping) {
               data.setMoveAnimation(EnumAnimation.WALK);
            }
         }
      }
   }

   public static void checkAnimation(Player player, ModelData data) {
      if (!(data.prevPosY <= 0.0) && player.f_19797_ >= 40) {
         double motionX = data.prevPosX - player.m_20185_();
         double motionY = data.prevPosY - player.m_20186_();
         double motionZ = data.prevPosZ - player.m_20189_();
         double speed = motionX * motionX + motionZ * motionZ;
         boolean isJumping = motionY * motionY > 0.08;
         if (data.animationTime > 0) {
            data.animationTime--;
         }

         if (player.m_5803_() || player.m_20159_() || data.animationTime == 0 || data.animation == EnumAnimation.BOW && player.m_6144_()) {
            data.setAnimation(EnumAnimation.NONE);
         }

         if (isJumping || !player.m_6144_() || data.animation != EnumAnimation.HUG && data.animation != EnumAnimation.DANCE) {
            if (speed > 0.01 || isJumping || player.m_5803_() || data.animation == EnumAnimation.SLEEP && speed > 0.001) {
               data.setAnimation(EnumAnimation.NONE);
            }
         }
      }
   }
}
