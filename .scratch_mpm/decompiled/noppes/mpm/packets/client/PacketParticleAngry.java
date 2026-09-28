package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;

public class PacketParticleAngry {
   public final UUID playerId;

   public PacketParticleAngry(UUID playerId) {
      this.playerId = playerId;
   }

   public static void encode(PacketParticleAngry msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
   }

   public static PacketParticleAngry decode(FriendlyByteBuf buf) {
      return new PacketParticleAngry(buf.m_130259_());
   }

   public static void handle(PacketParticleAngry msg, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               Player player = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
               if (player != null) {
                  ModelData data = ModelData.get(player);

                  for (int i = 0; i < 5; i++) {
                     double d0 = player.m_217043_().m_188583_() * 0.02;
                     double d1 = player.m_217043_().m_188583_() * 0.02;
                     double d2 = player.m_217043_().m_188583_() * 0.02;
                     double x = player.m_20185_() + (player.m_217043_().m_188501_() - 0.5F) * player.m_20205_() * 2.0F;
                     double z = player.m_20189_() + (player.m_217043_().m_188501_() - 0.5F) * player.m_20205_() * 2.0F;
                     player.m_9236_()
                        .m_7106_(
                           ParticleTypes.f_123792_,
                           x,
                           player.m_20186_() + 0.8F + player.m_217043_().m_188501_() * player.m_20206_() / 2.0F - player.m_6049_() - data.getBodyY(),
                           z,
                           d0,
                           d1,
                           d2
                        );
                  }
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
