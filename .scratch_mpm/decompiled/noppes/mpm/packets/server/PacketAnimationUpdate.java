package noppes.mpm.packets.server;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.constants.EnumAnimation;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketAnimationStart;

public record PacketAnimationUpdate(EnumAnimation animation) {
   public static void encode(PacketAnimationUpdate msg, FriendlyByteBuf buf) {
      buf.m_130068_(msg.animation);
   }

   public static PacketAnimationUpdate decode(FriendlyByteBuf buf) {
      return new PacketAnimationUpdate((EnumAnimation)buf.m_130066_(EnumAnimation.class));
   }

   public static void handle(PacketAnimationUpdate msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> setAnimation(ctx.get().getSender(), msg.animation));
      ctx.get().setPacketHandled(true);
   }

   public static void setAnimation(Player player, EnumAnimation animation) {
      if (MorePlayerModels.HasServerSide && player.m_9236_().f_46443_) {
         Packets.sendServer(new PacketAnimationUpdate(animation));
      } else {
         ModelData data = ModelData.get(player);
         if (animation == EnumAnimation.SLEEP) {
            data.sleepRotation = player.f_20883_;
         }

         if (data.animation == animation) {
            animation = EnumAnimation.NONE;
         } else if (data.moveAnimation == animation) {
            animation = EnumAnimation.IDLE;
         }

         data.setAnimation(animation);
         Packets.sendNearby(player, new PacketAnimationStart(player.m_20148_(), animation));
      }
   }
}
