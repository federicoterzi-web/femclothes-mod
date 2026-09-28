package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;

public class PacketPlayerDataSend {
   public final UUID playerId;
   public final CompoundTag compound;

   public PacketPlayerDataSend(UUID playerId, CompoundTag compound) {
      this.playerId = playerId;
      this.compound = compound;
   }

   public static void encode(PacketPlayerDataSend msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
      buf.m_130079_(msg.compound);
   }

   public static PacketPlayerDataSend decode(FriendlyByteBuf buf) {
      return new PacketPlayerDataSend(buf.m_130259_(), buf.m_130260_());
   }

   public static void handle(PacketPlayerDataSend msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ModelData data = ModelData.get(pl);
            data.readFromNBT(msg.compound);
            data.save();
            if (pl == Minecraft.m_91087_().f_91074_) {
               data.lastEdited = System.currentTimeMillis();
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
