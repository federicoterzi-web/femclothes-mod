package noppes.mpm.packets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.simple.SimpleChannel;
import noppes.mpm.MorePlayerModels;
import noppes.mpm.packets.client.PacketAnimationStart;
import noppes.mpm.packets.client.PacketBackItemUpdate;
import noppes.mpm.packets.client.PacketChatEvent;
import noppes.mpm.packets.client.PacketEyeBlink;
import noppes.mpm.packets.client.PacketParticleAngry;
import noppes.mpm.packets.client.PacketParticleLove;
import noppes.mpm.packets.client.PacketParticleNote;
import noppes.mpm.packets.client.PacketPlayerDataSend;
import noppes.mpm.packets.client.PacketPong;
import noppes.mpm.packets.server.PacketAnimationUpdate;
import noppes.mpm.packets.server.PacketPing;
import noppes.mpm.packets.server.PacketPlayerDataUpdate;
import noppes.mpm.util.MPMScheduler;

public class Packets {
   private static final String PROTOCOL = "mpm";
   public static SimpleChannel Channel;

   public static void register() {
      Channel = ChannelBuilder.named(new ResourceLocation("moreplayermodels", "packets"))
         .clientAcceptedVersions(ver -> ver.equals("mpm") || ver.equals(NetworkRegistry.ABSENT) || ver.equals(NetworkRegistry.ACCEPTVANILLA))
         .serverAcceptedVersions(ver -> ver.equals("mpm") || ver.equals(NetworkRegistry.ABSENT) || ver.equals(NetworkRegistry.ACCEPTVANILLA))
         .networkProtocolVersion(() -> "mpm")
         .simpleChannel();
      int index = 0;
      Channel.registerMessage(index++, PacketAnimationStart.class, PacketAnimationStart::encode, PacketAnimationStart::decode, PacketAnimationStart::handle);
      Channel.registerMessage(index++, PacketBackItemUpdate.class, PacketBackItemUpdate::encode, PacketBackItemUpdate::decode, PacketBackItemUpdate::handle);
      Channel.registerMessage(index++, PacketChatEvent.class, PacketChatEvent::encode, PacketChatEvent::decode, PacketChatEvent::handle);
      Channel.registerMessage(index++, PacketEyeBlink.class, PacketEyeBlink::encode, PacketEyeBlink::decode, PacketEyeBlink::handle);
      Channel.registerMessage(index++, PacketParticleAngry.class, PacketParticleAngry::encode, PacketParticleAngry::decode, PacketParticleAngry::handle);
      Channel.registerMessage(index++, PacketParticleLove.class, PacketParticleLove::encode, PacketParticleLove::decode, PacketParticleLove::handle);
      Channel.registerMessage(index++, PacketParticleNote.class, PacketParticleNote::encode, PacketParticleNote::decode, PacketParticleNote::handle);
      Channel.registerMessage(index++, PacketPong.class, PacketPong::encode, PacketPong::decode, PacketPong::handle);
      Channel.registerMessage(index++, PacketPlayerDataSend.class, PacketPlayerDataSend::encode, PacketPlayerDataSend::decode, PacketPlayerDataSend::handle);
      Channel.registerMessage(index++, PacketAnimationUpdate.class, PacketAnimationUpdate::encode, PacketAnimationUpdate::decode, PacketAnimationUpdate::handle);
      Channel.registerMessage(
         index++, PacketPlayerDataUpdate.class, PacketPlayerDataUpdate::encode, PacketPlayerDataUpdate::decode, PacketPlayerDataUpdate::handle
      );
      Channel.registerMessage(index++, PacketPing.class, PacketPing::encode, PacketPing::decode, PacketPing::handle);
   }

   public static <MSG> void send(ServerPlayer player, MSG msg) {
      if (MorePlayerModels.HasServerSide) {
         Channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
      }
   }

   public static <MSG> void sendDelayed(ServerPlayer player, MSG msg, int delay) {
      if (MorePlayerModels.HasServerSide) {
         MPMScheduler.runTack(() -> Channel.send(PacketDistributor.PLAYER.with(() -> player), msg), delay);
      }
   }

   public static <MSG> void sendNearby(Entity entity, MSG msg) {
      if (MorePlayerModels.HasServerSide) {
         Channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
      }
   }

   public static <MSG> void sendAll(MSG msg) {
      if (MorePlayerModels.HasServerSide) {
         Channel.send(PacketDistributor.ALL.noArg(), msg);
      }
   }

   public static <MSG> void sendServer(MSG msg) {
      if (MorePlayerModels.HasServerSide) {
         Channel.sendToServer(msg);
      }
   }
}
