package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketParticleNote {
   public final UUID playerId;
   public final int note;

   public PacketParticleNote(UUID playerId, int note) {
      this.playerId = playerId;
      this.note = note;
   }

   public static void encode(PacketParticleNote msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
      buf.writeInt(msg.note);
   }

   public static PacketParticleNote decode(FriendlyByteBuf buf) {
      return new PacketParticleNote(buf.m_130259_(), buf.readInt());
   }

   public static void handle(PacketParticleNote msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            pl.m_9236_().m_7106_(ParticleTypes.f_123758_, pl.m_20185_(), pl.m_20186_() + 2.0, pl.m_20189_(), msg.note / 24.0, 0.0, 0.0);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
