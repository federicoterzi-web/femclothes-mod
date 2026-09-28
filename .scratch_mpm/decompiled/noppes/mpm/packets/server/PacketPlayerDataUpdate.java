package noppes.mpm.packets.server;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketPlayerDataSend;

public class PacketPlayerDataUpdate {
   public final CompoundTag data;

   public PacketPlayerDataUpdate(CompoundTag data) {
      this.data = data;
   }

   public static void encode(PacketPlayerDataUpdate msg, FriendlyByteBuf buf) {
      buf.m_130079_(msg.data);
   }

   public static PacketPlayerDataUpdate decode(FriendlyByteBuf buf) {
      return new PacketPlayerDataUpdate(buf.m_130260_());
   }

   public static void handle(PacketPlayerDataUpdate msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         ModelData data = ModelData.get(player);
         data.readFromNBT(msg.data);
         if (!player.m_9236_().m_46469_().m_46207_(MorePlayerModels.ALLOW_ENTITY_MODELS)) {
            data.setEntity(null);
         }

         data.save();
         Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
      });
      ctx.get().setPacketHandled(true);
   }
}
