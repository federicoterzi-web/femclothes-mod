package noppes.mpm.packets.client;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.client.gui.GuiCreationScreenInterface;

public class PacketPong {
   public final int version;

   public PacketPong(int version) {
      this.version = version;
   }

   public static void encode(PacketPong msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.version);
   }

   public static PacketPong decode(FriendlyByteBuf buf) {
      return new PacketPong(buf.readInt());
   }

   public static void handle(PacketPong msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (msg.version == MorePlayerModels.Version) {
            MorePlayerModels.HasServerSide = true;
            GuiCreationScreenInterface.Message = "";
         } else if (msg.version < MorePlayerModels.Version) {
            MorePlayerModels.HasServerSide = false;
            GuiCreationScreenInterface.Message = "message.lowerversion";
         } else if (msg.version > MorePlayerModels.Version) {
            MorePlayerModels.HasServerSide = false;
            GuiCreationScreenInterface.Message = "message.higherversion";
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
