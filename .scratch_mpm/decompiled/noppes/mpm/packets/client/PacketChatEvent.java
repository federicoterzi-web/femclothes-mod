package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.client.ChatMessages;

public class PacketChatEvent {
   public final UUID playerId;
   public final Component message;

   public PacketChatEvent(UUID playerId, Component message) {
      this.playerId = playerId;
      this.message = message;
   }

   public static void encode(PacketChatEvent msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
      buf.m_130083_(msg.message);
   }

   public static PacketChatEvent decode(FriendlyByteBuf buf) {
      return new PacketChatEvent(buf.m_130259_(), buf.m_130238_());
   }

   public static void handle(PacketChatEvent msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ChatMessages.getChatMessages(pl.m_7755_().getString()).addMessage(msg.message);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
