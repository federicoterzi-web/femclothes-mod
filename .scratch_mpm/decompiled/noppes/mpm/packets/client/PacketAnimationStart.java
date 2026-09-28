package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;
import noppes.mpm.constants.EnumAnimation;

public class PacketAnimationStart {
   public final UUID playerId;
   public final EnumAnimation animation;

   public PacketAnimationStart(UUID playerId, EnumAnimation animation) {
      this.playerId = playerId;
      this.animation = animation;
   }

   public static void encode(PacketAnimationStart msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
      buf.m_130068_(msg.animation);
   }

   public static PacketAnimationStart decode(FriendlyByteBuf buf) {
      return new PacketAnimationStart(buf.m_130259_(), (EnumAnimation)buf.m_130066_(EnumAnimation.class));
   }

   public static void handle(PacketAnimationStart msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ModelData data = ModelData.get(pl);
            data.setAnimation(msg.animation);
            data.animationStart = pl.f_19797_;
            if (msg.animation == EnumAnimation.SLEEP) {
               data.sleepRotation = pl.f_20883_;
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
