package noppes.mpm.packets.client;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import noppes.mpm.ModelData;
import noppes.mpm.ModelEyeData;
import noppes.mpm.client.parts.MpmPartData;

public class PacketEyeBlink {
   public final UUID playerId;

   public PacketEyeBlink(UUID playerId) {
      this.playerId = playerId;
   }

   public static void encode(PacketEyeBlink msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerId);
   }

   public static PacketEyeBlink decode(FriendlyByteBuf buf) {
      return new PacketEyeBlink(buf.m_130259_());
   }

   public static void handle(PacketEyeBlink msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         Player pl = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerId);
         if (pl != null) {
            ModelData data = ModelData.get(pl);

            for (MpmPartData pd : data.mpmParts) {
               if (pd instanceof ModelEyeData) {
                  ((ModelEyeData)pd).blinkStart = System.currentTimeMillis();
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
