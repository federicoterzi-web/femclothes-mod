package noppes.mpm.packets.server;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.packets.Packets;
import noppes.mpm.packets.client.PacketBackItemUpdate;
import noppes.mpm.packets.client.PacketPlayerDataSend;
import noppes.mpm.packets.client.PacketPong;

public class PacketPing {
   public final int version;
   public final CompoundTag data;

   public PacketPing(int version, CompoundTag data) {
      this.version = version;
      this.data = data;
   }

   public static void encode(PacketPing msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.version);
      buf.m_130079_(msg.data);
   }

   public static PacketPing decode(FriendlyByteBuf buf) {
      return new PacketPing(buf.readInt(), buf.m_130260_());
   }

   public static void handle(PacketPing msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (msg.version == MorePlayerModels.Version) {
            ModelData data = ModelData.get(player);
            data.readFromNBT(msg.data);
            if (!player.m_9236_().m_46469_().m_46207_(MorePlayerModels.ALLOW_ENTITY_MODELS)) {
               data.setEntity(null);
            }

            data.save();
            Packets.sendNearby(player, new PacketPlayerDataSend(player.m_20148_(), data.writeToNBT()));
         }

         ItemStack back = (ItemStack)player.m_150109_().f_35974_.get(0);
         if (!back.m_41619_()) {
            Packets.sendNearby(player, new PacketBackItemUpdate(player.m_20148_(), back));
         }

         MorePlayerModels.HasServerSide = true;
         Packets.send(player, new PacketPong(MorePlayerModels.Version));
      });
      ctx.get().setPacketHandled(true);
   }
}
