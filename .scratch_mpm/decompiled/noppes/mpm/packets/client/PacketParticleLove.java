package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;

public class PacketParticleLove {
   public final UUID playerId;

   public PacketParticleLove(UUID playerId) {
      this.playerId = playerId;
   }

   public static void encode(PacketParticleLove msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
   }

   public static PacketParticleLove decode(FriendlyByteBuf buf) {
      return new PacketParticleLove(buf.m_130259_());
   }

   public static void handle(PacketParticleLove msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ModelData data = ModelData.get(pl);
            data.inLove = 40;
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
