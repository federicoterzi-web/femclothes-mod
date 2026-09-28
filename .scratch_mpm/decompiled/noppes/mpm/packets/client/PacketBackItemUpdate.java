package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;

public record PacketBackItemUpdate(UUID playerId, ItemStack item) {
   public static void encode(PacketBackItemUpdate msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
      buf.m_130055_(msg.item);
   }

   public static PacketBackItemUpdate decode(FriendlyByteBuf buf) {
      return new PacketBackItemUpdate(buf.m_130259_(), buf.m_130267_());
   }

   public static void handle(PacketBackItemUpdate msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ModelData data = ModelData.get(pl);
            data.backItem = msg.item;
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
